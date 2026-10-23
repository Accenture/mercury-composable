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

import com.dynatrace.oneagent.sdk.api.InProcessLink;
import com.dynatrace.oneagent.sdk.api.InProcessLinkTracer;
import com.dynatrace.oneagent.sdk.api.IncomingMessageProcessTracer;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingMessageTracer;
import com.dynatrace.oneagent.sdk.api.Tracer;
import com.dynatrace.oneagent.sdk.api.enums.ChannelType;
import com.dynatrace.oneagent.sdk.api.enums.MessageDestinationType;
import com.dynatrace.oneagent.sdk.api.enums.SDKState;
import com.dynatrace.oneagent.sdk.api.infos.MessagingSystemInfo;
import com.dynatrace.oneagent.sdk.api.infos.TraceContextInfo;
import org.platformlambda.core.logging.LogContext;
import org.platformlambda.core.logging.LogContextManager;
import org.platformlambda.core.spi.InvocationCarrier;
import org.platformlambda.core.spi.InvocationContinuation;
import org.platformlambda.core.spi.InvocationInfo;
import org.platformlambda.core.spi.InvocationObserver;
import org.platformlambda.core.spi.InvocationScope;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Brackets every composable-function hop with the Dynatrace OneAgent SDK, so a request shows in Dynatrace
 * as one connected PurePath instead of fragments that stop at Mercury's event bus.
 * <p>
 * <b>Each hop is a message.</b> A send is an outgoing-message tracer whose Dynatrace tag rides on the event
 * envelope ({@link #TAG}); the receiving function is an incoming-message-process tracer that consumes the
 * tag before it starts. A custom-service tracer would not do: it carries no tag, so each hop would become a
 * new entry point. All hops share one messaging system (vendor {@code Mercury}); the destination is the
 * application name by default, so a route does not become a Dynatrace service - the route travels as the
 * custom request attribute {@code mercury.route}. {@code dynatrace.oneagent.destination=route} makes each
 * route its own destination, for a check that wants every hop named in the PurePath.
 * <p>
 * <b>Thread affinity.</b> SDK tracers must start and end on one thread, LIFO. The engine guarantees it: a
 * hop's scope opens and closes on the worker thread, and an outgoing tracer opens and closes inside one
 * send. A function that returns a Mono hands over an in-process link ({@link InvocationScope#detach}) that
 * the completion thread resumes; the hop's own tracer still ends when the function returns, so a deferred
 * result appears as a linked node at the time it completes, not as a long hop.
 * <p>
 * <b>Log context (opt-in, {@code dynatrace.oneagent.log.context=true}).</b> When a hop starts, OneAgent's
 * own trace and span ids for it are written into Mercury's log context as {@code dt.trace_id} and
 * {@code dt.span_id}, so the JSON log lines of the function carry the ids of the PurePath node that ran it.
 * Do not combine it with OneAgent's own log enrichment, which writes the same field names.
 * <p>
 * <b>One trace id.</b> At a REST request that arrives without a trace id, Mercury adopts the trace id of
 * the PurePath that OneAgent's HTTP instrumentation already started ({@link #traceId}), so a Mercury trace
 * id is also the Dynatrace trace id.
 */
public class OneAgentObserver implements InvocationObserver {
    public static final String TAG = "dt_tag";
    public static final String ROUTE_ATTRIBUTE = "mercury.route";
    public static final String TRACE_ID_ATTRIBUTE = "mercury.trace_id";
    public static final String LOG_TRACE_ID = "dt.trace_id";
    public static final String LOG_SPAN_ID = "dt.span_id";
    private static final String VENDOR = "Mercury";
    private final OneAgentSDK sdk;
    private final String application;
    private final boolean destinationPerRoute;
    private final boolean logContext;
    private final ConcurrentMap<String, MessagingSystemInfo> destinations = new ConcurrentHashMap<>();

    /**
     * @param sdk the OneAgent SDK instance, created once for the JVM
     * @param application the application name: the messaging destination unless per-route
     * @param destinationPerRoute true to make each route its own messaging destination
     * @param logContext true to write OneAgent's trace and span ids into Mercury's log context
     */
    public OneAgentObserver(OneAgentSDK sdk, String application, boolean destinationPerRoute, boolean logContext) {
        this.sdk = sdk;
        this.application = application;
        this.destinationPerRoute = destinationPerRoute;
        this.logContext = logContext;
    }

    @Override
    public InvocationScope begin(InvocationInfo info, InvocationCarrier carrier) {
        if (sdk.getCurrentState() != SDKState.ACTIVE) {
            return null;
        }
        IncomingMessageProcessTracer tracer = sdk.traceIncomingMessageProcess(destination(info.route()));
        String tag = carrier.get(TAG);
        if (tag != null && !tag.isEmpty()) {
            // consumed before start: the hop becomes a child of the sender's outgoing message
            tracer.setDynatraceStringTag(tag);
        }
        tracer.setVendorMessageId(info.eventId());
        tracer.setCorrelationId(info.traceId());
        tracer.start();
        sdk.addCustomRequestAttribute(ROUTE_ATTRIBUTE, info.route());
        sdk.addCustomRequestAttribute(TRACE_ID_ATTRIBUTE, info.traceId());
        if (logContext) {
            writeLogContext();
        }
        return new HopScope(tracer);
    }

    @Override
    public void send(InvocationInfo info, InvocationCarrier carrier) {
        if (sdk.getCurrentState() != SDKState.ACTIVE) {
            carrier.remove(TAG);
            return;
        }
        OutgoingMessageTracer tracer = sdk.traceOutgoingMessage(destination(info.route()));
        tracer.setVendorMessageId(info.eventId());
        tracer.setCorrelationId(info.traceId());
        tracer.start();
        try {
            // an empty tag (no PurePath on this thread) removes any stale tag copied from an earlier event
            carrier.set(TAG, tracer.getDynatraceStringTag());
        } finally {
            tracer.end();
        }
    }

    @Override
    public String traceId() {
        if (sdk.getCurrentState() != SDKState.ACTIVE) {
            return null;
        }
        TraceContextInfo trace = sdk.getTraceContextInfo();
        return trace != null && trace.isValid()? trace.getTraceId() : null;
    }

    private MessagingSystemInfo destination(String route) {
        String name = destinationPerRoute? route : application;
        return destinations.computeIfAbsent(name, n ->
                sdk.createMessagingSystemInfo(VENDOR, n, MessageDestinationType.QUEUE, ChannelType.IN_PROCESS, null));
    }

    private void writeLogContext() {
        LogContext context = LogContextManager.get(Thread.currentThread().threadId());
        if (context != null) {
            TraceContextInfo trace = sdk.getTraceContextInfo();
            if (trace != null && trace.isValid()) {
                context.put(LOG_TRACE_ID, trace.getTraceId());
                context.put(LOG_SPAN_ID, trace.getSpanId());
            }
        }
    }

    /**
     * A hop, open on the worker thread.
     */
    private final class HopScope extends TracerScope {

        HopScope(Tracer tracer) {
            super(tracer);
        }

        @Override
        public InvocationContinuation detach() {
            // created while the hop's tracer is the active node on this thread
            InProcessLink link = sdk.createInProcessLink();
            return () -> {
                InProcessLinkTracer linked = sdk.traceInProcessLink(link);
                linked.start();
                return new TracerScope(linked);
            };
        }
    }

    /**
     * One started SDK tracer: error, then end, on its own thread.
     */
    private static class TracerScope implements InvocationScope {
        private final Tracer tracer;

        TracerScope(Tracer tracer) {
            this.tracer = tracer;
        }

        @Override
        public void error(String message) {
            tracer.error(message);
        }

        @Override
        public void end() {
            tracer.end();
        }
    }
}
