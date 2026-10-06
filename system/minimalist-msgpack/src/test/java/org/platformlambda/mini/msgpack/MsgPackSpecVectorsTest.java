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

package org.platformlambda.mini.msgpack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Byte vectors derived by hand from the specification's format diagrams: the writer must produce exactly these bytes
 * and the reader must decode them to exactly these values. They pin every format boundary.
 */
class MsgPackSpecVectorsTest {
    private static final HexFormat HEX = HexFormat.of();

    record Vector(String name, Object value, String hex) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Vector> vectors() {
        var list = new ArrayList<Vector>();
        // nil and bool
        list.add(new Vector("nil", null, "c0"));
        list.add(new Vector("false", false, "c2"));
        list.add(new Vector("true", true, "c3"));
        // int family: non-negative values take the unsigned widths, negative values the signed widths
        list.add(new Vector("0 positive fixint", 0L, "00"));
        list.add(new Vector("1 positive fixint", 1L, "01"));
        list.add(new Vector("127 positive fixint", 127L, "7f"));
        list.add(new Vector("128 uint 8", 128L, "cc80"));
        list.add(new Vector("255 uint 8", 255L, "ccff"));
        list.add(new Vector("256 uint 16", 256L, "cd0100"));
        list.add(new Vector("65535 uint 16", 65535L, "cdffff"));
        list.add(new Vector("65536 uint 32", 65536L, "ce00010000"));
        list.add(new Vector("4294967295 uint 32", 4294967295L, "ceffffffff"));
        list.add(new Vector("4294967296 uint 64", 4294967296L, "cf0000000100000000"));
        list.add(new Vector("Long.MAX_VALUE uint 64", Long.MAX_VALUE, "cf7fffffffffffffff"));
        list.add(new Vector("-1 negative fixint", -1L, "ff"));
        list.add(new Vector("-32 negative fixint", -32L, "e0"));
        list.add(new Vector("-33 int 8", -33L, "d0df"));
        list.add(new Vector("-128 int 8", -128L, "d080"));
        list.add(new Vector("-129 int 16", -129L, "d1ff7f"));
        list.add(new Vector("-32768 int 16", -32768L, "d18000"));
        list.add(new Vector("-32769 int 32", -32769L, "d2ffff7fff"));
        list.add(new Vector("Integer.MIN_VALUE int 32", (long) Integer.MIN_VALUE, "d280000000"));
        list.add(new Vector("Integer.MIN_VALUE - 1 int 64", Integer.MIN_VALUE - 1L, "d3ffffffff7fffffff"));
        list.add(new Vector("Long.MIN_VALUE int 64", Long.MIN_VALUE, "d38000000000000000"));
        // float family: IEEE 754 big-endian
        list.add(new Vector("1.5f float 32", 1.5f, "ca3fc00000"));
        list.add(new Vector("-0.0f float 32", -0.0f, "ca80000000"));
        list.add(new Vector("1.5 float 64", 1.5d, "cb3ff8000000000000"));
        list.add(new Vector("0.1 float 64", 0.1d, "cb3fb999999999999a"));
        list.add(new Vector("-0.0 float 64", -0.0d, "cb8000000000000000"));
        list.add(new Vector("+Infinity float 64", Double.POSITIVE_INFINITY, "cb7ff0000000000000"));
        // str family: UTF-8 under the shortest header
        list.add(new Vector("empty fixstr", "", "a0"));
        list.add(new Vector("a fixstr", "a", "a161"));
        list.add(new Vector("é fixstr (2 bytes)", "é", "a2c3a9"));
        list.add(new Vector("€ fixstr (3 bytes)", "€", "a3e282ac"));
        list.add(new Vector("U+1F600 fixstr (4 bytes)", "😀", "a4f09f9880"));
        list.add(new Vector("31 chars fixstr", "a".repeat(31), "bf" + "61".repeat(31)));
        list.add(new Vector("32 chars str 8", "a".repeat(32), "d920" + "61".repeat(32)));
        list.add(new Vector("255 chars str 8", "a".repeat(255), "d9ff" + "61".repeat(255)));
        list.add(new Vector("256 chars str 16", "a".repeat(256), "da0100" + "61".repeat(256)));
        list.add(new Vector("11 chars of 3 bytes = 33 bytes str 8", "€".repeat(11), "d921" + "e282ac".repeat(11)));
        // bin family
        list.add(new Vector("empty bin 8", new byte[0], "c400"));
        list.add(new Vector("one byte bin 8", new byte[]{(byte) 0xff}, "c401ff"));
        list.add(new Vector("255 bytes bin 8", new byte[255], "c4ff" + "00".repeat(255)));
        list.add(new Vector("256 bytes bin 16", new byte[256], "c50100" + "00".repeat(256)));
        // array family
        list.add(new Vector("empty fixarray", List.of(), "90"));
        list.add(new Vector("[1, -1, nil] fixarray", Arrays.asList(1L, -1L, null), "9301ffc0"));
        list.add(new Vector("15 nils fixarray", Collections.nCopies(15, null), "9f" + "c0".repeat(15)));
        list.add(new Vector("16 nils array 16", Collections.nCopies(16, null), "dc0010" + "c0".repeat(16)));
        // map family
        list.add(new Vector("empty fixmap", Map.of(), "80"));
        list.add(new Vector("{a: 1} fixmap", Map.of("a", 1L), "81a16101"));
        list.add(new Vector("{a: {b: [true]}} nested", Map.of("a", Map.of("b", List.of(true))), "81a16181a16291c3"));
        list.add(new Vector("15 entries fixmap", entries(15), "8f" + keyNils(15)));
        list.add(new Vector("16 entries map 16", entries(16), "de0010" + keyNils(16)));
        return list.stream();
    }

    // keys "0" to "9" and "A" to "F" in order, each with a nil value
    private static Map<String, Object> entries(int count) {
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < count; i++) {
            map.put(Integer.toHexString(i).toUpperCase(), null);
        }
        return map;
    }

    private static String keyNils(int count) {
        var sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("a1").append(HEX.formatHex(Integer.toHexString(i).toUpperCase().getBytes())).append("c0");
        }
        return sb.toString();
    }

    @ParameterizedTest
    @MethodSource("vectors")
    void theWriterProducesTheSpecifiedBytes(Vector vector) {
        assertEquals(vector.hex(), HEX.formatHex(TestCodec.encode(vector.value())));
    }

    @ParameterizedTest
    @MethodSource("vectors")
    void theReaderDecodesTheSpecifiedBytes(Vector vector) throws MsgPackException {
        Object decoded = TestCodec.decode(HEX.parseHex(vector.hex()));
        assertTrue(TestCodec.same(vector.value(), decoded), vector.name() + " decoded to " + decoded);
    }

    @Test
    void the32BitHeadersAreUsedBeyond65535() throws MsgPackException {
        // str 32, bin 32, array 32 and map 32 take a 4-byte big-endian length or count
        byte[] text = TestCodec.encode("a".repeat(65536));
        assertEquals("db00010000", HEX.formatHex(text, 0, 5));
        assertEquals(5 + 65536, text.length);
        byte[] bin = TestCodec.encode(new byte[65536]);
        assertEquals("c600010000", HEX.formatHex(bin, 0, 5));
        assertEquals(5 + 65536, bin.length);
        byte[] array = TestCodec.encode(Collections.nCopies(65536, null));
        assertEquals("dd00010000", HEX.formatHex(array, 0, 5));
        assertEquals(5 + 65536, array.length);
        byte[] array16 = TestCodec.encode(Collections.nCopies(65535, null));
        assertEquals("dcffff", HEX.formatHex(array16, 0, 3));
        var writer = new MsgPackWriter();
        writer.writeMapHeader(65536);
        assertEquals("df00010000", HEX.formatHex(writer.toByteArray()));
        writer.reset();
        writer.writeMapHeader(65535);
        assertEquals("deffff", HEX.formatHex(writer.toByteArray()));
        // and they all read back
        assertEquals(65536, ((String) TestCodec.decode(text)).length());
        assertArrayEquals(new byte[65536], (byte[]) TestCodec.decode(bin));
        assertEquals(65536, ((List<?>) TestCodec.decode(array)).size());
        var reader = new MsgPackReader(HEX.parseHex("df00010000"));
        assertEquals(MsgPackFormat.MAP32, reader.nextFormat());
    }
}
