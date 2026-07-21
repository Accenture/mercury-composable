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
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden vectors for the event envelope wire format - the cross-language
 * interoperability contract. The fixture file is shared verbatim with the
 * official Rust port, so any change that fails this test would also break a
 * peer implementation decoding the same bytes.
 * <p>
 * The comparison is semantic (decoded field values), never byte-identical:
 * MsgPack map ordering is unspecified and differs between languages.
 */
class EnvelopeWireFormatVectorTest {

    private static final String VECTOR_FILE = "/envelope-vectors/vectors.json";

    @SuppressWarnings("unchecked")
    @Test
    void goldenVectorsDecodeAndRoundTrip() throws Exception {
        Map<String, Object> fixture;
        try (InputStream in = this.getClass().getResourceAsStream(VECTOR_FILE)) {
            assertNotNull(in, "vector fixture must exist: " + VECTOR_FILE);
            String json = Utility.getInstance().stream2str(in);
            fixture = SimpleMapper.getInstance().getMapper().readValue(json, Map.class);
        }
        List<Map<String, Object>> vectors = (List<Map<String, Object>>) fixture.get("vectors");
        assertFalse(vectors.isEmpty());
        for (Map<String, Object> vector : vectors) {
            String name = (String) vector.get("name");
            EventEnvelope.Format format = EventEnvelope.Format.valueOf(
                    ((String) vector.get("format")).toUpperCase());
            byte[] bytes = Base64.getDecoder().decode((String) vector.get("base64"));
            Map<String, Object> expect = (Map<String, Object>) vector.get("expect");
            // 1) the frozen bytes decode with automatic format detection
            EventEnvelope decoded = new EventEnvelope(bytes);
            assertEquals(format, decoded.getWireFormat(), name + ": detected format");
            assertStandardView(name, expect, decoded);
            // 2) re-encode in the same format and decode again - semantic round trip
            EventEnvelope roundTrip = new EventEnvelope(decoded.toBytes(format));
            assertStandardView(name + " (round trip)", expect, roundTrip);
        }
    }

    /**
     * Compare the decoded envelope against the expected values through its
     * standard map view, using canonicalized JSON rendering (sorted map keys)
     * for deep semantic equality - map ordering is not part of the contract.
     */
    private void assertStandardView(String name, Map<String, Object> expect, EventEnvelope decoded) {
        Map<String, Object> actual = decoded.toMap(EventEnvelope.Format.STANDARD);
        var mapper = SimpleMapper.getInstance().getMapper();
        for (Map.Entry<String, Object> kv : expect.entrySet()) {
            String expectedJson = mapper.writeValueAsString(canonical(kv.getValue()));
            String actualJson = mapper.writeValueAsString(canonical(actual.get(kv.getKey())));
            assertEquals(expectedJson, actualJson, name + ": field '" + kv.getKey() + "'");
        }
    }

    @SuppressWarnings("unchecked")
    private Object canonical(Object value) {
        if (value instanceof Map) {
            Map<String, Object> sorted = new TreeMap<>();
            ((Map<String, Object>) value).forEach((k, v) -> sorted.put(k, canonical(v)));
            return sorted;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::canonical).toList();
        }
        return value;
    }
}
