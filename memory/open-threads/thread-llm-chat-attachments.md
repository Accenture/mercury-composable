- [ ] **Attachments on a single `llm.chat` call: images and PDF pages by reference (RFC-0016, raised 2026-11-24 from a
  field request, mercury-python #78).** The helper takes text only, on purpose; a bounded AI node that must read a
  page has no way to show it to the model. Round 1, drafted and not ruled: `attachments` on a single `llm.chat` call,
  each a path or `{path, pages}` inside one root (`llm.attachments.root`) under the agent tools' workspace rules; the
  kind decided by the first bytes; base64 `image` and `document` blocks before the last user turn's text; the
  provider's limits checked by the helper (the count, the source bytes, an image's size by backend); the audit keeping
  a reference with the source file's SHA-256.
  - **Amended from the request:** one root, not several; a missing root is a 503 and a bad one stops the application
    at start; reading and cutting inside the call's deadline; the hash taken as the bytes are read, not by a second
    read in `transcript.audit`; the root stated as readable through the model by every caller of the helper.
  - **Measured facts behind the limits (the provider's pages, read 2026-11-24):** an image at most 10 MB
    base64-encoded on the Claude API and 5 MB on Amazon Bedrock; more than 20 image and document blocks in a request
    tighten each image's dimension limit; 600 PDF pages a request, 100 on a model whose context window is under 1M
    tokens; no encrypted PDF; base64 sources only on Bedrock, which has no Files API.
  - **For the maintainer's review:** one root or several; the bound on source bytes or on bytes sent; a page limit of
    the helper's own; `llm.stream`; the Node twin's PDF library and what a shared vector can assert of a cut document;
    the `documents` extra or one of the helper's own.
  - **Raised as PR #668, MERGED 2026-11-24** (squash `cc16cfbc0`). **Build ordered the same day** (the maintainer:
    "proceed with implementation for RFC 0015 and 0016"), under the entry's defaults, which stay open to the review.
    **Step 1 BUILT 2026-11-24:** mercury-python #82 (MERGED 2026-11-24, merge `c367951`, on top of #81 of
    [[llm-chat-single-call-audit]]) - the
    field, the one root, the blocks, the limits by backend, the reference in the audit; a new shared vector file of
    48 cases over eight fixture files, six tests, thirty-one mutations each caught. No live call was made, and the
    measurement of a PDF cut beside a stream is not done.
  - **The maintainer's direction (2026-11-24), which answers the first open question:** the shared folder is
    configured in the helper and filled before a graph runs; a later iteration lets the helper download from an
    object store into it, for PDFs, images, spreadsheets and other inputs, and until then an external script does.
    The audit keeps a reference and the hash, never the file; an installation that must reconstruct an exchange
    keeps its files unchanged for the audit's retention. A spreadsheet is not a kind a document block takes and is
    converted first. Written into the entry as round 2, where option E now reads the object store as the source
    that fills the folder.
  - **Step 2 BUILT 2026-11-24:** mercury-nodejs #145 (MERGED 2026-11-24, merge `d8713c0`) - the Node twin from the
    same vector file and fixtures with no case or message changed; it reads in a worker thread and cuts pages with
    `@cantoo/pdf-lib`, the maintainer's choice; thirty-three mutations each caught. The entry's resolution records
    it, and the Node twin's PDF library leaves the entry's open questions.
  - **Next:** the schema page, the measurement and a live drive. No design spec is
    written. The request's reference implementation is not published.
  → proposal: RFC-0016
  → serves: vision-mercury-composable
  <!-- id: llm-chat-attachments | created: 2026-11-24 | last_used: 2026-11-24 | uses: 1 | tier: working | origin: 2026-11-24-051753 -->
