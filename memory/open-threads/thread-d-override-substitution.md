- [ ] **A `-D` runtime override's value is resolved with the same `${ENV_VAR:default}` substitution as a file value, in all
  four runtimes (Eric's request, 2026-11-17).** The packs met the asymmetry wiring the transcript audit store's database
  password: a system property was returned as written in Java, and the Rust port and both packs mirrored it. Built the
  same day, Java first, four PRs opened 2026-11-17: this repo #640 (`ConfigReader.resolveValue`), mercury #413
  (Increment 203, `ConfigReader::resolve_text`), mercury-python #60 and mercury-nodejs #127 (`AppConfig.get` on an
  override), each with a test, the configuration docs sentence and a CHANGELOG entry. Open: the four merges; then
  the packs' helper READMEs may drop the "a -D value is taken as written" caveat.
  → serves: vision-mercury-composable
  <!-- id: d-override-substitution | created: 2026-11-17 | last_used: 2026-11-17 | uses: 1 | tier: working | origin: 2026-11-17-084155 -->
