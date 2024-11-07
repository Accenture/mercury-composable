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

package com.accenture.test;

import com.accenture.adapters.StartFlow;
import com.accenture.models.PoJo;
import com.accenture.setup.TestBase;
import com.accenture.tasks.ParallelTask;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class FlowTests extends TestBase {
    private static final String HTTP_CLIENT = "async.http.request";

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
        EventEmitter po = EventEmitter.getInstance();
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
        EventEmitter po = EventEmitter.getInstance();
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
        EventEmitter po = EventEmitter.getInstance();
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
    public void pipelineForLoopIfThenElseTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop-if-then-else/"+USER).setQueryParameter("seq", SEQ);
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
    public void pipelineForLoopIfThenElseParallelTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop-if-then-else-parallel/"+USER).setQueryParameter("seq", SEQ);
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

    @SuppressWarnings("unchecked")
    @Test
    public void internalFlowTest() throws IOException, ExecutionException, InterruptedException {
        final long TIMEOUT = 8000;
        String traceId = Utility.getInstance().getUuid();
        PostOffice po = new PostOffice("unit.test", traceId, "INTERNAL /flow/test");
        String flowId = "header-test";
        Map<String, Object> headers = new HashMap<>();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("header", headers);
        dataset.put("body", Map.of("hello", "world"));
        headers.put("user-agent", "internal-flow");
        headers.put("accept", "application/json");
        headers.put("x-flow-id", flowId);
        StartFlow startFlow = StartFlow.getInstance();
        EventEnvelope result = startFlow.request(po, flowId, dataset, traceId, TIMEOUT).get();
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        // verify that input headers are mapped to the function's input body
        assertEquals("header-test", body.get("x-flow-id"));
        assertEquals("internal-flow", body.get("user-agent"));
        assertEquals("application/json", body.get("accept"));
        // do it again asynchronously
        startFlow.send(po, flowId, dataset, traceId);
        // and with a callback
        startFlow.send(po, flowId, dataset, "no.op", traceId);
    }

    @Test
    public void internalFlowWithoutFlowIdTest() {
        final long TIMEOUT = 8000;
        String traceId = Utility.getInstance().getUuid();
        PostOffice po = new PostOffice("unit.test", traceId, "INTERNAL /flow/test");
        Map<String, Object> headers = new HashMap<>();
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("header", headers);
        dataset.put("body", Map.of("hello", "world"));
        headers.put("user-agent", "internal-flow");
        StartFlow startFlow = StartFlow.getInstance();
        // missing flowId
        assertThrows(IllegalArgumentException.class, () ->
                startFlow.request(po, null, dataset, traceId, TIMEOUT).get());
        // missing correlation ID
        assertThrows(IllegalArgumentException.class, () ->
                startFlow.request(po, "dummy-flow", dataset, null, TIMEOUT).get());
        // missing body
        assertThrows(IllegalArgumentException.class, () ->
                startFlow.request(po, "dummy-flow", new HashMap<>(), null, TIMEOUT).get());
    }

}
