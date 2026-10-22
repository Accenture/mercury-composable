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
import com.dynatrace.oneagent.sdk.api.LoggingCallback;
import com.dynatrace.oneagent.sdk.api.OneAgentSDK;
import com.dynatrace.oneagent.sdk.api.OutgoingMessageTracer;
import com.dynatrace.oneagent.sdk.api.enums.SDKState;
import com.dynatrace.oneagent.sdk.api.infos.MessagingSystemInfo;
import com.dynatrace.oneagent.sdk.api.infos.TraceContextInfo;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A stand-in for the OneAgent SDK that records every call, in order, with the thread it ran on. The SDK's
 * types are interfaces, so a dynamic proxy is enough. An outgoing tracer hands out a fresh tag per message
 * ({@code tag-1}, {@code tag-2} ...), as an agent with an active PurePath would.
 */
final class FakeOneAgent {
    /** One SDK call. */
    record Call(int seq, String object, String method, String argument, String thread) { }

    /** One incoming-message-process tracer. */
    static final class Hop {
        final String destination;
        volatile String tag;
        volatile String messageId;
        volatile String correlationId;
        volatile boolean started;
        volatile boolean ended;
        volatile String error;
        volatile String startThread;
        volatile String endThread;

        Hop(String destination) {
            this.destination = destination;
        }
    }

    final List<Call> calls = Collections.synchronizedList(new ArrayList<>());
    final List<Hop> hops = Collections.synchronizedList(new ArrayList<>());
    final List<String> producedTags = Collections.synchronizedList(new ArrayList<>());
    /** The thread each tag was produced on: the sender's thread. */
    final Map<String, String> tagThreads = new ConcurrentHashMap<>();
    final List<String> infos = Collections.synchronizedList(new ArrayList<>());
    volatile SDKState state = SDKState.ACTIVE;
    volatile boolean pathActive = true;
    volatile boolean validTraceContext = true;
    volatile LoggingCallback logging;
    private final AtomicInteger sequence = new AtomicInteger();
    private final AtomicInteger tags = new AtomicInteger();
    final OneAgentSDK sdk = proxy(OneAgentSDK.class, (proxy, method, args) -> {
        record("sdk", method.getName(), args);
        return switch (method.getName()) {
            case "getCurrentState" -> state;
            case "setLoggingCallback" -> {
                logging = (LoggingCallback) args[0];
                yield null;
            }
            case "createMessagingSystemInfo" -> {
                String destination = (String) args[1];
                infos.add(destination);
                yield proxy(MessagingSystemInfo.class, (p, m, a) -> m.getName().equals("toString")? destination : null);
            }
            case "traceIncomingMessageProcess" -> incoming(String.valueOf(args[0]));
            case "traceOutgoingMessage" -> outgoing();
            case "createInProcessLink" -> proxy(InProcessLink.class, (p, m, a) -> m.getName().equals("toString")? "link" : null);
            case "traceInProcessLink" -> proxy(InProcessLinkTracer.class, (p, m, a) -> {
                record("link-tracer", m.getName(), a);
                return null;
            });
            case "getTraceContextInfo" -> proxy(TraceContextInfo.class, (p, m, a) -> switch (m.getName()) {
                case "isValid" -> validTraceContext;
                case "getTraceId" -> "dt-trace-1";
                case "getSpanId" -> "dt-span-1";
                default -> null;
            });
            default -> null;
        };
    });

    private IncomingMessageProcessTracer incoming(String destination) {
        Hop hop = new Hop(destination);
        hops.add(hop);
        return proxy(IncomingMessageProcessTracer.class, (proxy, method, args) -> {
            record("incoming", method.getName(), args);
            String thread = threadOf();
            switch (method.getName()) {
                case "setDynatraceStringTag" -> hop.tag = (String) args[0];
                case "setVendorMessageId" -> hop.messageId = (String) args[0];
                case "setCorrelationId" -> hop.correlationId = (String) args[0];
                case "start" -> {
                    hop.started = true;
                    hop.startThread = thread;
                }
                case "end" -> {
                    hop.ended = true;
                    hop.endThread = thread;
                }
                case "error" -> hop.error = String.valueOf(args[0]);
                default -> { }
            }
            return null;
        });
    }

    private OutgoingMessageTracer outgoing() {
        return proxy(OutgoingMessageTracer.class, (proxy, method, args) -> {
            record("outgoing", method.getName(), args);
            if (method.getName().equals("getDynatraceStringTag")) {
                if (!pathActive) {
                    return "";
                }
                String tag = "tag-" + tags.incrementAndGet();
                producedTags.add(tag);
                tagThreads.put(tag, threadOf());
                return tag;
            }
            return null;
        });
    }

    List<Hop> hopsOf(String destination) {
        synchronized (hops) {
            return hops.stream().filter(h -> h.destination.equals(destination)).toList();
        }
    }

    List<Call> callsOf(String object) {
        synchronized (calls) {
            return calls.stream().filter(c -> c.object().equals(object)).toList();
        }
    }

    private void record(String object, String method, Object[] args) {
        if (!method.equals("toString") && !method.equals("hashCode") && !method.equals("equals")) {
            calls.add(new Call(sequence.incrementAndGet(), object, method,
                    args == null || args.length == 0? null : String.valueOf(args[0]), threadOf()));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(FakeOneAgent.class.getClassLoader(), new Class<?>[]{type}, (p, m, a) -> {
            if (m.getName().equals("hashCode")) {
                return System.identityHashCode(p);
            }
            if (m.getName().equals("equals")) {
                return p == a[0];
            }
            Object result = handler.invoke(p, m, a);
            if (m.getName().equals("toString") && result == null) {
                return type.getSimpleName();
            }
            return result;
        });
    }

    /** A thread's identity: a virtual thread keeps its id when it parks and resumes on another carrier. */
    private static String threadOf() {
        Thread t = Thread.currentThread();
        return "#" + t.threadId() + (t.isVirtual()? " (virtual)" : " " + t.getName());
    }
}
