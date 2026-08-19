import {defineConfig} from 'cypress';

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:5911',
    supportFile: false,
    pageLoadTimeout: 120000,
  },
});
