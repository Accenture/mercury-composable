# Extending the webapp

Recipes for the changes that come up, each naming the files to touch and the test to add, followed
by the pitfalls that have bitten before. Finish every change with `npm test`, then release the
bundle to both engines ([build-test-deploy.md](build-test-deploy.md)).

## Recipes

### Add a playground

1. Add an entry to `PLAYGROUND_CONFIGS` in `src/config/playgrounds.ts`: `path`, `label`, `title`,
   `wsPath`, the storage keys (`storageKeyPayload`, `storageKeyHistory`, `storageKeyTab`, and
   `storageKeyHelpTopic` / `storageKeySavedGraphs` when used), the feature flags and `tabs`.
2. Nothing else: `App.tsx` generates the route, `Navigation` the menu row, `WebSocketProvider` the
   auto-connect, and the dev proxy already forwards `/ws`. Keep storage keys unique per playground.
3. Pin it in `src/config/__tests__/playgrounds.test.ts` (which flags are on, which help profile).

Sample payload buttons come from `SAMPLE_DATA` keys named `json_<label>` / `xml_<label>`.

### Add a right-panel tab

1. Extend `RightTab` in `components/RightPanel/RightPanel.tsx` and render the tab body there
   (keep it mounted and hidden like the others if it holds view state).
2. List it in the playground's `tabs`; the first entry is the default.
3. `useGraphData.normalizeRightTab` already maps a persisted value that is no longer valid back to
   the default, so a removed tab needs no migration.

### Handle a new backend line

The classifier is the only place that reads raw text, so a new line is a new rule:

1. Add the parser (a regex or exact match) to `src/utils/messageParser.ts` (or
   `session/sessionParser.ts`, `graphRun/graphRunProtocol.ts` for those families).
2. Add the event interface to `src/protocol/events.ts` and include it in the `ProtocolEvent`
   union; `ProtocolEventKind` follows.
3. Add the rule to `classifyMessage` in `src/protocol/classifier.ts`, in the right position:
   JSON rules return early; text rules may stack (one line can emit several kinds). If the line
   must not also render as `docs.response`, set the matching `matched…` flag used by rule 11.
4. Add a vector to the right fixture under `src/protocol/__tests__/fixtures/` (`raw`,
   `expectedKinds`, `expectedProps`, and `notExpectedKinds` when a rule must *not* fire).
5. Subscribe in the hook that owns the behaviour (`bus.on('<kind>', …)`), reading mutable state
   through refs so the subscription does not churn.

Mind the exclusions already encoded: node results require the `node ` prefix (so "Graph instance
created…" is not a mutation), echoes start with `> `, and `tryParseJSON` must keep rejecting JSON
primitives.

### Add a node type

1. `src/utils/minigraphNodeTheme.ts`: icon + label in the meta table and an accent colour.
2. `src/components/GraphView/NodeTypes.tsx`: add the type to the `nodeTypes` map (otherwise React
   Flow falls back to the default renderer with a console warning).
3. `src/components/GraphView/GraphMinimap.tsx`: the minimap palette (a second copy of the accents).
4. If the type must be laid out specially, `computeLayout`'s classification in
   `utils/graphTransformer.ts` (today only Dictionary / Provider / Module / Entity are special, and
   only when unconnected).

### Add an authoring action

Every action is a console command the user could type, plus its compensation:

1. Build the text in `src/graphActions/minigraphCommandBuilder.ts` and its rules in
   `graphActions/validation.ts` (the builder re-validates and size-checks).
2. Teach `useGraphAuthoring` the new `action` and how its result is matched: the engine's reply
   must be parsed by `parseNodeActionTextResult` (add the pattern and the `action` value) and
   matched by alias.
3. Add the inverse to `graphActions/undoCommands.ts` and push it from `Playground.tsx`'s
   `handleAuthoringAccepted` *after* the backend accepts; snapshot what the inverse needs when the
   gesture starts.
4. Expose the gesture in `GraphView` (a context-menu item, a key) through a new callback prop.
5. Tests: the builder (`minigraphCommandBuilder.test.ts`), validation, the undo recipe, and a
   fixture vector for the reply.

### Add a help topic

1. Drop `help <topic>.md` in `src/main/resources/help/` (the engine module). Categories are
   derived from the name: `graph-*` → Graph Skills, `tutorial N` → Tutorials (numeric order), the
   `INSTANCE_MODEL_TOPICS` set in `src/data/helpContent.ts` → Instance Model, everything else →
   Graph Model. Add an instance-model command to that set.
2. A new *command* also needs its entry in `docs/guides/knowledge-graph/minigraph-commands.json`
   (the `docs` workflow's `check-minigraph-grammar.py` cross-checks catalog ↔ help files) and in the
   console palette (`COMMAND_QUICKSTART` / `COMMAND_SUGGESTIONS` in `utils/commandSuggestions.ts`).
3. `npm run release:all`: the page is compiled into the bundle and mirrored to the Rust engine.

### Refresh the graph after a new engine mutation

`detectMutation` in `utils/messageParser.ts` decides what re-fetches the live graph: add the
reply's wording (it must start with `node ` or contain ` -> ` and `removed`, or be the import
line) and a vector in `graph-mutations.json`.

## Pitfalls

1. **Keep `WebSocketProvider` above `<BrowserRouter>`.** Sockets are keyed by `wsPath` and survive
   navigation only because the provider does not remount.
2. **Socket instances, ping timers and id counters stay in refs, never in React state** — a render
   per frame would follow.
3. **Message ids are not array indices.** The log is capped at 200 and evicts from the front;
   address rows by `id` (`classificationMap`, `connectionEpoch`).
4. **The kernel emits each message once.** Its watermark initialises on mount before the emitting
   effect; a hook that mounts later must read the `classificationMap` backlog itself with its own
   watermark and dedupe by `kind:msgId` (the session hook shows how).
5. **`classificationMap` has a new identity on every message.** Never put it in an effect's
   dependency list unless the effect is meant to run per message.
6. **`sendRawText` for silent commands, `sendCommand` for typed ones.** Only the latter records
   history and echoes locally handled commands; the engine echoes everything it receives.
7. **No correlation ids.** After a graph mutation an outstanding command's reply can no longer be
   trusted: quarantine (`outcome-uncertain`) rather than complete it.
8. **The live graph comes from `GET /api/graph/session/{id}`.** `refetchGraph` is stable (empty
   deps) and reads the path through a ref; do not add it to dependency lists expecting re-fetches.
9. **`payloadOverride` must be cleared on any manual edit** of the payload editor, and a
   cross-playground payload is never written to `localStorage`.
10. **Large payloads never go to `localStorage`**; they are appended to the console from the
    `/api/inspect/…` fetch, one at a time.
11. **Invitation and large-payload console rows are not activatable** (no nested interactive
    controls); the graph-link activation is a separate code path.
12. **Only `minigraphCommandBuilder.ts` produces command text**, and undo goes through it too;
    `'''` cannot be escaped in the engine grammar, so values containing it are refused.
13. **Result matching is text-based and alias-scoped.** A generic `ERROR:` matches any single
    pending submit and never a batch; batches end by timeout.
14. **`delete connection a and b` wipes the pair in both directions.** Any partial removal must
    reconnect the survivors (`planConnectionRemoval`) and so must its undo.
15. **Undo is paced** (next `graph.mutation` or 2.5 s per command) because the engine was seen
    answering a `connect` before a slow multi-line `create` finished; undo is last-write-wins in a
    collaborative session.
16. **Never bookmark a save while disconnected** — a bookmark without a server-side export file
    cannot be loaded; wait for the `graph.exported` line.
17. **Escape and the close button are blocked while a submit is in flight**, in every in-place
    editor; keep that when adding one.
18. **A node type must be in the `nodeTypes` map**, and nodes keep the `nokey` class so modifier
    clicks reach React Flow's selection while Shift+drag still draws a box.
19. **The measured relayout runs once per (graphData, mode)** and checks provenance by object
    identity (`properties`), which assumes a fetched graph is freshly parsed. Mutating `graphData`
    in place defeats it.
20. **`panelLayoutKey` describes panel visibility only**; a key that changes on separator drags
    re-fits the canvas on every drag.
21. **Transform errors are reported from an effect, never inside `useMemo`**; render-time throws
    reach the keyed `GraphViewErrorBoundary`.
22. **Help pages are compiled in.** The panel works offline, but a help edit without a bundle
    release changes nothing a user sees, and the Rust engine needs the mirrored files.
23. **Two `useLocalStorage` instances in one tab do not notify each other**; share the state
    through props or the owning hook instead of reading the same key twice.
24. **Session ids are validated (`^ws-\d+-\d+$`) and echoes are ignored** before any session text
    can drive the menu; reuse `sessionParser.ts` rather than matching text elsewhere.
25. **Workspace clipboard uniqueness is by alias across all playgrounds**, and a schema bump or a
    failed open discards the stored clips by design.
