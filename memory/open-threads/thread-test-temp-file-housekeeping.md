- [ ] **Unit tests leave no temporary files behind (Eric, 2026-10-06) - both repos, PRs open: Java #516, mercury #360
  (Increment 162).** Measured module by module and binary by binary (a marker file, each suite, then `find -newer` over the temp
  folders). Java main left the engine module's 9 Playground drafts and 2 suspend records, 8 HTTP upload files, 298 Kafka files, a
  Redis dump, and one empty `<app>-<origin>` folder per test JVM in `/tmp/reactive`; a Rust workspace run left 36 entries in the
  system temp folder and one `<app>-<uuid>/RUNNING` folder per default-store binary (33). **Eric's rulings:** the elastic store's
  shutdown hook removes its emptied folder (both engines; kept with `running.in.cloud`); `AsyncHttpClient` deletes an upload's temp
  files when the exchange ends (its comment said so, the code did not); the Rust exit cleanup lives in test code, not in the engine
  (`mercury-test-support`: `temp_path`, `run_at_exit`, one `atexit` hook; platform-core stays free of `unsafe`). Also: the Kafka and
  Redis helpers remove their data folder after stopping, the engine tests close their sessions and remove their suspend records,
  the Rust lifecycle cleanup test runs in a process of its own. Verified: both suites leave nothing (Java 36 modules + the two
  upload modules after the client fix; Rust 116 binaries and `cargo test --workspace`). Next: the merges, then close this thread.
  Rule: [[conv-tests-remove-temp-files]].
  → serves: vision-mercury-composable
  <!-- id: test-temp-file-housekeeping | created: 2026-10-06 | last_used: 2026-10-06 | uses: 1 | tier: working | origin: 2026-10-06-045656 -->
