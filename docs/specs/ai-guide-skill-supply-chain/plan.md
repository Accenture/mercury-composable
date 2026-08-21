# Plan: Runtime-served Mercury operational contract

- **Spec:** [`spec.md`](spec.md)
- **Status:** Done

> **Plan contract:** one pull request replaces the abandoned six-skill implementation
> with one contract artifact, one product command service, modular providers, and one
> deterministic offline skill export.

## Approach

First remove only the earlier session artifacts that implement the rejected six-skill
supply chain, retaining the corrected REST flow binding. Then add the `contracts/` Maven
module with a small provider API, canonical skill resources, read-only command registry,
and local Java exporter. Finally register providers in the three runtime layers and extend
MiniGraph's existing command dispatcher with three vocabulary-compatible read-only
commands. Verification drives the actual packaged resources and runtime command path, not
a repository scanner.

## Constraints

- No new third-party dependency; use Java 21 and the existing Maven reactor.
- One command authority, multiple module providers.
- One generated skill; generated output is test/temp output, not a committed vendor adapter.
- The generator receives one trusted existing root, atomically reserves the fixed
  `<root>/mercury-platform/` destination, writes only create-new/no-follow content, and
  publishes `manifest.json` last. It never overwrites an existing destination.
- MiniGraph remains development-only, keeps all current graph commands unchanged, and does
  not expose filesystem export through its companion path.
- `platform-contracts` has no Mercury runtime dependency. Platform Core owns only the
  private service adapter; Platform Core, Event Script, and MiniGraph each depend directly
  on the contract API for their provider.
- `docs/llms.txt` remains public-site discovery material and is excluded from the offline
  skill because it points outside the pinned snapshot.

## Construction tests

- **TDD:** provider uniqueness, deterministic ordering, compile-time anchor reporting,
  resource completeness, byte-stable rendering, and safe new-directory export.
- **Focused integration:** platform-core ServiceLoader discovery; Event Script and MiniGraph
  provider discovery with their real classes; development-only service registration;
  production-mode absence; MiniGraph command dispatch for help/list/describe.
- **Manual QA:** invoke the built local exporter once against a new temporary root; inspect
  the exported `mercury-platform/SKILL.md`, verify its manifest hashes, and open referenced
  files offline. Exercise the real help/list/describe commands separately. Recompute each
  raw-file hash and the spec's sorted `path\nfile-hash\n` snapshot digest, and reject
  undeclared files.
- **Goal checks:** strict MkDocs and available focused Maven/reactor tests.

## Tasks

### T0: Remove the abandoned implementation while retaining the drift fix

**Tests:** `git diff` contains no six-skill manifest/scanner, live-operator helper,
CODEOWNERS bootstrap, research comparison artifact, or fixture machinery whose only
purpose was that design. The overview and Event Script guides still require both REST
flow-binding fields, and the canonical REST fixture remains loaded through `RoutingEntry`.

**Approach:** remove `.github/CODEOWNERS`, `scripts/agent_skills/`, the old agent-skill
CODEOWNERS/manifest tests and scripts, the comparison matrix, the function/Event Script/
MiniGraph guide fixtures and their registrations/includes, and the obsolete `.loop-run`
ignore. Retain the REST binding fixture/test and its guide includes, the corrected Event
Script wording, the revised spec/plan/ADR, memory state, and immutable session logs.

### T1: Build the first-class contract artifact and pure Java generator

**Depends on:** T0

**Tests:** red-green tests for resource inventory, duplicate and mixed-version provider
rejection, deterministic provider ordering, identical renders, full link/include closure,
manifest/security metadata, no-follow confinement, atomic destination reservation,
manifest-last publication, stable redacted errors, and refusal to overwrite or escape the
trusted export root even when another process wins the destination race.

**Approach:** add `contracts/` before `platform-core` in the Maven reactor. Package the
canonical `SKILL.md`, the full linked Mercury guide set, and the focused grammar/catalog
references while excluding `docs/llms.txt`. Implement the provider contract, registry,
command results, manifest hashing, and exporter with the Java standard library. Use the
fixed source map and anchors in the spec. Expand MkDocs includes, verify link closure and
  all non-manifest bytes, then reserve the final directory and atomically create its manifest
  last through a no-replace hard link from a verified same-filesystem staging file (failing
  and cleaning up when that publication primitive is unsupported); the
CLI takes one existing root and performs one local export.

**Completion gate:** the isolated contract module packages the complete offline skill;
unit tests pass without loading a Mercury runtime module or using network access.

### T2: Anchor runtime modules and reuse MiniGraph's command service

**Depends on:** T1

**Tests:** each module provider compiles against the behavior classes it names, declares
the exact contract build identity, and is ServiceLoader-discoverable. Platform Core tests
prove the private service registers only for `app.env=dev`. MiniGraph integration tests
drive `help mercury`, `list contracts`, and `describe contract <id>` without regressing
existing graph-help and graph-export behavior or exposing the new skill exporter.

**Approach:** register one private `platform.command.service` adapter in `platform-core`
with the repository's existing `@OptionalService("app.env=dev")` convention; add direct
contract dependencies and providers for platform-core/REST automation, Event Script, and
MiniGraph. Delegate the three read-only forms from `GraphCommandService`. Update the
MiniGraph help resource, command reference, `minigraph-commands.json`, command suggestions,
and parity tests together. Translate all failures to the bounded public codes in the spec.

**Completion gate:** all expected providers are discoverable on the assembled test
classpath, a deliberately mismatched provider fails closed, and no production or companion
path can write a skill snapshot.

### T3: Prove the packaged, offline developer journey

**Depends on:** T2

**Tests:** build the relevant modules; invoke the real local exporter; validate the exported
skill structure, security metadata, internal links, file hashes, and snapshot hash; compare
two exports byte for byte; run strict MkDocs and the focused runtime tests.

**Approach:** document the runtime retrieval/export commands in the AI developer guide and
record the exact verification commands. Keep distribution outside this repository deferred.

## Rollout

Ship T0-T3 together in one maintainer PR. There is no staged multi-phase rollout and no
bot-authored follow-up. Reverting the PR removes the command/provider/export mechanism as one
unit and leaves the existing human documentation intact.

## Risks

- A ServiceLoader registration can disappear during shading; packaged-JAR tests enumerate
  providers and fail if an expected module is missing.
- A central command service can become a miscellaneous toolbox; version 1 accepts only the
  three read-only contract commands and module providers cannot add commands or effects.
- Documentation copied into a runtime artifact can contain links outside the snapshot;
  all relative links and includes are verified closed, while absolute public URLs remain
  informational and the skill explicitly forbids relying on network retrieval.
- MiniGraph is not installed by every platform-core consumer; the authoritative Java API
  and local exporter remain available without MiniGraph, while MiniGraph is only the first
  read-only frontend.

## Changelog

- 2026-08-21: Replaced the six-skill repository generator and live-operator design with an
  approved first-class contract artifact, one read-only product command service, modular
  compile-time behavior anchors, MiniGraph vocabulary, and one local pure-Java offline skill
  exporter.
- 2026-08-21: Replaced the non-portable atomic-directory-move claim with exclusive final
  directory reservation plus manifest-last publication, and expanded the artifact to the
  approved full offline guide closure.
