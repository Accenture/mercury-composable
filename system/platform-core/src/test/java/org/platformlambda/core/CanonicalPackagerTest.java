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
import org.platformlambda.core.serializers.CanonicalPackager;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The packager's own rules (RFC-0002): the builder's validation, key handling, the ordered read, the strict read
 * and the bounds. The byte-level contract is pinned by {@link CanonicalPackagerVectorsTest}.
 */
class CanonicalPackagerTest {
    private static final HexFormat HEX = HexFormat.of();

    @Test
    void theSameContentGivesTheSameBytesWhateverTheInsertionOrder() throws IOException {
        var forward = new LinkedHashMap<String, Object>();
        forward.put("b", 1);
        forward.put("a", Map.of("y", 2, "x", 1));
        var backward = new LinkedHashMap<String, Object>();
        backward.put("a", Map.of("x", 1, "y", 2));
        backward.put("b", 1L);
        assertArrayEquals(CanonicalPackager.encode(forward), CanonicalPackager.encode(backward));
        var first = CanonicalPackager.builder().add("b.json", forward).add("a.json", backward).build();
        var second = CanonicalPackager.builder().add("a.json", backward).add("b.json", forward).build();
        assertArrayEquals(first, second);
    }

    @SuppressWarnings("unchecked")
    @Test
    void jsonTextsThatDifferOnlyInKeyOrderAndWhitespaceGiveTheSamePackage() throws IOException {
        // the motivating case: Gson keeps the key order of the text, so the packager does the ordering
        var mapper = org.platformlambda.core.serializers.SimpleMapper.getInstance().getMapper();
        var one = "{\"nodes\":[{\"alias\":\"root\",\"types\":[\"Root\"]}],\"name\":\"quote\",\"n\":7}";
        var other = "{ \"n\": 7,\n  \"name\": \"quote\",\n  \"nodes\": [ { \"types\": [ \"Root\" ], \"alias\": \"root\" } ] }";
        Map<String, Object> first = mapper.readValue(one, Map.class);
        Map<String, Object> second = mapper.readValue(other, Map.class);
        assertNotEquals(new ArrayList<>(first.keySet()), new ArrayList<>(second.keySet()), "the parsed key orders differ");
        var a = CanonicalPackager.builder().manifest("graph_id", "quote").add("quote.json", first).build();
        var b = CanonicalPackager.builder().manifest("graph_id", "quote").add("quote.json", second).build();
        assertArrayEquals(a, b);
        assertArrayEquals(a, CanonicalPackager.builder().manifest("graph_id", "quote")
                .add("quote.json", new java.util.HashMap<>(first)).build());
    }

    @Test
    void keysSortByUtf8BytesNotUtf16() throws IOException {
        // U+1F600 is the surrogate pair D83D DE00 in UTF-16 (sorts before U+FF5E) but F0 9F 98 80 in UTF-8
        var map = new LinkedHashMap<String, Object>();
        map.put("\uD83D\uDE00", 1);
        map.put("～", 2); // U+FF5E, the fullwidth tilde
        var decoded = (Map<?, ?>) CanonicalPackager.decode(CanonicalPackager.encode(map));
        assertEquals(List.of("～", "\uD83D\uDE00"), new ArrayList<>(decoded.keySet()));
    }

    @Test
    void aNonTextKeyBecomesTextAndACollisionOrANullKeyIsAnError() throws IOException {
        var numbered = new LinkedHashMap<>();
        numbered.put(10, "ten");
        numbered.put(2, "two");
        var decoded = (Map<?, ?>) CanonicalPackager.decode(CanonicalPackager.encode(numbered));
        assertEquals(List.of("10", "2"), new ArrayList<>(decoded.keySet()), "text order, not numeric");
        var collision = new LinkedHashMap<>();
        collision.put(1, "a");
        collision.put("1", "b");
        var e = assertThrows(IllegalArgumentException.class, () -> CanonicalPackager.encode(collision));
        assertTrue(e.getMessage().contains("Two keys become '1'"), e.getMessage());
        var nullKey = new LinkedHashMap<String, Object>();
        nullKey.put(null, "x");
        assertThrows(IllegalArgumentException.class, () -> CanonicalPackager.encode(nullKey));
    }

    @Test
    void aNullValueIsKeptAsNil() throws IOException {
        var map = new LinkedHashMap<String, Object>();
        map.put("a", null);
        assertEquals("81a1" + "61c0", HEX.formatHex(CanonicalPackager.encode(map)));
    }

    @Test
    void exactNumbersAndDatesTravelAsStrings() throws IOException {
        assertEquals("a5" + HEX.formatHex("10.50".getBytes()), HEX.formatHex(CanonicalPackager.encode(new BigDecimal("10.50"))));
        assertEquals("a130", HEX.formatHex(CanonicalPackager.encode(new BigDecimal("0.00"))), "a zero of any scale is 0");
        assertEquals("a43130" + "3030", HEX.formatHex(CanonicalPackager.encode(new BigDecimal("1E+3"))), "plain notation");
        var epoch = Instant.parse("2026-10-01T00:00:00Z");
        assertArrayEquals(CanonicalPackager.encode(java.util.Date.from(epoch)), CanonicalPackager.encode(epoch));
        var text = (String) CanonicalPackager.decode(CanonicalPackager.encode(new Date(0)));
        assertTrue(text.startsWith("1970-01-01T00:00:00"), text);
    }

    @Test
    void smallIntegerTypesAllUseTheSmallestEncoding() throws IOException {
        for (Object n : new Object[]{(byte) 5, (short) 5, 5, 5L, new AtomicInteger(5), new AtomicLong(5)}) {
            assertEquals("05", HEX.formatHex(CanonicalPackager.encode(n)), n.getClass().getSimpleName());
        }
        assertEquals("cc80", HEX.formatHex(CanonicalPackager.encode(128)));
    }

    @Test
    void floatsAreFloat64AndFiniteOnly() throws IOException {
        assertEquals("cb3ff8000000000000", HEX.formatHex(CanonicalPackager.encode(1.5d)));
        assertThrows(IllegalArgumentException.class, () -> CanonicalPackager.encode(1.5f));
        assertThrows(IllegalArgumentException.class, () -> CanonicalPackager.encode(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> CanonicalPackager.encode(Double.NEGATIVE_INFINITY));
        // the integer 1 and the float 1.0 are different content
        assertNotEquals(HEX.formatHex(CanonicalPackager.encode(1)), HEX.formatHex(CanonicalPackager.encode(1.0d)));
    }

    @Test
    void anUnsupportedTypeIsRejectedNamingItsPath() {
        var map = Map.of("nodes", List.of(Map.of("when", new StringBuilder("x"))));
        var e = assertThrows(IllegalArgumentException.class, () -> CanonicalPackager.encode(map));
        assertTrue(e.getMessage().contains("java.lang.StringBuilder"), e.getMessage());
        assertTrue(e.getMessage().contains("$.nodes[0].when"), e.getMessage());
    }

    @Test
    void theBuilderRefusesReservedDuplicateAndEmptyNames() {
        var b = CanonicalPackager.builder();
        assertThrows(IllegalArgumentException.class, () -> b.manifest("format", "x"));
        assertThrows(IllegalArgumentException.class, () -> b.manifest("format_version", "9"));
        assertThrows(IllegalArgumentException.class, () -> b.manifest("", "x"));
        assertThrows(IllegalArgumentException.class, () -> b.manifest("k", null));
        b.manifest("graph_id", "quote");
        assertThrows(IllegalArgumentException.class, () -> b.manifest("graph_id", "again"));
        b.add("a.json", Map.of());
        assertThrows(IllegalArgumentException.class, () -> b.add("a.json", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> b.add("", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> b.add("n.json", null));
    }

    @Test
    void unpackReturnsOrderedMapsWithTheManifestFirst() throws IOException {
        var bytes = CanonicalPackager.builder().manifest("graph_id", "quote").manifest("version", "1.0.0")
                .add("quote.json", Map.of("z", 1, "a", 2)).add("quote-fees.json", Map.of("m", 1)).build();
        var unpacked = CanonicalPackager.unpack(bytes);
        assertEquals(List.of("format", "format_version", "graph_id", "version"),
                new ArrayList<>(unpacked.manifest().keySet()));
        assertEquals("quote", unpacked.manifest().get("graph_id"));
        assertEquals(List.of("quote-fees.json", "quote.json"), new ArrayList<>(unpacked.maps().keySet()));
        assertEquals(List.of("a", "z"), new ArrayList<>(unpacked.maps().get("quote.json").keySet()));
        // manifest sorts before maps, so a reader can read the metadata before decoding any map
        var top = (Map<?, ?>) CanonicalPackager.decode(bytes);
        assertEquals(List.of("manifest", "maps"), new ArrayList<>(top.keySet()));
    }

    @Test
    void aStrictReadRejectsWhatIsNotCanonicalButANonStrictReadDecodesIt() throws IOException {
        // the same content with a wider integer than needed: valid MsgPack, not the canonical bytes
        var canonical = CanonicalPackager.builder().add("g.json", Map.of("n", 1)).build();
        var wide = HEX.formatHex(canonical).replace("a16e01", "a16ecd0001");
        assertNotEquals(HEX.formatHex(canonical), wide);
        var bytes = HEX.parseHex(wide);
        var e = assertThrows(IOException.class, () -> CanonicalPackager.unpack(bytes));
        assertTrue(e.getMessage().startsWith("Not canonical"), e.getMessage());
        assertEquals(1L, CanonicalPackager.unpack(bytes, false).maps().get("g.json").get("n"));
    }

    @Test
    void aRoundTripKeepsTypesAndOrder() throws IOException {
        var content = new LinkedHashMap<String, Object>();
        content.put("n", 7);
        content.put("f", 2.5d);
        content.put("s", "text");
        content.put("b", new byte[]{1, 2, 3});
        content.put("flag", true);
        content.put("none", null);
        content.put("list", List.of(1, Map.of("k", "v")));
        var bytes = CanonicalPackager.builder().add("c.json", content).build();
        var back = CanonicalPackager.unpack(bytes).maps().get("c.json");
        assertEquals(7L, back.get("n"));
        assertEquals(2.5d, back.get("f"));
        assertEquals("text", back.get("s"));
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) back.get("b"));
        assertEquals(true, back.get("flag"));
        assertTrue(back.containsKey("none"));
        assertNull(back.get("none"));
        assertEquals(List.of("b", "f", "flag", "list", "n", "none", "s"), new ArrayList<>(back.keySet()));
    }

    @Test
    void nestingBeyondTheBoundIsRefusedOnBothSides() {
        Object deep = "x";
        for (int i = 0; i < 70; i++) {
            deep = List.of(deep);
        }
        var tooDeep = deep;
        assertThrows(IllegalArgumentException.class, () -> CanonicalPackager.encode(tooDeep));
        var bytes = new byte[140];
        for (int i = 0; i < 70; i++) {
            bytes[i] = (byte) 0x91;
        }
        bytes[70] = (byte) 0xc0;
        var e = assertThrows(IOException.class, () -> CanonicalPackager.decode(java.util.Arrays.copyOf(bytes, 71)));
        assertTrue(e.getMessage().contains("Nesting deeper"), e.getMessage());
    }
}
