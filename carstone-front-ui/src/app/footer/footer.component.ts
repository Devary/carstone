import {Component, computed, inject, input} from '@angular/core';
import {toObservable, toSignal} from '@angular/core/rxjs-interop';
import {HttpClient} from '@angular/common/http';
import {RouterLink} from '@angular/router';
import {switchMap} from 'rxjs';
import {themeVars} from 'searchcrudstone';
import {environment} from '../../environments/environment';
import {FooterConfig} from './footer.model';

/**
 * Self-fetching, like searchcrudstone's own EntitySearchComponent/sidebarcrudstone's sb-sidebar —
 * given a footer NAME, it fetches its own `GET /footer/{name}` and renders whatever the backend's
 * `@Footer`-annotated class declares, generically (no per-section special-casing: every section
 * renders the same way regardless of which optional pieces — logo/description/links/input/
 * buttons — it happens to carry). Themed through the SAME themeVars() override system
 * searchcrudstone/sidebar-crudstone already use (imported from searchcrudstone, not
 * reimplemented), not any hardcoded colors of its own.
 *
 * Deliberately still local to carstone-front-ui, not a published "footer-crudstone" library (a
 * user decision — the annotation/backend contract IS shared via context-gen's own @Footer, only
 * the rendering component isn't yet) — imports `environment` directly rather than a DI-injected
 * config token the way a real portable library would, since there's exactly one consumer today.
 */
@Component({
  selector: 'sf-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss',
  host: {'[style]': 'themeStyle()'},
})
export class FooterComponent {
  readonly name = input<string>('main');

  private readonly http = inject(HttpClient);

  protected readonly footer = toSignal(
    toObservable(this.name).pipe(
      switchMap(name => this.http.get<FooterConfig>(`${environment.apiUrl}footer/${name}`)),
    ),
    {initialValue: null},
  );

  protected readonly themeStyle = computed(() => themeVars(this.footer()?.theme ?? 'primary'));
  protected readonly year = new Date().getFullYear();

  // an absolute URL (http.../https...) is an external link (plain <a href target=_blank>); a
  // root-relative path ("/about") is an internal app route (Angular [routerLink]) — mailto:/tel:
  // hrefs (used by buttons, not links) are handled separately, this only classifies link URLs
  protected isExternal(url: string): boolean {
    return /^https?:\/\//.test(url);
  }

  // PrimeIcons classes are always two space-separated tokens starting with "pi " (e.g.
  // "pi pi-facebook") — anything else (a URL/path) is treated as an image instead
  protected isIconClass(logo: string): boolean {
    return logo.startsWith('pi ');
  }
}
