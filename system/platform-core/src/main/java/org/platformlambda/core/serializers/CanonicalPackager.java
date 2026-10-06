/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package org.platformlambda.core.serializers;

import org.platformlambda.core.util.Utility;
import org.platformlambda.mini.msgpack.MsgPackException;
import org.platformlambda.mini.msgpack.MsgPackFormat;
import org.platformlambda.mini.msgpack.MsgPackReader;
import org.platformlambda.mini.msgpack.MsgPackType;
import org.platformlambda.mini.msgpack.MsgPackWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A deterministic MsgPack packager (RFC-0002): the same content always gives the same bytes, in every engine.
 * <p>
 * A package is one MsgPack map with two parts. {@code manifest} is a metadata map: the packager writes
 * {@code format} and {@code format_version}, and everything else is a caller-defined string field that is
 * recorded and never interpreted (for a graph package, by convention {@code graph_id} holds the id of the graph
 * the package delivers). {@code maps} holds the packed maps keyed by entry name. Entry names are ordered by their
 * UTF-8 bytes, so the order does not depend on a file system listing or a locale.
 * <p>
 * The canonical profile, identical in every engine:
 * <ul>
 * <li>every map is written with its keys sorted at every depth, maps inside lists included (list order is kept);
 *     keys are text, in ascending order of their UTF-8 bytes (not the UTF-16 order of a Java {@code String}, which
 *     differs for supplementary characters); a non-text key is converted to text, and a null key or a collision
 *     after conversion is an error;</li>
 * <li>a null value is written as nil and is never dropped;</li>
 * <li>integers use the smallest encoding;</li>
 * <li>a floating-point number is a finite float64; a {@code Float} is accepted like any other number and widened
 *     through its shortest decimal text ({@code 0.1f} is the float64 {@code 0.1}, never {@code 0.10000000149011612}),
 *     so the same value gives the same bytes in every engine; NaN and Infinity are rejected;</li>
 * <li>text and bytes are str and bin, each with the shortest header;</li>
 * <li>an exact number travels as a string - a {@code BigInteger} as its digits, a {@code BigDecimal} in plain
 *     notation with its scale kept and a zero of any scale written {@code "0"} (the rule of RFC-0001) - and a
 *     date as an ISO-8601 string;</li>
 * <li>there are no extension types and no timestamps; any other type is rejected, naming its path.</li>
 * </ul>
 * The ordering is done here, by the packager, after any JSON parsing and before anything is written: the Gson
 * serializer has no ordered-keys option, and an ordered-keys option such as Jackson's sorts by {@code String}
 * order (UTF-16), which differs from the byte order of another engine for characters above U+FFFF. The input
 * map may be of any kind (a Gson {@code LinkedTreeMap}, a {@code HashMap}, a {@code LinkedHashMap}); its own
 * order never reaches the bytes. The bytes are written through the minimalist-msgpack writer directly rather than
 * through {@link MsgPack}, because that serializer drops null values unless a configuration switch is set, packs a
 * {@code Float} as float32 and writes a {@code BigDecimal} zero as {@code "0.00"}. Moreover, the profile allows none
 * of the three.
 * <p>
 * The packager is faithful to the type of each value, so the integer 1 and the float 1.0 are different content.
 * Strings are written as given: no Unicode normalization is applied.
 * <p>
 * Integrity is not part of a package. Nothing inside it refers to a hash or a signature: the user application
 * decides whether to protect the exact bytes, with which algorithm and where the proof is kept.
 */
public final class CanonicalPackager {
    public static final String FORMAT = "mercury-package";
    public static final String FORMAT_VERSION = "1";
    public static final String MANIFEST = "manifest";
    public static final String MAPS = "maps";
    public static final String FORMAT_KEY = "format";
    public static final String FORMAT_VERSION_KEY = "format_version";
    // a package is shallow; a bound keeps a hostile byte array from exhausting the stack
    private static final int MAX_DEPTH = 64;

    private CanonicalPackager() {
        // utility class
    }

    /**
     * A decoded package: ordered maps, in the order the bytes hold them.
     *
     * @param manifest the metadata map, {@code format} and {@code format_version} included
     * @param maps the packed maps keyed by entry name
     */
    public record Package(Map<String, String> manifest, Map<String, Map<String, Object>> maps) {
    }

    /**
     * @return a builder for a package
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Collects a package's manifest fields and maps, and packs them.
     */
    public static final class Builder {
        private final Map<String, String> fields = new TreeMap<>();
        private final Map<String, Map<?, ?>> entries = new TreeMap<>();

        private Builder() {
            // use CanonicalPackager.builder()
        }

        /**
         * @param key a caller-defined manifest field; {@code format} and {@code format_version} are reserved
         * @param value its text value
         * @return this builder
         * @throws IllegalArgumentException for a reserved or empty key, a null value or a duplicate field
         */
        public Builder manifest(String key, String value) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("A manifest field needs a name");
            }
            if (FORMAT_KEY.equals(key) || FORMAT_VERSION_KEY.equals(key)) {
                throw new IllegalArgumentException("The manifest field '" + key + "' is written by the packager");
            }
            if (value == null) {
                throw new IllegalArgumentException("The manifest field '" + key + "' has no value");
            }
            if (fields.put(key, value) != null) {
                throw new IllegalArgumentException("Duplicate manifest field '" + key + "'");
            }
            return this;
        }

        /**
         * @param name the entry name, a file name such as {@code quote.json}
         * @param map the map to pack
         * @return this builder
         * @throws IllegalArgumentException for an empty or duplicate entry name or a null map
         */
        public Builder add(String name, Map<?, ?> map) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("A map needs an entry name");
            }
            if (map == null) {
                throw new IllegalArgumentException("The map '" + name + "' is null");
            }
            if (entries.put(name, map) != null) {
                throw new IllegalArgumentException("Duplicate entry name '" + name + "'");
            }
            return this;
        }

        /**
         * @return the package as one deterministic byte array
         * @throws IllegalArgumentException for a value the profile rejects, naming its path
         */
        public byte[] build() {
            var manifest = new LinkedHashMap<String, Object>(fields);
            manifest.put(FORMAT_KEY, FORMAT);
            manifest.put(FORMAT_VERSION_KEY, FORMAT_VERSION);
            var document = new LinkedHashMap<String, Object>();
            document.put(MANIFEST, manifest);
            document.put(MAPS, new LinkedHashMap<String, Object>(entries));
            return encode(document);
        }
    }

    /**
     * Pack any value under the canonical profile.
     *
     * @param value a map, list or scalar
     * @return its canonical bytes
     * @throws IllegalArgumentException for a value the profile rejects, naming its path
     */
    public static byte[] encode(Object value) {
        var writer = new MsgPackWriter();
        write(writer, value, "$", 0);
        return writer.toByteArray();
    }

    /**
     * Read a package with the strict check: the decoded content is re-encoded canonically and the package is
     * rejected when the bytes differ, so an accepted package has exactly one byte form.
     *
     * @param bytes a package
     * @return the manifest and the maps, in the order the bytes hold them
     * @throws IOException when the bytes are not a canonical package
     */
    public static Package unpack(byte[] bytes) throws IOException {
        return unpack(bytes, true);
    }

    /**
     * Read a package.
     *
     * @param bytes a package
     * @param strict true to reject a package whose bytes are not the canonical form of its content
     * @return the manifest and the maps, in the order the bytes hold them
     * @throws IOException when the bytes are not a package, or not canonical under a strict read
     */
    public static Package unpack(byte[] bytes, boolean strict) throws IOException {
        var content = decode(bytes);
        if (!(content instanceof Map<?, ?> document) || document.size() != 2 ||
                !document.containsKey(MANIFEST) || !document.containsKey(MAPS)) {
            throw new IOException("A package is one map holding exactly '" + MANIFEST + "' and '" + MAPS + "'");
        }
        var manifest = readManifest(document.get(MANIFEST));
        if (!FORMAT.equals(manifest.get(FORMAT_KEY))) {
            throw new IOException("Not a package: the manifest format is '" + manifest.get(FORMAT_KEY) + "'");
        }
        if (!FORMAT_VERSION.equals(manifest.get(FORMAT_VERSION_KEY))) {
            throw new IOException("Unsupported package format_version '" + manifest.get(FORMAT_VERSION_KEY) + "'");
        }
        var maps = readMaps(document.get(MAPS));
        if (strict) {
            byte[] canonical;
            try {
                canonical = encode(content);
            } catch (IllegalArgumentException e) {
                throw new IOException("Not canonical: " + e.getMessage());
            }
            if (!Arrays.equals(canonical, bytes)) {
                throw new IOException("Not canonical: the bytes differ from the canonical form of their content");
            }
        }
        return new Package(manifest, maps);
    }

    /**
     * Decode MsgPack bytes into ordered maps (insertion order is the order of the bytes), lists and scalars.
     *
     * @param bytes MsgPack bytes holding exactly one value
     * @return a LinkedHashMap, a List or a scalar
     * @throws IOException for a malformed value, an extension type, a non-text key, a duplicate key, bytes after
     *         the value, or nesting beyond the bound
     */
    public static Object decode(byte[] bytes) throws IOException {
        var reader = new MsgPackReader(bytes);
        try {
            var value = read(reader, "$", 0);
            if (reader.hasNext()) {
                throw new IOException("Unexpected bytes after the value");
            }
            return value;
        } catch (MsgPackException e) {
            throw new IOException("Malformed MsgPack: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> readManifest(Object value) throws IOException {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IOException("The manifest is not a map");
        }
        var result = new LinkedHashMap<String, String>();
        for (var entry : map.entrySet()) {
            if (!(entry.getValue() instanceof String text)) {
                throw new IOException("The manifest field '" + entry.getKey() + "' is not text");
            }
            result.put(String.valueOf(entry.getKey()), text);
        }
        return result;
    }

    private static Map<String, Map<String, Object>> readMaps(Object value) throws IOException {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IOException("'" + MAPS + "' is not a map");
        }
        var result = new LinkedHashMap<String, Map<String, Object>>();
        for (var entry : map.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> packed)) {
                throw new IOException("The entry '" + entry.getKey() + "' is not a map");
            }
            var copy = new LinkedHashMap<String, Object>();
            packed.forEach((k, v) -> copy.put(String.valueOf(k), v));
            result.put(String.valueOf(entry.getKey()), copy);
        }
        return result;
    }

    private static void write(MsgPackWriter writer, Object value, String path, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Nesting deeper than " + MAX_DEPTH + " at " + path);
        }
        switch (value) {
            case null -> writer.writeNil();
            case Map<?, ?> map -> writeMap(writer, map, path, depth);
            case byte[] bytes -> writer.writeBinary(bytes);
            case Collection<?> collection -> writeList(writer, collection, path, depth);
            case Object[] array -> writeList(writer, Arrays.asList(array), path, depth);
            case String text -> writer.writeString(text);
            case Boolean flag -> writer.writeBoolean(flag);
            case Byte n -> writer.writeLong(n);
            case Short n -> writer.writeLong(n);
            case Integer n -> writer.writeLong(n);
            case Long n -> writer.writeLong(n);
            case AtomicInteger n -> writer.writeLong(n.get());
            case AtomicLong n -> writer.writeLong(n.get());
            case Double d -> writeDouble(writer, d, path);
            case Float f -> writeDouble(writer, Double.parseDouble(f.toString()), path);
            case BigInteger n -> writer.writeString(n.toString());
            case BigDecimal n -> writer.writeString(canonical(n));
            case Date date -> writer.writeString(Utility.getInstance().date2str(date));
            case Instant instant -> writer.writeString(Utility.getInstance().date2str(Date.from(instant)));
            default -> throw new IllegalArgumentException(
                    "Unsupported type " + value.getClass().getName() + " at " + path);
        }
    }

    private static void writeDouble(MsgPackWriter writer, double value, String path) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("A non-finite number is not canonical at " + path + ": " + value);
        }
        writer.writeDouble(value);
    }

    private static void writeList(MsgPackWriter writer, Collection<?> list, String path, int depth) {
        writer.writeArrayHeader(list.size());
        int i = 0;
        for (var item : list) {
            write(writer, item, path + "[" + i + "]", depth + 1);
            i++;
        }
    }

    private static void writeMap(MsgPackWriter writer, Map<?, ?> map, String path, int depth) {
        // keys become text; sorted by UTF-8 bytes (a TreeMap would sort by UTF-16, which differs above U+FFFF)
        var sorted = new ArrayList<Map.Entry<byte[], Map.Entry<String, Object>>>(map.size());
        var seen = new java.util.HashSet<String>();
        for (var entry : map.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("A null key at " + path);
            }
            var key = entry.getKey() instanceof String text ? text : String.valueOf(entry.getKey());
            if (!seen.add(key)) {
                throw new IllegalArgumentException("Two keys become '" + key + "' at " + path);
            }
            sorted.add(Map.entry(key.getBytes(StandardCharsets.UTF_8),
                    new java.util.AbstractMap.SimpleImmutableEntry<>(key, entry.getValue())));
        }
        sorted.sort((a, b) -> Arrays.compareUnsigned(a.getKey(), b.getKey()));
        writer.writeMapHeader(sorted.size());
        for (var item : sorted) {
            var key = item.getValue().getKey();
            writer.writeString(key);
            write(writer, item.getValue().getValue(), path + "." + key, depth + 1);
        }
    }

    /**
     * The canonical string of a decimal: plain notation, never scientific, the computed scale kept, and a zero of
     * any scale written "0" (the rule of RFC-0001).
     *
     * @param value a decimal
     * @return its canonical string
     */
    static String canonical(BigDecimal value) {
        if (value.signum() == 0) {
            return "0";
        }
        return (value.scale() < 0? value.setScale(0, java.math.RoundingMode.UNNECESSARY) : value).toPlainString();
    }

    private static Object read(MsgPackReader reader, String path, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("Nesting deeper than " + MAX_DEPTH + " at " + path);
        }
        MsgPackFormat format = reader.nextFormat();
        switch (format.getType()) {
            case NIL -> {
                reader.readNil();
                return null;
            }
            case BOOLEAN -> {
                return reader.readBoolean();
            }
            case INTEGER -> {
                return readInteger(reader, format);
            }
            case FLOAT -> {
                return readFloat(reader, format);
            }
            case STRING -> {
                return reader.readString();
            }
            case BINARY -> {
                return reader.readBinary();
            }
            case ARRAY -> {
                return readList(reader, path, depth);
            }
            case MAP -> {
                return readMap(reader, path, depth);
            }
            default -> throw new IOException("Unsupported MsgPack type " + format + " at " + path);
        }
    }

    private static Object readInteger(MsgPackReader reader, MsgPackFormat format) throws IOException {
        return format == MsgPackFormat.UINT64? readUnsigned64(reader) : (Object) reader.readLong();
    }

    private static Object readFloat(MsgPackReader reader, MsgPackFormat format) throws IOException {
        return format == MsgPackFormat.FLOAT32? (Object) reader.readFloat() : reader.readDouble();
    }

    private static List<Object> readList(MsgPackReader reader, String path, int depth) throws IOException {
        int size = reader.readArrayHeader();
        var list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(read(reader, path + "[" + i + "]", depth + 1));
        }
        return list;
    }

    private static Map<String, Object> readMap(MsgPackReader reader, String path, int depth)
            throws IOException {
        int size = reader.readMapHeader();
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < size; i++) {
            if (reader.nextFormat().getType() != MsgPackType.STRING) {
                throw new IOException("A key is not text at " + path);
            }
            var key = reader.readString();
            if (map.containsKey(key)) {
                throw new IOException("Duplicate key '" + key + "' at " + path);
            }
            map.put(key, read(reader, path + "." + key, depth + 1));
        }
        return map;
    }

    private static Object readUnsigned64(MsgPackReader reader) throws IOException {
        BigInteger n = reader.readBigInteger();
        return n.bitLength() < 64? (Object) n.longValue() : n;
    }
}
