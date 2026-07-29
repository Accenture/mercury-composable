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

`npm run release` cleans, builds, and deploys `dist/` into
`../src/main/resources/public`, which is the checked-in bundle served by the Java app.

## Working Rules

- Treat backend WebSocket session state as authoritative; frontend state mirrors it.
- Keep imperative transport operations distinct from reactive message/slot state.
- Do not let command-sending effects depend on callback identities that change with message lists.
- Prefer typed protocol events from `protocol/classifier.ts` over ad hoc message parsing in components.
- Add focused regression tests for command-loop, session, and auto-refresh fixes.