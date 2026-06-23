// test_memory_lint.mjs — node mirror of test_memory_lint.py.
// Same fixtures, same expectations: this is the cross-runtime contract that
// keeps memory-lint.mjs at parity with memory-lint.py. Run: node --test <file>
import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import {
  pinned_open_threads,
  memref_ids,
  load_repo,
  check_dangling,
  check_session_filenames,
} from "./memory-lint.mjs";

function byCodePoint(a, b) {
  if (a < b) return -1;
  if (a > b) return 1;
  return 0;
}
const sortedArr = (s) => [...s].sort(byCodePoint);
const assertPins = (text, expected) =>
  assert.deepEqual(sortedArr(pinned_open_threads(text)), [...expected].sort(byCodePoint));

test("pinned_open_threads flat", () => {
  assertPins(
    `
- [ ] Parent task
  <!-- id: t1 -->
- [x] Done task
  <!-- id: t2 -->
`,
    ["t1"]
  );
});

test("pinned_open_threads nested", () => {
  // Nested list inside an open thread
  assertPins(
    `
- [ ] Parent task
  - Subtask 1
  - Subtask 2
  <!-- id: t3 -->
`,
    ["t3"]
  );
});

test("pinned_open_threads nested open", () => {
  assertPins(
    `
- [ ] Parent task
  - [ ] Nested open
    <!-- id: t4 -->
`,
    ["t4"]
  );
});

test("pinned_open_threads sibling reset", () => {
  assertPins(
    `
- [ ] Parent task
  <!-- id: t5 -->
- Regular bullet that should reset
  <!-- id: t6 -->
`,
    ["t5"]
  );
});

test("pinned_open_threads mixed", () => {
  assertPins(
    `
- [ ] Open task 1
  - Subtask
  <!-- id: mix-1 -->
- [x] Done task
  <!-- id: mix-2 -->
- [ ] Open task 2
  <!-- id: mix-3 -->
- Regular sub-bullet
  <!-- id: mix-4 -->
`,
    ["mix-1", "mix-3"]
  );
});

test("memref_ids ignores prose and review-summary mentions (ot-review-step6-prose)", () => {
  // A fact named only in prose / a '## Memory Review' summary is NOT a use —
  // only '## Memory References' counts.
  const text = `# Session
## What happened
Archiving \`foo-fact\` because it is overdue.
## Memory Review (2026-06-19)
- Archived: 1 (\`foo-fact\` -> archive, faded)
- Tier changes: foo-fact archive-candidate->archived
## Memory References
- Created: bar-fact
- Referenced: baz-fact
`;
  const ids = memref_ids(text);
  assert.ok(ids.has("bar-fact"));
  assert.ok(ids.has("baz-fact"));
  assert.ok(!ids.has("foo-fact")); // prose / review-summary mention is not a reference
});

test("memref_ids is bounded by the next heading", () => {
  const text = `## Memory References
- Referenced: in-block-id
## Next Section
- not-a-ref-id mentioned here
`;
  const ids = memref_ids(text);
  assert.ok(ids.has("in-block-id"));
  assert.ok(!ids.has("not-a-ref-id"));
});

test("check_session_filenames flags date-only names", () => {
  const sessions = ["2026-06-12.md", "2026-06-23.md"];
  const warns = check_session_filenames(sessions);
  assert.equal(warns.length, 2);
  assert.ok(warns.every((w) => w.includes("[date-only-session]")));
});

test("check_session_filenames passes timestamped names", () => {
  const sessions = ["2026-06-23-153401.md", "2026-06-13-011149.md"];
  const warns = check_session_filenames(sessions);
  assert.equal(warns.length, 0);
});

test("check_session_filenames mixed", () => {
  const sessions = ["2026-06-12.md", "2026-06-23-153401.md"];
  const warns = check_session_filenames(sessions);
  assert.equal(warns.length, 1);
  assert.ok(warns[0].includes("2026-06-12.md"));
});

test("supersession target in vision.md is not dangling (cross-file resolution)", () => {
  // Regression for the dangling-link false positive: a supersession target whose
  // footer lives in another memory/*.md (e.g. vision.md) must resolve, not warn.
  // The bug was in load_repo (it only pooled continuity + archive footers), so this
  // exercises load_repo end-to-end against a temp memory/ layer, not check_dangling alone.
  const root = mkdtempSync(join(tmpdir(), "memlint-"));
  try {
    mkdirSync(join(root, "memory", "sessions"), { recursive: true });
    writeFileSync(
      join(root, "memory", "continuity.md"),
      `# Continuity
## Open Threads
- [x] Old vision retired
  <!-- id: old-fact | created: 2026-06-19 | last_used: 2026-06-19 | uses: 1 | tier: superseded | superseded-by: new-fact -->
- [x] Orphaned link
  <!-- id: orphan-fact | created: 2026-06-19 | last_used: 2026-06-19 | uses: 1 | tier: superseded | superseded-by: ghost-fact -->
`
    );
    writeFileSync(
      join(root, "memory", "vision.md"),
      `# Vision
<!-- id: new-fact | created: 2026-06-19 | last_used: 2026-06-19 | uses: 1 | tier: core -->
`
    );

    const { cont, arch, extra } = load_repo(root);
    // the vision fact is available for link resolution but NOT counted as a fact
    assert.ok(extra.has("new-fact"));
    assert.ok(!cont.has("new-fact"));
    assert.ok(!arch.has("new-fact"));

    const warns = check_dangling(new Map([...cont, ...arch, ...extra]));
    // superseded-by a vision.md fact resolves -> no dangling
    assert.ok(!warns.some((w) => w.includes("old-fact")), warns.join("\n"));
    // a genuinely missing target still dangles (negative control)
    assert.ok(
      warns.some((w) => w.includes("orphan-fact") && w.includes("ghost-fact")),
      warns.join("\n")
    );
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});
