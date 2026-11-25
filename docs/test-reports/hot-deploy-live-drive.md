---
title: Test Report — Hot deployment of graph sets, live on both engines
summary: Permanent record of the first live drive of RFC-0017 (hot deploy and undeploy of graph sets) - the Java and the
  Rust Playground engines on one hot folder, negative controls first, the Playground's Deploy view in dev mode, the two
  starter-template scripts, a replace, an undeploy, a restart, a set that reaches a language-pack sidecar through
  server.url, and the two engines converging on each other's deployments with no poll; every scenario held, three
  documentation corrections and four follow-ups found.
layer: reference
audience: [developer, architect]
keywords: [hot deployment, graph set, deploy, undeploy, RFC-0017, server.url, Event-over-HTTP, Playground, Deploy view,
  deploy-graph-set.mjs, X-Graph-Set-SHA256, record, reconcile, two instances, test report]
---

# Test Report — Hot deployment of graph sets, live on both engines

*The first live drive of RFC-0017 (`draft-design-specs/hot-deploy-undeploy-graph-sets.md`, work packages WP5 and WP6):
the Java engine's Playground example (`examples/minigraph-playground`, 4.12.30 at `4c249f88f`) and the Rust port's
(`examples/minigraph-playground` of the Rust repository, 4.12.30 at `1b8590fd`, Increment 214) on one hot folder,
conducted 2026-11-25 (UTC+8). Until this drive the lane had been proven in each engine's own tests: the twelve engine
steps and the four REST steps of `HotDeploymentTest`, `HotDeployEndpointTest` and the Rust `tests/hot_deployment.rs`,
the Deploy view against a mocked `fetch`, the scripts never run against an engine. This is the end-to-end test the RFC's
promotion to an ADR was held on.*

## Result in brief

- **Every scenario held, on both engines.** A set uploaded to one engine was served by it at once, by the other engine at
  its next request with no restart and no poll, replaced as a unit three times, undeployed from either side and gone on
  the other at the next lookup, restored by a restart from its record, and the records stayed the only door: a package
  copied into the folder by hand was ignored on every path. The Deploy view, the two scripts and plain `curl` are three
  clients of the same three entries, and each saw the same answers.
- **The negative controls refused as designed, with one correction to the design's wording.** A bundled id answered 409
  with the owner named, a set not named after its main graph 400, a truncated or non-package body 400 with the codec's
  reason, a wrong digest 400 with the body's digest, an empty body 400, an undeploy of a set the folder lacks 404, a bad
  set name 400 — and no refusal left a file. But **a switch that is off answers 404, not 503**: the function does not
  register, REST automation skips the entry at startup (`Skip [POST] /api/graph-set/deploy - Service deploy.graph.set
  not available`), and the URL answers `Resource not found` as any unknown path does. The 503 the design promised is the
  answer when a switch is **on** and the engine turned hot deployment off for the run because its folder is refused
  (finding 1). The Deploy view already treated both as "off"; the scripts and five pages said 503 only, corrected in
  this change.
- **Bytes altered inside a well-formed package pass the strict read.** Four bytes of a provider URL zeroed inside the
  package still read as a valid set and passed the gate; the altered graph would have failed at run time on a broken
  host. The strict read refuses a *malformed* package; the `X-Graph-Set-SHA256` header is the check that the bytes are
  the ledger's, and it refused the same upload at once (finding 2). The guide now says so.
- **A task's provider through `server.url` must be a public function.** The Java engine routed a hot-deployed task to the
  Rust engine's `/api/event` exactly as the node said (the trace annotation `server.url=http://127.0.0.1:8086/api/event`),
  and the Rust engine answered 403 `demo.order.lookup is private` - as it would for its own `async.http.request`, and as
  the Java engine would: the engines publish no public function, and the Event API serves public functions only. With a
  language-pack sidecar as the provider (the Python pack's agent tools, `time.now`, on a port the static map does not
  know) the call succeeded on both engines, which is the shape the design intends (finding 3). The guide now says so.
- **Four follow-ups, none blocking:** the on-demand reconcile that registers another instance's deployment logs nothing
  (the undeploy side does); the list entry answers 200 with the default folder while hot deployment is off for the run;
  a stray `.json` in the folder draws no WARN where a stray `.pack` does; the deploy INFO line carries no caller
  address (section 7).

## What was tested

The Playground example bundles every tutorial (`tutorial-1` loose, `tutorial-2` to `-14` as the `tutorials` set under
`file:/tmp/unpack`), so the drive's set is one the example does not bundle, built from two renamed tutorials:

- **`hot-demo`**, the main graph: tutorial-10's shape, a `graph.extension` node whose target is the set's own subgraph;
- **`hot-demo-sub`**, the private subgraph: tutorial-3, an API fetcher with a dictionary over the engine's own
  `/api/mdm/profile/{id}` mock (`${rest.server.port}` resolves on the engine that registers it).

Five versions of the set, packed with `helpers/graph-packager` (`pack --set hot-demo --manifest version=…`):

| Version | Change | SHA-256 (first 12) |
| --- | --- | --- |
| 1.0.0 | the profile as the body | `fcb1c0dcc254` |
| 2.0.0 | the profile under `profile`, `version: "2"` | `df347f515cc3` |
| 3.0.0 | a `graph.task` on `demo.order.lookup` with `server.url` naming the other engine (a private function - finding 3) | `f501a9d681f0` |
| 3.1.0 | the same on `async.http.request` (private too) | `195f396e7eeb` |
| 3.2.0 | a `graph.task` on `time.now` with `server.url=http://127.0.0.1:${hot.demo.tools.port:8099}` - a sidecar the static map (which says 8089) does not know | `8a144875e10c` |

And four packages for the negative controls: the version 1 set with `tutorial-2` added (a bundled id), a set named
`hot-demo` holding `hot-other` and the subgraph (not named after its main graph), version 1 with four bytes zeroed at
offset 1200 (inside the provider URL of the subgraph), and version 1 cut 100 bytes short.

## Setup

| Part | How |
| --- | --- |
| Java engine | `examples/minigraph-playground` 4.12.30 built from `main` at `4c249f88f`, run from a copy of the jar outside `target/`, on 8085; first with the switches at their defaults, then `java -Dgraph.hot.deploy=true -Dgraph.hot.undeploy=true -jar …`; `app.env=dev`, so the Playground served the Deploy view |
| Rust engine | the Rust repository's `examples/minigraph-playground` 4.12.30 built in release mode from `main` at `1b8590fd`, run from a copy of the binary, on 8086 with `-Drest.server.port=8086 -Dlocation.graph.temp=file:/tmp/graph-rust` and the same two switches |
| Hot folder | `file:/tmp/deploy`, the default, shared by both engines; `location.graph.temp` distinct per engine; the `tutorials` set's `unpack` folder `file:/tmp/unpack` shared (written at startup by each, the same bytes) |
| Provider | mercury-python `extensions/agent-tools` (`v4.12.30-115-g3d2f1fa`) with `-Drest.server.port=8099` and no other key, so `time.now` was its only tool; nothing listened on 8089, where both engines' static Event-over-HTTP maps point `time.now` |
| Clients | the Playground's Deploy view in a browser pane (the graph files and the package handed to the view's file inputs); `templates/starter-graph/scripts/deploy-graph-set.mjs` and `undeploy-graph-set.mjs` on Node.js 26.13.1; `curl` |
| Host | macOS 26.7.3, OpenJDK 27.0.1.1, Rust release binary |

## Scenarios

The switches off (both engines started with their defaults):

| | Scenario | Observed |
| --- | --- | --- |
| N0 | `POST /api/graph-set/deploy`, `POST /api/graph-set/undeploy/hot-demo`, `GET /api/graph-set/deployed` | **404** `Resource not found` on both engines; the Java log `Skip [POST] /api/graph-set/deploy - Service deploy.graph.set not available` per entry, the Rust log `Skip optional deploy.graph.set (condition: graph.hot.deploy=true)`; the Deploy view read the list, got the 404 and showed *Hot deployment is off on this engine. Turn on graph.hot.deploy to deploy and graph.hot.undeploy to undeploy* |

The switches on, nothing deployed, the negative controls first (Java, 8085):

| | Scenario | Observed |
| --- | --- | --- |
| N1 | the set with `tutorial-2` inside | **409** `graph id tutorial-2 is owned by set tutorials (file:/tmp/unpack)` |
| N2 | the set not named after its main graph | **400** `a hot-deployable set is named after its main graph - the set holds no graph 'hot-demo'` |
| N3 | four bytes zeroed inside a string value | **200, deployed** - the package read strictly and the graphs passed the gate; the subgraph's provider URL now read `http://127.0.0\u0000\u0000\u0000\u0000{rest.server.port:8080}/…` (finding 2); undeployed again |
| N3b | the package cut 100 bytes short | **400** `Not a graph set - Malformed MsgPack: FIXSTR declares 10 byte(s) but only 0 follow at offset 2099` |
| N3c | a JSON text as the body | **400** `Not a graph set - Unexpected bytes after the value` |
| N4 | version 1 with version 2's digest in `X-Graph-Set-SHA256` | **400** `SHA-256 does not match - the body's digest is fcb1c0dc…` |
| N5 | an empty body | **400** `The request body is the graph set (.pack) to deploy, sent as application/octet-stream` |
| N6 | undeploy of a set never deployed; of the bundled `tutorial-2`; of `bad name` | **404** `set hot-demo is not deployed in file:/tmp/deploy`; **404** for the bundled graph (the folder never holds a record for one); **400** `a set name uses letters, digits, '_' and '-' only` |
| N7 | the folder after every refusal | empty |
| N8 | an engine started with `location.graph.deploy` equal to its `location.graph.temp` (Java on 8087, Rust on 8088) | `ERROR Hot deployment is OFF for this run - location.graph.deploy must not be the Playground's temporary folder (location.graph.temp), inside it or above it`; the entries served and answering **503** `hot deploy is off on this engine (graph.hot.deploy)` and `hot undeploy is off on this engine (graph.hot.undeploy)`; the list entry 200 with `location: file:/tmp/deploy` (follow-up 2) |
| N9 | an engine started with `location.graph.deploy` equal to the `tutorials` set's `unpack` folder (Java, 8087) | the other direction of the same guard: `ERROR Graph sets in classpath:/graphs.yaml not deployed - 'unpack' must not be the hot deployment folder (location.graph.deploy), inside it or above it` - the manifest's set was refused, hot deployment stayed on, a deploy into that folder succeeded; the two folders refuse each other, whichever is configured second loses |

The positive path (Java, 8085, through the three clients):

| | Scenario | Observed |
| --- | --- | --- |
| P1 | **Deploy view**: the two graph files dropped, the set name `hot-demo`, the manifest field `version` `1.0.0`, **Deploy…**, **Pack and deploy hot-demo.pack** | `POST /api/graph-set/pack` 200 then `POST /api/graph-set/deploy` 200; the card *set hot-demo · version 1.0.0 · SHA-256 fcb1c0dc… · graphs hot-demo-sub, hot-demo · outcome deployed*; the table one row, status `deployed`; the log `Deployed set hot-demo (version 1.0.0) - 2 graphs into file:/tmp/deploy (SHA-256 fcb1c0dc…)` |
| P2 | `POST /api/graph/hot-demo` `{"person_id":"100"}` | **200** `{"address":"100 World Blvd","name":"Peter"}` - the extension ran the private subgraph, the fetcher called the engine's own mock on 8085 |
| P3 | the subgraph at the graph and OpenAPI entries | **404** `hot-demo-sub not found`; **404** `Graph model 'hot-demo-sub' not found`; the main graph's OpenAPI document 200 |
| P4 | the folder and the record | `hot-demo.pack`, `hot-demo.json`, `hot-demo-sub.json`, `hot-demo.deployed.yaml` (set, version, sha256, deployed_at, the two graphs, the three files); `GET /api/graph-set/deployed` the same as `status: deployed` |
| P5 | **Deploy view**: the version 1 `.pack` dropped on the view | the card *outcome already deployed with these bytes - nothing changed*; the table unchanged |
| P6 | **script**: `deploy-graph-set.mjs --engine http://127.0.0.1:8085 --file hot-demo.pack --sha256 df347f51…` (version 2) | **200**, exit 0; `replaced: {version: 1.0.0, sha256: fcb1c0dc…}`, `changed: true`; `undeploy-graph-set.mjs --list` prints the record at 2.0.0; `POST /api/graph/hot-demo` answers `{"profile": {...}, "version": "2"}` |
| P7 | **script** offline paths | `--digest` prints `df347f51…` and exits 0; no `--file` exits 2 with the usage line; `--sha256` the file does not match exits 1 *before* the upload: `refused before upload: the file's SHA-256 is df347f51…, not fcb1c0dc…` |
| P8 | **Deploy view**: **Refresh**, **Undeploy**, the confirmation *Undeploy 2 graphs?* → **Yes, undeploy** (the set then at 3.1.0) | the table *No set is deployed in the hot folder of this engine*; the log `Undeployed set hot-demo - 2 graphs from file:/tmp/deploy`; the folder empty |
| P9 | **script**: version 3.2 deployed with its digest; the Java engine restarted; `POST /api/graph/hot-demo` | the startup log `Hot deployment folder file:/tmp/deploy - 1 set deployed`; the call answered 200 with the profile, the sidecar's time and `version: "3.2"` - the set restored from its record through the gate |
| P10 | **script**: `undeploy-graph-set.mjs --graph hot-demo`, then the same again | **200** exit 0 with the set, its version, digest and graphs; then exit 1 `refused (404): set hot-demo is not deployed in file:/tmp/deploy`; `--list` empty on both engines |

The two engines on one folder (WP6; the Rust engine on 8086 started after P5, with version 1 in the folder):

| | Scenario | Observed |
| --- | --- | --- |
| T1 | the Rust engine's startup | `Hot deployment folder file:/tmp/deploy - 1 set`: `hot-demo` registered from the record; `POST /api/graph/hot-demo` on 8086 answered `{"address":"200 World Blvd","name":"Mary"}` (its own mock, on its own port); the subgraph 404 |
| T2 | version 2 deployed **on Java** (P6), then `POST /api/graph/hot-demo` **on Rust**, 180 ms later | **200** `version: "2"` - the Rust engine read the record at the lookup, found another digest, reconciled the set and served the new version within the request; no restart, no poll, no log line (follow-up 1) |
| T3 | version 3.0 deployed **on Rust** with its digest, then the call **on Java** | the Rust answer `replaced: {version: 2.0.0}` and the advisory `routes: [{graph: hot-demo, route: demo.order.lookup, server: http://127.0.0.1:8086, status: remote}]`; the Java engine served version 3 at its next lookup and routed the task to the Rust engine (`annotations={node=order, task=demo.order.lookup, server.url=http://127.0.0.1:8086/api/event}`); the Rust Event API answered **403** `demo.order.lookup is private`, which became the node's error and the run's answer (finding 3) |
| T4 | version 3.1 on `async.http.request` the same way | the same 403, `async.http.request is private`: the engine's built-in client is private too; `/info/routes` lists no public route on either engine |
| T5 | version 3.2 on `time.now` with `server.url` naming the sidecar on 8099, deployed **on Java** through the script; the call **on both** | the advisory `route: time.now, server: http://127.0.0.1:8099, status: remote`; Java **200** `{profile, time: {utc: 2026-11-25T04:50:39.639Z, timezone: UTC, …}, version: "3.2"}`; Rust **200** the same shape 40 ms later; the sidecar's log `Handled time.now status=200` twice with the engines' trace ids - the node's provider won over a static map entry that pointed at a port with nothing listening |
| T6 | the undeploy **on Java** (P8), then the call **on Rust** | **404** `hot-demo not found`; the Rust log `Set hot-demo undeployed elsewhere - its record is gone` |
| T7 | a package and a graph file copied into the folder by hand (`stray.pack`, `stray.json`), the list, a call to `stray`, a call to `hot-demo` on both | the list unchanged; `stray` **404** on both; nothing registered; at the Java restart (P9) `WARN Package stray.pack in file:/tmp/deploy has no record and is ignored - a deployment goes through the deploy endpoint`; the `.json` drew no line (follow-up 3) |

## Findings

1. **A switch that is off leaves the entry unserved: 404, not 503.** The three functions carry `@OptionalService` on
   their switch (the Rust twin `#[optional_service]`), so with the switch off the function never registers. REST
   automation resolves an entry's service when `rest.yaml` loads and **skips** an entry whose service is not available
   (`RoutingEntry`: `Skip [POST] /api/graph-set/deploy - Service deploy.graph.set not available`); the URL then answers
   404 `Resource not found` like any unknown path. The 503 `Service … not reachable` of `HttpRouter` is for a route that
   was present at load and is unreachable at request time, which is not this case. The 503 the lane does answer is the
   engine's own: a switch on, but the folder refused at startup - the Playground's temp folder, inside it or above it,
   or a folder the application cannot write - turns hot deployment off for the run with an ERROR, the functions are
   registered, and each answers 503 naming its key (N8). **What changed:** the design kept (`@OptionalService` is the
   project's shape for an operational choice, and an entry that is not served discloses nothing); the configuration
   reference, the AI agent guide, the Playground's `help deploy`, the scripts' README and the two scripts' hints now
   say 404 for *off* and 503 for *on but the folder refused*, on both engines; the spec and the RFC entry carry the
   correction. The Deploy view needed no change: it had treated 404 and 503 alike since WP2c.
2. **The strict read refuses a malformed package, not an altered one.** The canonical package (ADR-0026) carries no
   checksum of its own; a change inside a string value leaves the MsgPack structure valid, so the set reads and the gate
   sees a graph whose URL is now broken (N3). The `X-Graph-Set-SHA256` header is the check that the bytes are the
   ledger's (N4), and the scripts' `--sha256` checks the file before the upload and sends the header (P7). **What
   changed:** one sentence in the AI agent guide's hot-deployment section and in the `help deploy` page: send the
   digest; without it the engine accepts any well-formed package.
3. **The provider named by `server.url` must publish the function.** The Event API of either engine serves public
   functions only; the engines' built-in functions (`async.http.request`) and an application's `@PreLoad` functions are
   private by default, and neither Playground example publishes any (`/info/routes`: no public route). A private
   function answers 403 `<route> is private`, which is the node's error (T3, T4); the deploy advisory marks the route
   `remote` without a check, by design (the reachability check is skipped: the provider decides). A language pack's
   extension publishes its functions, so the sidecar case the design describes works as written (T5). **What
   changed:** one sentence in the skills reference's `server.url` paragraph and in the AI agent guide, both engines:
   the provider's function must be public, a private one answers 403.
4. **The hot folder and a manifest's `unpack` folder refuse each other, in the order they are configured.** The hot
   folder is configured before the manifests are read, so a collision refuses the *manifest's sets* with an ERROR and
   hot deployment stays on (N9), while a collision with the Playground's temp folder, known at configure time, turns hot
   deployment off (N8). Both are loud and nothing is shared; the configuration reference now names the two outcomes.

## Follow-ups (declared here, not changed)

1. **The on-demand reconcile is silent on a deploy or a replace.** When an instance registers a set another instance
   deployed (T1 at startup logs `1 set`; T2, T3, T5 at a lookup log nothing), an operator reading that instance's log
   cannot tell which version it serves; the undeploy side does log (`Set hot-demo undeployed elsewhere - its record is
   gone`, T6). One INFO line per on-demand registration, naming the set, the version and the digest, in both engines.
2. **The list entry under "off for this run".** With a switch on and the folder refused, `GET /api/graph-set/deployed`
   answers 200 with an empty list and the *default* location (N8), while the two POST entries answer 503. The list
   should answer 503 with the same reason, or name the refused folder.
3. **A stray `.json` draws no WARN.** The startup reconcile warns for a package no record names (`stray.pack`, T7) and
   not for a graph file (`stray.json`), though the design lists both. Either warn for both or say packages only.
4. **The deploy INFO line carries no caller address.** The design's step 8 names the remote address in the line; the
   line names the set, version, graph count, folder and digest (P1). The HTTP access log has the address; the design
   text or the line should agree.

## Reproduce by hand

```bash
# the set: two renamed tutorials, packed as hot-demo (set name = main graph id)
java -jar helpers/graph-packager/target/graph-packager-<version>.jar pack --set hot-demo --manifest version=1.0.0 --out v1 <folder with hot-demo.json and hot-demo-sub.json>

# the Java engine with the switches on (dev mode: the Playground's Deploy view at http://127.0.0.1:8085/)
java -Dgraph.hot.deploy=true -Dgraph.hot.undeploy=true -jar minigraph-playground-<version>.jar
# the Rust engine on the same folder
minigraph-playground -Drest.server.port=8086 -Dlocation.graph.temp=file:/tmp/graph-rust -Dgraph.hot.deploy=true -Dgraph.hot.undeploy=true

# negative controls (expect 409, 400, 400, 404)
curl -s -X POST --data-binary @bundled/hot-demo.pack -H 'content-type: application/octet-stream' http://127.0.0.1:8085/api/graph-set/deploy
curl -s -X POST --data-binary @v1/hot-demo.pack -H 'content-type: application/octet-stream' -H 'X-Graph-Set-SHA256: 0000' http://127.0.0.1:8085/api/graph-set/deploy
head -c 2100 v1/hot-demo.pack > truncated.pack && curl -s -X POST --data-binary @truncated.pack -H 'content-type: application/octet-stream' http://127.0.0.1:8085/api/graph-set/deploy
curl -s -X POST http://127.0.0.1:8085/api/graph-set/undeploy/hot-demo

# the positive path through the scripts, then the other engine
node templates/starter-graph/scripts/deploy-graph-set.mjs --file v1/hot-demo.pack --digest
node templates/starter-graph/scripts/deploy-graph-set.mjs --engine http://127.0.0.1:8085 --file v1/hot-demo.pack --sha256 <that digest>
curl -s -X POST -H 'content-type: application/json' -d '{"person_id":"100"}' http://127.0.0.1:8085/api/graph/hot-demo
curl -s -X POST -H 'content-type: application/json' -d '{"person_id":"100"}' http://127.0.0.1:8086/api/graph/hot-demo   # the Rust engine, never told
curl -s -X POST -H 'content-type: application/json' -d '{"person_id":"100"}' http://127.0.0.1:8085/api/graph/hot-demo-sub  # 404: private
node templates/starter-graph/scripts/undeploy-graph-set.mjs --engine http://127.0.0.1:8086 --graph hot-demo
curl -s -X POST -H 'content-type: application/json' -d '{"person_id":"100"}' http://127.0.0.1:8085/api/graph/hot-demo   # 404 at the next lookup
```

The Deploy view: **Tools → Graph set packaging**, drop the two `.json` files, name the set `hot-demo`, **Deploy…**,
**Pack and deploy**; drop the `.pack` on the Deploy view for *nothing changed*; **Undeploy** → **Yes, undeploy**. Since
this drive the Tools menu also has **🚀 Graph set deployment**, which opens the view directly (the maintainer's ask on
seeing the drive: deploy and undeploy one click from the menu, as the other tools are).

## What this record settles for the RFC

The run-time unit, the record as the allowlist and revision marker, the one swap, the private subgraph, the ownership
rule and the on-demand convergence of several instances behaved as the design states on both engines; `server.url`
routes a task to the provider the node names and the provider's own answer is the node's. The three corrections are
to the wording of the design and its guides, not to the engine. RFC-0017 can be promoted with ADR-0011 marked amended
and ADR-0027 extended; the four follow-ups are operational polish for a later increment.
