import {Routes} from '@angular/router';
import {EntitySearchPageComponent, EntitySearchResultsPageComponent} from 'searchcrudstone';
import {LandingPageComponent} from './pages/landing/landing-page.component';
import {AboutPageComponent} from './pages/about/about-page.component';
import {ContactPageComponent} from './pages/contact/contact-page.component';

// CarListing has @Searchable(externalResult = true) — /carListings auto-navigates to
// /carListings/results once a search runs (EntitySearchComponent's own externalResult handling).
// '' now renders a dedicated marketing landing page (hero, stats, browse-by-body-type,
// user-requested) instead of redirecting straight into the search bar — /carListings is still the
// direct search entry point (linked from the landing page's own CTA, the footer, and the header).
//
// Deliberately 'carListings' (the entity's own NAME), not 'listings' (its admin-side PATH,
// what carstone-admin-ui's sidebar links to) — EntitySearchComponent.runSearch() always builds
// the forward-to-results URL from `context().name`, never whatever route alias the user arrived
// by (confirmed live: visiting /listings and clicking Search still lands on /carListings/
// results). Aliasing this app's own default to the same canonical name avoids ever fighting
// that, rather than trying to make search-crudstone preserve an alias it has no concept of.
export const routes: Routes = [
  {path: '', component: LandingPageComponent},
  // must come BEFORE the ':entity' catch-all below, or search-crudstone would try (and fail) to
  // load a search context named "about"/"contact"
  {path: 'about', component: AboutPageComponent},
  {path: 'contact', component: ContactPageComponent},
  {path: ':entity/results', component: EntitySearchResultsPageComponent},
  {path: ':entity', component: EntitySearchPageComponent},
];
