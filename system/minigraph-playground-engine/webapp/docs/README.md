# MiniGraph Playground webapp — technical documentation

The Playground is the browser UI of the MiniGraph engine: a console for the engine's command
grammar, a live graph canvas with in-place authoring, a help browser, a workspace clipboard and
collaborative sessions. This folder documents the webapp as it is in the code, for developers and
for AI agents. The source under `../src/` is the truth; these pages are the map.

| Page | Read it when you need to know |
|---|---|
| [architecture.md](architecture.md) | How the app is layered (entry and routing, WebSocket context, protocol kernel, hooks, components), who owns which state, and the design decisions that must survive a change |
| [protocol.md](protocol.md) | The contract with the engine: the WebSocket session, the console text protocol and how every backend line is classified into typed events, the REST endpoints and the commands the UI sends — identical for the Java and the Rust engine |
| [components.md](components.md) | The React components: props, behaviour, keyboard shortcuts, and which hooks and events each one uses |
| [graph-view.md](graph-view.md) | The graph canvas: the data model, the layout algorithm and edge routing, node styling, selection, context menus, and the authoring layer that turns UI gestures into console commands |
| [hooks-and-state.md](hooks-and-state.md) | Every hook, the run and session state machines, the clipboard subsystem, and the `localStorage` / IndexedDB keys |
| [build-test-deploy.md](build-test-deploy.md) | Toolchain, scripts, the two deploy targets (Java and Rust engine), why the help pages are part of the bundle, how to verify a build, testing conventions |
| [extending.md](extending.md) | Recipes for the common changes (a new playground, tab, protocol event, node type, authoring action or help topic) and the pitfalls that have bitten before |

## Orientation for an AI agent

1. Read `architecture.md` first, then the page for the layer you are changing.
2. The engine is the source of truth for graph and session state; the UI mirrors acknowledged
   backend lines. Before adding UI logic that infers state, check whether the backend already says
   it in a line the classifier can parse (`protocol.md`).
3. Every UI mutation is a console command the user could have typed (`graph-view.md`,
   "Authoring"). Keep it that way: the engine stays a lightweight command executor.
4. A change is not done until `npm test` passes and the bundle is released to both engines
   (`build-test-deploy.md`). Help pages are compiled into the bundle.
5. Session memory for this webapp is kept at the repository root (`memory/`), not here.

## Scope of the two playgrounds

`src/config/playgrounds.ts` declares two playgrounds. **Minigraph** (`/`, WebSocket
`/ws/graph/playground`) has everything: graph tabs, authoring, run controls, help, clipboard and
sessions. **JSON-Path** (`/json-path`, WebSocket `/ws/json/path`) is a payload tool: a payload
editor, the `load` command and a JSON-Path overview in its help panel. Feature flags in the config
(`supportsAuthoring`, `supportsGraphRun`, `supportsHelp`, `supportsClipboard`,
`supportsSessionCollaboration`, `supportsUpload`) switch each capability per playground.
