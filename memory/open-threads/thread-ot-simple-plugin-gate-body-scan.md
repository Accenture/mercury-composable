- [ ] (hardening) **Simple-plugin allowlist gate: scan method bodies, not just signatures**
  (Eric's backlog ruling 2026-09-10: after the v4.12.6 release). Finding (field-spotted by
  Eric, confirmed in code): `SimplePluginLoader` parses plugin classes with
  `ClassReader.SKIP_CODE` and `RecursiveClassTypeExaminer.visitMethod` returns null, so the
  ALLOWED_PACKAGES check sees only superclass/interfaces/field/method-signature types —
  method-BODY references are invisible. That is why `TypeConversionUtils` (since the gate's
  birth) and `KeyNormalizationUtils` (v4.12.6) pass unlisted, and why a USER plugin could
  call java.io/java.net/threads inside `calculate()` and still register: the sub-millisecond
  containment the allowlist exists for is declared, not enforced. Agreed fix shape: give the
  examiner a real MethodVisitor (method-call owners, field owners, new/cast/instanceof
  types, invokedynamic handles, class-literal constants, try/catch types), drop SKIP_CODE
  (keep SKIP_DEBUG|SKIP_FRAMES); add TypeConversionUtils + KeyNormalizationUtils to
  ALLOWED_PACKAGES (with body scanning on, the ~50 built-ins all pass through
  `shouldRegisterPlugin` and become the regression net); align the flow-schema-reference
  sentence ("Mercury framework packages") with the enforced list. Java-only — the Rust
  engine's no-allowlist divergence stands (compiled/linked code, no runtime class loading).
  Not a release blocker: the gap predates v4.12.6 entirely.
  <!-- id: ot-simple-plugin-gate-body-scan | created: 2026-09-10 | last_used: 2026-09-10 | uses: 1 | tier: working | origin: 2026-09-10-150848 -->
