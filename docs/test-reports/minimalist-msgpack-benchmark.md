---
title: Test Report — MsgPack codec, minimalist-msgpack vs msgpack-core
summary: Permanent record of the two checks behind RFC-0006 - a two-way differential proving that the in-house minimalist-msgpack codec writes the same bytes as msgpack-core 0.9.12 and decodes them identically on a 20,000-document random corpus, and a benchmark of both codecs under platform-core's MsgPack on three payload shapes - kept as the evidence for replacing the dependency.
layer: reference
audience: [developer, architect]
keywords: [msgpack, minimalist-msgpack, msgpack-core, benchmark, differential, byte for byte, serialization, test report]
---

# Test Report — MsgPack codec, minimalist-msgpack vs msgpack-core

*Validation of `system/minimalist-msgpack`, the zero-dependency MessagePack codec that replaces `org.msgpack:msgpack-core`
under platform-core's `MsgPack` and `CanonicalPackager` (RFC-0006 in `docs/arch-decisions/RFC.md`), conducted 2026-10-06 on
the branch `feature/minimalist-msgpack`, before the change was reviewed.*

This report is a permanent record. It answers two questions: **does the new codec produce exactly the bytes msgpack-core
produced, and read them back to exactly the same values?** and **is it at par or faster?** The first is the correctness
contract of a wire codec that every event of a running installation passes through; the second was Eric's first criterion
for the replacement, with virtual-thread friendliness, no deprecated libraries and no unsafe operations as the other three.

## Why this report exists

msgpack-core 0.9.12 is flagged by the field's scanners for CVE-2026-90472 and CVE-2026-90473, with no fixed release
published. The engine reaches neither vulnerable method and bounds its own reader (CHANGELOG 4.12.21 Unreleased, items 9 and
25), but a flagged library at the serialization foundation can block a field release regardless, so the part of
MessagePack the engine uses - eight type families, no extension types - was written from the specification as a module of
this repository. A replacement at that altitude must be shown equivalent in bytes and in behaviour, and no slower; the
module's own 134 tests pin the specification, and this report pins the equivalence with what the installations run today.

## Method

| | msgpack-core backend | minimalist-msgpack backend |
|---|---|---|
| Engine under test | `platform-core` 4.12.20 as released (`~/.m2`, built from tag `v4.12.20`) | `platform-core` of `feature/minimalist-msgpack`, `target/classes` |
| Codec | `org.msgpack:msgpack-core` 0.9.12 | `org.platformlambda:minimalist-msgpack` 4.12.20 (the branch) |
| Value layer | `org.platformlambda.core.serializers.MsgPack` (identical logic on both; only the codec calls differ) | |

The harness is one Java program, `benchmark/msgpack-codec-harness/Harness.java`, compiled once against the `MsgPack` API
(which did not change) and run once per backend in a separate JVM with the backend's classpath. It is not part of the
reactor, because the msgpack-core backend needs the flagged library on its classpath and a Maven manifest that declares it
would reappear in a scanner.

**Differential.**

1. **Corpus.** 20,000 documents from a seeded generator (seed `20261006`): maps and lists nested to depth 4 with up to 40
   members, holding nulls (in maps and in lists), booleans, integers drawn half from the 21 encoding boundaries
   (`0, 1, 127, 128, 255, 256, 65535, 65536, 2^32-1, 2^32, Long.MAX_VALUE` and the negative boundaries down to
   `Long.MIN_VALUE`) and half from random bit lengths, `Integer` and `Long` values, `Float` and `Double` values including
   `-0.0` and infinity, byte arrays up to 400 bytes, and strings up to 600 characters from four alphabets (ASCII, Latin-1,
   BMP, supplementary) with an unpaired surrogate appended to one string in fifty. 16,837,267 bytes packed.
2. **Pack.** Each backend packs every document with `MsgPack.pack` and writes the bytes, length-prefixed, to a file.
3. **Binary diff.** `cmp` of the two files.
4. **Cross-decode.** Each backend reads the file the *other* backend wrote with `MsgPack.unpack` and compares every
   document with the regenerated original, normalized as the value layer documents it: a null map value is dropped
   (`serializer.null.transport` is off), a list null keeps its slot, an integer compares by value whatever its boxed width,
   and an unpaired surrogate reads back as `?`.
5. **The check must be able to fail.** The first cross-decode pass reported 9,124 mismatches, identical in count and
   position in both directions. The cause was the harness, not either codec: its expectation model normalized unpaired
   surrogates in string *values* but not in map *keys*, and the corpus has keys with such characters. Corrected, the count
   fell to 0 in both directions. The detour is recorded because it shows the comparison is live - a model that disagrees
   with both codecs equally is caught, and a codec that disagreed with the other would be too.

**Benchmark.**

1. **Payloads**, each a Map through `MsgPack.pack` and `MsgPack.unpack`, so the value layer's own cost (HashMap and
   ArrayList construction, type dispatch) is included - it is what the engine pays:
   - *small*: an event envelope of 12 fields (ids, route names, a headers map of 3, a body map of 4, a float, a long) - 395 bytes;
   - *medium*: the same envelope with a body of 50 records, each 7 fields including a 3-element list - 7,366 bytes;
   - *large*: the same envelope with a 2,160-character text, a 4,096-byte binary, a list of 200 integers and 20 short fields - 8,068 bytes.
2. **Measurement.** Per payload: `pack`, `unpack` of the pre-packed bytes, and a `pack` + `unpack` round trip, each warmed up
   for 3 s and then timed over 5 rounds of 2 s; a round's figure is its elapsed time divided by its operation count, and the
   round with the lowest figure is reported. A sink consumes every result so nothing is optimized away.
3. **Runs.** Three runs per backend, each in a fresh JVM, the two backends alternated (old, new, old, new, old, new), the
   machine otherwise idle. The table reports the best run per cell; the spread between runs of the same backend was within
   about 4%, which is the noise floor to read the medium payload against.
4. **Environment.** Apple M3 Pro (6 performance and 6 efficiency cores), 36 GB, macOS 26.7.1; OpenJDK 27 (Homebrew build)
   with default flags (G1, 9 GB maximum heap). One developer machine, not a CI runner; the engine is compiled for Java 21
   and the JIT of a Java 21 runtime may place the numbers differently, which is why the ratios matter more than the
   nanoseconds.

## Result

**Byte identity: the two corpora are byte-identical** - 16,917,271 bytes (the 20,000 documents and their length prefixes),
`cmp` finds no difference. The writer reproduces msgpack-core's smallest-format rule exactly, the unpaired-surrogate
replacement included.

**Cross-decode: 0 mismatches in both directions** on 20,000 documents - the new codec reads what msgpack-core wrote to the
same values, and msgpack-core reads what the new codec wrote to the same values.

**Benchmark:**

| payload | bytes | msgpack-core pack | minimalist pack | msgpack-core unpack | minimalist unpack | msgpack-core round trip | minimalist round trip |
|---|---:|---:|---:|---:|---:|---:|---:|
| small (an event envelope) | 395 | 825 ns | 446 ns (0.54x) | 983 ns | 540 ns (0.55x) | 1,788 ns | 1,026 ns (0.57x) |
| medium (50 records in a list) | 7366 | 11,916 ns | 11,496 ns (0.96x) | 18,205 ns | 18,701 ns (1.03x) | 31,220 ns | 29,681 ns (0.95x) |
| large (2 KB text, 4 KB bin, 200 ints) | 8068 | 8,470 ns | 3,323 ns (0.39x) | 3,376 ns | 2,420 ns (0.72x) | 11,926 ns | 5,759 ns (0.48x) |

Best of 3 runs per backend (each run the best of 5 rounds of 2 s after 3 s of warm-up), ns per operation; the ratio is minimalist over msgpack-core, so below 1.00x is faster. Payload sizes are the packed bytes.

Reading the numbers:

- **The event envelope**, the shape every event on the bus has, packs and unpacks in a little over half the time. The gain is
  fixed overhead that msgpack-core paid per call: `MessagePack.newDefaultPacker` allocated an 8 KB `MessageBuffer` for every
  pack, `MsgPack.packMapOrList` wrote it through a synchronized `ByteArrayOutputStream` and copied the result twice, and
  `MessageUnpacker` carried its own buffer and string-decoder state. The new writer starts from a 256-byte array and copies
  once; the new reader is a cursor over the input array.
- **The large payload** packs in well under half the time: the 2,160-character string went through msgpack-core's
  `CharsetEncoder` path (strings of 512 characters and more) and the binary through its buffer, where the new writer uses
  `String.getBytes(UTF_8)` and one `System.arraycopy`.
- **The medium payload** is at par: 50 small maps with short strings, where the time is the value layer's own and the
  per-string cost of the two codecs is alike; the differences between the backends are within the run-to-run spread.

**Negative controls in the test suites.** The module's tests show the reader refusing every proper prefix of 300 random
documents (over 50,000 cuts) and the hostile headers of both CVEs by name; platform-core's `MsgPackTest` shows a truncated
envelope failing as `IllegalArgumentException` from `EventEnvelope` at every cut, where msgpack-core's own unchecked
exceptions used to escape.

## What was not measured

- Throughput under many concurrent virtual threads. The codec holds no shared or thread-local state and takes no lock, so
  there is nothing to contend for by construction; a scaling measurement was not run.
- Allocation and GC profile. The small-payload result implies far less garbage per event, but no allocation profiler was
  attached.
- A Java 21 runtime. The numbers come from OpenJDK 27 on the developer machine; the engine's baseline is Java 21.

## Reproduce

```bash
# the classpaths: platform-core's runtime dependencies with and without msgpack-core
mvn -q dependency:build-classpath -f system/platform-core/pom.xml -Dmdep.outputFile=/tmp/cp.txt -Dmdep.includeScope=runtime
OLD="benchmark/msgpack-codec-harness/classes:$(cat /tmp/cp.txt):$HOME/.m2/repository/org/platformlambda/platform-core/4.12.20/platform-core-4.12.20.jar:$HOME/.m2/repository/org/msgpack/msgpack-core/0.9.12/msgpack-core-0.9.12.jar"
NEW="benchmark/msgpack-codec-harness/classes:$(tr ':' '\n' < /tmp/cp.txt | grep -v msgpack-core | paste -sd: -):system/platform-core/target/classes"
javac --release 21 -cp "$OLD" -d benchmark/msgpack-codec-harness/classes benchmark/msgpack-codec-harness/Harness.java

# differential: pack the corpus on each backend, diff, cross-decode
java -cp "$OLD" Harness corpus /tmp/corpus-old.bin 20000
java -cp "$NEW" Harness corpus /tmp/corpus-new.bin 20000
cmp /tmp/corpus-old.bin /tmp/corpus-new.bin && echo identical
java -cp "$NEW" Harness verify /tmp/corpus-old.bin 20000
java -cp "$OLD" Harness verify /tmp/corpus-new.bin 20000

# benchmark, one JVM per backend, machine otherwise idle
java -cp "$OLD" Harness bench
java -cp "$NEW" Harness bench
```

The msgpack-core backend needs `platform-core` 4.12.20 in the local Maven repository (Mercury artifacts are not on Maven
Central: `mvn install -DskipTests` on a checkout of tag `v4.12.20`); a reactor `mvn install` of the branch replaces that jar,
because the branch carries the same version number, so run the old backend from a checkout's `target/classes` instead
when that has happened. The new backend needs the branch's `platform-core` compiled and `minimalist-msgpack` installed.
`benchmark/msgpack-codec-harness/README.md` has the same recipe.
