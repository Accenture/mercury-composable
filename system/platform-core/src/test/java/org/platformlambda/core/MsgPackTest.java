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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.PoJo;
import org.platformlambda.core.serializers.CanonicalPackager;
import org.platformlambda.core.serializers.MsgPack;
import org.platformlambda.core.serializers.PayloadMapper;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class MsgPackTest {
    private static final MsgPack msgPack = new MsgPack();

    @SuppressWarnings("unchecked")
    @Test
    void nullTransportTest() throws IOException {
        System.setProperty("serializer.null.transport", "false");
        var map = new HashMap<String, Object>();
        map.put("hello", "world");
        map.put("null", null);
        var msgPack1 = new MsgPack();
        var b1 = msgPack1.pack(map);
        var x1 = msgPack1.unpack(b1);
        assertInstanceOf(Map.class, x1);
        var restored = (Map<String, Object>) x1;
        assertEquals("world", restored.get("hello"));
        assertNull(restored.get("null"));
        assertFalse(restored.containsKey("null"));
        // enable null transport
        System.setProperty("serializer.null.transport", "true");
        var msgPack2 = new MsgPack();
        var b2 = msgPack2.pack(map);
        var x2 = msgPack2.unpack(b2);
        assertInstanceOf(Map.class, x2);
        var restored1 = (Map<String, Object>) x2;
        assertEquals("world", restored1.get("hello"));
        assertNull(restored.get("null"));
        assertTrue(restored1.containsKey("null"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void dataIsMap() throws IOException {
        var config = AppConfigReader.getInstance();
        var transportNulls = "true".equalsIgnoreCase(
                                config.getProperty("serializer.null.transport", "false"));
        PoJo pojo = new PoJo();
        pojo.setName("hello world");
        String[] helloWorld = {"hello", "world"};
        Map<String, Object> input = new HashMap<>();
        input.put("hello", "world");
        input.put("boolean", true);
        input.put("array", helloWorld);
        input.put("integer", 12345L);
        input.put("long", 12345L);
        input.put("float", 12.345f);
        input.put("double", 12.345d);
        input.put("poJo", pojo);
        input.put(PayloadMapper.NOTHING, null);
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertInstanceOf(Map.class, o);
        Map<String, Object> result = (Map<String, Object>) o;
        // long number will be compressed into integer if applicable
        assertInstanceOf(Integer.class, result.get("integer"));
        assertInstanceOf(Integer.class, result.get("long"));
        assertInstanceOf(Float.class, result.get("float"));
        assertInstanceOf(Double.class, result.get("double"));
        // serializer.null.transport=true in application.properties
        // to enable transport of null values
        assertEquals(transportNulls, result.containsKey(PayloadMapper.NOTHING));
        result.remove(PayloadMapper.NOTHING);
        assertEquals(o, result);
        // array is converted to list of objects
        assertEquals(Arrays.asList(helloWorld), result.get("array"));
        // embedded pojo in a map is converted to a map
        Object innerPoJo = result.get("poJo");
        assertInstanceOf(Map.class, innerPoJo);
        PoJo restored = SimpleMapper.getInstance().getMapper().readValue(innerPoJo, PoJo.class);
        assertEquals(pojo.getName(), restored.getName());
    }

    @Test
    void dataIsAtomicInteger() throws IOException {
        AtomicInteger input = new AtomicInteger(10000);
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input.get(), o);
    }

    @Test
    void dataIsAtomicLong() throws IOException {
        AtomicLong input = new AtomicLong(10000);
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // smaller number will be packed as integer
        assertEquals((int) input.get(), o);
    }

    @Test
    void dataIsSmallInteger() throws IOException {
        int input = 10000;
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input, o);
    }

    @Test
    void smallLongBecomesInteger() throws IOException {
        // msgpack compresses number and data type information will be lost
        Long input = 10L;
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input.intValue(), o);
    }

    @SuppressWarnings("unchecked")
    @Test
    void dataIsBigLong() throws IOException {
        long input = -5106534569952410475L;
        Map<String, Object> map = new HashMap<>();
        map.put("number", input);
        byte[] b = msgPack.pack(map);
        Object o = msgPack.unpack(b);
        assertInstanceOf(Map.class, o);
        Map<String, Object> restored = (Map<String, Object>) o;
        assertEquals(input, restored.get("number"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void dataIsBorderLineLong() throws IOException {
        Long input = Integer.MAX_VALUE + 1L;
        List<Long> value = new ArrayList<>();
        value.add(input);
        byte[] b = msgPack.pack(value);
        Object o = msgPack.unpack(b);
        assertInstanceOf(List.class, o);
        List<Long> restored = (List<Long>) o;
        assertEquals(input, restored.getFirst());
    }

    @Test
    void dataIsBigInteger() throws IOException {
        BigInteger input = new BigInteger("10");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input.toString(), o);
    }

    @Test
    void dataIsBigDecimal() throws IOException {
        BigDecimal input = new BigDecimal("0.0000000000012345");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input.toPlainString(), o);
    }

    @Test
    void dataIsNull() throws IOException {
        byte[] b = msgPack.pack(null);
        Object o = msgPack.unpack(b);
        assertNull(o);
    }

    @Test
    void dataIsBoolean() throws IOException {
        byte[] b = msgPack.pack(true);
        Object o = msgPack.unpack(b);
        assertEquals(true, o);
    }

    @Test
    void dataIsFloat() throws IOException {
        Float input = 3.2f;
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input, o);
    }

    @Test
    void dataIsSmallDouble() throws IOException {
        Double input = 3.2d;
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input, o);
    }

    @Test
    void dataIsDouble() throws IOException {
        Double input = Float.MAX_VALUE + 1.0d;
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input, o);
    }

    @Test
    void dataIsDate() throws IOException {
        Date input = new Date();
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // date object is serialized as UTC string
        assertEquals(Utility.getInstance().date2str(input), o);
    }

    @Test
    void dataIsInstant() throws IOException {
        Instant input = Instant.now();
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // Instant is serialized as a UTC ISO-8601 string, like Date
        assertEquals(Utility.getInstance().date2str(Date.from(input)), o);
    }

    @Test
    void instantInsideMap() throws IOException {
        Instant input = Instant.now();
        byte[] b = msgPack.pack(Map.of("ts", input));
        Object o = msgPack.unpack(b);
        assertInstanceOf(Map.class, o);
        // an Instant value nested in a map is packed as the same UTC ISO-8601 string
        assertEquals(Utility.getInstance().date2str(Date.from(input)), ((Map<?, ?>) o).get("ts"));
    }

    @Test
    void dataIsList() throws IOException {
        List<String> input = new ArrayList<>();
        input.add("hello");
        input.add("world");
        input.add(null);    // prove that null value in a list can be transported
        input.add("1");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // MsgPack transports null elements in an array list so that absolute sequencing can be preserved
        assertEquals(input, o);
    }

    @Test
    void dataIsArray() throws IOException {
        String[] input = {"hello", "world", null, "1"};
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(Arrays.asList(input), o);
    }

    @Test
    void dataIsShortNumber() throws IOException {
        Short number = 10;
        byte[] b = msgPack.pack(number);
        Object o = msgPack.unpack(b);
        assertEquals((int) number, o);
    }

    @Test
    void dataIsByte() throws IOException {
        byte number = 10;
        byte[] b = msgPack.pack(number);
        Object o = msgPack.unpack(b);
        // a single byte is converted to an integer
        assertEquals((int) number, o);
    }

    @Test
    void dataIsPoJo() throws IOException {
        PoJo input = new PoJo();
        input.setName("testing Integer transport");
        input.setNumber(12345);
        input.setAddress("123 Planet Earth");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // successfully restored to PoJo
        assertInstanceOf(Map.class, o);
        PoJo result = SimpleMapper.getInstance().getMapper().readValue(o, PoJo.class);
        assertEquals(input.getNumber(), result.getNumber());
        assertEquals(input.getName(), result.getName());
        assertEquals(input.getAddress(), result.getAddress());
    }

    @Test
    void dataIsPoJoWithLong() throws IOException {
        PoJo input = new PoJo();
        input.setName("testing Long number transport");
        input.setLongNumber(10L);
        input.setAddress("100 Planet Earth");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // successfully restored to PoJo when the intermediate value becomes an integer
        assertInstanceOf(Map.class, o);
        PoJo result = SimpleMapper.getInstance().getMapper().readValue(o, PoJo.class);
        assertEquals(input.getLongNumber(), result.getLongNumber());
        assertEquals(input.getName(), result.getName());
        assertEquals(input.getAddress(), result.getAddress());
    }

    @Test
    void dataIsPoJoWithBigInteger() throws IOException {
        PoJo input = new PoJo();
        input.setName("testing BigInteger transport");
        input.setBigInteger(new BigInteger("10"));
        input.setAddress("100 Planet Earth");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // successfully restored to PoJo when the intermediate value becomes an integer
        assertInstanceOf(Map.class, o);
        PoJo result = SimpleMapper.getInstance().getMapper().readValue(o, PoJo.class);
        assertEquals(input.getBigInteger(), result.getBigInteger());
        assertEquals(input.getName(), result.getName());
        assertEquals(input.getAddress(), result.getAddress());
    }

    @Test
    void dataIsPoJoWithBigDecimal() throws IOException {
        PoJo input = new PoJo();
        input.setName("testing BigInteger transport");
        input.setBigDecimal(new BigDecimal("0.00000012345"));
        input.setAddress("100 Planet Earth");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // successfully restored to PoJo when the intermediate value becomes an integer
        assertInstanceOf(Map.class, o);
        PoJo result = SimpleMapper.getInstance().getMapper().readValue(o, PoJo.class);
        assertEquals(input.getBigDecimal(), result.getBigDecimal());
        assertEquals(input.getName(), result.getName());
        assertEquals(input.getAddress(), result.getAddress());
    }

    @SuppressWarnings("unchecked")
    @Test
    void packMapOrListRoundTripsAMap() throws IOException {
        Map<String, Object> input = new HashMap<>();
        input.put("name", "general purpose serialization");
        input.put("count", 100);
        input.put("nested", Map.of("hello", "world"));
        input.put("items", List.of(1, 2, 3));
        byte[] b = msgPack.packMapOrList(input);
        Object o = msgPack.unpackMapOrList(b);
        assertInstanceOf(Map.class, o);
        Map<String, Object> result = (Map<String, Object>) o;
        assertEquals("general purpose serialization", result.get("name"));
        assertEquals(100, result.get("count"));
        assertEquals(Map.of("hello", "world"), result.get("nested"));
        assertEquals(List.of(1, 2, 3), result.get("items"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void packMapOrListRoundTripsAList() throws IOException {
        List<Object> input = List.of("alpha", 2, Map.of("k", "v"));
        byte[] b = msgPack.packMapOrList(input);
        Object o = msgPack.unpackMapOrList(b);
        assertInstanceOf(List.class, o);
        List<Object> result = (List<Object>) o;
        assertEquals(3, result.size());
        assertEquals("alpha", result.getFirst());
        assertEquals(2, result.get(1));
        assertEquals(Map.of("k", "v"), result.get(2));
    }

    @SuppressWarnings("unchecked")
    @Test
    void packMapOrListDoesNotApplyTheTypeEncoding() throws IOException {
        // "_T"/"_D" are the event-payload type encoding used by pack/unpack. A general purpose
        // serializer must treat them as ordinary user data - this is why the new methods exist.
        Map<String, Object> input = new HashMap<>();
        input.put("_T", "not-a-type-marker");
        input.put("_D", "not-a-payload");
        byte[] b = msgPack.packMapOrList(input);
        Object o = msgPack.unpackMapOrList(b);
        assertInstanceOf(Map.class, o);
        Map<String, Object> result = (Map<String, Object>) o;
        assertEquals(2, result.size(), "the map must be restored verbatim");
        assertEquals("not-a-type-marker", result.get("_T"));
        assertEquals("not-a-payload", result.get("_D"));
        // the envelope-aware unpack() would have unwrapped the same bytes to the "_D" value
        assertEquals("not-a-payload", msgPack.unpack(b));
    }

    @Test
    void packMapOrListRejectsPoJoAndPrimitives() {
        PoJo pojo = new PoJo();
        pojo.setName("pojo is not supported directly");
        assertThrows(IllegalArgumentException.class, () -> msgPack.packMapOrList(pojo));
        assertThrows(IllegalArgumentException.class, () -> msgPack.packMapOrList("a string"));
        assertThrows(IllegalArgumentException.class, () -> msgPack.packMapOrList(42));
        assertThrows(IllegalArgumentException.class, () -> msgPack.packMapOrList(null));
    }

    @SuppressWarnings("unchecked")
    @Test
    void packMapOrListTakesAPoJoConvertedToMap() throws IOException {
        // the documented path for a PoJo: convert it into a Map with SimpleMapper first
        PoJo pojo = new PoJo();
        pojo.setName("converted pojo");
        pojo.setAddress("100 Planet Earth");
        Map<String, Object> asMap = SimpleMapper.getInstance().getMapper().readValue(pojo, Map.class);
        byte[] b = msgPack.packMapOrList(asMap);
        Map<String, Object> restored = (Map<String, Object>) msgPack.unpackMapOrList(b);
        PoJo result = SimpleMapper.getInstance().getMapper().readValue(restored, PoJo.class);
        assertEquals(pojo.getName(), result.getName());
        assertEquals(pojo.getAddress(), result.getAddress());
    }

    @Test
    void nestingUpToTheLimitUnpacks() throws IOException {
        // 64 nested lists, the outermost being level 1, unpack; the innermost holds a nil
        assertEquals(MsgPack.MAX_DEPTH, listDepth(msgPack.unpackMapOrList(nestedLists(MsgPack.MAX_DEPTH))));
        assertInstanceOf(Map.class, msgPack.unpackMapOrList(nestedMaps(MsgPack.MAX_DEPTH)));
        assertInstanceOf(Map.class, msgPack.unpackMapOrList(nestedMix(MsgPack.MAX_DEPTH)));
    }

    @Test
    void nestingBeyondTheLimitIsRefused() {
        // one level more is refused by name; maps count like lists, and so does a mix of both
        var e = assertThrows(IOException.class, () -> msgPack.unpackMapOrList(nestedLists(MsgPack.MAX_DEPTH + 1)));
        assertEquals("Nesting deeper than 64 levels", e.getMessage());
        assertThrows(IOException.class, () -> msgPack.unpackMapOrList(nestedMaps(MsgPack.MAX_DEPTH + 1)));
        assertThrows(IOException.class, () -> msgPack.unpackMapOrList(nestedMix(MsgPack.MAX_DEPTH + 1)));
    }

    @Test
    void aDeeplyNestedPayloadIsRefusedBeforeTheStackRunsOut() {
        // 100,000 nested lists, about 100 KB, used to end in StackOverflowError, which no caller catches
        assertThrows(IOException.class, () -> msgPack.unpack(nestedLists(100_000)));
    }

    // each 0x91 opens a list of one element; the innermost element is nil (0xc0)
    private static byte[] nestedLists(int depth) {
        var bytes = new byte[depth + 1];
        Arrays.fill(bytes, 0, depth, (byte) 0x91);
        bytes[depth] = (byte) 0xc0;
        return bytes;
    }

    // each level is a map of one entry, the key "a" (0x81 0xa1 0x61); the innermost value is nil
    private static byte[] nestedMaps(int depth) {
        var out = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < depth; i++) {
            out.writeBytes(new byte[]{(byte) 0x81, (byte) 0xa1, 0x61});
        }
        out.write(0xc0);
        return out.toByteArray();
    }

    // maps and lists alternate, starting with a map
    private static byte[] nestedMix(int depth) {
        var out = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < depth; i++) {
            out.writeBytes(i % 2 == 0 ? new byte[]{(byte) 0x81, (byte) 0xa1, 0x61} : new byte[]{(byte) 0x91});
        }
        out.write(0xc0);
        return out.toByteArray();
    }

    private static int listDepth(Object value) {
        int depth = 0;
        while (value instanceof List<?> list) {
            depth++;
            value = list.isEmpty() ? null : list.getFirst();
        }
        return depth;
    }

    @Test
    void truncatedBytesAreAnIOExceptionNeverAnUncheckedOne() throws IOException {
        // the codec reports every malformed payload as an IOException, which EventEnvelope reports as
        // IllegalArgumentException as its contract says; msgpack-core used to throw its own unchecked exceptions
        var map = new HashMap<String, Object>();
        map.put("hello", "world");
        map.put("list", List.of(1, 2, 3));
        map.put("nested", Map.of("k", 1.5d));
        byte[] full = msgPack.pack(map);
        for (int cut = 1; cut < full.length; cut++) {
            byte[] truncated = Arrays.copyOf(full, cut);
            assertThrows(IOException.class, () -> msgPack.unpack(truncated), "cut at " + cut);
            assertThrows(IllegalArgumentException.class, () -> new EventEnvelope(truncated), "cut at " + cut);
        }
        // a top-level value that is not a Map or a List is an IOException too
        var e = assertThrows(IOException.class, () -> msgPack.unpackMapOrList(new byte[]{(byte) 0xa1, 0x61}));
        assertEquals("Packed input should be Map or List, Actual: STRING", e.getMessage());
        // and so is the never-used format byte inside a payload
        assertThrows(IOException.class, () -> msgPack.unpack(new byte[]{(byte) 0x81, (byte) 0xa1, 0x61, (byte) 0xc1}));
    }

    @Test
    void bytesAfterTheValueAreRefused() throws IOException {
        // the input holds exactly one value: a byte after the top-level container is a decoding error whatever it is,
        // through the event codec as through the canonical decoder, which always refused it; the Rust engine applies
        // the same rule, and the shared hostile-header vectors pin both inputs in both engines
        byte[] neverUsed = {(byte) 0x80, (byte) 0xc1};  // an empty map, then the format byte the specification never uses
        byte[] wellFormed = {(byte) 0x80, (byte) 0xc0}; // an empty map, then a well-formed nil
        for (byte[] bytes : List.of(neverUsed, wellFormed)) {
            var e = assertThrows(IOException.class, () -> msgPack.unpack(bytes));
            assertEquals("Unexpected bytes after the value at offset 1", e.getMessage());
            assertThrows(IOException.class, () -> msgPack.unpackMapOrList(bytes));
            assertThrows(IllegalArgumentException.class, () -> new EventEnvelope(bytes));
            assertThrows(IOException.class, () -> CanonicalPackager.decode(bytes));
        }
        // the value alone decodes: the rule refuses what follows the value, not the value
        assertEquals(Map.of(), msgPack.unpack(new byte[]{(byte) 0x80}));
    }

    @SuppressWarnings("unchecked")
    @Test
    void aUint64AboveLongMaxReadsAsBigInteger() throws IOException {
        // this serializer never writes one, but another MessagePack encoder may: a map with one key "n" holding 2^64 - 1
        byte[] bytes = {(byte) 0x81, (byte) 0xa1, 0x6e, (byte) 0xcf, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        var restored = (Map<String, Object>) msgPack.unpackMapOrList(bytes);
        assertEquals(new BigInteger("18446744073709551615"), restored.get("n"));
        // a uint64 that fits a long is a Long, and a small one an Integer, as before
        byte[] fits = {(byte) 0x81, (byte) 0xa1, 0x6e, (byte) 0xcf, 0, 0, 0, 1, 0, 0, 0, 0};
        assertEquals(4294967296L, ((Map<String, Object>) msgPack.unpackMapOrList(fits)).get("n"));
        byte[] small = {(byte) 0x81, (byte) 0xa1, 0x6e, (byte) 0xcf, 0, 0, 0, 0, 0, 0, 0, 5};
        assertEquals(5, ((Map<String, Object>) msgPack.unpackMapOrList(small)).get("n"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void anExtensionValueReadsAsNull() throws IOException {
        var config = AppConfigReader.getInstance();
        var transportNulls = "true".equalsIgnoreCase(config.getProperty("serializer.null.transport", "false"));
        // custom (extension) types are not supported: {"a": fixext1(type 1, 0x00), "b": "kept"} - "a" reads as null,
        // which the map treats like any null, and the reader stays in step for "b"
        byte[] bytes = {(byte) 0x82, (byte) 0xa1, 0x61, (byte) 0xd4, 0x01, 0x00,
                (byte) 0xa1, 0x62, (byte) 0xa4, 'k', 'e', 'p', 't'};
        var restored = (Map<String, Object>) msgPack.unpackMapOrList(bytes);
        assertEquals("kept", restored.get("b"));
        assertNull(restored.get("a"));
        assertEquals(transportNulls, restored.containsKey("a"));
        // in a list the null keeps its slot: [timestamp32 (fixext4, type -1), 1]
        byte[] list = {(byte) 0x92, (byte) 0xd6, (byte) 0xff, 0, 0, 0, 0, 0x01};
        assertEquals(Arrays.asList(null, 1), msgPack.unpackMapOrList(list));
    }
}
