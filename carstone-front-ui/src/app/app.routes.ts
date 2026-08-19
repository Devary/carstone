import {Routes} from '@angular/router';
import {EntitySearchPageComponent, EntitySearchResultsPageComponent} from 'searchcrudstone';

// CarListing has @Searchable(externalResult = true) — landing on /carListings auto-navigates to
// /carListings/results once a search runs (EntitySearchComponent's own externalResult handling),
// which genuinely IS the "search+results page is the homepage" autoscout24 experience with no
// extra redirect logic needed here beyond '' -> 'carListings'. Same route SHAPE as search-
// crudstone's own demo app (':entity/results' + ':entity'), just with our own default entity.
//
// Deliberately 'carListings' (the entity's own NAME), not 'listings' (its admin-side PATH,
// what carstone-admin-ui's sidebar links to) — EntitySearchComponent.runSearch() always builds
// the forward-to-results URL from `context().name`, never whatever route alias the user arrived
// by (confirmed live: visiting /listings and clicking Search still lands on /carListings/
// results). Aliasing this app's own default to the same canonical name avoids ever fighting
// that, rather than trying to make search-crudstone preserve an alias it has no concept of.
export const routes: Routes = [
  {path: '', redirectTo: 'carListings', pathMatch: 'full'},
  {path: ':entity/results', component: EntitySearchResultsPageComponent},
  {path: ':entity', component: EntitySearchPageComponent},
];
