import {Component, computed, effect, signal} from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {Toast} from 'primeng/toast';
import {themeVars} from 'searchcrudstone';
import {FooterComponent} from './footer/footer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Toast, FooterComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  // CarListing itself is theme="blue" (search bar/results already render blue from that context
  // value) — applying the SAME themeVars('blue') override here too, at the app root, means the
  // two static pages (About/Contact) and the footer inherit the identical accent instead of
  // falling back to PrimeNG's own Aura default, so the whole site reads as one consistent brand.
  host: {'[style]': 'themeStyle()'},
})
export class AppComponent {
  title = 'carstone-front-ui';
  protected readonly themeStyle = computed(() => themeVars('blue'));

  /** Dark mode, persisted across sessions; drives Aura's .app-dark selector on <html> — same
   * pattern standardized across dynamic-crud/search-crudstone/sidebar-crudstone/carstone-admin-ui.
   * Defaults to true (unlike the sibling apps, which default false): this app's whole dark/blurry
   * glassmorphism look (user-requested) only renders under html.app-dark — still user-togglable. */
  protected readonly dark = signal(localStorage.getItem('darkMode') !== 'false');

  constructor() {
    effect(() => {
      document.documentElement.classList.toggle('app-dark', this.dark());
      localStorage.setItem('darkMode', String(this.dark()));
    });
  }

  protected toggleDark(): void {
    this.dark.update(dark => !dark);
  }
}
