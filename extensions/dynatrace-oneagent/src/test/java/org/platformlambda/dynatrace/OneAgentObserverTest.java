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

package org.platformlambda.dynatrace;

import com.dynatrace.oneagent.sdk.api.enums.SDKState;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.logging.LogContext;
import org.platformlambda.core.logging.LogContextManager;
import org.platformlambda.core.models.TraceInfo;
import org.platformlambda.core.spi.InvocationCarrier;
import org.platformlambda.core.spi.InvocationContinuation;
import org.platformlambda.core.spi.InvocationInfo;
import org.platformlambda.core.spi.InvocationScope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The mapping of a hop onto the OneAgent SDK, call by call.
 */
class OneAgentObserverTest {
    private static final InvocationInfo HOP = new InvocationInfo("demo.route", "sender", "event-1",
            "trace-1", "GET /api/demo", "span-1", 1);

    @Test
    void aHopConsumesTheSendersTagBeforeItStarts() {
        FakeOneAgent agent = new FakeOneAgent();
        OneAgentObserver observer = new OneAgentObserver(agent.sdk, "demo-app", false, false);
        MapCarrier carrier = new MapCarrier(Map.of(OneAgentObserver.TAG, "tag-from-sender"));

        InvocationScope scope = observer.begin(HOP, carrier);
        scope.end();

        FakeOneAgent.Hop hop = agent.hops.getFirst();
        assertEquals("demo-app", hop.destination);
        assertEquals("tag-from-sender", hop.tag);
        assertEquals("event-1", hop.messageId);
        assertEquals("trace-1", hop.correlationId);
        assertTrue(hop.started && hop.ended);
        assertEquals(hop.startThread, hop.endThread);
        List<String> order = agent.callsOf("incoming").stream().map(FakeOneAgent.Call::method).toList();
        assertTrue(order.indexOf("setDynatraceStringTag") < order.indexOf("start"));
        assertEquals("end", order.getLast());
        List<String> attributes = agent.callsOf("sdk").stream()
                .filter(c -> c.method().equals("addCustomRequestAttribute")).map(FakeOneAgent.Call::argument).toList();
        assertEquals(List.of(OneAgentObserver.ROUTE_ATTRIBUTE, OneAgentObserver.TRACE_ID_ATTRIBUTE), attributes);
    }

    @Test
    void aHopWithoutATagIsANewEntryPoint() {
        FakeOneAgent agent = new FakeOneAgent();
        OneAgentObserver observer = new OneAgentObserver(agent.sdk, "demo-app", false, false);

        observer.begin(HOP, new MapCarrier(Map.of())).end();

        assertNull(agent.hops.getFirst().tag);
        assertTrue(agent.callsOf("incoming").stream().noneMatch(c -> c.method().equals("setDynatraceStringTag")));
    }

    @Test
    void aFailedHopRecordsTheErrorThenEnds() {
        FakeOneAgent agent = new FakeOneAgent();
        OneAgentObserver observer = new OneAgentObserver(agent.sdk, "demo-app", false, false);

        InvocationScope scope = observer.begin(HOP, new MapCarrier(Map.of()));
        scope.error("demo failure");
        scope.end();

        assertEquals("demo failure", agent.hops.getFirst().error);
        assertTrue(agent.hops.getFirst().ended);
    }

    @Test
    void aSendWritesTheOutgoingMessagesTag() {
        FakeOneAgent agent = new FakeOneAgent();
        OneAgentObserver observer = new OneAgentObserver(agent.sdk, "demo-app", false, false);
        MapCarrier carrier = new MapCarrier(Map.of());

        observer.send(HOP, carrier);

        assertEquals("tag-1", carrier.get(OneAgentObserver.TAG));
        List<String> order = agent.callsOf("outgoing").stream().map(FakeOneAgent.Call::method).toList();
        assertEquals(List.of("setVendorMessageId", "setCorrelationId", "start", "getDynatraceStringTag", "end"), order);
    }

    @Test
    void aSendWithoutAPurePathRemovesAStaleTag() {
        FakeOneAgent agent = new FakeOneAgent();
        agent.pathActive = false;
        OneAgentObserver observer = new OneAgentObserver(agent.sdk, "demo-app", false, false);
        MapCarrier carrier = new MapCarrier(Map.of(OneAgentObserver.TAG, "stale"));

        observer.send(HOP, carrier);

        assertNull(carrier.get(OneAgentObserver.TAG));
    }

    @Test
    void anInactiveAgentObservesNothing() {
        FakeOneAgent agent = new FakeOneAgent();
        agent.state = SDKState.TEMPORARILY_INACTIVE;
        OneAgentObserver observer = new OneAgentObserver(agent.sdk, "demo-app", false, false);
        MapCarrier carrier = new MapCarrier(Map.of(OneAgentObserver.TAG, "stale"));

        assertNull(observer.begin(HOP, carrier));
        observer.send(HOP, carrier);

        assertNull(carrier.get(OneAgentObserver.TAG));
        assertTrue(agent.hops.isEmpty());
    }

    @Test
    void aDeferredResultResumesThroughAnInProcessLink() throws Exception {
        FakeOneAgent agent = new FakeOneAgent();
        OneAgentObserver observer = new OneAgentObserver(agent.sdk, "demo-app", false, false);

        InvocationScope scope = observer.begin(HOP, new MapCarrier(Map.of()));
        InvocationContinuation continuation = scope.detach();
        scope.end();
        InvocationScope[] resumed = new InvocationScope[1];
        Thread completion = Thread.ofVirtual().start(() -> {
            resumed[0] = continuation.resume();
            resumed[0].error("late failure");
            resumed[0].end();
        });
        completion.join();

        List<FakeOneAgent.Call> sdkCalls = agent.callsOf("sdk");
        int link = indexOf(sdkCalls, "createInProcessLink");
        int traceLink = indexOf(sdkCalls, "traceInProcessLink");
        assertTrue(link >= 0 && traceLink > link);
        // the link is created while the hop's tracer is still open, on the hop's thread
        FakeOneAgent.Call created = sdkCalls.get(link);
        assertEquals(agent.hops.getFirst().startThread, created.thread());
        List<String> linked = agent.callsOf("link-tracer").stream().map(FakeOneAgent.Call::method).toList();
        assertEquals(List.of("start", "error", "end"), linked);
        assertNotEquals(created.thread(), agent.callsOf("link-tracer").getFirst().thread());
    }

    @Test
    void theDestinationIsTheApplicationOrEachRoute() {
        FakeOneAgent agent = new FakeOneAgent();
        OneAgentObserver shared = new OneAgentObserver(agent.sdk, "demo-app", false, false);
        shared.begin(HOP, new MapCarrier(Map.of())).end();
        shared.send(HOP, new MapCarrier(Map.of()));
        assertEquals(List.of("demo-app"), agent.infos);

        FakeOneAgent perRoute = new FakeOneAgent();
        OneAgentObserver named = new OneAgentObserver(perRoute.sdk, "demo-app", true, false);
        named.begin(HOP, new MapCarrier(Map.of())).end();
        named.begin(HOP, new MapCarrier(Map.of())).end();
        assertEquals(List.of("demo.route"), perRoute.infos);
    }

    @Test
    void theAgentsTraceparentIsOfferedWhileAPurePathIsActive() {
        FakeOneAgent agent = new FakeOneAgent();
        OneAgentObserver observer = new OneAgentObserver(agent.sdk, "demo-app", false, false);

        assertEquals("00-" + FakeOneAgent.TRACE_ID + "-" + FakeOneAgent.SPAN_ID + "-01", observer.traceparent());
        agent.validTraceContext = false;
        assertNull(observer.traceparent(), "no PurePath on this thread");
        agent.validTraceContext = true;
        agent.state = SDKState.TEMPORARILY_INACTIVE;
        assertNull(observer.traceparent(), "an inactive agent offers nothing");
    }

    @Test
    void theLogContextCarriesOneAgentsIdsWhenEnabled() {
        FakeOneAgent agent = new FakeOneAgent();
        long thread = Thread.currentThread().threadId();
        LogContext context = new LogContext(new TraceInfo("demo.route", "trace-1", "GET /api/demo", null), null);
        LogContextManager.register(thread, context);
        try {
            new OneAgentObserver(agent.sdk, "demo-app", false, false).begin(HOP, new MapCarrier(Map.of())).end();
            assertFalse(context.getCustomKeys().containsKey(OneAgentObserver.LOG_TRACE_ID));

            agent.validTraceContext = false;
            new OneAgentObserver(agent.sdk, "demo-app", false, true).begin(HOP, new MapCarrier(Map.of())).end();
            assertFalse(context.getCustomKeys().containsKey(OneAgentObserver.LOG_TRACE_ID));

            agent.validTraceContext = true;
            new OneAgentObserver(agent.sdk, "demo-app", false, true).begin(HOP, new MapCarrier(Map.of())).end();
            assertEquals(FakeOneAgent.TRACE_ID, context.getCustomKeys().get(OneAgentObserver.LOG_TRACE_ID));
            assertEquals(FakeOneAgent.SPAN_ID, context.getCustomKeys().get(OneAgentObserver.LOG_SPAN_ID));
        } finally {
            LogContextManager.remove(thread);
        }
        // no log context registered on this thread: nothing to write, nothing thrown
        assertDoesNotThrow(() ->
                new OneAgentObserver(agent.sdk, "demo-app", false, true).begin(HOP, new MapCarrier(Map.of())).end());
    }

    private static int indexOf(List<FakeOneAgent.Call> calls, String method) {
        for (int i = 0; i < calls.size(); i++) {
            if (calls.get(i).method().equals(method)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * A carrier over a plain map.
     */
    static final class MapCarrier implements InvocationCarrier {
        private final Map<String, String> tags = new HashMap<>();

        MapCarrier(Map<String, String> initial) {
            tags.putAll(initial);
        }

        @Override
        public String get(String key) {
            return tags.get(key);
        }

        @Override
        public void set(String key, String value) {
            if (value == null || value.isEmpty()) {
                tags.remove(key);
            } else {
                tags.put(key, value);
            }
        }

        @Override
        public void remove(String key) {
            tags.remove(key);
        }
    }
}
