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

package com.accenture.minigraph.contract;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.serializers.CanonicalPackager;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The graph contract (RFC-0007) pinned by the shared vector file {@code graph-contract-vectors.json}: for
 * each case the contract view, the OpenAPI 3.0 document and the 'describe' lines the engine must derive
 * from the model alone. The same file, byte-identical, drives the Rust engine's test; a value compared
 * through the canonical packager is order- and integer-width-insensitive, so the two engines' maps are
 * compared as values.
 */
class GraphContractTest {

    @Test
    @SuppressWarnings("unchecked")
    void sharedVectorsPinTheContractAndTheDocument() throws Exception {
        Map<String, Object> vectors;
        try (var in = GraphContractTest.class.getResourceAsStream("/graph-contract-vectors.json")) {
            assertNotNull(in, "the vector file");
            vectors = SimpleMapper.getInstance().getMapper().readValue(new String(in.readAllBytes(), UTF_8), Map.class);
        }
        var cases = (List<Map<String, Object>>) vectors.get("cases");
        assertFalse(cases.isEmpty());
        for (var c : cases) {
            var name = String.valueOf(c.get("name"));
            var graph = (Map<String, Object>) c.get("graph");
            var others = (Map<String, Object>) c.getOrDefault("others", Map.of());
            var contract = GraphContract.derive(name, graph, id -> (Map<String, Object>) others.get(id));
            assertArrayEquals(CanonicalPackager.encode(c.get("contract")), CanonicalPackager.encode(contract.toMap()),
                    "contract view of " + name);
            var document = OpenApiDocument.of(contract, "1.0.0", null);
            assertArrayEquals(CanonicalPackager.encode(c.get("openapi")), CanonicalPackager.encode(document),
                    "OpenAPI document of " + name);
            assertEquals(c.get("describe"), contract.describe(), "describe lines of " + name);
            // the YAML rendering reads back as the same document
            var yaml = OpenApiDocument.toYaml(document);
            assertTrue(yaml.startsWith("openapi: 3.0.3\n"), "YAML of " + name + ":\n" + yaml);
            Object parsed = new Yaml().load(yaml);
            assertArrayEquals(CanonicalPackager.encode(document), CanonicalPackager.encode(parsed), "YAML round trip of " + name);
        }
    }

    @Test
    void theDocumentNamesItsServerAndVersion() {
        var model = Map.<String, Object>of("nodes", List.of(
                Map.of("alias", "root", "types", List.of("Root"), "properties", Map.of("purpose", "Hello")),
                Map.of("alias", "end", "types", List.of("End"), "properties",
                        Map.of("mapping", List.of("input.body.name -> output.body.greeting")))),
                "connections", List.of(Map.of("from", "root", "to", "end", "label", "contains")));
        var contract = GraphContract.derive("hello", model, null);
        var document = OpenApiDocument.of(contract, "2.3.4", "http://127.0.0.1:8085");
        assertEquals("3.0.3", document.get("openapi"));
        assertEquals(List.of(Map.of("url", "http://127.0.0.1:8085")), document.get("servers"));
        @SuppressWarnings("unchecked")
        var info = (Map<String, Object>) document.get("info");
        assertEquals("2.3.4", info.get("version"));
        assertEquals("hello", info.get("title"));
        assertEquals("Hello", info.get("description"));
        var json = OpenApiDocument.toJson(document);
        assertTrue(json.replaceAll("\\s", "").startsWith("{\"openapi\":\"3.0.3\""), json);
        assertTrue(contract.describe().contains("  input.body.name\n"));
        assertTrue(contract.describe().contains("Declared schema: none\n"));
    }
}
