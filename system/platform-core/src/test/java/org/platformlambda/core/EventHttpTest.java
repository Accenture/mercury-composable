/*

    Copyright 2018-2025 Accenture Technology

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class EventHttpTest extends TestBase {

    @Test
    void configTest() {
        String ROUTE = "event.http.test";
        AppConfigReader config = AppConfigReader.getInstance();
        String serverPort = config.getProperty("server.port");
        String SHOULD_BE_TARGET = "http://127.0.0.1:" + serverPort+ "/api/event";
        EventEmitter po = EventEmitter.getInstance();
        String target = po.getEventHttpTarget(ROUTE);
        assertEquals(SHOULD_BE_TARGET, target);
        Map<String, String> headers = po.getEventHttpHeaders(ROUTE);
        assertEquals("demo", headers.get("authorization"));
    }

    @Test
    void declarativeEventOverHttpTest() throws IOException, ExecutionException, InterruptedException {
        /*
         * This test illustrates automatic forwarding of events to a peer using "event over HTTP" configuration.
         * The rest of the tests in this class use programmatic "Event over HTTP" API.
         */
        Platform platform = Platform.getInstance();
        final String BLOCKING_EVENT_WAIT = "blocking.event.wait";
        final BlockingQueue<Object> wait1 = new ArrayBlockingQueue<>(1);
        final BlockingQueue<Object> wait2 = new ArrayBlockingQueue<>(1);
        LambdaFunction f = (headers, body, instance) -> {
            wait1.add(body);
            platform.release(BLOCKING_EVENT_WAIT);
            return null;
        };
        platform.registerPrivate(BLOCKING_EVENT_WAIT, f, 1);
        String ROUTE = "event.save.get";
        String HELLO = "hello";
        EventEnvelope save = new EventEnvelope().setTo(ROUTE).setHeader("type", "save").setBody(HELLO)
                .setReplyTo(BLOCKING_EVENT_WAIT);
        PostOffice po = new PostOffice("unit.test", "1200001", "EVENT /save/then/get");
        po.send(save);
        Object serviceResponse = wait1.poll(5, TimeUnit.SECONDS);
        assertEquals("saved", serviceResponse);
        EventEnvelope get = new EventEnvelope().setTo(ROUTE).setHeader("type", "get");
        EventEnvelope response = po.request(get, 10000).get();
        assertEquals(HELLO, response.getBody());
        Future<EventEnvelope> asyncResponse = po.asyncRequest(get, 10000);
        asyncResponse.onSuccess(evt -> wait2.add(evt.getBody()));
        Object result = wait2.poll(5, TimeUnit.SECONDS);
        assertEquals(HELLO, result);
        // test API forwarding
        EventEnvelope forward = new EventEnvelope().setTo("event.api.forwarder")
                                .setBody(get.toBytes()).setHeader("timeout", 10000);
        EventEnvelope kotlinResponse = po.request(forward, 10000).get();
        assertEquals(HELLO, kotlinResponse.getBody());
    }

    @Test
    void remoteEventApiAuthTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long TIMEOUT = 3000;
        int NUMBER_THREE = 3;
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "anyone");
        PostOffice po = new PostOffice("unit.test", "123", "TEST /remote/event");
        EventEnvelope event = new EventEnvelope().setTo("hello.world")
                .setBody(NUMBER_THREE).setHeader("hello", "world");
        Future<EventEnvelope> response = po.asyncRequest(event, TIMEOUT, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        response.onSuccess(bench::add);
        EventEnvelope result = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertNotNull(result);
        assertEquals(401, result.getStatus());
        assertInstanceOf(String.class, result.getBody());
        assertEquals("Unauthorized", result.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    void remoteEventApiWithLargePayloadTest() throws IOException, InterruptedException {
        // create a large payload of 100 KB
        String PAYLOAD = "123456789.".repeat(10000);
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long TIMEOUT = 3000;
        PostOffice po = new PostOffice("unit.test", "1230", "TEST /remote/event/large");
        EventEnvelope event = new EventEnvelope();
        event.setTo("hello.world").setBody(PAYLOAD).setHeader("hello", "world");
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        Future<EventEnvelope> response = po.asyncRequest(event, TIMEOUT, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        response.onSuccess(bench::add);
        // add 500 ms to the bench to capture HTTP-408 response if any
        EventEnvelope result = bench.poll(TIMEOUT + 500, TimeUnit.MILLISECONDS);
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("world", map.getElement("headers.hello"));
        assertEquals(PAYLOAD, map.getElement("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void remoteEventApiWithLargePayloadKotlinTest() throws IOException, InterruptedException {
        // create a large payload of 100 KB
        String PAYLOAD = "123456789.".repeat(10000);
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long TIMEOUT = 3000;
        PostOffice po = new PostOffice("unit.test", "1231", "TEST /remote/event/large/k");
        EventEnvelope event = new EventEnvelope().setTo("hello.world").setBody(PAYLOAD).setHeader("hello", "world");
        EventEnvelope forward = new EventEnvelope().setTo("event.api.forwarder")
                .setBody(event.toBytes()).setHeader("timeout", TIMEOUT).setHeader("rpc", true)
                .setHeader("endpoint", "http://127.0.0.1:"+port+"/api/event")
                .setHeader("authorization", "demo");
        Future<EventEnvelope> response = po.asyncRequest(forward, TIMEOUT);
        response.onSuccess(bench::add);
        // add 500 ms to the bench to capture HTTP-408 response if any
        EventEnvelope result = bench.poll(TIMEOUT + 500, TimeUnit.MILLISECONDS);
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("world", map.getElement("headers.hello"));
        assertEquals(PAYLOAD, map.getElement("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void remoteEventApiOneWayTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long TIMEOUT = 3000;
        int NUMBER_THREE = 3;
        PostOffice po = new PostOffice("unit.test", "12002", "TEST /remote/event/oneway");
        EventEnvelope event = new EventEnvelope();
        event.setTo("hello.world").setBody(NUMBER_THREE).setHeader("hello", "world");
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        Future<EventEnvelope> response = po.asyncRequest(event, TIMEOUT, securityHeaders,
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

    @SuppressWarnings("unchecked")
    @Test
    void remoteEventApiOneWayKotlinTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long TIMEOUT = 3000;
        int NUMBER_THREE = 3;
        PostOffice po = new PostOffice("unit.test", "12003", "TEST /remote/event/oneway/k");
        EventEnvelope event = new EventEnvelope();
        event.setTo("hello.world").setBody(NUMBER_THREE).setHeader("hello", "world");
        EventEnvelope forward = new EventEnvelope().setTo("event.api.forwarder")
                .setBody(event.toBytes()).setHeader("timeout", TIMEOUT).setHeader("rpc", false)
                .setHeader("endpoint", "http://127.0.0.1:"+port+"/api/event")
                .setHeader("authorization", "demo");
        Future<EventEnvelope> response = po.asyncRequest(forward, TIMEOUT);
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

    @SuppressWarnings("unchecked")
    @Test
    void remoteEventApiKotlinTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long TIMEOUT = 3000;
        int NUMBER_THREE = 3;
        PostOffice po = new PostOffice("unit.test", "123", "TEST /remote/event");
        EventEnvelope event = new EventEnvelope().setTo("hello.world")
                .setBody(NUMBER_THREE).setHeader("hello", "world");
        EventEnvelope forward = new EventEnvelope().setTo("event.api.forwarder")
                .setBody(event.toBytes()).setHeader("timeout", TIMEOUT).setHeader("rpc", true)
                .setHeader("endpoint", "http://127.0.0.1:"+port+"/api/event")
                .setHeader("authorization", "demo");
        Future<EventEnvelope> response = po.asyncRequest(forward, TIMEOUT);
        response.onSuccess(bench::add);
        EventEnvelope result = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("world", map.getElement("headers.hello"));
        assertEquals(NUMBER_THREE, map.getElement("body"));
    }

    @Test
    void remoteEventApiKotlinAuthTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long TIMEOUT = 3000;
        int NUMBER_THREE = 3;
        PostOffice po = new PostOffice("unit.test", "123", "TEST /remote/event");
        EventEnvelope event = new EventEnvelope().setTo("hello.world")
                .setBody(NUMBER_THREE).setHeader("hello", "world");
        EventEnvelope forward = new EventEnvelope().setTo("event.api.forwarder")
                .setBody(event.toBytes()).setHeader("timeout", TIMEOUT).setHeader("rpc", true)
                .setHeader("endpoint", "http://127.0.0.1:"+port+"/api/event")
                .setHeader("authorization", "anyone");
        Future<EventEnvelope> response = po.asyncRequest(forward, TIMEOUT);
        response.onSuccess(bench::add);
        EventEnvelope result = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertNotNull(result);
        assertEquals(401, result.getStatus());
        assertInstanceOf(String.class, result.getBody());
        assertEquals("Unauthorized", result.getBody());
    }

    @Test
    void remoteEventApiMissingRouteTest() {
        String TRACE_ID = "123";
        long TIMEOUT = 3000;
        int NUMBER_THREE = 3;
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        PostOffice po = new PostOffice("unit.test", TRACE_ID, "TEST /remote/event");
        EventEnvelope event = new EventEnvelope().setBody(NUMBER_THREE).setHeader("hello", "world");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                po.asyncRequest(event, TIMEOUT, securityHeaders,
                        "http://127.0.0.1:"+port+"/api/event", true));
        assertEquals("Missing routing path", ex.getMessage());
    }

    @Test
    void remoteEventApiNullEventTest() {
        String TRACE_ID = "123";
        long TIMEOUT = 3000;
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        PostOffice po = new PostOffice("unit.test", TRACE_ID, "TEST /remote/event");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                po.asyncRequest(null, TIMEOUT, securityHeaders,
                        "http://127.0.0.1:"+port+"/api/event", true));
        assertEquals("Missing outgoing event", ex.getMessage());
    }

    @Test
    void remoteEventApiRouteNotFoundTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String TRACE_ID = "123";
        long TIMEOUT = 3000;
        int NUMBER_THREE = 3;
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        PostOffice po = new PostOffice("unit.test", TRACE_ID, "TEST /remote/event");
        EventEnvelope event = new EventEnvelope().setTo("some.dummy.route")
                .setBody(NUMBER_THREE).setHeader("hello", "world");
        Future<EventEnvelope> response = po.asyncRequest(event, TIMEOUT, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        response.onSuccess(bench::add);
        EventEnvelope result = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert result != null;
        assertEquals(404, result.getStatus());
        assertEquals("Route some.dummy.route not found", result.getError());
    }

    @Test
    void remoteEventApiAccessControlTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long TIMEOUT = 3000;
        String DEMO_FUNCTION = "demo.private.function";
        LambdaFunction f = (headers, input, instance) -> true;
        Platform platform = Platform.getInstance();
        platform.registerPrivate(DEMO_FUNCTION, f, 1);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope event = new EventEnvelope();
        event.setTo(DEMO_FUNCTION).setBody("ok").setHeader("hello", "world");
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        Future<EventEnvelope> response = po.asyncRequest(event, TIMEOUT, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        response.onSuccess(bench::add);
        EventEnvelope result = bench.poll(5, TimeUnit.SECONDS);
        assert result != null;
        assertEquals(403, result.getStatus());
        assertEquals(DEMO_FUNCTION+" is private", result.getError());
    }

    @Test
    void remoteEventApiAccessControlFutureTest() throws IOException, InterruptedException, ExecutionException {
        long TIMEOUT = 3000;
        String DEMO_FUNCTION = "demo.private.function";
        LambdaFunction f = (headers, input, instance) -> true;
        Platform platform = Platform.getInstance();
        platform.registerPrivate(DEMO_FUNCTION, f, 1);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope event = new EventEnvelope();
        event.setTo(DEMO_FUNCTION).setBody("ok").setHeader("hello", "world");
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        java.util.concurrent.Future<EventEnvelope> future = po.request(event, TIMEOUT, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        EventEnvelope result = future.get();
        assert result != null;
        assertEquals(403, result.getStatus());
        assertEquals(DEMO_FUNCTION+" is private", result.getError());
    }

    @SuppressWarnings("unchecked")
    @Test
    void remoteEventApiFutureTest() throws IOException, InterruptedException, ExecutionException {
        long TIMEOUT = 3000;
        int NUMBER_THREE = 3;
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        PostOffice po = new PostOffice("unit.test", "123", "TEST /remote/event");
        EventEnvelope event = new EventEnvelope().setTo("hello.world")
                .setBody(NUMBER_THREE).setHeader("hello", "world");
        java.util.concurrent.Future<EventEnvelope> future = po.request(event, TIMEOUT, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        EventEnvelope result = future.get();
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("world", map.getElement("headers.hello"));
        // validate that session information is passed by the demo authentication service "event.api.auth"
        assertEquals("demo", map.getElement("headers.user"));
        assertEquals(NUMBER_THREE, map.getElement("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void remoteEventApiTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        long TIMEOUT = 3000;
        int NUMBER_THREE = 3;
        Map<String, String> securityHeaders = new HashMap<>();
        securityHeaders.put("Authorization", "demo");
        PostOffice po = new PostOffice("unit.test", "123", "TEST /remote/event");
        EventEnvelope event = new EventEnvelope().setTo("hello.world")
                .setBody(NUMBER_THREE).setHeader("hello", "world");
        Future<EventEnvelope> response = po.asyncRequest(event, TIMEOUT, securityHeaders,
                "http://127.0.0.1:"+port+"/api/event", true);
        response.onSuccess(bench::add);
        EventEnvelope result = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("world", map.getElement("headers.hello"));
        // validate that session information is passed by the demo authentication service "event.api.auth"
        assertEquals("demo", map.getElement("headers.user"));
        assertEquals(NUMBER_THREE, map.getElement("body"));
    }

}
