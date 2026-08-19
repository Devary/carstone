describe('carstone-admin-ui smoke', () => {
  it('loads, shows the sidebar, and the listings table', () => {
    cy.intercept('GET', '**/carstone-admin/listings*').as('loadListings');
    cy.visit('/');
    cy.wait('@loadListings');
    cy.get('sb-sidebar', {timeout: 10000}).should('be.visible');
    cy.contains('Marketplace').should('be.visible');
    cy.contains('Listings').should('be.visible');
    cy.get('table').should('be.visible');
    cy.screenshot('01-listings-table');
  });

  it('renders numeric fields as real inputs in the New Listing dialog (createEditType check)', () => {
    cy.intercept('GET', '**/carstone-admin/listings*').as('loadListings');
    cy.visit('/listings');
    cy.wait('@loadListings');
    cy.get('[data-cy=lock-toggle-button] button').click({force: true});
    cy.contains('button', 'Yes, unlock').click({force: true});
    cy.get('[data-cy=new-button] button').click({force: true});
    cy.get('.p-dialog').should('be.visible');
    cy.screenshot('02-new-listing-dialog');
    // year/price/mileage/power have createEditType="inputText" — confirms the admin-form gap
    // (silently missing without that override) is genuinely closed, not just compiled
    cy.get('input#year').should('be.visible');
    cy.get('input#price').should('be.visible');
    cy.get('input#mileage').should('be.visible');
    cy.get('input#power').should('be.visible');
    // leave the dialog closed before the next test — a dangling open dialog otherwise blocks the
    // next test's own cy.visit() navigation (confirmed live: without this, the very next test's
    // page load hangs past a 120s timeout). Form is still clean (nothing typed in), so Cancel
    // closes it silently per the CRUD-modal pattern — no confirm to click through.
    cy.get('[data-cy=cancel-button] button').click({force: true});
    cy.get('.p-dialog').should('not.exist');
  });

  it('navigates to Brands via the sidebar', () => {
    cy.visit('/');
    cy.contains('a', 'Brands').click({force: true});
    cy.url().should('include', '/brands');
    cy.contains('BMW').should('be.visible');
    cy.screenshot('03-brands-table');
  });
});
