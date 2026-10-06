# Minimalist MsgPack

A MessagePack codec written from the published specification
([spec.md](https://github.com/msgpack/msgpack/blob/master/spec.md)), with no dependency beyond `java.base`.
It is the serialization foundation of `platform-core`: the event envelope, `MsgPack.pack`/`unpack`, the
general purpose `packMapOrList`/`unpackMapOrList` pair and the `CanonicalPackager` all write and read through it.

## Why it exists

The engine used `org.msgpack:msgpack-core`. In 2026 that library was flagged for CVE-2026-90472 (stack exhaustion
on deeply nested input) and CVE-2026-90473 (an integer overflow on a `map 32` count), and no fixed release was
published. The engine reached neither vulnerable method, but a serialization library at the foundation of every
event is exactly the kind of dependency a field pipeline's scanner blocks a release on. The engine uses a small
part of MessagePack - eight type families, no extension types - so the part it uses is written here, from the
specification, and the dependency is gone.

## What it supports

| Family | Formats | Java type on read | Written from |
|---|---|---|---|
| nil | `nil` | `null` | `writeNil()` |
| bool | `true`, `false` | `boolean` | `writeBoolean(boolean)` |
| int | positive and negative fixint, `uint 8/16/32/64`, `int 8/16/32/64` | `long`, or `BigInteger` for a `uint 64` above `Long.MAX_VALUE` | `writeLong(long)` |
| float | `float 32`, `float 64` | `float`, `double` | `writeFloat(float)`, `writeDouble(double)` |
| str | fixstr, `str 8/16/32` | `String` | `writeString(String)` |
| bin | `bin 8/16/32` | `byte[]` | `writeBinary(byte[])` |
| array | fixarray, `array 16/32` | the element count; the caller reads the elements | `writeArrayHeader(int)` |
| map | fixmap, `map 16/32` | the entry count; the caller reads the keys and values | `writeMapHeader(int)` |

Extension types (`fixext`, `ext 8/16/32`, the timestamp among them) are **not supported**: the writer has no method for
them and the reader decodes none. The reader knows their framing, so `skipValue()` steps over one without losing its
place - the value layer in `platform-core` reads an extension value as `null`, as it always did.

The writer uses the smallest format for every value, as the specification recommends, with the integer rule the Java
and Rust MessagePack libraries share: `-32..127` is one byte, a larger non-negative value takes the smallest unsigned
width and a smaller negative value the smallest signed width. The bytes are therefore identical to those of
`msgpack-core` and `rmp`, which the engine's canonical package vectors (`canonical-package-vectors.json`, byte-identical
with the Rust engine) prove.

## Robustness

The reader is built for bytes that arrive from outside the process:

- every read is bounded by the array window, and bytes that end before the value does are refused by offset;
- the lengths of `str`, `bin` and `ext` values and the counts of arrays and maps are 32-bit **unsigned** and are checked
  against the bytes that remain before anything is allocated or consumed (an array of N elements needs at least N bytes,
  a map of N entries at least 2N), so a hostile header is refused at the header;
- `0xc1`, which the specification never uses, is refused as a format byte;
- `skipValue()` is iterative, so skipping a deeply nested value costs no stack; the depth bound for *decoding* a nested
  structure (64 levels, `MsgPack.MAX_DEPTH`) stays in the value layer that recurses;
- a failed read leaves the position where it was, so a caller can recover (for example `readBigInteger()` after
  `readLong()` refused a `uint 64` above `Long.MAX_VALUE`);
- every decoding failure is one checked exception, `MsgPackException extends IOException`; the codec throws nothing
  unchecked for malformed input.

## Performance, virtual threads, safety

- Multi-byte fields are read and written through `VarHandle` byte-array views (big-endian), the sanctioned intrinsic
  that replaced `sun.misc.Unsafe`; there is no reflection, no native code and no `--add-opens`.
- A reader or writer holds no shared or thread-local state and takes no lock, so one is created per call, on a virtual
  thread or any other; nothing pins a carrier.
- Short ASCII strings are written straight into the buffer without an intermediate UTF-8 array; longer or non-ASCII text
  goes through `String.getBytes(UTF_8)`. Reading a string is one `new String(bytes, offset, length, UTF_8)`, which the
  JDK fast-paths for ASCII.

## Usage

```java
var writer = new MsgPackWriter();
writer.writeMapHeader(2);
writer.writeString("name").writeString("Alice");
writer.writeString("age").writeLong(30);
byte[] bytes = writer.toByteArray();

var reader = new MsgPackReader(bytes);
int entries = reader.readMapHeader();
for (int i = 0; i < entries; i++) {
    String key = reader.readString();
    switch (reader.nextFormat().getType()) {
        case STRING -> System.out.println(key + " = " + reader.readString());
        case INTEGER -> System.out.println(key + " = " + reader.readLong());
        default -> reader.skipValue();
    }
}
```

Application code normally uses the value layer instead: `MsgPack.packMapOrList`/`unpackMapOrList` in `platform-core`
(see *API Overview → Binary serialization with MsgPack*).

## Tests

```bash
mvn test -f system/minimalist-msgpack/pom.xml
```

The tests pin the specification's format table byte by byte, hand-derived vectors for every format boundary, decoding of
non-minimal encodings, every hostile-header shape above, and a seeded random corpus that round-trips and whose every
proper prefix is refused. JaCoCo enforces 90% line and branch coverage.
