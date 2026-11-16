import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the Vite server forwards /api/* to Spring Boot on port 8080,
// so the browser only ever talks to one origin and no CORS is needed.
// Set CARETRACK_API_URL to point the proxy at a different backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: process.env.CARETRACK_API_URL ?? 'http://localhost:8080',
        changeOrigin: false,
      },
    },
  },
});
