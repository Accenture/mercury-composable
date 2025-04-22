package org.platformlambda.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.EventEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTransportTest extends TestBase {

    private static final String ROUTE = "exception.test";
    private static final String CALLBACK = "callback.function";
    private static final String DEMO = "demo";
    private static final BlockingQueue<EventEnvelope> callbackBench = new ArrayBlockingQueue<>(1);

    @BeforeAll
    public static void setup() throws IOException {
        Platform platform = Platform.getInstance();
        LambdaFunction f = (headers, input, instance) -> {
            throw new IllegalArgumentException("demo");
        };
        platform.registerPrivate(ROUTE, f, 1);
    }

    @Test
    void transportTest() throws IOException, InterruptedException {
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope request = new EventEnvelope().setTo(ROUTE).setBody("demo");
        po.asyncRequest(request, 5000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(400, response.getStatus());
        assertEquals(DEMO, response.getError());

    }

    @Test
    void callbackExceptionTest() throws IOException, InterruptedException {
        Platform platform = Platform.getInstance();
        platform.registerPrivate(CALLBACK, new MyCallBack(), 1);
        EventEnvelope request = new EventEnvelope();
        request.setTo(ROUTE);
        request.setReplyTo(CALLBACK);
        request.setBody("ok");
        EventEmitter po = EventEmitter.getInstance();
        po.send(request);
        EventEnvelope result = callbackBench.poll(10, TimeUnit.SECONDS);
        assertNotNull(result);
        assertNotNull(result.getException());
        assertInstanceOf(IllegalArgumentException.class, result.getException());
        assertEquals(DEMO, result.getException().getMessage());
    }

    private static class MyCallBack implements TypedLambdaFunction<EventEnvelope, Object> {

        @Override
        public Object handleEvent(Map<String, String> headers, EventEnvelope body, int instance) {
            callbackBench.add(body);
            return true;
        }
    }
}
