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

package com.accenture.flows;

import com.accenture.adapters.FlowExecutor;
import com.accenture.mock.EventScriptMock;
import com.accenture.models.PoJo;
import com.accenture.setup.TestBase;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FlowTests extends TestBase {
    private static final Logger log = LoggerFactory.getLogger(FlowTests.class);
    private static final String HTTP_CLIENT = "async.http.request";

    @SuppressWarnings("unchecked")
    @Test
    void httpClientByConfigTest() throws ExecutionException, InterruptedException {
        final long timeout = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("POST")
                .setHeader("accept", "application/json")
                .setHeader("content-type", "application/json")
                .setUrl("/api/http/client/by/config/test");
        request.setBody(Map.of("hello", "world"));
        PostOffice po = new PostOffice("unit.test", "10", "TEST /http/client/by/config");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, timeout).get();
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("test", map.getElement("parameters.path.demo"));
        assertEquals("world", map.getElement("parameters.query.hello"));
        assertInstanceOf(Map.class, map.getElement("body"));
        assertEquals(Map.of("hello", "world"), map.getElement("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void noSuchFlowTest() throws ExecutionException, InterruptedException {
        final long timeout = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json")
                .setUrl("/api/no-such-flow");
        PostOffice po = new PostOffice("unit.test", "2000", "TEST /no/such/flow");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, timeout).get();
        assertInstanceOf(Map.class, result.getBody());
        assertEquals(500, result.getStatus());
        Map<String, Object> data = (Map<String, Object>) result.getBody();
        assertEquals("Flow no-such-flow not found", data.get("message"));
    }

    @Test
    void externalStateMachineTest() throws InterruptedException, ExecutionException {
        executeExtStateMachine("/api/ext/state/");
    }

    @Test
    void externalStateMachineFlowTest() throws InterruptedException, ExecutionException {
        executeExtStateMachine("/api/ext/state/flow/");
    }

    @SuppressWarnings("unchecked")
    void executeExtStateMachine(String uriPath) throws InterruptedException, ExecutionException {
        final long timeout = 8000;
        String placeholder = "test";
        var payload = Map.of("hello", "world");
        AsyncHttpRequest request1 = new AsyncHttpRequest();
        request1.setTargetHost(HOST).setMethod("PUT").setHeader("accept", "application/json")
                .setHeader("content-type", "application/json").setBody(payload);
        request1.setUrl(uriPath+placeholder);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req1 = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request1);
        EventEnvelope res1 = po.request(req1, timeout).get();
        assert res1 != null;
        assertInstanceOf(Map.class, res1.getBody());
        Map<String, Object> result1 = (Map<String, Object>) res1.getBody();
        assertEquals("world", result1.get("hello"));
        /*
         * We must assume that external state machine is eventual consistent.
         * Therefore, result may not be immediately available.
         *
         * However, for unit test, we set the external state machine function to have a single worker instance
         * so that the GET request will wait until the PUT request is done, thus returning result correctly.
         */
        AsyncHttpRequest request2 = new AsyncHttpRequest();
        request2.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request2.setUrl("/api/ext/state/"+placeholder);
        EventEnvelope req2 = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request2);
        EventEnvelope res2 = po.request(req2, timeout).get();
        assert res2 != null;
        assertInstanceOf(Map.class, res2.getBody());
        Map<String, Object> result2 = (Map<String, Object>) res2.getBody();
        assertEquals(placeholder, result2.get("user"));
        assertEquals(payload, result2.get("payload"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void typeMatchingTest() throws ExecutionException, InterruptedException {
        Utility util = Utility.getInstance();
        final String helloWorld = "hello world";
        final String hello = "hello";
        final String world = "world";
        final byte[] helloWorldBytes = util.getUTF(helloWorld);
        final String B64_TEXT = util.bytesToBase64(helloWorldBytes);
        final long timeout = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json")
                .setUrl("/api/type/matching");
        PostOffice po = new PostOffice("unit.test", "2000", "TEST /type/matching");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, timeout).get();
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap mm = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals(B64_TEXT, mm.getElement("b64"));
        assertArrayEquals(helloWorldBytes, getByteArrayFromEncodedIntegers((List<Integer>) mm.getElement("binary")));
        Map<String, Object> m1 = Map.of("b64", B64_TEXT, "text", helloWorld);
        Map<String, Object> m2 = SimpleMapper.getInstance().getMapper().readValue(mm.getElement("json"), Map.class);
        equalsMap(m1, m2);
        assertEquals(hello, mm.getElement("substring"));
        byte[] bson = getByteArrayFromEncodedIntegers((List<Integer>) mm.getElement("bson"));
        Map<String, Object> m3 = SimpleMapper.getInstance().getMapper().readValue(bson, Map.class);
        equalsMap(m1, m3);
        assertEquals(hello, mm.getElement("source.substring"));
        assertEquals(world, mm.getElement("source.substring-2"));
        assertArrayEquals(helloWorldBytes,
                getByteArrayFromEncodedIntegers((List<Integer>) mm.getElement("source.keep-as-binary")));
        assertEquals(helloWorld, mm.getElement("source.no-change"));
        assertArrayEquals(helloWorldBytes,
                getByteArrayFromEncodedIntegers((List<Integer>) mm.getElement("source.binary")));
        assertArrayEquals(helloWorldBytes,
                getByteArrayFromEncodedIntegers((List<Integer>) mm.getElement("source.bytes")));
        assertEquals(helloWorld, mm.getElement("source.out-of-bound"));
        assertEquals(helloWorld, mm.getElement("source.invalid-substring"));
        assertEquals(helloWorld, mm.getElement("source.text"));
        typeMatchingAssertions(mm);
    }

    private void typeMatchingAssertions(MultiLevelMap mm) {
        assertEquals(true, mm.getElement("positive"));
        assertEquals(false, mm.getElement("negative"));
        assertEquals(false, mm.getElement("boolean-text"));
        assertEquals(true, mm.getElement("boolean-text-true"));
        assertEquals(true, mm.getElement("is-null"));
        assertEquals(true, mm.getElement("null"));
        assertEquals(false, mm.getElement("has-file"));
        assertEquals(100, mm.getElement("integer"));
        assertEquals(101, mm.getElement("long"));
        assertEquals(100.01, mm.getElement("float"));
        assertEquals(101.01, mm.getElement("double"));
        /*
         * after passing through an HTTP endpoint, JSON string serialization is applied.
         * GSON turns numbers into integer as much as possible
         * and floating point numbers into double.
         */
        assertInstanceOf(Integer.class, mm.getElement("integer"));
        assertInstanceOf(Integer.class, mm.getElement("long"));
        assertInstanceOf(Double.class, mm.getElement("float"));
        assertInstanceOf(Double.class, mm.getElement("double"));
        /*
         * test boolean and/or feature
         *
         * true and false = false
         * true or false = true
         */
        assertEquals(false, mm.getElement("and"));
        assertEquals(true, mm.getElement("or"));
        /*
         * test failed boolean mapping
         *
         * The following will return true because "nothing" does not belong to the state machine:
         * 'model.positive:and(nothing) -> output.body.nothing'
         */
        assertEquals(true, mm.getElement("nothing"));
        // type matching to get size of a list
        assertEquals(3, mm.getElement("source.list_size"));
        // type matching to get length of the number 1000
        assertEquals(4, mm.getElement("source.number_length"));
    }

    private void equalsMap(Map<String, Object> a, Map<String, Object> b) {
        assertEquals(a.size(), b.size());
        for (String k : a.keySet()) {
            assertEquals(a.get(k), b.get(k));
        }
    }

    private byte[] getByteArrayFromEncodedIntegers(List<Integer> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Integer i: items) {
            out.write(i);
        }
        return out.toByteArray();
    }

    @Test
    void bodyTest() throws ExecutionException, InterruptedException {
        final long timeout = 8000;
        final String hello = "hello world";
        final String valueA = "A";
        final String valueB = "B";
        final int seq = 1;
        Map<String, Object> pojoBody = new HashMap<>();
        pojoBody.put("user", hello);
        pojoBody.put("sequence", seq);
        pojoBody.put("date", new Date());
        pojoBody.put("key1", valueA);
        pojoBody.put("key2", valueB);
        // put the pojo data structure into a holder to test "input data mapping" feature
        Map<String, Object> holder = new HashMap<>();
        holder.put("pojoHolder", pojoBody);
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("POST")
                .setHeader("accept", "application/json")
                .setHeader("content-type", "application/json")
                .setBody(holder)
                .setUrl("/api/body/test");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, timeout).get();
        // Take return value as PoJo
        assertInstanceOf(Map.class, result.getBody());
        PoJo restored = result.getBody(PoJo.class);
        assertEquals(hello, restored.user);
        assertEquals(seq, restored.sequence);
        assertEquals(valueA, restored.key1);
        assertEquals(valueB, restored.key2);
        Utility util = Utility.getInstance();
        // verify that result contains headers set by "input data mapping" earlier
        assertEquals(seq, util.str2int(result.getHeader("X-Sequence")));
        assertEquals("AAA", result.getHeader("X-Tag"));
        assertEquals("async-http-client", result.getHeader("x-agent"));
        assertEquals("com.accenture.models.PoJo", result.getHeader("x-datatype"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void headerTest() throws ExecutionException, InterruptedException {
        final long timeout = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json")
                .setUrl("/api/header/test");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, timeout).get();
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        // verify that input headers are mapped to the function's input body
        assertEquals("header-test", body.get("x-flow-id"));
        assertEquals("async-http-client", body.get("user-agent"));
        assertEquals("application/json", body.get("accept"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void fileVaultTest() throws ExecutionException, InterruptedException {
        final long timeout = 8000;
        final String hello = """
                { "hello": "world" }
                """;
        File f1 = new File("/tmp/temp-test-input.txt");
        Utility util = Utility.getInstance();
        util.str2file(f1, hello);
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json").setUrl("/api/file/vault");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, timeout).get();
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap mm = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertInstanceOf(Map.class, mm.getElement("json"));
        assertEquals("world", mm.getElement("json.hello"));
        // "output data mapping" will pass the input classpath file as output body
        InputStream in = this.getClass().getResourceAsStream("/files/hello.txt");
        String resourceContent = util.stream2str(in);
        assertEquals(resourceContent, mm.getElement("text"));
        assertEquals("application/json", result.getHeader("content-type"));
        assertEquals(200, result.getStatus());
        File f2 = new File("/tmp/temp-test-output.txt");
        assertTrue(f2.exists());
        String text = util.file2str(f2);
        assertEquals(hello, text);
        File f3 = new File("/tmp/temp-test-match.txt");
        assertTrue(f3.exists());
        String matched = util.file2str(f3);
        assertEquals("true", matched);
        File f4 = new File("/tmp/temp-test-binary.txt");
        assertTrue(f4.exists());
        String binary = util.file2str(f4);
        assertEquals("binary", binary);
        File f5 = new File("/tmp/temp-test-list.json");
        String value = util.file2str(f5);
        assertTrue(value.startsWith("["));
        assertTrue(value.endsWith("]"));
        var json = SimpleMapper.getInstance().getMapper().readValue(value, List.class);
        assertEquals(List.of("hello", "world"), json);
        // f1 will be deleted by the output data mapping 'model.none -> file(/tmp/temp-test-input.txt)'
        f2.deleteOnExit();
        f3.deleteOnExit();
        f4.deleteOnExit();
        f5.deleteOnExit();
    }

    @SuppressWarnings("unchecked")
    @Test
    void circuitBreakerRetryTest() throws ExecutionException, InterruptedException {
        final long timeout = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json").setUrl("/api/circuit/breaker/2");
        PostOffice po = new PostOffice("unit.test", "100100", "TEST /circuit/breaker/retry");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, timeout).get();
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> output = (Map<String, Object>) result.getBody();
        assertEquals(2, output.get("attempt"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void circuitBreakerAbortTest() throws ExecutionException, InterruptedException {
        final long timeout = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        // the max_attempt is 2 for the circuit breaker and thus this will break
        request.setUrl("/api/circuit/breaker/3");
        PostOffice po = new PostOffice("unit.test", "100101", "TEST /circuit/breaker/abort");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, timeout).get();
        assertEquals(400, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> output = (Map<String, Object>) result.getBody();
        assertEquals("error", output.get("type"));
        assertEquals(400, output.get("status"));
        assertEquals("Demo Exception", output.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void resilienceHandlerTest() throws ExecutionException, InterruptedException {
        // delete control files before running test
        File f1 = new File("/tmp/resilience/cumulative");
        File f2 = new File("/tmp/resilience/backoff");
        if (f1.exists()) {
            f1.delete();
        }
        if (f2.exists()) {
            f2.delete();
        }
        // run test
        final PostOffice po = new PostOffice("unit.test", "100102", "TEST /resilience");
        final long timeout = 8000;
        // Create condition for backoff by forcing it to throw exception over the backoff_trigger (threshold of 3)
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setUrl("/api/resilience");
        request.setQueryParameter("exception", 400);
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope first = po.request(req, timeout).get();
        // after 3 attempts, it aborts and returns error 400
        assertEquals(400, first.getStatus());
        EventEnvelope second = po.request(req, timeout).get();
        // the system will enter into backoff mode when the cumulative attempt reaches 5
        assertEquals(503, second.getStatus());
        assertInstanceOf(Map.class, second.getBody());
        Map<String, Object> output = (Map<String, Object>) second.getBody();
        assertEquals(Map.of("type", "error",
                "message", "Service temporarily not available - please try again in 2 seconds",
                "status", 503), output);
        // Let the backoff period expires
        log.info("Making request during backoff period will throw exception 503");
        AsyncHttpRequest requestDuringBackoff = new AsyncHttpRequest();
        requestDuringBackoff.setTargetHost(HOST).setMethod("GET").setUrl("/api/resilience");
        requestDuringBackoff.setQueryParameter("exception", 400);
        EventEnvelope requestBo = new EventEnvelope().setTo(HTTP_CLIENT).setBody(requestDuringBackoff);
        EventEnvelope resultBo = po.request(requestBo, timeout).get();
        assertEquals(503, resultBo.getStatus());
        assertInstanceOf(Map.class, resultBo.getBody());
        Map<String, Object> outputBo = (Map<String, Object>) resultBo.getBody();
        assertTrue(outputBo.containsKey("message"));
        assertEquals(503, outputBo.get("status"));
        assertEquals("error", outputBo.get("type"));
        var message = outputBo.get("message").toString();
        assertTrue(message.startsWith("Service temporarily not available"));
        log.info("Waiting for backoff period to expire");
        Thread.sleep(2000);
        // Test alternative path using 'text(401, 403-404) -> reroute'
        // Let exception simulator to throw HTTP-401
        AsyncHttpRequest request1 = new AsyncHttpRequest();
        request1.setTargetHost(HOST).setMethod("GET").setUrl("/api/resilience");
        request1.setQueryParameter("exception", 401);
        EventEnvelope req1 = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request1);
        EventEnvelope result1 = po.request(req1, timeout).get();
        assertEquals(200, result1.getStatus());
        assertInstanceOf(Map.class, result1.getBody());
        Map<String, Object> output1 = (Map<String, Object>) result1.getBody();
        assertEquals(Map.of("path", "alternative"), output1);
        // Try again with HTTP-403
        AsyncHttpRequest request2 = new AsyncHttpRequest();
        request2.setTargetHost(HOST).setMethod("GET").setUrl("/api/resilience");
        request2.setQueryParameter("exception", 403);
        EventEnvelope req2 = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request2);
        EventEnvelope result2 = po.request(req2, timeout).get();
        assertEquals(200, result2.getStatus());
        assertInstanceOf(Map.class, result2.getBody());
        Map<String, Object> output2 = (Map<String, Object>) result2.getBody();
        assertEquals(Map.of("path", "alternative"), output2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void greetingTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        final String traceId = "1001";
        String placeholder = "12345";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/greetings/"+placeholder);
        PostOffice po = new PostOffice("unit.test", traceId, "TEST /greeting");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(placeholder, result.get("user"));
        assertEquals(getAppName(), result.get("name"));
        assertEquals("hello world", result.get("greeting"));
        assertEquals(true, result.get("positive"));
        assertTrue(result.containsKey("original"));
        Map<String, Object> original = (Map<String, Object>) result.get("original");
        assertEquals(201, res.getStatus());
        // output mapping 'input.header -> header' delivers the result EventEnvelope's headers
        assertEquals("test-header", res.getHeader("demo"));
        // output mapping 'header.demo -> output.header.x-demo' maps the original header "demo" to "x-demo"
        assertEquals("test-header", res.getHeader("x-demo"));
        greetingAssertions(placeholder, original, result);

    }

    private void greetingAssertions(String user, Map<String, Object> original, Map<String, Object> result) {
        final String traceId = "1001";
        final String greetings = "greetings";
        /*
         * serialization compresses numbers to long and float
         * if the number is not greater than MAX integer or float
         */
        assertEquals(Utility.getInstance().str2int(user), original.get("user_number"));
        assertEquals(12345, original.get("long_number"));
        assertEquals(12.345, original.get("float_number"));
        assertEquals(12.345, original.get("double_number"));
        assertEquals(true, original.get("boolean_value"));
        assertEquals(false, original.get("negate_value"));
        // double negate becomes true
        assertEquals(true, original.get("double_negate_value"));
        // test non-exist model variable in boolean null and uuid use cases
        assertEquals(true, original.get("none_is_true"));
        assertEquals(false, original.get("none_is_false"));
        assertNotNull(original.get("unique_id1"));
        assertNotNull(original.get("unique_id2"));
        assertNotEquals(original.get("unique_id1"), original.get("unique_id2"));
        assertEquals(original.get("unique_id2"), original.get("unique_id3"));
        // demonstrate string concatenation
        assertEquals("a b,c", original.get("concat_string"));
        // check environment variable substitution
        assertEquals(System.getenv("PATH"), original.get("path"));
        // check metadata for a flow
        assertEquals(traceId, original.get("trace_id"));
        assertEquals(greetings, original.get("flow_id"));
        // the "demo" key-value is collected from the input headers to the test function
        assertEquals("ok", result.get("demo1"));
        assertEquals(user, result.get("demo2"));
        // input mapping 'input.header -> header' relays all HTTP headers
        assertEquals("greetings", result.get("demo3"));
        // check map values
        String port = AppConfigReader.getInstance().getProperty("server.port");
        /*
         * Prove that map containing system properties or environment variables is resolved by the config system.
         * The mapping of "map(test.map)" should return the resolved key-values.
         */
        assertEquals(Map.of("hello", "world", "good", "day", "port", port), result.get("map1"));
        assertEquals(Map.of("test", "message", "direction", "right"), result.get("map2"));
        /*
         * The test case with map3 demonstrates that the key/value pairs from both application.properties
         * and application.yml * are merged into one map.  In the example below, map(test.map3) contains 3 entries.
         *
         * test.map3.hello=world
         * test.map3.ping=pong
         *
         * test.map3:
         *   good: day
         */
        assertEquals(Map.of("hello", "world", "ping", "pong", "good", "day"), result.get("map3"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void parentGreetingTest() throws InterruptedException, ExecutionException {
        final long timeout = 8000;
        String placeholder = "test";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/parent-greeting/"+placeholder);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope res = po.request(req, timeout).get();
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(placeholder, result.get("user"));
        assertEquals(getAppName(), result.get("name"));
        assertEquals("hello", result.get("hello"));
        assertEquals("hello world", result.get("greeting"));
        assertTrue(result.containsKey("original"));
        Map<String, Object> original = (Map<String, Object>) result.get("original");
        assertEquals(201, res.getStatus());
        assertEquals("test-header", res.getHeader("demo"));
        assertEquals("test-header", res.getHeader("x-demo"));
        assertEquals(12345, original.get("long_number"));
        assertEquals(12.345, original.get("float_number"));
        assertEquals(12.345, original.get("double_number"));
        assertEquals(true, original.get("boolean_value"));
        // the "demo" key-value is collected from the input headers to the test function
        assertEquals("ok", result.get("demo1"));
        assertEquals(placeholder, result.get("demo2"));
        // input mapping 'input.header -> header' relays all HTTP headers
        assertEquals("parent-greetings", result.get("demo3"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void missingSubFlow() throws InterruptedException, ExecutionException {
        final long timeout = 8000;
        String placeholder = "test";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/missing-flow/"+placeholder);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope res = po.request(req, timeout).get();
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(500, res.getStatus());
        assertEquals("flow://flow-not-found not defined", result.get("message"));
    }

    private String getAppName() {
        AppConfigReader config = AppConfigReader.getInstance();
        return config.getProperty("application.name");
    }

    @SuppressWarnings("unchecked")
    @Test
    void exceptionTest() throws InterruptedException, ExecutionException {
        final long timeout = 8000;
        String placeholder = "test";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/greetings/"+placeholder).setQueryParameter("ex", "403");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope res = po.request(req, timeout).get();
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(403, result.get("status"));
        assertEquals("just a test", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void exceptionLoopTest() throws InterruptedException, ExecutionException {
        final long timeout = 8000;
        String placeholder = "test";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/greetings/"+placeholder).setQueryParameter("ex", "409");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope res = po.request(req, timeout).get();
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(400, result.get("status"));
        assertEquals("Demonstrate throwing exception at top level", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void nonStandardExceptionTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        String placeholder = "test";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/greetings/"+placeholder).setQueryParameter("ex", "custom");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(400, res.getStatus());
        assertEquals(400, result.get("status"));
        assertEquals(Map.of("error", "non-standard-format"), result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void flowTimeoutTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        String placeholder = "test";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/timeout/"+placeholder).setQueryParameter("ex", "timeout");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(408, result.get("status"));
        assertEquals("Flow timeout for 1000 ms", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void simpleDecisionTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/decision");
        // setting decision to false will trigger decision.case.two
        request.setQueryParameter("decision", "false");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(false, result.get("decision"));
        assertEquals("two", result.get("from"));
        // setting decision to true will trigger decision.case.one
        request.setQueryParameter("decision", "true");
        req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        result = (Map<String, Object>) res.getBody();
        assertEquals(true, result.get("decision"));
        assertEquals("one", result.get("from"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void noOpDecisionTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/noop/decision");
        // setting decision to false will trigger decision.case.two
        request.setQueryParameter("decision", "something-else");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(false, result.get("decision"));
        assertEquals("two", result.get("from"));
        // setting decision to true will trigger decision.case.one
        // "hello" is mapped to true in decision-with-no-op.yml
        request.setQueryParameter("decision", "hello");
        req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        result = (Map<String, Object>) res.getBody();
        assertEquals(true, result.get("decision"));
        assertEquals("one", result.get("from"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void numericDecisionTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/numeric-decision");
        // setting decision to 3 will trigger decision.case.three
        request.setQueryParameter("decision", 3);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(3, result.get("decision"));
        assertEquals("three", result.get("from"));
        // setting decision to 1 will trigger decision.case.one
        request.setQueryParameter("decision", 1);
        req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        result = (Map<String, Object>) res.getBody();
        assertEquals(1, result.get("decision"));
        assertEquals("one", result.get("from"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void invalidNumericDecisionTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/numeric-decision");
        // setting decision to larger than the number of next tasks will result in invalid decision
        request.setQueryParameter("decision", 100);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertEquals(500, res.getStatus());
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(500, result.get("status"));
        assertEquals("error", result.get("type"));
        assertEquals("Task numeric.decision returned invalid decision (100)", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void sequentialTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        String placeholder = "test";
        int seq = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/sequential/"+placeholder).setQueryParameter("seq", seq);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        MultiLevelMap result = new MultiLevelMap((Map<String, Object>) res.getBody());
        assertEquals(seq, result.getElement("pojo.sequence"));
        assertEquals(placeholder, result.getElement("pojo.user"));
        /*
         * serialization compresses numbers to long and float
         * if the number is not greater than MAX integer or float
         */
        assertEquals(12345, result.getElement("integer"));
        assertEquals(12345, result.getElement("long"));
        assertEquals(12.345, result.getElement("float"));
        assertEquals(12.345, result.getElement("double"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void responseTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        String placeholder = "test";
        int seq = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/response/"+placeholder).setQueryParameter("seq", seq);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        // This is the result from the "response" task and not the "end" task
        // where the end task return content type as "text/plain".
        assertEquals("application/json", res.getHeader("content-type"));
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(seq, result.get("sequence"));
        assertEquals(placeholder, result.get("user"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void delayedResponseTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        String placeholder = "test";
        int seq = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/delayed-response/"+placeholder).setQueryParameter("seq", seq);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(seq, result.get("sequence"));
        assertEquals(placeholder, result.get("user"));
    }

    @Test
    void forkJoinTest() throws InterruptedException, ExecutionException {
        forkJoin("/api/fork-n-join/", false);
    }

    @Test
    void forkJoinFlowTest() throws InterruptedException, ExecutionException {
        forkJoin("/api/fork-n-join-flows/", false);
    }

    @Test
    void forkJoinWithExceptionTest() throws InterruptedException, ExecutionException {
        forkJoin("/api/fork-n-join/", true);
    }

    @Test
    void forkJoinFlowWithExceptionTest() throws InterruptedException, ExecutionException {
        forkJoin("/api/fork-n-join-flows/", true);
    }

    @Test
    void forkJoinWithDynamicModeListTest() throws InterruptedException, ExecutionException {
        var mockForkedTask = "mock.echo.me";
        ConcurrentMap<String, Integer> itemsAndIndexes = new ConcurrentHashMap<>();
        TypedLambdaFunction<Map<String, Object>, Object> f1 =
                (headers, input, instance) -> {
                    Object item = input.get("item");
                    Object index = input.get("index");
                    if (item instanceof String text && index instanceof Integer n) {
                        itemsAndIndexes.put(text, n);
                    }
                    return input;
                };
        Platform.getInstance().registerPrivate(mockForkedTask, f1, 10);
        var mock = new EventScriptMock("fork-n-join-with-dynamic-model-test");
        mock.assignFunctionRoute("echo.me", mockForkedTask);
        forkJoin("/api/fork-n-join-with-dynamic-model/", false);
        assertEquals(5, itemsAndIndexes.size());
        assertEquals(0, itemsAndIndexes.get("one"));
        assertEquals(1, itemsAndIndexes.get("two"));
        assertEquals(2, itemsAndIndexes.get("three"));
        assertEquals(3, itemsAndIndexes.get("four"));
        assertEquals(4, itemsAndIndexes.get("five"));
    }

    @SuppressWarnings("unchecked")
    void forkJoin(String apiPath, boolean exception) throws InterruptedException, ExecutionException {
        final int UNAUTHORIZED = 401;
        final long timeout = 8000;
        String placeholder = "test";
        int seq = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl(apiPath+placeholder).setQueryParameter("seq", seq);
        if (exception) {
            request.setQueryParameter("exception", UNAUTHORIZED);
        }
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope res = po.request(req, timeout).get();
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        if (exception) {
            assertEquals(Map.of(
                    "message", "Simulated Exception",
                    "type", "error",
                    "status", UNAUTHORIZED), res.getBody());
            assertEquals(UNAUTHORIZED, res.getStatus());
        } else {
            Map<String, Object> result = (Map<String, Object>) res.getBody();
            PoJo pw = SimpleMapper.getInstance().getMapper().readValue(result, PoJo.class);
            assertEquals(seq, pw.sequence);
            assertEquals(placeholder, pw.user);
            assertEquals("hello-world-one", pw.key1);
            assertEquals("hello-world-two", pw.key2);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        String placeholder = "test";
        int seq = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/pipeline/"+placeholder).setQueryParameter("seq", seq);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(seq, pojo.sequence);
        assertEquals(placeholder, pojo.user);
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineForLoopTest() throws InterruptedException {
        Platform platform = Platform.getInstance();
        // The first task of the flow "for-loop-test" is "echo.one" that is using "no.op".
        // We want to override no.op with my.mock.function to demonstrate mocking a function
        // for a flow.
        var echoOne = "echo.one";
        var mockFunction = "my.mock.function";
        var iteration = new AtomicInteger(0);
        LambdaFunction f = (headers, body, instance) -> {
            var n = iteration.incrementAndGet();
            log.info("Iteration-{} {}", n, body);
            return body;
        };
        platform.registerPrivate(mockFunction, f, 1);
        // override the function for the task "echo.one" to the mock function
        var mock = new EventScriptMock("for-loop-test");
        var previousRoute = mock.getFunctionRoute(echoOne);
        var currentRoute = mock.assignFunctionRoute(echoOne, mockFunction).getFunctionRoute(echoOne);
        assertEquals("no.op", previousRoute);
        assertEquals(mockFunction, currentRoute);
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        String placeholder = "test";
        int seq = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop/"+placeholder).setQueryParameter("seq", seq);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        // the for-loop has executed 3 times and each round deposits "one,", "two," and "three," using file append mode
        assertEquals("one,two,three,one,two,three,one,two,three,", result.get("content"));
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(seq, pojo.sequence);
        assertEquals(placeholder, pojo.user);
        assertEquals(3, result.get("n"));
        assertEquals(3, iteration.get());
        platform.release(mockFunction);
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineForLoopTestSingleTask() throws InterruptedException {
        Platform platform = Platform.getInstance();
        /*
         * In this pipeline test, there is only one task in the pipeline.
         *
         * we will mock the pipeline task that will take a list of strings and an index.
         * It will return UPPER case of the selected item in the list and save it in the listStore.
         *
         * This demonstrates that the system can pass the index (from model.n) and
         * the list (from model.list).
         */
        var itemPicker = "item.picker";
        var mockFunction = "mock.item.picker";
        var iteration = new AtomicInteger(0);
        final List<String> listStore = new ArrayList<>();
        final List<Integer> indexes = new ArrayList<>();
        TypedLambdaFunction<Map<String, Object>, String> f =
                (headers, input, instance) -> {
            var n = iteration.incrementAndGet();
            log.info("Running {} iteration", n);
            Utility util = Utility.getInstance();
            int idx = util.str2int(headers.getOrDefault("idx", "0"));
            indexes.add(idx);
            Object item = input.get("item");
            if (item instanceof String text) {
                var upperText = text.toUpperCase();
                listStore.add(upperText);
                return upperText;
            } else {
                throw new IllegalArgumentException("Input item must be a String");
            }
        };
        platform.registerPrivate(mockFunction, f, 1);
        // mock the pipeline task
        var mock = new EventScriptMock("for-loop-test-single-task");
        var previousRoute = mock.getFunctionRoute(itemPicker);
        var currentRoute = mock.assignFunctionRoute(itemPicker, mockFunction).getFunctionRoute(itemPicker);
        assertEquals("no.op", previousRoute);
        assertEquals(mockFunction, currentRoute);
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        String placeholder = "test";
        int seq = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop-single/"+placeholder).setQueryParameter("seq", seq);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(seq, pojo.sequence);
        assertEquals(placeholder, pojo.user);
        assertEquals(3, result.get("n"));
        assertEquals(3, iteration.get());
        assertEquals(List.of("x", "y", "z", "ITEM3"), result.get("latest"));
        platform.release(mockFunction);
        assertEquals(List.of("ITEM1", "ITEM2", "ITEM3"), listStore);
        assertEquals(List.of(0, 1, 2), indexes);
        assertEquals(List.of("a", "b -> x(b)", "c", "ITEM3"), result.get("items"));
        assertEquals("x -> y", result.get("formula"));
    }

    @Test
    void mockHelperTest() {
        assertThrows(IllegalArgumentException.class, () -> new EventScriptMock(null));
        assertThrows(IllegalArgumentException.class, () -> new EventScriptMock(""));
        assertThrows(IllegalArgumentException.class, () -> new EventScriptMock("no-such-flow"));
        var mock = new EventScriptMock("for-loop-test");
        assertThrows(IllegalArgumentException.class, () -> mock.getFunctionRoute(null));
        assertThrows(IllegalArgumentException.class, () -> mock.getFunctionRoute(""));
        assertThrows(IllegalArgumentException.class, () -> mock.getFunctionRoute("no-such-task"));
        assertThrows(IllegalArgumentException.class, () ->
                mock.assignFunctionRoute("echo.one", null));
        assertThrows(IllegalArgumentException.class, () ->
                mock.assignFunctionRoute("echo.two", ""));
        assertThrows(IllegalArgumentException.class, () ->
                mock.assignFunctionRoute(null, "no-such-mock"));
        assertThrows(IllegalArgumentException.class, () ->
                mock.assignFunctionRoute("", "no-such-mock"));
        assertThrows(IllegalArgumentException.class, () ->
                    mock.assignFunctionRoute("no-such-task", "no-such-mock"));
    }

    @Test
    void pipelineForLoopBreakConditionOne() throws InterruptedException {
        pipelineForLoopBreakConditionTest("break");
    }

    @Test
    void pipelineForLoopBreakConditionTwo() throws InterruptedException {
        pipelineForLoopBreakConditionTest("jump");
    }

    @SuppressWarnings("unchecked")
    void pipelineForLoopBreakConditionTest(String type) throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        String placeholder = "test";
        int seq = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop-break/"+placeholder).setQueryParameter("seq", seq).setQueryParameter(type, 2);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(seq, pojo.sequence);
        assertEquals(placeholder, pojo.user);
        assertEquals(2, result.get("n"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineForLoopBreakConditionTestSingleTask() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        String placeholder = "test";
        int seq = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop-break-single/"+placeholder).setQueryParameter("seq", seq).setQueryParameter("none", 2);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(seq, pojo.sequence);
        assertEquals(placeholder, pojo.user);
        assertEquals(0, result.get("n"));
    }

    @Test
    void pipelineForLoopContinueTest() throws InterruptedException {
        pipelineLoopTest("/api/for-loop-continue/", 4);
    }

    @Test
    void pipelineWhileLoopTest() throws InterruptedException {
        pipelineLoopTest("/api/while-loop/", 3);
    }

    @Test
    void pipelineWhileLoopBreakTest() throws InterruptedException {
        pipelineLoopTest("/api/while-loop-break/", 2);
    }

    @Test
    void pipelineWhileLoopContinueTest() throws InterruptedException {
        pipelineLoopTest("/api/while-loop-continue/", 3);
    }

    @SuppressWarnings("unchecked")
    private void pipelineLoopTest(String uri, int n) throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        String placeholder = "test";
        int seq = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl(uri+placeholder).setQueryParameter("seq", seq);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(seq, pojo.sequence);
        assertEquals(placeholder, pojo.user);
        assertEquals(n, result.get("n"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineExceptionTest() throws InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long timeout = 8000;
        String placeholder = "test";
        int seq = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/pipeline-exception/"+placeholder).setQueryParameter("seq", seq);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, timeout).onSuccess(bench::add);
        EventEnvelope res = bench.poll(timeout, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(400, result.get("status"));
        assertEquals("just a test", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void parallelTest() throws InterruptedException, ExecutionException {
        final long timeout = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/parallel");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope res = po.request(req, timeout).get();
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(2, result.size());
        assertEquals(Map.of("key1", "hello-world-one", "key2", "hello-world-two"), result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void internalFlowTest() throws ExecutionException, InterruptedException {
        final String ORIGINATOR = "unit.test";
        final long timeout = 8000;
        Utility util = Utility.getInstance();
        String traceId = util.getUuid();
        // the "header-test" flow maps the input.header to function input body, thus the input.body is ignored
        String flowId = "header-test";
        Map<String, Object> headers = new HashMap<>();
        headers.put("user-agent", "internal-flow");
        headers.put("accept", "application/json");
        headers.put("x-flow-id", flowId);
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("header", headers);
        dataset.put("body", Map.of("hello", "world"));
        FlowExecutor flowExecutor = FlowExecutor.getInstance();
        EventEnvelope result1 = flowExecutor.request(ORIGINATOR, traceId, "INTERNAL /flow/test",
                flowId, dataset, util.getUuid(), timeout).get();
        assertInstanceOf(Map.class, result1.getBody());
        Map<String, Object> body1 = (Map<String, Object>) result1.getBody();
        // verify that input headers are mapped to the function's input body
        assertEquals("header-test", body1.get("x-flow-id"));
        assertEquals("internal-flow", body1.get("user-agent"));
        assertEquals("application/json", body1.get("accept"));
        EventEnvelope result2 = flowExecutor.request(ORIGINATOR, flowId, dataset, util.getUuid(), timeout).get();
        assertInstanceOf(Map.class, result2.getBody());
        Map<String, Object> body2 = (Map<String, Object>) result2.getBody();
        // verify that input headers are mapped to the function's input body
        assertEquals("header-test", body2.get("x-flow-id"));
        assertEquals("internal-flow", body2.get("user-agent"));
        assertEquals("application/json", body2.get("accept"));
        // do it again asynchronously
        flowExecutor.launch(ORIGINATOR, flowId, dataset, util.getUuid());
        flowExecutor.launch(ORIGINATOR, traceId, "INTERNAL /flow/test/async", flowId, dataset, util.getUuid());
        // and with a callback
        flowExecutor.launch(ORIGINATOR, flowId, dataset, "no.op", util.getUuid());
        final String CALLBACK = "internal.flow.callback";
        final BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(1);
        LambdaFunction f = (eventHeaders, body, instance) -> {
            if (body instanceof Map m) {
                bench.add(m);
            }
            return null;
        };
        Platform platform = Platform.getInstance();
        platform.registerPrivate(CALLBACK, f, 1);
        flowExecutor.launch(ORIGINATOR, traceId, "INTERNAL /flow/test/callback", flowId, CALLBACK,
                        dataset, util.getUuid());
        Map<String, Object> response = bench.poll(5, TimeUnit.SECONDS);
        assertNotNull(response);
        assertEquals("header-test", response.get("x-flow-id"));
        assertEquals("internal-flow", response.get("user-agent"));
        assertEquals("application/json", response.get("accept"));
    }

    @Test
    void internalFlowWithoutFlowIdTest() {
        String uuid = Utility.getInstance().getUuid();
        Map<String, Object> headers = new HashMap<>();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("header", headers);
        dataset.put("body", Map.of("hello", "world"));
        headers.put("user-agent", "internal-flow");
        assertThrowTest(uuid, dataset, null, uuid, "Missing flowId");
        assertThrowTest(uuid, dataset, uuid, null, "Missing correlation ID");
        assertThrowTest(uuid, new HashMap<>(), uuid, uuid, "Missing body in dataset");
    }

    private void assertThrowTest(String traceId, Map<String, Object> dataset, String flowId, String correlationId,
                                 String error) {
        final long timeout = 8000;
        FlowExecutor flowExecutor = FlowExecutor.getInstance();
        var ex = assertThrows(IllegalArgumentException.class, () ->
                flowExecutor.request("unit.test", traceId, "INTERNAL /flow/test",
                                        flowId, dataset, correlationId, timeout));
        assertEquals(error, ex.getMessage());
    }
}
