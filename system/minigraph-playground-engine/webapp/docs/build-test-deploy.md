# Build, test and deploy

The webapp is built once and deployed to **two engines**: the Java engine in this repository and the
Rust engine in the sibling `mercury` repository. Both serve the same bundle. This page is the
operational contract: what the scripts do, where the output goes, and how to prove a build is what
you think it is.

## Toolchain

| Tool | Version | Notes |
|---|---|---|
| Node.js | 22.12 or newer | `react-router` 8.3 declares `node >= 22.22`, so `npm ci` on 22.12 prints one `EBADENGINE` warning; the build and the tests are unaffected. Node 25+ works too (see [Testing](#testing)). |
| npm | 11 | `npm ci` installs exactly the locked tree (`package-lock.json`, 269 packages). Never `npm install` in a release: it may rewrite the lockfile. |
| Vite | 8 (Rolldown) | Configured in `vite.config.ts`. |
| TypeScript | 5.9 | `npm run typecheck` is `tsc --noEmit` and runs before every build. |

Run every command from `system/minigraph-playground-engine/webapp/`.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Vite dev server on port **3000**; proxies `/ws` (WebSocket), `/api`, `/info`, `/health` and `/env` to an engine on port **8085**. Start a playground example first (Java: `examples/minigraph-playground`; Rust: `cargo run -p minigraph-playground` in the `mercury` repo). |
| `npm run typecheck` | `tsc --noEmit`. |
| `npm run build` | typecheck, then `vite build` into `dist/` (`emptyOutDir`). Produces hashed assets and `dist/index.html`. |
| `npm test` / `npm run test:watch` | Vitest, once or in watch mode. |
| `npm run clean` / `npm run clean:rust` | Remove exactly what a deploy writes for the Java or the Rust target. |
| `npm run deploy` / `npm run deploy:rust` | Copy `dist/` into the Java or the Rust engine's resources (fails with a clear message when `dist/` is missing or the target repo is not found). |
| `npm run release` | `clean` + `build` + `deploy` — the Java engine (the default). |
| `npm run release:rust` | `clean:rust` + `build` + `deploy:rust` — the Rust engine. |
| `npm run release:all` | Both targets from **one** build. Use this after any change to the sources or the help pages when both repositories are checked out. |

The target resolution lives in [`scripts/targets.js`](../scripts/targets.js); `clean.js` and
`deploy.js` take the target name as their first argument (`java` when absent).

## Deploy layout

A deploy writes the bundle in two parts and, for the Rust target, mirrors the help pages:

| Output | Java target (this repo) | Rust target (`mercury` repo) |
|---|---|---|
| Hashed assets (`index-*.js`, `vendor-*.js`, `*.css`, source maps) | `src/main/resources/public/assets/` | `crates/knowledge-graph/resources/public/assets/` |
| Entry page (`dist/index.html`) | `src/main/resources/template/playground.html` | `crates/knowledge-graph/resources/template/playground.html` |
| Help pages (`help *.md`) | *(the source)* `src/main/resources/help/` | mirrored to `crates/knowledge-graph/resources/help/` |

Why the entry page lives outside the static folder: the engine's `get.index.html` function serves
`template/playground.html` only when `app.env=dev`. Without dev mode the home page is the plain
"MiniGraph Service" page in `public/index.html`, so a production deployment never shows the
Playground. `public/index.html` is never touched by the scripts.

The Rust repo is expected **beside** this one under the same parent folder
(`…/sandbox/mercury-composable` and `…/sandbox/mercury`). Set `MERCURY_RUST_REPO` to point
elsewhere, for example at a git worktree:

```bash
MERCURY_RUST_REPO=/path/to/mercury-worktree npm run release:rust
```

The Rust repo ignores the source maps (`crates/knowledge-graph/resources/public/**/*.map` in its
`.gitignore`); this repo tracks them. Everything else that a deploy writes is committed in both
repos, so a fresh clone of either serves the Playground without Node.

## The help pages are part of the bundle

`src/data/helpContent.ts` compiles every `src/main/resources/help/*.md` into the bundle at build
time (`import.meta.glob` with `?raw`, eager). The same files are read by the engines at run time to
answer the console `help` command, and the Rust engine reads its mirrored copy. Consequences:

- **A help edit is not done until the bundle is rebuilt and committed** — in both repos
  (`npm run release:all`). Nothing in CI builds the webapp, so a stale bundle is silent: the
  Playground kept showing five `graph.math` statement types for a week after the help listed seven
  (fixed in PR #491).
- The help is single-sourced here. Do not edit the copy under the Rust repo; the next deploy
  overwrites it.
- Any webapp source edit needs a release too, comments included: the source map embeds every
  source file, so a comment-only change alters the tracked map while the chunk and its hash stay the
  same (PR #492).

## Reproducibility and how to verify a build

The build is deterministic for the same sources and lockfile: rebuilding unchanged sources
reproduces every committed file byte for byte. Use that to attribute a diff before committing:

```bash
npm ci && npm run build
for f in dist/assets/*; do cmp -s "$f" "../src/main/resources/public/assets/$(basename "$f")" \
  && echo "same  $(basename "$f")" || echo "DIFF  $(basename "$f")"; done
cmp dist/index.html ../src/main/resources/template/playground.html && echo "same  playground.html"
```

Expected churn by kind of change:

| Change | Files that change |
|---|---|
| Help page text | `index-<hash>.js`, its `.map`, `template/playground.html` (new hash) |
| Application source | `index-<hash>.js`, its `.map`, `playground.html`; a vendor chunk only when tree-shaking changes (PR #493 shrank `vendor-panels`) |
| Comment-only source edit | the `.map` only |
| Dependency bump | the matching `vendor-*` chunk and map (`manualChunks` in `vite.config.ts` keeps react-router, @xyflow/react, react-markdown + remark-gfm, react-json-view-lite and react-resizable-panels in their own chunks) |

Live check: run the engine's playground example, open `http://127.0.0.1:8085`, and confirm the page
references the new chunk (`curl -s http://127.0.0.1:8085/ | grep index-`); older hashes answer 404.

## Testing

`npm test` runs Vitest once (53 files, 416 tests as of 2026-10-06). Conventions:

- The default environment is `node` (`vitest.config.ts`); component and hook tests that need a DOM
  declare `// @vitest-environment happy-dom` at the top of the file.
- `src/test/setupWebStorage.ts` (a `setupFiles` entry) installs happy-dom's `localStorage` and
  `sessionStorage` on every Node version. Node 25 turned its own Web Storage API on by default, and
  Vitest 4 keeps Node's unusable globals in happy-dom files; the setup file replaces both without
  reading them. It can go when the webapp moves to Vitest 5 (vitest-dev/vitest#10293).
- The protocol classifier is tested from JSON fixtures in `src/protocol/__tests__/fixtures/`: each
  fixture lists raw backend strings and the event kinds they must produce. Add a fixture entry when
  you add a classifier rule.
- Tests that import `src/data/helpContent.ts` run the real glob (Vite resolves it under Vitest), so
  `helpContent.test.ts` asserts on the shipped help text.

Engine-side checks that read the help pages or the entry page, worth running after a release:

| Engine | Check |
|---|---|
| Java | `python3 scripts/check-minigraph-grammar.py` (every catalog command and skill has its help file; runs in the `docs` workflow) |
| Java | `mvn -o test -pl system/minigraph-playground-engine -Dtest=ClaimMathExpressionDialectTest,PlaygroundTest,RestEndpointTest,GraphTaskTest,GraphSuspendResumeTest` |
| Rust | `cargo test -p mercury-knowledge-graph -p minigraph-playground` (`tests/playground.rs` expects `help connect` and `describe skill graph.math` to answer from the mirrored pages) |

## Troubleshooting

- `Resources folder of target 'rust' not found` — the `mercury` repo is not beside this one; set
  `MERCURY_RUST_REPO`.
- `No build output at …/dist` — run `npm run build` (or the `release` script) before `deploy`.
- `npm ci` prints `EBADENGINE` for `react-router` — harmless on Node 22.12; silent on Node 22.22+.
- The Playground shows old help after a help edit — the bundle was not released; run
  `npm run release:all` and commit the hashed files in both repos.
- The dev server shows an empty Help panel — `server.fs.allow` in `vite.config.ts` must keep
  allowing the parent folder, where the help glob resolves.
