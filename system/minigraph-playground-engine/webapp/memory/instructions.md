# MiniGraph Webapp Memory Instructions

## Scope

This memory bank is for the React/Vite MiniGraph Playground webapp under
`system/minigraph-playground-engine/webapp`.

Record here:

- Webapp UI architecture decisions and regressions.
- WebSocket/session/command-input behavior.
- Protocol classification and frontend state-management conventions.
- Bundle build/deploy workflow facts for the Java-served assets.
- UI implications of MiniGraph engine features, when the detail is frontend-owned.

Record in root `memory/` instead:

- Java engine/framework contracts.
- Event Script, platform-core, or Mercury-wide architecture decisions.
- Cross-language parity facts.
- Any webapp change that creates or changes a backend public contract.

## Build And Test

From this directory:

```bash
npm test -- --run
npm test -- --run src/session/__tests__/useSessionCollaboration.test.ts
npm run build
npm run release
```

`npm run release` cleans, builds, and deploys `dist/`. The bundle (the hashed assets and their source maps) goes to
`../src/main/resources/public/` (under `assets/`), the checked-in static content the Java app serves. Only `index.html`
goes elsewhere: it becomes `../src/main/resources/template/playground.html`, served only when `app.env=dev`
(`public/index.html` is the plain home page and is never touched).

The help pages (`../src/main/resources/help/*.md`) are compiled into the bundle (`src/data/helpContent.ts`,
`import.meta.glob`), so a help edit is not done until `npm run release` has rebuilt the bundle and the new hashed files
are committed. CI does not build the webapp, so nothing else catches a stale bundle: the Playground showed no help for
`CONDITION` or `DECIMAL` after four help edits because the bundle was last regenerated before them (fixed in PR #491).

## Working Rules

- Treat backend WebSocket session state as authoritative; frontend state mirrors it.
- Keep imperative transport operations distinct from reactive message/slot state.
- Do not let command-sending effects depend on callback identities that change with message lists.
- Prefer typed protocol events from `protocol/classifier.ts` over ad hoc message parsing in components.
- Add focused regression tests for command-loop, session, and auto-refresh fixes.