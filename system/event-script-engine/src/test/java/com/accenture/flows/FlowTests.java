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
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FlowTests extends TestBase {
    private static final Logger log = LoggerFactory.getLogger(FlowTests.class);
    private static final String HTTP_CLIENT = "async.http.request";

    @SuppressWarnings("unchecked")
    @Test
    void httpClientByConfigTest() throws IOException, ExecutionException, InterruptedException {
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("POST")
                .setHeader("accept", "application/json")
                .setHeader("content-type", "application/json")
                .setUrl("/api/http/client/by/config/test");
        request.setBody(Map.of("hello", "world"));
        PostOffice po = new PostOffice("unit.test", "10", "TEST /http/client/by/config");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("test", map.getElement("parameters.path.demo"));
        assertEquals("world", map.getElement("parameters.query.hello"));
        assertInstanceOf(Map.class, map.getElement("body"));
        assertEquals(Map.of("hello", "world"), map.getElement("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void noSuchFlowTest() throws IOException, ExecutionException, InterruptedException {
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json")
                .setUrl("/api/no-such-flow");
        PostOffice po = new PostOffice("unit.test", "2000", "TEST /no/such/flow");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertInstanceOf(Map.class, result.getBody());
        assertEquals(500, result.getStatus());
        Map<String, Object> data = (Map<String, Object>) result.getBody();
        assertEquals("Flow no-such-flow not found", data.get("message"));
    }

    @Test
    void externalStateMachineTest() throws IOException, InterruptedException, ExecutionException {
        executeExtStateMachine("/api/ext/state/");
    }

    @Test
    void externalStateMachineFlowTest() throws IOException, InterruptedException, ExecutionException {
        executeExtStateMachine("/api/ext/state/flow/");
    }

    @SuppressWarnings("unchecked")
    void executeExtStateMachine(String uriPath) throws IOException, InterruptedException, ExecutionException {
        final long TIMEOUT = 8000;
        String USER = "test-user";
        var PAYLOAD = Map.of("hello", "world");
        AsyncHttpRequest request1 = new AsyncHttpRequest();
        request1.setTargetHost(HOST).setMethod("PUT").setHeader("accept", "application/json")
                .setHeader("content-type", "application/json").setBody(PAYLOAD);
        request1.setUrl(uriPath+USER);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req1 = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request1);
        EventEnvelope res1 = po.request(req1, TIMEOUT).get();
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
        request2.setUrl("/api/ext/state/"+USER);
        EventEnvelope req2 = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request2);
        EventEnvelope res2 = po.request(req2, TIMEOUT).get();
        assert res2 != null;
        assertInstanceOf(Map.class, res2.getBody());
        Map<String, Object> result2 = (Map<String, Object>) res2.getBody();
        assertEquals(USER, result2.get("user"));
        assertEquals(PAYLOAD, result2.get("payload"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void typeMatchingTest() throws IOException, ExecutionException, InterruptedException {
        Utility util = Utility.getInstance();
        final String HELLO_WORLD = "hello world";
        final String HELLO = "hello";
        final String WORLD = "world";
        final byte[] HELLO_WORLD_BYTES = util.getUTF(HELLO_WORLD);
        final String B64_TEXT = util.bytesToBase64(HELLO_WORLD_BYTES);
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json")
                .setUrl("/api/type/matching");
        PostOffice po = new PostOffice("unit.test", "2000", "TEST /type/matching");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap mm = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals(B64_TEXT, mm.getElement("b64"));
        assertArrayEquals(HELLO_WORLD_BYTES, getByteArrayFromEncodedIntegers((List<Integer>) mm.getElement("binary")));
        Map<String, Object> m1 = Map.of("b64", B64_TEXT, "text", HELLO_WORLD);
        Map<String, Object> m2 = SimpleMapper.getInstance().getMapper().readValue(mm.getElement("json"), Map.class);
        equalsMap(m1, m2);
        assertEquals(HELLO, mm.getElement("substring"));
        byte[] bson = getByteArrayFromEncodedIntegers((List<Integer>) mm.getElement("bson"));
        Map<String, Object> m3 = SimpleMapper.getInstance().getMapper().readValue(bson, Map.class);
        equalsMap(m1, m3);
        assertEquals(HELLO, mm.getElement("source.substring"));
        assertEquals(WORLD, mm.getElement("source.substring-2"));
        assertArrayEquals(HELLO_WORLD_BYTES,
                getByteArrayFromEncodedIntegers((List<Integer>) mm.getElement("source.keep-as-binary")));
        assertEquals(HELLO_WORLD, mm.getElement("source.no-change"));
        assertArrayEquals(HELLO_WORLD_BYTES,
                getByteArrayFromEncodedIntegers((List<Integer>) mm.getElement("source.binary")));
        assertArrayEquals(HELLO_WORLD_BYTES,
                getByteArrayFromEncodedIntegers((List<Integer>) mm.getElement("source.bytes")));
        assertEquals(HELLO_WORLD, mm.getElement("source.out-of-bound"));
        assertEquals(HELLO_WORLD, mm.getElement("source.invalid-substring"));
        assertEquals(HELLO_WORLD, mm.getElement("source.text"));
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
         * after passing through a HTTP endpoint, JSON string serialization is applied.
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
    void bodyTest() throws IOException, ExecutionException, InterruptedException {
        final long TIMEOUT = 8000;
        final String HELLO = "hello world";
        final String VALUE_A = "A";
        final String VALUE_B = "B";
        final int SEQ = 1;
        Map<String, Object> pojoBody = new HashMap<>();
        pojoBody.put("user", HELLO);
        pojoBody.put("sequence", SEQ);
        pojoBody.put("date", new Date());
        pojoBody.put("key1", VALUE_A);
        pojoBody.put("key2", VALUE_B);
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
        EventEnvelope result = po.request(req, TIMEOUT).get();
        // Take return value as PoJo
        assertInstanceOf(Map.class, result.getBody());
        PoJo restored = result.getBody(PoJo.class);
        assertEquals(HELLO, restored.user);
        assertEquals(SEQ, restored.sequence);
        assertEquals(VALUE_A, restored.key1);
        assertEquals(VALUE_B, restored.key2);
        Utility util = Utility.getInstance();
        // verify that result contains headers set by "input data mapping" earlier
        assertEquals(SEQ, util.str2int(result.getHeader("X-Sequence")));
        assertEquals("AAA", result.getHeader("X-Tag"));
        assertEquals("async-http-client", result.getHeader("x-agent"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void headerTest() throws IOException, ExecutionException, InterruptedException {
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json")
                .setUrl("/api/header/test");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        // verify that input headers are mapped to the function's input body
        assertEquals("header-test", body.get("x-flow-id"));
        assertEquals("async-http-client", body.get("user-agent"));
        assertEquals("application/json", body.get("accept"));
    }

    @Test
    void fileVaultTest() throws IOException, ExecutionException, InterruptedException {
        final long TIMEOUT = 8000;
        final String HELLO = "hello world";
        File f1 = new File("/tmp/temp-test-input.txt");
        Utility util = Utility.getInstance();
        util.str2file(f1, HELLO);
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json").setUrl("/api/file/vault");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertInstanceOf(String.class, result.getBody());
        // "output data mapping" will pass the input classpath file as output body
        InputStream in = this.getClass().getResourceAsStream("/files/hello.txt");
        String resourceContent = util.stream2str(in);
        assertEquals(resourceContent, result.getBody());
        assertEquals("text/plain", result.getHeader("content-type"));
        assertEquals(200, result.getStatus());
        File f2 = new File("/tmp/temp-test-output.txt");
        assertTrue(f2.exists());
        String text = util.file2str(f2);
        assertEquals(HELLO, text);
        File f3 = new File("/tmp/temp-test-match.txt");
        assertTrue(f3.exists());
        String matched = util.file2str(f3);
        assertEquals("true", matched);
        File f4 = new File("/tmp/temp-test-binary.txt");
        assertTrue(f4.exists());
        String binary = util.file2str(f4);
        assertEquals("binary", binary);
        // f1 will be deleted by the output data mapping 'model.none -> file(/tmp/temp-test-input.txt)'
        f2.deleteOnExit();
        f3.deleteOnExit();
        f4.deleteOnExit();
    }

    @SuppressWarnings("unchecked")
    @Test
    void circuitBreakerRetryTest() throws IOException, ExecutionException, InterruptedException {
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json").setUrl("/api/circuit/breaker/2");
        PostOffice po = new PostOffice("unit.test", "100100", "TEST /circuit/breaker/retry");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> output = (Map<String, Object>) result.getBody();
        assertEquals(2, output.get("attempt"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void circuitBreakerAbortTest() throws IOException, ExecutionException, InterruptedException {
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        // the max_attempt is 2 for the circuit breaker and thus this will break
        request.setUrl("/api/circuit/breaker/3");
        PostOffice po = new PostOffice("unit.test", "100101", "TEST /circuit/breaker/abort");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertEquals(400, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> output = (Map<String, Object>) result.getBody();
        assertEquals("error", output.get("type"));
        assertEquals(400, output.get("status"));
        assertEquals("Demo Exception", output.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void resilienceHandlerTest() throws IOException, ExecutionException, InterruptedException {
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
        final long TIMEOUT = 8000;
        // Create condition for backoff by forcing it to throw exception over the backoff_trigger (threshold of 3)
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setUrl("/api/resilience");
        request.setQueryParameter("exception", 400);
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope first = po.request(req, TIMEOUT).get();
        // after 3 attempts, it aborts and returns error 400
        assertEquals(400, first.getStatus());
        EventEnvelope second = po.request(req, TIMEOUT).get();
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
        EventEnvelope resultBo = po.request(requestBo, TIMEOUT).get();
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
        EventEnvelope result1 = po.request(req1, TIMEOUT).get();
        assertEquals(200, result1.getStatus());
        assertInstanceOf(Map.class, result1.getBody());
        Map<String, Object> output1 = (Map<String, Object>) result1.getBody();
        assertEquals(Map.of("path", "alternative"), output1);
        // Try again with HTTP-403
        AsyncHttpRequest request2 = new AsyncHttpRequest();
        request2.setTargetHost(HOST).setMethod("GET").setUrl("/api/resilience");
        request2.setQueryParameter("exception", 403);
        EventEnvelope req2 = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request2);
        EventEnvelope result2 = po.request(req2, TIMEOUT).get();
        assertEquals(200, result2.getStatus());
        assertInstanceOf(Map.class, result2.getBody());
        Map<String, Object> output2 = (Map<String, Object>) result2.getBody();
        assertEquals(Map.of("path", "alternative"), output2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void greetingTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        final String TRACE_ID = "1001";
        final String GREETINGS = "greetings";
        String USER = "12345";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/greetings/"+USER);
        PostOffice po = new PostOffice("unit.test", TRACE_ID, "TEST /greeting");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(USER, result.get("user"));
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
        /*
         * serialization compresses numbers to long and float
         * if the number is not greater than MAX integer or float
         */
        assertEquals(Utility.getInstance().str2int(USER), original.get("user_number"));
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
        assertEquals(TRACE_ID, original.get("trace_id"));
        assertEquals(GREETINGS, original.get("flow_id"));
        // the "demo" key-value is collected from the input headers to the test function
        assertEquals("ok", result.get("demo1"));
        assertEquals(USER, result.get("demo2"));
        // input mapping 'input.header -> header' relays all HTTP headers
        assertEquals("greetings", result.get("demo3"));
        // check map values
        String port = AppConfigReader.getInstance().getProperty("server.port");
        /*
         * Prove that map containing system properties or environment variables is resolved by the config system.
         * The mapping of "map(test.map)" should return the resolved key-values.
         *
         *    test.map:
         *      good: day
         *      hello: world
         *      port: ${server.port}
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
    void parentGreetingTest() throws IOException, InterruptedException, ExecutionException {
        final long TIMEOUT = 8000;
        String USER = "test-user";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/parent-greeting/"+USER);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope res = po.request(req, TIMEOUT).get();
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(USER, result.get("user"));
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
        assertEquals(USER, result.get("demo2"));
        // input mapping 'input.header -> header' relays all HTTP headers
        assertEquals("parent-greetings", result.get("demo3"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void missingSubFlow() throws IOException, InterruptedException, ExecutionException {
        final long TIMEOUT = 8000;
        String USER = "test-user";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/missing-flow/"+USER);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope res = po.request(req, TIMEOUT).get();
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
    void exceptionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/greetings/"+USER).setQueryParameter("ex", true);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(403, result.get("status"));
        assertEquals("just a test", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void nonStandardExceptionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/greetings/"+USER).setQueryParameter("ex", "custom");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(400, res.getStatus());
        assertEquals(400, result.get("status"));
        assertEquals(Map.of("error", "non-standard-format"), result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void flowTimeoutTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/timeout/"+USER).setQueryParameter("ex", "timeout");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(408, result.get("status"));
        assertEquals("Flow timeout for 1000 ms", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void simpleDecisionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/decision");
        // setting decision to false will trigger decision.case.two
        request.setQueryParameter("decision", "false");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(false, result.get("decision"));
        assertEquals("two", result.get("from"));
        // setting decision to true will trigger decision.case.one
        request.setQueryParameter("decision", "true");
        req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        result = (Map<String, Object>) res.getBody();
        assertEquals(true, result.get("decision"));
        assertEquals("one", result.get("from"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void noOpDecisionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/noop/decision");
        // setting decision to false will trigger decision.case.two
        request.setQueryParameter("decision", "something-else");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(false, result.get("decision"));
        assertEquals("two", result.get("from"));
        // setting decision to true will trigger decision.case.one
        // "hello" is mapped to true in decision-with-no-op.yml
        request.setQueryParameter("decision", "hello");
        req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        result = (Map<String, Object>) res.getBody();
        assertEquals(true, result.get("decision"));
        assertEquals("one", result.get("from"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void numericDecisionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/numeric-decision");
        // setting decision to 3 will trigger decision.case.three
        request.setQueryParameter("decision", 3);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(3, result.get("decision"));
        assertEquals("three", result.get("from"));
        // setting decision to 1 will trigger decision.case.one
        request.setQueryParameter("decision", 1);
        req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        result = (Map<String, Object>) res.getBody();
        assertEquals(1, result.get("decision"));
        assertEquals("one", result.get("from"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void invalidNumericDecisionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/numeric-decision");
        // setting decision to larger than the number of next tasks will result in invalid decision
        request.setQueryParameter("decision", 100);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
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
    void sequentialTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/sequential/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        MultiLevelMap result = new MultiLevelMap((Map<String, Object>) res.getBody());
        assertEquals(SEQ, result.getElement("pojo.sequence"));
        assertEquals(USER, result.getElement("pojo.user"));
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
    void responseTest() throws InterruptedException, IOException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/response/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        // This is the result from the "response" task and not the "end" task
        // where the end task return content type as "text/plain".
        assertEquals("application/json", res.getHeader("content-type"));
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(SEQ, result.get("sequence"));
        assertEquals(USER, result.get("user"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void delayedResponseTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/delayed-response/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(SEQ, result.get("sequence"));
        assertEquals(USER, result.get("user"));
    }

    @Test
    void forkJoinTest() throws IOException, InterruptedException, ExecutionException {
        forkJoin("/api/fork-n-join/", false);
    }

    @Test
    void forkJoinFlowTest() throws IOException, InterruptedException, ExecutionException {
        forkJoin("/api/fork-n-join-flows/", false);
    }

    @Test
    void forkJoinWithExceptionTest() throws IOException, InterruptedException, ExecutionException {
        forkJoin("/api/fork-n-join/", true);
    }

    @Test
    void forkJoinFlowWithExceptionTest() throws IOException, InterruptedException, ExecutionException {
        forkJoin("/api/fork-n-join-flows/", true);
    }

    @SuppressWarnings("unchecked")
    void forkJoin(String apiPath, boolean exception) throws IOException, InterruptedException, ExecutionException {
        final int UNAUTHORIZED = 401;
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl(apiPath+USER).setQueryParameter("seq", SEQ);
        if (exception) {
            request.setQueryParameter("exception", UNAUTHORIZED);
        }
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope res = po.request(req, TIMEOUT).get();
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
            assertEquals(SEQ, pw.sequence);
            assertEquals(USER, pw.user);
            assertEquals("hello-world-one", pw.key1);
            assertEquals("hello-world-two", pw.key2);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/pipeline/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(SEQ, pojo.sequence);
        assertEquals(USER, pojo.user);
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineForLoopTest() throws IOException, InterruptedException {
        Platform platform = Platform.getInstance();
        // The first task of the flow "for-loop-test" is "echo.one" that is using "no.op".
        // We want to override no.op with my.mock.function to demonstrate mocking a function
        // for a flow.
        var ECHO_ONE = "echo.one";
        var MOCK_FUNCTION = "my.mock.function";
        var iteration = new AtomicInteger(0);
        LambdaFunction f = (headers, body, instance) -> {
            var n = iteration.incrementAndGet();
            log.info("Iteration-{} {}", n, body);
            return body;
        };
        platform.registerPrivate(MOCK_FUNCTION, f, 1);
        // override the function for the task "echo.one" to the mock function
        var mock = new EventScriptMock("for-loop-test");
        var previousRoute = mock.getFunctionRoute(ECHO_ONE);
        var currentRoute = mock.assignFunctionRoute(ECHO_ONE, MOCK_FUNCTION).getFunctionRoute(ECHO_ONE);
        assertEquals("no.op", previousRoute);
        assertEquals(MOCK_FUNCTION, currentRoute);
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(SEQ, pojo.sequence);
        assertEquals(USER, pojo.user);
        assertEquals(3, result.get("n"));
        assertEquals(3, iteration.get());
        platform.release(MOCK_FUNCTION);
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineForLoopTestSingleTask() throws IOException, InterruptedException {
        Platform platform = Platform.getInstance();
        // The first task of the flow "for-loop-test" is "echo.one" that is using "no.op".
        // We want to override no.op with my.mock.function to demonstrate mocking a function
        // for a flow.
        var ECHO_ONE = "echo.one";
        var MOCK_FUNCTION = "my.mock.function.single";
        var iteration = new AtomicInteger(0);
        LambdaFunction f = (headers, body, instance) -> {
            var n = iteration.incrementAndGet();
            log.info("Iteration-{} for single-task pipeline {}", n, body);
            return body;
        };
        platform.registerPrivate(MOCK_FUNCTION, f, 1);
        // override the function for the task "echo.one" to the mock function
        var mock = new EventScriptMock("for-loop-test-single-task");
        var previousRoute = mock.getFunctionRoute(ECHO_ONE);
        var currentRoute = mock.assignFunctionRoute(ECHO_ONE, MOCK_FUNCTION).getFunctionRoute(ECHO_ONE);
        assertEquals("no.op", previousRoute);
        assertEquals(MOCK_FUNCTION, currentRoute);
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop-single/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(SEQ, pojo.sequence);
        assertEquals(USER, pojo.user);
        assertEquals(3, result.get("n"));
        assertEquals(3, iteration.get());
        platform.release(MOCK_FUNCTION);
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
    void pipelineForLoopBreakConditionOne() throws IOException, InterruptedException {
        pipelineForLoopBreakConditionTest("break");
    }

    @Test
    void pipelineForLoopBreakConditionTwo() throws IOException, InterruptedException {
        pipelineForLoopBreakConditionTest("jump");
    }

    @SuppressWarnings("unchecked")
    void pipelineForLoopBreakConditionTest(String type) throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop-break/"+USER).setQueryParameter("seq", SEQ).setQueryParameter(type, 2);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(SEQ, pojo.sequence);
        assertEquals(USER, pojo.user);
        assertEquals(2, result.get("n"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineForLoopBreakConditionTestSingleTask() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop-break-single/"+USER).setQueryParameter("seq", SEQ).setQueryParameter("none", 2);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(SEQ, pojo.sequence);
        assertEquals(USER, pojo.user);
        assertEquals(0, result.get("n"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineForLoopContinueTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop-continue/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(SEQ, pojo.sequence);
        assertEquals(USER, pojo.user);
        assertEquals(4, result.get("n"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineWhileLoopTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/while-loop/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(SEQ, pojo.sequence);
        assertEquals(USER, pojo.user);
        assertEquals(3, result.get("n"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineWhileLoopBreakTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/while-loop-break/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(SEQ, pojo.sequence);
        assertEquals(USER, pojo.user);
        assertEquals(2, result.get("n"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineWhileLoopContinueTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/while-loop-continue/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(SEQ, pojo.sequence);
        assertEquals(USER, pojo.user);
        assertEquals(3, result.get("n"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void pipelineExceptionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/pipeline-exception/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(400, result.get("status"));
        assertEquals("just a test", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void parallelTest() throws IOException, InterruptedException, ExecutionException {
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/parallel");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope res = po.request(req, TIMEOUT).get();
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(2, result.size());
        assertEquals(Map.of("key1", "hello-world-one", "key2", "hello-world-two"), result);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void internalFlowTest() throws IOException, ExecutionException, InterruptedException {
        final String ORIGINATOR = "unit.test";
        final long TIMEOUT = 8000;
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
                flowId, dataset, util.getUuid(), TIMEOUT).get();
        assertInstanceOf(Map.class, result1.getBody());
        Map<String, Object> body1 = (Map<String, Object>) result1.getBody();
        // verify that input headers are mapped to the function's input body
        assertEquals("header-test", body1.get("x-flow-id"));
        assertEquals("internal-flow", body1.get("user-agent"));
        assertEquals("application/json", body1.get("accept"));
        EventEnvelope result2 = flowExecutor.request(ORIGINATOR, flowId, dataset, util.getUuid(), TIMEOUT).get();
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
        final long TIMEOUT = 8000;
        String traceId = Utility.getInstance().getUuid();
        Map<String, Object> headers = new HashMap<>();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("header", headers);
        dataset.put("body", Map.of("hello", "world"));
        headers.put("user-agent", "internal-flow");
        FlowExecutor flowExecutor = FlowExecutor.getInstance();
        // missing flowId
        assertThrows(IllegalArgumentException.class, () ->
                flowExecutor.request("unit.test", traceId, "INTERNAL /flow/test", null,
                        dataset, traceId, TIMEOUT).get());
        // missing correlation ID
        assertThrows(IllegalArgumentException.class, () ->
                flowExecutor.request("unit.test", traceId, "INTERNAL /flow/test", "dummy-flow",
                        dataset, null, TIMEOUT).get());
        // missing body
        assertThrows(IllegalArgumentException.class, () ->
                flowExecutor.request("unit.test", traceId, "INTERNAL /flow/test", "dummy-flow",
                        new HashMap<>(), null, TIMEOUT).get());
    }

}
