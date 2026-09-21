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
import org.platformlambda.demo.common.TestBase;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The request-response demo: hello.rpc calls hello.world and returns its echo; a budget shorter than the
 * callee's work surfaces as a 408 to the caller.
 */
class HelloRpcTest extends TestBase {

    @SuppressWarnings("unchecked")
    @Test
    void rpcReturnsTheCalleesReply() throws InterruptedException, ExecutionException {
        AsyncHttpRequest http = new AsyncHttpRequest().setMethod("POST").setUrl("/api/hello/rpc")
                .setHeader("content-type", "application/json").setBody(Map.of("name", "rpc"));
        PostOffice po = new PostOffice("unit.test", "20001", "POST /api/hello/rpc");
        EventEnvelope response = po.request(new EventEnvelope().setTo("hello.rpc").setBody(http), 8000).get();
        assertEquals(200, response.getStatus());
        assertInstanceOf(Map.class, response.getBody());
        MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
        assertEquals("hello.world", map.getElement("callee"));
        assertEquals(5000L, ((Number) map.getElement("timeout_ms")).longValue());
        // hello.world echoes the request body under "body"
        assertEquals("rpc", map.getElement("reply.body.name"));
        assertTrue(((Number) map.getElement("round_trip_ms")).longValue() >= 0);
    }

    @Test
    void aBudgetShorterThanTheCalleesWorkIsA408() throws InterruptedException, ExecutionException {
        // hello.world sleeps for sleep_ms; a 300 ms budget expires first and the RPC timeout reaches the
        // caller as a 408 reply (the platform resolves the status from the TimeoutException on the chain)
        AsyncHttpRequest http = new AsyncHttpRequest().setMethod("POST").setUrl("/api/hello/rpc")
                .setQueryParameter("timeout", "300")
                .setHeader("content-type", "application/json").setBody(Map.of("sleep_ms", 1500));
        PostOffice po = new PostOffice("unit.test", "20002", "POST /api/hello/rpc");
        EventEnvelope response = po.request(new EventEnvelope().setTo("hello.rpc").setBody(http), 8000).get();
        assertEquals(408, response.getStatus());
        assertTrue(String.valueOf(response.getError()).contains("Timeout for 300 ms"),
                "unexpected error: " + response.getError());
    }
}
