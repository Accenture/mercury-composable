// Scaffolded from Mercury Composable's starter templates (https://github.com/Accenture/mercury-composable, Apache-2.0)

package com.accenture.starter;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GreetingFlowTest extends TestBase {
    private static final String HTTP_CLIENT = "async.http.request";
    private static final long TIMEOUT = 8000;

    @SuppressWarnings("unchecked")
    @Test
    void flowComposesGreeting() throws ExecutionException, InterruptedException {
        PostOffice po = PostOffice.trackable("unit.test", "200", "TEST /greeting");
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(host).setMethod("POST")
                .setHeader("content-type", "application/json")
                .setHeader("accept", "application/json")
                .setBody(Map.of("name", "Mercury"))
                .setUrl("/api/greeting");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("Hello, Mercury", body.get("greeting"));
        assertEquals("v1.make.greeting", body.get("served_by"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void invalidRequestReturns400() throws ExecutionException, InterruptedException {
        PostOffice po = PostOffice.trackable("unit.test", "201", "TEST /greeting");
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(host).setMethod("POST")
                .setHeader("content-type", "application/json")
                .setHeader("accept", "application/json")
                .setBody(Map.of("unexpected", "payload"))
                .setUrl("/api/greeting");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertEquals(400, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("error", body.get("type"));
    }
}
