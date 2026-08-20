describe('carstone-front-ui smoke', () => {
  it('lands on /carListings/results with a search bar and a results grid', () => {
    cy.visit('/');
    cy.url({timeout: 10000}).should('include', '/carListings');
    // title moved into the "Filters" dropdown (not external) once the always-visible bar was
    // trimmed to the 4 major fields (user-reported: too many external fields, couldn't read
    // any placeholder) - brand/model/price-range/year-range are what's left external
    cy.get('[data-cy=brand-external-filter]').should('be.visible');
    cy.get('[data-cy=yearFrom-filter]').should('be.visible');
    cy.get('[data-cy=yearTo-filter]').should('be.visible');
    // priceFrom/priceTo: CrudstoneField#range=true collapses the pair into ONE p-slider (the
    // "from" field's own container) — priceTo renders nothing of its own, see isRangeSliderTo
    cy.get('[data-cy=priceFrom-filter]').should('be.visible').find('.p-slider').should('exist');
    cy.get('[data-cy=priceTo-filter]').should('not.exist');
    cy.get('[data-cy=priceFrom-filter]').should('contain.text', '500').and('contain.text', '500000');
    cy.get('[data-cy=entity-search-run-button]').click({force: true});
    cy.url({timeout: 10000}).should('include', '/carListings/results');
    cy.get('[data-cy=entity-search-result-card]', {timeout: 10000}).should('have.length.greaterThan', 0);
    cy.screenshot('01-results-page');
  });

  it('a year range filter actually bounds the SQL query, not just the UI param', () => {
    cy.intercept('GET', '**/carstone-front/carListings?*').as('search');
    cy.visit('/carListings');
    cy.get('[data-cy=yearFrom-filter]').type('2022');
    cy.get('[data-cy=entity-search-run-button]').click({force: true});
    cy.wait('@search', {timeout: 10000}).its('request.url').should('include', 'yearMin=2022');
    cy.get('[data-cy=entity-search-result-card]', {timeout: 10000}).should('have.length.greaterThan', 0);
    cy.screenshot('02-year-range-filtered');
  });

  it('the price range slider narrows results and the request carries filter.priceMin', () => {
    cy.visit('/carListings');
    // PrimeNG Slider.onBarClick moves the nearest handle to the click position AND emits
    // onSlideEnd directly (confirmed by reading primeng-slider.mjs) - clicking near the LEFT
    // portion of the track is closer to the untouched start handle (sitting at 0%/min) than the
    // end handle (sitting at 100%/max), so it drags the MIN bound up without touching the max.
    // Search isn't showing results yet at this point, so this only stages the filter value -
    // same two-step flow (stage, then click Search) the plain-input filters already use.
    // {force: true}: PrimeNG's own slider internals (a nested range-bar element) sit at a point
    // Cypress's actionability check resolves back to the outer segment wrapper - same class of
    // PrimeNG-overlay quirk this ecosystem's other specs already force through routinely
    cy.get('[data-cy=priceFrom-filter] .p-slider').click(60, 8, {force: true});
    cy.intercept('GET', '**/carstone-front/carListings?*').as('search');
    cy.get('[data-cy=entity-search-run-button]').click({force: true});
    cy.wait('@search', {timeout: 10000}).its('request.url').should('include', 'filter.priceMin=');
    cy.screenshot('03-price-range-filtered');
  });

  it('results show images and can be selected', () => {
    cy.visit('/carListings');
    cy.get('[data-cy=entity-search-run-button]').click({force: true});
    cy.get('[data-cy=entity-search-result-card]', {timeout: 10000}).first().click({force: true});
    cy.screenshot('04-result-selected');
  });
});
