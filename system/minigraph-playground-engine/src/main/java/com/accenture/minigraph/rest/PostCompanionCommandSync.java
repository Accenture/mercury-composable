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
 * command's {@code out}; the capture route buffers each line (and tees it), then a
 * FIFO sentinel marks the buffer fully drained. The existing endpoint and the
 * WebSocket console are unchanged.
 */
@OptionalService("app.env=dev")
@PreLoad(route = "post.companion.command.sync", instances = 10)
public class PostCompanionCommandSync implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private static final Logger log = LoggerFactory.getLogger(PostCompanionCommandSync.class);

    private static final String SYNC_SENTINEL = "__companion_sync_done__";
    private static final long COMMAND_TIMEOUT_MS = 30000;
    private static final long DRAIN_TIMEOUT_MS = 5000;

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) throws Exception {
        var id = input.getPathParameter("id");
        if (id == null) {
            throw new IllegalArgumentException("Missing path parameter: id");
        }
        if (!(input.getBody() instanceof String raw)) {
            throw new IllegalArgumentException("Body must be a non-empty text/plain command");
        }
        var command = raw.trim();
        if (command.isEmpty()) {
            throw new IllegalArgumentException("Body must be a non-empty text/plain command");
        }
        if (!GraphCommandService.hasSession(id)) {
            throw new AppException(404, "No active session for id " + id);
        }
        // Inverse of GraphUserInterface's public session id mapping.
        var route = id.replace('-', '.');
        var inRoute = route + ".in";
        var outRoute = route + ".out";
        var captureRoute = "companion.sync." + Utility.getInstance().getUuid();

        var po = new PostOffice(headers, instance);
        var platform = Platform.getInstance();
        var emitter = EventEmitter.getInstance();

        // A private, per-call capture route: buffer each line for the in-band response and
        // tee it to the session's real WebSocket .out for the live human view.
        List<Object> buffer = new CopyOnWriteArrayList<>();
        CountDownLatch drained = new CountDownLatch(1);
        LambdaFunction capture = (hdr, body, inst) -> {
            if (SYNC_SENTINEL.equals(body)) {
                drained.countDown();
            } else {
                buffer.add(body);
                try {
                    emitter.send(new EventEnvelope().setTo(outRoute).setBody(body));
                } catch (Exception e) {
                    // best-effort tee: the .out route may have no live WebSocket
                    log.debug("Tee to {} skipped - {}", outRoute, e.getMessage());
                }
            }
            return null;
        };
        platform.registerPrivate(captureRoute, capture, 1);
        try {
            // RPC the singleton handler with the capture route as `out`; its handleEvent
            // completes (returns) only after all command output has been enqueued.
            po.request(new EventEnvelope()
                    .setTo(GraphCommandService.SINGLETON_COMMAND_HANDLER)
                    .setBody(Map.of(
                            "type", "command",
                            "in", inRoute,
                            "out", captureRoute,
                            "message", command)),
                    COMMAND_TIMEOUT_MS).get();
            // The sentinel is enqueued after the command's (FIFO) output, so seeing it means
            // the buffer is fully drained - deterministic, no arbitrary sleep.
            po.send(new EventEnvelope().setTo(captureRoute).setBody(SYNC_SENTINEL));
            if (!drained.await(DRAIN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                log.warn("Companion sync drain timed out for {}", id);
            }
        } finally {
            platform.release(captureRoute);
        }

        // Build the structured outcome from the captured output.
        List<String> output = new ArrayList<>();
        List<Object> result = new ArrayList<>();
        String error = null;
        for (var item : buffer) {
            if (item instanceof String line) {
                if (error == null && isErrorLine(line)) {
                    error = line;
                }
                output.add(line);
            } else {
                result.add(item);
            }
        }
        var body = new HashMap<String, Object>();
        body.put("ok", error == null);
        body.put("id", id);
        body.put("command", command);
        body.put("output", output);
        body.put("error", error);
        body.put("result", result.isEmpty() ? null : result);
        return new EventEnvelope().setHeader("Content-Type", "application/json").setBody(body);
    }

    private static boolean isErrorLine(String line) {
        return line.startsWith("ERROR:") || line.contains("aborted") || line.contains("does not have")
                || line.startsWith("Invalid") || line.contains("not found") || line.contains("Please try 'help'");
    }
}
