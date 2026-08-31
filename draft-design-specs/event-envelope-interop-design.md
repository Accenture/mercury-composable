# Design: Common Event Envelope Wire Format for Cross-Language Interoperability

**Status:** DRAFT for review (Eric)
**Date:** 2026-07-21
**Scope:** Event over HTTP interoperability between mercury-composable (Java) and the
official Rust port (github.com/Accenture/mercury), extensible to future Node.js, Python,
and Go ports.
**Testbed:** the Rust port (read-only for this session; its `/api/event` implementation is
a separate workstream).

---

## 1. Problem

`POST /api/event` (Event over HTTP) carries a serialized `EventEnvelope` as
`application/octet-stream`. The Java implementation serializes with **proprietary
single-character map keys** before MsgPack encoding:

| Compact key | Meaning | | Compact key | Meaning |
|---|---|---|---|---|
| `0` | id | | `S` | status |
| `T` | to | | `H` | headers |
| `F` | from | | `B` | body |
| `R` | reply_to | | `7` | tags |
| `t` | trace_id | | `6` | annotations |
| `p` | trace_path | | `4` | exception (bytes) |
| `s` | span_id | | `5` | stack |
| `X` | cid | | `O` | obj_type |
| `1` | exec_time | | `2` | round_trip |

The Rust port encodes the same envelope with **full field names** via serde
(`rmp_serde::to_vec_named`): `id`, `to`, `from`, `reply_to`, `cid`, `trace_id`,
`trace_path`, `span_id`, `status`, `headers`, `body`, `exec_time`.

The two cannot interoperate today, and the compact keys are undocumented outside the Java
source — a barrier for every future port.

**Key observation:** Java already maintains BOTH representations. `toBytes()`/`load()` use
the compact flags; `toMap()`/`fromMap()` use descriptive keys that are almost exactly the
Rust field names. The common format below is therefore not an invention — it is the
promotion of the existing descriptive form to a documented, normative wire contract.

## 2. Goals / non-goals

**Goals**
1. A language-neutral, self-describing serialized envelope any MsgPack-capable language
   can encode/decode with its idiomatic tooling (serde, msgpack-java, msgpack-lite,
   msgpack-python, vmihailenco/msgpack…) — no custom codec required.
2. Backward compatible: existing Java↔Java Event over HTTP (compact keys) keeps working
   through the transition, with no flag-day.
3. Documented as a spec page that passes the fresh-agent test: a new port implements the
   format from the page alone.

**Non-goals**
- Changing the in-memory `EventEnvelope` API in any language.
- The Kafka service-mesh wire format (same-language mesh; can migrate later).
- Cross-language transport of language-native exception objects (impossible by
  definition; see §3.4).
- Byte-identical encodings across languages (MsgPack map ordering is unspecified; the
  contract is semantic round-trip, not binary equality).

## 3. The interchange format ("standard envelope", v1)

A single MsgPack **map with string keys**. Field registry:

### 3.1 Core fields (all implementations)

| Key | MsgPack type | Encode | Decode | Semantics |
|---|---|---|---|---|
| `id` | str | REQUIRED | REQUIRED | Event instance id (opaque string; UUID recommended) |
| `to` | str | when set | optional | Target route name |
| `from` | str | when set | optional | Sender route name |
| `reply_to` | str | when set | optional | Reply route name |
| `cid` | str | when set | optional | Business correlation id |
| `trace_id` | str | when set | optional | Distributed trace id |
| `trace_path` | str | when set | optional | Trace path, e.g. `GET /api/hello` |
| `span_id` | str | when set | optional | Sender's span id (receiver's parent span) |
| `status` | int | when set | optional, default 200 | HTTP-style status code |
| `headers` | map<str,str> | REQUIRED (may be empty) | REQUIRED | User parameters |
| `body` | any | when set (absent = nil) | optional, default nil | Payload — see §3.3 |
| `exec_time` | float | when set | optional | Function execution ms |
| `round_trip` | float | when set | optional | End-to-end ms |

### 3.2 Extension fields (currently Java-only; optional everywhere)

| Key | MsgPack type | Semantics |
|---|---|---|
| `tags` | map<str,str> | Routing metadata (`optional`, `json`, `broadcast`) |
| `annotations` | map<str,any> | Trace annotations |
| `stack` | str | Stack-trace text — the **portable** error detail |
| `obj_type` | str | Language-specific payload type hint (PoJo class for Java). Advisory; receivers MAY ignore |

### 3.3 Encoding rules

1. **Map keys are strings.** Body maps must also use string keys (Java already
   auto-converts integer keys; other languages must not emit them).
2. **Optional fields:** encoders SHOULD omit unset optional fields; encoding them as nil
   is equally valid. Decoders MUST treat *absent* and *nil* identically.
3. **Required-on-encode:** `id` and `headers` are always present (empty map when there
   are no headers) — this helps statically-typed decoders. `body` is emitted when set;
   decoders default a missing body to nil. (Implementation finding: Java's `MsgPack.packMap`
   skips null map values unless `serializer.null.transport=true`, so a nil body cannot be
   emitted by default — the Rust decoder needs a serde default on `body`.)
4. **Unknown keys MUST be ignored** by decoders (forward compatibility). This is serde's
   default and trivial in dynamic languages.
5. **Numbers:** encoders use the smallest natural MsgPack width; decoders MUST accept any
   integer width for integer fields and float32/float64 for float fields. (Java's
   documented Long→Integer downcast on small values is within spec.)
6. **Binary:** `body` (or any nested value) may be MsgPack `bin`. Strings are UTF-8 `str`.
7. **No MsgPack extension types** in v1 — timestamps travel as ISO-8601 UTC strings with
   millisecond precision (the existing Java `Date`/`Instant` convention), keeping every
   decoder trivial.
8. The envelope is self-contained: no headers/metadata outside the MsgPack bytes are
   required to decode it.

### 3.4 The exception field (deliberately not in the spec)

Java's `exception` ("4") is a Java `ObjectOutputStream` blob — meaningful only to another
JVM. It is **excluded from the standard format**. The portable error contract is:
`status` (≥400) + `body` (error message) + optional `stack` (text). Java keeps the
exception blob working for compact-mode Java↔Java exchanges; when encoding standard mode
it is dropped (the stack text and status carry the diagnosis).

## 4. Compatibility & negotiation

The compact and standard key namespaces are **disjoint** (compact keys are exactly one
character; standard keys are all ≥2). Decoding is therefore unambiguous without any
out-of-band signal:

- map contains `0`, `T`, `H` or `B` → compact
- map contains `id`, `to`, `headers` or `body` → standard

**Decode: sniff everywhere.** Java's `load(byte[])` gains the sniff and routes compact
maps through the existing flag decoder and standard maps through the existing
`fromMap()`. Every 4.10+ Java node then accepts BOTH formats forever.

**Encode: requester chooses, responder mirrors.** The Event-over-HTTP *service* replies
in the same format the request arrived in (it knows — it sniffed). The *client* picks the
request format:

- `event.over.http.format = compact | standard` in application.properties
  (global default), overridable per call via the existing headers parameter of
  `po.asyncRequest(event, timeout, headers, endpoint, rpc)` with
  `x-event-format: compact | standard`.
- **Default: `standard`** (CONFIRMED by maintainer, 2026-07-21). Event over HTTP is a
  transient transport — no payload is stored after transmission — so there is no
  serialized-data migration concern; caller and callee upgrade together in practice.
  `compact` is the explicit **fallback** for installations that are slow to upgrade one
  side (the FIFO-vs-BDB elastic-queue precedent: the new format is the default, the old
  one remains selectable).

This gives: Rust ↔ new-Java (standard, zero config), new-Java → old-Java (set
`event.over.http.format=compact` globally or `x-event-format: compact` per target until
the old side upgrades). No handshake, no version headers on the wire, no flag-day.

## 5. Implementation plan

**Phase 1 — Java (this repo, feature release):**

*Agreed API shape (maintainer + agent review, 2026-07-21).* Inbound decoding is automatic
at the lowest level; outbound format is transport policy passed explicitly. No-arg
defaults preserve today's behavior at both API levels, so every existing caller —
in-process `toMap()` cloning (EventEmitter), mesh/stream `toBytes()` — is untouched:

```java
public enum Format { COMPACT, STANDARD }

public Map<String, Object> toMap(Format format)   // explicit selection
public Map<String, Object> toMap()                // = toMap(Format.STANDARD)  (today's toMap)
public byte[] toBytes(Format format)              // = msgPack.pack(toMap(format))
public byte[] toBytes()                           // = toBytes(Format.COMPACT) (today's wire)
public void load(byte[] bytes)                    // sniffs compact vs standard automatically
```

The enum (not a boolean selector) is deliberate: call-site readability
(`toMap(Format.STANDARD)` vs `toMap(true)`), Sonar java:S2301 avoidance, 1:1 mapping to
the config string, and room for future representations. This lays the groundwork for
other event-to-bytes transports (Redis, S3, …): each transport picks its `Format` and
packs — the envelope owns representations, transports own policy.

1. Implement the API above — `toMap(STANDARD)` is today's `toMap()` plus the
   required-on-encode rule (`id`/`headers`/`body` always present) and the exception-blob
   drop (§3.4); `toMap(COMPACT)` is the flag map extracted from today's `toBytes()`.
2. `load()` sniffs (§4) — `fromMap()` already implements standard decode.
3. `EventApiService` mirrors the request format in its response.
4. `EventEmitter`/`PostOffice` remote path: `event.over.http.format` config (default
   `standard`) + per-call `x-event-format` header selection.
5. Docs: new reference page **"Event Envelope wire format"** carrying §3 verbatim
   (the language-neutral spec), linked from the Event-over-HTTP guide and `llms.txt`.
6. Config reference: `event.over.http.format` key.

**Phase 2 — Rust (separate session, the testbed):**
- Implement `/api/event` + client with the standard format (its envelope already IS the
  §3.1 core, minus `round_trip`; needs `#[serde(default)]`-style tolerance checks and
  the required-on-encode rule).
- Optional: adopt extension fields as needed (`stack` for error reporting first).

**Phase 3 — future ports (Node.js re-port, Python, Go):** implement from the spec page.

## 6. Interoperability test plan (Rust as testbed)

1. **Golden vectors** (shared fixtures, checked into both repos): canonical envelopes —
   minimal, full-metadata, nested map body, list body, binary body, unicode strings,
   ISO-8601 timestamp strings, 4xx error with stack — as base64 MsgPack files with a JSON
   sidecar describing expected decoded values. Each repo has a decode-assert test and an
   encode→decode round-trip test (semantic comparison, not byte comparison).
2. **Live two-runtime exchange** (after Phase 2): Java app + Rust app;
   (a) Java→Rust RPC and async (`x-ttl`, `x-async` honored), (b) Rust→Java both modes,
   (c) trace continuity: `trace_id`/`span_id` propagate and telemetry chains across the
   language boundary, (d) Java↔Java compact regression unchanged.
3. **Fresh-agent test** on the spec page (per docs convention): an agent implements an
   encoder/decoder in a scripting language from the page alone and passes the golden
   vectors.

## 7. Proposed ADR (human gate — for docs/arch-decisions/ADR.md if approved)

> **ADR-NNNN: Standard event envelope wire format for cross-language interop**
> Event over HTTP exchanges a MsgPack map with descriptive string keys (spec §3) as the
> language-neutral envelope encoding. Compact single-character keys remain supported for
> backward compatibility (decode always, encode by config) but are frozen — new fields
> are added to the standard format only. Language-native artifacts (Java exception blobs)
> never cross the wire in standard mode; portable errors are status + message + stack
> text. Decoders ignore unknown keys.

## 8. Review decisions (maintainer, 2026-07-21)

1. **Default at introduction** — RESOLVED: **standard**, with compact as the explicit
   fallback for slow-to-upgrade installations. Rationale: Event over HTTP is transport,
   not storage — nothing serialized outlives the exchange, so both parties upgrading
   together removes the compatibility concern.
2. **Config + override** — CONFIRMED: `event.over.http.format = compact | standard`
   (global) + `x-event-format: compact | standard` (per-request header).

Still open (do not block Phase 1):

3. **Version bump** — this is a feature: 4.10.0?
4. **Kafka mesh** — migrate `cloud.connector` payloads to standard in a later release, or
   leave compact indefinitely (same-language assumption)?
   *Investigated 2026-07-21: the mesh IS compact today on every hop — event transport
   (kafka-connector `EventConsumer.load`, platform-core `MultipartPayload.toBytes` incl.
   large-payload segmentation blocks), presence reporting (`PresenceConnector`), and the
   service monitor (`TopicController`). The Rust port has no mesh, so the same-language
   assumption holds by construction. Note: `MultipartPayload` segmentation is a second
   proprietary layer — any future cross-language mesh needs that spec'd too, so v1
   correctly keeps the mesh out of scope.*
5. **Golden-vector home** — `system/platform-core/src/test/resources/envelope-vectors/`
   mirrored into the Rust repo, or a dedicated top-level `interop/` folder?
