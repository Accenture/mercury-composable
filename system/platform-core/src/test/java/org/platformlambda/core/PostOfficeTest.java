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
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.common.JacksonSerializer;
import org.platformlambda.core.models.SimplePoJo;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.*;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.services.ActuatorServices;
import org.platformlambda.core.system.*;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.CryptoApi;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.websocket.client.PersistentWsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class PostOfficeTest extends TestBase {
    private static final Logger log = LoggerFactory.getLogger(PostOfficeTest.class);
    private static final Utility util = Utility.getInstance();
    private static final CryptoApi crypto = new CryptoApi();
    private static final BlockingQueue<String> interceptorBench = new ArrayBlockingQueue<>(1);
    private static final String HELLO_ALIAS = "hello.alias";
    private static final String REACTIVE_MONO = "v1.reactive.mono.function";
    private static final String REACTIVE_FLUX = "v1.reactive.flux.function";
    private static final String X_STREAM_ID = "x-stream-id";
    private static final String X_TTL = "x-ttl";

    @Test
    void getHelloPoJo() throws ExecutionException, InterruptedException {
        var name = "Peter";
        var address = "100 World Blvd";
        PostOffice po = PostOffice.trackable("unit.test", "5", "TEST /api/hello/pojo");
        var req = new EventEnvelope().setTo("hello.pojo").setHeader("name", name).setHeader("address", address);
        var res = po.request(req, 5000).get();
        assertInstanceOf(Map.class, res.getBody());
        var restored = res.restoreBodyAsPoJo();
        assertInstanceOf(PoJo.class, restored.getBody());
        // Note that the "restoreBodyAsPoJo" method updates the original EventEnvelope too
        var pojo = (PoJo) res.getBody();
        assertEquals(name, pojo.getName());
        assertEquals(address, pojo.getAddress());
    }

    @Test
    void getNonStandardErrorResponse() throws ExecutionException, InterruptedException {
        AppConfigReader config = AppConfigReader.getInstance();
        String port = config.getProperty("server.port");
        var data = Map.of("message", "non-standard");
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setTargetHost("http://127.0.0.1:"+port).setUrl("/api/hello/error")
                .setMethod("POST").setBody(data)
                .setHeader("accept", "application/json")
                .setHeader("content-type", "application/json");
        PostOffice po = PostOffice.trackable("unit.test", "10", "TEST /api/test/error");
        EventEnvelope res1 = po.request(new EventEnvelope().setTo("async.http.request")
                                .setBody(req.toMap()), 5000).get();
        EventEnvelope res2 = po.eRequest(new EventEnvelope().setTo("async.http.request")
                                .setBody(req.toMap()), 5000).get();
        assertEquals(res1.getBody(), res2.getBody());
        assertEquals(400, res1.getStatus());
        // demonstrate that non-standard error result can be transported
        assertEquals(Map.of("error", data), res1.getBody());
    }

    @Test
    void testMonoFunction() throws ExecutionException, InterruptedException {
        // test multiple times to validate worker flow control
        final var data = Map.of("hello", "world");
        for (int i=0; i < 12; i++) {
            EventEnvelope request = new EventEnvelope().setTo(REACTIVE_MONO).setBody(data);
            PostOffice po = new PostOffice("unit.test", "100", "TEST /api/mono");
            EventEnvelope response = po.eRequest(request, 5000).get();
            assertEquals(200, response.getStatus());
            assertEquals(data, response.getBody());
        }
    }

    @Test
    void testMonoFunctionWithNullPayload() throws ExecutionException, InterruptedException {
        EventEnvelope request = new EventEnvelope().setTo(REACTIVE_MONO);
        PostOffice po = new PostOffice("unit.test", "101", "TEST /api/mono");
        EventEnvelope response = po.eRequest(request, 5000).get();
        assertEquals(200, response.getStatus());
        assertNull(response.getBody());
    }

    @Test
    void testMonoFunctionWithException() throws ExecutionException, InterruptedException {
        final var data = Map.of("hello", "test");
        final var message = "hello test";
        EventEnvelope request = new EventEnvelope().setTo(REACTIVE_MONO).setBody(data).setHeader("exception", message);
        PostOffice po = new PostOffice("unit.test", "102", "TEST /error/mono");
        EventEnvelope response = po.eRequest(request, 5000).get();
        assertEquals(400, response.getStatus());
        assertEquals(message, response.getError());
        assertInstanceOf(AppException.class, response.getException());
        assertEquals(message, response.getException().getMessage());
    }

    @Test
    void testFluxFunction() throws ExecutionException, InterruptedException {
        final var first = new PoJo();
        first.setName("first_one");
        final var data = new PoJo();
        data.setName("hello");
        EventEnvelope request = new EventEnvelope().setTo(REACTIVE_FLUX).setBody(data);
        PostOffice po = new PostOffice("unit.test", "103", "TEST /api/flux");
        EventEnvelope response = po.eRequest(request, 5000).get();
        assertEquals(200, response.getStatus());
        assertNotNull(response.getHeader(X_STREAM_ID));
        assertNotNull(response.getHeader(X_TTL));
        long ttl = util.str2long(response.getHeader(X_TTL));
        String streamId = response.getHeader(X_STREAM_ID);
        final List<PoJo> messages = new ArrayList<>();
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        // demonstrate pojo transport
        FluxConsumer<PoJo> fc = new FluxConsumer<>(streamId, ttl);
        fc.consume(messages::add, null, () -> bench.add(true), PoJo.class);
        Object done = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(true, done);
        assertEquals(2, messages.size());
        assertEquals(first.getName(), messages.getFirst().getName());
        assertEquals(data.getName(), messages.get(1).getName());
    }

    @Test
    void testFluxWithCustomSerializer() throws ExecutionException, InterruptedException {
        var customSerializer = new JacksonSerializer();
        // the first pojo is inserted by the user function for test purpose
        final var first = new PoJo();
        first.setName("first_one");
        final var data = new PoJo();
        data.setName("custom");
        data.setNumber(123);
        data.setLongNumber(200);
        PostOffice po = PostOffice.withSerializer("unit.test", "103", "TEST /custom/flux", customSerializer);
        EventEnvelope request = new EventEnvelope().setTo("v1.reactive.flux.custom.serializer");
        // perform custom serialization
        po.setEventBodyAsPoJo(request, data);
        EventEnvelope response = po.eRequest(request, 5000).get();
        assertEquals(200, response.getStatus());
        assertNotNull(response.getHeader(X_STREAM_ID));
        assertNotNull(response.getHeader(X_TTL));
        long ttl = util.str2long(response.getHeader(X_TTL));
        String streamId = response.getHeader(X_STREAM_ID);
        final List<PoJo> messages = new ArrayList<>();
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        // demonstrate pojo transport
        FluxConsumer<PoJo> fc = new FluxConsumer<>(streamId, ttl);
        fc.consume(messages::add, null, () -> bench.add(true), PoJo.class, customSerializer);
        Object done = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(true, done);
        assertEquals(2, messages.size());
        assertEquals(first.getName(), messages.getFirst().getName());
        assertEquals(data.getName(), messages.get(1).getName());
        assertEquals(123, messages.get(1).getNumber());
        assertEquals(200, messages.get(1).getLongNumber());
    }

    @Test
    void testFluxFunctionWithException() throws ExecutionException, InterruptedException {
        final var data = Map.of("hello", "test");
        final var message = "hello test";
        EventEnvelope request = new EventEnvelope().setTo(REACTIVE_FLUX).setBody(data).setHeader("exception", message);
        PostOffice po = new PostOffice("unit.test", "104", "TEST /error/flux");
        EventEnvelope response = po.eRequest(request, 5000).get();
        assertEquals(200, response.getStatus());
        assertNotNull(response.getHeader(X_STREAM_ID));
        assertNotNull(response.getHeader(X_TTL));
        long ttl = util.str2long(response.getHeader(X_TTL));
        String streamId = response.getHeader(X_STREAM_ID);
        Map<String, Object> store = new HashMap<>();
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        FluxConsumer<Map<String, Object>> fc = new FluxConsumer<>(streamId, ttl);
        fc.consume(null, e -> {
            store.put("e", e);
            bench.add(false);
        }, () -> bench.add(true));
        Object signal = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(false, signal);
        assertEquals(1, store.size());
        assertInstanceOf(AppException.class, store.get("e"));
        AppException ex = (AppException) store.get("e");
        assertEquals(400, ex.getStatus());
        assertEquals(message, ex.getMessage());
    }

    @Test
    void httpClientRenderSmallPayloadAsBytes() throws ExecutionException, InterruptedException {
        final String hello = "hello world 0123456789";
        final AppConfigReader config = AppConfigReader.getInstance();
        String port = config.getProperty("server.port");
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setTargetHost("http://127.0.0.1:"+port).setHeader("X-Small-Payload-As-Bytes", "true")
                .setUrl("/api/hello/bytes").setMethod("GET");
        EventEnvelope httpResponse = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope response = po.eRequest(httpResponse, 5000).get();
        assertEquals(200, response.getStatus());
        assertInstanceOf(byte[].class, response.getBody());
        if (response.getBody() instanceof byte[] b) {
            assertEquals(hello, util.getUTF(b));
        }
        // HTTP response header "x-content-length" is provided by AsyncHttpClient when rendering small payload as bytes
        assertEquals(String.valueOf(hello.length()), response.getHeader("x-content-length"));
    }

    @Test
    void httpClientDetectStreamingContent() throws ExecutionException, InterruptedException {
        final String hello = "hello world 0123456789";
        final AppConfigReader config = AppConfigReader.getInstance();
        String port = config.getProperty("server.port");
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setTargetHost("http://127.0.0.1:"+port).setUrl("/api/hello/bytes").setMethod("GET");
        EventEnvelope httpResponse = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope response = po.eRequest(httpResponse, 5000).get();
        /*
         * The system will print an INFO log when your app tries to read the empty response body
         * in an EventEnvelope that contains streaming content.
         *
         * It would look like this:
         * EventEnvelope:285 - Event contains streaming content - stream.869af6eb40f043cfb855b8795df63fde.in
         */
        assertEquals(200, response.getStatus());
        assertNull(response.getBody());
        String streamId = response.getHeader("x-stream-id");
        int ttl = util.str2int(response.getHeader("x-ttl"));
        assertNotNull(streamId);
        assertTrue(streamId.startsWith("stream."));
        // HTTP response header "x-content-length" is provided by AsyncHttpClient when rendering small payload as bytes
        assertEquals(String.valueOf(hello.length()), response.getHeader("x-content-length"));
        // Read stream content
        BlockingQueue<Boolean> completion = new LinkedBlockingQueue<>();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FluxConsumer<byte[]> consumer = new FluxConsumer<>(streamId, ttl);
        consumer.consume(b -> {
            try {
                out.write(b);
            } catch (IOException e) {
                // ok to ignore
            }
        }, e -> log.error("unexpected error", e), () -> completion.add(true));
        Boolean done = completion.take();
        assertEquals(true, done);
        assertEquals(hello, util.getUTF(out.toByteArray()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void aliasRouteTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 5000;
        EventEmitter po = EventEmitter.getInstance();
        final String message = "test message";
        po.asyncRequest(new EventEnvelope().setTo(HELLO_ALIAS).setBody(message), timeout).onSuccess(bench::add);
        EventEnvelope response = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(message, body.get("body"));
    }


    @SuppressWarnings("unchecked")
    @Test
    void aliasRouteFutureTest() throws InterruptedException, ExecutionException {
        final long timeout = 5000;
        EventEmitter po = EventEmitter.getInstance();
        final String message = "test message";
        CompletableFuture<EventEnvelope> future = po.eRequest(new EventEnvelope().setTo(HELLO_ALIAS).setBody(message), timeout);
        assert future != null;
        EventEnvelope response = future.get();
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(message, body.get("body"));
    }

    @Test
    void nullRouteListTest() {
        EventEmitter po = EventEmitter.getInstance();
        assertFalse(po.exists((String[]) null));
        assertFalse(po.exists((String) null));
    }

    @SuppressWarnings("unchecked")
    @Test
    void rpcTagTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final PostOffice po = new PostOffice("unit.test", "8011", "TEST /rpc1/timeout/tag");
        final long timeout = 5000;
        final int body = 100;
        final String RPC_TIMEOUT_CHECK = "rpc.timeout.check";
        EventEnvelope request = new EventEnvelope().setTo(RPC_TIMEOUT_CHECK).setBody(body);
        po.asyncRequest(request, timeout).onSuccess(bench::add);
        EventEnvelope response = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(String.valueOf(timeout), result.get(EventEmitter.RPC));
        assertEquals(body, result.get("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void rpcTagFutureTest() throws InterruptedException, ExecutionException {
        final PostOffice po = new PostOffice("unit.test", "8012", "TEST /rpc1/timeout/tag");
        final long timeout = 5000;
        final int body = 100;
        final String rpcTimeoutCheck = "rpc.timeout.check";
        EventEnvelope request = new EventEnvelope().setTo(rpcTimeoutCheck).setBody(body);
        EventEnvelope response = po.eRequest(request, timeout).get();
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(String.valueOf(timeout), result.get(EventEmitter.RPC));
        assertEquals(body, result.get("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void parallelRpcTagTest() throws InterruptedException {
        final BlockingQueue<List<EventEnvelope>> bench = new ArrayBlockingQueue<>(1);
        final PostOffice po = new PostOffice("unit.test", "8021", "TEST /rpc2/timeout/tag");
        final int cycle = 3;
        final long timeout = 5500;
        final String body = "body";
        final String rpcTimeoutCheck = "rpc.timeout.check";
        List<EventEnvelope> requests = new ArrayList<>();
        for (int i=0; i < cycle; i++) {
            requests.add(new EventEnvelope().setTo(rpcTimeoutCheck).setBody(i+1));
        }
        po.asyncRequest(requests, timeout).onSuccess(bench::add);
        List<EventEnvelope> responses = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert responses != null;
        assertEquals(cycle, responses.size());
        List<Integer> payloads = new ArrayList<>();
        for (EventEnvelope response: responses) {
            assertInstanceOf(Map.class, response.getBody());
            Map<String, Object> result = (Map<String, Object>) response.getBody();
            assertTrue(result.containsKey(body));
            assertInstanceOf(Integer.class, result.get(body));
            payloads.add((Integer) result.get(body));
            assertEquals(String.valueOf(timeout), result.get(EventEmitter.RPC));
        }
        assertEquals(cycle, payloads.size());
    }

    @Test
    void parallelRpcTagFutureTest() throws InterruptedException, ExecutionException {
        final PostOffice po = new PostOffice("unit.test", "8022", "TEST /rpc2/timeout/tag");
        final int cycle = 3;
        final long timeout = 5500;
        final String rpcTimeoutCheck = "rpc.timeout.check";
        List<EventEnvelope> requests = new ArrayList<>();
        for (int i=0; i < cycle; i++) {
            requests.add(new EventEnvelope().setTo(rpcTimeoutCheck).setBody(i+1));
        }
        assertParallelRpcTest(po.request(requests, timeout).get());
        assertParallelRpcTest(po.eRequest(requests, timeout).get());
        assertParallelRpcTest(po.request(requests, timeout, false).get());
        assertParallelRpcTest(po.eRequest(requests, timeout, true).get());
    }

    @SuppressWarnings("unchecked")
    void assertParallelRpcTest(List<EventEnvelope> responses) {
        final int cycle = 3;
        final long timeout = 5500;
        final String body = "body";
        assert responses != null;
        assertEquals(cycle, responses.size());
        List<Integer> payloads = new ArrayList<>();
        for (EventEnvelope response: responses) {
            assertInstanceOf(Map.class, response.getBody());
            Map<String, Object> result = (Map<String, Object>) response.getBody();
            assertTrue(result.containsKey(body));
            assertInstanceOf(Integer.class, result.get(body));
            payloads.add((Integer) result.get(body));
            assertEquals(String.valueOf(timeout), result.get(EventEmitter.RPC));
        }
        assertEquals(cycle, payloads.size());
    }

    @Test
    void serviceTimeoutFutureTest() throws ExecutionException, InterruptedException {
        final long timeout = 500;
        final PostOffice po = new PostOffice("unit.test", "28001", "Future /timeout");
        // simulate timeout
        EventEnvelope request = new EventEnvelope().setTo("hello.world").setBody(0)
                .setHeader("timeout_exception", true).setHeader("seq", 0);
        // timeout is returned as a regular event
        CompletableFuture<EventEnvelope> future = po.eRequest(request, timeout, false);
        EventEnvelope result = future.get();
        assertEquals(408, result.getStatus());
        assertEquals("Timeout for "+timeout+" ms", result.getBody());
        // timeout is thrown as an ExecutionException wrapping a TimeoutException
        assertThrows(ExecutionException.class, () -> po.request(request, timeout, true).get());
        ExecutionException e = assertThrows(ExecutionException.class, () ->
                                            po.eRequest(request, timeout, true).get());
        assertNotNull(e);
        assertEquals(ExecutionException.class, e.getClass());
        Throwable ex = e.getCause();
        assertEquals(TimeoutException.class, ex.getClass());
        assertEquals("Timeout for "+timeout+" ms", ex.getMessage());
    }

    @Test
    void wsTest() throws InterruptedException {
        final AppConfigReader config = AppConfigReader.getInstance();
        final int port = util.str2int(config.getProperty("websocket.server.port",
                                        config.getProperty("server.port", "8085")));
        final String welcomeMessage = "welcome";
        final String message = "hello world";
        final String end = "end";
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final EventEmitter po = EventEmitter.getInstance();
        List<String> welcome = new ArrayList<>();
        List<String> txPaths = new ArrayList<>();
        LambdaFunction connector = (headers, input, instance) -> {
            if ("open".equals(headers.get("type"))) {
                String txPath = headers.get("tx_path");
                if (txPaths.isEmpty()) {
                    txPaths.add(txPath);
                }
                assertNotNull(txPath);
                po.send(txPath, welcomeMessage.getBytes());
                po.send(txPath, message);
                po.send(txPath, end);
            }
            if ("string".equals(headers.get("type"))) {
                assertInstanceOf(String.class, input);
                String text = (String) input;
                assertEquals(message, text);
                bench.add(true);
            }
            if ("bytes".equals(headers.get("type"))) {
                assertInstanceOf(byte[].class, input);
                welcome.add(util.getUTF( (byte[]) input));
            }
            return true;
        };
        for (int i=0; i < 3; i++) {
            if (util.portReady("127.0.0.1", port, 3000)) {
                break;
            } else {
                log.info("Waiting for websocket server at port-{} to get ready", port);
                Thread.sleep(1000);
            }
        }
        PersistentWsClient client = new PersistentWsClient(connector,
                Collections.singletonList("ws://127.0.0.1:"+port+"/ws/hello"));
        client.start();
        bench.poll(5, TimeUnit.SECONDS);
        assertEquals(1, welcome.size());
        assertEquals(welcomeMessage, welcome.getFirst());
        client.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testExceptionTransport() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 5000;
        String exception = "exception";
        String message = "just a test";
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo("hello.world").setBody("demo").setHeader(exception, true);
        po.asyncRequest(request, timeout).onSuccess(bench::add);
        EventEnvelope response = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert response != null;
        assertEquals(400, response.getStatus());
        assertEquals(message, response.getBody());
        assertEquals(AppException.class, response.getException().getClass());
        log.info("Exception transported - {}", String.valueOf(response.getException()));
        var stack = util.stackTraceToMap(util.getStackTrace(response.getException()));
        var list = stack.get("stack");
        assertInstanceOf(List.class, list);
        List<String> relevantItems = new ArrayList<>();
        var lines = (List<String>) stack.get("stack");
        lines.forEach(line -> {
            if (line.contains("org.platformlambda.")) {
                relevantItems.add(line);
            }
        });
        assertFalse(relevantItems.isEmpty());
        var stackTrace = response.getStackTrace();
        List<String> records = util.split(stackTrace, "\n");
        // since max.stack.trace.size=5, the number of lines is 6 where the last line indicates the actual stack size.
        assertEquals(6, records.size());
        assertTrue(records.get(5).startsWith("...("));
    }

    @Test
    void testNestedExceptionTransport() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 5000;
        String nestedException = "nested_exception";
        String message = "just a test";
        String sqlError = "sql error";
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo("hello.world").setBody("hi").setHeader(nestedException, true);
        po.asyncRequest(request, timeout).onSuccess(bench::add);
        EventEnvelope response = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert response != null;
        assertEquals(400, response.getStatus());
        // event error is mapped to the root cause
        assertEquals(sqlError, response.getError());
        // nested exception is transported by the response event
        Throwable ex = response.getException();
        // immediate exception
        assertEquals(AppException.class, ex.getClass());
        AppException appEx = (AppException) ex;
        assertEquals(400, appEx.getStatus());
        assertEquals(message, appEx.getMessage());
        // nested exception
        Throwable nested = ex.getCause();
        assertNotNull(nested);
        assertEquals(SQLException.class, nested.getClass());
        assertEquals(sqlError, nested.getMessage());
    }

    @Test
    void findProviderThatExists() throws InterruptedException {
        BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        Platform platform = Platform.getInstance();
        Future<Boolean> status = platform.waitForProvider("cloud.connector", 10);
        status.onSuccess(bench::add);
        Boolean result = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void findProviderThatDoesNotExists() throws InterruptedException {
        BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        Platform platform = Platform.getInstance();
        Future<Boolean> status = platform.waitForProvider("no.such.service", 1);
        status.onSuccess(bench::add);
        Boolean result = bench.poll(12, TimeUnit.SECONDS);
        assertNotEquals(Boolean.TRUE, result);
    }

    @Test
    void findProviderThatIsPending() throws InterruptedException {
        final BlockingQueue<Boolean> bench1 = new ArrayBlockingQueue<>(1);
        final BlockingQueue<EventEnvelope> bench2 = new ArrayBlockingQueue<>(1);
        String futureOperation = "future.operation";
        String delayedStart = "delayed.start";
        Platform platform = Platform.getInstance();
        LambdaFunction noOp = (headers, input, instance) -> true;
        LambdaFunction f = (headers, input, instance) -> {
            platform.register(futureOperation, noOp, 1);
            return true;
        };
        platform.registerPrivate(delayedStart, f, 1);
        PostOffice po = new PostOffice("unit.test", "11", "CHECK /provider");
        // start service one second later, so we can test the waitForProvider method
        var ref = po.sendLater(new EventEnvelope().setTo(delayedStart).setBody("hi"),
                                new Date(System.currentTimeMillis() + 1000));
        assertNotNull(ref);
        Future<Boolean> status = platform.waitForProvider(futureOperation, 5);
        status.onSuccess(bench1::add);
        Boolean result = bench1.poll(12, TimeUnit.SECONDS);
        assertEquals(Boolean.TRUE, result);
        EventEnvelope request = new EventEnvelope().setTo(futureOperation).setBody("ok");
        po.asyncRequest(request, 5000).onSuccess(bench2::add);
        EventEnvelope response = bench2.poll(12, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(true, response.getBody());
        platform.release(futureOperation);
        platform.release(delayedStart);
    }

    @Test
    void deriveOriginIdFromAppId() {
        /*
         * Usually you do not need to set application-ID
         * When you set it, the origin-ID will be generated from the app-ID
         * so that you can correlate user specific information for tracking purpose.
         *
         * Since appId must be set before the "getOrigin" method, the setId is done in the BeforeClass
         * for this unit test.
         */
        Platform platform = Platform.getInstance();
        assertEquals(APP_ID, platform.getAppId());
        // validate the hashing algorithm
        String id = util.getUuid();
        byte[] hash = crypto.getSHA256(util.getUTF(platform.getAppId()));
        id = util.bytes2hex(hash).substring(0, id.length());
        String originId = util.getDateOnly(new Date()) + id;
        assertEquals(platform.getOrigin(), originId);
    }

    @Test
    void registerInvalidRoute() {
        Platform platform = Platform.getInstance();
        LambdaFunction noOp = (headers, input, instance) -> true;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                platform.register("invalidFormat", noOp, 1));
        assertEquals("Invalid route - use 0-9, a-z, period, hyphen or underscore characters",
                ex.getMessage());
    }

    @Test
    void registerNullRoute() {
        Platform platform = Platform.getInstance();
        LambdaFunction noOp = (headers, input, instance) -> true;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                platform.register(null, noOp, 1));
        assertEquals("Missing service routing path", ex.getMessage());
    }

    @Test
    void registerNullService() {
        Platform platform = Platform.getInstance();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                platform.register("no.service", null, 1));
        assertEquals("Missing LambdaFunction instance", ex.getMessage());
    }

    @Test
    void reservedExtensionNotAllowed() {
        Platform platform = Platform.getInstance();
        LambdaFunction noOp = (headers, input, instance) -> true;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                platform.register("nothing.com", noOp, 1));
        assertEquals("Invalid route nothing.com which is use a reserved extension", ex.getMessage());
    }

    @Test
    void reservedFilenameNotAllowed() {
        Platform platform = Platform.getInstance();
        LambdaFunction noOp = (headers, input, instance) -> true;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                platform.register("thumbs.db", noOp, 1));
        assertEquals("Invalid route thumbs.db which is a reserved Windows filename", ex.getMessage());
    }

    @Test
    void reloadPublicServiceAsPrivate() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String service = "reloadable.service";
        long timeout = 5000;
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        LambdaFunction trueFunction = (headers, input, instance) -> true;
        LambdaFunction falseFunction = (headers, input, instance) -> false;
        platform.register(service, trueFunction, 1);
        EventEnvelope request = new EventEnvelope().setTo(service).setBody("HELLO");
        po.asyncRequest(request, timeout).onSuccess(bench::add);
        EventEnvelope result = bench.poll(10, TimeUnit.SECONDS);
        assert result != null;
        assertEquals(true, result.getBody());
        // reload as private
        platform.registerPrivate(service, falseFunction, 1);
        po.asyncRequest(request, timeout).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(false, response.getBody());
        // convert to public
        platform.makePublic(service);
        platform.release(service);
    }

    @Test
    void emptyRouteNotAllowed() {
        Platform platform = Platform.getInstance();
        LambdaFunction noOp = (headers, input, instance) -> true;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                platform.register("", noOp, 1));
        assertEquals("Invalid route - use 0-9, a-z, period, hyphen or underscore characters",
                ex.getMessage());
    }

    @Test
    void checkLocalRouting() {
        Platform platform = Platform.getInstance();
        ConcurrentMap<String, ServiceDef> routes = platform.getLocalRoutingTable();
        assertFalse(routes.isEmpty());
    }

    @Test
    void testExists() throws InterruptedException {
        BlockingQueue<List<String>> bench = new ArrayBlockingQueue<>(1);
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        assertFalse(po.exists());
        assertTrue(po.exists(HELLO_WORLD));
        assertFalse(po.exists(HELLO_WORLD, "unknown.service"));
        assertFalse(po.exists(HELLO_WORLD, "unknown.1", "unknown.2"));
        Future<List<String>> asyncResponse1 = po.search(HELLO_WORLD);
        asyncResponse1.onSuccess(bench::add);
        List<String> origins = bench.poll(5, TimeUnit.SECONDS);
        assert origins != null;
        assertTrue(origins.contains(platform.getOrigin()));
        Future<List<String>> asyncResponse2 = po.search(HELLO_WORLD, true);
        asyncResponse2.onSuccess(bench::add);
        List<String> remoteOrigins = bench.poll(5, TimeUnit.SECONDS);
        assert remoteOrigins != null;
        assertTrue(remoteOrigins.isEmpty());
        assertTrue(po.exists(platform.getOrigin()));
    }

    @Test
    void testNonExistRoute() {
        EventEmitter po = EventEmitter.getInstance();
        var ex = assertThrows(IllegalArgumentException.class, () ->
                po.send("undefined.route", "OK"));
        assertEquals("Route undefined.route not found", ex.getMessage());
    }

    @Test
    void cancelFutureEventTest() {
        long fiveSeconds = 5000;
        long now = System.currentTimeMillis();
        String traceId = util.getUuid();
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope event1 = new EventEnvelope().setTo(HELLO_WORLD)
                .setTraceId(traceId).setTracePath("GET /1").setBody(1);
        EventEnvelope event2 = new EventEnvelope().setTo(HELLO_WORLD)
                .setTraceId(traceId).setTracePath("GET /2").setBody(2);
        String id1 = po.sendLater(event1, new Date(now+(fiveSeconds/10)));
        String id2 = po.sendLater(event2, new Date(now+fiveSeconds));
        assertEquals(event1.getId(), id1);
        assertEquals(event2.getId(), id2);
        List<String> future1 = po.getFutureEvents(HELLO_WORLD);
        assertEquals(2, future1.size());
        assertTrue(future1.contains(id1));
        assertTrue(future1.contains(id2));
        List<String> futureRoutes = po.getAllFutureEvents();
        assertTrue(futureRoutes.contains(HELLO_WORLD));
        Date time = po.getFutureEventTime(id2);
        long diff = time.getTime() - now;
        assertEquals(fiveSeconds, diff);
        po.cancelFutureEvent(id2);
        List<String> futureEvents = po.getFutureEvents(HELLO_WORLD);
        assertTrue(futureEvents.contains(id1));
        po.cancelFutureEvent(id1);
    }

    @Test
    void journalYamlTest() {
        String myFunction = "my.test.function";
        String anotherFunction = "another.function";
        EventEmitter po = EventEmitter.getInstance();
        List<String> routes = po.getJournaledRoutes();
        assertEquals(2, routes.size());
        assertTrue(routes.contains(anotherFunction));
        assertTrue(routes.contains(myFunction));
    }

    @SuppressWarnings("unchecked")
    @Test
    void journalTest() throws InterruptedException {
        String transactionJournalRecorder = "transaction.journal.recorder";
        BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(1);
        Platform platform = Platform.getInstance();
        String from = "unit.test";
        String hello = "hello";
        String world = "world";
        String rv = "some_value";
        String testFunction = "my.test.function";
        String traceId = util.getUuid();
        LambdaFunction f = (headers, input, instance) -> {
            // guarantee that this function has received the correct trace and journal
            Map<String, Object> trace = (Map<String, Object>) input;
            MultiLevelMap map = new MultiLevelMap(trace);
            if (traceId.equals(map.getElement("trace.id"))) {
                bench.add(trace);
            }
            return null;
        };
        LambdaFunction myFunction = (headers, input, instance) -> {
            PostOffice po = PostOffice.trackable(headers, instance);
            po.annotateTrace(hello, world);
            return rv;
        };
        platform.registerPrivate(transactionJournalRecorder, f, 1);
        platform.registerPrivate(testFunction, myFunction, 1);
        PostOffice po = new PostOffice(from, traceId, "GET /api/hello/journal");
        EventEnvelope event = new EventEnvelope().setTo(testFunction).setBody(hello);
        po.send(event);
        // wait for function completion
        Map<String, Object> result = bench.poll(10, TimeUnit.SECONDS);
        platform.release(transactionJournalRecorder);
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals(testFunction, multi.getElement("trace.service"));
        assertEquals(from, multi.getElement("trace.from"));
        assertEquals(traceId, multi.getElement("trace.id"));
        assertEquals(true, multi.getElement("trace.success"));
        assertEquals(hello, multi.getElement("journal.input.body"));
        assertEquals(rv, multi.getElement("journal.output.body"));
        assertEquals(world, multi.getElement("annotations.hello"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void rpcJournalTest() throws InterruptedException {
        String journalRecorder = "transaction.journal.recorder";
        BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(1);
        Platform platform = Platform.getInstance();
        String from = "unit.test";
        String hello = "hello";
        String world = "world";
        String rv = "some_value";
        String testFunction = "my.test.function";
        String traceId = util.getUuid();
        LambdaFunction f = (headers, input, instance) -> {
            // guarantee that this function has received the correct trace and journal
            Map<String, Object> trace = (Map<String, Object>) input;
            MultiLevelMap map = new MultiLevelMap(trace);
            if (traceId.equals(map.getElement("trace.id"))) {
                bench.add(trace);
            }
            return null;
        };
        LambdaFunction myFunction = (headers, input, instance) -> {
            PostOffice po = new PostOffice(headers, instance);
            po.annotateTrace(hello, world);
            return rv;
        };
        platform.registerPrivate(journalRecorder, f, 1);
        platform.registerPrivate(testFunction, myFunction, 1);
        PostOffice po = new PostOffice(from, traceId, "GET /api/hello/journal");
        EventEnvelope event = new EventEnvelope().setTo(testFunction).setBody(hello);
        po.asyncRequest(event, 8000)
            .onSuccess(response -> {
                assertEquals(rv, response.getBody());
                log.info("RPC response for journal verified");
            });
        // wait for function completion
        Map<String, Object> result = bench.poll(10, TimeUnit.SECONDS);
        platform.release(journalRecorder);
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals(testFunction, multi.getElement("trace.service"));
        assertEquals(from, multi.getElement("trace.from"));
        assertEquals(traceId, multi.getElement("trace.id"));
        assertEquals(true, multi.getElement("trace.success"));
        assertEquals(hello, multi.getElement("journal.input.body"));
        assertEquals(rv, multi.getElement("journal.output.body"));
        assertEquals(world, multi.getElement("annotations.hello"));
        // round trip latency is NOT available in journal. It is only delivered to "distributed.trace.forwarder"
        assertFalse(multi.exists("trace.round_trip"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void telemetryTest() throws InterruptedException {
        String traceForwarder = "distributed.trace.forwarder";
        BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(1);
        Platform platform = Platform.getInstance();
        String from = "unit.test";
        String hello = "hello";
        String world = "world";
        String rv = "some_value";
        String simpleFunction = "another.simple.function";
        String traceId = util.getUuid();
        LambdaFunction f = (headers, input, instance) -> {
            // guarantee that this function has received the correct trace
            Map<String, Object> trace = (Map<String, Object>) input;
            MultiLevelMap map = new MultiLevelMap(trace);
            if (traceId.equals(map.getElement("trace.id"))) {
                bench.add(trace);
            }
            return null;
        };
        LambdaFunction myFunction = (headers, input, instance) -> {
            PostOffice po = new PostOffice(headers, instance);
            po.annotateTrace(hello, world);
            return rv;
        };
        platform.registerPrivate(traceForwarder, f, 1);
        platform.registerPrivate(simpleFunction, myFunction, 1);
        PostOffice po = new PostOffice(from, traceId, "GET /api/hello/telemetry");
        po.send(simpleFunction, hello);
        // wait for function completion
        Map<String, Object> result = bench.poll(10, TimeUnit.SECONDS);
        platform.release(traceForwarder);
        platform.release(simpleFunction);
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals(simpleFunction, multi.getElement("trace.service"));
        assertEquals(from, multi.getElement("trace.from"));
        assertEquals(traceId, multi.getElement("trace.id"));
        assertEquals(true, multi.getElement("trace.success"));
        assertEquals(world, multi.getElement("annotations.hello"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void rpcTelemetryTest() throws InterruptedException {
        String traceForwarder = "distributed.trace.forwarder";
        BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(1);
        Platform platform = Platform.getInstance();
        String from = "unit.test";
        String hello = "hello";
        String world = "world";
        String rv = "some_value";
        String simpleFunction = "another.simple.function";
        String traceId = util.getUuid();
        LambdaFunction f = (headers, input, instance) -> {
            // guarantee that this function has received the correct trace
            Map<String, Object> trace = (Map<String, Object>) input;
            MultiLevelMap map = new MultiLevelMap(trace);
            if (traceId.equals(map.getElement("trace.id"))) {
                bench.add(trace);
            }
            return null;
        };
        LambdaFunction myFunction = (headers, input, instance) -> {
            PostOffice po = new PostOffice(headers, instance);
            po.annotateTrace(hello, world);
            return rv;
        };
        platform.registerPrivate(traceForwarder, f, 1);
        platform.registerPrivate(simpleFunction, myFunction, 1);
        PostOffice po = new PostOffice(from, traceId, "GET /api/hello/telemetry");
        po.asyncRequest(new EventEnvelope().setTo(simpleFunction).setBody(hello), 8000)
            .onSuccess(response -> {
               assertEquals(rv, response.getBody());
               log.info("RPC response verified");
            });
        // wait for function completion
        Map<String, Object> result = bench.poll(10, TimeUnit.SECONDS);
        platform.release(traceForwarder);
        platform.release(simpleFunction);
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals(simpleFunction, multi.getElement("trace.service"));
        assertEquals(from, multi.getElement("trace.from"));
        assertEquals(traceId, multi.getElement("trace.id"));
        assertEquals(true, multi.getElement("trace.success"));
        assertEquals(world, multi.getElement("annotations.hello"));
        // round trip latency is available because RPC metrics are delivered to the caller
        assertTrue(multi.exists("trace.round_trip"));
    }

    @Test
    void traceHeaderTest() throws ExecutionException, InterruptedException {
        String traceDetector = "trace.detector";
        String traceId = "101";
        String tracePath = "GET /api/trace";
        Platform platform = Platform.getInstance();
        PostOffice po = new PostOffice("unit.test", traceId, tracePath);
        LambdaFunction f = (headers, input, instance) -> {
            if (headers.containsKey("my_route") &&
                    headers.containsKey("my_trace_id") && headers.containsKey("my_trace_path")) {
                log.info("Trace detector got {}", headers);
                return true;
            } else {
                return false;
            }
        };
        platform.registerPrivate(traceDetector, f, 1);
        EventEnvelope req = new EventEnvelope().setTo(traceDetector).setBody("ok");
        EventEnvelope result = po.eRequest(req, 5000).get();
        platform.release(traceDetector);
        assertEquals(Boolean.TRUE, result.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    void broadcastTest() throws InterruptedException {
        BlockingQueue<String> bench = new ArrayBlockingQueue<>(1);
        String myCallback = "my.callback";
        String message = "test";
        String done = "done";
        Platform platform = Platform.getInstance();
        LambdaFunction callback = (headers, input, instance) -> {
            if (input instanceof Map && message.equals(((Map<String, Object>) input).get("body"))) {
                    bench.add(done);
            }
            return null;
        };
        platform.registerPrivate(myCallback, callback, 1);
        PostOffice po = new PostOffice("unit.test", "222", "/broadcast/test");
        po.send(new EventEnvelope().setTo(HELLO_WORLD).setBody(message).setReplyTo(myCallback));
        String result = bench.poll(10, TimeUnit.SECONDS);
        assertEquals(done, result);
        // these are drop-n-forget since there are no reply-to address
        po.send(HELLO_WORLD, new Kv("test", "message"), new Kv("key", "value"));
        po.send(HELLO_WORLD, "some message", new Kv("hello", "world"));
        po.broadcast(HELLO_WORLD, "another message");
        po.broadcast(HELLO_WORLD, "another message", new Kv("key", "value"));
        po.broadcast(HELLO_WORLD, new Kv("hello", "world"), new Kv("key", "value"));
        // this one has replyTo
        po.broadcast(new EventEnvelope().setTo(HELLO_WORLD).setBody(message).setReplyTo(myCallback));
        result = bench.poll(10, TimeUnit.SECONDS);
        assertEquals(done, result);
        platform.release(myCallback);
    }

    @Test
    void eventHasFromAddress() throws InterruptedException {
        String first = "hello.world.one";
        String second = "hello.world.two";
        Platform platform = Platform.getInstance();
        EventEmitter emitter = EventEmitter.getInstance();
        LambdaFunction f1 = (headers, input, instance) -> {
            PostOffice po = new PostOffice(headers, instance);
            po.send(second, true);
            return Optional.empty();
        };
        platform.register(first, f1, 1);
        platform.register(second, new SimpleInterceptor(), 1);
        // without tracing
        emitter.send(first, Optional.empty());
        String result = interceptorBench.poll(5, TimeUnit.SECONDS);
        // validate the "from" address
        assertEquals(first, result);
    }

    @Test
    void singleRequestWithTimeout() throws InterruptedException {
        BlockingQueue<Throwable> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo("hello.world").setBody(2);
        po.asyncRequest(request, 800).onFailure(bench::add);
        Throwable ex = bench.poll(10, TimeUnit.SECONDS);
        assert ex != null;
        assertEquals("Timeout for 800 ms", ex.getMessage());
        assertEquals(TimeoutException.class, ex.getClass());
    }

    @Test
    void singleRequestWithException() throws InterruptedException {
        BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo("hello.world").setFrom("unit.test")
                                    .setTrace("100", "TEST /timeout/exception")
                                    .setHeader("exception", true).setBody(1);
        po.asyncRequest(request, 8000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(400, response.getStatus());
        assertEquals("just a test", response.getError());
        assertEquals(AppException.class, response.getException().getClass());
    }

    @SuppressWarnings("unchecked")
    @Test
    void singleRequest() throws InterruptedException {
        BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        int input = 111;
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo("hello.world").setHeader("a", "b").setBody(input);
        po.asyncRequest(request, 8000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(HashMap.class, response.getBody().getClass());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(input, result.get("body"));
    }

    @Test
    void asyncRequestTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> success = new ArrayBlockingQueue<>(1);
        final String service = "hello.future.1";
        final String text = "hello world";
        final Platform platform = Platform.getInstance();
        final EventEmitter po = EventEmitter.getInstance();
        final LambdaFunction f = (headers, input, instance) -> input;
        platform.registerPrivate(service, f, 1);
        EventEnvelope request = new EventEnvelope().setTo(service)
                                    .setBody(text).setTrace("1030", "TEST /api/async/request");
        Future<EventEnvelope> future = po.asyncRequest(request, 1500);
        future.onSuccess(event -> {
            platform.release(service);
            success.add(event);
        });
        EventEnvelope result = success.poll(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertEquals(text, result.getBody());
    }

    @Test
    void futureExceptionAsResult() throws InterruptedException {
        final BlockingQueue<EventEnvelope> completion = new ArrayBlockingQueue<>(1);
        int status = 400;
        String error = "some exception";
        String service = "hello.future.2";
        String text = "hello world";
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        LambdaFunction f = (headers, input, instance) -> {
            throw new AppException(status, error);
        };
        platform.registerPrivate(service, f, 1);
        Future<EventEnvelope> future = po.asyncRequest(new EventEnvelope().setTo(service).setBody(text), 5000);
        future.onSuccess(event -> {
            platform.release(service);
            completion.add(event);
        });
        EventEnvelope result = completion.poll(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(status, result.getStatus());
        assertEquals(error, result.getBody());
    }

    @Test
    void asyncForkJoinTest() throws InterruptedException {
        final BlockingQueue<List<EventEnvelope>> success = new ArrayBlockingQueue<>(1);
        final String from = "unit.test";
        final String traceId = "1020";
        final String tracePath = "TEST /async/fork-n-join";
        final String service = "hello.future.3";
        final String text = "hello world";
        final int parallelInstances = 5;
        final Platform platform = Platform.getInstance();
        final EventEmitter po = EventEmitter.getInstance();
        final LambdaFunction f1 = (headers, input, instance) -> input;
        platform.registerPrivate(service, f1, parallelInstances);
        List<EventEnvelope> requests = new ArrayList<>();
        for (int i=1; i < parallelInstances + 1; i++) {
            EventEnvelope req = new EventEnvelope().setTo(service).setBody(text + "." + i)
                                    .setFrom(from).setTrace(traceId, tracePath);
            requests.add(req);
        }
        Future<List<EventEnvelope>> future = po.asyncRequest(requests, 1500);
        future.onSuccess(event -> {
            platform.release(service);
            success.add(event);
        });
        List<EventEnvelope> result = success.poll(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(parallelInstances, result.size());
        for (EventEnvelope r: result) {
            assertInstanceOf(String.class, r.getBody());
            String responseText = (String) r.getBody();
            assertTrue(responseText.startsWith(text));
            log.info("Received response #{} {} - {}", r.getCorrelationId(), r.getId(), text);
        }
    }

    @Test
    void asyncForkJoinTimeoutTest() throws InterruptedException {
        final long timeout = 500;
        final BlockingQueue<Throwable> exception = new ArrayBlockingQueue<>(1);
        final String service = "hello.future.4";
        final String text = "hello world";
        final int parallelInstances = 5;
        final Platform platform = Platform.getInstance();
        final EventEmitter po = EventEmitter.getInstance();
        final LambdaFunction f1 = (headers, input, instance) -> {
            log.info("Received fork-n-join event {}, {}", headers, input);
            return input;
        };
        platform.registerPrivate(service, f1, parallelInstances);
        List<EventEnvelope> requests = new ArrayList<>();
        for (int i=1; i <= parallelInstances; i++) {
            requests.add(new EventEnvelope().setTo(service).setBody(text + "." + i)
                            .setHeader("timeout_exception", true)
                            .setHeader("seq", i));
        }
        requests.add(new EventEnvelope().setTo("hello.world").setBody(2));
        Future<List<EventEnvelope>> future = po.asyncRequest(requests, timeout, true);
        future.onFailure(exception::add);
        Throwable e = exception.poll(5, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(TimeoutException.class, e.getClass());
        assertEquals("Timeout for "+timeout+" ms", e.getMessage());
        platform.release(service);
    }

    @Test
    void asyncForkJoinPartialResultTest() throws InterruptedException {
        final long timeout = 800;
        final BlockingQueue<List<EventEnvelope>> result = new ArrayBlockingQueue<>(1);
        final String service = "hello.future.5";
        final String text = "hello world";
        final int parallelInstances = 5;
        final Platform platform = Platform.getInstance();
        final EventEmitter po = EventEmitter.getInstance();
        final LambdaFunction f1 = (headers, input, instance) -> {
            log.info("Received event {}, {}", headers, input);
            return input;
        };
        platform.registerPrivate(service, f1, parallelInstances);
        List<EventEnvelope> requests = new ArrayList<>();
        for (int i=1; i <= parallelInstances; i++) {
            requests.add(new EventEnvelope().setTo(service).setBody(text + "." + i)
                    .setHeader("partial_result", true)
                    .setHeader("seq", i));
        }
        // hello.world will make an artificial delay of one second so that it will not be included in the result set.
        requests.add(new EventEnvelope().setTo("hello.world").setBody(2));
        Future<List<EventEnvelope>> future = po.asyncRequest(requests, timeout, false);
        future.onSuccess(result::add);
        List<EventEnvelope> responses = result.poll(10, TimeUnit.SECONDS);
        assert responses != null;
        assertEquals(parallelInstances, responses.size());
        for (EventEnvelope evt: responses) {
            assertNotNull(evt.getBody());
            assertTrue(evt.getBody().toString().startsWith(text));
        }
        platform.release(service);
    }

    @SuppressWarnings("unchecked")
    @Test
    void multilevelTrace() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final String routeOne = "hello.level.1";
        final String routeTwo = "hello.level.2";
        final String traceId = "cid-123456";
        final String tracePath = "GET /api/hello/world";
        Platform platform = Platform.getInstance();
        LambdaFunction tier2 = (headers, input, instance) -> {
            PostOffice po = new PostOffice(headers, instance);
            assertEquals(routeTwo, po.getRoute());
            assertEquals(traceId, po.getTraceId());
            // annotations are local to a service and should not be transported to the next service
            assertTrue(po.getTrace().annotations.isEmpty());
            return po.getTraceId();
        };
        platform.register(routeTwo, tier2, 1);
        // test tracing to 2 levels
        String testMessage = "some message";
        EventEnvelope event = new EventEnvelope();
        event.setTo(routeOne).setHeader("hello", "world").setBody(testMessage);
        event.setTrace(traceId, tracePath).setFrom("unit.test");
        EventEmitter po = EventEmitter.getInstance();
        po.asyncRequest(event, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(HashMap.class, response.getBody().getClass());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("world", map.getElement("headers.hello"));
        assertEquals(testMessage, map.getElement("body"));
        assertEquals(traceId, map.getElement("trace_id"));
        assertEquals(tracePath, map.getElement("trace_path"));
        assertEquals(routeOne, map.getElement("route_one"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void routeSubstitution() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        int input = 111;
        EventEmitter po = EventEmitter.getInstance();
        // with route substitution in the application.properties, hello.test will route to hello.world
        EventEnvelope request = new EventEnvelope().setTo("hello.test").setBody(input);
        po.asyncRequest(request, 8000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(HashMap.class, response.getBody().getClass());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(input, result.get("body"));
        Map<String, String> list = po.getRouteSubstitutionList();
        assertTrue(list.containsKey("hello.test"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void healthTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo(ActuatorServices.ACTUATOR_SERVICES).setHeader("type" ,"health");
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> map = (Map<String, Object>) response.getBody();
        assertEquals("UP", map.get("status"));
        assertEquals("platform-core", map.get("name"));
        assertEquals(platform.getOrigin(), map.get("origin"));
        Object dependency = map.get("dependency");
        assertInstanceOf(List.class, dependency);
        List<Map<String, Object>> dependencyList = (List<Map<String, Object>>) dependency;
        assertEquals(1, dependencyList.size());
        Map<String, Object> health = dependencyList.getFirst();
        assertEquals("mock.connector", health.get("service"));
        assertEquals("mock.topic", health.get("topics"));
        assertEquals("fine", health.get("message"));
        assertEquals("true", health.get("required").toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    void infoTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        PostOffice po = new PostOffice("unit.test", "201", "TEST /info/test");
        EventEnvelope request = new EventEnvelope().setTo(ActuatorServices.ACTUATOR_SERVICES).setHeader("type" ,"info");
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        var result = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertTrue(result.exists("app"));
        assertTrue(result.exists("memory"));
        assertTrue(result.exists("personality"));
        assertTrue(result.exists("java.version"));
        assertTrue(result.exists("streams"));
        assertTrue(result.exists("origin"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void libTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo(ActuatorServices.ACTUATOR_SERVICES).setHeader("type" ,"lib");
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertTrue(result.containsKey("app"));
        assertTrue(result.containsKey("library"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void infoRouteTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String myFunction = "my.test.function";
        String anotherFunction = "another.function";
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo(ActuatorServices.ACTUATOR_SERVICES).setHeader("type" ,"routes");
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap multi = new MultiLevelMap((Map<String, Object>) response.getBody());
        Object journalRoutes = multi.getElement("journal");
        assertInstanceOf(List.class, journalRoutes);
        List<String> routes = (List<String>) journalRoutes;
        assertTrue(routes.contains(myFunction));
        assertTrue(routes.contains(anotherFunction));
    }

    @Test
    void livenessProbeTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo(ActuatorServices.ACTUATOR_SERVICES)
                                    .setHeader("type" ,"livenessprobe");
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals("OK", response.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    void envTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        // making RPC directly to the actuator service
        EventEnvelope request = new EventEnvelope().setTo(ActuatorServices.ACTUATOR_SERVICES).setHeader("type" ,"env");
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        // normalize the map for easy retrieval using MultiLevelMap
        Map<String, Object> result = util.getFlatMap((Map<String, Object>) response.getBody());
        MultiLevelMap multi = new MultiLevelMap();
        result.forEach(multi::setElement);
        assertEquals("platform-core", multi.getElement("app.name"));
        assertInstanceOf(Map.class, multi.getElement("env"));
        assertEquals(System.getenv("PATH"), multi.getElement("env.environment.PATH"));
        // environment variables that are not found will be shown as empty string
        assertEquals("", multi.getElement("env.environment.NON_EXIST"));
        assertEquals("true", multi.getElement("env.properties.rest.automation"));
        assertEquals("true", multi.getElement("env.properties.snake.case.serialization"));
    }

    @Test
    void envelopeAsResponseTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String target = "test.route.1";
        String message = "hello world";
        EventEmitter po = EventEmitter.getInstance();
        Platform.getInstance().register(target, new EventEnvelopeReader(), 1);
        EventEnvelope request = new EventEnvelope().setTo(target).setBody(message);
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(message, response.getBody());
    }

    @Test
    void threadPoolTest() throws InterruptedException {
        final int cycles = 200;
        final int workerPool = 50;
        final ConcurrentMap<Long, Boolean> threads = new ConcurrentHashMap<>();
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final AtomicInteger counter = new AtomicInteger(0);
        final AtomicLong last = new AtomicLong(0);
        String multiCores = "multi.cores";
        LambdaFunction f= (headers, input, instance) -> {
            int n = counter.incrementAndGet();
            long id = Thread.currentThread().threadId();
            log.debug("Instance #{}, count={}, thread #{} {}", instance, n, id, input);
            threads.put(id, true);
            if (n == cycles) {
                last.set(System.currentTimeMillis());
                bench.add(true);
            }
            return true;
        };
        Platform.getInstance().registerPrivate(multiCores, f, workerPool);
        EventEmitter po = EventEmitter.getInstance();
        long t1 = System.currentTimeMillis();
        for (int i=0; i < cycles; i++) {
            po.send(multiCores, "hello world");
        }
        Boolean result = bench.poll(10, TimeUnit.SECONDS);
        long diff = last.get() - t1;
        log.info("{} cycles done? {}, {} workers consumed {} threads in {} ms",
                cycles, result, workerPool, threads.size(), diff);
        assertTrue(counter.get() > workerPool);
    }

    @Test
    void testCallBackEventHandler() throws InterruptedException {
        final BlockingQueue<Object> bench = new ArrayBlockingQueue<>(1);
        String traceId = "10000";
        String hello = "hello";
        String pojoHappyCase = "pojo.happy.case.1";
        String simpleCallback = "simple.callback.1";
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        LambdaFunction f = (headers, input, instance) -> {
            PoJo pojo = new PoJo();
            pojo.setName((String) input);
            return pojo;
        };
        platform.registerPrivate(pojoHappyCase, f, 1);
        platform.registerPrivate(simpleCallback, new SimpleCallback(bench, traceId), 1);
        po.send(new EventEnvelope().setTo(pojoHappyCase).setReplyTo(simpleCallback).setBody(hello)
                        .setFrom("unit.test").setTrace(traceId, "TEST /callback"));
        Object result = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(PoJo.class, result.getClass());
        assertEquals(hello, ((PoJo) result).getName());
        platform.release(pojoHappyCase);
        platform.release(simpleCallback);
    }

    @Test
    void testMapToPoJoCasting() throws InterruptedException {
        final BlockingQueue<Object> bench = new ArrayBlockingQueue<>(1);
        var traceId = "30000";
        var text = "test";
        var hello = Map.of("name", text);
        String pojoSuccessCase = "pojo.success.case";
        String simpleCallback = "simple.callback.2";
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        LambdaFunction f = (headers, input, instance) -> hello;
        platform.registerPrivate(pojoSuccessCase, f, 1);
        platform.registerPrivate(simpleCallback, new SimpleCallback(bench, traceId), 1);
        po.send(new EventEnvelope().setTo(pojoSuccessCase).setReplyTo(simpleCallback).setBody(hello)
                .setTrace(traceId, "TEST /best/effort/mapping"));
        Object result = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(PoJo.class, result.getClass());
        PoJo restored = (PoJo) result;
        assertEquals(text, restored.getName());
        platform.release(pojoSuccessCase);
        platform.release(simpleCallback);
    }

    @Test
    void testCallBackCastingException() throws InterruptedException {
        final BlockingQueue<Object> bench = new ArrayBlockingQueue<>(1);
        String traceId = "30000";
        String hello = "hello";
        String pojoErrorCase = "pojo.error.case.3";
        String simpleCallback = "simple.callback.3";
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        LambdaFunction f = (headers, input, instance) -> hello;
        platform.registerPrivate(pojoErrorCase, f, 1);
        platform.registerPrivate(simpleCallback, new SimpleCallback(bench, traceId), 1);
        po.send(new EventEnvelope().setTo(pojoErrorCase).setReplyTo(simpleCallback).setBody(hello)
                .setTrace(traceId, "TEST /cast/error"));
        Object result = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(AppException.class, result.getClass());
        AppException ex = (AppException) result;
        assertEquals(500, ex.getStatus());
        assertEquals("class java.lang.String cannot be cast to class org.platformlambda.core.models.PoJo",
                ex.getMessage());
        platform.release(pojoErrorCase);
        platform.release(simpleCallback);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testInputObjectMapping() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String traceId = "101010";
        String tracePath = "TEST /api/hello/input/mapping";
        String autoMapping = "hello.input.mapping";
        String helloWorld = "hello world";
        Date now = new Date();
        LocalDateTime time = Instant.ofEpochMilli(now.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        EventEmitter po = EventEmitter.getInstance();
        // prove that two PoJo are compatible when sending data fields that intersect
        PoJoSubset minimalData = new PoJoSubset();
        minimalData.setName(helloWorld);
        minimalData.setDate(now);
        minimalData.setLocalDateTime(time);
        // the function "hello.input.mapping" is configured with input serialization strategy = camel
        // and output serialization strategy = default
        var camelCaseData = SimpleMapper.getInstance().getCamelCaseMapper().readValue(minimalData, Map.class);
        EventEnvelope request = new EventEnvelope().setTo(autoMapping).setBody(camelCaseData)
                                .setTrace(traceId,tracePath).setFrom("unit.test");
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        // for security, automatic PoJo class restore is disabled
        assertEquals(ArrayList.class, response.getBody().getClass());
        // original PoJo class name is transported by the event envelope
        assertEquals(PoJo.class.getName(), response.getType());
        List<Object> listOfMaps = (List<Object>) response.getBody();
        assertEquals(1, listOfMaps.size());
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(listOfMaps.getFirst(), PoJo.class);
        assertEquals(now, pojo.getDate());
        assertEquals(time, pojo.getLocalDateTime());
        assertEquals(helloWorld, pojo.getName());
        // default values in PoJo
        assertEquals(0, pojo.getNumber());
        assertEquals(0L, pojo.getLongNumber());
        // the demo function is designed to return its function execution types
        assertEquals("true", response.getHeader("coroutine"));
        assertEquals("false", response.getHeader("interceptor"));
        assertEquals("true", response.getHeader("tracing"));
        // the demo function will also echo the READ only route, trace ID and path
        assertEquals(autoMapping, response.getHeader("route"));
        assertEquals(traceId, response.getHeader("trace_id"));
        assertEquals(tracePath, response.getHeader("trace_path"));
        // the system will filter out reserved metadata - my_route, my_trace_id, my_trace_path
        assertNull(response.getHeader("my_route"));
        assertNull(response.getHeader("my_trace_id"));
        assertNull(response.getHeader("my_trace_path"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPrimitiveTransport() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String helloWorld = "hello.world";
        int number = 101;
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo(helloWorld).setBody(number);
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        Map<String, Object> map = (Map<String, Object>) response.getBody();
        assertEquals(number, map.get("body"));
        Date now = new Date();
        request = new EventEnvelope().setTo(helloWorld).setBody(now);
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        map = (Map<String, Object>) response.getBody();
        assertEquals(util.date2str(now), map.get("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCustomSerializer() throws ExecutionException, InterruptedException {
        PostOffice po = new PostOffice("custom.serializer.test",
                "10108", "/custom/serializer", new JacksonSerializer());
        SimplePoJo pojo = new SimplePoJo();
        pojo.fullName = "hello";
        pojo.address = "world";
        pojo.telephone = 12345678;
        var event1 = new EventEnvelope().setTo("custom.serializer.service.java");
        po.setEventBodyAsPoJo(event1, pojo);
        // class name is encoded as type so user function can inspect it
        assertEquals(pojo.getClass().getName(), event1.getType());
        // test java user function
        EventEnvelope result1 = po.eRequest(event1, 5000).get();
        assertInstanceOf(Map.class, result1.getBody());
        Map<String, Object> data1 = (Map<String, Object>) result1.getBody();
        assertEquals(pojo.fullName, data1.get("fullName"));
        assertEquals(pojo.address, data1.get("address"));
        assertEquals(pojo.telephone, data1.get("telephone"));
        SimplePoJo responsePoJo1 = po.getEventBodyAsPoJo(result1, SimplePoJo.class);
        assertEquals(pojo.fullName, responsePoJo1.fullName);
        assertEquals(pojo.address, responsePoJo1.address);
        assertEquals(pojo.telephone, responsePoJo1.telephone);
        assertEquals("org.platformlambda.core.models.SimplePoJo", result1.getType());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testInputAsListOfPoJoJava() throws ExecutionException, InterruptedException {
        var route = "input.list.of.pojo.java";
        var traceId = "10201";
        PostOffice po = new PostOffice("list.of.pojo.test", traceId, "GET /list/of/pojo");
        List<PoJo> input = new ArrayList<>();
        // note that when using list of pojo, the pojo type must be the same
        // there must be at least one pojo in the list.
        PoJo pojo1 = new PoJo();
        pojo1.setName("hello1");
        input.add(pojo1);
        PoJo pojo2 = new PoJo();
        pojo2.setName("hello2");
        input.add(pojo2);
        // prove that null can also be transported in a list of pojo
        input.add(null);
        var event = new EventEnvelope().setTo(route).setBody(input);
        var result = po.eRequest(event, 5000).get();
        assertInstanceOf(Map.class, result.getBody());
        var map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals(pojo1.getName(), map.getElement("names[0]"));
        assertEquals(pojo2.getName(), map.getElement("names[1]"));
        assertEquals("null", map.getElement("names[2]"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testInputAsPoJoUntyped() throws ExecutionException, InterruptedException {
        PostOffice po = new PostOffice("untyped.pojo.test", "10300", "GET /untyped/pojo");
        PoJo pojo = new PoJo();
        pojo.setName("hello");
        // prove that list of pojo can be restored using the inputPojoClass in the PreLoad annotation
        var event = new EventEnvelope().setTo("input.pojo.untyped").setBody(pojo);
        var result = po.eRequest(event, 5000).get();
        assertInstanceOf(Map.class, result.getBody());
        var map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals(pojo.getName(), map.getElement("map.name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testInputAsPoJoListUntyped() throws ExecutionException, InterruptedException {
        PostOffice po = new PostOffice("untyped.pojo.test", "10301", "GET /untyped/pojo");
        PoJo pojo = new PoJo();
        pojo.setName("hello");
        // prove that list of pojo can be restored using the inputPojoClass in the PreLoad annotation
        var event = new EventEnvelope().setTo("input.pojo.untyped").setBody(List.of(pojo));
        var result = po.eRequest(event, 5000).get();
        assertInstanceOf(Map.class, result.getBody());
        var map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals(pojo.getName(), map.getElement("list[0].name"));
    }

    private record SimpleCallback(BlockingQueue<Object> bench, String traceId)
                                    implements TypedLambdaFunction<PoJo, Void>, MappingExceptionHandler {
        @Override
        public void onError(String route, AppException e, EventEnvelope event, int instance) {
            EventEmitter po = EventEmitter.getInstance();
            TraceInfo trace = po.getTrace(route, instance);
            if (trace != null && traceId.equals(trace.id)) {
                log.info("Caught casting exception, id={}, status={}, message={}",
                        trace.id, e.getStatus(), e.getMessage());
                bench.add(e);
            }
        }

        @Override
        public Void handleEvent(Map<String, String> headers, PoJo body, int instance) {
            PostOffice po = new PostOffice(headers, instance);
            if (traceId.equals(po.getTraceId())) {
                log.info("onEvent found trace path '{}'", po.getTrace().path);
                bench.add(body);
            }
            return null;
        }
    }

    @EventInterceptor
    private static class SimpleInterceptor implements TypedLambdaFunction<EventEnvelope, Void> {
        @Override
        public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance) {
            log.info("{} received event from {}", headers, event.getFrom());
            interceptorBench.add(event.getFrom());
            return null;
        }
    }

    private static class EventEnvelopeReader implements TypedLambdaFunction<EventEnvelope, EventEnvelope> {
        @Override
        public EventEnvelope handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
            return new EventEnvelope().setBody(input.getBody());
        }
    }
}
