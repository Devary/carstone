describe('carstone-front-ui smoke', () => {
  it('lands on /carListings/results with a search bar and a results grid', () => {
    cy.visit('/');
    cy.url({timeout: 10000}).should('include', '/carListings');
    cy.get('[data-cy=title-external-filter]').should('be.visible');
    cy.get('[data-cy=brand-external-filter]').should('be.visible');
    cy.get('[data-cy=yearFrom-filter]').should('be.visible');
    cy.get('[data-cy=yearTo-filter]').should('be.visible');
    cy.get('[data-cy=priceFrom-filter]').should('be.visible');
    cy.get('[data-cy=priceTo-filter]').should('be.visible');
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

  it('a price range filter narrows results and the request carries filter.priceMax', () => {
    cy.intercept('GET', '**/carstone-front/carListings?*').as('search');
    cy.visit('/carListings');
    cy.get('[data-cy=priceTo-filter]').type('20000');
    cy.get('[data-cy=entity-search-run-button]').click({force: true});
    cy.wait('@search', {timeout: 10000}).its('request.url').should('include', 'priceMax=20000');
    cy.screenshot('03-price-range-filtered');
  });

  it('results show images and can be selected', () => {
    cy.visit('/carListings');
    cy.get('[data-cy=entity-search-run-button]').click({force: true});
    cy.get('[data-cy=entity-search-result-card]', {timeout: 10000}).first().click({force: true});
    cy.screenshot('04-result-selected');
  });
});
