import unittest
import importlib.util
import os
import tempfile
from pathlib import Path
import sys

# Load memory-lint.py dynamically since it has a hyphen in the name
script_path = Path(__file__).parent / "memory-lint.py"
spec = importlib.util.spec_from_file_location("memory_lint", str(script_path))
memory_lint = importlib.util.module_from_spec(spec)
sys.modules["memory_lint"] = memory_lint
spec.loader.exec_module(memory_lint)

class TestMemoryLint(unittest.TestCase):
    def test_pinned_open_threads_flat(self):
        text = """
- [ ] Parent task
  <!-- id: t1 -->
- [x] Done task
  <!-- id: t2 -->
"""
        self.assertEqual(memory_lint.pinned_open_threads(text), {"t1"})

    def test_pinned_open_threads_nested(self):
        # Nested list inside an open thread
        text = """
- [ ] Parent task
  - Subtask 1
  - Subtask 2
  <!-- id: t3 -->
"""
        self.assertEqual(memory_lint.pinned_open_threads(text), {"t3"})

    def test_pinned_open_threads_nested_open(self):
        text = """
- [ ] Parent task
  - [ ] Nested open
    <!-- id: t4 -->
"""
        self.assertEqual(memory_lint.pinned_open_threads(text), {"t4"})

    def test_pinned_open_threads_sibling_reset(self):
        text = """
- [ ] Parent task
  <!-- id: t5 -->
- Regular bullet that should reset
  <!-- id: t6 -->
"""
        self.assertEqual(memory_lint.pinned_open_threads(text), {"t5"})

    def test_pinned_open_threads_mixed(self):
        text = """
- [ ] Open task 1
  - Subtask
  <!-- id: mix-1 -->
- [x] Done task
  <!-- id: mix-2 -->
- [ ] Open task 2
  <!-- id: mix-3 -->
- Regular sub-bullet
  <!-- id: mix-4 -->
"""
        self.assertEqual(memory_lint.pinned_open_threads(text), {"mix-1", "mix-3"})

    def test_memref_ids_ignores_prose_and_review_summary(self):
        # The ot-review-step6-prose livelock: a fact named only in prose / a
        # '## Memory Review' summary is NOT a use — only '## Memory References' counts.
        text = """# Session
## What happened
Archiving `foo-fact` because it is overdue.
## Memory Review (2026-06-19)
- Archived: 1 (`foo-fact` -> archive, faded)
- Tier changes: foo-fact archive-candidate->archived
## Memory References
- Created: bar-fact
- Referenced: baz-fact
"""
        ids = memory_lint.memref_ids(text)
        self.assertIn("bar-fact", ids)
        self.assertIn("baz-fact", ids)
        self.assertNotIn("foo-fact", ids)  # prose / review-summary mention is not a reference

    def test_memref_ids_bounded_by_next_heading(self):
        text = """## Memory References
- Referenced: in-block-id
## Next Section
- not-a-ref-id mentioned here
"""
        ids = memory_lint.memref_ids(text)
        self.assertIn("in-block-id", ids)
        self.assertNotIn("not-a-ref-id", ids)


class TestDanglingCrossFile(unittest.TestCase):
    # Regression for the dangling-link false positive: a supersession target whose
    # footer lives in another memory/*.md (e.g. vision.md) must resolve, not warn.
    # The bug was in load_repo (it only pooled continuity + archive footers), so this
    # exercises load_repo end-to-end against a temp memory/ layer, not check_dangling alone.
    @staticmethod
    def _write(path, text):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(text)

    def test_supersession_target_in_vision_is_not_dangling(self):
        with tempfile.TemporaryDirectory() as root:
            mem = os.path.join(root, "memory")
            self._write(os.path.join(mem, "continuity.md"), """# Continuity
## Open Threads
- [x] Old vision retired
  <!-- id: old-fact | created: 2026-06-19 | last_used: 2026-06-19 | uses: 1 | tier: superseded | superseded-by: new-fact -->
- [x] Orphaned link
  <!-- id: orphan-fact | created: 2026-06-19 | last_used: 2026-06-19 | uses: 1 | tier: superseded | superseded-by: ghost-fact -->
""")
            self._write(os.path.join(mem, "vision.md"), """# Vision
<!-- id: new-fact | created: 2026-06-19 | last_used: 2026-06-19 | uses: 1 | tier: core -->
""")
            os.makedirs(os.path.join(mem, "sessions"), exist_ok=True)

            cont, pinned, arch, extra, sessions, refs = memory_lint.load_repo(root)
            # the vision fact is available for link resolution but NOT counted as a fact
            self.assertIn("new-fact", extra)
            self.assertNotIn("new-fact", cont)
            self.assertNotIn("new-fact", arch)

            warns = memory_lint.check_dangling({**cont, **arch, **extra})
            # superseded-by a vision.md fact resolves -> no dangling
            self.assertFalse(any("old-fact" in w for w in warns), warns)
            # a genuinely missing target still dangles (negative control)
            self.assertTrue(any("orphan-fact" in w and "ghost-fact" in w for w in warns), warns)


if __name__ == "__main__":
    unittest.main()
