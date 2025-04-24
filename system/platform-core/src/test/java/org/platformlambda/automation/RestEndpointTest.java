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

package org.platformlambda.automation;

import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.serializers.SimpleXmlParser;
import org.platformlambda.core.serializers.SimpleXmlWriter;
import org.platformlambda.core.system.*;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class RestEndpointTest extends TestBase {

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private static final long RPC_TIMEOUT = 10000;

    private static final SimpleXmlParser xml = new SimpleXmlParser();

    @BeforeAll
    public static void setupAuthenticator() throws IOException {
        Platform platform = Platform.getInstance();
        if (!platform.hasRoute("v1.api.auth")) {
            LambdaFunction f = (headers, input, instance) -> {
                PostOffice po = new PostOffice(headers, instance);
                po.annotateTrace("hello", "world");
                po.annotateTrace("demo-map", Map.of("status", "authenticated"));
                po.annotateTrace("demo-list", List.of("item1", "item2"));
                return true;
            };
            platform.registerPrivate("v1.api.auth", f, 1);
        }
        if (!platform.hasRoute("v1.demo.auth")) {
            LambdaFunction f = (headers, input, instance) -> {
                PostOffice po = new PostOffice(headers, instance);
                po.annotateTrace("demo", "will show 'unauthorized'");
                return false;
            };
            platform.registerPrivate("v1.demo.auth", f, 1);
        }
    }

    @Test
    void optionsMethodTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setHeader("accept", "application/json");
        req.setUrl("/api/hello/world?hello world=abc").setQueryParameter("x1", "y").setMethod("OPTIONS");
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        req.setQueryParameter("x2", list);
        req.setTargetHost("http://127.0.0.1:"+port);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        // response body is null
        assertNull(response.getBody());
        // CORS headers are inserted - retrieve it with a case-insensitive key
        assertEquals("*", response.getHeader("access-control-Allow-Origin"));
    }

    @SuppressWarnings(value = "unchecked")
    @Test
    void serviceTest() throws IOException, InterruptedException, ExecutionException {
        final int TTL_SECONDS = 7;
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setHeader("accept", "application/json");
        req.setUrl("/api/hello/world?hello world=abc#hello&test=message");
        req.setQueryParameter("x1", "y");
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        req.setQueryParameter("x2", list);
        req.setTargetHost("http://127.0.0.1:"+port);
        req.setTimeoutSeconds(TTL_SECONDS);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        EventEnvelope response = po.request(request, RPC_TIMEOUT).get();
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        // validate custom content type
        assertEquals("application/vnd.my.org-v2.1+json; charset=utf-8", response.getHeader("content-type"));
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("application/json", map.getElement("headers.accept"));
        assertEquals(false, map.getElement("https"));
        assertEquals("/api/hello/world", map.getElement("url"));
        assertEquals("GET", map.getElement("method"));
        assertEquals("127.0.0.1", map.getElement("ip"));
        assertEquals(String.valueOf(TTL_SECONDS * 1000), map.getElement("headers.x-ttl"));
        assertEquals("y", map.getElement("parameters.query.x1"));
        assertEquals(list, map.getElement("parameters.query.x2"));
        // the HTTP request filter will not execute because the request is not a static content request
        assertNull(response.getHeader("x-filter"));
    }

    @Test
    void nonExistUrlTest() throws IOException, InterruptedException, ExecutionException {
        checkHttpRouting("/api/hello/../world &moved to https://evil.site?hello world=abc");
        checkHttpRouting("/api/hello/world <div>test</div>");
        checkHttpRouting("/api/hello/world > something");
        checkHttpRouting("/api/hello/world &nbsp;");
        /*
         * This is a valid URL with matrix parameters
         *
         * It will return HTTP-404 to prove that it has passed thru to the REST endpoint.
         * If the URL format is invalid, the system will return HTTP-400 with empty HTTP response body.
         *
         * Matrix parameter feature may not be supported in some REST application server.
         * Please check documentation for your application server framework being using it.
         *
         * REST automation supports it as follows
         * --------------------------------------
         * When using matrix parameters, the URI segment containing the parameters can be configured
         * as a "path parameter" in the "rest.yaml" endpoint configuration file. Your application
         * can retrieve and parse the URI segments containing the matrix parameters for processing.
         * e.g.
         *
         * Endpoint in rest.yaml:
         * url: "/api/hello/{base}/{option}"
         *
         * would return:
         * base="world;a=b 2;c$=d$3"
         * option=";feature=12 3"
         */
        checkHttpRouting("/api/hello/world;a=b 2;c$=d$3/;feature=12 3");
    }

    @SuppressWarnings("unchecked")
    private void checkHttpRouting(String uri) throws IOException, InterruptedException, ExecutionException {
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setHeader("accept", "application/json");
        req.setUrl(uri);
        req.setQueryParameter("x1", "y");
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        req.setQueryParameter("x2", list);
        req.setTargetHost("http://127.0.0.1:"+port);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        EventEnvelope response = po.request(request, RPC_TIMEOUT).get();
        assert response != null;
        assertEquals(404, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> map = (Map<String, Object>) response.getBody();
        assertEquals("Resource not found", map.get("message"));
        assertEquals("error", map.get("type"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void authRoutingTest1() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setHeader("accept", "application/json");
        String credentials = "Basic " + util.bytesToBase64(util.getUTF("hello:world"));
        req.setHeader("authorization", credentials);
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(503, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> map = (Map<String, Object>) response.getBody();
        assertEquals("Service v1.basic.auth not reachable", map.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void authRoutingTest2() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setHeader("accept", "application/json");
        req.setHeader("x-app-name", "demo");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(401, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> map = (Map<String, Object>) response.getBody();
        assertEquals("Unauthorized", map.get("message"));
    }

    @Test
    void uploadBytesWithPut() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        int len = 0;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i=0; i < 100; i++) {
            byte[] line = util.getUTF("hello world "+i+"\n");
            bytes.write(line);
            len += line.length;
        }
        byte[] b = bytes.toByteArray();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("PUT");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        req.setContentLength(len).setBody(b);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(byte[].class, response.getBody());
        assertArrayEquals(b, (byte[]) response.getBody());
    }

    @Test
    void uploadSmallBlockWithPut() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench1 = new ArrayBlockingQueue<>(1);
        final BlockingQueue<Boolean> bench2 = new ArrayBlockingQueue<>(1);
        final Utility util = Utility.getInstance();
        final EventEmitter po = EventEmitter.getInstance();
        int TTL = 9;
        int len = 0;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        EventPublisher publisher = new EventPublisher(10000);
        for (int i=0; i < 100; i++) {
            byte[] line = util.getUTF("hello world "+i+"\n");
            publisher.publish(line);
            bytes.write(line);
            len += line.length;
        }
        publisher.publishCompletion();
        byte[] b = bytes.toByteArray();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("PUT");
        /*
         * The "/api/v1/hello/world" prefix tests the REST automation system HTTP relay feature.
         * It will rewrite the URI to "/api/hello/world" based on the rest.yaml configuration.
         */
        req.setUrl("/api/v1/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        req.setStreamRoute(publisher.getStreamId());
        req.setTimeoutSeconds(TTL);
        req.setContentLength(len);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench1::add);
        EventEnvelope response = bench1.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertNull(response.getBody());
        // async.http.request returns a stream
        String streamId = response.getHeader("X-Stream-Id");
        assertEquals(String.valueOf(TTL * 1000), response.getHeader("x-ttl"));
        assertNotNull(streamId);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        FluxConsumer<byte[]> flux = new FluxConsumer<>(streamId, RPC_TIMEOUT);
        flux.consume(data -> {
            try {
                result.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, null, () -> bench2.add(true));
        Boolean done = bench2.poll(10, TimeUnit.SECONDS);
        assertEquals(true, done);
        assertArrayEquals(b, result.toByteArray());
    }

    @Test
    void uploadLargePayloadWithPut() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        int len = 0;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i=0; i < 4000; i++) {
            byte[] line = util.getUTF("hello world "+i+"\n");
            bytes.write(line);
            len += line.length;
        }
        /*
         * While the payload size is large, the content-length is given.
         * Therefore, the system will render it as a byte array.
         */
        byte[] b = bytes.toByteArray();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("PUT");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        req.setBody(b);
        req.setContentLength(len);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(byte[].class, response.getBody());
        assertArrayEquals(b, (byte[]) response.getBody());
    }

    @Test
    void uploadStreamWithPut() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench1 = new ArrayBlockingQueue<>(1);
        final BlockingQueue<Boolean> bench2 = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        int len = 0;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        EventPublisher publisher = new EventPublisher(10000);
        for (int i=0; i < 600; i++) {
            String line = "hello world "+i+"\n";
            byte[] d = util.getUTF(line);
            publisher.publish(d);
            bytes.write(d);
            len += d.length;
        }
        publisher.publishCompletion();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("PUT");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        req.setHeader("accept", "application/octet-stream");
        req.setHeader("content-type", "application/octet-stream");
        req.setContentLength(len);
        req.setStreamRoute(publisher.getStreamId());
        req.setTimeoutSeconds((int) RPC_TIMEOUT / 1000);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench1::add);
        EventEnvelope response = bench1.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertNotNull(response.getHeader("x-stream-id"));
        String streamId = response.getHeader("x-stream-id");
        long ttl = util.str2long(response.getHeader("x-ttl"));
        assertEquals(RPC_TIMEOUT, ttl);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        FluxConsumer<byte[]> flux = new FluxConsumer<>(streamId, ttl);
        flux.consume(data -> {
            try {
                result.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, null, () -> bench2.add(true));
        Boolean done = bench2.poll(10, TimeUnit.SECONDS);
        assertEquals(true, done);
        assertArrayEquals(bytes.toByteArray(), result.toByteArray());
    }

    @Test
    void uploadMultipartWithPost() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench1 = new ArrayBlockingQueue<>(1);
        final BlockingQueue<Boolean> bench2 = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        int len = 0;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        EventPublisher publisher = new EventPublisher(10000);
        for (int i=0; i < 600; i++) {
            String line = "hello world "+i+"\n";
            byte[] d = util.getUTF(line);
            publisher.publish(d);
            bytes.write(d);
            len += d.length;
        }
        publisher.publishCompletion();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("POST");
        req.setUrl("/api/upload/demo");
        req.setTargetHost("http://127.0.0.1:"+port);
        req.setHeader("accept", "application/json");
        req.setHeader("content-type", MULTIPART_FORM_DATA);
        req.setContentLength(len);
        req.setFileName("hello-world.txt");
        req.setStreamRoute(publisher.getStreamId());
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench1::add);
        EventEnvelope response = bench1.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertNotNull(response.getHeader("x-stream-id"));
        String streamId = response.getHeader("x-stream-id");
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        FluxConsumer<byte[]> flux = new FluxConsumer<>(streamId, RPC_TIMEOUT);
        flux.consume(data -> {
            try {
                result.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, null, () -> bench2.add(true));
        Boolean done = bench2.poll(10, TimeUnit.SECONDS);
        assertEquals(true, done);
        assertArrayEquals(bytes.toByteArray(), result.toByteArray());
    }

    @SuppressWarnings("unchecked")
    @Test
    void postJson() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("POST");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        Map<String, Object> data = new HashMap<>();
        data.put("hello", "world");
        data.put("test", "message");
        String json = SimpleMapper.getInstance().getMapper().writeValueAsString(data);
        req.setBody(json);
        req.setHeader("accept", "application/json");
        req.setHeader("content-type", "application/json");
        req.setTimeoutSeconds((int) RPC_TIMEOUT / 1000);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("application/json", map.getElement("headers.content-type"));
        assertEquals("application/json", map.getElement("headers.accept"));
        assertEquals(false, map.getElement("https"));
        assertEquals("/api/hello/world", map.getElement("url"));
        assertEquals("POST", map.getElement("method"));
        assertEquals("127.0.0.1", map.getElement("ip"));
        assertEquals(String.valueOf(RPC_TIMEOUT), map.getElement("headers.x-ttl"));
        assertInstanceOf(Map.class, map.getElement("body"));
        Map<String, Object> received = (Map<String, Object>) map.getElement("body");
        assertEquals(data, received);
    }

    @SuppressWarnings("unchecked")
    @Test
    void postXmlAsMap() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        SimpleXmlWriter xmlWriter = new SimpleXmlWriter();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("POST");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        Map<String, Object> data = new HashMap<>();
        data.put("hello", "world");
        data.put("test", "message");
        String xml = xmlWriter.write(data);
        req.setBody(xml);
        req.setTimeoutSeconds((int) RPC_TIMEOUT / 1000);
        req.setHeader("accept", "application/xml");
        req.setHeader("content-type", "application/xml");
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("application/xml", map.getElement("headers.content-type"));
        assertEquals("application/xml", map.getElement("headers.accept"));
        assertEquals("false", map.getElement("https"));
        assertEquals("/api/hello/world", map.getElement("url"));
        assertEquals("POST", map.getElement("method"));
        assertEquals("127.0.0.1", map.getElement("ip"));
        assertEquals(String.valueOf(RPC_TIMEOUT), map.getElement("headers.x-ttl"));
        assertInstanceOf(Map.class, map.getElement("body"));
        Map<String, Object> received = (Map<String, Object>) map.getElement("body");
        assertEquals(data, received);
    }

    @Test
    void postXmlAsText() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("POST");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        req.setBody("hello world");
        req.setHeader("accept", "application/xml");
        req.setHeader("content-type", "application/xml");
        req.setHeader("x-raw-xml", "true");
        req.setTimeoutSeconds((int) RPC_TIMEOUT / 1000);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        // when x-raw-xml header is true, the response is rendered as a string
        assertInstanceOf(String.class, response.getBody());
        Map<String, Object> payload = xml.parse((String) response.getBody());
        MultiLevelMap map = new MultiLevelMap(payload);
        assertEquals("application/xml", map.getElement("headers.content-type"));
        assertEquals("application/xml", map.getElement("headers.accept"));
        assertEquals("true", map.getElement("headers.x-raw-xml"));
        assertEquals("false", map.getElement("https"));
        assertEquals("/api/hello/world", map.getElement("url"));
        assertEquals("POST", map.getElement("method"));
        assertEquals("127.0.0.1", map.getElement("ip"));
        assertEquals(String.valueOf(RPC_TIMEOUT), map.getElement("headers.x-ttl"));
        assertInstanceOf(String.class, map.getElement("body"));
        assertEquals("hello world", map.getElement("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void postJsonMap() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("POST");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        Map<String, Object> data = new HashMap<>();
        data.put("hello", "world");
        data.put("test", "message");
        req.setBody(data);
        req.setHeader("accept", "application/json");
        req.setHeader("content-type", "application/json");
        req.setTimeoutSeconds((int) RPC_TIMEOUT / 1000);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("application/json", map.getElement("headers.content-type"));
        assertEquals("application/json", map.getElement("headers.accept"));
        assertEquals(false, map.getElement("https"));
        assertEquals("/api/hello/world", map.getElement("url"));
        assertEquals("POST", map.getElement("method"));
        assertEquals("127.0.0.1", map.getElement("ip"));
        assertEquals(String.valueOf(RPC_TIMEOUT), map.getElement("headers.x-ttl"));
        assertInstanceOf(Map.class, map.getElement("body"));
        Map<String, Object> received = (Map<String, Object>) map.getElement("body");
        assertEquals(data, received);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testJsonResultList() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("POST");
        req.setUrl("/api/hello/list");
        req.setTargetHost("http://127.0.0.1:"+port);
        Map<String, Object> data = new HashMap<>();
        data.put("hello", "world");
        data.put("test", "message");
        req.setBody(data);
        req.setHeader("accept", "application/json");
        req.setHeader("content-type", "application/json");
        req.setTimeoutSeconds((int) RPC_TIMEOUT / 1000);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(List.class, response.getBody());
        List<Map<String, Object>> list = (List<Map<String, Object>>) response.getBody();
        assertEquals(1, list.size());
        assertEquals(HashMap.class, list.getFirst().getClass());
        MultiLevelMap map = new MultiLevelMap(list.getFirst());
        assertEquals("application/json", map.getElement("headers.content-type"));
        assertEquals("application/json", map.getElement("headers.accept"));
        assertEquals(false, map.getElement("https"));
        assertEquals("/api/hello/list", map.getElement("url"));
        assertEquals("POST", map.getElement("method"));
        assertEquals("127.0.0.1", map.getElement("ip"));
        assertEquals(String.valueOf(RPC_TIMEOUT), map.getElement("headers.x-ttl"));
        assertInstanceOf(Map.class, map.getElement("body"));
        Map<String, Object> received = (Map<String, Object>) map.getElement("body");
        assertEquals(data, received);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testXmlResultList() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        SimpleXmlWriter xmlWriter = new SimpleXmlWriter();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("POST");
        req.setUrl("/api/hello/list");
        req.setTargetHost("http://127.0.0.1:"+port);
        Map<String, Object> data = new HashMap<>();
        data.put("hello", "world");
        data.put("test", "message");
        String xml = xmlWriter.write(data);
        req.setTimeoutSeconds((int) (RPC_TIMEOUT / 1000)).setBody(xml);
        req.setHeader("accept", "application/xml");
        req.setHeader("content-type", "application/xml");
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("application/xml", map.getElement("result.headers.content-type"));
        assertEquals("application/xml", map.getElement("result.headers.accept"));
        // xml key-values are parsed as text
        assertEquals("false", map.getElement("result.https"));
        assertEquals("/api/hello/list", map.getElement("result.url"));
        assertEquals("POST", map.getElement("result.method"));
        assertEquals("127.0.0.1", map.getElement("result.ip"));
        assertEquals(String.valueOf(RPC_TIMEOUT), map.getElement("result.headers.x-ttl"));
        assertInstanceOf(Map.class, map.getElement("result.body"));
        Map<String, Object> received = (Map<String, Object>) map.getElement("result.body");
        assertEquals(data, received);
    }

    @SuppressWarnings("unchecked")
    @Test
    void sendHttpDelete() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("DELETE");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        req.setHeader("accept", "application/json");
        req.setTimeoutSeconds((int) RPC_TIMEOUT / 1000);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("application/json", map.getElement("headers.accept"));
        assertEquals(false, map.getElement("https"));
        assertEquals("/api/hello/world", map.getElement("url"));
        assertEquals("DELETE", map.getElement("method"));
        assertEquals("127.0.0.1", map.getElement("ip"));
        assertEquals(String.valueOf(RPC_TIMEOUT), map.getElement("headers.x-ttl"));
        assertNull(map.getElement("body"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void sendHttpHeadWithCID() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String traceId = Utility.getInstance().getDateUuid();
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("HEAD");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        req.setHeader("accept", "application/json");
        req.setHeader("x-correlation-id", traceId);
        // prove that CR and LF will be filtered out
        req.setHeader("x-hello", "hello\r\nworld");
        req.setCookie("hello", "world");
        req.setCookie("another", "one");
        req.setCookie("invalid", "cookie\nnot\nallowed");
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        // HTTP head response may include custom headers and content-length
        assertEquals("HEAD request received", response.getHeader("X-Response"));
        assertEquals("100", response.getHeader("Content-Length"));
        // the same correlation-id is returned to the caller
        assertEquals(traceId, response.getHeader("X-Correlation-Id"));
        // multiple "set-cookie" headers are consolidated into one composite value
        assertEquals("first=cookie|second=one", response.getHeader("set-cookie"));
        var json = response.getHeader("x-cookies");
        assertInstanceOf(String.class, json);
        Map<String, String> restoredCookies = SimpleMapper.getInstance().getCompactGson().fromJson(json, Map.class);
        // prove that multiple cookies from user can be read and transported
        assertEquals(Map.of("hello", "world", "another", "one"), restoredCookies);
        var helloWorld = response.getHeader("x-hello");
        assertInstanceOf(String.class, helloWorld);
        assertEquals("hello world", helloWorld);
    }

    @Test
    void sendHttpHeadWithTraceId() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        String traceId = Utility.getInstance().getDateUuid();
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("HEAD");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        req.setHeader("accept", "application/json");
        req.setHeader("x-trace-id", traceId);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        Map<String, String> headers = response.getHeaders();
        // HTTP head response may include custom headers and content-length
        assertEquals("HEAD request received", headers.get("X-Response"));
        assertEquals("100", headers.get("Content-Length"));
        // the same correlation-id is returned to the caller
        assertEquals(traceId, headers.get("X-Trace-Id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void postXmlMap() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("POST");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        Map<String, Object> data = new HashMap<>();
        data.put("hello", "world");
        data.put("test", "message");
        req.setBody(data);
        req.setHeader("accept", "application/json");
        req.setHeader("content-type", "application/xml");
        req.setTimeoutSeconds((int) RPC_TIMEOUT / 1000);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("application/xml", map.getElement("headers.content-type"));
        assertEquals("application/json", map.getElement("headers.accept"));
        assertEquals(false, map.getElement("https"));
        assertEquals("/api/hello/world", map.getElement("url"));
        assertEquals("POST", map.getElement("method"));
        assertEquals("127.0.0.1", map.getElement("ip"));
        assertEquals(String.valueOf(RPC_TIMEOUT), map.getElement("headers.x-ttl"));
        assertInstanceOf(Map.class, map.getElement("body"));
        Map<String, Object> received = (Map<String, Object>) map.getElement("body");
        assertEquals(data, received);
    }

    @SuppressWarnings("unchecked")
    @Test
    void postList() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("POST");
        req.setUrl("/api/hello/world");
        req.setTargetHost("http://127.0.0.1:"+port);
        Map<String, Object> data = new HashMap<>();
        data.put("hello", "world");
        data.put("test", "message");
        req.setBody(Collections.singletonList(data));
        req.setHeader("accept", "application/json");
        req.setHeader("content-type", "application/json");
        req.setTimeoutSeconds((int) RPC_TIMEOUT / 1000);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("application/json", map.getElement("headers.content-type"));
        assertEquals("application/json", map.getElement("headers.accept"));
        assertEquals(false, map.getElement("https"));
        assertEquals("/api/hello/world", map.getElement("url"));
        assertEquals("POST", map.getElement("method"));
        assertEquals("127.0.0.1", map.getElement("ip"));
        assertEquals(String.valueOf(RPC_TIMEOUT), map.getElement("headers.x-ttl"));
        assertInstanceOf(List.class, map.getElement("body"));
        List<Map<String, Object>> received = (List<Map<String, Object>>) map.getElement("body");
        assertEquals(1, received.size());
        assertEquals(data, received.getFirst());
    }

    @Test
    void getIndexHtml() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setUrl("/index.html");
        req.setTargetHost("http://127.0.0.1:"+port);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals("text/html", response.getHeader("Content-Type"));
        assertInstanceOf(String.class, response.getBody());
        String html = (String) response.getBody();
        InputStream in = this.getClass().getResourceAsStream("/public/index.html");
        String content = util.stream2str(in);
        assertEquals(content, html);
        // this page is configured for "no cache" and ETag should not exist
        assertNull(response.getHeader("ETag"));
        // and it should have the cache-control headers
        assertEquals("no-cache, no-store", response.getHeader("Cache-Control"));
        assertEquals("no-cache", response.getHeader("Pragma"));
        assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", response.getHeader("Expires"));
    }

    @Test
    void getIndexWithoutExtension() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setUrl("/");
        req.setTargetHost("http://127.0.0.1:"+port);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals("text/html", response.getHeader("Content-Type"));
        assertInstanceOf(String.class, response.getBody());
        String html = (String) response.getBody();
        InputStream in = this.getClass().getResourceAsStream("/public/index.html");
        String content = util.stream2str(in);
        assertEquals(content, html);
        // the HTTP request filter will add a test header
        assertEquals("demo", response.getHeader("x-filter"));
        // this page is configured for "no cache" and ETag should not exist
        assertNull(response.getHeader("ETag"));
        // and it should have the cache-control headers
        assertEquals("no-cache, no-store", response.getHeader("Cache-Control"));
        assertEquals("no-cache", response.getHeader("Pragma"));
        assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", response.getHeader("Expires"));
    }

    @Test
    void getResourceDirectoryNotAllowed() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setTargetHost("http://127.0.0.1:"+port).setUrl("/assets");
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals("application/json", response.getHeader("Content-Type"));
        assertEquals(404, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
    }

    @Test
    void pathTraversalAvoidance() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        // "path traversal" avoidance - "../" in the URI path is dropped
        req.setTargetHost("http://127.0.0.1:"+port).setUrl("/test/%2e%2e/%2e%2e/index.html");
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals("application/json", response.getHeader("Content-Type"));
        assertEquals(404, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
    }

    @Test
    void getCssPage() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        // "path traversal" avoidance - "../" in the URI path is dropped
        req.setUrl("/assets/../another.css");
        req.setTargetHost("http://127.0.0.1:"+port);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals("text/css", response.getHeader("Content-Type"));
        assertInstanceOf(String.class, response.getBody());
        String html = (String) response.getBody();
        InputStream in = this.getClass().getResourceAsStream("/public/assets/another.css");
        String content = util.stream2str(in);
        assertEquals(content, html);
        // the HTTP request filter is not executed because ".css" extension is excluded in rest.yaml
        assertNull(response.getHeader("x-filter"));
        // this page is not configured for "no cache", thus there is a ETag response header
        assertNotNull(response.getHeader("ETag"));
    }

    @Test
    void getJsPage() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setUrl("/sample.js");
        req.setTargetHost("http://127.0.0.1:"+port);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals("text/javascript", response.getHeader("Content-Type"));
        assertInstanceOf(String.class, response.getBody());
        String html = (String) response.getBody();
        InputStream in = this.getClass().getResourceAsStream("/public/sample.js");
        String content = util.stream2str(in);
        assertEquals(content, html);
        // the HTTP request filter will add a test header
        assertEquals("demo", response.getHeader("x-filter"));
    }

    @Test
    void getXmlPage() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setUrl("/sample.xml").setHeader("x-raw-xml", "true");
        req.setTargetHost("http://127.0.0.1:"+port);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals("application/xml", response.getHeader("Content-Type"));
        assertInstanceOf(String.class, response.getBody());
        String html = (String) response.getBody();
        InputStream in = this.getClass().getResourceAsStream("/public/sample.xml");
        String content = util.stream2str(in);
        assertEquals(content, html);
    }

    @Test
    void getAssetPage() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        Utility util = Utility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setUrl("/assets/hello.txt");
        req.setTargetHost("http://127.0.0.1:"+port);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals("text/plain", response.getHeader("Content-Type"));
        assertInstanceOf(String.class, response.getBody());
        String text = (String) response.getBody();
        InputStream in = this.getClass().getResourceAsStream("/public/assets/hello.txt");
        String content = util.stream2str(in);
        assertEquals(content, text);
        // the HTTP request filter will add a test header
        assertEquals("demo", response.getHeader("x-filter"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAssetJSON() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new LinkedBlockingQueue<>();
        Utility util = Utility.getInstance();
        EventEmitter po = EventEmitter.getInstance();
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setUrl("/assets/hello.json");
        req.setTargetHost("http://127.0.0.1:"+port);
        EventEnvelope request = new EventEnvelope().setTo(AsyncHttpClient.ASYNC_HTTP_REQUEST).setBody(req);
        Future<EventEnvelope> res = po.asyncRequest(request, RPC_TIMEOUT);
        res.onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals("application/json", response.getHeader("Content-Type"));
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> data = (Map<String, Object>) response.getBody();
        InputStream in = this.getClass().getResourceAsStream("/public/assets/hello.json");
        String content = util.stream2str(in);
        Map<String, Object> source = SimpleMapper.getInstance().getMapper().readValue(content, Map.class);
        assertEquals(source, data);
    }

}
