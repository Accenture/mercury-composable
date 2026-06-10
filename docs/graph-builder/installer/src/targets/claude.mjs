// Claude Code target adapter. Emits a Skill at `.claude/skills/graph-<phase>/SKILL.md`
// (verified format — see evidence/agent-command-formats.md, Claude row, 2026-06-09).
// Body = hand-authored framing + the verbatim-lifted spec sections + a link to
// the authoritative canonical spec under `.graph-builder/`.

const DEFAULT_ENGINE_PREREQ =
  "**Prerequisites.** This phase drives a live MiniGraph engine via `companion.mjs`: it needs Node >=20, a reachable `MINIGRAPH_HOST` (default `localhost:8085`), and — for `/graph-test` — the stub-server pattern for mocked sources. (`/graph-requirements` and `/graph-design` need none of this.)";

export function claudeWrapper({ phase, idx, total, numbered, bodySections, specFile }) {
  const name = `graph-${phase.phase}`;
  const shortPurpose = phase.purpose.split(/[:.]/)[0].trim();
  const description = numbered
    ? `Graph-builder workflow phase ${idx + 1}/${total} — ${shortPurpose} (user-invoked).`
    : `Graph-builder optional cross-cutting step — ${shortPurpose} (user-invoked).`;
  const specLink = `.graph-builder/${specFile}`;

  const lines = [
    "---",
    `name: ${name}`,
    `description: ${JSON.stringify(description)}`,
    `argument-hint: ${JSON.stringify(phase.argumentHint)}`,
    "disable-model-invocation: true",
    "---",
    "",
    phase.purpose,
    "",
    `**Invocation.** ${phase.invocation}`,
    "",
  ];

  // Prerequisites: an explicit per-command block overrides; else the default
  // engine text when the phase touches the engine; else nothing.
  const prereq = phase.prerequisites ?? (phase.engine ? DEFAULT_ENGINE_PREREQ : null);
  if (prereq) lines.push(prereq, "");

  lines.push(
    `**Canonical spec (authoritative).** Full detail and precedence live in [\`${specLink}\`](${specLink}); defer to it on any conflict. The section(s) below are lifted verbatim from that spec — this wrapper is not a second source of truth.`,
    ""
  );

  // The pipeline-phase renaming note only applies to the numbered phases.
  if (numbered) {
    lines.push(
      "**Command names.** The spec text below names the phases `/requirements`, `/design`, `/build`, `/test`; in this installation they are `/graph-requirements`, `/graph-design`, `/graph-build`, `/graph-test`.",
      ""
    );
  }

  for (const section of bodySections) {
    lines.push(section, "");
  }

  return { relPath: `.claude/skills/${name}/SKILL.md`, content: lines.join("\n") };
}
