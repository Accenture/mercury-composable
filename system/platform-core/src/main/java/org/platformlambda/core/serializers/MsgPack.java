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

import org.platformlambda.core.models.TypedPayload;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.mini.msgpack.MsgPackException;
import org.platformlambda.mini.msgpack.MsgPackFormat;
import org.platformlambda.mini.msgpack.MsgPackReader;
import org.platformlambda.mini.msgpack.MsgPackWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The event payload serializer and the general purpose Map/List codec, over the minimalist-msgpack reader and writer
 * (a zero-dependency module of this repository, written from the MessagePack specification; no extension types).
 * <p>
 * Every decoding failure - truncated or malformed bytes, bytes after the value (the input holds exactly one), a nesting
 * deeper than {@link #MAX_DEPTH}, a top-level value that is not a Map or a List - is an {@link IOException}; no unchecked
 * exception leaves {@link #unpack(byte[])} for bytes that arrive from outside the process.
 */
public class MsgPack {
    private static final Utility util = Utility.getInstance();
    private static final PayloadMapper converter = PayloadMapper.getInstance();
    private static final SimpleObjectMapper mapper = SimpleMapper.getInstance().getMapper();
    private static final String DATA = "_D";
    private static final String TYPE = "_T";
    /**
     * The deepest nesting of maps and lists an unpack accepts, the outermost container being level 1. The
     * reader recurses once per level, so a payload nested deeper is refused with an IOException instead of
     * exhausting the thread's stack. The Rust engine applies the same limit to its decoder.
     */
    public static final int MAX_DEPTH = 64;
    private final boolean supportNulls;

    public MsgPack() {
        var config = AppConfigReader.getInstance();
        this.supportNulls = "true".equalsIgnoreCase(
                                config.getProperty("serializer.null.transport", "false"));
    }
    /**
     * Unpack method for an event payload that may carry the "_T"/"_D" type encoding.
     * <p>
     * This is the counterpart of {@link #pack(Object)} and is used for event payload transport,
     * where a PoJo or a primitive is wrapped with its type information. If the packed value is a
     * plain Map or List, prefer {@link #unpackMapOrList(byte[])} - it does no type unwrapping, so a
     * map that happens to contain a "_T" key is restored verbatim.
     *
     * @param bytes - packed structure
     * @return result - Map, List or PoJo object
     *
     * @throws IOException for mapping exception
     */
    @SuppressWarnings("unchecked")
    public Object unpack(byte[] bytes) throws IOException  {
        Object result = unpackMapOrList(bytes);
        if (result instanceof Map) {
            // is this an encoded payload
            Map<String, Object> map = (Map<String, Object>) result;
            if (map.containsKey(TYPE)) {
                if (map.size() == 2 && map.containsKey(DATA)) {
                    return map.get(DATA);
                }
                if (map.size() == 1 && map.get(TYPE).equals(PayloadMapper.NOTHING)) {
                    return null;
                }
            }
        }
        return result;
    }
    /**
     * Unpack a byte array into a Map or List - general purpose binary serialization.
     * <p>
     * Unlike {@link #unpack(byte[])}, this method does no "_T"/"_D" type unwrapping, so a map that
     * happens to contain a "_T" key is restored exactly as it was packed. Use this with
     * {@link #packMapOrList(Object)} when MsgPack is your application's own serializer rather than
     * the event payload codec.
     * <p>
     * Integers are restored with best-effort type matching: an Integer when the value fits, a Long when it
     * does not, and a BigInteger for a uint64 above Long.MAX_VALUE (which this serializer never writes, but
     * another MessagePack encoder may). An extension value, which this codec does not support, reads as null.
     *
     * @param bytes - packed structure
     * @return result - Map or List
     * @throws IOException for mapping exception, if the packed value is not a Map or List, or if bytes follow the
     *         value - the input holds exactly one value, as for {@link CanonicalPackager#decode(byte[])}
     */
    public Object unpackMapOrList(byte[] bytes) throws IOException  {
        var reader = new MsgPackReader(bytes);
        if (reader.hasNext()) {
            MsgPackFormat format = reader.nextFormat();
            Object result = switch (format.getType()) {
                case MAP -> unpack(reader, new HashMap<>(), 1);
                case ARRAY -> unpack(reader, new ArrayList<>(), 1);
                default -> throw new MsgPackException("Packed input should be Map or List, Actual: " + format.getType());
            };
            // the input holds exactly one value: bytes after it, well-formed or not, are a decoding error, as the
            // canonical decoder has always ruled; a decoder that read one value and stopped would never see them
            if (reader.hasNext()) {
                throw new MsgPackException("Unexpected bytes after the value at offset " + reader.position());
            }
            return result;
        }
        // this should not occur
        return new HashMap<String, Object>();
    }

    private Map<String, Object> unpack(MsgPackReader reader, Map<String, Object> map, int depth)
            throws IOException {
        checkDepth(depth);
        int n = reader.readMapHeader();
        for (int i=0; i < n; i++) {
            String key = reader.readString();
            MsgPackFormat format = reader.nextFormat();
            switch (format.getType()) {
                case MAP -> {
                    Map<String, Object> submap = new HashMap<>();
                    map.put(key, submap);
                    unpack(reader, submap, depth + 1);
                }
                case ARRAY -> {
                    List<Object> array = new ArrayList<>();
                    map.put(key, array);
                    unpack(reader, array, depth + 1);
                }
                default -> {
                    Object value = unpackValue(reader, format);
                    if (supportNulls || value != null) {
                        map.put(key, value);
                    }
                }
            }
        }
        return map;
    }

    private List<Object> unpack(MsgPackReader reader, List<Object> list, int depth) throws IOException {
        checkDepth(depth);
        int len = reader.readArrayHeader();
        for (int i=0; i < len; i++) {
            MsgPackFormat format = reader.nextFormat();
            switch (format.getType()) {
                case MAP -> {
                    Map<String, Object> submap = new HashMap<>();
                    list.add(submap);
                    unpack(reader, submap, depth + 1);
                }
                case ARRAY -> {
                    List<Object> array = new ArrayList<>();
                    list.add(array);
                    unpack(reader, array, depth + 1);
                }
                // null value is allowed to preserve the original sequence of the list
                default -> list.add(unpackValue(reader, format));
            }
        }
        return list;
    }

    private static void checkDepth(int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("Nesting deeper than " + MAX_DEPTH + " levels");
        }
    }

    private Object unpackValue(MsgPackReader reader, MsgPackFormat format) throws IOException {
        switch (format.getType()) {
            case STRING:
                return reader.readString();
            case INTEGER:
                // best effort type matching
                if (format == MsgPackFormat.UINT64) {
                    BigInteger big = reader.readBigInteger();
                    return big.bitLength() < 64 ? narrow(big.longValue()) : big;
                }
                return narrow(reader.readLong());
            case FLOAT:
                if (format == MsgPackFormat.FLOAT64) {
                    return reader.readDouble();
                } else {
                    return reader.readFloat();
                }
            case BINARY:
                return reader.readBinary();
            case BOOLEAN:
                return reader.readBoolean();
            case NIL:
                reader.readNil();
                return null;
            default:
                // for simplicity, custom (extension) types are not supported - the value is skipped and reads as null
                reader.skipValue();
                return null;
        }
    }

    private static Object narrow(long n) {
        if (n > Integer.MAX_VALUE || n < Integer.MIN_VALUE) {
            return n;
        } else {
            return (int) n;
        }
    }
    /**
     * Pack an event payload into a byte array, wrapping a PoJo or a primitive with its type
     * information ("_T"/"_D") so that {@link #unpack(byte[])} can restore the original object.
     * <p>
     * For general purpose serialization of a Map or List, prefer {@link #packMapOrList(Object)} -
     * it never adds the type encoding.
     *
     * @param obj - Map, List or a PoJo Object that contains get/set methods for variables
     * @return packed byte array
     *
     * @throws IOException for msgpack object mapping exception
     */
    public byte[] pack(Object obj) throws IOException {
        if (obj instanceof Map || obj instanceof List) {
            return packMapOrList(obj);
        } else {
            TypedPayload typed = converter.encode(obj, true);
            Map<String, Object> map = new HashMap<>();
            map.put(TYPE, typed.type());
            map.put(DATA, typed.payload());
            return packMapOrList(map);
        }
    }

    /**
     * Pack a Map or List into a byte array - general purpose binary serialization.
     * <p>
     * Unlike {@link #pack(Object)}, this method never adds the "_T"/"_D" type encoding, so the
     * bytes hold exactly the given structure and {@link #unpackMapOrList(byte[])} restores it
     * verbatim - even when the map itself contains a "_T" key. This makes it a plain, portable
     * MsgPack codec that any language can read.
     * <p>
     * Only a Map or a List is accepted. A PoJo or a Java primitive must be encoded by the
     * application first - for example, convert a PoJo into a Map with
     * {@code SimpleMapper.getInstance().getMapper().readValue(pojo, Map.class)}.
     *
     * @param obj - Map or List
     * @return packed byte array
     * @throws IllegalArgumentException if the input is not a Map or a List
     */
    public byte[] packMapOrList(Object obj) {
        if (obj instanceof Map || obj instanceof List) {
            // select low level processing for faster performance
            var writer = new MsgPackWriter();
            pack(writer, obj);
            return writer.toByteArray();
        }
        throw new IllegalArgumentException("Input must be a Map or List. Encode a PoJo or a " +
                "primitive in your application first (e.g. convert a PoJo into a Map with SimpleMapper)");
    }

    private void pack(MsgPackWriter writer, Object o) {
        switch (o) {
            case null -> writer.writeNil();
            case Map<?, ?> map -> packMap(writer, map);
            case Collection<?> list -> {
                writer.writeArrayHeader(list.size());
                for (Object l : list) {
                    pack(writer, l);
                }
            }
            case Object[] objects -> {
                // Array is treated like a list
                writer.writeArrayHeader(objects.length);
                for (Object l : objects) {
                    pack(writer, l);
                }
            }
            case String str -> writer.writeString(str);
            case Short s -> writer.writeLong(s);
            case Byte b -> writer.writeLong(b);
            case Integer i -> writer.writeLong(i);
            case AtomicInteger aInt -> writer.writeLong(aInt.get());
            case Long l -> writer.writeLong(l);
            case AtomicLong aLong -> writer.writeLong(aLong.get());
            case Float f -> writer.writeFloat(f);
            case Double d -> writer.writeDouble(d);
            case BigInteger bInt ->
                // convert to string to preserve precision
                writer.writeString(bInt.toString());
            case BigDecimal bDecimal ->
                // convert to string to preserve precision
                writer.writeString(bDecimal.toPlainString());
            case Boolean bb -> writer.writeBoolean(bb);
            case byte[] b -> writer.writeBinary(b);
            case Date d ->
                // Date object will be packed as ISO-8601 string
                writer.writeString(util.date2str(d));
            case Instant i ->
                // Instant (java.time) is packed as an ISO-8601 UTC string, like Date
                writer.writeString(util.date2str(Date.from(i)));
            default -> {
                // handle pojo inside data structure
                if (util.isPoJo(o)) {
                    try {
                        var value = mapper.readValue(o, Map.class);
                        pack(writer, value);
                    } catch (Exception e) {
                        writer.writeString(String.valueOf(o));
                    }
                } else {
                    // unknown object
                    writer.writeString(String.valueOf(o));
                }
            }
        }
    }

    private void packMap(MsgPackWriter writer, Map<?, ?> map) {
        int mapSize = map.size();
        List<Object> keys = new ArrayList<>(map.keySet());
        mapSize -= getNullKeyCount(map, keys);
        writer.writeMapHeader(mapSize);
        if (mapSize > 0) {
            for (var k : keys) {
                Object value = map.get(k);
                if (supportNulls || value != null) {
                    // Enforce key as a string
                    writer.writeString(k instanceof String text ? text : String.valueOf(k));
                    pack(writer, value);
                }
            }
        }
    }

    private int getNullKeyCount(Map<?, ?> map, List<Object> keys) {
        if (supportNulls) {
            return 0;
        } else {
            int count = 0;
            for (var k : keys) {
                // reduce map size if null value
                if (map.get(k) == null) {
                    count++;
                }
            }
            return count;
        }
    }
}
