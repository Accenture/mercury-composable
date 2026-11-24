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
  - **Build ordered 2026-11-24** (the maintainer: "proceed with implementation for RFC 0015 and 0016"): step 1 is the
    Python pack, after [[llm-chat-single-call-audit]] (the audit's part depends on it), under the entry's defaults,
    which stay open to the review; then the Node twin, the READMEs and the schema page, the measurement and a live
    drive. No design spec is written. The request's reference implementation is not published.
  → proposal: RFC-0016
  → serves: vision-mercury-composable
  <!-- id: llm-chat-attachments | created: 2026-11-24 | last_used: 2026-11-24 | uses: 1 | tier: working | origin: 2026-11-24-051753 -->
