// @ts-check
import { defineConfig } from 'astro/config';

export default defineConfig({
  site: "${PRIMARY_URL}",
  vite: {
    server: {
      allowedHosts: ["." + process.env.DDEV_TLD],
      cors: { origin: process.env.DDEV_PRIMARY_URL },
    },
  },
});
