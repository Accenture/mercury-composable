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

package com.accenture.cache.demo;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * End-to-end CRUD against the running app on an embedded Redis: each layer's route family
 * (Platform Core / Event Script) stores, reads, and evicts a profile, a miss returns HTTP 404
 * "Profile not found", and a profile written through one layer is readable through another - the
 * cross-layer interoperability the shared cache + EventEnvelope wire format provides.
 */
class ProfileCacheTest extends TestBase {
    private static final String HTTP_CLIENT = "async.http.request";
    private static final long TIMEOUT = 8000;

    private EventEnvelope http(String method, String url, Object body) throws Exception {
        PostOffice po = PostOffice.trackable("unit.test", Utility.getInstance().getUuid(), "TEST " + method + " " + url);
        AsyncHttpRequest request = new AsyncHttpRequest()
                .setTargetHost(host).setMethod(method).setHeader("accept", "application/json").setUrl(url);
        if (body != null) {
            request.setHeader("content-type", "application/json").setBody(body);
        }
        return po.request(new EventEnvelope().setTo(HTTP_CLIENT).setBody(request), TIMEOUT).get();
    }

    private static Map<String, Object> profile(String name, String email) {
        Map<String, Object> p = new HashMap<>();
        p.put("name", name);
        p.put("email", email);
        return p;
    }

    /** Full GET/POST/DELETE cycle for one layer's route family, including the 404-on-miss contract. */
    @SuppressWarnings("unchecked")
    private void crudCycle(String base, int layer) throws Exception {
        String url = base + "/carol";
        // GET a non-existent profile -> HTTP 404 "Profile not found"
        EventEnvelope miss = http("GET", url, null);
        assertEquals(404, miss.getStatus());
        assertInstanceOf(Map.class, miss.getBody());
        assertEquals("Profile not found", ((Map<String, Object>) miss.getBody()).get("message"));
        // POST -> HTTP 201
        EventEnvelope created = http("POST", url, profile("Carol", "carol@example.com"));
        assertEquals(201, created.getStatus());
        assertEquals(layer, ((Map<String, Object>) created.getBody()).get("layer"));
        // GET -> HTTP 200 with the stored profile
        EventEnvelope found = http("GET", url, null);
        assertEquals(200, found.getStatus());
        Map<String, Object> body = (Map<String, Object>) found.getBody();
        assertEquals("Carol", body.get("name"));
        assertEquals("carol@example.com", body.get("email"));
        // DELETE -> HTTP 200, then GET is a miss again
        assertEquals(200, http("DELETE", url, null).getStatus());
        assertEquals(404, http("GET", url, null).getStatus());
    }

    @Test
    void layer1Crud() throws Exception {
        crudCycle("/api/l1/profile", 1);
    }

    @Test
    void layer2Crud() throws Exception {
        crudCycle("/api/l2/profile", 2);
    }

    /** Layer 3 (Knowledge Graph): the action + id (+ profile for save) ride in the POST payload. */
    @SuppressWarnings("unchecked")
    @Test
    void layer3GraphCrud() throws Exception {
        Map<String, Object> get = new HashMap<>();
        get.put("action", "get");
        get.put("id", "erin");
        // SAVE
        Map<String, Object> save = new HashMap<>();
        save.put("action", "save");
        save.put("id", "erin");
        save.put("profile", profile("Erin", "erin@example.com"));
        EventEnvelope saved = http("POST", "/api/graph/profile-cache", save);
        assertEquals(200, saved.getStatus());
        assertEquals(3, ((Map<String, Object>) saved.getBody()).get("layer"));
        // GET -> the stored profile
        EventEnvelope found = http("POST", "/api/graph/profile-cache", get);
        assertEquals(200, found.getStatus());
        Map<String, Object> body = (Map<String, Object>) found.getBody();
        assertEquals("Erin", body.get("name"));
        assertEquals("erin@example.com", body.get("email"));
        // DELETE, then GET is a miss again
        Map<String, Object> del = new HashMap<>();
        del.put("action", "delete");
        del.put("id", "erin");
        assertEquals(200, http("POST", "/api/graph/profile-cache", del).getStatus());
        assertEquals(404, http("POST", "/api/graph/profile-cache", get).getStatus());
    }

    /**
     * The graph's dispatch table is closed: an unknown or missing action is rejected by the reject node
     * with HTTP-400 rather than falling through to whichever branch happens to be last.
     */
    @SuppressWarnings("unchecked")
    @Test
    void layer3RejectsAnUnknownAction() throws Exception {
        Map<String, Object> save = new HashMap<>();
        save.put("action", "save");
        save.put("id", "frank");
        save.put("profile", profile("Frank", "frank@example.com"));
        assertEquals(200, http("POST", "/api/graph/profile-cache", save).getStatus());
        // an unrecognised action is rejected, and the profile must survive it
        Map<String, Object> bogus = new HashMap<>();
        bogus.put("action", "purge");
        bogus.put("id", "frank");
        EventEnvelope rejected = http("POST", "/api/graph/profile-cache", bogus);
        assertEquals(400, rejected.getStatus());
        assertEquals("Invalid action. Use get, save or delete",
                ((Map<String, Object>) rejected.getBody()).get("message"));
        // a missing action is rejected the same way
        Map<String, Object> noAction = new HashMap<>();
        noAction.put("id", "frank");
        assertEquals(400, http("POST", "/api/graph/profile-cache", noAction).getStatus());
        // the record is still there - neither rejection reached the delete branch
        Map<String, Object> get = new HashMap<>();
        get.put("action", "get");
        get.put("id", "frank");
        EventEnvelope found = http("POST", "/api/graph/profile-cache", get);
        assertEquals(200, found.getStatus());
        assertEquals("Frank", ((Map<String, Object>) found.getBody()).get("name"));
        Map<String, Object> del = new HashMap<>();
        del.put("action", "delete");
        del.put("id", "frank");
        assertEquals(200, http("POST", "/api/graph/profile-cache", del).getStatus());
    }

    /** A profile written through Layer 1 is readable through Layer 2 - one cache, one wire format. */
    @SuppressWarnings("unchecked")
    @Test
    void crossLayerInterop() throws Exception {
        EventEnvelope created = http("POST", "/api/l1/profile/shared", profile("Dave", "dave@example.com"));
        assertEquals(201, created.getStatus());
        // read it back through a different layer
        EventEnvelope viaL2 = http("GET", "/api/l2/profile/shared", null);
        assertEquals(200, viaL2.getStatus());
        Map<String, Object> body = (Map<String, Object>) viaL2.getBody();
        assertEquals("Dave", body.get("name"));
        assertEquals("dave@example.com", body.get("email"));
        http("DELETE", "/api/l1/profile/shared", null);
    }
}
