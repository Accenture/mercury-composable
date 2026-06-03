# AGENTS.md

Universal context for AI coding agents working in this repository. This is the portable, tool-agnostic entry point (read by Codex, Cursor, GitHub Copilot, Aider, and others). Claude Code reads it via `CLAUDE.md`, which imports this file.

## Project

**Mercury Composable** — a Java framework for building composable, event-driven applications from self-contained functions wired together by YAML-configured event flows. See [README.md](./README.md) and [CONTRIBUTING.md](./CONTRIBUTING.md).

## Capabilities

### Graph Builder (MiniGraph)

When the user asks you to **build, modify, debug, or generate commands for a MiniGraph graph** (any composition of computation, branching/decision logic, data sourcing, orchestration, or sub-graph reuse), follow the documented workflow instead of improvising:

1. Read [docs/graph-builder/workflow.md](./docs/graph-builder/workflow.md) — the end-to-end process (analyze → design → generate commands → execute via the Companion API → verify), plus reusable graph patterns.
2. Read [docs/graph-builder/minigraph-syntax.md](./docs/graph-builder/minigraph-syntax.md) — the command syntax, node types, naming conventions, and mapping-expression reference.

Key rules (full detail in those docs):
- Graphs are authored via commands sent to the Companion API (`POST /api/companion/{session-id}`, `Content-Type: text/plain`) — never write graph JSON directly.
- One command per HTTP request; commands end at the body boundary (no terminator). Never include a literal `...` line.
- Strip `\r` from command bodies (the parser requires LF line endings).
- Verify after every create/update/delete before continuing.
