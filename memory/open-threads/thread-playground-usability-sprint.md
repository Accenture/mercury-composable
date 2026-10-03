- [ ] **Playground UI usability sprint (Eric, from 2026-10-03)** - a run of small usability items for the MiniGraph Playground, one source for both engines
  ([[playground-webapp-single-source]]). Done in PR #500 (squash `856e084b`) / mercury #350 (merge `dae6377d`), both MERGED 2026-10-03: (1) a graph JSON file dragged from the desktop or Finder onto the Graph view
  imports it as the session draft; (2) a Download icon beside Copy saves the model as `<graph-id>.json`; (3) an Import Graph button opens the file picker; (4) the "Graph Data (Raw)"
  tab is "Raw" - all through the new `POST /api/graph/import/{id}`, which travels like a command ([[playground-file-import-download]]). Next: the items Eric brings next (the
  next engine release ships the endpoint on both engines). Not built: a CI step that rebuilds the bundle and compares it with the committed copy.
  → serves: vision-mercury-composable
  <!-- id: playground-usability-sprint | created: 2026-10-03 | last_used: 2026-10-03 | uses: 1 | tier: working | origin: 2026-10-03-153654 -->
