import { existsSync } from 'fs';
import { resolve } from 'path';

// One webapp, two engines. The bundle is built once and deployed to a TARGET: the
// resources folder of the Java engine (this repo) or of the Rust engine (the sibling
// mercury repo). deploy.js writes the hashed assets under {resources}/public/assets and
// the entry page to {resources}/template/playground.html; clean.js removes exactly
// those. The Rust target also receives a mirror of the help pages, because the help
// markdown is compiled INTO the bundle at build time and ALSO read by the engine at
// run time for the console 'help' command - one source (this repo), two copies.
//
// Paths are relative to the webapp folder (process.cwd() when npm runs a script).
// The Rust repo is expected beside this one under the same parent folder
// (…/sandbox/mercury-composable and …/sandbox/mercury); set MERCURY_RUST_REPO to
// point elsewhere, for example at a git worktree.
const webapp = process.cwd();
const JAVA_RESOURCES = resolve(webapp, '../src/main/resources');
const RUST_REPO = process.env.MERCURY_RUST_REPO ?? resolve(webapp, '../../../../mercury');

export const HELP_SOURCE = resolve(JAVA_RESOURCES, 'help');

export const TARGETS = {
  java: { name: 'java', resources: JAVA_RESOURCES, syncHelp: false },
  rust: { name: 'rust', resources: resolve(RUST_REPO, 'crates/knowledge-graph/resources'), syncHelp: true },
};

/** The target named by the script's first argument ('java' when absent); throws when unknown or absent on disk. */
export function resolveTarget(argv) {
  const name = argv[2] ?? 'java';
  const target = TARGETS[name];
  if (!target) {
    throw new Error(`Unknown deploy target '${name}' - use 'java' (the default) or 'rust'`);
  }
  if (!existsSync(target.resources)) {
    const hint = name === 'rust'
      ? '\nThe Rust repo is expected at ../../../../mercury relative to the webapp folder ' +
        '(…/sandbox/mercury beside …/sandbox/mercury-composable); set MERCURY_RUST_REPO to override.'
      : '';
    throw new Error(`Resources folder of target '${name}' not found: ${target.resources}${hint}`);
  }
  return target;
}
