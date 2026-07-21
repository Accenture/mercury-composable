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
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.models.TypedLambdaFunction;
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

/**
 * Synchronous AI-companion endpoint (ADR-0008) — the additive sibling of
 * {@link PostCompanionCommand}. Where the fire-and-forget endpoint returns
 * {@code {status:"accepted"}} and streams the real outcome only to the WebSocket
 * console, this endpoint returns the command's <b>outcome in-band</b> as
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
 * "Graph traversal aborted"), always emitted last; every other command emits all
 * output before it replies, so a <b>FIFO sentinel</b> marks its buffer drained. A
 * sentinel would race (and usually beat) the traversal tail, truncating the
 * capture. The existing endpoint and the WebSocket console are unchanged.
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

        // A private, per-call capture route: buffer each line for the in-band response and
        // tee it to the session's real WebSocket .out for the live human view.
        // A traversal (`run`) streams its output after the handler replies; every other
        // command emits all output before replying. One latch serves both: it fires on
        // the traveler's terminal line for a traversal, or on the FIFO sentinel otherwise.
        final var traversal = isTraversalCommand(command);
        List<Object> buffer = new CopyOnWriteArrayList<>();
        CountDownLatch drained = new CountDownLatch(1);
        platform.registerPrivate(captureRoute, captureFunction(outRoute, traversal, buffer, drained), 1);
        try {
            dispatchAndDrain(po, id, command, inRoute, captureRoute, traversal, drained);
        } finally {
            platform.release(captureRoute);
        }
        return outcomeResponse(id, command, buffer);
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

    private LambdaFunction captureFunction(String outRoute, boolean traversal,
                                           List<Object> buffer, CountDownLatch drained) {
        var emitter = EventEmitter.getInstance();
        return (hdr, body, inst) -> {
            if (SYNC_SENTINEL.equals(body)) {
                drained.countDown();
                return null;
            }
            buffer.add(body);
            try {
                emitter.send(new EventEnvelope().setTo(outRoute).setBody(body));
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
        };
    }

    private void dispatchAndDrain(PostOffice po, String id, String command, String inRoute,
                                  String captureRoute, boolean traversal, CountDownLatch drained)
            throws ExecutionException, InterruptedException {
        // RPC the singleton handler with the capture route as `out`; handleEvent
        // returns once the command is dispatched (a traversal is still running).
        // "direct" marks a synchronous companion RPC: not a flaky WS client, so
        // the identical-command dedup guard does not apply (finding #62)
        po.request(new EventEnvelope()
                .setTo(GraphCommandService.SINGLETON_COMMAND_HANDLER)
                .setBody(Map.of(
                        "type", COMMAND,
                        "in", inRoute,
                        "out", captureRoute,
                        "message", command,
                        "direct", true)),
                COMMAND_TIMEOUT_MS).get();
        // Synchronous commands: enqueue the FIFO sentinel to mark the buffer drained.
        // Traversals: the capture route counts the latch down on the terminal line.
        if (!traversal) {
            po.send(new EventEnvelope().setTo(captureRoute).setBody(SYNC_SENTINEL));
        }
        // The timeout is only a safety net (matched to the command timeout for a
        // traversal); correctness comes from the signal, not from a timer.
        var drainTimeout = traversal ? COMMAND_TIMEOUT_MS : DRAIN_TIMEOUT_MS;
        if (!drained.await(drainTimeout, TimeUnit.MILLISECONDS)) {
            log.warn("Companion sync drain timed out for {}", id);
        }
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
     * Build the structured outcome from the captured output. Collect the console
     * lines first, then classify with whole-output context (see firstErrorLine).
     */
    private static EventEnvelope outcomeResponse(String id, String command, List<Object> buffer) {
        List<String> output = new ArrayList<>();
        List<Object> result = new ArrayList<>();
        for (var item : buffer) {
            if (item instanceof String line) {
                output.add(line);
            } else {
                result.add(item);
            }
        }
        String error = firstErrorLine(output);
        var body = new HashMap<String, Object>();
        body.put("ok", error == null);
        body.put("id", id);
        body.put(COMMAND, command);
        body.put("output", output);
        body.put("error", error);
        body.put("result", result.isEmpty() ? null : result);
        return new EventEnvelope().setHeader("Content-Type", "application/json").setBody(body);
    }

    private static boolean isErrorLine(String line) {
        // A usage hint beginning with the Syntax prefix is the engine's rejection of a
        // malformed command. The command did nothing, so the caller must see ok=false
        // per finding 63. No help page starts a line with that prefix, which rules out
        // a false positive.
        return line.startsWith("ERROR:") || line.contains("aborted") || line.contains("does not have")
                || line.startsWith("Invalid") || line.contains("not found") || line.contains("Please try 'help'")
                || line.startsWith("Syntax:");
    }

    /**
     * Whole-output-aware error classification. {@code import graph from {deployed}}
     * legitimately prints "Graph model not found in /tmp/..." before falling back
     * to the deployed classpath copy — a benign line that must not mark the
     * command failed. It is forgiven <b>only</b> when the same output also carries
     * the fallback's success marker; a genuinely missing model prints the
     * not-found line alone and stays an error.
     */
    private static String firstErrorLine(List<String> lines) {
        var deployedFallback = lines.stream().anyMatch(l -> l.contains("Found deployed graph model"));
        for (var line : lines) {
            if (deployedFallback && line.startsWith("Graph model not found in")) {
                continue;
            }
            if (isErrorLine(line)) {
                return line;
            }
        }
        return null;
    }

    /**
     * The Playground's only <b>asynchronous</b> command: {@code run} launches the
     * traveler, which streams its output after the command handler has already
     * replied — so the sentinel drain races the traversal tail. A traversal is
     * drained on its terminal line instead (see {@link #isTraversalTerminal}).
     */
    private static boolean isTraversalCommand(String command) {
        return "run".equalsIgnoreCase(command);
    }

    /**
     * The traveler's end-of-transmission lines. One of these is <b>always</b>
     * emitted last (success or failure), so the synchronous endpoint drains a
     * traversal deterministically — no timer, no truncated capture.
     */
    private static boolean isTraversalTerminal(String line) {
        return line.startsWith("Graph traversal completed in") || line.equals("Graph traversal aborted");
    }
}
