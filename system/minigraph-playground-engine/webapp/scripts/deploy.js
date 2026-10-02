import { cpSync, existsSync, mkdirSync, readdirSync, rmSync } from 'fs';
import { resolve } from 'path';
import { HELP_SOURCE, resolveTarget } from './targets.js';
// The built bundle is deployed in two parts. The hashed assets are static content
// ({resources}/public/assets). The web app's entry page goes OUTSIDE the static folder,
// to {resources}/template/playground.html, where get.index.html serves it only when
// app.env=dev - so a production deployment never shows the Playground UI.
// {resources}/public/index.html stays the plain home page and is not touched.
//
// The target is java (this repo, the default) or rust (the sibling mercury repo). For
// the Rust target the help pages are mirrored as well: they are compiled into the
// bundle at build time, and the Rust engine reads the same files at run time for the
// console 'help' command, so both copies come from one source - this repo's help folder.
const target = resolveTarget(process.argv);
const src = resolve(process.cwd(), 'dist');
if (!existsSync(resolve(src, 'index.html'))) {
  throw new Error(`No build output at ${src} - run 'npm run build' first (or 'npm run release')`);
}
const assets = resolve(target.resources, 'public/assets');
const page = resolve(target.resources, 'template/playground.html');
console.log(`Deploying (${target.name}): ${src}/assets → ${assets}`);
mkdirSync(assets, { recursive: true });
cpSync(resolve(src, 'assets'), assets, { recursive: true });
console.log(`Deploying (${target.name}): ${src}/index.html → ${page}`);
cpSync(resolve(src, 'index.html'), page);
if (target.syncHelp) {
  const help = resolve(target.resources, 'help');
  console.log(`Mirroring help pages (${target.name}): ${HELP_SOURCE} → ${help}`);
  rmSync(help, { recursive: true, force: true });
  mkdirSync(help, { recursive: true });
  cpSync(HELP_SOURCE, help, { recursive: true });
  console.log(`Mirrored ${readdirSync(help).length} help pages`);
}
