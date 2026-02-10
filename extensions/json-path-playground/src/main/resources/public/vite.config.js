import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: true,
  },
  server: {
    port: 3000,
    proxy: {
      // Proxy WebSocket connections to the Spring Boot backend
      '/ws': {
        target: 'ws://localhost:8085',
        ws: true,
        changeOrigin: true,
      },
      // Proxy API endpoints to the Spring Boot backend
      '/info': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
      '/health': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
      '/env': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
      '/api': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
    },
  },
});

