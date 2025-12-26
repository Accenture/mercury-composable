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

import io.vertx.core.Future;
import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class EventHttpTest extends TestBase {

    @Test
    void configTest() {
        String route = "event.http.test";
        AppConfigReader config = AppConfigReader.getInstance();
        String serverPort = config.getProperty("server.port");
        String shouldBeTarget = "http://127.0.0.1:" + serverPort+ "/api/event";
        EventEmitter po = EventEmitter.getInstance();
        String target = po.getEventHttpTarget(route);
        assertEquals(shouldBeTarget, target);
        Map<String, String> headers = po.getEventHttpHeaders(route);
        assertEquals("demo", headers.get("authorization"));
    }

    @Test
    void declarativeEventOverHttpTest() throws ExecutionException, InterruptedException {
        /*
         * This test illustrates automatic forwarding of events to a peer using "event over HTTP" configuration.
         * The rest of the tests in this class use programmatic "Event over HTTP" API.
         */
        Platform platform = Platform.getInstance();
        final String blockingEventWait = "blocking.event.wait";
        final BlockingQueue<Object> wait1 = new ArrayBlockingQueue<>(1);
        final BlockingQueue<Object> wait2 = new ArrayBlockingQueue<>(1);
        LambdaFunction f = (headers, body, instance) -> {
            wait1.add(body);
            platform.release(blockingEventWait);
            return null;
        };
        platform.registerPrivate(blockingEventWait, f, 1);
        String route = "event.save.get";
        String hello = "hello";
        EventEnvelope save = new EventEnvelope().setTo(route).setHeader("type", "save").setBody(hello)
                .setReplyTo(blockingEventWait);
        PostOffice po = new PostOffice("unit.test", "1200001", "EVENT /save/then/get");
        po.send(save);
        Object serviceResponse = wait1.poll(5, TimeUnit.SECONDS);
        assertEquals("saved", serviceResponse);
        EventEnvelope get = new EventEnvelope().setTo(route).setHeader("type", "get");
        EventEnvelope response = po.eRequest(get, 10000).get();
        assertEquals(hello, response.getBody());
        Future<EventEnvelope> asyncResponse = po.asyncRequest(get, 10000);
        asyncResponse.onSuccess(evt -> wait2.add(evt.getBody()));
        Object result = wait2.poll(5, TimeUnit.SECONDS);
        assertEquals(hello, result);
    }

    @Test
    void remoteEventApiAuthTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long timeout = 3000;
        int numberThree = 3;
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "anyone");
        PostOffice po = new PostOffice("unit.test", "123", "TEST /remote/event");
        EventEnvelope event = new EventEnvelope().setTo("hello.world")
                .setBody(numberThree).setHeader("hello", "world");
        Future<EventEnvelope> response = po.asyncRequest(event, timeout, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        response.onSuccess(bench::add);
        EventEnvelope result = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assertNotNull(result);
        assertEquals(401, result.getStatus());
        assertInstanceOf(String.class, result.getBody());
        assertEquals("Unauthorized", result.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    void remoteEventApiWithLargePayloadTest() throws InterruptedException {
        // create a large payload of 100 KB
        String payload = "123456789.".repeat(10000);
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long timeout = 3000;
        PostOffice po = new PostOffice("unit.test", "1230", "TEST /remote/event/large");
        EventEnvelope event = new EventEnvelope();
        event.setTo("hello.world").setBody(payload).setHeader("hello", "world");
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        Future<EventEnvelope> response = po.asyncRequest(event, timeout, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        response.onSuccess(bench::add);
        // add 500 ms to the bench to capture HTTP-408 response if any
        EventEnvelope result = bench.poll(timeout + 500, TimeUnit.MILLISECONDS);
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("world", map.getElement("headers.hello"));
        assertEquals(payload, map.getElement("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void remoteEventApiOneWayTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long timeout = 3000;
        int numberThree = 3;
        PostOffice po = new PostOffice("unit.test", "12002", "TEST /remote/event/oneway");
        EventEnvelope event = new EventEnvelope();
        event.setTo("hello.world").setBody(numberThree).setHeader("hello", "world");
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        Future<EventEnvelope> response = po.asyncRequest(event, timeout, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", false);
        response.onSuccess(bench::add);
        EventEnvelope result = bench.poll(5, TimeUnit.SECONDS);
        assertNotNull(result);
        // status code 202 indicates that a drop-n-forget event has been sent asynchronously
        assertEquals(202, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> map = (Map<String, Object>) result.getBody();
        assertTrue(map.containsKey("time"));
        assertEquals("async", map.get("type"));
        assertEquals(true, map.get("delivered"));
    }

    @Test
    void remoteEventApiMissingRouteTest() {
        String traceId = "123";
        long timeout = 3000;
        int numberThree = 3;
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        PostOffice po = new PostOffice("unit.test", traceId, "TEST /remote/event");
        EventEnvelope event = new EventEnvelope().setBody(numberThree).setHeader("hello", "world");
        var ex = assertThrows(IllegalArgumentException.class, () ->
                po.asyncRequest(event, timeout, securityHeaders,
                        "http://127.0.0.1:"+port+"/api/event", true));
        assertEquals("Missing routing path", ex.getMessage());
    }

    @Test
    void remoteEventApiNullEventTest() {
        String traceId = "123";
        long timeout = 3000;
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        PostOffice po = new PostOffice("unit.test", traceId, "TEST /remote/event");
        var ex = assertThrows(IllegalArgumentException.class, () ->
                po.asyncRequest(null, timeout, securityHeaders,
                        "http://127.0.0.1:"+port+"/api/event", true));
        assertEquals("Missing outgoing event", ex.getMessage());
    }

    @Test
    void remoteEventApiRouteNotFoundTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String traceId = "123";
        long timeout = 3000;
        int numberThree = 3;
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        PostOffice po = new PostOffice("unit.test", traceId, "TEST /remote/event");
        EventEnvelope event = new EventEnvelope().setTo("some.dummy.route")
                .setBody(numberThree).setHeader("hello", "world");
        Future<EventEnvelope> response = po.asyncRequest(event, timeout, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        response.onSuccess(bench::add);
        EventEnvelope result = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert result != null;
        assertEquals(404, result.getStatus());
        assertEquals("Route some.dummy.route not found", result.getError());
    }

    @Test
    void remoteEventApiAccessControlTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long timeout = 3000;
        String demoFunction = "demo.private.function";
        LambdaFunction f = (headers, input, instance) -> true;
        Platform platform = Platform.getInstance();
        platform.registerPrivate(demoFunction, f, 1);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope event = new EventEnvelope();
        event.setTo(demoFunction).setBody("ok").setHeader("hello", "world");
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        Future<EventEnvelope> response = po.asyncRequest(event, timeout, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        response.onSuccess(bench::add);
        EventEnvelope result = bench.poll(5, TimeUnit.SECONDS);
        assert result != null;
        assertEquals(403, result.getStatus());
        assertEquals(demoFunction+" is private", result.getError());
    }

    @Test
    void remoteEventApiAccessControlFutureTest() throws InterruptedException, ExecutionException {
        long timeout = 3000;
        String demoFunction = "demo.private.function";
        LambdaFunction f = (headers, input, instance) -> true;
        Platform platform = Platform.getInstance();
        platform.registerPrivate(demoFunction, f, 1);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope event = new EventEnvelope();
        event.setTo(demoFunction).setBody("ok").setHeader("hello", "world");
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        CompletableFuture<EventEnvelope> future = po.eRequest(event, timeout, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        EventEnvelope result = future.get();
        assert result != null;
        assertEquals(403, result.getStatus());
        assertEquals(demoFunction+" is private", result.getError());
    }

    @SuppressWarnings("unchecked")
    @Test
    void remoteEventApiFutureTest() throws InterruptedException, ExecutionException {
        long timeout = 3000;
        int numberThree = 3;
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        PostOffice po = new PostOffice("unit.test", "123", "TEST /remote/event");
        EventEnvelope event = new EventEnvelope().setTo("hello.world")
                .setBody(numberThree).setHeader("hello", "world");
        // prove that po.request and po.eRequest return the same result from different worker instances
        EventEnvelope result1 = po.request(event, timeout, securityHeaders,
                                "http://127.0.0.1:"+port+"/api/event", true).get();
        EventEnvelope result2 = po.eRequest(event, timeout, securityHeaders,
                                "http://127.0.0.1:"+port+"/api/event", true).get();
        assertNotNull(result1);
        assertNotEquals(result1.getBody(), result2.getBody());
        assertEquals(200, result1.getStatus());
        assertInstanceOf(Map.class, result1.getBody());
        assertInstanceOf(Map.class, result2.getBody());
        MultiLevelMap map1 = new MultiLevelMap((Map<String, Object>) result1.getBody());
        MultiLevelMap map2 = new MultiLevelMap((Map<String, Object>) result2.getBody());
        assertEquals("world", map1.getElement("headers.hello"));
        assertEquals("world", map2.getElement("headers.hello"));
        // validate that session information is passed by the demo authentication service "event.api.auth"
        assertEquals("demo", map1.getElement("headers.user"));
        assertEquals("demo", map2.getElement("headers.user"));
        assertEquals(numberThree, map1.getElement("body"));
        assertEquals(numberThree, map2.getElement("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void remoteEventApiTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long timeout = 3000;
        int numberThree = 3;
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        PostOffice po = new PostOffice("unit.test", "123", "TEST /remote/event");
        EventEnvelope event = new EventEnvelope().setTo("hello.world")
                .setBody(numberThree).setHeader("hello", "world");
        Future<EventEnvelope> response = po.asyncRequest(event, timeout, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        response.onSuccess(bench::add);
        EventEnvelope result = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("world", map.getElement("headers.hello"));
        // validate that session information is passed by the demo authentication service "event.api.auth"
        assertEquals("demo", map.getElement("headers.user"));
        assertEquals(numberThree, map.getElement("body"));
    }
}
