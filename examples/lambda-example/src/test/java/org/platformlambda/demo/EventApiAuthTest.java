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

package org.platformlambda.demo;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.demo.common.TestBase;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * End-to-end coverage of the Event-over-HTTP authentication demo: rest.yaml
 * overrides the default "/api/event" endpoint with the "event.api.auth"
 * service, which validates the caller's "authorization" header against the
 * shared demo token (demo.peer.token, resolved from DEMO_PEER_TOKEN).
 */
class EventApiAuthTest extends TestBase {

    private String eventEndpoint() {
        String port = AppConfigReader.getInstance().getProperty("rest.server.port", "8885");
        return "http://127.0.0.1:" + port + "/api/event";
    }

    @SuppressWarnings("unchecked")
    @Test
    void acceptsTheSharedDemoToken() throws InterruptedException, ExecutionException {
        PostOffice po = new PostOffice("unit.test", "310", "TEST /api/event/auth");
        Map<String, String> securityHeaders = Map.of("authorization", "demo");
        EventEnvelope req = new EventEnvelope().setTo("hello.world").setBody(Map.of("hello", "auth"));
        EventEnvelope response = po.request(req, 8000, securityHeaders, eventEndpoint(), true).get();
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("auth", map.getElement("body.hello"));
        // session info injected by event.api.auth rides to the target function as a header
        assertEquals("demo", map.getElement("headers.user"));
    }

    @Test
    void rejectsAWrongToken() throws InterruptedException, ExecutionException {
        PostOffice po = new PostOffice("unit.test", "311", "TEST /api/event/auth");
        Map<String, String> securityHeaders = Map.of("authorization", "let-me-in");
        EventEnvelope req = new EventEnvelope().setTo("hello.world").setBody("x");
        EventEnvelope response = po.request(req, 8000, securityHeaders, eventEndpoint(), true).get();
        assertEquals(401, response.getStatus());
        assertEquals("Unauthorized", response.getBody());
    }

    @Test
    void rejectsAMissingToken() throws InterruptedException, ExecutionException {
        PostOffice po = new PostOffice("unit.test", "312", "TEST /api/event/auth");
        EventEnvelope req = new EventEnvelope().setTo("hello.world").setBody("x");
        EventEnvelope response = po.request(req, 8000, Map.of(), eventEndpoint(), true).get();
        assertEquals(401, response.getStatus());
    }
}
