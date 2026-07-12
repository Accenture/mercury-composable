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

package org.platformlambda.demo;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.platformlambda.demo.common.TestBase;
import org.platformlambda.models.ObjectWithGenericType;
import org.platformlambda.models.SamplePoJo;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the demo services that the earlier tests did not reach: health probe, generic PoJo
 * holder, simple request parser, authentication demo and file download streaming.
 */
class ServiceTopUpTest extends TestBase {

    private static final long RPC_TIMEOUT = 8000;

    private EventEnvelope call(String route, Object body, Map<String, String> headers)
            throws InterruptedException, ExecutionException {
        PostOffice po = new PostOffice("unit.test", Utility.getInstance().getUuid(), "TEST /" + route);
        EventEnvelope request = new EventEnvelope().setTo(route).setBody(body);
        if (headers != null) {
            headers.forEach(request::setHeader);
        }
        return po.request(request, RPC_TIMEOUT).get();
    }

    @SuppressWarnings("unchecked")
    @Test
    void healthProbeAnswersInfoAndHealth() throws InterruptedException, ExecutionException {
        EventEnvelope info = call("demo.health", "ping", Map.of("type", "info"));
        assertInstanceOf(Map.class, info.getBody());
        assertEquals("demo.service", ((Map<String, Object>) info.getBody()).get("service"));
        EventEnvelope health = call("demo.health", "ping", Map.of("type", "health"));
        assertEquals("demo.service is running fine", health.getBody());
        EventEnvelope invalid = call("demo.health", "ping", Map.of("type", "nonsense"));
        assertTrue(invalid.getStatus() >= 400);
    }

    @SuppressWarnings("unchecked")
    @Test
    void genericPoJoHolderRoundTrip() throws InterruptedException, ExecutionException {
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setUrl("/api/generic/1");
        req.setPathParameter("id", "1");
        EventEnvelope response = call("hello.generic", req.toMap(), null);
        assertEquals(200, response.getStatus());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("Generic class with parametric type SamplePoJo", map.getElement("content.name"));
        // any other id hits the not-found path
        req.setPathParameter("id", "2");
        EventEnvelope notFound = call("hello.generic", req.toMap(), null);
        assertEquals(404, notFound.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    void simpleEndpointParsesPathAndQuery() throws InterruptedException, ExecutionException {
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setUrl("/api/simple/foo/last");
        req.setPathParameter("task", "foo");
        req.setQueryParameter("a", "1");
        req.setQueryParameter("b", "2");
        EventEnvelope response = call("hello.simple", req.toMap(), null);
        assertEquals(200, response.getStatus());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("foo", map.getElement("task"));
        assertEquals("last", map.getElement("last_path_parameter"));
        assertEquals("1", map.getElement("query.a"));
        // missing query parameters are rejected
        AsyncHttpRequest incomplete = new AsyncHttpRequest();
        incomplete.setMethod("GET");
        incomplete.setUrl("/api/simple/foo/last");
        EventEnvelope error = call("hello.simple", incomplete.toMap(), null);
        assertTrue(error.getStatus() >= 400);
    }

    @Test
    void authDemoAcceptsAndTagsTheSession() throws InterruptedException, ExecutionException {
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setUrl("/api/hello/generic/1");
        EventEnvelope response = call("v1.api.auth", req.toMap(), null);
        assertEquals(Boolean.TRUE, response.getBody());
        assertEquals("demo", response.getHeaders().get("user"));
    }

    @Test
    void fileDownloadStreamsTheSampleFile() throws InterruptedException, ExecutionException {
        AsyncHttpRequest req = new AsyncHttpRequest();
        req.setMethod("GET");
        req.setUrl("/api/download");
        req.setRemoteIp("127.0.0.1");
        EventEnvelope response = call("hello.download", req.toMap(), null);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getHeaders().get("x-stream-id"));
        assertTrue(response.getHeaders().get("Content-Disposition").contains("helloworld.txt"));
    }

    @Test
    void samplePoJoModelRoundTrip() {
        Date now = new Date();
        SamplePoJo pojo = new SamplePoJo(1, "name", "address");
        pojo.setDate(now);
        pojo.setInstance(2);
        pojo.setSeq(3);
        pojo.setOrigin("origin");
        assertEquals(1, pojo.getId());
        assertEquals("name", pojo.getName());
        assertEquals("address", pojo.getAddress());
        assertEquals(now, pojo.getDate());
        assertEquals(2, pojo.getInstance());
        assertEquals(3, pojo.getSeq());
        assertEquals("origin", pojo.getOrigin());
        ObjectWithGenericType<SamplePoJo> holder = new ObjectWithGenericType<>();
        holder.setContent(pojo);
        holder.setId(9);
        assertEquals(9, holder.getId());
        assertEquals(pojo, holder.getContent());
    }
}
