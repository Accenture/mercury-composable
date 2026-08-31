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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.EventStreamWriter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The producer-side contract: each segment is one event to the caller's reply_to,
 * marked with x-event-stream; the first event carries the head control (status,
 * content type, optional idle ttl); writes after close are dropped.
 */
class EventStreamWriterTest extends TestBase {

    @EventInterceptor
    private record CaptureRoute(BlockingQueue<EventEnvelope> received) implements LambdaFunction {
        @Override
        public Object handleEvent(Map<String, String> headers, Object input, int instance) {
            received.add((EventEnvelope) input);
            return null;
        }
    }

    private EventEnvelope next(BlockingQueue<EventEnvelope> queue) throws InterruptedException {
        EventEnvelope event = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(event, "expected a captured segment");
        return event;
    }

    @Test
    void writerSpeaksTheMultiShotReplyRouteProtocol() throws InterruptedException {
        var platform = Platform.getInstance();
        var captured = new LinkedBlockingQueue<EventEnvelope>();
        String route = "capture.stream.protocol";
        platform.registerPrivate(route, new CaptureRoute(captured), 1);
        try {
            var out = new EventStreamWriter(route + "@" + platform.getOrigin(), "cid-100");
            out.first(200, "text/event-stream", 30);
            assertFalse(out.isClosed());
            out.write("Hello");
            out.write("tokens", Map.of("n", 2));
            out.close(Map.of("usage", 42));
            assertTrue(out.isClosed());
            out.write("late segment must be dropped");
            out.fail(new AppException(500, "fail after close is a no-op"));

            EventEnvelope first = next(captured);
            assertEquals("cid-100", first.getCorrelationId());
            assertEquals(EventStreamWriter.DATA, first.getHeaders().get(EventStreamWriter.X_EVENT_STREAM));
            assertEquals("text/event-stream", first.getHeaders().get("content-type"));
            assertEquals("30", first.getHeaders().get("x-ttl"));
            assertEquals(200, first.getStatus());
            assertEquals("Hello", first.getRawBody());

            EventEnvelope second = next(captured);
            assertEquals(EventStreamWriter.DATA, second.getHeaders().get(EventStreamWriter.X_EVENT_STREAM));
            assertEquals("tokens", second.getHeaders().get(EventStreamWriter.X_EVENT_NAME));
            assertNull(second.getHeaders().get("content-type"), "head control rides the first event only");
            assertEquals(2, ((Map<?, ?>) second.getRawBody()).get("n"));

            EventEnvelope eof = next(captured);
            assertEquals(EventStreamWriter.EOF, eof.getHeaders().get(EventStreamWriter.X_EVENT_STREAM));
            assertEquals(42, ((Map<?, ?>) eof.getRawBody()).get("usage"));

            Utility.getInstance().sleep(200);
            assertTrue(captured.isEmpty(), "segments after close must be dropped");
        } finally {
            platform.release(route);
        }
    }

    @Test
    void failMapsExceptionsToTheInBandErrorContract() throws InterruptedException {
        var platform = Platform.getInstance();
        var captured = new LinkedBlockingQueue<EventEnvelope>();
        String route = "capture.stream.failure";
        platform.registerPrivate(route, new CaptureRoute(captured), 1);
        try {
            var out = new EventStreamWriter(route + "@" + platform.getOrigin(), "cid-200");
            out.fail(new AppException(429, "slow down"));
            assertTrue(out.isClosed());

            EventEnvelope error = next(captured);
            assertEquals(EventStreamWriter.EXCEPTION, error.getHeaders().get(EventStreamWriter.X_EVENT_STREAM));
            assertEquals(429, error.getStatus());
            var body = (Map<?, ?>) error.getRawBody();
            assertEquals(429, body.get("status"));
            assertEquals("slow down", body.get("message"));
        } finally {
            platform.release(route);
        }
    }

    @Test
    void writerRequiresAReplyRoute() {
        assertThrows(IllegalArgumentException.class, () -> new EventStreamWriter(null, "cid"));
        var requestWithoutReplyTo = new EventEnvelope();
        assertThrows(IllegalArgumentException.class, () -> new EventStreamWriter(requestWithoutReplyTo));
    }
}
