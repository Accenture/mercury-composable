// Heading-scoped section extraction — the single rule that makes a wrapper's
// body a verbatim projection of the canonical spec (distribution-plan.md, D1).
//
// Rule: take a heading's content (including the heading line) until the next
// heading of level <= its own. Deeper subsections (e.g. requirements' Gate's
// `### Gate A-D`) are therefore included; the lift stops only at the next
// same-or-higher heading. Verified to extract `## Step checklist` and `## Gate`
// cleanly from all four phase specs (worklog W33/W34).

/**
 * Extract one markdown section by its exact heading line (e.g. "## Gate").
 * Returns the section text verbatim (trailing whitespace trimmed), or throws
 * if the heading is absent — an absent section is a spec-conformance bug, not
 * something to paper over.
 */
export function extractSection(markdown, headingLine) {
  const lines = markdown.split("\n");
  const start = lines.findIndex((l) => l.trim() === headingLine);
  if (start < 0) {
    throw new Error(`section not found: ${JSON.stringify(headingLine)}`);
  }
  const level = lines[start].match(/^#+/)[0].length;
  let end = lines.length;
  for (let i = start + 1; i < lines.length; i++) {
    const m = lines[i].match(/^(#{1,6})\s/);
    if (m && m[1].length <= level) {
      end = i;
      break;
    }
  }
  return lines.slice(start, end).join("\n").replace(/\s+$/, "");
}

/** Extract the two wrapper-body sections from a phase spec's markdown. */
export function extractPhaseBody(specMarkdown) {
  return {
    stepChecklist: extractSection(specMarkdown, "## Step checklist"),
    gate: extractSection(specMarkdown, "## Gate"),
  };
}
