- [ ] **AI SDLC sprint 2 - document ingestion into executable rule graphs** (serves [[bp-agent-orchestration]]). The design:
  typed parts by reference in the LLM helper (a file resolver under a configured root and an S3 resolver first, sha256,
  ceilings checked before encoding), the manifest of per-block outcomes (roles with a relevance axis against the declared
  processing scope, profiles per document type, graph thresholds deciding status and next), a deterministic segmenter (pass 0)
  and an Excel parser as common tools, pass 2 reconcile on the blocks alone. Next: the segmenter prototype and a field-level
  agreement run; an RFC in the register for the helper contract and the manifest; a higher model when one is available to
  the lab. The lab lives OUTSIDE the repositories (client-proprietary
  inputs under neutral names; nothing from them enters a repo).
  <!-- id: ai-sdlc-document-ingestion | created: 2026-10-16 | last_used: 2026-10-16 | uses: 1 | tier: working | origin: 2026-10-16-012720 -->
