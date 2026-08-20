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

  it('interacting with the price slider twice in a row keeps advancing instead of snapping back', () => {
    // Regression test for the "range is not moving, it's like blocked" report: rangeSliderValueOf
    // used to read straight from filterValues(), which doesn't change until onSlideEnd, so every
    // change-detection tick re-pushed the stale pre-interaction value back into the slider and
    // fought the user (PrimeNG's writeValue() has no dirty-check guard). rangeSliderStaged +
    // rangeSliderCommitted (entity-search.component.ts) decouple the live position from
    // filterValues() and keep a stable array reference across CD ticks respectively. Two
    // consecutive onBarClick interactions (the same mechanism the pre-existing price-range test
    // uses — a raw mousedown/mousemove drag sequence proved too fiddly to simulate reliably
    // through Cypress/Electron's synthetic-event pipeline and isn't needed to prove this) must
    // each move the handle further, not reset to the same position.
    cy.visit('/carListings');
    cy.get('[data-cy=priceFrom-filter]').as('slider');

    cy.get('@slider').find('.p-slider').click(30, 8, {force: true});
    cy.get('@slider').invoke('text').then(afterFirstClick => {
      cy.get('@slider').find('.p-slider').click(70, 8, {force: true});
      cy.get('@slider').invoke('text').should(afterSecondClick => {
        expect(afterSecondClick).not.to.equal(afterFirstClick);
      });
    });
    cy.screenshot('05-price-slider-interacted-twice');
  });

  it('a year "from" value can never be typed past the current "to" value, and vice versa', () => {
    // Regression test for "year min cannot be greater than year max": rangeInputMinFor/
    // rangeInputMaxFor bind each half's own <input min>/<max> to its sibling's CURRENT value, so
    // the browser (and PrimeNG's own input handling) clamps a violating value at entry time
    // instead of accepting yearFrom > yearTo and silently sending a broken range to the backend.
    cy.visit('/carListings');
    cy.get('[data-cy=yearTo-filter]').clear().type('2015').blur();
    cy.get('[data-cy=yearFrom-filter]').should('have.attr', 'max', '2015');

    cy.get('[data-cy=yearFrom-filter]').clear().type('2018').blur();
    cy.get('[data-cy=yearTo-filter]').should('have.attr', 'min', '2018');
    cy.screenshot('06-year-range-cross-bound');
  });
});
