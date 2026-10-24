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

package com.accenture.minigraph.rest;

import com.accenture.minigraph.services.GraphCommandService;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * THE AI-companion endpoint (ADR-0008, synchronous). Its fire-and-forget sibling
 * (POST /api/companion/{id}, which returned {@code {status:"accepted"}} and streamed
 * the real outcome only to the WebSocket console) was RETIRED 2026-09-02 — it hid
 * errors from the caller and forced slower, sleep-padded drivers. This endpoint
 * returns the command's <b>outcome in-band</b> as
 * {@code {ok, id, command, output, error, result}} so an AI agent can self-correct
 * without a human relaying the console. Each output line is <b>also teed</b> to the
 * session's real WebSocket {@code .out} route, so a watching human — and, via the
 * command service's subscriber fan-out, any {@code session subscribe}d session —
 * sees the same output live (real-time human+AI collaboration).
 * <p>
 * Mechanism: dispatch the command to the singleton command handler over
 * request-response RPC with a private, per-call capture route supplied as the
 * command's {@code out}; the capture route buffers each line (and tees it). The
 * end-of-transmission signal depends on the command shape: a <b>traversal</b>
 * ({@code run}) is asynchronous — the handler launches the traveler and replies
 * immediately, then the traveler streams its output afterward — so it is drained
 * on the traveler's <b>terminal line</b> ("Graph traversal completed in N ms" |
 * "Graph traversal aborted: {reason}"), always emitted last; every other command emits all
 * output before it replies, so a <b>FIFO sentinel</b> marks its buffer drained. A
 * sentinel would race (and usually beat) the traversal tail, truncating the
 * capture. The WebSocket console behavior is unchanged.
 * <p>
 * The outcome is a failure when the engine says so, never inferred from the wording of
 * its lines: the command service sends every refusal (the line that says why a command
 * did nothing or failed) with an error status, as the traveler sends a traversal's
 * abort, and the first such line is the {@code error}. A command that throws past the
 * command service fails its RPC, which is an error too.
 */
@OptionalService("app.env=dev")
@PreLoad(route = "post.companion.command.sync", instances = 10)
public class PostCompanionCommandSync implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private static final Logger log = LoggerFactory.getLogger(PostCompanionCommandSync.class);

    private static final String SYNC_SENTINEL = "__companion_sync_done__";
    private static final String COMMAND = "command";
    private static final String TEXT_COMMAND_REQUIRED = "Body must be a non-empty text/plain command";
    private static final long COMMAND_TIMEOUT_MS = 30000;
    private static final long DRAIN_TIMEOUT_MS = 5000;
    // margin past the dry-run watcher deadline so the watcher's terminal always beats
    // the drain safety net; the cap stays under the endpoint's own rest.yaml timeout
    private static final long WATCHER_GRACE_MS = 5000;
    private static final long MAX_TRAVERSAL_DRAIN_MS = 35000;

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) throws Exception {
        var id = input.getPathParameter("id");
        if (id == null) {
            throw new IllegalArgumentException("Missing path parameter: id");
        }
        var command = commandFromBody(input);
        if (!GraphCommandService.hasSession(id)) {
            throw new AppException(404, "No active session for id " + id);
        }
        // Inverse of GraphUserInterface's public session id mapping.
        var route = id.replace('-', '.');
        var inRoute = route + ".in";
        var outRoute = route + ".out";
        // Reject session-topology commands before dispatch: only the read-only "session"
        // status query is available from a companion (a companion is an assistant to the
        // session in the URL, not a WebSocket session of its own; executed here the
        // subscribe path would also bind the ephemeral capture route as a subscriber).
        var topology = GraphCommandService.sessionTopologySubcommand(command);
        if (topology != null) {
            return refusedResponse(id, command, GraphCommandService.refuseSessionTopology(outRoute, command, topology));
        }
        var captureRoute = "companion.sync." + Utility.getInstance().getUuid();
        var po = new PostOffice(headers, instance);
        var platform = Platform.getInstance();

        // a private, per-call capture route (see Capture)
        var capture = new Capture(outRoute, isTraversalCommand(command));
        platform.registerPrivate(captureRoute, capture, 1);
        final boolean complete;
        try {
            complete = dispatchAndDrain(po, id, command, inRoute, captureRoute, capture);
        } finally {
            platform.release(captureRoute);
        }
        return outcomeResponse(id, command, capture, complete);
    }

    private static String commandFromBody(AsyncHttpRequest input) {
        if (!(input.getBody() instanceof String raw)) {
            throw new IllegalArgumentException(TEXT_COMMAND_REQUIRED);
        }
        var command = raw.trim();
        if (command.isEmpty()) {
            throw new IllegalArgumentException(TEXT_COMMAND_REQUIRED);
        }
        return command;
    }

    /**
     * The private, per-call capture route: it buffers each line for the in-band response, tees it
     * to the session's real WebSocket .out for the live human view, and remembers the first line
     * the engine flagged as a refusal. It takes the whole event, because the flag is the event's
     * error status. A traversal (`run`) streams its output after the handler replies; every other
     * command emits all output before replying. One latch serves both: it fires on the traveler's
     * terminal line for a traversal, or on the FIFO sentinel otherwise.
     */
    private static final class Capture implements TypedLambdaFunction<EventEnvelope, Void> {
        private final EventEmitter emitter = EventEmitter.getInstance();
        private final String outRoute;
        private final boolean traversal;
        private final List<Object> buffer = new CopyOnWriteArrayList<>();
        private final AtomicReference<String> refusal = new AtomicReference<>();
        private final AtomicReference<String> failure = new AtomicReference<>();
        private final CountDownLatch drained = new CountDownLatch(1);

        Capture(String outRoute, boolean traversal) {
            this.outRoute = outRoute;
            this.traversal = traversal;
        }

        @Override
        public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance) {
            var body = event.getBody();
            if (SYNC_SENTINEL.equals(body)) {
                drained.countDown();
                return null;
            }
            buffer.add(body);
            if (event.hasError()) {
                refusal.compareAndSet(null, asText(event.getError()));
            }
            try {
                emitter.send(new EventEnvelope().setTo(outRoute).setStatus(event.getStatus()).setBody(body));
            } catch (Exception e) {
                // best-effort tee: the .out route may have no live WebSocket
                log.debug("Tee to {} skipped - {}", outRoute, e.getMessage());
            }
            // A traversal is drained on its terminal line (emitted last), so once seen
            // every prior line is already buffered - deterministic, no racing sentinel.
            if (traversal && body instanceof String line && isTraversalTerminal(line)) {
                drained.countDown();
            }
            return null;
        }

        private static String asText(Object error) {
            return error instanceof String text? text
                    : SimpleMapper.getInstance().getMapper().writeValueAsString(error);
        }

        /**
         * The traveler's end-of-transmission lines. One of these is <b>always</b>
         * emitted last (success or failure), so the synchronous endpoint drains a
         * traversal deterministically — no timer, no truncated capture.
         */
        private static boolean isTraversalTerminal(String line) {
            return line.startsWith("Graph traversal completed in") || line.startsWith("Graph traversal aborted");
        }
    }

    private boolean dispatchAndDrain(PostOffice po, String id, String command, String inRoute,
                                     String captureRoute, Capture capture)
            throws ExecutionException, InterruptedException {
        // RPC the singleton handler with the capture route as `out`; handleEvent
        // returns once the command is dispatched (a traversal is still running).
        // "direct" marks a synchronous companion RPC: not a flaky WS client, so
        // the identical-command dedup guard does not apply (finding #62)
        var dispatched = po.request(new EventEnvelope()
                .setTo(GraphCommandService.SINGLETON_COMMAND_HANDLER)
                .setBody(Map.of(
                        "type", COMMAND,
                        "in", inRoute,
                        "out", captureRoute,
                        "message", command,
                        "direct", true)),
                COMMAND_TIMEOUT_MS).get();
        // the command service catches what it expects and prints it as a refusal; anything
        // else fails the RPC, which the console never shows
        if (dispatched.hasError()) {
            capture.failure.set("ERROR: " + dispatched.getError());
        }
        // Synchronous commands: enqueue the FIFO sentinel to mark the buffer drained.
        // Traversals: the capture route counts the latch down on the terminal line.
        if (!capture.traversal) {
            po.send(new EventEnvelope().setTo(captureRoute).setBody(SYNC_SENTINEL));
        }
        // The timeout is only a safety net; correctness comes from the signal, not from
        // a timer. A traversal's window is sized past the dry-run watcher deadline so
        // the watcher's terminal - not this net - ends the drain even in the default
        // config. When the net DOES catch - the signal never arrived - the caller must
        // know: the capture is truncated and the command is still running, so the
        // outcome is classified as a failure, never a silent success.
        var drainTimeout = capture.traversal ? traversalDrainWindow(inRoute) : DRAIN_TIMEOUT_MS;
        var complete = capture.drained.await(drainTimeout, TimeUnit.MILLISECONDS);
        if (!complete) {
            log.warn("Companion sync drain timed out for {}", id);
        }
        return complete;
    }

    private static long traversalDrainWindow(String inRoute) {
        var deadline = GraphCommandService.getRunDeadlineMs(inRoute);
        var window = deadline == null? COMMAND_TIMEOUT_MS : deadline + WATCHER_GRACE_MS;
        // the cap keeps the drain under the endpoint's rest.yaml timeout: a dry-run
        // with a larger seeded model.ttl outlives this endpoint's budget and is
        // reported as truncated (ok=false) - use the WebSocket console for long runs
        return Math.clamp(window, DRAIN_TIMEOUT_MS, MAX_TRAVERSAL_DRAIN_MS);
    }

    private static EventEnvelope refusedResponse(String id, String command, String error) {
        var refused = new HashMap<String, Object>();
        refused.put("ok", false);
        refused.put("id", id);
        refused.put(COMMAND, command);
        refused.put("output", List.of("> " + command, error));
        refused.put("error", error);
        refused.put("result", null);
        return new EventEnvelope().setHeader("Content-Type", "application/json").setBody(refused);
    }

    /**
     * Build the structured outcome from the captured output. The error is the first line the
     * engine flagged as a refusal, else the dispatch's own failure, else a truncated capture.
     */
    private static EventEnvelope outcomeResponse(String id, String command, Capture capture, boolean complete) {
        List<String> output = new ArrayList<>();
        List<Object> result = new ArrayList<>();
        for (var item : capture.buffer) {
            if (item instanceof String line) {
                output.add(line);
            } else {
                result.add(item);
            }
        }
        var error = capture.refusal.get();
        if (error == null) {
            error = capture.failure.get();
        }
        if (error == null && !complete) {
            error = "Output truncated - the command did not finish within the drain window";
        }
        var body = new HashMap<String, Object>();
        body.put("ok", error == null);
        body.put("id", id);
        body.put(COMMAND, command);
        body.put("output", output);
        body.put("error", error);
        body.put("result", result.isEmpty() ? null : result);
        return new EventEnvelope().setHeader("Content-Type", "application/json").setBody(body);
    }

    /**
     * The Playground's only <b>asynchronous</b> command: {@code run} launches the
     * traveler, which streams its output after the command handler has already
     * replied — so the sentinel drain races the traversal tail. A traversal is
     * drained on its terminal line instead (see {@link Capture#isTraversalTerminal}).
     */
    private static boolean isTraversalCommand(String command) {
        return "run".equalsIgnoreCase(command);
    }
}
