- [x] **Add Gradle build support — COMPLETE (Eric's ruling, 2026-09-11).** Delivered on the
  consumer side by the starter templates (PR #357): each `templates/*` project ships both
  Maven and Gradle build files, CI-verified by the templates-gradle job. Eric ruled the
  Gradle-or-Maven choice applies to TEMPLATES ONLY — the engine's multi-module Maven
  reactor stays as-is by design, so no engine-side Gradle work remains. Durable lesson:
  Mercury artifacts are not on Maven Central, so template Gradle builds resolve engine
  artifacts from mavenLocal. origin: 2026-09-11-005808
  <!-- id: thread-add-gradle-build | created: 2026-06-24 | last_used: 2026-09-11 | uses: 2 | tier: active -->
