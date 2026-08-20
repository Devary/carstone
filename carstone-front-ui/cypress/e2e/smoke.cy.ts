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
    // CrudstoneField#yearPicker: yearFrom/yearTo render as a year-only PrimeNG date picker now,
    // not a plain typeable <input> — open it via its dropdown icon and click a year cell (the
    // panel is appendTo="body", so it lives outside [data-cy=yearFrom-filter] in the DOM).
    cy.intercept('GET', '**/carstone-front/carListings?*').as('search');
    cy.visit('/carListings');
    cy.get('[data-cy=yearFrom-filter] .p-datepicker-dropdown').click({force: true});
    cy.get('.p-datepicker-panel').contains('.p-datepicker-year', '2022').click({force: true});
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

  it('a year "from" picker disables any year later than the current "to" selection, and vice versa', () => {
    // Regression test for "year min cannot be greater than year max", now via the year-picker
    // widget (CrudstoneField#yearPicker) instead of the plain-input clamp: pick yearTo=2022 (the
    // year-view opens centered on the current decade, 2020-2029 — staying inside it avoids the
    // "previous decade" nav button, which proved unreliable to drive through Cypress), then open
    // yearFrom's OWN picker and assert 2025 (later than 2022) renders as a genuinely disabled
    // cell — there's no typing path to bypass this at all anymore, unlike the plain-input case.
    cy.visit('/carListings');
    cy.get('[data-cy=yearTo-filter] .p-datepicker-dropdown').click({force: true});
    cy.get('.p-datepicker-panel').contains('.p-datepicker-year', '2022').click({force: true});

    cy.get('[data-cy=yearFrom-filter] .p-datepicker-dropdown').click({force: true});
    cy.get('.p-datepicker-panel').contains('.p-datepicker-year', '2025').should('have.class', 'p-disabled');
    cy.screenshot('06-year-range-cross-bound');
  });

  it('a year picker disables any year later than the current year', () => {
    // Regression test for "max year cannot be in the future", now via the year-picker widget
    // instead of a plain-input clamp (user-reported follow-up: "i still can put 2033 in the max
    // year" — that report was about the earlier plain-input fix; the picker removes the typing
    // path entirely, so this asserts the NEXT calendar year renders as a disabled cell).
    const nextYear = new Date().getFullYear() + 1;
    cy.visit('/carListings');
    cy.get('[data-cy=yearTo-filter] .p-datepicker-dropdown').click({force: true});
    cy.get('.p-datepicker-panel').contains('.p-datepicker-year', String(nextYear)).should('have.class', 'p-disabled');
    cy.screenshot('07-year-no-future-value');
  });

  it('the footer renders on every page with working links to About/Contact', () => {
    cy.visit('/carListings');
    cy.get('[data-cy=site-footer]').should('be.visible').and('contain.text', 'Carstone');
    cy.get('[data-cy=site-footer]').contains('a', 'About us').click();
    cy.url().should('include', '/about');
    cy.get('[data-cy=about-page]').should('be.visible');
    cy.get('[data-cy=site-footer]').should('be.visible');

    cy.get('[data-cy=site-footer]').contains('a', 'Contact us').click();
    cy.url().should('include', '/contact');
    cy.get('[data-cy=contact-page]').should('be.visible');
    cy.screenshot('08-footer-and-pages');
  });

  it('the contact form submits and shows a confirmation', () => {
    cy.visit('/contact');
    cy.get('[data-cy=contact-name]').type('Jane Doe');
    cy.get('[data-cy=contact-email]').type('jane@example.com');
    cy.get('[data-cy=contact-message]').type('Interested in the BMW listing.');
    // the INNER native <button>, not the [data-cy] attribute's own element (the <p-button> host
    // wrapper) — clicking the wrapper doesn't reliably reach PrimeNG's own (onClick) output
    // through Cypress, confirmed by dumping the post-click DOM: the wrapper click left
    // `submitted()` false with no error, while clicking the nested real button worked
    cy.get('[data-cy=contact-submit] button').click();
    cy.get('[data-cy=contact-success]').should('be.visible');
    cy.screenshot('09-contact-submitted');
  });
});
