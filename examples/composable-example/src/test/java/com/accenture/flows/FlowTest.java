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

import com.accenture.demo.models.Profile;
import com.accenture.support.TestBase;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class FlowTest extends TestBase {
    private static final Logger log = LoggerFactory.getLogger(FlowTest.class);
    private static final String HTTP_CLIENT = "async.http.request";

    @SuppressWarnings("unchecked")
    @Test
    public void endToEndFlowTest() throws IOException, ExecutionException, InterruptedException {
        final long TIMEOUT = 8000;
        final int PROFILE_ID = 300;
        PostOffice po = new PostOffice("unit.test", "1000", "TEST /flow/tests");
        // try to retrieve a non-exist profile will get HTTP-404
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json")
                .setUrl("/api/profile/no-such-profile");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertInstanceOf(Map.class, result.getBody());
        assertEquals(404, result.getStatus());
        Map<String, Object> data = (Map<String, Object>) result.getBody();
        assertEquals("Profile no-such-profile not found", data.get("message"));
        assertEquals("error", data.get("type"));
        // create a profile
        Profile profile = new Profile(Map.of("id", 300, "name", "Peter",
                        "address", "100 World Blvd", "telephone", "888-123-0000"));
        AsyncHttpRequest request1 = new AsyncHttpRequest();
        request1.setTargetHost(HOST).setMethod("POST")
                .setHeader("content-type", "application/json")
                .setHeader("accept", "application/json")
                .setBody(profile).setUrl("/api/profile");
        EventEnvelope req1 = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request1);
        EventEnvelope result1 = po.request(req1, TIMEOUT).get();
        assertInstanceOf(Map.class, result1.getBody());
        assertEquals(201, result1.getStatus());
        var mm1 = new MultiLevelMap((Map<String, Object>) result1.getBody());
        assertEquals(PROFILE_ID, mm1.getElement("profile.id"));
        assertEquals("Peter", mm1.getElement("profile.name"));
        assertEquals("***", mm1.getElement("profile.address"));
        assertEquals("***", mm1.getElement("profile.telephone"));
        assertEquals("CREATE", mm1.getElement("type"));
        // since "create profile" write operation is asynchronous, let's give it a brief moment to complete
        Thread.sleep(1000);
        // retrieve the profile
        AsyncHttpRequest request2 = new AsyncHttpRequest()
                .setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json")
                .setUrl("/api/profile/"+PROFILE_ID);
        EventEnvelope req2 = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request2);
        EventEnvelope result2 = po.request(req2, TIMEOUT).get();
        assertInstanceOf(Map.class, result2.getBody());
        assertEquals(200, result2.getStatus());
        var mm2 = new MultiLevelMap((Map<String, Object>) result2.getBody());
        assertEquals(PROFILE_ID, mm2.getElement("id"));
        assertEquals("Peter", mm2.getElement("name"));
        assertEquals("100 World Blvd", mm2.getElement("address"));
        assertEquals("888-123-0000", mm2.getElement("telephone"));
        // delete the profile
        AsyncHttpRequest request3 = new AsyncHttpRequest()
                .setTargetHost(HOST).setMethod("DELETE")
                .setHeader("accept", "application/json")
                .setUrl("/api/profile/"+PROFILE_ID);
        EventEnvelope req3 = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request3);
        EventEnvelope result3 = po.request(req3, TIMEOUT).get();
        assertInstanceOf(Map.class, result3.getBody());
        assertEquals(200, result3.getStatus());
        var mm3 = new MultiLevelMap((Map<String, Object>) result3.getBody());
        assertEquals(PROFILE_ID, mm3.getElement("id"));
        assertEquals(true, mm3.getElement("deleted"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void healthCheck() throws IOException, ExecutionException, InterruptedException {
        final long TIMEOUT = 8000;
        PostOffice po = new PostOffice("unit.test", "2000", "TEST /health/check");
        // try to retrieve a non-exist profile will get HTTP-404
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET")
                .setHeader("accept", "application/json")
                .setUrl("/health");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertInstanceOf(Map.class, result.getBody());
        assertEquals(200, result.getStatus());
        var mm = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("UP", mm.getElement("status"));
        assertEquals("demo.health", mm.getElement("dependency[0].route"));
        assertEquals("composable-example", mm.getElement("name"));
        assertEquals("I am running fine", mm.getElement("dependency[0].message.demo"));
    }
}
