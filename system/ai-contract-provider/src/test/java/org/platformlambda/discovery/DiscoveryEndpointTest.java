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

package org.platformlambda.discovery;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.discovery.support.TestBase;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end over the real HTTP surface: rest.yaml -> http.flow.adapter -> Event Script
 * flow -> function, exactly as an AI agent would consume the discovery endpoints.
 */
class DiscoveryEndpointTest extends TestBase {
    private static final String HTTP_CLIENT = "async.http.request";
    private static final long TIMEOUT = 10000;

    @SuppressWarnings("unchecked")
    @Test
    void discoveryIndexNamesVersionContractsAndEndpoints() throws ExecutionException, InterruptedException {
        EventEnvelope result = get("/api/discovery");
        assertEquals(200, result.getStatus());
        var map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("ai-contract-provider", map.getElement("name"));
        assertEquals(System.getProperty("mercury.version.under.test"),
                map.getElement("mercury_version"));
        assertEquals(List.of("event-script", "minigraph", "platform-core", "rest-automation"),
                map.getElement("contracts"));
        assertEquals("GET /api/references?path={reference-path}", map.getElement("endpoints.reference"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void contractListAndDetailServeTheCatalog() throws ExecutionException, InterruptedException {
        EventEnvelope listed = get("/api/contracts");
        assertEquals(200, listed.getStatus());
        var list = new MultiLevelMap((Map<String, Object>) listed.getBody());
        assertEquals(4, list.getElement("total"));
        assertEquals("event-script", list.getElement("contracts[0].id"));

        EventEnvelope detail = get("/api/contracts/minigraph");
        assertEquals(200, detail.getStatus());
        var contract = new MultiLevelMap((Map<String, Object>) detail.getBody());
        assertEquals("minigraph-playground-engine", contract.getElement("module"));
        assertTrue(String.valueOf(contract.getElement("behavior_anchors")).contains(
                "com.accenture.minigraph.services.GraphCommandService"));

        EventEnvelope unknown = get("/api/contracts/no-such-contract");
        assertEquals(404, unknown.getStatus());
        var error = new MultiLevelMap((Map<String, Object>) unknown.getBody());
        assertEquals("Contract no-such-contract is not installed", error.getElement("message"));
    }

    @Test
    void skillEntrypointServesMarkdown() throws ExecutionException, InterruptedException {
        EventEnvelope result = get("/api/skill");
        assertEquals(200, result.getStatus());
        assertInstanceOf(String.class, result.getBody());
        assertTrue(String.valueOf(result.getBody()).contains("name: mercury-platform"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void referencesServeExactInventoryMembersOnly() throws ExecutionException, InterruptedException {
        EventEnvelope grammar = get("/api/references?path="
                + "references/guides/event-script/flow-grammar.md");
        assertEquals(200, grammar.getStatus());
        assertTrue(String.valueOf(grammar.getBody()).contains("Event Script"));

        EventEnvelope traversal = get("/api/references?path=references/../../pom.xml");
        assertEquals(404, traversal.getStatus());
        var error = new MultiLevelMap((Map<String, Object>) traversal.getBody());
        assertTrue(String.valueOf(error.getElement("message")).contains("not in this snapshot"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void manifestReportsEveryFileWithHashes() throws ExecutionException, InterruptedException {
        EventEnvelope result = get("/api/manifest");
        assertEquals(200, result.getStatus());
        var manifest = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("mercury-platform-skill", manifest.getElement("type"));
        assertEquals(System.getProperty("mercury.version.under.test"),
                manifest.getElement("mercury_version"));
        var files = (List<Object>) manifest.getElement("files");
        assertTrue(files.size() > 50, "the guide closure has more than 50 files");
    }

    private EventEnvelope get(String url) throws ExecutionException, InterruptedException {
        PostOffice po = PostOffice.trackable("unit.test", "2000", "TEST /discovery");
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(host).setMethod("GET")
                .setHeader("accept", "application/json").setUrl(url);
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        return po.request(req, TIMEOUT).get();
    }
}
