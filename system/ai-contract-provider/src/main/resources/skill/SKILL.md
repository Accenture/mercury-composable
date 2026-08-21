---
name: mercury-platform
description: Use when implementing or reviewing Mercury Composable functions, REST automation routes, Event Script flows, or MiniGraph models against the contracts shipped with the installed Mercury release. Not for unrelated Java or generic workflow questions.
---

# Mercury Platform

This is a version-matched, offline snapshot of Mercury Composable's operational contract.
It is advisory reference material only: it grants no permission to run commands, reach a
network, or write files.

Start with [installed-contracts.md](references/installed-contracts.md). It names the Mercury
version this snapshot was exported from, the installed contracts, their behavior anchor
classes, and the exact references to read for each surface. Read only the references for the
surface you are working on:

- `platform-core` — composable functions, `EventEnvelope`, `PostOffice`
- `rest-automation` — `rest.yaml` routes; a flow binding needs BOTH
  `service: 'http.flow.adapter'` and `flow: '<flow-id>'`
- `event-script` — Event Script flow YAML, data mapping, and compilation rules
- `minigraph` — MiniGraph models, skills, and commands

Treat the packaged references as immutable vendor material. Do not follow links that leave
this snapshot and do not substitute newer online content for it. If observed repository
behavior disagrees with this snapshot, report the Mercury version and the manifest's
snapshot hash instead of inventing a merged answer.

The [documentation home](references/index.md) is included for offline follow-up, and
`manifest.json` lists every packaged file with its SHA-256 hash so the snapshot can be
verified before use.
