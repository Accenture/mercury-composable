- [x] **Simple-plugin allowlist gate: method bodies scanned — SHIPPED.** PR #358 (squash
  `3c751f14`, merged 2026-09-11): RecursiveClassTypeExaminer gained a real MethodVisitor
  (call/field owners, new/cast types, invokedynamic + ConstantDynamic handles, class
  literals, try/catch, throws); SKIP_CODE dropped; TypeConversionUtils +
  KeyNormalizationUtils allowlisted; two soundness rules (analyzed classes excluded from
  the disallowed set; nested classes recursively analyzed). SimplePluginGateTest proves
  skip/register through the real loader; registry ≥ 50 pins the built-ins; the
  flow-schema-reference sentence states the enforced behavior. Durable lesson: a
  containment allowlist that skips the code attribute is declared, not enforced — the
  body scan is what makes it real. Java-only (Rust has no runtime plugin loading).
  origin: 2026-09-11-024330
  <!-- id: ot-simple-plugin-gate-body-scan | created: 2026-09-10 | last_used: 2026-09-11 | uses: 3 | tier: active | origin: 2026-09-10-150848 -->
