- [ ] **An in-house JSONPath engine to RFC 9535 replaces Jayway JsonPath** (→ proposal: RFC-0010; serves [[vision-mercury-composable]]).
  - **Why:** `json-smart` 2.6.0 is reported vulnerable (CVE-2026-113602), has no fixed release, and json-path 3.0.0's filter
    engine calls its parser directly, so the dependency cannot be bumped or excluded away.
  - **Shape:** a module `system/minimalist-jsonpath` evaluating RFC 9535 over the engine's own trees.
  - **Rules kept:** Mercury's result shape and hyphens in dotted names.
  - **Refusal by name** of Jayway-only syntax, also at deployment.
  - **Substitution as data:** a `{namespace.key}` value renders as a literal, which closes a JSONPath injection.
  - **Proof:** the RFC 9535 compliance suite (715 cases) as the shared oracle for both engines.
  - **Eric (2026-10-22): RFC 9535 is preferred.** Jayway predates the standard, and moving Java to it is a transition.
  - **Eric's answers (2026-10-22):**
    - module `minimalist-jsonpath`, package `org.platformlambda.mini.jsonpath`;
    - **ships in v4.12.26**, swapping and refusing in one release (no warning release);
    - strict RFC 9535, no alternative to `in`;
    - Event Script's `{model.key}` substitution yields a concrete value, never a JSONPath query. The flow compiler refusing a
      model reference inside a `$` source is proposed, to confirm.
    - [[json-path-boot-bom-override]] closes with the swap.
  - **Next:** RFC-0010's PR (#577), then WP1, the module and the suite.
  - If accepted, it makes [[json-path-boot-bom-override]] moot. Relates [[conv-library-pins-are-direct-dependencies]].
  <!-- id: minimalist-jsonpath | created: 2026-10-22 | last_used: 2026-10-22 | uses: 1 | tier: working | origin: 2026-10-22-040817 -->
