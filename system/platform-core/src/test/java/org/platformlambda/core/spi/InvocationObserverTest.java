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

package org.platformlambda.core.spi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The live invocation observer seam: what the engine calls, on which thread, in which order.
 */
class InvocationObserverTest extends TestBase {
    private static final String PREFIX = "obs.test.";
    private static final String ECHO = PREFIX + "echo";
    private static final String FAIL = PREFIX + "fail";
    private static final String DEFERRED = PREFIX + "deferred";
    private static final String NEVER = PREFIX + "never";
    private static final String ONLY = PREFIX + "only";
    private static final String TAG = "rec_tag";
    private static final long TIMEOUT = 5000;
    private static final AtomicBoolean routesRegistered = new AtomicBoolean(false);
    private final List<InvocationObserver> registered = new ArrayList<>();

    @BeforeEach
    void setup() {
        InvocationObservers.configure(null, null, null);
        if (!routesRegistered.compareAndSet(false, true)) {
            return;
        }
        Platform platform = Platform.getInstance();
        LambdaFunction echo = (headers, input, instance) -> input;
        LambdaFunction fail = (headers, input, instance) -> {
            throw new AppException(400, "demo failure");
        };
        LambdaFunction deferred = (headers, input, instance) ->
                Mono.delay(Duration.ofMillis(300)).map(ignored -> "late " + input);
        LambdaFunction never = (headers, input, instance) -> Mono.never();
        platform.registerPrivate(ECHO, echo, 1);
        platform.registerPrivate(FAIL, fail, 1);
        platform.registerPrivate(DEFERRED, deferred, 1);
        platform.registerPrivate(NEVER, never, 1);
        platform.registerPrivate(ONLY, echo, 1);
    }

    @AfterEach
    void cleanup() {
        registered.forEach(InvocationObservers::unregister);
        registered.clear();
        InvocationObservers.reload();
    }

    @Test
    void observesATracedInvocationOnOneThread() throws Exception {
        Recorder recorder = register(new Recorder("a"));

        EventEnvelope response = request(ECHO, "hello", "trace-101");

        assertEquals("hello", response.getBody());
        Record begin = recorder.await(r -> r.is("begin", ECHO));
        Record end = recorder.await(r -> r.is("end", ECHO));
        assertEquals("trace-101", begin.traceId);
        // the sender's tag reached the receiving function: the linkage rides on the envelope
        assertEquals("sent-to-" + ECHO, begin.tag);
        assertEquals(begin.thread, end.thread);
        assertTrue(recorder.find(r -> r.is("send", ECHO)) != null);
        assertNull(recorder.find(r -> r.is("error", ECHO)));
    }

    @Test
    void ignoresAnEventWithoutATrace() throws Exception {
        Recorder recorder = register(new Recorder("a"));

        EventEnvelope response = EventEmitter.getInstance()
                .eRequest(new EventEnvelope().setTo(ECHO).setBody("plain"), TIMEOUT).get(TIMEOUT, TimeUnit.MILLISECONDS);

        assertEquals("plain", response.getBody());
        Utility.getInstance().sleep(200);
        assertNull(recorder.find(r -> r.route.equals(ECHO)));
    }

    @Test
    void recordsTheErrorBeforeTheEnd() throws Exception {
        Recorder recorder = register(new Recorder("a"));

        EventEnvelope response = request(FAIL, "x", "trace-102");

        assertEquals(400, response.getStatus());
        Record error = recorder.await(r -> r.is("error", FAIL));
        Record end = recorder.await(r -> r.is("end", FAIL));
        assertEquals("demo failure", error.detail);
        assertTrue(error.seq < end.seq);
        assertEquals(error.thread, end.thread);
    }

    @Test
    void resumesAMonoOnItsCompletionThread() throws Exception {
        Recorder recorder = register(new Recorder("a"));

        EventEnvelope response = request(DEFERRED, "news", "trace-103");

        assertEquals("late news", response.getBody());
        Record begin = recorder.await(r -> r.is("begin", DEFERRED));
        Record detach = recorder.await(r -> r.is("detach", DEFERRED));
        Record end = recorder.await(r -> r.is("end", DEFERRED));
        Record resume = recorder.await(r -> r.is("resume", DEFERRED));
        Record resumedEnd = recorder.await(r -> r.is("resumed-end", DEFERRED));
        // the hop closes on its own thread when the function returns; the continuation opens later
        assertEquals(begin.thread, detach.thread);
        assertEquals(begin.thread, end.thread);
        assertTrue(end.seq < resume.seq);
        assertEquals(resume.thread, resumedEnd.thread);
        assertEquals(1, recorder.count(r -> r.is("resume", DEFERRED)));
    }

    @Test
    void abandonsAMonoThatNeverCompletes() throws Exception {
        Recorder recorder = register(new Recorder("a"));
        EventEnvelope event = new EventEnvelope().setTo(NEVER).setBody("x").setTrace("trace-104", "GET /never");

        Exception e = assertThrows(ExecutionException.class,
                () -> EventEmitter.getInstance().eRequest(event, 500).get(TIMEOUT, TimeUnit.MILLISECONDS));

        assertInstanceOf(TimeoutException.class, e.getCause());
        Record error = recorder.await(r -> r.is("resumed-error", NEVER));
        recorder.await(r -> r.is("resumed-end", NEVER));
        assertTrue(error.detail.startsWith("Async response"));
        assertEquals(1, recorder.count(r -> r.is("resume", NEVER)));
    }

    @Test
    void theExcludeListIsNeverObserved() throws Exception {
        Recorder recorder = register(new Recorder("a"));
        InvocationObservers.configure(null, PREFIX + "ec*", null);

        request(ECHO, "hello", "trace-105");

        Utility.getInstance().sleep(200);
        assertNull(recorder.find(r -> r.route.equals(ECHO)));
    }

    @Test
    void theIncludeListLimitsObservation() throws Exception {
        Recorder recorder = register(new Recorder("a"));
        InvocationObservers.configure(ONLY, null, null);

        request(ECHO, "hello", "trace-106");
        request(ONLY, "hello", "trace-107");

        recorder.await(r -> r.is("end", ONLY));
        assertNull(recorder.find(r -> r.route.equals(ECHO)));
    }

    @Test
    void theNegativeControlHidesTheIncomingTags() throws Exception {
        Recorder recorder = register(new Recorder("a"));
        InvocationObservers.configure(null, null, ECHO);

        request(ECHO, "hello", "trace-108");

        Record begin = recorder.await(r -> r.is("begin", ECHO));
        // the sender still tagged the event, but the unlinked route starts a new trace
        assertNotNull(recorder.find(r -> r.is("send", ECHO)));
        assertNull(begin.tag);
    }

    @Test
    void aFailingObserverNeverBreaksDelivery() throws Exception {
        register(new Failing());
        Recorder recorder = register(new Recorder("a"));

        EventEnvelope response = request(ECHO, "still works", "trace-109");

        assertEquals("still works", response.getBody());
        recorder.await(r -> r.is("end", ECHO));
    }

    @Test
    void severalObserversCloseInReverseOrder() throws Exception {
        ConcurrentLinkedQueue<Record> shared = new ConcurrentLinkedQueue<>();
        Recorder first = register(new Recorder("first", shared));
        Recorder second = register(new Recorder("second", shared));

        request(ECHO, "hello", "trace-110");

        Record firstEnd = first.await(r -> r.is("end", ECHO));
        Record secondEnd = second.await(r -> r.is("end", ECHO));
        Record firstBegin = first.await(r -> r.is("begin", ECHO));
        Record secondBegin = second.await(r -> r.is("begin", ECHO));
        assertTrue(firstBegin.seq < secondBegin.seq);
        assertTrue(secondEnd.seq < firstEnd.seq);
    }

    @Test
    void anOutgoingRouteIsMatchedWithoutItsOrigin() {
        Recorder recorder = register(new Recorder("a"));
        EventEnvelope event = new EventEnvelope().setTo(ECHO + "@some-origin").setTrace("trace-111", "GET /x");

        InvocationObservers.send(event);

        Record send = recorder.find(r -> r.is("send", ECHO));
        assertNotNull(send);
        assertEquals("sent-to-" + ECHO, event.getTag(TAG));
    }

    @Test
    void theRouteFilterMatchesExactNamesAndPrefixes() {
        var filter = new InvocationObservers.RouteFilter("", "a.b, c.*", "z.z");

        assertTrue(filter.isObserved("a.bc"));
        assertFalse(filter.isObserved("a.b"));
        assertFalse(filter.isObserved("c.d"));
        assertTrue(filter.isUnlinked("z.z"));
        assertFalse(filter.isUnlinked("z.y"));
        assertFalse(new InvocationObservers.RouteFilter("x.*", "", "").isObserved("y.y"));
    }

    @Test
    void anUnusedScopeOrContinuationIsANoOp() {
        assertDoesNotThrow(() -> InvocationObservers.close(null, "x"));
        assertNull(InvocationObservers.detach(null));
        AtomicInteger delivered = new AtomicInteger();
        InvocationObservers.resume(null, null, delivered::incrementAndGet);
        InvocationObservers.abandon(null, "x");
        assertEquals(1, delivered.get());
        assertFalse(InvocationObservers.unregister(new Recorder("never registered")));
    }

    private <T extends InvocationObserver> T register(T observer) {
        InvocationObservers.register(observer);
        registered.add(observer);
        return observer;
    }

    private EventEnvelope request(String route, Object body, String traceId) throws Exception {
        EventEnvelope event = new EventEnvelope().setTo(route).setBody(body).setTrace(traceId, "GET /api/test");
        return EventEmitter.getInstance().eRequest(event, TIMEOUT).get(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    private static final AtomicInteger sequence = new AtomicInteger();

    private record Record(int seq, String observer, String kind, String route, String traceId, String tag,
                          String detail, String thread) {
        boolean is(String expectedKind, String expectedRoute) {
            return kind.equals(expectedKind) && route.equals(expectedRoute);
        }
    }

    /**
     * Records what the engine calls, for the routes of this test only.
     */
    private static class Recorder implements InvocationObserver {
        private final String name;
        private final ConcurrentLinkedQueue<Record> records;

        Recorder(String name) {
            this(name, new ConcurrentLinkedQueue<>());
        }

        Recorder(String name, ConcurrentLinkedQueue<Record> records) {
            this.name = name;
            this.records = records;
        }

        @Override
        public InvocationScope begin(InvocationInfo info, InvocationCarrier carrier) {
            if (!info.route().startsWith(PREFIX)) {
                return null;
            }
            add("begin", info.route(), info.traceId(), carrier.get(TAG), null);
            String route = info.route();
            return new InvocationScope() {
                @Override
                public void error(String message) {
                    add("error", route, null, null, message);
                }

                @Override
                public void end() {
                    add("end", route, null, null, null);
                }

                @Override
                public InvocationContinuation detach() {
                    add("detach", route, null, null, null);
                    return () -> {
                        add("resume", route, null, null, null);
                        return new InvocationScope() {
                            @Override
                            public void error(String message) {
                                add("resumed-error", route, null, null, message);
                            }

                            @Override
                            public void end() {
                                add("resumed-end", route, null, null, null);
                            }
                        };
                    };
                }
            };
        }

        @Override
        public void send(InvocationInfo info, InvocationCarrier carrier) {
            if (info.route().startsWith(PREFIX)) {
                add("send", info.route(), info.traceId(), null, null);
                carrier.set(TAG, "sent-to-" + info.route());
            }
        }

        private void add(String kind, String route, String traceId, String tag, String detail) {
            records.add(new Record(sequence.incrementAndGet(), name, kind, route, traceId, tag, detail,
                    threadOf()));
        }

        Record find(Predicate<Record> condition) {
            return records.stream().filter(r -> r.observer.equals(name)).filter(condition).findFirst().orElse(null);
        }

        long count(Predicate<Record> condition) {
            return records.stream().filter(r -> r.observer.equals(name)).filter(condition).count();
        }

        Record await(Predicate<Record> condition) {
            for (int i = 0; i < 100; i++) {
                Record found = find(condition);
                if (found != null) {
                    return found;
                }
                Utility.getInstance().sleep(50);
            }
            throw new AssertionError("no matching record for observer " + name + ": " + records);
        }
    }

    /**
     * An observer that throws everywhere.
     */
    private static class Failing implements InvocationObserver {
        @Override
        public InvocationScope begin(InvocationInfo info, InvocationCarrier carrier) {
            throw new IllegalStateException("begin failed");
        }

        @Override
        public void send(InvocationInfo info, InvocationCarrier carrier) {
            throw new IllegalStateException("send failed");
        }
    }

    /** A thread's identity: a virtual thread keeps its id when it parks and resumes on another carrier. */
    private static String threadOf() {
        Thread t = Thread.currentThread();
        return "#" + t.threadId() + (t.isVirtual()? " (virtual)" : " " + t.getName());
    }
}
