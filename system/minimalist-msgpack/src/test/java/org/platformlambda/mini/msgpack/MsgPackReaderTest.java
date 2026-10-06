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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MsgPackReaderTest {
    private static final HexFormat HEX = HexFormat.of();

    private static MsgPackReader reader(String hex) {
        return new MsgPackReader(HEX.parseHex(hex));
    }

    @Test
    void nonMinimalEncodingsDecodeToTheSameValues() throws MsgPackException {
        // a writer never produces these, but the specification allows any width that holds the value
        assertEquals(5L, reader("cc05").readLong());
        assertEquals(5L, reader("cd0005").readLong());
        assertEquals(5L, reader("ce00000005").readLong());
        assertEquals(5L, reader("cf0000000000000005").readLong());
        assertEquals(5L, reader("d005").readLong());
        assertEquals(5L, reader("d10005").readLong());
        assertEquals(5L, reader("d200000005").readLong());
        assertEquals(5L, reader("d30000000000000005").readLong());
        assertEquals(-1L, reader("d3ffffffffffffffff").readLong());
        assertEquals(-1L, reader("d2ffffffff").readLong());
        assertEquals("a", reader("d90161").readString());
        assertEquals("a", reader("da000161").readString());
        assertEquals("a", reader("db0000000161").readString());
        assertArrayEquals(new byte[]{1}, reader("c50001" + "01").readBinary());
        assertEquals(1, reader("dc0001c0").readArrayHeader());
        assertEquals(1, reader("dd00000001c0").readArrayHeader());
        assertEquals(0, reader("de0000").readMapHeader());
        assertEquals(0, reader("df00000000").readMapHeader());
        assertEquals(List.of(), TestCodec.decode(HEX.parseHex("dd00000000")));
    }

    @Test
    void aUint64AboveLongMaxIsRefusedByReadLongAndReadByReadBigInteger() throws MsgPackException {
        var reader = reader("cf8000000000000000");
        var e = assertThrows(MsgPackException.class, reader::readLong);
        assertTrue(e.getMessage().contains("Integer overflow"), e.getMessage());
        assertTrue(e.getMessage().contains("9223372036854775808"), e.getMessage());
        // the failed read consumed nothing, so the same value can be read another way
        assertEquals(0, reader.position());
        assertEquals(BigInteger.TWO.pow(63), reader.readBigInteger());
        assertFalse(reader.hasNext());
        assertEquals(BigInteger.valueOf(-1).add(BigInteger.TWO.pow(64)), reader("cfffffffffffffffff").readBigInteger());
        // the other integer formats read as BigInteger too
        assertEquals(BigInteger.valueOf(127), reader("7f").readBigInteger());
        assertEquals(BigInteger.valueOf(-128), reader("d080").readBigInteger());
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE), reader("cf7fffffffffffffff").readBigInteger());
    }

    @Test
    void aFloat64IsNeverNarrowedAndAFloat32AlwaysWidensExactly() throws MsgPackException {
        var e = assertThrows(MsgPackException.class, () -> reader("cb3ff8000000000000").readFloat());
        assertEquals("Expected a float32 but found FLOAT64 at offset 0", e.getMessage());
        assertEquals(1.5d, reader("cb3ff8000000000000").readDouble());
        assertEquals(1.5d, reader("ca3fc00000").readDouble());
        assertEquals(1.5f, reader("ca3fc00000").readFloat());
        assertEquals(0.1d, reader("ca3dcccccd").readDouble(), 1e-8, "0.1f widened keeps the float32 value");
        assertEquals((double) 0.1f, reader("ca3dcccccd").readDouble(), "exactly the float32 value, not 0.1d");
    }

    @Test
    void strAndBinAreInterchangeableOnRead() throws MsgPackException {
        // an older encoder writes text under the raw (bin-less) family; a reader accepts both ways round
        assertEquals("hi", reader("c4026869").readString());
        assertArrayEquals("hi".getBytes(), reader("a26869").readBinary());
        // the format still tells them apart
        assertEquals(MsgPackType.BINARY, reader("c4026869").nextFormat().getType());
        assertEquals(MsgPackType.STRING, reader("a26869").nextFormat().getType());
    }

    @Test
    void invalidUtf8DecodesLeniently() throws MsgPackException {
        // the specification allows a str to hold an invalid byte sequence; the reader substitutes U+FFFD
        String text = reader("a3fffe61").readString();
        assertEquals(3, text.length());
        assertEquals('�', text.charAt(0));
        assertEquals('a', text.charAt(2));
    }

    @Test
    void theNeverUsedByteIsRefused() {
        var e = assertThrows(MsgPackException.class, () -> reader("c1").nextFormat());
        assertEquals("Invalid format byte 0xc1 at offset 0", e.getMessage());
        var nested = assertThrows(MsgPackException.class, () -> TestCodec.decode(HEX.parseHex("9201c1")));
        assertEquals("Invalid format byte 0xc1 at offset 2", nested.getMessage());
        assertThrows(MsgPackException.class, () -> reader("c1").skipValue());
    }

    @Test
    void theEndOfInputIsNamedByOffset() {
        var empty = assertThrows(MsgPackException.class, () -> reader("").nextFormat());
        assertEquals("Unexpected end of input at offset 0", empty.getMessage());
        var cut = assertThrows(MsgPackException.class, () -> reader("cd01").readLong());
        assertEquals("Unexpected end of input: 3 byte(s) needed at offset 0 but 2 remain", cut.getMessage());
        var inside = assertThrows(MsgPackException.class, () -> TestCodec.decode(HEX.parseHex("92cc05")));
        assertEquals("Unexpected end of input at offset 3", inside.getMessage());
        var pastValue = assertThrows(MsgPackException.class, () -> TestCodec.decode(HEX.parseHex("0101")));
        assertEquals("Unexpected bytes after the value at offset 1", pastValue.getMessage());
    }

    @Test
    void aTypeMismatchNamesWhatWasFound() {
        assertEquals("Expected an integer but found FIXSTR at offset 0",
                assertThrows(MsgPackException.class, () -> reader("a161").readLong()).getMessage());
        assertEquals("Expected nil but found POSITIVE_FIXINT at offset 0",
                assertThrows(MsgPackException.class, () -> reader("01").readNil()).getMessage());
        assertEquals("Expected a boolean but found NIL at offset 0",
                assertThrows(MsgPackException.class, () -> reader("c0").readBoolean()).getMessage());
        assertEquals("Expected an array but found FIXMAP at offset 0",
                assertThrows(MsgPackException.class, () -> reader("80").readArrayHeader()).getMessage());
        assertEquals("Expected a map but found FIXARRAY at offset 0",
                assertThrows(MsgPackException.class, () -> reader("90").readMapHeader()).getMessage());
        assertEquals("Expected a str but found POSITIVE_FIXINT at offset 0",
                assertThrows(MsgPackException.class, () -> reader("01").readString()).getMessage());
        assertEquals("Expected a bin but found BOOLEAN at offset 0",
                assertThrows(MsgPackException.class, () -> reader("c3").readBinary()).getMessage());
        assertEquals("Expected a float but found UINT8 at offset 0",
                assertThrows(MsgPackException.class, () -> reader("cc05").readDouble()).getMessage());
        assertEquals("Expected an integer but found FIXEXT1 at offset 0",
                assertThrows(MsgPackException.class, () -> reader("d40100").readBigInteger()).getMessage());
        // the offset is where the value starts, inside a container too
        assertEquals("Expected an integer but found FIXSTR at offset 2", assertThrows(MsgPackException.class, () -> {
            var reader = reader("9201a161");
            reader.readArrayHeader();
            reader.readLong();
            reader.readLong();
        }).getMessage());
    }

    @Test
    void aHeaderThatPromisesMoreThanTheInputHoldsIsRefusedAtTheHeader() throws MsgPackException {
        // array 32 with the maximum count and nothing after it: refused before anything is allocated
        var array = reader("ddffffffff");
        var e1 = assertThrows(MsgPackException.class, array::readArrayHeader);
        assertEquals("ARRAY32 declares 4294967295 element(s) but only 0 byte(s) follow at offset 0", e1.getMessage());
        assertEquals(0, array.position());
        // map 32 with a count whose doubling overflows a 32-bit int (the shape of CVE-2026-90473): refused by name
        var map = reader("df80000000" + "a16101a16201");
        var e2 = assertThrows(MsgPackException.class, map::readMapHeader);
        assertEquals("MAP32 declares 2147483648 element(s) but only 6 byte(s) follow at offset 0", e2.getMessage());
        assertEquals(0, map.position());
        assertThrows(MsgPackException.class, map::skipValue);
        assertEquals(0, map.position());
        // str 32 and bin 32 with the maximum length
        var str = reader("dbffffffff00");
        var e3 = assertThrows(MsgPackException.class, str::readString);
        assertEquals("STR32 declares 4294967295 byte(s) but only 1 follow at offset 0", e3.getMessage());
        assertEquals(0, str.position());
        assertThrows(MsgPackException.class, () -> reader("c6ffffffff").readBinary());
        // a count that the remaining bytes cannot hold, one byte per element and two per entry
        assertThrows(MsgPackException.class, () -> reader("dc0005c0c0").readArrayHeader());
        assertThrows(MsgPackException.class, () -> reader("de0001a1").readMapHeader());
        assertThrows(MsgPackException.class, () -> reader("93c0c0").readArrayHeader());
        assertThrows(MsgPackException.class, () -> reader("81a1").readMapHeader());
        // and a count the bytes can hold is accepted
        assertEquals(2, reader("dc0002c0c0").readArrayHeader());
        assertEquals(1, reader("de0001a161c0").readMapHeader());
    }

    @Test
    void skipValueStepsOverEveryFormatIncludingExtensions() throws MsgPackException {
        var out = new ByteArrayOutputStream();
        String[] elements = {
                "c0", "c3", "01", "ff", "ccff", "cd0100", "ce00010000", "cf0000000100000000",
                "d0df", "d1ff7f", "d2ffff7fff", "d3ffffffff7fffffff", "ca3fc00000", "cb3ff8000000000000",
                "a161", "d90161", "da000161", "db0000000161", "c40100", "c5000100", "c60000000100",
                "d40100", "d5010000", "d60100000000", "d7010000000000000000", "d80100000000000000000000000000000000",
                "c70001", "c70201aabb", "c800010100", "c90000000101ff",
                "81a161c0", "9190", "82a16191c0a162ca3fc00000"};
        for (var element : elements) {
            out.writeBytes(HEX.parseHex(element));
        }
        byte[] body = out.toByteArray();
        var framed = new ByteArrayOutputStream();
        framed.writeBytes(new byte[]{(byte) 0xdc, 0, (byte) elements.length});
        framed.writeBytes(body);
        framed.write(0xc3);   // the sentinel after the array
        byte[] bytes = framed.toByteArray();
        // skip the whole array in one call
        var whole = new MsgPackReader(bytes);
        whole.skipValue();
        assertTrue(whole.readBoolean());
        assertFalse(whole.hasNext());
        // skip each element in turn
        var each = new MsgPackReader(bytes);
        assertEquals(elements.length, each.readArrayHeader());
        for (var element : elements) {
            int before = each.position();
            each.skipValue();
            assertEquals(element.length() / 2, each.position() - before, element);
        }
        assertTrue(each.readBoolean());
        assertFalse(each.hasNext());
        // an extension value is recognized, never decoded: the value layer skips it
        assertEquals(MsgPackType.EXTENSION, reader("d40100").nextFormat().getType());
        assertEquals("<ext>", TestCodec.decode(HEX.parseHex("d40100")));
    }

    @Test
    void skipValueOnATruncatedValueRestoresThePosition() {
        byte[] full = TestCodec.encode(Map.of("a", List.of(1L, 2L, Map.of("b", "c"))));
        for (int cut = 0; cut < full.length; cut++) {
            var reader = new MsgPackReader(Arrays.copyOf(full, cut));
            assertThrows(MsgPackException.class, reader::skipValue, "cut at " + cut);
            assertEquals(0, reader.position(), "cut at " + cut);
        }
        // an extension that promises more than the input holds
        var ext = reader("c9ffffffff01aa");
        assertThrows(MsgPackException.class, ext::skipValue);
        assertEquals(0, ext.position());
        var fixext = reader("d801" + "0000");
        assertThrows(MsgPackException.class, fixext::skipValue);
        assertEquals(0, fixext.position());
    }

    @Test
    void deeplyNestedBytesAreSkippedWithoutRecursion() throws MsgPackException {
        // 100,000 nested arrays of one element: a recursive skip would exhaust the stack
        var bytes = new byte[100_001];
        Arrays.fill(bytes, 0, 100_000, (byte) 0x91);
        bytes[100_000] = (byte) 0xc0;
        var reader = new MsgPackReader(bytes);
        reader.skipValue();
        assertFalse(reader.hasNext());
        // the same for maps, and for an alternation of both
        var maps = new ByteArrayOutputStream();
        for (int i = 0; i < 100_000; i++) {
            maps.writeBytes(new byte[]{(byte) 0x81, (byte) 0xa1, 0x61});
        }
        maps.write(0xc0);
        var mapReader = new MsgPackReader(maps.toByteArray());
        mapReader.skipValue();
        assertFalse(mapReader.hasNext());
    }

    @Test
    void aReaderOverAWindowStaysInsideIt() throws MsgPackException {
        byte[] bytes = HEX.parseHex("ee" + "0102" + "ee");
        var reader = new MsgPackReader(bytes, 1, 2);
        assertEquals(1, reader.position());
        assertEquals(2, reader.remaining());
        assertEquals(1L, reader.readLong());
        assertEquals(2L, reader.readLong());
        assertFalse(reader.hasNext());
        assertEquals("Unexpected end of input at offset 3",
                assertThrows(MsgPackException.class, reader::nextFormat).getMessage());
        // a length that runs past the window is refused although the array has more bytes
        byte[] text = HEX.parseHex("a3616263" + "6465");
        var window = new MsgPackReader(text, 0, 3);
        assertThrows(MsgPackException.class, window::readString);
        assertEquals(0, window.position());
        assertEquals("abc", new MsgPackReader(text, 0, 4).readString());
    }

    @Test
    void aFailedReadLeavesThePositionUnchanged() {
        String[] cases = {"a161", "cd01", "c1", "ddffffffff", "cf8000000000000000"};
        for (var hex : cases) {
            var reader = reader("c0" + hex);
            assertThrows(MsgPackException.class, () -> {
                reader.readNil();
                reader.readLong();
            }, hex);
            assertEquals(1, reader.position(), hex);
        }
    }

    @Test
    void theConstructorRejectsANullArrayAndABadWindow() {
        assertThrows(NullPointerException.class, () -> new MsgPackReader(null));
        assertThrows(NullPointerException.class, () -> new MsgPackReader(null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> new MsgPackReader(new byte[2], 1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> new MsgPackReader(new byte[2], -1, 1));
        var empty = new MsgPackReader(new byte[2], 2, 0);
        assertFalse(empty.hasNext());
        assertNull(MsgPackFormat.of((byte) 0xc1));
    }
}
