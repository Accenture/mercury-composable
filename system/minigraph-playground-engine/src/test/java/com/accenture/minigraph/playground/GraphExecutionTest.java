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

package com.accenture.minigraph.playground;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GraphExecutionTest {
    private static final Utility util = Utility.getInstance();
    private static final AtomicInteger PORT = new AtomicInteger(0);
    private static final String JS = "js";
    private static final String MATH = "math";
    private static final String CONVERT = "convert";
    private static final long TIMEOUT = 8000;

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
        var config = AppConfigReader.getInstance();
        PORT.set(util.str2int(config.getProperty("rest.server.port", "8090")));
    }

    @Test
    void testGraphExecutionMath() throws ExecutionException, InterruptedException {
        testGraphExecution("hello", MATH);
        testGraphExecution("helloworld", CONVERT);
        testGraphExecution("helloworld2", MATH);
    }

    @Test
    void testGraphExecutionJs() throws ExecutionException, InterruptedException {
        testGraphExecution("hellojs", JS);
    }

    @SuppressWarnings("unchecked")
    private void testGraphExecution(String graphId, String type) throws ExecutionException, InterruptedException {
        var host = "http://127.0.0.1:" + PORT.get();
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(host);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type", "application/json");
        request.setUrl("/api/graph/"+graphId).setBody(Map.of("person_id", 100));
        var po = PostOffice.trackable("unit.test", "100", "TEST /api/graph/hello");
        var event = new EventEnvelope().setTo("async.http.request").setBody(request.toMap());
        var response = po.request(event, TIMEOUT).get();
        assertInstanceOf(Map.class, response.getBody());
        var mm = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("Peter", mm.getElement("name"));
        assertEquals("100 World Blvd", mm.getElement("address"));
        // number representation in JavaScript and Graph Math package is different
        if (JS.equals(type)) {
            // JavaScript returns Integer class when the operands are integers
            assertEquals(558, mm.getElement("sum"));
            assertEquals(50000, mm.getElement("multiply"));
        }
        if (MATH.equals(type)) {
            // For simplicity, Graph Math package always returns numbers in Double class
            assertEquals(558.0, mm.getElement("sum"));
            assertEquals(50000.0, mm.getElement("multiply"));
        }
        if (!CONVERT.equals(type)) {
            assertEquals(List.of("a101", "b202", "c303", "d400", "e500"), mm.getElement("accounts"));
            assertInstanceOf(List.class, mm.getElement("account_details"));
            var list = (List<String>) mm.getElement("account_details");
            assertEquals(5, list.size());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGraphException() throws ExecutionException, InterruptedException {
        var host = "http://127.0.0.1:" + PORT.get();
        var request = new AsyncHttpRequest().setMethod("POST").setTargetHost(host);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type", "application/json");
        request.setUrl("/api/graph/helloworld").setBody(Map.of("person_id", 10));
        var po = PostOffice.trackable("unit.test", "100", "TEST /api/graph/helloworld");
        var event = new EventEnvelope().setTo("async.http.request").setBody(request.toMap());
        var response = po.request(event, TIMEOUT).get();
        assertInstanceOf(Map.class, response.getBody());
        assertEquals(400, response.getStatus());
        var mm = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("error", mm.getElement("type"));
        assertEquals("Profile 10 not found", mm.getElement("message"));
    }
}
