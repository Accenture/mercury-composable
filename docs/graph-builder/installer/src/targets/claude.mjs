// Claude Code target adapter. Emits a Skill at `.claude/skills/graph-<phase>/SKILL.md`
// (verified format — see evidence/agent-command-formats.md, Claude row, 2026-06-09).
// Body = hand-authored framing + the verbatim-lifted spec sections + a link to
// the authoritative canonical spec under `.graph-builder/`.

export function claudeWrapper({ phase, idx, total, sections, specFile }) {
  const name = `graph-${phase.phase}`;
  const shortPurpose = phase.purpose.split(/[:.]/)[0].trim();
  const description = `Graph-builder workflow phase ${idx + 1}/${total} — ${shortPurpose} (user-invoked).`;
  const specLink = `.graph-builder/${specFile}`;

  const lines = [
    "---",
    `name: ${name}`,
    `description: ${description}`,
    `argument-hint: ${phase.argumentHint}`,
    "disable-model-invocation: true",
    "---",
    "",
    phase.purpose,
    "",
    `**Invocation.** ${phase.invocation}`,
    "",
  ];

  if (phase.engine) {
    lines.push(
      "**Prerequisites.** This phase drives a live MiniGraph engine via `companion.mjs`: it needs Node >=20, a reachable `MINIGRAPH_HOST` (default `localhost:8085`), and — for `/graph-test` — the stub-server pattern for mocked sources. (`/graph-requirements` and `/graph-design` need none of this.)",
      ""
    );
  }

  lines.push(
    `**Canonical spec (authoritative).** Full detail and precedence live in [\`${specLink}\`](${specLink}); defer to it on any conflict. The step checklist and gate below are lifted verbatim from that spec — this wrapper is not a second source of truth.`,
    "",
    "**Command names.** The spec text below names the phases `/requirements`, `/design`, `/build`, `/test`; in this installation they are `/graph-requirements`, `/graph-design`, `/graph-build`, `/graph-test`.",
    "",
    sections.stepChecklist,
    "",
    sections.gate,
    ""
  );

  return { relPath: `.claude/skills/${name}/SKILL.md`, content: lines.join("\n") };
}
