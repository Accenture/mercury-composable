import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react({
      babel: {
        plugins: ['babel-plugin-react-compiler'],
      }
    })],
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    sourcemap: true,
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks: {
          // Routing
          'vendor-router': ['react-router-dom'],
          // Flow/graph renderer (largest single dependency)
          'vendor-xyflow': ['@xyflow/react'],
          // Markdown rendering + GFM plugin
          'vendor-markdown': ['react-markdown', 'remark-gfm'],
          // JSON viewer
          'vendor-json-view': ['react-json-view-lite'],
          // Resizable panels
          'vendor-panels': ['react-resizable-panels'],
        },
      },
    },
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

