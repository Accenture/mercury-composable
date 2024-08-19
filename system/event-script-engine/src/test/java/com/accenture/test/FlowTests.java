package com.accenture.test;

import com.accenture.models.PoJo;
import com.accenture.setup.TestBase;
import com.accenture.tasks.ParallelTask;
import org.junit.Assert;
import org.junit.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class FlowTests extends TestBase {
    private static final String HTTP_CLIENT = "async.http.request";

    @Test
    public void fileVaultTest() throws IOException, ExecutionException, InterruptedException {
        final long TIMEOUT = 8000;
        final String HELLO = "hello world";
        File f1 = new File("/tmp/temp-test-input.txt");
        Utility util = Utility.getInstance();
        util.str2file(f1, HELLO);
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/file/vault");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        Assert.assertEquals(200, result.getStatus());
        File f2 = new File("/tmp/temp-test-output.txt");
        Assert.assertTrue(f2.exists());
        String text = util.file2str(f2);
        Assert.assertEquals(HELLO, text);
        File f3 = new File("/tmp/temp-test-match.txt");
        Assert.assertTrue(f3.exists());
        String matched = util.file2str(f3);
        Assert.assertEquals("true", matched);
        File f4 = new File("/tmp/temp-test-binary");
        Assert.assertTrue(f4.exists());
        String binary = util.file2str(f4);
        Assert.assertEquals("binary", binary);
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
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/circuit/breaker/2");
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        Assert.assertEquals(200, result.getStatus());
        Assert.assertTrue(result.getBody() instanceof Map);
        Map<String, Object> output = (Map<String, Object>) result.getBody();
        Assert.assertEquals(2, output.get("attempt"));
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
        Assert.assertEquals(400, result.getStatus());
        Assert.assertTrue(result.getBody() instanceof Map);
        Map<String, Object> output = (Map<String, Object>) result.getBody();
        Assert.assertEquals("Just a demo exception for circuit breaker to handle", output.get("message"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertEquals(USER, result.get("user"));
        Assert.assertEquals(getAppName(), result.get("name"));
        Assert.assertEquals("hello world", result.get("greeting"));
        Assert.assertTrue(result.containsKey("original"));
        Map<String, Object> original = (Map<String, Object>) result.get("original");
        Assert.assertEquals(201, res.getStatus());
        // output mapping 'input.header -> header' delivers the result EventEnvelope's headers
        Assert.assertEquals("test-header", res.getHeader("demo"));
        // output mapping 'header.demo -> output.header.x-demo' maps the original header "demo" to "x-demo"
        Assert.assertEquals("test-header", res.getHeader("x-demo"));
        /*
         * serialization compresses numbers to long and float
         * if the number is not greater than MAX integer or float
         */
        Assert.assertEquals(12345, original.get("long_number"));
        Assert.assertEquals(12.345, original.get("float_number"));
        Assert.assertEquals(12.345, original.get("double_number"));
        Assert.assertEquals(true, original.get("boolean_value"));
        Assert.assertEquals(System.getenv("PATH"), original.get("path"));
        // the "demo" key-value is collected from the input headers to the test function
        Assert.assertEquals("ok", result.get("demo1"));
        Assert.assertEquals(USER, result.get("demo2"));
        // input mapping 'input.header -> header' relays all HTTP headers
        Assert.assertEquals("greetings", result.get("demo3"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertEquals(400, result.get("status"));
        Assert.assertEquals("just a test", result.get("message"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertEquals(408, result.get("status"));
        Assert.assertEquals("Flow timeout for 1000 ms", result.get("message"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertEquals(false, result.get("decision"));
        Assert.assertEquals("two", result.get("from"));
        // setting decision to true will trigger decision.case.one
        request.setQueryParameter("decision", "true");
        req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        Assert.assertTrue(res.getBody() instanceof Map);
        result = (Map<String, Object>) res.getBody();
        Assert.assertEquals(true, result.get("decision"));
        Assert.assertEquals("one", result.get("from"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertEquals(2, result.get("decision"));
        Assert.assertEquals("two", result.get("from"));
        // setting decision to 1 will trigger decision.case.one
        request.setQueryParameter("decision", 1);
        req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::offer);
        res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        Assert.assertTrue(res.getBody() instanceof Map);
        result = (Map<String, Object>) res.getBody();
        Assert.assertEquals(1, result.get("decision"));
        Assert.assertEquals("one", result.get("from"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertEquals(500, result.get("status"));
        Assert.assertTrue(result.get("message").toString().contains("invalid decision"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        MultiLevelMap result = new MultiLevelMap((Map<String, Object>) res.getBody());
        Assert.assertEquals(SEQ, result.getElement("pojo.sequence"));
        Assert.assertEquals(USER, result.getElement("pojo.user"));
        /*
         * serialization compresses numbers to long and float
         * if the number is not greater than MAX integer or float
         */
        Assert.assertEquals(12345, result.getElement("integer"));
        Assert.assertEquals(12345, result.getElement("long"));
        Assert.assertEquals(12.345, result.getElement("float"));
        Assert.assertEquals(12.345, result.getElement("double"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertEquals(SEQ, result.get("sequence"));
        Assert.assertEquals(USER, result.get("user"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertEquals(SEQ, result.get("sequence"));
        Assert.assertEquals(USER, result.get("user"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        PoJo pw = SimpleMapper.getInstance().getMapper().readValue(result, PoJo.class);
        Assert.assertEquals(SEQ, pw.sequence);
        Assert.assertEquals(USER, pw.user);
        Assert.assertEquals("hello-world-one", pw.key1);
        Assert.assertEquals("hello-world-two", pw.key2);
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        Assert.assertEquals(SEQ, pojo.sequence);
        Assert.assertEquals(USER, pojo.user);
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        Assert.assertEquals(SEQ, pojo.sequence);
        Assert.assertEquals(USER, pojo.user);
        Assert.assertEquals(3, result.get("n"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        Assert.assertEquals(SEQ, pojo.sequence);
        Assert.assertEquals(USER, pojo.user);
        Assert.assertEquals(2, result.get("n"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        Assert.assertEquals(SEQ, pojo.sequence);
        Assert.assertEquals(USER, pojo.user);
        Assert.assertEquals(4, result.get("n"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        Assert.assertEquals(SEQ, pojo.sequence);
        Assert.assertEquals(USER, pojo.user);
        Assert.assertEquals(4, result.get("n"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        Assert.assertEquals(SEQ, pojo.sequence);
        Assert.assertEquals(USER, pojo.user);
        Assert.assertEquals(4, result.get("n"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertEquals(400, result.get("status"));
        Assert.assertEquals("just a test", result.get("message"));
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
        Assert.assertTrue(res.getBody() instanceof Map);
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(2, ParallelTask.bench.size());
        // At the end of parallel execution of 2 tasks, the bench should have received 2 key-values
        Map<String, Object> map1 = ParallelTask.bench.poll(5, TimeUnit.SECONDS);
        Assert.assertNotNull(map1);
        Map<String, Object> consolidated = new HashMap<>(map1);
        Map<String, Object> map2 = ParallelTask.bench.poll(5, TimeUnit.SECONDS);
        Assert.assertNotNull(map2);
        consolidated.putAll(map2);
        Assert.assertEquals(2, consolidated.size());
        Assert.assertTrue(consolidated.containsKey("key1"));
        Assert.assertTrue(consolidated.containsKey("key2"));
    }

}
