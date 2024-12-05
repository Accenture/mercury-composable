/*

    Copyright 2018-2024 Accenture Technology

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
import com.accenture.models.PoJo;
import com.accenture.setup.TestBase;
import com.accenture.tasks.ParallelTask;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class FlowTests extends TestBase {
    private static final String HTTP_CLIENT = "async.http.request";

    @SuppressWarnings("unchecked")
    @Test
    public void noSuchFlowTest() throws IOException, ExecutionException, InterruptedException {
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

    @SuppressWarnings("unchecked")
    @Test
    public void externalStateMachineTest() throws IOException, InterruptedException, ExecutionException {
        final long TIMEOUT = 8000;
        String USER = "test-user";
        var PAYLOAD = Map.of("hello", "world");
        AsyncHttpRequest request1 = new AsyncHttpRequest();
        request1.setTargetHost(HOST).setMethod("PUT").setHeader("accept", "application/json")
                        .setHeader("content-type", "application/json").setBody(PAYLOAD);
        request1.setUrl("/api/ext/state/"+USER);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req1 = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request1);
        EventEnvelope res1 = po.request(req1, TIMEOUT).get();
        assert res1 != null;
        assertInstanceOf(Map.class, res1.getBody());
        Map<String, Object> result1 = (Map<String, Object>) res1.getBody();
        assertEquals("world", result1.get("hello"));
        // We must assume that external state machine is eventual consistent.
        // Therefore, result may not be immediately available.
        // However, for unit test, we set the external state machine function to have a single worker instance
        // so that the GET request will wait until the PUT request is done, thus returning result correctly.
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
    public void typeMatchingTest() throws IOException, ExecutionException, InterruptedException {
        Utility util = Utility.getInstance();
        final String HELLO_WORLD = "hello world";
        final String HELLO = "hello";
        final String WORLD = "world";
        final String B64_TEXT = util.bytesToBase64(util.getUTF(HELLO_WORLD));
        final byte[] HELLO_WORLD_BYTES = util.getUTF(HELLO_WORLD);
        final long TIMEOUT = 8000;
        final String USERNAME = "test user";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json")
                .setHeader("content-type", "application/json")
                .setUrl("/api/type/matching");
        PostOffice po = new PostOffice("unit.test", "2000", "TEST /type/matching");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertInstanceOf(Map.class, result.getBody());
        MultiLevelMap mm = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals(B64_TEXT, mm.getElement("b64"));
        assertArrayEquals(HELLO_WORLD_BYTES, getBytesFromIntegerList((List<Integer>) mm.getElement("binary")));
        Map<String, Object> m1 = Map.of("b64", B64_TEXT, "text", HELLO_WORLD);
        Map<String, Object> m2 = SimpleMapper.getInstance().getMapper().readValue(mm.getElement("json"), Map.class);
        equalsMap(m1, m2);
        assertEquals(HELLO, mm.getElement("substring"));
        byte[] bson = getBytesFromIntegerList((List<Integer>) mm.getElement("bson"));
        Map<String, Object> m3 = SimpleMapper.getInstance().getMapper().readValue(bson, Map.class);
        equalsMap(m1, m3);
        assertEquals(HELLO, mm.getElement("source.substring"));
        assertEquals(WORLD, mm.getElement("source.substring-2"));
        assertArrayEquals(HELLO_WORLD_BYTES,
                getBytesFromIntegerList((List<Integer>) mm.getElement("source.keep-as-binary")));
        assertEquals(HELLO_WORLD, mm.getElement("source.no-change"));
        assertArrayEquals(HELLO_WORLD_BYTES,
                getBytesFromIntegerList((List<Integer>) mm.getElement("source.binary")));
        assertArrayEquals(HELLO_WORLD_BYTES,
                getBytesFromIntegerList((List<Integer>) mm.getElement("source.bytes")));
        assertEquals(HELLO_WORLD, mm.getElement("source.out-of-bound"));
        assertEquals(HELLO_WORLD, mm.getElement("source.invalid-substring"));
        assertEquals(HELLO_WORLD, mm.getElement("source.text"));
        assertEquals(true, mm.getElement("positive"));
        assertEquals(true, mm.getElement("positive"));
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
    }

    private void equalsMap(Map<String, Object> a, Map<String, Object> b) {
        assertEquals(a.size(), b.size());
        for (String k : a.keySet()) {
            assertEquals(a.get(k), b.get(k));
        }
    }

    private byte[] getBytesFromIntegerList(List<Integer> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Integer i: items) {
            out.write(i);
        }
        return out.toByteArray();
    }

    @Test
    public void bodyTest() throws IOException, ExecutionException, InterruptedException {
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
    public void headerTest() throws IOException, ExecutionException, InterruptedException {
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
    public void fileVaultTest() throws IOException, ExecutionException, InterruptedException {
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
        File f4 = new File("/tmp/temp-test-binary");
        assertTrue(f4.exists());
        String binary = util.file2str(f4);
        assertEquals("binary", binary);
        f1.deleteOnExit();
        f2.deleteOnExit();
        f3.deleteOnExit();
        f4.deleteOnExit();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void circuitBreakerRetryTest() throws IOException, ExecutionException, InterruptedException {
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
    public void circuitBreakerAbortTest() throws IOException, ExecutionException, InterruptedException {
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
        assertEquals("Just a demo exception for circuit breaker to handle", output.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void greetingTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/greeting/"+USER);
        PostOffice po = new PostOffice("unit.test", "1001", "TEST /greeting");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(USER, result.get("user"));
        assertEquals(getAppName(), result.get("name"));
        assertEquals("hello world", result.get("greeting"));
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
        assertEquals(12345, original.get("long_number"));
        assertEquals(12.345, original.get("float_number"));
        assertEquals(12.345, original.get("double_number"));
        assertEquals(true, original.get("boolean_value"));
        assertEquals(System.getenv("PATH"), original.get("path"));
        // the "demo" key-value is collected from the input headers to the test function
        assertEquals("ok", result.get("demo1"));
        assertEquals(USER, result.get("demo2"));
        // input mapping 'input.header -> header' relays all HTTP headers
        assertEquals("greetings", result.get("demo3"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void parentGreetingTest() throws IOException, InterruptedException, ExecutionException {
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
    public void missingSubFlow() throws IOException, InterruptedException, ExecutionException {
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
    public void exceptionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/greeting/"+USER).setQueryParameter("ex", true);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(400, result.get("status"));
        assertEquals("just a test", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void flowTimeoutTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/timeout/"+USER).setQueryParameter("ex", "timeout");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(408, result.get("status"));
        assertEquals("Flow timeout for 1000 ms", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void simpleDecisionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/decision");
        // setting decision to false will trigger decision.case.two
        request.setQueryParameter("decision", "false");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(false, result.get("decision"));
        assertEquals("two", result.get("from"));
        // setting decision to true will trigger decision.case.one
        request.setQueryParameter("decision", "true");
        req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        result = (Map<String, Object>) res.getBody();
        assertEquals(true, result.get("decision"));
        assertEquals("one", result.get("from"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void noOpDecisionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/noop/decision");
        // setting decision to false will trigger decision.case.two
        request.setQueryParameter("decision", "something-else");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
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
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        result = (Map<String, Object>) res.getBody();
        assertEquals(true, result.get("decision"));
        assertEquals("one", result.get("from"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void numericDecisionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/numeric-decision");
        // setting decision to 2 will trigger decision.case.two
        request.setQueryParameter("decision", 2);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(2, result.get("decision"));
        assertEquals("two", result.get("from"));
        // setting decision to 1 will trigger decision.case.one
        request.setQueryParameter("decision", 1);
        req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        result = (Map<String, Object>) res.getBody();
        assertEquals(1, result.get("decision"));
        assertEquals("one", result.get("from"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void invalidNumericDecisionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/numeric-decision");
        // setting decision to larger than the number of next tasks will result in invalid decision
        request.setQueryParameter("decision", 100);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(500, result.get("status"));
        assertTrue(result.get("message").toString().contains("invalid decision"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void sequentialTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/sequential/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
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
    public void responseTest() throws InterruptedException, IOException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/response/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(SEQ, result.get("sequence"));
        assertEquals(USER, result.get("user"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void futureResponseTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        int DELAY = 500;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/future-response/"+USER).setQueryParameter("seq", SEQ)
                .setQueryParameter("delay", DELAY);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(SEQ, result.get("sequence"));
        assertEquals(USER, result.get("user"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void forkJoinTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/fork-n-join/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        PoJo pw = SimpleMapper.getInstance().getMapper().readValue(result, PoJo.class);
        assertEquals(SEQ, pw.sequence);
        assertEquals(USER, pw.user);
        assertEquals("hello-world-one", pw.key1);
        assertEquals("hello-world-two", pw.key2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void pipelineTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/pipeline/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
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
    public void pipelineForLoopTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
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
    public void pipelineForLoopBreakTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop-break/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
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
    public void pipelineForLoopContinueTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop-continue/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
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
    public void pipelineExceptionTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/pipeline-exception/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertEquals(400, result.get("status"));
        assertEquals("just a test", result.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void parallelTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/parallel");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertFalse(result.isEmpty());
        assertEquals(2, ParallelTask.bench.size());
        // At the end of parallel execution of 2 tasks, the bench should have received 2 key-values
        Map<String, Object> map1 = ParallelTask.bench.poll(5, TimeUnit.SECONDS);
        assertNotNull(map1);
        Map<String, Object> consolidated = new HashMap<>(map1);
        Map<String, Object> map2 = ParallelTask.bench.poll(5, TimeUnit.SECONDS);
        assertNotNull(map2);
        consolidated.putAll(map2);
        assertEquals(2, consolidated.size());
        assertTrue(consolidated.containsKey("key1"));
        assertTrue(consolidated.containsKey("key2"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void internalFlowTest() throws IOException, ExecutionException, InterruptedException {
        final String ORIGINATOR = "unit.test";
        final long TIMEOUT = 8000;
        Utility util = Utility.getInstance();
        String traceId = util.getUuid();
        // the "header-test" flow maps the input.header to function input body, thus the input.body is ignored
        String flowId = "header-test";
        Map<String, Object> headers = new HashMap<>();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("header", headers);
        dataset.put("body", Map.of("hello", "world"));
        headers.put("user-agent", "internal-flow");
        headers.put("accept", "application/json");
        headers.put("x-flow-id", flowId);
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
                bench.offer(m);
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
    public void internalFlowWithoutFlowIdTest() {
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