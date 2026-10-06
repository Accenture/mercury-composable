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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.CanonicalPackager;
import org.platformlambda.core.serializers.MsgPack;
import org.platformlambda.core.serializers.SimpleMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared hostile-header vectors ({@code msgpack-hostile-header-vectors.json}, byte-identical with the Rust engine's
 * {@code crates/platform-core/tests/resources/} copy; Rust twin {@code msgpack_hostile_header_vectors}): a MsgPack header
 * that promises more than the input holds - a str, bin or ext length or an array or map count beyond the remaining bytes,
 * the never-used byte {@code 0xc1}, nesting beyond the shared 64-level bound - is refused as a decoding error by every
 * decoder this engine exposes to bytes from outside the process: {@code MsgPack.unpack} (the path the Event API feeds),
 * {@code EventEnvelope} and {@code CanonicalPackager.decode}. Never an unchecked exception, a stack overflow or an
 * allocation sized by the header. The controls decode to exactly their JSON value, so a decoder that refuses everything
 * fails too.
 */
class MsgPackHostileHeaderVectorsTest {
    private static final HexFormat HEX = HexFormat.of();
    private static final MsgPack msgPack = new MsgPack();

    private static JsonObject vectors() throws IOException {
        try (var in = MsgPackHostileHeaderVectorsTest.class.getResourceAsStream("/msgpack-hostile-header-vectors.json")) {
            assertNotNull(in, "msgpack-hostile-header-vectors.json is missing");
            var doc = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("mercury-msgpack-hostile-header-vectors", doc.get("format").getAsString());
            assertEquals("1", doc.get("version").getAsString());
            return doc;
        }
    }

    @Test
    void everyHostileHeaderIsRefusedAtTheHeader() throws IOException {
        int checked = 0;
        for (var item : vectors().getAsJsonArray("vectors")) {
            var v = item.getAsJsonObject();
            if (!"reject".equals(v.get("expect").getAsString())) {
                continue;
            }
            var id = v.get("id").getAsString();
            byte[] bytes = HEX.parseHex(v.get("hex").getAsString());
            // the event payload codec, which the Event API feeds: a decoding error, never an unchecked exception
            assertThrows(IOException.class, () -> msgPack.unpack(bytes), id + " through MsgPack.unpack");
            // the canonical decoder
            assertThrows(IOException.class, () -> CanonicalPackager.decode(bytes), id + " through CanonicalPackager");
            // and the envelope, which reports a decoding error as IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> new EventEnvelope(bytes), id + " through EventEnvelope");
            checked++;
        }
        assertTrue(checked >= 20, "rejections checked: " + checked);
    }

    @Test
    void everyControlDecodesToItsValue() throws IOException {
        int checked = 0;
        for (var item : vectors().getAsJsonArray("vectors")) {
            var v = item.getAsJsonObject();
            if (!"accept".equals(v.get("expect").getAsString())) {
                continue;
            }
            var id = v.get("id").getAsString();
            byte[] bytes = HEX.parseHex(v.get("hex").getAsString());
            JsonElement expected = v.get("value");
            assertEquals(expected, asJson(msgPack.unpackMapOrList(bytes)), id + " through MsgPack");
            assertEquals(expected, asJson(CanonicalPackager.decode(bytes)), id + " through CanonicalPackager");
            checked++;
        }
        assertTrue(checked >= 8, "controls checked: " + checked);
    }

    // the decoded Map/List, rendered as JSON and parsed back, compares with the vector's value structurally
    private static JsonElement asJson(Object decoded) {
        return JsonParser.parseString(SimpleMapper.getInstance().getMapper().writeValueAsString(decoded));
    }
}
