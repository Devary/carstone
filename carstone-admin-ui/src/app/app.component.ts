import {Component, effect, signal} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {Toast} from 'primeng/toast';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, Toast],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  title = 'carstone-admin-ui';

  /** Dark mode, persisted across sessions; drives Aura's .app-dark selector on <html> — same
   * pattern standardized across dynamic-crud/search-crudstone/sidebar-crudstone. */
  protected readonly dark = signal(localStorage.getItem('darkMode') === 'true');

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
