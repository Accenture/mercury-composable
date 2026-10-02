- [x] **(drift) The Java Playground's committed webapp bundle lacked the `CONDITION` and `DECIMAL` help that its help sources carry. CLOSED 2026-10-02.** PR #491 (squash `f9ddf3c7`) regenerated the bundle
  (`npm ci`, `npm run release`; `index-CAGJOSB4` became `index-r5ZZ6DXj`, source map included) and, at Eric's request, put `DECIMAL` and `CONDITION` into the page's property list and corrected the webapp's scoped
  instructions (bundle to `public/`, only `index.html` to `template/playground.html`). A rebuild reproduced the other 14 files byte for byte; net 110 lines added, 8 removed. Lesson: a help edit is not done until
  the bundle is rebuilt and committed, and CI never builds the webapp ([[help-edit-needs-bundle-release]]). origin: 2026-10-02-010808; close 2026-10-02-015208.
  → serves: vision-mercury-composable
  <!-- id: playground-bundle-stale | created: 2026-10-01 | last_used: 2026-10-02 | uses: 2 | tier: active | origin: 2026-10-02-010808 -->
