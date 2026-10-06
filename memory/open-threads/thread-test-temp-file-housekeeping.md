- [ ] **Unit tests leave no temporary files behind (Eric, 2026-10-06) - both repos, in progress.** Measured module by module (a
  marker file, each module's tests, then `find -newer` over the temp folders): Java main left the engine module's 9 Playground drafts
  in `/tmp/graph` and 2 suspend records, 8 HTTP upload files in `/tmp/async-http-temp` (platform-core, lambda-example), 298 Kafka files
  in `/tmp/kafka-logs` (kafka-connector), a Redis dump in `/tmp/soa-redis`, and one empty `<app>-<origin>` folder per test JVM in
  `/tmp/reactive`; one Rust workspace run left 36 per-process folders and `rest-*.yaml` files in the system temp folder (233 had built
  up) and `<app>-<uuid>/RUNNING` folders in `/tmp/reactive`. **Java branch `test/remove-temporary-files`** (a worktree, three commits on
  `eb6021a9`): the elastic store's shutdown hook removes its emptied `<app>-<origin>` folder (Eric's ruling; not with
  `running.in.cloud`), the standalone Kafka and Redis helpers remove their data folder once stopped (both wipe it at every start), and
  the engine tests close their sessions and remove their suspend records. **Open, Eric's call:** (1) `AsyncHttpClient` keeps an
  upload's temp file until its 30-minute housekeeper, although its comment says such files are removed immediately after relay - fix
  the client, or have the two tests remove theirs; (2) a Rust test binary never runs `elastic_queue::shutdown_cleanup` (only the
  lifecycle's graceful exit does), and its per-process folders need an exit hook too - register the cleanup at process exit in the
  engine, or give the test binaries their own. Then the Rust twin of the empty-folder rule and the Rust test fixes, and a PR per repo.
  Rule: [[conv-tests-remove-temp-files]].
  → serves: vision-mercury-composable
  <!-- id: test-temp-file-housekeeping | created: 2026-10-06 | last_used: 2026-10-06 | uses: 1 | tier: working | origin: 2026-10-06-045656 -->
