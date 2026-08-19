// Everything routes through carstone-gateway (:9220), not the backend directly — that's the
// point of building one. gateway.services.carstone-admin.base-url=http://localhost:9200 on the
// gateway side is what makes this prefix resolve.
export const environment = {
  name: 'dev',
  production: false,
  apiUrl: 'http://localhost:9220/carstone-admin/',
  contextUrl: 'http://localhost:9220/carstone-admin/context/',
  sidebarUrl: 'http://localhost:9220/carstone-admin/sidebar/',
  // NOT the gateway/API prefix: this is what SidebarComponent.linkHref builds a node's href
  // from (crudstoneUrl + node.path) — in sidebar-crudstone's OWN demo, that's deliberately a
  // DIFFERENT app's origin (crudstoneUrl: 'http://localhost:5900/', dynamic-crud's own demo,
  // a separate Angular app the sidebar links OUT to). This app combines crudstone (tables) and
  // sidebarcrudstone (nav) into ONE app sharing one router, so the sidebar's own links must
  // point back at THIS app's own origin instead — otherwise clicking a sidebar link navigates
  // the browser to the gateway's raw JSON API endpoint instead of this app's own /brands route
  // (confirmed live: caught by a Cypress spec hanging on page load after clicking "Brands").
  crudstoneUrl: 'http://localhost:5910/',
};
