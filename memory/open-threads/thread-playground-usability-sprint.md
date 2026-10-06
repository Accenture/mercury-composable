- [x] **Playground UI usability sprint (Eric, 2026-10-03). CLOSED 2026-10-03 (Eric: delivered).** PR #500 (squash `856e084b`) with the Rust twin mercury #350 (merge
  `dae6377d`, Increment 155) and the Sonar follow-up #501 (squash `f46f98f6`): a graph JSON file dropped on the Graph view or picked with Import Graph becomes the session
  draft through `POST /api/graph/import/{id}`, which travels like a command; Download saves `<graph-id>.json`; the "Graph Data (Raw)" tab is "Raw"
  ([[playground-file-import-download]]). Lesson: a dev-route addition touches every pre-wired `rest.yaml` (five Java copies, three Rust) plus the guide excerpt, and a
  `<dialog>` under the webapp's global margin reset needs `margin: auto` to center. origin: 2026-10-03-153654; close 2026-10-03-153654.
  → serves: vision-mercury-composable
  <!-- id: playground-usability-sprint | created: 2026-10-03 | last_used: 2026-10-03 | uses: 1 | tier: archive-candidate | origin: 2026-10-03-153654 -->
