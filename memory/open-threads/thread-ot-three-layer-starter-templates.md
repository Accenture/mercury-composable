- [ ] **First-class starter templates for the three layers.** Eric's ruling (2026-09-10,
  the fresh-agent greenfield discussion): "A first class template for the 3 layers is the
  right move." Design decisions RULED 2026-09-11 (Eric): templates live IN-REPO under
  `templates/` (reactor/workspace-built so every build proves them; standalone build files
  so a copy builds anywhere); each ships an AGENTS.md that OFFERS AI-enablement (no
  pre-committed memory/); the Java trio carries BOTH Maven and Gradle build files (the
  Gradle-or-Maven choice applies to templates only — discharges the consumer half of the
  old add-gradle-build backlog); source files carry a one-line scaffold attribution
  instead of the Apache/Accenture header (field applications are not open source).
  IMPLEMENTED 2026-09-11: starter-function / starter-flow / starter-graph in both engines,
  each tested end to end (Maven + Gradle green on Java; cargo test + clippy green on
  Rust); ai-developer-guide playbook step 4 scaffolds from them; ci.yml gained a
  templates-gradle job (Mercury is NOT on Maven Central — mavenLocal resolves engine
  artifacts). RELEASE-SWEEP consequence recorded as [[conv-template-version-sweep]].
  Remaining: PR + merge; then close.
  <!-- id: ot-three-layer-starter-templates | created: 2026-09-10 | last_used: 2026-09-11 | uses: 2 | tier: working | origin: 2026-09-10-234940 -->
