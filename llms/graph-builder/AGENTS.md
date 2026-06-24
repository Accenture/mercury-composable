# Graph Builder — Agent Capability Contract

Status: canonical. The single place that states **what graph-builder assumes about your coding agent** — and the only place agent-dependent *mechanisms* are named.

Purpose: keep every phase spec ([llms/graph-builder/requirements-gathering.md](llms/graph-builder/requirements-gathering.md), [llms/graph-builder/graph-design.md](llms/graph-builder/graph-design.md), [llms/graph-builder/build.md](llms/graph-builder/build.md), [llms/graph-builder/test.md](llms/graph-builder/test.md), [llms/graph-builder/verify.md](llms/graph-builder/verify.md)) **capability-neutral**. A spec states the *invariant it needs* (e.g. "an independent reviewer with authoring context withheld"); it never names a vendor primitive (e.g. "spawn a subagent"). When a step needs a capability whose *mechanism* differs across agents, the spec points here and this doc owns the per-agent mapping. That way the workflow runs on any agent, and there is exactly one file to update when a new agent is supported.

> **File references in this doc are repo-root-relative paths** (e.g. `llms/graph-builder/verify.md`), the same convention [llms/graph-builder/install-commands.md](llms/graph-builder/install-commands.md) uses for agent-facing pointers — they resolve identically regardless of the reader's working directory or whether it parses markdown links.

> **Companion file:** [llms/graph-builder/install-commands.md](llms/graph-builder/install-commands.md) covers *how to install the `/graph-*` commands in your agent's native format*. This file covers *what your agent must be able to do for those commands to work*. No overlap.

## Authority & precedence

This doc owns **how to satisfy an agent-dependent capability on your agent**. It does **not** override:

- [llms/graph-builder/adversarial-review-checklist.md](llms/graph-builder/adversarial-review-checklist.md) — owns **what independence *means*** (a reviewer that neither authored nor advocated the artifact; Gate A's "as if a rival built it"). This doc only maps that requirement onto concrete agent mechanisms.
- Each phase spec — owns its own steps, gates, and schemas.
- Engine source / observed runtime — authoritative for engine behavior, as always.

## The capability contract

**C1–C3 are table stakes for almost any modern coding agent** — the workflow assumes your agent can:

- **C1 — follow a referenced doc as the source of truth:** open a repo-relative path and execute its instructions instead of working from memory. (If your agent caches command bodies, start a fresh session after install.)
- **C2 — read and write repository files:** every phase produces a brief, design spec, build log, or report as a file.
- **C3 — execute Node ≥ 20** to run [llms/graph-builder/companion.mjs](llms/graph-builder/companion.mjs) against a reachable `MINIGRAPH_HOST` (default `localhost:8085`). Only `/graph-build` and `/graph-test` (and `/graph-verify` *when reproducing engine-behavior claims*) need the engine; the doc-only phases need none — see [llms/graph-builder/install-commands.md](llms/graph-builder/install-commands.md) → *Engine prerequisites*.

**C4 — obtain an independent, fresh-context review — is the one load-bearing, agent-dependent capability, and the reason this file exists.** Its **invariant** is fixed; only the **mechanism** varies by agent. Used by `/graph-verify` and the ship-class independent-review rule in [llms/graph-builder/adversarial-review-checklist.md](llms/graph-builder/adversarial-review-checklist.md). Detailed next.

## C4 — Independent review: the invariant, then the mechanism

**Invariant (does not vary, owned by [llms/graph-builder/adversarial-review-checklist.md](llms/graph-builder/adversarial-review-checklist.md)).** The review must be performed by a reviewer that:

- **neither authored nor advocated** the artifact;
- runs in a **genuinely fresh context** — the authoring/advocacy history and any desired verdict **withheld**, so "re-evaluate as if a rival built it" is *literally true*, not imagined;
- is pointed at the artifact, the [llms/graph-builder/adversarial-review-checklist.md](llms/graph-builder/adversarial-review-checklist.md), and (when reproducing engine claims) the live engine + framework source + [llms/graph-builder/minigraph-syntax.md](llms/graph-builder/minigraph-syntax.md).

A spec that needs this says exactly that — *"obtain an independent, fresh-context review (C4)"* — and stops. It must **not** prescribe a vendor primitive.

**Mechanism (varies by agent — pick the first your agent supports).**

| Your agent has… | Satisfy C4 by… |
|---|---|
| a subagent / task-spawn primitive (e.g. Claude Code's Agent/Task tool) | spawning a **fresh-context subagent**, passing only the artifact + checklist + ground-truth access, withholding authorship and the desired verdict. |
| no subagent primitive, but separate sessions | running the review in a **separate session/instance** of the agent with the authoring conversation withheld; paste in only the artifact + checklist + ground-truth pointers. |
| a second agent available | handing the artifact to a **different agent** under the same withholding rules. |
| a human reviewer in the loop | a **human** who neither authored nor recommended the artifact. |

**What never satisfies C4:** same-context self-review (the author re-reading their own work in the conversation that produced it). It is a useful *pre-filter* and the author's duty, but it does not meet the bar for a ship-class artifact — see [llms/graph-builder/adversarial-review-checklist.md](llms/graph-builder/adversarial-review-checklist.md) → *Independent review*.

**In the `--report` record** ([llms/graph-builder/verify.md](llms/graph-builder/verify.md) schema), describe the reviewer by the invariant it satisfied — *"independent reviewer — authoring/advocacy context withheld"* — not by the vendor primitive used.

## Adding support for a new agent

You do not edit the phase specs. You:

1. Verify and cite the agent's native custom-command format in [llms/graph-builder/evidence/agent-command-formats.md](llms/graph-builder/evidence/agent-command-formats.md) (Gate 0 — no command is installed for an agent without a verified row).
2. Confirm the agent can satisfy **C1–C3**, and identify **which C4 mechanism row** above it supports (add a row if it is a genuinely new mechanism class).
3. Install per [llms/graph-builder/install-commands.md](llms/graph-builder/install-commands.md) in that agent's format.

If the agent cannot satisfy a C4 mechanism at all, `/graph-verify` and the ship-class independent-review rule degrade to human review — note that, rather than silently downgrading to self-review.
