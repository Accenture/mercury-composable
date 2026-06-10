// Generator: compose each phase's hand-authored framing with the verbatim
// spec-section lift, through a per-agent adapter. (distribution-plan.md, D2.)

import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, resolve, join } from "node:path";
import { fileURLToPath } from "node:url";
import { PHASES } from "../phases.mjs";
import { extractPhaseBody } from "./extract.mjs";
import { claudeWrapper } from "./targets/claude.mjs";

const __dirname = dirname(fileURLToPath(import.meta.url));
const DEFAULT_SPECS_DIR = resolve(__dirname, "..", ".."); // docs/graph-builder

const ADAPTERS = { claude: claudeWrapper };

/**
 * Generate wrapper files for one agent target.
 * Returns [{ relPath, content, phase, sections }]. Pure — no disk writes.
 */
export function generate({ target = "claude", specsDir = DEFAULT_SPECS_DIR } = {}) {
  const adapter = ADAPTERS[target];
  if (!adapter) {
    throw new Error(
      `no adapter for target ${JSON.stringify(target)} (have: ${Object.keys(ADAPTERS).join(", ")})`
    );
  }
  const total = PHASES.length;
  return PHASES.map((phase, idx) => {
    const md = readFileSync(join(specsDir, phase.spec), "utf8");
    const sections = extractPhaseBody(md);
    const wrapper = adapter({ phase, idx, total, sections, specFile: phase.spec });
    return { ...wrapper, phase: phase.phase, sections };
  });
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
    const faithful =
      w.content.includes(w.sections.stepChecklist) && w.content.includes(w.sections.gate);
    allFaithful &&= faithful;
    console.log(
      `graph-${w.phase}  ->  ${w.relPath}  (${w.content.length} bytes)  verbatim-fidelity: ${faithful}`
    );
  }
  console.log(`\nwrote ${wrappers.length} wrappers under ${outDir}`);
  console.log(`all wrappers contain their spec's Step checklist + Gate verbatim: ${allFaithful}`);
  process.exit(allFaithful ? 0 : 1);
}
