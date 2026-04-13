import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import svgr from 'vite-plugin-svgr';

export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: ['babel-plugin-react-compiler'],
      },
    }),
    // SVG files imported with ?react are transformed into React components.
    // Plain ?url / asset imports remain unaffected.
    svgr(),
  ],
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
    fs: {
      // Allow the dev server to serve files from the parent directory so that
      // import.meta.glob in src/data/helpContent.ts can resolve
      // ../../../src/main/resources/help/*.md at dev time.
      // This restriction does not apply to production builds (Rollup resolves
      // glob imports statically at build time with no path restrictions).
      allow: ['..'],
    },
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

