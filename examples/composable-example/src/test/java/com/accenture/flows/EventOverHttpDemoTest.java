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

package com.accenture.flows;

import com.accenture.support.TestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * End-to-end coverage of the two Event-over-HTTP demo endpoints. The test
 * application.properties points peer.demo.host/port back at this test server,
 * so both patterns make a REAL HTTP hop through /api/event and land on the
 * public echo functions registered below - the same wiring as the live demo
 * against the lambda-example, but self-contained in one JVM.
 */
class EventOverHttpDemoTest extends TestBase {
    private static final String HTTP_CLIENT = "async.http.request";

    @BeforeAll
    static void registerEcho() {
        // stand-in for the lambda-example's public echo (hello.world + its alias)
        LambdaFunction echo = (headers, input, instance) -> {
            Map<String, Object> result = new HashMap<>();
            result.put("body", input);
            result.put("headers", headers);
            result.put("instance", instance);
            result.put("origin", Platform.getInstance().getOrigin());
            return result;
        };
        Platform platform = Platform.getInstance();
        platform.register("hello.world", echo, 5);
        platform.register("hello.declarative", echo, 5);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeDemoEndpoint(String uri) throws ExecutionException, InterruptedException {
        PostOffice po = PostOffice.trackable("unit.test", "2000", "TEST /event/over/http");
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(host).setMethod("POST")
                .setHeader("content-type", "application/json")
                .setHeader("accept", "application/json")
                .setBody(Map.of("hello", "world")).setUrl(uri);
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, 12000).get();
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        return (Map<String, Object>) result.getBody();
    }

    @Test
    void programmaticEventOverHttpDemo() throws ExecutionException, InterruptedException {
        var response = new MultiLevelMap(invokeDemoEndpoint("/api/event/http/programmatic"));
        assertEquals("world", response.getElement("body.hello"));
        assertEquals(Platform.getInstance().getOrigin(), response.getElement("origin"));
    }

    @Test
    void declarativeEventOverHttpDemo() throws ExecutionException, InterruptedException {
        var response = new MultiLevelMap(invokeDemoEndpoint("/api/event/http/demo"));
        assertEquals("world", response.getElement("body.hello"));
        assertEquals(Platform.getInstance().getOrigin(), response.getElement("origin"));
    }
}
