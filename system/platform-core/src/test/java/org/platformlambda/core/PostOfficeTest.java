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
    private static final String TASK_EXECUTOR = "task.executor";
    private static final String EVENT_MANAGER = "event.script.manager";
    private static final String REGULAR_FUNCTION = "regular.function";
    private static final String HELLO_ALIAS = "hello.alias";
    private static final String REACTIVE_MONO = "v1.reactive.mono.function";
    private static final String REACTIVE_FLUX = "v1.reactive.flux.function";
    private static final String REACTIVE_MONO_KOTLIN = "v1.reactive.mono.kotlin";
    private static final String REACTIVE_FLUX_KOTLIN = "v1.reactive.flux.kotlin";
    private static final String X_STREAM_ID = "x-stream-id";
    private static final String X_TTL = "x-ttl";

    @Test
    void getNonStandardErrorResponse() throws IOException, ExecutionException, InterruptedException {
        AppConfigReader config = AppConfigReader.getInstance();
        String port = config.getProperty("server.port");
        var data = Map.of("message", "non-standard");
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setTargetHost("http://127.0.0.1:"+port).setUrl("/api/hello/error")
                .setMethod("POST").setBody(data)
                .setHeader("accept", "application/json")
                .setHeader("content-type", "application/json");
        PostOffice po = new PostOffice("unit.test", "10", "TEST /api/test/error");
        EventEnvelope res = po.request(new EventEnvelope().setTo("async.http.request")
                            .setBody(req.toMap()), 5000).get();
        assertEquals(400, res.getStatus());
        // demonstrate that non-standard error result can be transported
        assertEquals(Map.of("error", data), res.getBody());
    }

    @Test
    void testEventManagementOptimization() throws IOException, InterruptedException {
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        final String UNKNOWN = "unknown";
        BlockingQueue<String> routeBench = new LinkedBlockingQueue<>();
        BlockingQueue<Object> inputBench = new LinkedBlockingQueue<>();
        BlockingQueue<Integer> freeWorkerBench = new LinkedBlockingQueue<>();
        LambdaFunction f = (headers, input, instance) -> {
            String myRoute = headers.getOrDefault("my_route", UNKNOWN);
            routeBench.add(myRoute);
            inputBench.add(input);
            if (input instanceof String functionName) {
                Map<String, ServiceDef> routingTable = platform.getLocalRoutingTable();
                var service = routingTable.get(functionName);
                freeWorkerBench.add(service.getManager().getFreeWorkers());
            } else if (input instanceof EventEnvelope event) {
                Map<String, ServiceDef> routingTable = platform.getLocalRoutingTable();
                var service = routingTable.get(event.getTo());
                freeWorkerBench.add(service.getManager().getFreeWorkers());
            } else {
                freeWorkerBench.add(-1);
            }
            return null;
        };
        /*
         * In this unit test, we want to prove that the regular function is executed through
         * the event loop and the event script functions are served by virtual threads directly.
         *
         * This optimization permits ideal event choreography in the event-script-engine module.
         */
        platform.registerPrivate(TASK_EXECUTOR, f, 1);
        platform.registerPrivate(EVENT_MANAGER, f, 1);
        platform.registerPrivate(REGULAR_FUNCTION, f, 1);
        // metadata is inserted by the event system when routing to regular functions
        po.send(REGULAR_FUNCTION, REGULAR_FUNCTION);
        String regularResult = routeBench.poll(10, TimeUnit.SECONDS);
        assertEquals(REGULAR_FUNCTION, regularResult);
        assertInstanceOf(String.class, inputBench.take());
        // since this is a regular event routing, the number of worker is decremented by one
        assertEquals(0, freeWorkerBench.take());
        // metadata is not set when routing to the event script manager function
        po.send(TASK_EXECUTOR, TASK_EXECUTOR);
        String taskExecutorResult = routeBench.poll(10, TimeUnit.SECONDS);
        assertEquals(UNKNOWN, taskExecutorResult);
        Object taskExecutorInput = inputBench.take();
        assertInstanceOf(EventEnvelope.class, taskExecutorInput);
        var taskExecutorEvent = (EventEnvelope) taskExecutorInput;
        assertEquals(TASK_EXECUTOR, taskExecutorEvent.getTo());
        // since this is a direct execution, the number of worker is not changed
        assertEquals(1, freeWorkerBench.take());
        // metadata is not set when routing to the task executor function
        po.send(EVENT_MANAGER, EVENT_MANAGER);
        String eventManagerResult = routeBench.poll(10, TimeUnit.SECONDS);
        assertEquals(UNKNOWN, eventManagerResult);
        Object eventManagerInput = inputBench.take();
        assertInstanceOf(EventEnvelope.class, eventManagerInput);
        var eventManagerEvent = (EventEnvelope) eventManagerInput;
        assertEquals(EVENT_MANAGER, eventManagerEvent.getTo());
        // since this is a direct execution, the number of worker is not changed
        assertEquals(1, freeWorkerBench.take());
    }

    @Test
    void testMonoFunction() throws IOException, ExecutionException, InterruptedException {
        // test multiple times to validate worker flow control
        for (int i=0; i < 12; i++) {
            testMonoFunction(REACTIVE_MONO);
        }
    }

    @Test
    void testMonoKotlinFunction() throws IOException, ExecutionException, InterruptedException {
        // test multiple times to validate worker flow control
        for (int i=0; i < 8; i++) {
            testMonoFunction(REACTIVE_MONO_KOTLIN);
        }
    }

    private void testMonoFunction(String target) throws IOException, ExecutionException, InterruptedException {
        final var data = Map.of("hello", "world");
        EventEnvelope request = new EventEnvelope().setTo(target).setBody(data);
        PostOffice po = new PostOffice("unit.test", "100", "TEST /api/mono");
        EventEnvelope response = po.request(request, 5000).get();
        assertEquals(200, response.getStatus());
        assertEquals(data, response.getBody());
    }

    @Test
    void testMonoFunctionWithNullPayload() throws IOException, ExecutionException, InterruptedException {
        testMonoFunctionWithNullPayload(REACTIVE_MONO);
    }

    @Test
    void testMonoKotlinFunctionWithNullPayload() throws IOException, ExecutionException, InterruptedException {
        testMonoFunctionWithNullPayload(REACTIVE_MONO_KOTLIN);
    }

    private void testMonoFunctionWithNullPayload(String target) throws IOException, ExecutionException, InterruptedException {
        EventEnvelope request = new EventEnvelope().setTo(target);
        PostOffice po = new PostOffice("unit.test", "101", "TEST /api/mono");
        EventEnvelope response = po.request(request, 5000).get();
        assertEquals(200, response.getStatus());
        assertNull(response.getBody());
    }

    @Test
    void testMonoFunctionWithException() throws IOException, ExecutionException, InterruptedException {
        testMonoFunctionWithException(REACTIVE_MONO);
    }

    @Test
    void testMonoKotlinFunctionWithException() throws IOException, ExecutionException, InterruptedException {
        testMonoFunctionWithException(REACTIVE_MONO_KOTLIN);
    }

    private void testMonoFunctionWithException(String target) throws IOException, ExecutionException, InterruptedException {
        final var data = Map.of("hello", "test");
        final var MESSAGE = "hello test";
        EventEnvelope request = new EventEnvelope().setTo(target).setBody(data).setHeader("exception", MESSAGE);
        PostOffice po = new PostOffice("unit.test", "102", "TEST /error/mono");
        EventEnvelope response = po.request(request, 5000).get();
        assertEquals(400, response.getStatus());
        assertEquals(MESSAGE, response.getError());
        assertInstanceOf(AppException.class, response.getException());
        assertEquals(MESSAGE, response.getException().getMessage());
    }

    @Test
    void testFluxFunction() throws IOException, ExecutionException, InterruptedException {
        final var first = new PoJo();
        first.setName("first_one");
        final var data = new PoJo();
        data.setName("hello");
        EventEnvelope request = new EventEnvelope().setTo(REACTIVE_FLUX).setBody(data);
        PostOffice po = new PostOffice("unit.test", "103", "TEST /api/flux");
        EventEnvelope response = po.request(request, 5000).get();
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
    void testFluxWithCustomSerializer() throws IOException, ExecutionException, InterruptedException {
        var customSerializer = new JacksonSerializer();
        // the first pojo is inserted by the user function for test purpose
        final var first = new PoJo();
        first.setName("first_one");
        final var data = new PoJo();
        data.setName("custom");
        data.setNumber(123);
        data.setLongNumber(200);
        PostOffice po = new PostOffice("unit.test", "103", "TEST /custom/flux", customSerializer);
        EventEnvelope request = new EventEnvelope().setTo("v1.reactive.flux.custom.serializer");
        // perform custom serialization
        po.setEventBodyAsPoJo(request, data);
        EventEnvelope response = po.request(request, 5000).get();
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
    void testFluxKotlinFunction() throws IOException, ExecutionException, InterruptedException {
        final var first = Map.of("first", "message");
        final var data = Map.of("hello", "world");
        EventEnvelope request = new EventEnvelope().setTo(REACTIVE_FLUX_KOTLIN).setBody(data);
        PostOffice po = new PostOffice("unit.test", "103", "TEST /api/flux");
        EventEnvelope response = po.request(request, 5000).get();
        assertEquals(200, response.getStatus());
        assertNotNull(response.getHeader(X_STREAM_ID));
        assertNotNull(response.getHeader(X_TTL));
        long ttl = util.str2long(response.getHeader(X_TTL));
        String streamId = response.getHeader(X_STREAM_ID);
        final List<Map<String, Object>> messages = new ArrayList<>();
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        FluxConsumer<Map<String, Object>> fc = new FluxConsumer<>(streamId, ttl);
        // test Map transport
        fc.consume(messages::add, null, () -> bench.add(true));
        Object done = bench.poll(5, TimeUnit.SECONDS);
        assertEquals(true, done);
        assertEquals(2, messages.size());
        assertEquals(first, messages.getFirst());
        assertEquals(data, messages.get(1));
    }

    @Test
    void testFluxFunctionWithException() throws IOException, ExecutionException, InterruptedException {
        testFluxFunctionWithException(REACTIVE_FLUX);
    }

    @Test
    void testFluxKotlinFunctionWithException() throws IOException, ExecutionException, InterruptedException {
        testFluxFunctionWithException(REACTIVE_FLUX_KOTLIN);
    }

    private void testFluxFunctionWithException(String target) throws IOException, ExecutionException, InterruptedException {
        final var data = Map.of("hello", "test");
        final var MESSAGE = "hello test";
        EventEnvelope request = new EventEnvelope().setTo(target).setBody(data).setHeader("exception", MESSAGE);
        PostOffice po = new PostOffice("unit.test", "104", "TEST /error/flux");
        EventEnvelope response = po.request(request, 5000).get();
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
        assertEquals(MESSAGE, ex.getMessage());
    }

    @Test
    void httpClientRenderSmallPayloadAsBytes() throws IOException, ExecutionException, InterruptedException {
        final String HELLO = "hello world 0123456789";
        final AppConfigReader config = AppConfigReader.getInstance();
        String port = config.getProperty("server.port");
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setTargetHost("http://127.0.0.1:"+port).setHeader("X-Small-Payload-As-Bytes", "true")
                .setUrl("/api/hello/bytes").setMethod("GET");
        EventEnvelope httpResponse = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope response = po.request(httpResponse, 5000).get();
        assertEquals(200, response.getStatus());
        assertInstanceOf(byte[].class, response.getBody());
        if (response.getBody() instanceof byte[] b) {
            assertEquals(HELLO, util.getUTF(b));
        }
        // HTTP response header "x-content-length" is provided by AsyncHttpClient when rendering small payload as bytes
        assertEquals(String.valueOf(HELLO.length()), response.getHeader("x-content-length"));
    }

    @Test
    void httpClientDetectStreamingContent() throws IOException, ExecutionException, InterruptedException {
        final String HELLO = "hello world 0123456789";
        final AppConfigReader config = AppConfigReader.getInstance();
        String port = config.getProperty("server.port");
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setTargetHost("http://127.0.0.1:"+port).setUrl("/api/hello/bytes").setMethod("GET");
        EventEnvelope httpResponse = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope response = po.request(httpResponse, 5000).get();
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
        assertEquals(String.valueOf(HELLO.length()), response.getHeader("x-content-length"));
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
        assertEquals(HELLO, util.getUTF(out.toByteArray()));
    }

    @Test
    void concurrentEventTest() throws InterruptedException {
        final BlockingQueue<Boolean> wait = new ArrayBlockingQueue<>(1);
        final AppConfigReader config = AppConfigReader.getInstance();
        int poolSize = Math.max(32, util.str2int(config.getProperty("kernel.thread.pool", "100")));
        final ExecutorService executor = Platform.getInstance().getVirtualThreadExecutor();
        final String RPC_FORWARDER = "rpc.forwarder";
        final String SLOW_SERVICE = "artificial.delay";
        final int CYCLES = poolSize / 2;
        log.info("Test sync and blocking RPC with {} workers", CYCLES);
        final long TIMEOUT = 10000;
        PostOffice po = new PostOffice("unit.test", "12345", "/TEST");
        // make nested RPC calls
        long begin = System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger passes = new AtomicInteger(0);
        for (int i=0; i < CYCLES; i++) {
            /*
             * The blocking RPC will hold up a large number of worker threads = CYCLES
             * Therefore, this test will break if CYCLES > poolSize.
             */
            executor.submit(() -> {
                int count = counter.incrementAndGet();
                try {
                    final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
                    String message = "hello world "+count;
                    EventEnvelope request = new EventEnvelope().setTo(RPC_FORWARDER)
                            .setHeader("target", SLOW_SERVICE).setHeader("timeout", TIMEOUT).setBody(message);
                    po.asyncRequest(request, TIMEOUT, true).onSuccess(bench::add);
                    EventEnvelope response = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                    assert response != null;
                    if (message.equals(response.getBody())) {
                        passes.incrementAndGet();
                    }
                    if (passes.get() >= CYCLES) {
                        wait.add(true);
                    }
                } catch (Exception e) {
                    log.error("Exception - {}", e.getMessage());
                }
            });
        }
        log.info("Wait for concurrent responses from service");
        wait.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        long diff = System.currentTimeMillis() - begin;
        // demonstrate parallelism that the total time consumed should not be too far from the artificial delay
        log.info("Finished in {} ms", diff);
        assertEquals(CYCLES, passes.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    void aliasRouteTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 5000;
        EventEmitter po = EventEmitter.getInstance();
        final String MESSAGE = "test message";
        po.asyncRequest(new EventEnvelope().setTo(HELLO_ALIAS).setBody(MESSAGE), TIMEOUT).onSuccess(bench::add);
        EventEnvelope response = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(MESSAGE, body.get("body"));
    }


    @SuppressWarnings("unchecked")
    @Test
    void aliasRouteFutureTest() throws IOException, InterruptedException, ExecutionException {
        final long TIMEOUT = 5000;
        EventEmitter po = EventEmitter.getInstance();
        final String MESSAGE = "test message";
        java.util.concurrent.Future<EventEnvelope> future = po.request(new EventEnvelope().setTo(HELLO_ALIAS).setBody(MESSAGE), TIMEOUT);
        assert future != null;
        EventEnvelope response = future.get();
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(MESSAGE, body.get("body"));
    }

    @Test
    void nullRouteListTest() {
        EventEmitter po = EventEmitter.getInstance();
        assertFalse(po.exists((String[]) null));
        assertFalse(po.exists((String) null));
    }

    @SuppressWarnings("unchecked")
    @Test
    void rpcTagTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final PostOffice po = new PostOffice("unit.test", "8011", "TEST /rpc1/timeout/tag");
        final long TIMEOUT = 5000;
        final int BODY = 100;
        final String RPC_TIMEOUT_CHECK = "rpc.timeout.check";
        EventEnvelope request = new EventEnvelope().setTo(RPC_TIMEOUT_CHECK).setBody(BODY);
        po.asyncRequest(request, TIMEOUT).onSuccess(bench::add);
        EventEnvelope response = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(String.valueOf(TIMEOUT), result.get(EventEmitter.RPC));
        assertEquals(BODY, result.get("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void rpcTagFutureTest() throws IOException, InterruptedException, ExecutionException {
        final PostOffice po = new PostOffice("unit.test", "8012", "TEST /rpc1/timeout/tag");
        final long TIMEOUT = 5000;
        final int BODY = 100;
        final String RPC_TIMEOUT_CHECK = "rpc.timeout.check";
        EventEnvelope request = new EventEnvelope().setTo(RPC_TIMEOUT_CHECK).setBody(BODY);
        EventEnvelope response = po.request(request, TIMEOUT).get();
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> result = (Map<String, Object>) response.getBody();
        assertEquals(String.valueOf(TIMEOUT), result.get(EventEmitter.RPC));
        assertEquals(BODY, result.get("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void parallelRpcTagTest() throws IOException, InterruptedException {
        final BlockingQueue<List<EventEnvelope>> bench = new ArrayBlockingQueue<>(1);
        final PostOffice po = new PostOffice("unit.test", "8021", "TEST /rpc2/timeout/tag");
        final int CYCLE = 3;
        final long TIMEOUT = 5500;
        final String BODY = "body";
        final String RPC_TIMEOUT_CHECK = "rpc.timeout.check";
        List<EventEnvelope> requests = new ArrayList<>();
        for (int i=0; i < CYCLE; i++) {
            requests.add(new EventEnvelope().setTo(RPC_TIMEOUT_CHECK).setBody(i+1));
        }
        po.asyncRequest(requests, TIMEOUT).onSuccess(bench::add);
        List<EventEnvelope> responses = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert responses != null;
        assertEquals(CYCLE, responses.size());
        List<Integer> payloads = new ArrayList<>();
        for (EventEnvelope response: responses) {
            assertInstanceOf(Map.class, response.getBody());
            Map<String, Object> result = (Map<String, Object>) response.getBody();
            assertTrue(result.containsKey(BODY));
            assertInstanceOf(Integer.class, result.get(BODY));
            payloads.add((Integer) result.get(BODY));
            assertEquals(String.valueOf(TIMEOUT), result.get(EventEmitter.RPC));
        }
        assertEquals(CYCLE, payloads.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void parallelRpcTagFutureTest() throws IOException, InterruptedException, ExecutionException {
        final PostOffice po = new PostOffice("unit.test", "8022", "TEST /rpc2/timeout/tag");
        final int CYCLE = 3;
        final long TIMEOUT = 5500;
        final String BODY = "body";
        final String RPC_TIMEOUT_CHECK = "rpc.timeout.check";
        List<EventEnvelope> requests = new ArrayList<>();
        for (int i=0; i < CYCLE; i++) {
            requests.add(new EventEnvelope().setTo(RPC_TIMEOUT_CHECK).setBody(i+1));
        }
        List<EventEnvelope> responses = po.request(requests, TIMEOUT).get();
        assert responses != null;
        assertEquals(CYCLE, responses.size());
        List<Integer> payloads = new ArrayList<>();
        for (EventEnvelope response: responses) {
            assertInstanceOf(Map.class, response.getBody());
            Map<String, Object> result = (Map<String, Object>) response.getBody();
            assertTrue(result.containsKey(BODY));
            assertInstanceOf(Integer.class, result.get(BODY));
            payloads.add((Integer) result.get(BODY));
            assertEquals(String.valueOf(TIMEOUT), result.get(EventEmitter.RPC));
        }
        assertEquals(CYCLE, payloads.size());
    }

    @Test
    void serviceTimeoutFutureTest() throws IOException, ExecutionException, InterruptedException {
        final long TIMEOUT = 500;
        final PostOffice po = new PostOffice("unit.test", "28001", "Future /timeout");
        // simulate timeout
        EventEnvelope request = new EventEnvelope().setTo("hello.world").setBody(0)
                .setHeader("timeout_exception", true).setHeader("seq", 0);
        // timeout is returned as a regular event
        java.util.concurrent.Future<EventEnvelope> future = po.request(request, TIMEOUT, false);
        EventEnvelope result = future.get();
        assertEquals(408, result.getStatus());
        assertEquals("Timeout for "+TIMEOUT+" ms", result.getBody());
        // timeout is thrown as an ExecutionException wrapping a TimeoutException
        ExecutionException e = assertThrows(ExecutionException.class, () ->
                po.request(request, TIMEOUT, true).get());
        assertNotNull(e);
        assertEquals(ExecutionException.class, e.getClass());
        Throwable ex = e.getCause();
        assertEquals(TimeoutException.class, ex.getClass());
        assertEquals("Timeout for "+TIMEOUT+" ms", ex.getMessage());
    }

    @Test
    void wsTest() throws InterruptedException {
        final AppConfigReader config = AppConfigReader.getInstance();
        final int PORT = util.str2int(config.getProperty("websocket.server.port",
                                        config.getProperty("server.port", "8085")));
        final String WELCOME = "welcome";
        final String MESSAGE = "hello world";
        final String END = "end";
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
                po.send(txPath, WELCOME.getBytes());
                po.send(txPath, MESSAGE);
                po.send(txPath, END);
            }
            if ("string".equals(headers.get("type"))) {
                assertInstanceOf(String.class, input);
                String text = (String) input;
                assertEquals(MESSAGE, text);
                bench.add(true);
            }
            if ("bytes".equals(headers.get("type"))) {
                assertInstanceOf(byte[].class, input);
                welcome.add(util.getUTF( (byte[]) input));
            }
            return true;
        };
        for (int i=0; i < 3; i++) {
            if (util.portReady("127.0.0.1", PORT, 3000)) {
                break;
            } else {
                log.info("Waiting for websocket server at port-{} to get ready", PORT);
                Thread.sleep(1000);
            }
        }
        PersistentWsClient client = new PersistentWsClient(connector,
                Collections.singletonList("ws://127.0.0.1:"+PORT+"/ws/hello"));
        client.start();
        bench.poll(5, TimeUnit.SECONDS);
        assertEquals(1, welcome.size());
        assertEquals(WELCOME, welcome.getFirst());
        client.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testExceptionTransport() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 5000;
        String EXCEPTION = "exception";
        String MESSAGE = "just a test";
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo("hello.world").setBody("demo").setHeader(EXCEPTION, true);
        po.asyncRequest(request, TIMEOUT).onSuccess(bench::add);
        EventEnvelope response = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert response != null;
        assertEquals(400, response.getStatus());
        assertEquals(MESSAGE, response.getBody());
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
        log.info("{}", Map.of("stack", relevantItems));
    }

    @Test
    void testNestedExceptionTransport() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 5000;
        String NEST_EXCEPTION = "nested_exception";
        String MESSAGE = "just a test";
        String SQL_ERROR = "sql error";
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo("hello.world").setBody("hi").setHeader(NEST_EXCEPTION, true);
        po.asyncRequest(request, TIMEOUT).onSuccess(bench::add);
        EventEnvelope response = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert response != null;
        assertEquals(400, response.getStatus());
        // event error is mapped to the root cause
        assertEquals(SQL_ERROR, response.getError());
        // nested exception is transported by the response event
        Throwable ex = response.getException();
        // immediate exception
        assertEquals(AppException.class, ex.getClass());
        AppException appEx = (AppException) ex;
        assertEquals(400, appEx.getStatus());
        assertEquals(MESSAGE, appEx.getMessage());
        // nested exception
        Throwable nested = ex.getCause();
        assertNotNull(nested);
        assertEquals(SQLException.class, nested.getClass());
        assertEquals(SQL_ERROR, nested.getMessage());
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
    void findProviderThatIsPending() throws IOException, InterruptedException {
        final BlockingQueue<Boolean> bench1 = new ArrayBlockingQueue<>(1);
        final BlockingQueue<EventEnvelope> bench2 = new ArrayBlockingQueue<>(1);
        String NO_OP = "no.op";
        String PENDING_SERVICE = "pending.service";
        Platform platform = Platform.getInstance();
        LambdaFunction noOp = (headers, input, instance) -> true;
        LambdaFunction f = (headers, input, instance) -> {
            platform.register(NO_OP, noOp, 1);
            return true;
        };
        platform.registerPrivate(PENDING_SERVICE, f, 1);
        PostOffice po = new PostOffice("unit.test", "11", "CHECK /provider");
        // start service two seconds later, so we can test the waitForProvider method
        po.sendLater(new EventEnvelope().setTo(PENDING_SERVICE).setBody("hi"),
                new Date(System.currentTimeMillis()+2100));
        Future<Boolean> status = platform.waitForProvider(NO_OP, 5);
        status.onSuccess(bench1::add);
        Boolean result = bench1.poll(12, TimeUnit.SECONDS);
        assertEquals(Boolean.TRUE, result);
        EventEnvelope request = new EventEnvelope().setTo(NO_OP).setBody("ok");
        po.asyncRequest(request, 5000).onSuccess(bench2::add);
        EventEnvelope response = bench2.poll(12, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(true, response.getBody());
        platform.release(NO_OP);
        platform.release(PENDING_SERVICE);
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
    void reloadPublicServiceAsPrivate() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String SERVICE = "reloadable.service";
        long TIMEOUT = 5000;
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        LambdaFunction TRUE_FUNCTION = (headers, input, instance) -> true;
        LambdaFunction FALSE_FUNCTION = (headers, input, instance) -> false;
        platform.register(SERVICE, TRUE_FUNCTION, 1);
        EventEnvelope request = new EventEnvelope().setTo(SERVICE).setBody("HELLO");
        po.asyncRequest(request, TIMEOUT).onSuccess(bench::add);
        EventEnvelope result = bench.poll(10, TimeUnit.SECONDS);
        assert result != null;
        assertEquals(true, result.getBody());
        // reload as private
        platform.registerPrivate(SERVICE, FALSE_FUNCTION, 1);
        po.asyncRequest(request, TIMEOUT).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(false, response.getBody());
        // convert to public
        platform.makePublic(SERVICE);
        platform.release(SERVICE);
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
        IOException ex = assertThrows(IOException.class, () ->
                po.send("undefined.route", "OK"));
        assertEquals("Route undefined.route not found", ex.getMessage());
    }

    @Test
    void cancelFutureEventTest() {
        long FIVE_SECONDS = 5000;
        long now = System.currentTimeMillis();
        String TRACE_ID = util.getUuid();
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope event1 = new EventEnvelope().setTo(HELLO_WORLD)
                .setTraceId(TRACE_ID).setTracePath("GET /1").setBody(1);
        EventEnvelope event2 = new EventEnvelope().setTo(HELLO_WORLD)
                .setTraceId(TRACE_ID).setTracePath("GET /2").setBody(2);
        String id1 = po.sendLater(event1, new Date(now+(FIVE_SECONDS/10)));
        String id2 = po.sendLater(event2, new Date(now+FIVE_SECONDS));
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
        assertEquals(FIVE_SECONDS, diff);
        po.cancelFutureEvent(id2);
        List<String> futureEvents = po.getFutureEvents(HELLO_WORLD);
        assertTrue(futureEvents.contains(id1));
        po.cancelFutureEvent(id1);
    }

    @Test
    void journalYamlTest() {
        String MY_FUNCTION = "my.test.function";
        String ANOTHER_FUNCTION = "another.function";
        EventEmitter po = EventEmitter.getInstance();
        List<String> routes = po.getJournaledRoutes();
        assertEquals(2, routes.size());
        assertTrue(routes.contains(ANOTHER_FUNCTION));
        assertTrue(routes.contains(MY_FUNCTION));
    }

    @SuppressWarnings("unchecked")
    @Test
    void journalTest() throws IOException, InterruptedException {
        String TRANSACTION_JOURNAL_RECORDER = "transaction.journal.recorder";
        BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(1);
        Platform platform = Platform.getInstance();
        String FROM = "unit.test";
        String HELLO = "hello";
        String WORLD = "world";
        String RETURN_VALUE = "some_value";
        String MY_FUNCTION = "my.test.function";
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
            po.annotateTrace(HELLO, WORLD);
            return RETURN_VALUE;
        };
        platform.registerPrivate(TRANSACTION_JOURNAL_RECORDER, f, 1);
        platform.registerPrivate(MY_FUNCTION, myFunction, 1);
        PostOffice po = new PostOffice(FROM, traceId, "GET /api/hello/journal");
        EventEnvelope event = new EventEnvelope().setTo(MY_FUNCTION).setBody(HELLO);
        po.send(event);
        // wait for function completion
        Map<String, Object> result = bench.poll(10, TimeUnit.SECONDS);
        platform.release(TRANSACTION_JOURNAL_RECORDER);
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals(MY_FUNCTION, multi.getElement("trace.service"));
        assertEquals(FROM, multi.getElement("trace.from"));
        assertEquals(traceId, multi.getElement("trace.id"));
        assertEquals(true, multi.getElement("trace.success"));
        assertEquals(HELLO, multi.getElement("journal.input.body"));
        assertEquals(RETURN_VALUE, multi.getElement("journal.output.body"));
        assertEquals(WORLD, multi.getElement("annotations.hello"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void rpcJournalTest() throws IOException, InterruptedException {
        String TRANSACTION_JOURNAL_RECORDER = "transaction.journal.recorder";
        BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(1);
        Platform platform = Platform.getInstance();
        String FROM = "unit.test";
        String HELLO = "hello";
        String WORLD = "world";
        String RETURN_VALUE = "some_value";
        String MY_FUNCTION = "my.test.function";
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
            po.annotateTrace(HELLO, WORLD);
            return RETURN_VALUE;
        };
        platform.registerPrivate(TRANSACTION_JOURNAL_RECORDER, f, 1);
        platform.registerPrivate(MY_FUNCTION, myFunction, 1);
        PostOffice po = new PostOffice(FROM, traceId, "GET /api/hello/journal");
        EventEnvelope event = new EventEnvelope().setTo(MY_FUNCTION).setBody(HELLO);
        po.asyncRequest(event, 8000)
            .onSuccess(response -> {
                assertEquals(RETURN_VALUE, response.getBody());
                log.info("RPC response for journal verified");
            });
        // wait for function completion
        Map<String, Object> result = bench.poll(10, TimeUnit.SECONDS);
        platform.release(TRANSACTION_JOURNAL_RECORDER);
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals(MY_FUNCTION, multi.getElement("trace.service"));
        assertEquals(FROM, multi.getElement("trace.from"));
        assertEquals(traceId, multi.getElement("trace.id"));
        assertEquals(true, multi.getElement("trace.success"));
        assertEquals(HELLO, multi.getElement("journal.input.body"));
        assertEquals(RETURN_VALUE, multi.getElement("journal.output.body"));
        assertEquals(WORLD, multi.getElement("annotations.hello"));
        // round trip latency is NOT available in journal. It is only delivered to "distributed.trace.forwarder"
        assertFalse(multi.exists("trace.round_trip"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void telemetryTest() throws IOException, InterruptedException {
        String DISTRIBUTED_TRACE_FORWARDER = "distributed.trace.forwarder";
        BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(1);
        Platform platform = Platform.getInstance();
        String FROM = "unit.test";
        String HELLO = "hello";
        String WORLD = "world";
        String RETURN_VALUE = "some_value";
        String SIMPLE_FUNCTION = "another.simple.function";
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
            po.annotateTrace(HELLO, WORLD);
            return RETURN_VALUE;
        };
        platform.registerPrivate(DISTRIBUTED_TRACE_FORWARDER, f, 1);
        platform.registerPrivate(SIMPLE_FUNCTION, myFunction, 1);
        PostOffice po = new PostOffice(FROM, traceId, "GET /api/hello/telemetry");
        po.send(SIMPLE_FUNCTION, HELLO);
        // wait for function completion
        Map<String, Object> result = bench.poll(10, TimeUnit.SECONDS);
        platform.release(DISTRIBUTED_TRACE_FORWARDER);
        platform.release(SIMPLE_FUNCTION);
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals(SIMPLE_FUNCTION, multi.getElement("trace.service"));
        assertEquals(FROM, multi.getElement("trace.from"));
        assertEquals(traceId, multi.getElement("trace.id"));
        assertEquals(true, multi.getElement("trace.success"));
        assertEquals(WORLD, multi.getElement("annotations.hello"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void rpcTelemetryTest() throws IOException, InterruptedException {
        String DISTRIBUTED_TRACE_FORWARDER = "distributed.trace.forwarder";
        BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(1);
        Platform platform = Platform.getInstance();
        String FROM = "unit.test";
        String HELLO = "hello";
        String WORLD = "world";
        String RETURN_VALUE = "some_value";
        String SIMPLE_FUNCTION = "another.simple.function";
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
            po.annotateTrace(HELLO, WORLD);
            return RETURN_VALUE;
        };
        platform.registerPrivate(DISTRIBUTED_TRACE_FORWARDER, f, 1);
        platform.registerPrivate(SIMPLE_FUNCTION, myFunction, 1);
        PostOffice po = new PostOffice(FROM, traceId, "GET /api/hello/telemetry");
        po.asyncRequest(new EventEnvelope().setTo(SIMPLE_FUNCTION).setBody(HELLO), 8000)
            .onSuccess(response -> {
               assertEquals(RETURN_VALUE, response.getBody());
               log.info("RPC response verified");
            });
        // wait for function completion
        Map<String, Object> result = bench.poll(10, TimeUnit.SECONDS);
        platform.release(DISTRIBUTED_TRACE_FORWARDER);
        platform.release(SIMPLE_FUNCTION);
        MultiLevelMap multi = new MultiLevelMap(result);
        assertEquals(SIMPLE_FUNCTION, multi.getElement("trace.service"));
        assertEquals(FROM, multi.getElement("trace.from"));
        assertEquals(traceId, multi.getElement("trace.id"));
        assertEquals(true, multi.getElement("trace.success"));
        assertEquals(WORLD, multi.getElement("annotations.hello"));
        // round trip latency is available because RPC metrics are delivered to the caller
        assertTrue(multi.exists("trace.round_trip"));
    }

    @Test
    void traceHeaderTest() throws IOException, ExecutionException, InterruptedException {
        String TRACE_DETECTOR = "trace.detector";
        String TRACE_ID = "101";
        String TRACE_PATH = "GET /api/trace";
        Platform platform = Platform.getInstance();
        PostOffice po = new PostOffice("unit.test", TRACE_ID, TRACE_PATH);
        LambdaFunction f = (headers, input, instance) -> {
            if (headers.containsKey("my_route") &&
                    headers.containsKey("my_trace_id") && headers.containsKey("my_trace_path")) {
                log.info("Trace detector got {}", headers);
                return true;
            } else {
                return false;
            }
        };
        platform.registerPrivate(TRACE_DETECTOR, f, 1);
        EventEnvelope req = new EventEnvelope().setTo(TRACE_DETECTOR).setBody("ok");
        EventEnvelope result = po.request(req, 5000).get();
        platform.release(TRACE_DETECTOR);
        assertEquals(Boolean.TRUE, result.getBody());
    }

    @Test
    void coroutineTraceHeaderTest() throws IOException, InterruptedException, ExecutionException {
        String COROUTINE_TRACE_DETECTOR = "coroutine.trace.detector";
        String TRACE_ID = "102";
        String TRACE_PATH = "GET /api/trace";
        PostOffice po = new PostOffice("unit.test", TRACE_ID, TRACE_PATH);
        EventEnvelope req = new EventEnvelope().setTo(COROUTINE_TRACE_DETECTOR).setBody("ok");
        EventEnvelope result = po.request(req, 5000).get();
        assertEquals(Boolean.TRUE, result.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    void broadcastTest() throws IOException, InterruptedException {
        BlockingQueue<String> bench = new ArrayBlockingQueue<>(1);
        String CALLBACK = "my.callback";
        String MESSAGE = "test";
        String DONE = "done";
        Platform platform = Platform.getInstance();
        LambdaFunction callback = (headers, input, instance) -> {
            if (input instanceof Map) {
                if (MESSAGE.equals(((Map<String, Object>) input).get("body"))) {
                    bench.add(DONE);
                }
            }
            return null;
        };
        platform.registerPrivate(CALLBACK, callback, 1);
        PostOffice po = new PostOffice("unit.test", "222", "/broadcast/test");
        po.send(new EventEnvelope().setTo(HELLO_WORLD).setBody(MESSAGE).setReplyTo(CALLBACK));
        String result = bench.poll(10, TimeUnit.SECONDS);
        assertEquals(DONE, result);
        // these are drop-n-forget since there are no reply-to address
        po.send(HELLO_WORLD, new Kv("test", "message"), new Kv("key", "value"));
        po.send(HELLO_WORLD, "some message", new Kv("hello", "world"));
        po.broadcast(HELLO_WORLD, "another message");
        po.broadcast(HELLO_WORLD, "another message", new Kv("key", "value"));
        po.broadcast(HELLO_WORLD, new Kv("hello", "world"), new Kv("key", "value"));
        // this one has replyTo
        po.broadcast(new EventEnvelope().setTo(HELLO_WORLD).setBody(MESSAGE).setReplyTo(CALLBACK));
        result = bench.poll(10, TimeUnit.SECONDS);
        assertEquals(DONE, result);
        platform.release(CALLBACK);
    }

    @Test
    void eventHasFromAddress() throws IOException, InterruptedException {
        String FIRST = "hello.world.one";
        String SECOND = "hello.world.two";
        Platform platform = Platform.getInstance();
        EventEmitter emitter = EventEmitter.getInstance();
        LambdaFunction f1 = (headers, input, instance) -> {
            PostOffice po = new PostOffice(headers, instance);
            po.send(SECOND, true);
            return Optional.empty();
        };
        platform.register(FIRST, f1, 1);
        platform.register(SECOND, new SimpleInterceptor(), 1);
        // without tracing
        emitter.send(FIRST, Optional.empty());
        String result = interceptorBench.poll(5, TimeUnit.SECONDS);
        // validate the "from" address
        assertEquals(FIRST, result);
    }

    @Test
    void singleRequestWithTimeout() throws IOException, InterruptedException {
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
    void singleRequestWithException() throws IOException, InterruptedException {
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
    void singleRequest() throws IOException, InterruptedException {
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
    void asyncRequestTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> success = new ArrayBlockingQueue<>(1);
        final String SERVICE = "hello.future.1";
        final String TEXT = "hello world";
        final Platform platform = Platform.getInstance();
        final EventEmitter po = EventEmitter.getInstance();
        final LambdaFunction f = (headers, input, instance) -> input;
        platform.registerPrivate(SERVICE, f, 1);
        EventEnvelope request = new EventEnvelope().setTo(SERVICE)
                                    .setBody(TEXT).setTrace("1030", "TEST /api/async/request");
        Future<EventEnvelope> future = po.asyncRequest(request, 1500);
        future.onSuccess(event -> {
            platform.release(SERVICE);
            success.add(event);
        });
        EventEnvelope result = success.poll(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(200, result.getStatus());
        assertEquals(TEXT, result.getBody());
    }

    @Test
    void futureExceptionAsResult() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> completion = new ArrayBlockingQueue<>(1);
        int STATUS = 400;
        String ERROR = "some exception";
        String SERVICE = "hello.future.2";
        String TEXT = "hello world";
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        LambdaFunction f = (headers, input, instance) -> {
            throw new AppException(STATUS, ERROR);
        };
        platform.registerPrivate(SERVICE, f, 1);
        Future<EventEnvelope> future = po.asyncRequest(new EventEnvelope().setTo(SERVICE).setBody(TEXT), 5000);
        future.onSuccess(event -> {
            platform.release(SERVICE);
            completion.add(event);
        });
        EventEnvelope result = completion.poll(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(STATUS, result.getStatus());
        assertEquals(ERROR, result.getBody());
    }

    @Test
    void asyncForkJoinTest() throws IOException, InterruptedException {
        final BlockingQueue<List<EventEnvelope>> success = new ArrayBlockingQueue<>(1);
        final String from = "unit.test";
        final String traceId = "1020";
        final String tracePath = "TEST /async/fork-n-join";
        final String SERVICE = "hello.future.3";
        final String TEXT = "hello world";
        final int PARALLEL_INSTANCES = 5;
        final Platform platform = Platform.getInstance();
        final EventEmitter po = EventEmitter.getInstance();
        final LambdaFunction f1 = (headers, input, instance) -> input;
        platform.registerPrivate(SERVICE, f1, PARALLEL_INSTANCES);
        List<EventEnvelope> requests = new ArrayList<>();
        for (int i=1; i < PARALLEL_INSTANCES + 1; i++) {
            EventEnvelope req = new EventEnvelope().setTo(SERVICE).setBody(TEXT + "." + i)
                                    .setFrom(from).setTrace(traceId, tracePath);
            requests.add(req);
        }
        Future<List<EventEnvelope>> future = po.asyncRequest(requests, 1500);
        future.onSuccess(event -> {
            platform.release(SERVICE);
            success.add(event);
        });
        List<EventEnvelope> result = success.poll(5, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(PARALLEL_INSTANCES, result.size());
        for (EventEnvelope r: result) {
            assertInstanceOf(String.class, r.getBody());
            String text = (String) r.getBody();
            assertTrue(text.startsWith(TEXT));
            log.info("Received response #{} {} - {}", r.getCorrelationId(), r.getId(), text);
        }
    }

    @Test
    void asyncForkJoinTimeoutTest() throws IOException, InterruptedException {
        final long TIMEOUT = 500;
        final BlockingQueue<Throwable> exception = new ArrayBlockingQueue<>(1);
        final String SERVICE = "hello.future.4";
        final String TEXT = "hello world";
        final int PARALLEL_INSTANCES = 5;
        final Platform platform = Platform.getInstance();
        final EventEmitter po = EventEmitter.getInstance();
        final LambdaFunction f1 = (headers, input, instance) -> {
            log.info("Received fork-n-join event {}, {}", headers, input);
            return input;
        };
        platform.registerPrivate(SERVICE, f1, PARALLEL_INSTANCES);
        List<EventEnvelope> requests = new ArrayList<>();
        for (int i=1; i <= PARALLEL_INSTANCES; i++) {
            requests.add(new EventEnvelope().setTo(SERVICE).setBody(TEXT + "." + i)
                            .setHeader("timeout_exception", true)
                            .setHeader("seq", i));
        }
        requests.add(new EventEnvelope().setTo("hello.world").setBody(2));
        Future<List<EventEnvelope>> future = po.asyncRequest(requests, TIMEOUT, true);
        future.onFailure(exception::add);
        Throwable e = exception.poll(5, TimeUnit.SECONDS);
        assertNotNull(e);
        assertEquals(TimeoutException.class, e.getClass());
        assertEquals("Timeout for "+TIMEOUT+" ms", e.getMessage());
        platform.release(SERVICE);
    }

    @Test
    void asyncForkJoinPartialResultTest() throws IOException, InterruptedException {
        final long TIMEOUT = 800;
        final BlockingQueue<List<EventEnvelope>> result = new ArrayBlockingQueue<>(1);
        final String SERVICE = "hello.future.5";
        final String TEXT = "hello world";
        final int PARALLEL_INSTANCES = 5;
        final Platform platform = Platform.getInstance();
        final EventEmitter po = EventEmitter.getInstance();
        final LambdaFunction f1 = (headers, input, instance) -> {
            log.info("Received event {}, {}", headers, input);
            return input;
        };
        platform.registerPrivate(SERVICE, f1, PARALLEL_INSTANCES);
        List<EventEnvelope> requests = new ArrayList<>();
        for (int i=1; i <= PARALLEL_INSTANCES; i++) {
            requests.add(new EventEnvelope().setTo(SERVICE).setBody(TEXT + "." + i)
                    .setHeader("partial_result", true)
                    .setHeader("seq", i));
        }
        // hello.world will make an artificial delay of one second so that it will not be included in the result set.
        requests.add(new EventEnvelope().setTo("hello.world").setBody(2));
        Future<List<EventEnvelope>> future = po.asyncRequest(requests, TIMEOUT, false);
        future.onSuccess(result::add);
        List<EventEnvelope> responses = result.poll(10, TimeUnit.SECONDS);
        assert responses != null;
        assertEquals(PARALLEL_INSTANCES, responses.size());
        for (EventEnvelope evt: responses) {
            assertNotNull(evt.getBody());
            assertTrue(evt.getBody().toString().startsWith(TEXT));
        }
        platform.release(SERVICE);
    }

    @SuppressWarnings("unchecked")
    @Test
    void nonBlockingRpcTest() throws IOException, InterruptedException, ExecutionException {
        final PostOffice po = new PostOffice("unit.test", "10000", "POST /api/slow/rpc");
        final String HELLO_WORLD = "hello world";
        // the target service will simulate a one-second delay
        EventEnvelope request = new EventEnvelope().setTo("slow.rpc.function").setBody(HELLO_WORLD);
        EventEnvelope response = po.request(request, 5000).get();
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals(2, map.getElement("body"));
        assertEquals(HELLO_WORLD, map.getElement("headers.body"));
    }

    @Test
    void nonBlockingRpcTimeoutTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final String HELLO_WORLD = "hello world";
        EventEnvelope request = new EventEnvelope().setTo("slow.rpc.function").setBody(HELLO_WORLD)
                .setHeader("timeout", 500)
                .setTrace("10001", "/api/non-blocking/rpc").setFrom("unit.test");
        /*
         * Since it is the nested service that throws TimeoutException,
         * the exception is transported as a regular response event.
         */
        EventEmitter.getInstance().asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(408, response.getStatus());
        assertEquals("Timeout for 500 ms", response.getError());
    }

    @SuppressWarnings("unchecked")
    @Test
    void nonBlockingForkAndJoinTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final String FORK_N_JOIN = "fork-n-join";
        final String HELLO_WORLD = "hello world";
        EventEnvelope request = new EventEnvelope().setTo("long.running.rpc").setBody(HELLO_WORLD)
                .setFrom("unit.test")
                .setHeader(FORK_N_JOIN, true)
                .setHeader("timeout", 2000)
                .setTrace("20000", "/api/non-blocking/fork-n-join");
        EventEmitter.getInstance().asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals(4, map.getMap().size());
        assertEquals(0, map.getElement("cid-0.body"));
        assertEquals(HELLO_WORLD, map.getElement("cid-0.headers.body"));
        assertEquals("true", map.getElement("cid-0.headers." + FORK_N_JOIN));
        assertEquals(1, map.getElement("cid-1.body"));
        assertEquals(HELLO_WORLD, map.getElement("cid-1.headers.body"));
        assertEquals("true", map.getElement("cid-1.headers." + FORK_N_JOIN));
        assertEquals(2, map.getElement("cid-2.body"));
        assertEquals(HELLO_WORLD, map.getElement("cid-2.headers.body"));
        assertEquals("true", map.getElement("cid-2.headers." + FORK_N_JOIN));
        assertEquals(3, map.getElement("cid-3.body"));
        assertEquals(HELLO_WORLD, map.getElement("cid-3.headers.body"));
        assertEquals("true", map.getElement("cid-3.headers." + FORK_N_JOIN));
    }

    @SuppressWarnings("unchecked")
    @Test
    void nonBlockingForkAndJoinTimeoutTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final String FORK_N_JOIN = "fork-n-join";
        final String HELLO_WORLD = "hello world";
        EventEnvelope request = new EventEnvelope().setTo("long.running.rpc").setBody(HELLO_WORLD)
                .setFrom("unit.test")
                .setHeader(FORK_N_JOIN, true)
                .setHeader("timeout", 500)
                .setTrace("20000", "/api/non-blocking/fork-n-join");
        EventEmitter.getInstance().asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        // since 2 requests will time out with artificial delay of one second, there will only be 2 responses.
        assertEquals(2, map.getMap().size());
        assertEquals(1, map.getElement("cid-1.body"));
        assertEquals(HELLO_WORLD, map.getElement("cid-1.headers.body"));
        assertEquals("true", map.getElement("cid-1.headers." + FORK_N_JOIN));
        assertEquals(3, map.getElement("cid-3.body"));
        assertEquals(HELLO_WORLD, map.getElement("cid-3.headers.body"));
        assertEquals("true", map.getElement("cid-3.headers." + FORK_N_JOIN));
    }

    @SuppressWarnings("unchecked")
    @Test
    void multilevelTrace() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final String ROUTE_ONE = "hello.level.1";
        final String ROUTE_TWO = "hello.level.2";
        final String TRACE_ID = "cid-123456";
        final String TRACE_PATH = "GET /api/hello/world";
        Platform platform = Platform.getInstance();
        LambdaFunction tier2 = (headers, input, instance) -> {
            PostOffice po = new PostOffice(headers, instance);
            assertEquals(ROUTE_TWO, po.getRoute());
            assertEquals(TRACE_ID, po.getTraceId());
            // annotations are local to a service and should not be transported to the next service
            assertTrue(po.getTrace().annotations.isEmpty());
            return po.getTraceId();
        };
        platform.register(ROUTE_TWO, tier2, 1);
        // test tracing to 2 levels
        String testMessage = "some message";
        EventEnvelope event = new EventEnvelope();
        event.setTo(ROUTE_ONE).setHeader("hello", "world").setBody(testMessage);
        event.setTrace(TRACE_ID, TRACE_PATH).setFrom("unit.test");
        EventEmitter po = EventEmitter.getInstance();
        po.asyncRequest(event, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(HashMap.class, response.getBody().getClass());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("world", map.getElement("headers.hello"));
        assertEquals(testMessage, map.getElement("body"));
        assertEquals(TRACE_ID, map.getElement("trace_id"));
        assertEquals(TRACE_PATH, map.getElement("trace_path"));
        assertEquals(ROUTE_ONE, map.getElement("route_one"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void routeSubstitution() throws IOException, InterruptedException {
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
    void healthTest() throws IOException, InterruptedException {
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
    void infoTest() throws IOException, InterruptedException {
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
    void libTest() throws IOException, InterruptedException {
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
    void infoRouteTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String MY_FUNCTION = "my.test.function";
        String ANOTHER_FUNCTION = "another.function";
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
        assertTrue(routes.contains(MY_FUNCTION));
        assertTrue(routes.contains(ANOTHER_FUNCTION));
    }

    @Test
    void livenessProbeTest() throws IOException, InterruptedException {
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
    void envTest() throws IOException, InterruptedException {
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
    void envelopeAsResponseTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String TARGET = "test.route.1";
        String MESSAGE = "hello world";
        EventEmitter po = EventEmitter.getInstance();
        Platform.getInstance().register(TARGET, new EventEnvelopeReader(), 1);
        EventEnvelope request = new EventEnvelope().setTo(TARGET).setBody(MESSAGE);
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(MESSAGE, response.getBody());
    }

    @Test
    void threadPoolTest() throws IOException, InterruptedException {
        final int CYCLES = 200;
        final int WORKER_POOL = 50;
        final ConcurrentMap<Long, Boolean> threads = new ConcurrentHashMap<>();
        final BlockingQueue<Boolean> bench = new ArrayBlockingQueue<>(1);
        final AtomicInteger counter = new AtomicInteger(0);
        final AtomicLong last = new AtomicLong(0);
        String MULTI_CORES = "multi.cores";
        LambdaFunction f= (headers, input, instance) -> {
            int n = counter.incrementAndGet();
            long id = Thread.currentThread().threadId();
            log.debug("Instance #{}, count={}, thread #{} {}", instance, n, id, input);
            threads.put(id, true);
            if (n == CYCLES) {
                last.set(System.currentTimeMillis());
                bench.add(true);
            }
            return true;
        };
        Platform.getInstance().registerPrivate(MULTI_CORES, f, WORKER_POOL);
        EventEmitter po = EventEmitter.getInstance();
        long t1 = System.currentTimeMillis();
        for (int i=0; i < CYCLES; i++) {
            po.send(MULTI_CORES, "hello world");
        }
        Boolean result = bench.poll(10, TimeUnit.SECONDS);
        long diff = last.get() - t1;
        log.info("{} cycles done? {}, {} workers consumed {} threads in {} ms",
                CYCLES, result, WORKER_POOL, threads.size(), diff);
        assertTrue(counter.get() > WORKER_POOL);
    }

    @Test
    void testCallBackEventHandler() throws IOException, InterruptedException {
        final BlockingQueue<Object> bench = new ArrayBlockingQueue<>(1);
        String TRACE_ID = "10000";
        String HELLO = "hello";
        String POJO_HAPPY_CASE = "pojo.happy.case.1";
        String SIMPLE_CALLBACK = "simple.callback.1";
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        LambdaFunction f = (headers, input, instance) -> {
            PoJo pojo = new PoJo();
            pojo.setName((String) input);
            return pojo;
        };
        platform.registerPrivate(POJO_HAPPY_CASE, f, 1);
        platform.registerPrivate(SIMPLE_CALLBACK, new SimpleCallback(bench, TRACE_ID), 1);
        po.send(new EventEnvelope().setTo(POJO_HAPPY_CASE).setReplyTo(SIMPLE_CALLBACK).setBody(HELLO)
                        .setFrom("unit.test").setTrace(TRACE_ID, "TEST /callback"));
        Object result = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(PoJo.class, result.getClass());
        assertEquals(HELLO, ((PoJo) result).getName());
        platform.release(POJO_HAPPY_CASE);
        platform.release(SIMPLE_CALLBACK);
    }

    @Test
    void testMapToPoJoCasting() throws IOException, InterruptedException {
        final BlockingQueue<Object> bench = new ArrayBlockingQueue<>(1);
        var TRACE_ID = "30000";
        var TEXT = "test";
        var HELLO = Map.of("name", TEXT);
        String POJO_SUCCESS_CASE = "pojo.success.case";
        String SIMPLE_CALLBACK = "simple.callback.2";
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        LambdaFunction f = (headers, input, instance) -> HELLO;
        platform.registerPrivate(POJO_SUCCESS_CASE, f, 1);
        platform.registerPrivate(SIMPLE_CALLBACK, new SimpleCallback(bench, TRACE_ID), 1);
        po.send(new EventEnvelope().setTo(POJO_SUCCESS_CASE).setReplyTo(SIMPLE_CALLBACK).setBody(HELLO)
                .setTrace(TRACE_ID, "TEST /best/effort/mapping"));
        Object result = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(PoJo.class, result.getClass());
        PoJo restored = (PoJo) result;
        assertEquals(TEXT, restored.getName());
        platform.release(POJO_SUCCESS_CASE);
        platform.release(SIMPLE_CALLBACK);
    }

    @Test
    void testCallBackCastingException() throws IOException, InterruptedException {
        final BlockingQueue<Object> bench = new ArrayBlockingQueue<>(1);
        String TRACE_ID = "30000";
        String HELLO = "hello";
        String POJO_ERROR_CASE = "pojo.error.case.3";
        String SIMPLE_CALLBACK = "simple.callback.3";
        Platform platform = Platform.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        LambdaFunction f = (headers, input, instance) -> HELLO;
        platform.registerPrivate(POJO_ERROR_CASE, f, 1);
        platform.registerPrivate(SIMPLE_CALLBACK, new SimpleCallback(bench, TRACE_ID), 1);
        po.send(new EventEnvelope().setTo(POJO_ERROR_CASE).setReplyTo(SIMPLE_CALLBACK).setBody(HELLO)
                .setTrace(TRACE_ID, "TEST /cast/error"));
        Object result = bench.poll(10, TimeUnit.SECONDS);
        assertNotNull(result);
        assertEquals(AppException.class, result.getClass());
        AppException ex = (AppException) result;
        assertEquals(500, ex.getStatus());
        assertEquals("class java.lang.String cannot be cast to class org.platformlambda.core.models.PoJo",
                ex.getMessage());
        platform.release(POJO_ERROR_CASE);
        platform.release(SIMPLE_CALLBACK);
    }

    @Test
    void testInputObjectMapping() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String TRACE_ID = "101010";
        String TRACE_PATH = "TEST /api/hello/input/mapping";
        String AUTO_MAPPING = "hello.input.mapping";
        String HELLO_WORLD = "hello world";
        Date now = new Date();
        LocalDateTime time = Instant.ofEpochMilli(now.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        EventEmitter po = EventEmitter.getInstance();
        // prove that two PoJo are compatible when sending data fields that intersect
        PoJoSubset minimalData = new PoJoSubset();
        minimalData.setName(HELLO_WORLD);
        minimalData.setDate(now);
        minimalData.setLocalDateTime(time);
        EventEnvelope request = new EventEnvelope().setTo(AUTO_MAPPING).setBody(minimalData)
                                .setTrace(TRACE_ID,TRACE_PATH).setFrom("unit.test");
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        // for security, automatic PoJo class restore is disabled
        assertEquals(HashMap.class, response.getBody().getClass());
        // original PoJo class name is transported by the event envelope
        assertEquals(PoJo.class.getName(), response.getType());
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(response.getBody(), PoJo.class);
        assertEquals(now, pojo.getDate());
        assertEquals(time, pojo.getLocalDateTime());
        assertEquals(HELLO_WORLD, pojo.getName());
        // default values in PoJo
        assertEquals(0, pojo.getNumber());
        assertEquals(0L, pojo.getLongNumber());
        // the demo function is designed to return its function execution types
        assertEquals("true", response.getHeader("coroutine"));
        assertEquals("false", response.getHeader("suspend"));
        assertEquals("false", response.getHeader("interceptor"));
        assertEquals("true", response.getHeader("tracing"));
        // the demo function will also echo the READ only route, trace ID and path
        assertEquals(AUTO_MAPPING, response.getHeader("route"));
        assertEquals(TRACE_ID, response.getHeader("trace_id"));
        assertEquals(TRACE_PATH, response.getHeader("trace_path"));
        // the system will filter out reserved metadata - my_route, my_trace_id, my_trace_path
        assertNull(response.getHeader("my_route"));
        assertNull(response.getHeader("my_trace_id"));
        assertNull(response.getHeader("my_trace_path"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPrimitiveTransport() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String HELLO_WORLD = "hello.world";
        int number = 101;
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo(HELLO_WORLD).setBody(number);
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        Map<String, Object> map = (Map<String, Object>) response.getBody();
        assertEquals(number, map.get("body"));
        Date now = new Date();
        request = new EventEnvelope().setTo(HELLO_WORLD).setBody(now);
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        map = (Map<String, Object>) response.getBody();
        assertEquals(util.date2str(now), map.get("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCustomSerializer() throws IOException, ExecutionException, InterruptedException {
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
        EventEnvelope result1 = po.request(event1, 5000).get();
        assertInstanceOf(Map.class, result1.getBody());
        Map<String, Object> data1 = (Map<String, Object>) result1.getBody();
        assertEquals(pojo.fullName, data1.get("fullName"));
        assertEquals(pojo.address, data1.get("address"));
        assertEquals(pojo.telephone, data1.get("telephone"));
        SimplePoJo responsePoJo1 = po.getEventBodyAsPoJo(result1, SimplePoJo.class);
        assertEquals(pojo.fullName, responsePoJo1.fullName);
        assertEquals(pojo.address, responsePoJo1.address);
        assertEquals(pojo.telephone, responsePoJo1.telephone);
        // result event envelope is encoded as Map ("M") because the custom serializer is external to the function
        assertEquals("M", result1.getType());
        // test kotlin user function
        var event2 = new EventEnvelope().setTo("custom.serializer.service.kotlin");
        po.setEventBodyAsPoJo(event2, pojo);
        EventEnvelope result2 = po.request(event2, 5000).get();
        assertInstanceOf(Map.class, result2.getBody());
        Map<String, Object> data2 = (Map<String, Object>) result2.getBody();
        assertEquals(pojo.fullName, data2.get("fullName"));
        assertEquals(pojo.address, data2.get("address"));
        assertEquals(pojo.telephone, data2.get("telephone"));
        SimplePoJo responsePoJo2 = po.getEventBodyAsPoJo(result2, SimplePoJo.class);
        assertEquals(pojo.fullName, responsePoJo2.fullName);
        assertEquals(pojo.address, responsePoJo2.address);
        assertEquals(pojo.telephone, responsePoJo2.telephone);
    }

    @Test
    void testInputAsListOfPoJoJava() throws IOException, ExecutionException, InterruptedException {
        testInputAsListOfPoJo("input.list.of.pojo.java", "10201");
    }

    @Test
    void testInputAsListOfPoJoKotlin() throws IOException, ExecutionException, InterruptedException {
        testInputAsListOfPoJo("input.list.of.pojo.kotlin", "10202");
    }

    @SuppressWarnings("unchecked")
    private void testInputAsListOfPoJo(String route, String traceId) throws IOException, ExecutionException, InterruptedException {
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
        var result = po.request(event, 5000).get();
        assertInstanceOf(Map.class, result.getBody());
        var map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals(pojo1.getName(), map.getElement("names[0]"));
        assertEquals(pojo2.getName(), map.getElement("names[1]"));
        assertEquals("null", map.getElement("names[2]"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testInputAsPoJoUntyped() throws IOException, ExecutionException, InterruptedException {
        PostOffice po = new PostOffice("untyped.pojo.test", "10300", "GET /untyped/pojo");
        PoJo pojo = new PoJo();
        pojo.setName("hello");
        // prove that list of pojo can be restored using the inputPojoClass in the PreLoad annotation
        var event = new EventEnvelope().setTo("input.pojo.untyped").setBody(pojo);
        var result = po.request(event, 5000).get();
        assertInstanceOf(Map.class, result.getBody());
        System.out.println(result.getBody());
        var map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals(pojo.getName(), map.getElement("map.name"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testInputAsPoJoListUntyped() throws IOException, ExecutionException, InterruptedException {
        PostOffice po = new PostOffice("untyped.pojo.test", "10301", "GET /untyped/pojo");
        PoJo pojo = new PoJo();
        pojo.setName("hello");
        // prove that list of pojo can be restored using the inputPojoClass in the PreLoad annotation
        var event = new EventEnvelope().setTo("input.pojo.untyped").setBody(List.of(pojo));
        var result = po.request(event, 5000).get();
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
