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

// Marker an optional/cross-cutting spec uses to self-designate a wrapper-body
// section. Placed on its own line immediately above the section heading.
export const WRAPPER_MARKER = "<!-- wrapper-body -->";

/**
 * Extract the wrapper body of an optional command spec: every section the spec
 * marks with `WRAPPER_MARKER` (on the line above its heading), lifted verbatim
 * by the same heading-scoped rule and returned in document order. The marker
 * selects *which* sections; the lift is still verbatim of the named heading, so
 * the body cannot say something the spec doesn't. Throws if a marker is not
 * immediately followed (ignoring blanks) by a heading, or if none are found —
 * a malformed/absent marker set is a spec-conformance bug, not silent output.
 */
export function extractMarkedBody(specMarkdown) {
  const lines = specMarkdown.split("\n");
  const headings = [];
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].trim() !== WRAPPER_MARKER) continue;
    let j = i + 1;
    while (j < lines.length && lines[j].trim() === "") j++;
    if (j >= lines.length || !/^#{1,6}\s/.test(lines[j])) {
      throw new Error(
        `${WRAPPER_MARKER} at line ${i + 1} is not immediately above a heading`
      );
    }
    headings.push(lines[j].trim());
  }
  if (headings.length === 0) {
    throw new Error(`no ${WRAPPER_MARKER} markers found`);
  }
  // Lift from a marker-stripped copy so a marker (which sits at the tail of the
  // preceding section, before the next heading) never leaks into the body.
  // Removing the marker lines restores the spec's original prose verbatim.
  const cleaned = lines.filter((l) => l.trim() !== WRAPPER_MARKER).join("\n");
  return headings.map((h) => extractSection(cleaned, h));
}
