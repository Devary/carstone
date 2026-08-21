import {Component} from '@angular/core';
import {RouterLink} from '@angular/router';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss',
})
export class LandingPageComponent {
  // real CarListing.BodyType enum values (carstone-domain) — each chip pre-fills the search
  // bar's bodyType filter via router navigation `state` (EntitySearchPageComponent's own
  // `initialFilters` reads `history.state.filterValues`, the same mechanism its "Back to search"
  // link already relies on), landing the user on a pre-narrowed bar rather than a blank one. It
  // doesn't auto-run the search (EntitySearchPageComponent hardcodes autoSearchOnInit=false) —
  // the user still confirms with their own Search click, same as any other filter change.
  protected readonly bodyTypes = [
    {label: 'Sedan', value: 'SEDAN', icon: 'pi-car'},
    {label: 'SUV', value: 'SUV', icon: 'pi-truck'},
    {label: 'Hatchback', value: 'HATCHBACK', icon: 'pi-car'},
    {label: 'Estate', value: 'ESTATE', icon: 'pi-box'},
    {label: 'Coupe', value: 'COUPE', icon: 'pi-car'},
    {label: 'Convertible', value: 'CONVERTIBLE', icon: 'pi-sun'},
  ];
}
