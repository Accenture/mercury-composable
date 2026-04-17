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

package com.accenture.minigraph.tests;

import com.accenture.minigraph.start.MainApp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Just some examples to show how to do unit test to evaluate graph models
 */
class GraphTests {
    private static final Logger log = LoggerFactory.getLogger(GraphTests.class);
    private static final long TIMEOUT = 5000;
    private static String target;

    @BeforeAll
    static void beforeAll() {
        MainApp.main(new String[0]);
        var config = AppConfigReader.getInstance();
        target = "http://127.0.0.1:"+config.getProperty("rest.server.port");
    }


    @Test
    void tutorial1() throws TimeoutException {
        var result = runTutorial(1, Map.of());
        assertEquals("hello world", result);
        log.info("Tutorial 1 works");
    }

    @Test
    void tutorial2() throws TimeoutException {
        var result = runTutorial(2, Map.of("hello", "world"));
        assertEquals(Map.of("hello", "world"), result);
        log.info("Tutorial 2 works");
    }

    @Test
    void tutorial3() throws TimeoutException {
        var result = runTutorial(3, Map.of("person_id", 100));
        assertEquals(Map.of("name", "Peter", "address", "100 World Blvd"), result);
        log.info("Tutorial 3 works");
    }

    @SuppressWarnings("unchecked")
    @Test
    void tutorial3Negative() throws TimeoutException {
        var result = runTutorial(3, Map.of("person_id", 10));
        assertInstanceOf(Map.class, result);
        var mm = new MultiLevelMap((Map<String, Object>) result);
        assertEquals("Profile 10 not found", mm.getElement("message"));
        assertEquals(400, mm.getElement("status"));
        assertEquals("error", mm.getElement("type"));
        log.info("Tutorial 3 fails");
    }

    @SuppressWarnings("unchecked")
    @Test
    void tutorial4() throws TimeoutException {
        var result = runTutorial(4, Map.of("a", 100, "b", 200));
        assertInstanceOf(Map.class, result);
        var mm = new MultiLevelMap((Map<String, Object>) result);
        assertEquals("a < b", mm.getElement("message"));
        assertEquals(300.0, mm.getElement("sum"));
        assertEquals(true, mm.getElement("less_than"));
        log.info("Tutorial 4 works");
    }

    @SuppressWarnings("unchecked")
    @Test
    void tutorial5() throws TimeoutException {
        var result = runTutorial(5, Map.of("person1", 100, "person2", 200));
        assertInstanceOf(Map.class, result);
        var mm = new MultiLevelMap((Map<String, Object>) result);
        assertInstanceOf(List.class, mm.getElement("profile"));
        var list = (List<Map<String, Object>>) mm.getElement("profile");
        assertEquals(2, list.size());
        for (Map<String, Object> profile : list) {
            var name = profile.get("name");
            var address = profile.get("address");
            assertTrue(name.equals("Peter") || name.equals("Mary"));
            if ("Peter".equals(name)) {
                assertEquals("100 World Blvd", address);
            }
            if ("Mary".equals(name)) {
                assertEquals("200 World Blvd", address);
            }
        }
        log.info("Tutorial 5 works");
    }

    @SuppressWarnings("unchecked")
    @Test
    void tutorial6() throws TimeoutException {
        var result = runTutorial(6, Map.of("person_id", 100));
        assertInstanceOf(Map.class, result);
        var mm = new MultiLevelMap((Map<String, Object>) result);
        System.out.println(result);
        assertEquals("Peter", mm.getElement("name"));
        assertEquals("100 World Blvd", mm.getElement("address"));
        assertInstanceOf(List.class, mm.getElement("accounts"));
        var list = (List<Map<String, Object>>) mm.getElement("accounts");
        assertEquals(5, list.size());
        for (Map<String, Object> account : list) {
            var type = account.get("type");
            var id = account.get("id");
            var balance = account.get("balance");
            assertTrue(id.equals("a101") || id.equals("b202") || id.equals("c303") || id.equals("d400") || id.equals("e500"));
            if ("a101".equals(id)) {
                assertEquals("Saving", type);
                assertEquals(25032.13, balance);
            }
            if ("b202".equals(id)) {
                assertEquals("Current", type);
                assertEquals(6020.68, balance);
            }
            if ("c303".equals(id)) {
                assertEquals("C/D", type);
                assertEquals(120000.0, balance);
            }
            if ("d400".equals(id)) {
                assertEquals("apple", type);
                assertEquals(6000.0, balance);
            }
            if ("e500".equals(id)) {
                assertEquals("google", type);
                assertEquals(8200.0, balance);
            }
        }
        log.info("Tutorial 6 works");
    }

    @SuppressWarnings("unchecked")
    @Test
    void tutorial7() throws TimeoutException {
        var data = new HashMap<String, Object>();
        data.put("profile",
                Map.of("name", "Peter", "address1", "100 World Blvd", "address2", "New York"));
        var result = runTutorial(7, data);
        assertInstanceOf(Map.class, result);
        var mm = new MultiLevelMap((Map<String, Object>) result);
        assertEquals("Peter", mm.getElement("name"));
        assertEquals("world", mm.getElement("hello"));
        assertEquals("100 World Blvd", mm.getElement("address[0]"));
        assertEquals("New York", mm.getElement("address[1]"));
        log.info("Tutorial 7 works");
    }

    @SuppressWarnings("unchecked")
    @Test
    void tutorial8() throws TimeoutException {
        var text = """
                {
                  "profile": {
                    "name": "Peter",
                    "account": [
                      {
                        "id": "100",
                        "amount": 18000.30,
                        "description": "Time deposit",
                        "type": "C/D"
                      },
                      {
                        "id": "200",
                        "amount": 62050.80,
                        "description": "Saving account",
                        "type": "Saving"
                      }
                    ]
                  }
                }
                """;
        var data = SimpleMapper.getInstance().getMapper().readValue(text, Map.class);
        var result = runTutorial(8, data);
        assertInstanceOf(Map.class, result);
        var mm = new MultiLevelMap((Map<String, Object>) result);
        assertEquals("Peter", mm.getElement("name"));
        assertInstanceOf(List.class, mm.getElement("account"));
        var list = (List<Map<String, Object>>) mm.getElement("account");
        assertEquals(2, list.size());
        assertEquals(18000.3, mm.getElement("account[0].amount"));
        assertEquals(62050.8, mm.getElement("account[1].amount"));
        assertEquals("100", mm.getElement("account[0].id"));
        assertEquals("200", mm.getElement("account[1].id"));
        assertEquals("C/D", mm.getElement("account[0].type"));
        assertEquals("Saving", mm.getElement("account[1].type"));
        log.info("Tutorial 8 works");
    }

    @SuppressWarnings("unchecked")
    @Test
    void tutorial9() throws TimeoutException {
        var result = runTutorial(9, Map.of("a", 10, "b", 20));
        assertInstanceOf(Map.class, result);
        var mm = new MultiLevelMap((Map<String, Object>) result);
        assertEquals(30.0, mm.getElement("sum"));
        log.info("Tutorial 9 works");
    }

    @SuppressWarnings("unchecked")
    @Test
    void tutorial10() throws TimeoutException {
        var result = runTutorial(10, Map.of("person_id", 100));
        assertInstanceOf(Map.class, result);
        var mm = new MultiLevelMap((Map<String, Object>) result);
        assertEquals("Peter", mm.getElement("name"));
        assertEquals("100 World Blvd", mm.getElement("address"));
        log.info("Tutorial 10 works");
    }

    @SuppressWarnings("unchecked")
    @Test
    void tutorial11() throws TimeoutException {
        var result = runTutorial(11, Map.of("hello", "world", "message", "this is a good day"));
        assertInstanceOf(Map.class, result);
        var mm = new MultiLevelMap((Map<String, Object>) result);
        assertEquals("world", mm.getElement("hello"));
        assertEquals("this is a good day", mm.getElement("message"));
        log.info("Tutorial 11 works");
    }

    @SuppressWarnings("unchecked")
    @Test
    void tutorial12() throws TimeoutException {
        var result = runTutorial(12, Map.of("person_id", 100, "exception", true));
        assertInstanceOf(Map.class, result);
        var mm = new MultiLevelMap((Map<String, Object>) result);
        assertEquals("Peter", mm.getElement("name"));
        assertEquals("100 World Blvd", mm.getElement("address"));
        log.info("Tutorial 12 works");
    }

    private Object runTutorial(int chapter, Map<String, Object> input) throws TimeoutException {
        var request = new AsyncHttpRequest().setMethod(input.isEmpty()? "GET" : "POST").setTargetHost(target);
        if (!input.isEmpty()) {
            request.setBody(input).setHeader("Content-Type", "application/json");
        }
        request.setHeader("Accept", "application/json");
        request.setUrl("/api/graph/tutorial-"+chapter);
        var event = new EventEnvelope().setTo("async.http.request").setBody(request);
        var po = PostOffice.trackable("unit.test", "ch-"+chapter, "TEST /chapter/"+chapter);
        var response = po.asyncRequest(event, TIMEOUT).await(TIMEOUT, TimeUnit.MILLISECONDS);
        if (response.hasError()) {
            log.error("HTTP-{} - {}", response.getStatus(), response.getBody());
        }
        return response.getBody();
    }
}
