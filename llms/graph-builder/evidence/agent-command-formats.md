# Agent Command Formats — D0 Evidence

Purpose: a verified, cited record of each target agent's native custom-command mechanism, so the generator emits the correct format per agent. **No format is asserted from memory** — each row cites a current source. (Gate 0: no wrapper is generated for an agent without a verified row here.)

---

## Claude Code — VERIFIED 2026-06-09

Source: official Claude Code docs, retrieved 2026-06-09 via the `claude-code-guide` agent — `https://code.claude.com/docs/en/skills.md` and `https://code.claude.com/docs/en/commands.md`.

- **Primitive:** **Skill** (current). Custom slash commands have been merged into skills — `.claude/commands/<name>.md` (legacy) and `.claude/skills/<name>/SKILL.md` (current) both create `/<name>` and behave identically. We target the **Skill** form.
- **Location:** project `.claude/skills/<name>/SKILL.md`; personal `~/.claude/skills/<name>/SKILL.md`.
- **Invocation name = directory name.** `.claude/skills/graph-requirements/SKILL.md` → `/graph-requirements`.
- **Namespacing (the load-bearing finding):** subdirectories do **NOT** create a namespace prefix in the invocation. `.claude/skills/graph/requirements/SKILL.md` is still invoked as `/requirements`; the subdirectory is only an organizational label, not a `/graph:requirements` prefix. ⇒ the `graph` prefix (locked item 1) must be **baked into the skill name** (`graph-requirements`), not expressed as `/graph:requirements`. This is the hyphen-prefix branch the locked decision anticipated.
- **Frontmatter we use:** `name`, `description`, `argument-hint`, `disable-model-invocation: true` (user-only — our phases are explicitly invoked, never auto-triggered). Available but unused: `when_to_use`, `arguments`, `allowed-tools`, `disallowed-tools`, `model`, `effort`, `context`, `agent`, `hooks`, `paths`, `shell`.
- **Arguments:** `$ARGUMENTS`, `$ARGUMENTS[N]`, `$N`, and `$name` (when declared in `arguments`).
- **Collision check (decision 4):** none of `graph-requirements`, `graph-design`, `graph-build`, `graph-test` collide with any built-in command (built-ins include `/review`, `/verify`, `/run`, `/init`, `/plan`, … — there is no built-in `/build`, `/test`, `/design`, or `/requirements`, and certainly none with the `graph-` prefix). The `graph-` prefix also clears the user's installed gstack skills (`/review`, `/ship`, `/qa`, …). **Safe.**
- **Caveat to confirm at D4:** the no-subdirectory-namespacing behavior is doc-cited but not yet live-verified in *this* environment. D4's install-and-invoke is the execution check (the plan's fidelity gate).

**Generator target for Claude:** emit `.claude/skills/graph-{phase}/SKILL.md`, frontmatter `{name, description, argument-hint, disable-model-invocation: true}`, body = hand-authored framing + the verbatim-lifted `## Step checklist` + `## Gate`, plus a link to `.graph-builder/{spec}`. **Project-scoped only** — the CLI writes `<project>/.claude/skills/…`, never the personal `~/.claude/skills/` (decision 7).

---

## GitHub Copilot — NOT YET VERIFIED (D5)

To verify before generating: prompt-file vs. chat-mode mechanism (`.github/prompts/*.prompt.md` / `.github/chatmodes/*.chatmode.md` / `copilot-instructions.md`), invocation syntax, frontmatter, namespacing, collision check. Cite a current source.

## GPT Codex — NOT YET VERIFIED (D5)

To verify before generating: `AGENTS.md` + custom prompt mechanism (`~/.codex/prompts/` or repo), invocation, namespacing, collision check. Cite a current source.

## Amazon Kiro — NOT YET VERIFIED (D5)

To verify before generating: steering (`.kiro/steering/`) vs. specs (`.kiro/specs/`) vs. hooks, invocation, namespacing, collision check. Cite a current source.
