import {Component, computed, input} from '@angular/core';
import {RouterLink} from '@angular/router';
import {themeVars} from 'searchcrudstone';
import {FooterConfig} from './footer.model';

// Parametrized (config-driven via [config]), same reusable-component shape as searchcrudstone's
// own EntitySearchComponent and sidebar-crudstone's sb-sidebar — themed through the SAME
// themeVars() override system those two already use (imported from searchcrudstone, not
// reimplemented), rather than any hardcoded colors of its own.
@Component({
  selector: 'sf-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.scss',
  host: {'[style]': 'themeStyle()'},
})
export class FooterComponent {
  readonly config = input.required<FooterConfig>();
  readonly theme = input<string>('blue');

  protected readonly themeStyle = computed(() => themeVars(this.theme()));
  protected readonly year = new Date().getFullYear();
}
