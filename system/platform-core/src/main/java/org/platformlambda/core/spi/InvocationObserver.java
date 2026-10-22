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

/**
 * A live observer of function invocations: the vendor-neutral seam for code-level tracers (an APM
 * agent's SDK, the OpenTelemetry API) that must bracket the real call on the real call stack.
 * <p>
 * This is a different mechanism from the {@code distributed.trace.forwarder} route, which receives a
 * FINISHED span record after the fact. An observer is called synchronously, inline on the dispatch
 * path: {@link #begin} on the thread that runs the function, {@link #send} on the thread that sends an
 * event. Platform-core holds no vendor dependency; an adapter lives in an optional extension module and
 * registers itself with {@link InvocationObservers#register}.
 * <p>
 * Linkage between hops rides as a string tag on the event envelope (see {@link InvocationCarrier}), never
 * as thread state: Mercury hands an event from the sender to the receiving function through the event
 * bus and a per-route queue, so a context kept in a thread-local is lost at every hop. The tag is
 * serialized with the envelope, so it also crosses Event-over-HTTP and cloud-connector boundaries.
 * <p>
 * An observer must be fast and must never throw: it runs on every observed dispatch. The engine guards
 * every call (an exception is logged and the invocation proceeds), but a slow observer slows every
 * function.
 */
public interface InvocationObserver {

    /**
     * A function is about to run on the current thread. Read the observer's own tag from the carrier
     * (the sender wrote it in {@link #send}) and open a scope that this same thread closes when the
     * function returns.
     *
     * @param info the invocation: route, sender, event id and Mercury's trace context
     * @param carrier the incoming event's tags, read-only for the observer's purpose
     * @return the open scope, or null to leave this invocation unobserved
     */
    InvocationScope begin(InvocationInfo info, InvocationCarrier carrier);

    /**
     * An event is about to leave the current thread: a function's send or request, a response, or an
     * engine edge (REST automation, Event Script). Write the observer's tag for the current context into
     * the carrier, or remove it when the current thread has no context, so a stale tag copied from an
     * earlier event is never consumed twice.
     *
     * @param info the outgoing event: destination route, sender, event id and Mercury's trace context
     * @param carrier the outgoing event's tags
     */
    void send(InvocationInfo info, InvocationCarrier carrier);
}
