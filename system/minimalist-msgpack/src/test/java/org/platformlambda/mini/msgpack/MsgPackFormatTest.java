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

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MsgPackFormatTest {
    // the "Overview" table of the specification (spec.md, "Formats"): first byte in hex -> format name
    private static final String[] SPECIFICATION_TABLE = {
            "00-7f POSITIVE_FIXINT", "80-8f FIXMAP", "90-9f FIXARRAY", "a0-bf FIXSTR", "c0 NIL", "c1 (never used)",
            "c2 BOOLEAN", "c3 BOOLEAN", "c4 BIN8", "c5 BIN16", "c6 BIN32", "c7 EXT8", "c8 EXT16", "c9 EXT32",
            "ca FLOAT32", "cb FLOAT64", "cc UINT8", "cd UINT16", "ce UINT32", "cf UINT64",
            "d0 INT8", "d1 INT16", "d2 INT32", "d3 INT64", "d4 FIXEXT1", "d5 FIXEXT2", "d6 FIXEXT4", "d7 FIXEXT8",
            "d8 FIXEXT16", "d9 STR8", "da STR16", "db STR32", "dc ARRAY16", "dd ARRAY32", "de MAP16", "df MAP32",
            "e0-ff NEGATIVE_FIXINT"};

    @Test
    void everyFirstByteFollowsTheSpecificationTable() {
        var expected = new String[256];
        for (var row : SPECIFICATION_TABLE) {
            var parts = row.split(" ", 2);
            var range = parts[0].split("-");
            int from = Integer.parseInt(range[0], 16);
            int to = Integer.parseInt(range[range.length - 1], 16);
            for (int b = from; b <= to; b++) {
                expected[b] = parts[1];
            }
        }
        for (int b = 0; b < 256; b++) {
            var format = MsgPackFormat.of((byte) b);
            String hex = String.format("0x%02x", b);
            if (expected[b].startsWith("(")) {
                assertNull(format, hex + " is never used");
            } else {
                assertEquals(expected[b], format.name(), hex);
            }
        }
    }

    @Test
    void everyFormatDecodesToTheTypeOfTheSpecificationTable() {
        // the "Deserialization: format to type conversion" table of the specification
        assertType(MsgPackType.INTEGER, MsgPackFormat.POSITIVE_FIXINT, MsgPackFormat.NEGATIVE_FIXINT,
                MsgPackFormat.INT8, MsgPackFormat.INT16, MsgPackFormat.INT32, MsgPackFormat.INT64,
                MsgPackFormat.UINT8, MsgPackFormat.UINT16, MsgPackFormat.UINT32, MsgPackFormat.UINT64);
        assertType(MsgPackType.NIL, MsgPackFormat.NIL);
        assertType(MsgPackType.BOOLEAN, MsgPackFormat.BOOLEAN);
        assertType(MsgPackType.FLOAT, MsgPackFormat.FLOAT32, MsgPackFormat.FLOAT64);
        assertType(MsgPackType.STRING, MsgPackFormat.FIXSTR, MsgPackFormat.STR8, MsgPackFormat.STR16,
                MsgPackFormat.STR32);
        assertType(MsgPackType.BINARY, MsgPackFormat.BIN8, MsgPackFormat.BIN16, MsgPackFormat.BIN32);
        assertType(MsgPackType.ARRAY, MsgPackFormat.FIXARRAY, MsgPackFormat.ARRAY16, MsgPackFormat.ARRAY32);
        assertType(MsgPackType.MAP, MsgPackFormat.FIXMAP, MsgPackFormat.MAP16, MsgPackFormat.MAP32);
        assertType(MsgPackType.EXTENSION, MsgPackFormat.FIXEXT1, MsgPackFormat.FIXEXT2, MsgPackFormat.FIXEXT4,
                MsgPackFormat.FIXEXT8, MsgPackFormat.FIXEXT16, MsgPackFormat.EXT8, MsgPackFormat.EXT16,
                MsgPackFormat.EXT32);
        // and the table above is complete: every format appears in exactly one row
        Set<MsgPackFormat> all = EnumSet.allOf(MsgPackFormat.class);
        // 37 rows: true and false share BOOLEAN, and 0xc1 has no format
        assertEquals(35, all.size());
        assertTrue(all.stream().allMatch(f -> f.getType() != null));
    }

    private static void assertType(MsgPackType type, MsgPackFormat... formats) {
        for (var format : formats) {
            assertEquals(type, format.getType(), format.name());
        }
    }
}
