# mercury-composable — Gemini CLI

Event-driven Java framework (Maven reactor, `com.accenture.mercury` v4.10.0) — self-contained functions wired by YAML event flows.

This project uses the agent-memory shared memory system. **Read [`AGENTS.md`](./AGENTS.md)
first** — it is the hub: it carries the memory protocol and what to read under `memory/`.

The hub and core memory files are imported below, so they are structurally present at
session start (presence is guaranteed; *attending* to them is still the protocol). Imports
can't express dynamic paths — still scan the newest 2–3 logs in `memory/sessions/` per the
protocol.

@./AGENTS.md
@./memory/instructions.md
@./memory/continuity.md
@./memory/vision.md

Identify yourself as **Gemini CLI** in all session logs.
