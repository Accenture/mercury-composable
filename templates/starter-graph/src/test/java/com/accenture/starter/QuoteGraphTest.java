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

class QuoteGraphTest extends TestBase {
    private static final String HTTP_CLIENT = "async.http.request";
    private static final long TIMEOUT = 10000;

    @SuppressWarnings("unchecked")
    @Test
    void deployedGraphAnswersQuote() throws ExecutionException, InterruptedException {
        PostOffice po = PostOffice.trackable("unit.test", "300", "TEST /graph/quote");
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(host).setMethod("POST")
                .setHeader("content-type", "application/json")
                .setHeader("accept", "application/json")
                .setBody(Map.of("item", "widget"))
                .setUrl("/api/graph/starter-quote");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertEquals(200, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("widget", body.get("item"));
        assertEquals(100, body.get("unit_price"));
        assertEquals("quoted", body.get("status"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void missingItemIsRejected() throws ExecutionException, InterruptedException {
        PostOffice po = PostOffice.trackable("unit.test", "301", "TEST /graph/quote");
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(host).setMethod("POST")
                .setHeader("content-type", "application/json")
                .setHeader("accept", "application/json")
                .setBody(Map.of("unexpected", "payload"))
                .setUrl("/api/graph/starter-quote");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertEquals(400, result.getStatus());
        assertInstanceOf(Map.class, result.getBody());
        // a graph that sets output.status 400+ yields the standard error response
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertEquals("Missing item. Provide the item name in the 'item' field", body.get("message"));
    }

    @Test
    void unlistedGraphIs404() throws ExecutionException, InterruptedException {
        // "compiled or 404": a graph absent from the manifest behaves as nonexistent
        PostOffice po = PostOffice.trackable("unit.test", "302", "TEST /graph/quote");
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(host).setMethod("POST")
                .setHeader("content-type", "application/json")
                .setHeader("accept", "application/json")
                .setBody(Map.of("item", "widget"))
                .setUrl("/api/graph/no-such-graph");
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        EventEnvelope result = po.request(req, TIMEOUT).get();
        assertEquals(404, result.getStatus());
    }
}
