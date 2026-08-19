import {defineConfig} from 'cypress';

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:5910',
    supportFile: false,
    pageLoadTimeout: 120000,
  },
});
