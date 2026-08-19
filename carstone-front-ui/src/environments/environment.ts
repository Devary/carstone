// Routes through carstone-gateway (:9220), same as carstone-admin-ui — never the backend
// directly. gateway.services.carstone-front.base-url=http://localhost:9210 on the gateway side
// is what makes this prefix resolve.
export const environment = {
  name: 'dev',
  production: false,
  apiUrl: 'http://localhost:9220/carstone-front/',
  contextUrl: 'http://localhost:9220/carstone-front/context/',
};
