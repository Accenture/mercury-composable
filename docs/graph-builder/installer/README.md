# graph-builder installer

Installs the four-phase graph-authoring workflow (`/graph-requirements` → `/graph-design` → `/graph-build` → `/graph-test`) as native commands for a coding agent. Claude Code is supported today; Copilot / Codex / Kiro are planned.

The wrapper bodies are not hand-written — each command's step checklist and gate are lifted **verbatim** from the canonical phase spec it links to, so a command can never drift into a second source of truth. See [../distribution-plan.md](../distribution-plan.md).

## Requirements

- **Node ≥ 20** to run the installer. No dependencies, no install step, no npm publish.

## Usage

Run it directly (no `npx`):

```bash
# install the Claude Code commands into a project (defaults to the current directory)
node docs/graph-builder/installer/bin/cli.mjs install claude --to /path/to/your/project

# preview without writing anything
node docs/graph-builder/installer/bin/cli.mjs install claude --to /path/to/project --dry-run

# verify installed commands still match the current specs (integrity check)
node docs/graph-builder/installer/bin/cli.mjs check claude --to /path/to/project
```

Installation is **per-project only** — it never touches your home config (`~/.claude`). It writes:

| Path | What |
|---|---|
| `<project>/.claude/skills/graph-{requirements,design,build,test}/SKILL.md` | the four commands |
| `<project>/.graph-builder/` | the canonical spec set the commands reference (phase specs, `minigraph-syntax.md`, `workflow.md`, `companion.mjs`, examples, evidence) |

Start a new agent session after installing so it picks up the skills.

## Per-command prerequisites

The installer drops files only — it does **not** stand up an engine. Engine needs are **per command**:

| Command | Engine needed? | Prerequisite |
|---|---|---|
| `/graph-requirements` | No | none — produces a brief document |
| `/graph-design` | No | none — produces a design document |
| `/graph-build` | **Yes** | Node ≥ 20, `companion.mjs`, a reachable `MINIGRAPH_HOST` (default `localhost:8085`) |
| `/graph-test` | **Yes** | the above, plus the stub-server pattern (`.graph-builder/evidence/stub-server.mjs`) for mocked sources |

## Layout

```
installer/
  bin/cli.mjs            # the runnable CLI (install / check)
  phases.mjs             # the four phases' hand-authored framing (the only hand-written wrapper content)
  src/
    extract.mjs          # the heading-scoped lift: a section's content until the next heading of level <= its own
    generate.mjs         # composes framing + verbatim spec sections via a per-agent adapter
    install.mjs          # copies the spec set + writes wrappers into a project; check = regenerate-and-diff
    targets/claude.mjs   # Claude Code adapter (emits .claude/skills/<name>/SKILL.md)
```
