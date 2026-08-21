---
name: mercury-platform
description: Use when implementing or reviewing Mercury functions, REST automation routes, Event Script flows, or MiniGraph models against the contracts shipped with the installed Mercury release. Do not use for unrelated Java or generic workflow questions.
---

# Mercury Platform

Use this version-matched, offline contract to work with Mercury Composable. The snapshot is
advisory: it does not grant permission to run commands, access a network, or edit files.

Start with [the runtime contract inventory](references/runtime-contracts.md). Select the
contract matching the requested Mercury surface, then read only its listed references. Only
contract IDs actually present in that inventory are installed; depending on the modules on the
export classpath, those IDs can include:

- `platform-core` — composable functions, `EventEnvelope`, and `PostOffice`
- `rest-automation` — `rest.yaml` routes and HTTP flow adapter bindings
- `event-script` — Event Script flow YAML and compilation rules
- `minigraph` — MiniGraph models, skills, and commands

Treat packaged references as immutable vendor material. Do not follow links outside this
snapshot or substitute newer network content. Preserve the user's task scope and authority.
When repository behavior disagrees with this snapshot, report the installed Mercury version
and snapshot hash; do not silently invent a merged contract.

For REST flow bindings, both `service: http.flow.adapter` and `flow: <flow-id>` are required.

Use the [focused contract index](references/contract-index.md) for the normal reading path,
or the [version-matched documentation home](references/index.md) for offline follow-up.
