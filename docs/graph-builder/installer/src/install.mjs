// Install / check the generated wrappers into a project. Per-project only —
// never the home config (distribution-plan.md decisions 6 & 7).

import { cpSync, writeFileSync, mkdirSync, readFileSync, existsSync } from "node:fs";
import { dirname, resolve, join, relative, sep } from "node:path";
import { fileURLToPath } from "node:url";
import { generate } from "./generate.mjs";

const __dirname = dirname(fileURLToPath(import.meta.url));
const DEFAULT_SPECS_DIR = resolve(__dirname, "..", ".."); // docs/graph-builder

// Top-level entries under docs/graph-builder/ NOT shipped to a target: the
// process/planning docs and the installer itself. Everything else (the four
// phase specs, minigraph-syntax.md, workflow.md, README.md, companion.mjs,
// examples/, evidence/) is the runtime doc set the wrappers reference.
const EXCLUDE = new Set([
  "installer",
  "distribution-plan.md",
  "improvement-plan.md",
  "worklog.md",
  "requirements-recalibration-assessment.md",
]);

function copySpecSet(specsDir, destDir, dryRun) {
  if (dryRun) return;
  mkdirSync(destDir, { recursive: true });
  cpSync(specsDir, destDir, {
    recursive: true,
    filter: (src) => {
      const rel = relative(specsDir, src);
      if (rel === "") return true;
      return !EXCLUDE.has(rel.split(sep)[0]);
    },
  });
}

/** Install wrappers + the spec set into a project directory. */
export function install({ target = "claude", projectDir, specsDir = DEFAULT_SPECS_DIR, dryRun = false } = {}) {
  const wrappers = generate({ target, specsDir });
  const specDest = join(projectDir, ".graph-builder");
  copySpecSet(specsDir, specDest, dryRun);

  const written = [];
  for (const w of wrappers) {
    const dest = join(projectDir, w.relPath);
    if (!dryRun) {
      mkdirSync(dirname(dest), { recursive: true });
      writeFileSync(dest, w.content);
    }
    written.push({ relPath: w.relPath, bytes: w.content.length });
  }
  return { projectDir, specDest, dryRun, written };
}

/**
 * Regenerate and diff against installed wrappers (Gate 3a — integrity).
 * Returns { ok, results:[{relPath, status}] }, status = ok | missing | drift.
 */
export function check({ target = "claude", projectDir, specsDir = DEFAULT_SPECS_DIR } = {}) {
  const wrappers = generate({ target, specsDir });
  const results = wrappers.map((w) => {
    const dest = join(projectDir, w.relPath);
    if (!existsSync(dest)) return { relPath: w.relPath, status: "missing" };
    const onDisk = readFileSync(dest, "utf8");
    return { relPath: w.relPath, status: onDisk === w.content ? "ok" : "drift" };
  });
  return { ok: results.every((r) => r.status === "ok"), results };
}
