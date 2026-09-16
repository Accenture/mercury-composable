- [x] (deferred → **CLOSED 2026-09-16, Eric**) **Publish the Java engine to Maven Central.** Not
  doing it: the field consumes Java releases through the **enterprise artifactory**, and that is
  the *preferred* path, not merely the current one — the governed pipeline gives the field
  visibility into security vulnerabilities and code-quality findings that Central publication would
  not. This confirms rather than contradicts the thread's own framing ("Central is reach/OSS
  convenience rather than a delivery need").
  **If it is ever picked up**, the prerequisites were already scoped and are worth not re-deriving:
  Sonatype Central account + `com.accenture` namespace verification, GPG signing, javadoc/sources
  jars, and a pom metadata audit (licenses/scm/developers); the Rust/python/node publication lessons
  (name and burst policies, artifact-tag correspondence) carry over.
  origin: `memory/sessions/2026-09-02-005936.md`
  <!-- id: ot-maven-central-later | created: 2026-09-02 | last_used: 2026-09-02 | uses: 1 | tier: archive-candidate | origin: 2026-09-02-005936 -->
