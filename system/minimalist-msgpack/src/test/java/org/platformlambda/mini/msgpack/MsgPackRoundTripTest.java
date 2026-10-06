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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MsgPackRoundTripTest {
    private static final HexFormat HEX = HexFormat.of();
    private static final long SEED = 20261006L;

    @Test
    void randomDocumentsRoundTrip() throws MsgPackException {
        var random = new Random(SEED);
        long bytes = 0;
        for (int i = 0; i < 3000; i++) {
            Object document = TestCodec.randomDocument(random);
            byte[] encoded = TestCodec.encode(document);
            bytes += encoded.length;
            Object decoded = TestCodec.decode(encoded);
            assertTrue(TestCodec.same(document, decoded), "document " + i);
            // encoding the decoded form gives the same bytes: the writer is deterministic for a given structure
            assertArrayEquals(encoded, TestCodec.encode(decoded), "document " + i);
        }
        assertTrue(bytes > 1_000_000, "the corpus is substantial: " + bytes + " bytes");
    }

    @Test
    void everyProperPrefixOfARandomDocumentIsRefused() {
        // MessagePack is prefix-free, so a truncated value can never read as a complete one; the reader must
        // refuse every cut with a MsgPackException and nothing else
        var random = new Random(SEED + 1);
        int cuts = 0;
        for (int i = 0; i < 300; i++) {
            byte[] encoded = TestCodec.encode(TestCodec.randomDocument(random));
            for (int cut = 0; cut < encoded.length; cut++) {
                byte[] prefix = Arrays.copyOf(encoded, cut);
                assertThrows(MsgPackException.class, () -> TestCodec.decode(prefix), "document " + i + " cut at " + cut);
                cuts++;
            }
        }
        assertTrue(cuts > 50_000, "cuts checked: " + cuts);
    }

    @Test
    void writerAndReaderAgreeOnEveryIntegerBoundary() throws MsgPackException {
        for (long boundary : TestCodec.INTEGER_BOUNDARIES) {
            for (long delta = -2; delta <= 2; delta++) {
                long value = boundary + delta;
                if ((delta < 0 && value > boundary) || (delta > 0 && value < boundary)) {
                    continue;   // wrapped around Long.MIN_VALUE or Long.MAX_VALUE
                }
                byte[] encoded = new MsgPackWriter().writeLong(value).toByteArray();
                assertEquals(value, new MsgPackReader(encoded).readLong(), Long.toString(value));
                assertEquals(value, new MsgPackReader(encoded).readBigInteger().longValueExact(), Long.toString(value));
            }
        }
    }

    @Test
    void floatsRoundTripBitForBitIncludingNaNPayloads() throws MsgPackException {
        int nanBits = 0x7fc00001;
        long nanLongBits = 0x7ff8000000000001L;
        var writer = new MsgPackWriter();
        writer.writeFloat(Float.intBitsToFloat(nanBits));
        writer.writeDouble(Double.longBitsToDouble(nanLongBits));
        writer.writeFloat(Float.NEGATIVE_INFINITY);
        writer.writeDouble(Double.MIN_VALUE);
        var reader = new MsgPackReader(writer.toByteArray());
        assertEquals(nanBits, Float.floatToRawIntBits(reader.readFloat()));
        assertEquals(nanLongBits, Double.doubleToRawLongBits(reader.readDouble()));
        assertEquals(Float.NEGATIVE_INFINITY, reader.readFloat());
        assertEquals(Double.MIN_VALUE, reader.readDouble());
        assertFalse(reader.hasNext());
    }

    @Test
    void stringsAreWrittenAsGetBytesWritesThem() throws MsgPackException {
        // an unpaired surrogate becomes '?', as String.getBytes(UTF_8) writes it (and as msgpack-java does)
        String unpaired = "a\ud800b";
        byte[] expected = unpaired.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, new byte[]{'a', '?', 'b'});
        assertEquals("a3" + HEX.formatHex(expected), HEX.formatHex(TestCodec.encode(unpaired)));
        assertEquals("a?b", TestCodec.decode(TestCodec.encode(unpaired)));
        // a non-ASCII char at the end of a short string: the ASCII attempt gives way to the UTF-8 path
        String late = "x".repeat(100) + "é";
        byte[] lateBytes = late.getBytes(StandardCharsets.UTF_8);
        assertEquals("d9" + HEX.toHexDigits((byte) lateBytes.length) + HEX.formatHex(lateBytes),
                HEX.formatHex(TestCodec.encode(late)));
        assertEquals(late, TestCodec.decode(TestCodec.encode(late)));
        // a long string takes the UTF-8 path directly
        String longText = "€".repeat(300) + "y".repeat(300);
        assertEquals(longText, TestCodec.decode(TestCodec.encode(longText)));
        // and ASCII of every short length
        for (int n = 0; n <= 130; n++) {
            String text = "z".repeat(n);
            assertEquals(text, TestCodec.decode(TestCodec.encode(text)), "length " + n);
        }
    }

    @Test
    void headersAreTheShortestForEveryLength() throws MsgPackException {
        int[] lengths = {0, 31, 32, 255, 256, 65535, 65536};
        for (int n : lengths) {
            byte[] str = TestCodec.encode("s".repeat(n));
            byte[] bin = TestCodec.encode(new byte[n]);
            int strHeader = n < 32 ? 1 : n < 256 ? 2 : n < 65536 ? 3 : 5;
            int binHeader = n < 256 ? 2 : n < 65536 ? 3 : 5;
            assertEquals(strHeader + n, str.length, "str " + n);
            assertEquals(binHeader + n, bin.length, "bin " + n);
            assertEquals(n, ((String) TestCodec.decode(str)).length(), "str " + n);
            assertEquals(n, ((byte[]) TestCodec.decode(bin)).length, "bin " + n);
        }
        int[] counts = {0, 15, 16, 65535, 65536};
        for (int n : counts) {
            int header = n < 16 ? 1 : n < 65536 ? 3 : 5;
            var writer = new MsgPackWriter();
            writer.writeArrayHeader(n);
            assertEquals(header, writer.size(), "array " + n);
            writer.writeMapHeader(n);
            assertEquals(2 * header, writer.size(), "map " + n);
        }
    }

    @Test
    void theWriterGrowsFromAnEmptyBufferAndIsReusable() throws MsgPackException {
        var writer = new MsgPackWriter(0);
        var items = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            items.add((long) i * 1_000_003L);
        }
        items.add("t".repeat(70_000));
        TestCodec.write(writer, items);
        byte[] first = writer.toByteArray();
        assertEquals(first.length, writer.size());
        assertTrue(TestCodec.same(items, TestCodec.decode(first)));
        // the copy is independent of the writer
        byte[] again = writer.toByteArray();
        assertNotSame(first, again);
        again[0] = 0;
        assertArrayEquals(first, writer.toByteArray());
        // reset keeps the buffer and forgets the bytes
        writer.reset();
        assertEquals(0, writer.size());
        writer.writeBoolean(true);
        assertEquals("c3", HEX.formatHex(writer.toByteArray()));
    }

    @Test
    void writeBinaryTakesAWindow() throws MsgPackException {
        byte[] source = {9, 1, 2, 3, 9};
        byte[] encoded = new MsgPackWriter().writeBinary(source, 1, 3).toByteArray();
        assertEquals("c403010203", HEX.formatHex(encoded));
        assertArrayEquals(new byte[]{1, 2, 3}, new MsgPackReader(encoded).readBinary());
        assertThrows(IndexOutOfBoundsException.class, () -> new MsgPackWriter().writeBinary(source, 3, 3));
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MsgPackWriter(-1));
        assertThrows(IllegalArgumentException.class, () -> new MsgPackWriter().writeArrayHeader(-1));
        assertThrows(IllegalArgumentException.class, () -> new MsgPackWriter().writeMapHeader(-1));
        assertThrows(NullPointerException.class, () -> new MsgPackWriter().writeString(null));
        assertThrows(NullPointerException.class, () -> new MsgPackWriter().writeBinary(null));
        assertThrows(IllegalArgumentException.class, () -> TestCodec.encode(List.of(new Object())));
    }

    @Test
    void aDocumentWithEveryTypeRoundTrips() throws MsgPackException {
        var document = new java.util.LinkedHashMap<String, Object>();
        document.put("nil", null);
        document.put("bool", true);
        document.put("int", 42L);
        document.put("negative", -1_000_000L);
        document.put("float", 1.25f);
        document.put("double", Math.PI);
        document.put("text", "café € 😀");
        document.put("bytes", new byte[]{0, 1, 2, (byte) 0xff});
        document.put("list", Arrays.asList(1L, null, "two", List.of()));
        document.put("map", java.util.Map.of("k", java.util.Map.of()));
        byte[] encoded = TestCodec.encode(document);
        assertTrue(TestCodec.same(document, TestCodec.decode(encoded)));
    }
}
