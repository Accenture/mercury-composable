import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    include: ['src/**/*.test.ts', 'src/**/*.test.tsx'],
    environment: 'node',
    globals: true,
    // happy-dom's localStorage and sessionStorage on Node 25+ as well (see the file).
    setupFiles: ['./src/test/setupWebStorage.ts'],
  },
});
