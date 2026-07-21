---
title: Event Envelope Wire Format
summary: The language-neutral serialized form of an EventEnvelope for Event over HTTP interoperability.
layer: reference
audience: [developer, reference, ai-agent]
keywords: [event envelope, wire format, msgpack, interoperability, event over http, serialization, polyglot]
---

# Event Envelope Wire Format

*Reference: the serialized event envelope contract shared by every language implementation.*

> **At a glance**
>
> - **What** — the standard wire format: one MsgPack map with descriptive string keys that
>   any MsgPack-capable language (Java, Rust, Node.js, Python, Go, …) can encode and decode
>   with its idiomatic tooling — no custom codec.
> - **Why it matters** — it is the interoperability contract for
>   [Event over HTTP](event-over-http.md) between different language runtimes, and the
>   template for any future event-to-bytes transport.
> - **For** developers integrating runtimes across languages, and implementers of new
>   language ports. This page is self-contained: a new implementation needs nothing else.

Two serialized forms exist. The **standard** format on this page is the language-neutral
contract and the default for Event over HTTP. The **compact** format (single-character map
keys) is the classic same-language form that remains the default for `toBytes()` and the
Kafka service mesh; it is documented here only as far as detection requires.

## The envelope

A serialized envelope is a single **MsgPack map with string keys**. No MsgPack extension
types are used. The envelope is self-contained — no out-of-band metadata is needed to
decode it.

## Core fields

### `id`

| Type | Required |
|------|----------|
| str | always |

Unique event instance id — an opaque string (UUID recommended). Encoders always emit it.

### `to`

| Type | Required |
|------|----------|
| str | when set |

Target route name, e.g. `hello.world`.

### `from`

| Type | Required |
|------|----------|
| str | when set |

Sender route name, used for observability and reply semantics.

### `reply_to`

| Type | Required |
|------|----------|
| str | when set |

Route that should receive the response, when the sender expects one.

### `cid`

| Type | Required |
|------|----------|
| str | when set |

Business correlation id, preserved end-to-end.

### `trace_id`

| Type | Required |
|------|----------|
| str | when set |

Distributed trace id.

### `trace_path`

| Type | Required |
|------|----------|
| str | when set |

Trace path, e.g. `GET /api/hello`.

### `span_id`

| Type | Required |
|------|----------|
| str | when set |

The sender's span id, carried so the receiver knows its own parent span.

### `status`

| Type | Required |
|------|----------|
| int | when set (default 200) |

HTTP-style status code. A value ≥ 400 marks the event as an error.

### `headers`

| Type | Required |
|------|----------|
| map of str → str | always (may be empty) |

User-defined parameters. Encoders always emit this field — an empty map when there are no
headers — so statically-typed decoders need no per-field default handling.

### `body`

| Type | Required |
|------|----------|
| any MsgPack value | when set |

The payload: a map, array, string, integer, float, boolean, binary (`bin`), or nil.
An absent `body` means nil.

### `exec_time`

| Type | Required |
|------|----------|
| float | when set |

Function execution time in milliseconds.

### `round_trip`

| Type | Required |
|------|----------|
| float | when set |

End-to-end response time in milliseconds.

## Extension fields

Optional fields a receiver MAY use and MAY ignore. Implementations that do not produce
them remain fully conformant.

### `tags`

| Type | Required |
|------|----------|
| map of str → str | optional |

Routing metadata (e.g. `optional`, `json`, `broadcast`).

### `annotations`

| Type | Required |
|------|----------|
| map of str → any | optional |

Trace annotations recorded by the sender.

### `stack`

| Type | Required |
|------|----------|
| str | optional |

Stack-trace text — the **portable** error detail. The portable error contract is
`status` (≥ 400) + `body` (error message) + optional `stack`.

### `obj_type`

| Type | Required |
|------|----------|
| str | optional |

Language-specific payload type hint (a PoJo class name for Java). Advisory only.

### `exception`

| Type | Required |
|------|----------|
| bin | optional |

A language-native serialized exception object (Java only today). Meaningful only to a
receiver of the same language — cross-language receivers MUST ignore it. Use `stack` for
portable diagnostics.

## Encoding rules

1. **Map keys are strings** — including all keys inside a map-valued `body`.
2. **Optional fields**: encoders SHOULD omit unset fields; encoding nil is equally valid.
   Decoders MUST treat *absent* and *nil* identically.
3. **Unknown keys MUST be ignored** by decoders (forward compatibility).
4. **Numbers**: encoders use the smallest natural MsgPack width; decoders MUST accept any
   integer width for integer fields and float32 or float64 for float fields.
5. **Binary** values use the MsgPack `bin` family; strings are UTF-8 `str`.
6. **Timestamps** travel as ISO-8601 UTC strings with millisecond precision
   (e.g. `2026-07-21T12:00:00.000Z`) — never as MsgPack extension types.
7. **No byte-identical guarantee**: map ordering is unspecified. Conformance is semantic —
   decode, compare values.

## Format detection

The compact format's keys are all exactly **one character**; standard keys are all
**two or more**. A decoder therefore detects the format from the first map key without any
negotiation, version field, or content-type variant. The Java implementation accepts both
formats on every decode path.

## Selecting the outbound format (Java)

Outbound format is transport policy. For Event over HTTP:

- `event.over.http.format = standard | compact` in application.properties — the
  application default (`standard` when unset).
- `x-event-format: standard | compact` in the optional headers of
  `po.request(event, timeout, headers, endpoint, rpc)` — a per-call override, also usable
  per target in the `yaml.event.over.http` configuration's `headers` section. The header
  is consumed by the client and never sent to the peer.
- The `/api/event` service **mirrors** the requester's format in its response, so callers
  need no response-side configuration.

Use `compact` as a fallback when the peer is an older Java runtime that does not yet
decode the standard format.

## Conformance vectors

Golden vectors (MsgPack bytes as base64, with expected decoded values) are maintained at
`system/platform-core/src/test/resources/envelope-vectors/vectors.json` and shared with
the official [Rust implementation](https://github.com/Accenture/mercury). A new
implementation is conformant when it decodes every vector to the expected values and its
own encodings round-trip through another implementation.

## See also

- [Event over HTTP](event-over-http.md) — the transport this contract serves
- [Event Envelope Reference](event-envelope-reference.md) — the in-memory API
- [Configuration Reference](configuration-reference.md) — `event.over.http.format`
