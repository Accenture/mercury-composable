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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.serializers.CanonicalPackager;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the shared conformance vectors of the canonical MsgPack packager (RFC-0002). The file is engine-neutral and
 * is the byte-for-byte contract between the Java and Rust engines: its expected bytes come from an independent
 * encoder written from the specification, so an engine that agrees with the file agrees with every other engine
 * that does. A JSON number without a fraction or exponent is an integer; any other number is a float64.
 */
class CanonicalPackagerVectorsTest {
    private static final HexFormat HEX = HexFormat.of();

    private static JsonObject vectors() throws IOException {
        try (var in = CanonicalPackagerVectorsTest.class.getResourceAsStream("/canonical-package-vectors.json")) {
            assertNotNull(in, "canonical-package-vectors.json is missing");
            var doc = JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertEquals("mercury-canonical-package-vectors", doc.get("format").getAsString());
            return doc;
        }
    }

    /** The Java value a vector's JSON denotes. */
    private static Object value(JsonElement e) {
        if (e.isJsonNull()) {
            return null;
        }
        if (e.isJsonArray()) {
            var list = new ArrayList<>();
            e.getAsJsonArray().forEach(item -> list.add(value(item)));
            return list;
        }
        if (e.isJsonObject()) {
            var o = e.getAsJsonObject();
            if (o.size() == 1) {
                var tag = o.keySet().iterator().next();
                var arg = o.get(tag);
                switch (tag) {
                    case "$bytes" -> {
                        return HEX.parseHex(arg.getAsString());
                    }
                    case "$decimal" -> {
                        return new BigDecimal(arg.getAsString());
                    }
                    case "$integer" -> {
                        return new BigInteger(arg.getAsString());
                    }
                    case "$float32" -> {
                        return arg.getAsFloat();
                    }
                    case "$double" -> {
                        return Double.parseDouble(arg.getAsString());
                    }
                    case "$unsupported" -> {
                        return new Object();
                    }
                    default -> {
                        // an ordinary one-key map
                    }
                }
            }
            var map = new LinkedHashMap<String, Object>();
            o.entrySet().forEach(kv -> map.put(kv.getKey(), value(kv.getValue())));
            return map;
        }
        var p = e.getAsJsonPrimitive();
        if (p.isBoolean()) {
            return p.getAsBoolean();
        }
        if (p.isString()) {
            return p.getAsString();
        }
        var text = p.getAsString();
        if (text.contains(".") || text.contains("e") || text.contains("E")) {
            return Double.parseDouble(text);
        }
        return Long.parseLong(text);
    }

    private static CanonicalPackager.Builder builder(JsonObject v) {
        var builder = CanonicalPackager.builder();
        if (v.has("manifest")) {
            v.getAsJsonObject("manifest").entrySet()
                    .forEach(kv -> builder.manifest(kv.getKey(), kv.getValue().getAsString()));
        }
        for (var pair : v.getAsJsonArray("maps")) {
            var entry = pair.getAsJsonArray();
            builder.add(entry.get(0).getAsString(), (Map<?, ?>) value(entry.get(1)));
        }
        return builder;
    }

    /** The package a vector describes: the builder calls and the build, both of which can reject a value. */
    private static byte[] pack(JsonObject v) throws IOException {
        return builder(v).build();
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private void everyPackageHolds(JsonArray packages) throws Exception {
        var ids = new HashSet<String>();
        for (var item : packages) {
            var v = item.getAsJsonObject();
            var id = v.get("id").getAsString();
            assertTrue(ids.add(id), "duplicate vector id " + id);
            var bytes = builder(v).build();
            assertEquals(v.get("hex").getAsString(), HEX.formatHex(bytes), id);
            assertEquals(v.get("sha256").getAsString(), sha256(bytes), id + " sha256");
            // an accepted package has exactly one byte form: the strict read re-encodes and compares
            var unpacked = CanonicalPackager.unpack(bytes);
            assertEquals("mercury-package", unpacked.manifest().get("format"), id);
            assertArrayEquals(bytes, CanonicalPackager.encode(Map.of(
                    "manifest", unpacked.manifest(), "maps", unpacked.maps())), id + " re-encodes to itself");
        }
    }

    @Test
    void everyValueEncodesToTheExpectedBytes() throws IOException {
        var all = vectors().getAsJsonArray("values");
        assertTrue(all.size() > 60, "the vector file looks truncated: " + all.size());
        var ids = new HashSet<String>();
        for (var item : all) {
            var v = item.getAsJsonObject();
            var id = v.get("id").getAsString();
            assertTrue(ids.add(id), "duplicate vector id " + id);
            assertEquals(v.get("hex").getAsString(), HEX.formatHex(CanonicalPackager.encode(value(v.get("value")))),
                    id);
        }
    }

    @Test
    void everyPackageHoldsInTheJavaPackager() throws Exception {
        var packages = vectors().getAsJsonArray("packages");
        assertTrue(packages.size() >= 6);
        everyPackageHolds(packages);
    }

    @Test
    void theGeneratedCorpusMatchesByteForByte() throws Exception {
        var corpus = vectors().getAsJsonArray("corpus");
        assertEquals(60, corpus.size(), "the corpus looks truncated");
        everyPackageHolds(corpus);
    }

    @Test
    void everyPackRejectionFailsByName() throws Exception {
        var fragments = Map.of(
                "float32", "Float is not canonical",
                "non-finite", "non-finite number",
                "unsupported-type", "Unsupported type",
                "duplicate-entry", "Duplicate entry name",
                "reserved-field", "written by the packager");
        int checked = 0;
        for (var item : vectors().getAsJsonArray("rejections")) {
            var v = item.getAsJsonObject();
            if (!"pack".equals(v.get("kind").getAsString())) {
                continue;
            }
            var id = v.get("id").getAsString();
            var fragment = fragments.get(v.get("error").getAsString());
            assertNotNull(fragment, "unknown error code in " + id);
            if (!v.has("maps")) {
                v.add("maps", new JsonArray());
            }
            var e = assertThrows(IllegalArgumentException.class, () -> pack(v), id);
            assertTrue(e.getMessage().contains(fragment), id + ": '" + e.getMessage() + "'");
            checked++;
        }
        assertTrue(checked >= 7, "pack rejections checked: " + checked);
    }

    @Test
    void everyUnpackRejectionFailsByName() throws Exception {
        var fragments = Map.of(
                "not-canonical", List.of("Not canonical"),
                "trailing-bytes", List.of("Unexpected bytes after the value"),
                "not-a-package", List.of("A package is one map", "is not a map", "is not text"),
                "unsupported-format", List.of("Not a package: the manifest format"),
                "unsupported-version", List.of("Unsupported package format_version"),
                "malformed", List.of("Malformed MsgPack", "Duplicate key", "A key is not text",
                        "Unsupported MsgPack type"));
        int checked = 0;
        for (var item : vectors().getAsJsonArray("rejections")) {
            var v = item.getAsJsonObject();
            if (!"unpack".equals(v.get("kind").getAsString())) {
                continue;
            }
            var id = v.get("id").getAsString();
            var bytes = HEX.parseHex(v.get("hex").getAsString());
            var accepted = fragments.get(v.get("error").getAsString());
            assertNotNull(accepted, "unknown error code in " + id);
            var e = assertThrows(IOException.class, () -> CanonicalPackager.unpack(bytes), id);
            assertTrue(accepted.stream().anyMatch(f -> e.getMessage().contains(f)),
                    id + ": '" + e.getMessage() + "' does not match " + v.get("error"));
            if (v.get("strict_only").getAsBoolean()) {
                // the bytes decode: only the strict read rejects a form that is not canonical
                assertNotNull(CanonicalPackager.unpack(bytes, false), id + " decodes when not strict");
            }
            checked++;
        }
        assertTrue(checked >= 15, "unpack rejections checked: " + checked);
    }
}
