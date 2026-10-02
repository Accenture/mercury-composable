import { rmSync } from 'fs';
import { resolve } from 'path';
import { resolveTarget } from './targets.js';
// Removes exactly what deploy.js writes for the target (java by default, or rust) -
// the hashed assets and the Playground entry page. {resources}/public/index.html (the
// plain home page) is kept, and so are the help pages: deploy.js mirrors them.
const target = resolveTarget(process.argv);
for (const path of [resolve(target.resources, 'public/assets'), resolve(target.resources, 'template/playground.html')]) {
  console.log(`Cleaning (${target.name}): ${path}`);
  rmSync(path, { recursive: true, force: true });
}
