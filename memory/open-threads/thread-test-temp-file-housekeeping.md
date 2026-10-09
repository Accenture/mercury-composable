- [x] **Unit tests leave no temporary files behind (Eric, 2026-10-06) - DONE: #516 squash `fd02ca6e` and mercury #360 merge
  `acf767d7` (Increment 162), both MERGED 2026-10-06.** Both engines' shutdown cleanup removes its emptied holding folder,
  `AsyncHttpClient` deletes an upload's temp files when the exchange ends, the Kafka and Redis helpers remove their data folder, the
  tests close their sessions and remove their records, and Rust tests use `mercury-test-support` (`temp_path`, `run_at_exit`);
  measured afterwards, neither suite leaves anything. Lesson: measure leftovers per module or binary (a marker file, then `find
  -newer`); the measurement found what reading missed, an HTTP comment the code contradicted and a Rust test removing a folder its
  siblings shared. Rule: [[conv-tests-remove-temp-files]]. origin: 2026-10-06-045656, closed in 2026-10-06-054340.
  → serves: vision-mercury-composable
  <!-- id: test-temp-file-housekeeping | created: 2026-10-06 | last_used: 2026-10-06 | uses: 3 | tier: archive-candidate | origin: 2026-10-06-045656 -->
