// Scaffolded from Mercury Composable's starter templates (https://github.com/Accenture/mercury-composable, Apache-2.0)

package com.accenture.starter;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GreetingTest extends TestBase {
    private static final String HTTP_CLIENT = "async.http.request";
    private static final long TIMEOUT = 8000;

    @Test
    void functionWorksInIsolation() {
        // a composable function is plain Java - unit test it without the framework
        Greeting greeting = new Greeting();
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setUrl("/api/greeting").setQueryParameter("name", "Mercury");
        Map<String, Object> result = greeting.handleEvent(Map.of(), request, 1);
        assertEquals("Hello, Mercury", result.get("greeting"));
        assertEquals("v1.greeting", result.get("served_by"));
    }

    @Test
    void missingNameIsRejectedInIsolation() {
        Greeting greeting = new Greeting();
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setUrl("/api/greeting");
        AppException ex = assertThrows(AppException.class, () -> greeting.handleEvent(Map.of(), request, 1));
        assertEquals(400, ex.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    void endToEndOverHttp() throws ExecutionException, InterruptedException {
        PostOffice po = PostOffice.trackable("unit.test", "100", "TEST /greeting");
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(host).setMethod("GET")
                .setHeader("accept", "application/json")
                .setQueryParameter("name", "Mercury")
                .setUrl("/api/greeting");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("Hello, Mercury", body.get("greeting"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void missingNameReturns400OverHttp() throws ExecutionException, InterruptedException {
        PostOffice po = PostOffice.trackable("unit.test", "101", "TEST /greeting");
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(host).setMethod("GET")
                .setHeader("accept", "application/json")
                .setUrl("/api/greeting");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertEquals(400, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("error", body.get("type"));
    }
}
