# Graph Builder — Install the `/` Commands (agent self-install)

Status: the installer. This is the whole mechanism — there is no script to run.

> **How to use this:** point your coding agent at this file — e.g. *"Read `docs/graph-builder/install-commands.md` and install the graph-builder commands."* The agent creates the slash commands itself, in **its own native format**. Then start a fresh session so it picks them up.

**Scope:** this repository only. The commands reference `docs/graph-builder/*.md` by repo-root-relative path and assume the agent is working inside this repo (or a checkout of it). They are not designed to be copied into an unrelated project.

---

## The idea: thin pointers, not copies

Each `/` command is a **launcher, not a duplicate**. Its body does one thing: tell the agent to open the canonical doc and follow it as the source of truth. The command carries **no** workflow content of its own — no steps, no gate, no schema, no engine facts.

Because the command copies nothing, it **cannot drift** from the spec, and there is no second source of truth to keep in sync. Editing a phase means editing its one canonical doc; every command picks the change up automatically, with no reinstall.

## Commands to install

One command per canonical doc. The doc is authoritative; the command just points at it.

| Command | Canonical doc (source of truth) | Purpose | Argument hint |
|---|---|---|---|
| `/graph-requirements` | `docs/graph-builder/requirements-gathering.md` | Phase 1 — turn rough intent into a design-ready brief | `[optional: intent notes, a sample request/response, or a generated artifact]` |
| `/graph-design` | `docs/graph-builder/graph-design.md` | Phase 2 — turn the brief into a buildable architecture | `[path to the requirements brief]` |
| `/graph-build` | `docs/graph-builder/build.md` | Phase 3 — lower the design into verified MiniGraph commands | `[path to the design spec]` |
| `/graph-test` | `docs/graph-builder/test.md` | Phase 4 — prove runtime behavior by execution + inspection | `[graph name, or path to the build log]` |
| `/graph-verify` | `docs/graph-builder/verify.md` | Optional, any time — adversarial quality review of any artifact | `<target> [--report [path]]` |
| `/graph-builder` | `docs/graph-builder/README.md` | Overview — the doc index, precedence, and terminology (start here) | _(none)_ |

`/graph-requirements → /graph-design → /graph-build → /graph-test` is the linear pipeline. `/graph-verify` is optional and runs against any artifact at any time. `/graph-builder` is orientation.

## Command body template

Fill this per row. Keep it this short — it must **not** restate the doc.

```
Run the graph-builder <PURPOSE>.

**Source of truth:** open and follow `<CANONICAL DOC PATH>` in full — it is
authoritative for every step, gate, and schema. This command is only a
launcher: read the doc now, do not work from memory of it, and if this text
and the doc ever disagree, the doc wins.

**Input:** treat the command argument (if any) as <ARGUMENT MEANING>.

**Naming note:** the doc refers to the phases as `/requirements`, `/design`,
`/build`, `/test`; in this installation they are `/graph-requirements`,
`/graph-design`, `/graph-build`, `/graph-test`.
```

Worked example — `/graph-requirements`:

```
Run the graph-builder requirements phase (Phase 1 of 4).

**Source of truth:** open and follow `docs/graph-builder/requirements-gathering.md`
in full — it is authoritative for every step, gate, and schema. This command is
only a launcher: read the doc now, do not work from memory of it, and if this
text and the doc ever disagree, the doc wins.

**Input:** treat the command argument (if any) as a path to intent notes, a
sample request/response, or a generated artifact to seed the brief.

**Naming note:** the doc refers to the phases as `/requirements`, `/design`,
`/build`, `/test`; in this installation they are `/graph-requirements`,
`/graph-design`, `/graph-build`, `/graph-test`.
```

## Install it in your agent's native format

**General rule (any agent).** For each row, create one **project-level** custom slash command using your environment's native mechanism, with:

- **invocation name = the Command column** (e.g. `graph-requirements`). Bake the `graph-` prefix into the name; do not rely on directory nesting to namespace it.
- a one-line **description** and the **argument hint** from the table,
- **user-invoked only** (not auto/model-triggered) if your environment distinguishes the two,
- **body = the filled template above** — nothing more.

The body is identical regardless of agent; only the file location/format differs. So follow your environment's *current* docs for where project commands live, and drop the same body in.

### Claude Code — verified format

Source: `evidence/agent-command-formats.md` (Claude row, verified 2026-06-09).

- Create `.claude/skills/<command>/SKILL.md` — e.g. `.claude/skills/graph-requirements/SKILL.md`. **The directory name is the invocation name.**
- Subdirectories do **not** create a namespace prefix, which is why the `graph-` prefix is baked into the skill name rather than expressed as `/graph:requirements`.
- Frontmatter: `name`, `description`, `argument-hint`, `disable-model-invocation: true` (these phases are user-invoked, never auto-triggered).
- **Project-scoped** (`.claude/skills/…`) — do not write to the personal `~/.claude/skills/`.

Full Claude skill for `/graph-requirements`:

```markdown
---
name: graph-requirements
description: "Graph-builder phase 1/4 — turn rough intent into a design-ready brief (user-invoked)."
argument-hint: "[optional: intent notes, a sample request/response, or a generated artifact]"
disable-model-invocation: true
---

Run the graph-builder requirements phase (Phase 1 of 4).

**Source of truth:** open and follow `docs/graph-builder/requirements-gathering.md`
in full — it is authoritative for every step, gate, and schema. This command is
only a launcher: read the doc now, do not work from memory of it, and if this
text and the doc ever disagree, the doc wins.

**Input:** treat the command argument (if any) as a path to intent notes, a
sample request/response, or a generated artifact to seed the brief.

**Naming note:** the doc refers to the phases as `/requirements`, `/design`,
`/build`, `/test`; in this installation they are `/graph-requirements`,
`/graph-design`, `/graph-build`, `/graph-test`.
```

Repeat for the other rows, changing only `name`, `description`, `argument-hint`, the purpose line, the doc path, and the argument meaning.

### Copilot / Codex / Kiro — use your own mechanism

This repo has only verified Claude's exact format. For these agents, follow your environment's current docs for the project-level custom-command file (Copilot prompt files, Codex prompts, Kiro steering/specs, etc.). Keep the invocation name `graph-<phase>` and use the same command body. If you verify a format, add a cited row to `evidence/agent-command-formats.md`.

## Verify the install

Invoke `/graph-requirements` (and the others). Each should open its canonical doc and begin that phase rather than improvise. If your agent caches commands, start a new session first.

## Engine prerequisites (per command)

The commands are pointers — installing them stands up no engine. Only two phases touch a live engine:

- `/graph-requirements`, `/graph-design`, `/graph-builder`, `/graph-verify`* — no engine needed (they produce or review documents).
- `/graph-build`, `/graph-test` — need Node ≥ 20, `companion.mjs`, and a reachable `MINIGRAPH_HOST` (default `localhost:8085`); `/graph-test` also uses the stub-server pattern (`evidence/stub-server.mjs`) for mocked sources.

*`/graph-verify` needs a live engine only when it is reproducing engine-behavior claims; a doc/spec review needs none.
