- [ ] **(drift) The Java Playground's committed webapp bundle lacks the `CONDITION` and `DECIMAL` help that its help sources carry.** Found 2026-10-02 while closing the Rust twin (mercury #346, whose bundle had the
  same gap). `system/minigraph-playground-engine/src/main/resources/public/assets/index-CAGJOSB4.js` was last regenerated 2026-09-23 (`b5850be5`, #456); four commits then edited `help graph-math.md` (107 lines
  added, 7 removed): #462 `CONDITION`, #467 the closed dialect, #471 `DECIMAL`, #480 the money loop. Checked with a positive control: the bundle holds the embedded `statement[]=COMPUTE` lines (6) and no `DECIMAL` or
  `CONDITION` text at all, while the help source has `statement[]=DECIMAL` and `statement[]=CONDITION` (2 lines each); no other help page and nothing under `webapp/` changed since. The fix is the Rust PR's recipe:
  `npm ci` then `npm run release` in `system/minigraph-playground-engine/webapp`, a determinism check first (a rebuild of the unchanged sources should reproduce every other committed asset byte for byte), then the hashed
  bundle and the re-pointed `resources/template/playground.html` in one PR with a CHANGELOG line. A code change, so its own PR: Eric decides.
  → serves: vision-mercury-composable
  <!-- id: playground-bundle-stale | created: 2026-10-01 | last_used: 2026-10-01 | uses: 1 | tier: working | origin: 2026-10-02-010808 -->
