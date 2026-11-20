- [ ] **(blueprint) Raise the minimum Java to 25 (planned 2026-11-20, Eric; approval pending).** When approved: the
  reactor's compiler release and the toolchain pins, the CI matrix (the `Build & Unit Tests (Java 21)` job and the
  starter-template checks), the starter templates' Gradle and Maven files, the docs that name Java 21 as the baseline
  ([[stack-language-java21]]), and then the lift of [[conv-reentrantlock-not-synchronized]] - JEP 491 makes
  `synchronized` non-pinning on JDK 24+, so the `ReentrantLock` rule becomes a preference, not a requirement; the lock
  code written under it (#661, #662) stays as it is. Until approval nothing changes: the rule holds and the build stays
  on 21. Status: planned, awaiting Eric's approval; the trigger used to be field adoption of Java 25.
  → serves: vision-mercury-composable
  <!-- id: java25-baseline | created: 2026-11-20 | last_used: 2026-11-20 | uses: 1 | tier: working | origin: 2026-11-20-104438 -->
