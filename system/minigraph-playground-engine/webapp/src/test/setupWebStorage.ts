/**
 * Vitest setup file: happy-dom tests get happy-dom's web storage on every Node version.
 *
 * Node 25 turned its own Web Storage API on by default. Without `--localstorage-file`, Node's
 * `localStorage` global is unusable (on Node 26 it reads as undefined and prints an
 * ExperimentalWarning), and its `sessionStorage` is Node's in-memory store. Vitest 4's happy-dom
 * environment copies a window property onto the global only when Node does not define it
 * already, or when the name is on Vitest's own key list: `Storage` is on that list,
 * `localStorage` and `sessionStorage` are not. So on Node 25+ Node's two globals stay, and
 * `localStorage.clear()` throws. Node 22 defines neither, so happy-dom's storage arrives there
 * by itself.
 *
 * In the happy-dom environment this file installs a fresh happy-dom `Storage` under both names,
 * which is what happy-dom gives every window (and each test file gets a new window), so the
 * tests see the same storage on every Node version. The globals are replaced without being
 * read, because reading Node's `localStorage` is what prints the warning. Files in the `node`
 * environment are left alone. A setup file rather than `--no-experimental-webstorage`: Node
 * 26 already lists that flag as an alias of `--webstorage`, and a Node that predates the flag
 * refuses to start with it.
 *
 * Vitest 5 fixes this upstream (vitest-dev/vitest#10293 adds both names to its key list; the
 * 4.x report, vitest-dev/vitest#10867, was closed as not planned), so this file can go when the
 * webapp moves to Vitest 5.
 */
if ('happyDOM' in globalThis) {
  for (const name of ['localStorage', 'sessionStorage'] as const) {
    Object.defineProperty(globalThis, name, {
      value: new Storage(),
      configurable: true,
      enumerable: true,
      writable: true,
    });
  }
}
