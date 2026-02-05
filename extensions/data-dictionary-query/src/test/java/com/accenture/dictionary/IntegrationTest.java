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

package com.accenture.dictionary;

import com.accenture.dictionary.models.QuestionSpecs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static String host;

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
        var config = AppConfigReader.getInstance();
        var port = config.getProperty("rest.server.port", "8080");
        host = "http://127.0.0.1:" + port;
    }

    @SuppressWarnings("unchecked")
    @Test
    void questionWithSingleEndpointTest() throws ExecutionException, InterruptedException {
        var uri = "/api/data/get-accounts";
        var request = new AsyncHttpRequest();
        request.setMethod("POST").setTargetHost(host).setUrl(uri);
        request.setHeader("Accept", "application/json").setHeader("Content-Type", "application/json");
        request.setBody(Map.of("person_id", 100));
        var po = PostOffice.trackable("unit.test", "100", "TEST /question");
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var response = po.request(event, 8000).get();
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        var mm = new MultiLevelMap(body);
        assertEquals(List.of("a101", "b202", "c303", "d400", "e500"), mm.getElement("account_id"));
        assertEquals(100, mm.getElement("person_id"));
        assertEquals("Peter", mm.getElement("name"));
        var details = mm.getElement("account_details");
        assertInstanceOf(List.class, details);
        var balances = mm.getElement("$.account_details[*].balance");
        assertInstanceOf(List.class, balances);
        var balanceList = (List<Double>) balances;
        Collections.sort(balanceList);
        assertEquals(List.of(6000.0, 6020.68, 8200.0, 25032.13, 120000.0), balanceList);
        // test with invalid person_id
        request.setBody(Map.of("person_id", 200));
        event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        response = po.request(event, 8000).get();
        assertEquals(400, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        body = (Map<String, Object>) response.getBody();
        assertEquals("error", body.get("type"));
        assertEquals(400, body.get("status"));
        assertEquals("classpath:/mock/profile-200.json not found", body.get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void questionSpecEndpointTest() throws ExecutionException, InterruptedException {
        var uri = "/api/specs/question/get-accounts";
        var request = new AsyncHttpRequest();
        request.setMethod("GET").setTargetHost(host).setUrl(uri);
        request.setHeader("Accept", "application/json");
        var po = PostOffice.trackable("unit.test", "200", "TEST /specs/question");
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var response = po.request(event, 8000).get();
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        var mm = new MultiLevelMap(body);
        assertTrue(mm.exists("purpose"));
        assertInstanceOf(List.class, mm.getElement("questions"));
        var specs = response.getBody(QuestionSpecs.class);
        assertEquals(3, specs.concurrency);
    }

    @SuppressWarnings("unchecked")
    @Test
    void dataSpecEndpointTest() throws ExecutionException, InterruptedException {
        var uri = "/api/specs/data/person_name";
        var request = new AsyncHttpRequest();
        request.setMethod("GET").setTargetHost(host).setUrl(uri);
        request.setHeader("Accept", "application/json");
        var po = PostOffice.trackable("unit.test", "300", "TEST /spec/data");
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var response = po.request(event, 8000).get();
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertInstanceOf(List.class, body.get("input"));
        assertInstanceOf(List.class, body.get("output"));
        assertEquals("person_name", body.get("id"));
        assertEquals("mdm://profile", body.get("target"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void providerSpecEndpointTest() throws ExecutionException, InterruptedException {
        var uri = "/api/specs/provider/account/details";
        var request = new AsyncHttpRequest();
        request.setMethod("GET").setTargetHost(host).setUrl(uri);
        request.setHeader("Accept", "application/json");
        var po = PostOffice.trackable("unit.test", "400", "TEST /spec/provider");
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request);
        var response = po.request(event, 8000).get();
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertInstanceOf(List.class, body.get("input"));
        assertInstanceOf(List.class, body.get("headers"));
        assertEquals("account://details", body.get("id"));
        assertEquals("POST", body.get("method"));
        assertEquals("account", body.get("protocol"));
        assertEquals("details", body.get("service"));
    }
}
