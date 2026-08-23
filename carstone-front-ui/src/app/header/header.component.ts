import {Component, computed, HostListener, inject, input, signal} from '@angular/core';
import {toObservable, toSignal} from '@angular/core/rxjs-interop';
import {HttpClient} from '@angular/common/http';
import {NgTemplateOutlet} from '@angular/common';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {switchMap} from 'rxjs';
import {themeVars} from 'searchcrudstone';
import {environment} from '../../environments/environment';
import {HeaderConfig, HeaderLink, HeaderSection} from './header.model';

/**
 * Self-fetching, same pattern as `sf-footer`/searchcrudstone's own `EntitySearchComponent` — given
 * a header NAME, fetches its own `GET /header/{name}` and renders whatever the backend's
 * `@Header`-annotated class declares.
 *
 * The SAME `sections` data renders two different ways depending on `variant`, not two different
 * data shapes (see `@Header`'s own context-gen javadoc):
 * - `mega_menu`: each section is its own top-level trigger (labeled by its own `title`), opening
 *   a dropdown panel listing that section's own links.
 * - `simple_menu`: every section's own links are flattened into one row (section boundaries/
 *   titles dropped — a simple menu conventionally declares a single, untitled section anyway).
 *   A link within that row can itself be a dropdown (nested children), same as a mega-menu
 *   panel's own entries can.
 *
 * `position` (left/right/center) is applied as `justify-content` on this component's own host —
 * `app.component.scss` gives it a flexible middle grid column to align within, between the logo
 * and the dark-mode toggle.
 */
@Component({
  selector: 'sh-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgTemplateOutlet],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
  host: {'[style]': 'themeStyle()'},
})
export class HeaderComponent {
  readonly name = input<string>('main');

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  protected readonly header = toSignal(
    toObservable(this.name).pipe(
      switchMap(name => this.http.get<HeaderConfig>(`${environment.apiUrl}header/${name}`)),
    ),
    {initialValue: null},
  );

  protected readonly themeStyle = computed(() => themeVars(this.header()?.theme ?? 'primary'));

  // simple_menu only: every section's own links flattened into one row — see this class's own
  // header comment for why section boundaries/titles are dropped here
  protected readonly flatLinks = computed<HeaderLink[]>(() =>
    (this.header()?.sections ?? []).flatMap(section => section.links));

  // which top-level trigger is currently open — a mega_menu's own HeaderSection, or a
  // simple_menu's own dropdown-type HeaderLink. Identified by object reference (not an
  // index/label), so two differently-positioned same-labeled triggers never collide.
  private readonly openTrigger = signal<HeaderSection | HeaderLink | null>(null);

  protected toggle(trigger: HeaderSection | HeaderLink, event: Event): void {
    event.stopPropagation();
    this.openTrigger.update(current => (current === trigger ? null : trigger));
  }

  protected isOpen(trigger: HeaderSection | HeaderLink): boolean {
    return this.openTrigger() === trigger;
  }

  // any click that reaches document (i.e. wasn't stopped by toggle() above — a click on a plain
  // link, or anywhere outside this component entirely) closes whichever trigger is currently open
  @HostListener('document:click')
  protected closeOnOutsideClick(): void {
    this.openTrigger.set(null);
  }

  // a link's own url MAY carry a "?field=value" pair (see MainHeader's own header comment) —
  // context-gen's @Header contract only knows a link as a plain label+url, it has no concept of
  // a search filter; encoding it in the url and decoding it back out here is what turns a mega
  // menu entry like "SUV" into an ACTUAL pre-filtered search instead of just a page load. A plain
  // [routerLink] can't carry navigation state at all, so a query-string link renders as a plain
  // href (real, working without JS — a fallback for ctrl-click/hover-preview/no-JS) but is
  // intercepted on click and replayed as a stateful navigation instead — the exact mechanism the
  // landing page's own "Browse by body type" cards already use.
  protected hasFilter(url: string): boolean {
    return url.includes('?');
  }

  protected navigateWithFilter(url: string, event: Event): void {
    event.preventDefault();
    const [path, queryString] = url.split('?');
    const filterValues: Record<string, string> = {};
    new URLSearchParams(queryString).forEach((value, key) => {
      filterValues[key] = value;
    });
    this.router.navigate([path], {state: {filterValues}});
  }
}
