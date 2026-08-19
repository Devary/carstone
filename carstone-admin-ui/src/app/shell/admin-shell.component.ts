import {Component, computed, inject, signal} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {ActivatedRoute, NavigationEnd, Router, RouterOutlet} from '@angular/router';
import {filter, map, startWith} from 'rxjs';
import {SidebarComponent} from 'sidebarcrudstone';

/**
 * The admin app's persistent shell: sb-sidebar (fetches the backend's own "main" @Sidebar
 * config directly via its `name` input — no local settings/merge needed the way sidebar-
 * crudstone's own playground demo does, since this app's sidebar layout is fixed, not user-
 * configurable) pinned beside a router-outlet. sidebar-crudstone's own demo has no page like
 * this (SidebarPageComponent just renders `<sb-sidebar>` alone, no outlet) — this is genuinely
 * new, reusing only the pinned-layout CSS math from that demo's playground.component.scss
 * (MainSidebar is variant=SIDEBAR/collapsible=ICON, a fixed, known config, so the margin
 * calculation below is simplified to just those two states rather than the playground's fully
 * generic variant-aware version).
 */
@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent],
  templateUrl: './admin-shell.component.html',
  styleUrl: './admin-shell.component.scss',
})
export class AdminShellComponent {

  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly sidebarOpen = signal(true);

  protected readonly activePaths = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      startWith(null),
      map(() => {
        const entity = this.route.snapshot.firstChild?.paramMap.get('entity');
        return entity ? [entity] : [];
      }),
    ),
    {initialValue: []},
  );

  protected readonly contentStyle = computed<Record<string, string>>(() => ({
    'margin-left': this.sidebarOpen() ? '16rem' : '3.25rem',
  }));
}
