# Mercury Composable — system modules (scoped guide)

> **Contributors working in this repository:** follow the root [AGENTS.md](../AGENTS.md)
> (the agent-memory protocol) first — this file does not replace it. This scoped guide
> serves AI tools that **consume mercury-composable as a plugin or dependency** and need
> the fastest correct starting point.

## Starting point for consumer AI tools

Mercury's version-matched operational contract is served by a dedicated app in this
folder: [`ai-contract-provider`](ai-contract-provider/README.md).

- **Live discovery:** run the app (`java -jar ai-contract-provider-<version>-exec.jar`,
  port 8999) and start with `GET /api/discovery` — it names the Mercury version, the
  installed contracts, and every other endpoint (`/api/contracts`, `/api/contracts/{id}`,
  `/api/skill`, `/api/references?path=...`, `/api/manifest`).
- **Offline:** `--export <dir>` writes the self-contained `mercury-platform` Agent Skill:
  `SKILL.md` entrypoint, the full linked guide set, the installed contract inventory, and
  a per-file SHA-256 manifest for integrity verification.

Both surfaces report `mercury_version` from the installed release — verify claims against
it rather than assuming the docs match your runtime.

## What lives in this folder

| Module | Role |
| --- | --- |
| `platform-core` | core engine: functions, `EventEnvelope`, `PostOffice`, REST automation |
| `event-script-engine` | Event Script: YAML flows replacing orchestration code |
| `minigraph-playground-engine` | Active Knowledge Graph engine + playground |
| `rest-spring-4` | optional Spring Boot integration |
| `mini-scheduler`, `minimalist-kafka`, `twin-kafka` | scheduling and Kafka libraries |
| `ai-contract-provider` | AI discovery app serving this operational contract |

## Key references (repo-relative)

- Read first: `docs/guides/ai-developer-guide.md`
- Machine catalogs: `docs/guides/rest-automation/rest-automation.json`,
  `docs/guides/event-script/event-script-flow.json`,
  `docs/guides/knowledge-graph/minigraph-commands.json`
- Contract catalog: `system/ai-contract-provider/src/main/resources/contracts.yaml`

## The one gotcha

A `rest.yaml` flow binding requires **both** `service: 'http.flow.adapter'` and
`flow: '<flow-id>'` — an entry with `flow:` alone is skipped as invalid at startup.
