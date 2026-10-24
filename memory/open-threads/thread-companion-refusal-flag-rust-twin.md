- [x] **The Rust engine's companion endpoint reads the engine's refusal flag** (the twin of PR #586; closed 2026-10-24).
  - **Outcome:** mercury #394 (merge `5ddb994f`, Increment 188): `refuse()` at 35 call sites, `CaptureSink` reads the flag, the
    word list removed. The Java guides lost their Java-only qualifier, and both claims registries pin `companion-ok-is-the-engine-verdict`.
  - **Lesson:** run the twin's tests against the port's old code before assuming the defect is shared: it confirmed the same nine
    misread help pages, and showed `{not json}` was already `ok: false` on Rust.
  - origin: 2026-10-24-023905 (closed in 2026-10-24-040441)
  <!-- id: companion-refusal-flag-rust-twin | created: 2026-10-24 | last_used: 2026-10-24 | uses: 1 | tier: working | origin: 2026-10-24-023905 -->
