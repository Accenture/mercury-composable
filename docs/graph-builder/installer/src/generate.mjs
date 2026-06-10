// Generator: compose each phase's hand-authored framing with the verbatim
// spec-section lift, through a per-agent adapter. (distribution-plan.md, D2.)

import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, resolve, join } from "node:path";
import { fileURLToPath } from "node:url";
import { PHASES, EXTRAS } from "../phases.mjs";
import { extractPhaseBody, extractMarkedBody } from "./extract.mjs";
import { claudeWrapper } from "./targets/claude.mjs";

const __dirname = dirname(fileURLToPath(import.meta.url));
const DEFAULT_SPECS_DIR = resolve(__dirname, "..", ".."); // docs/graph-builder

const ADAPTERS = { claude: claudeWrapper };

/**
 * Generate wrapper files for one agent target.
 * Returns [{ relPath, content, phase, bodySections }]. Pure — no disk writes.
 *
 * Two command classes, two governance regimes for the verbatim body:
 *  - PHASES: lifted by the FIXED rule (`## Step checklist` + `## Gate`), which is
 *    meaningful because workflow.md's Common Phase-Spec Skeleton mandates those
 *    headings uniformly — "no room for a wrong choice."
 *  - EXTRAS (optional, cross-cutting): the spec self-designates its body with
 *    `<!-- wrapper-body -->` markers; the lift is still verbatim of named
 *    headings. Selection lives in the spec, not an external map.
 * In both, `bodySections` is the ordered list of verbatim section texts the
 * fidelity check asserts are present in the wrapper.
 */
export function generate({ target = "claude", specsDir = DEFAULT_SPECS_DIR } = {}) {
  const adapter = ADAPTERS[target];
  if (!adapter) {
    throw new Error(
      `no adapter for target ${JSON.stringify(target)} (have: ${Object.keys(ADAPTERS).join(", ")})`
    );
  }
  const total = PHASES.length;

  const build = (phase, { numbered, idx, bodySections }) => {
    const wrapper = adapter({ phase, idx, total, numbered, bodySections, specFile: phase.spec });
    return { ...wrapper, phase: phase.phase, bodySections };
  };

  const phaseWrappers = PHASES.map((phase, idx) => {
    const md = readFileSync(join(specsDir, phase.spec), "utf8");
    const { stepChecklist, gate } = extractPhaseBody(md);
    return build(phase, { numbered: true, idx, bodySections: [stepChecklist, gate] });
  });

  const extraWrappers = EXTRAS.map((phase) => {
    const md = readFileSync(join(specsDir, phase.spec), "utf8");
    return build(phase, { numbered: false, idx: null, bodySections: extractMarkedBody(md) });
  });

  return [...phaseWrappers, ...extraWrappers];
}

// Run directly: write previews and assert verbatim fidelity (the body must
// contain its spec's Step checklist + Gate, character-for-character).
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const target = process.argv[2] || "claude";
  const outFlag = process.argv.indexOf("--out");
  const outDir =
    outFlag > -1 ? process.argv[outFlag + 1] : resolve(__dirname, "..", ".preview", target);

  const wrappers = generate({ target });
  let allFaithful = true;
  for (const w of wrappers) {
    const dest = join(outDir, w.relPath);
    mkdirSync(dirname(dest), { recursive: true });
    writeFileSync(dest, w.content);
    const faithful = w.bodySections.every((s) => w.content.includes(s));
    allFaithful &&= faithful;
    console.log(
      `graph-${w.phase}  ->  ${w.relPath}  (${w.content.length} bytes)  ` +
        `verbatim-fidelity (${w.bodySections.length} sections): ${faithful}`
    );
  }
  console.log(`\nwrote ${wrappers.length} wrappers under ${outDir}`);
  console.log(`every wrapper contains each of its designated spec sections verbatim: ${allFaithful}`);
  process.exit(allFaithful ? 0 : 1);
}
