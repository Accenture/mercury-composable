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

package org.platformlambda.automation.services;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import org.platformlambda.automation.config.RoutingEntry;
import org.platformlambda.automation.models.AsyncContextHolder;
import org.platformlambda.automation.models.EventStreamState;
import org.platformlambda.automation.models.HeaderInfo;
import org.platformlambda.automation.util.SimpleHttpUtility;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.EventStreamWriter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Progressive rendering of a streaming HTTP response (the multi-shot return route).
 * <p>
 * A callee streams by sending events to the caller's reply_to, each marked with the
 * reserved envelope header {@code x-event-stream: data | eof | exception}. The first
 * data event commits the HTTP head; subsequent segments append progressively; eof ends
 * the response; exception fails it in-band. The marker is internal protocol - the wire
 * carries only standard HTTP: chunked transfer, and Server-Sent Events framing when the
 * content type is text/event-stream.
 * <p>
 * This is reserved for system use. DO NOT use this directly in your application code.
 */
public class EventStreamRenderer {
    private static final Logger log = LoggerFactory.getLogger(EventStreamRenderer.class);
    private static final Utility util = Utility.getInstance();

    private static final String DATA = EventStreamWriter.DATA;
    private static final String EOF = EventStreamWriter.EOF;
    private static final String EXCEPTION = EventStreamWriter.EXCEPTION;
    private static final String ENVELOPE = EventStreamWriter.ENVELOPE;
    private static final String X_EVENT_STREAM = EventStreamWriter.X_EVENT_STREAM;
    private static final String X_EVENT_NAME = EventStreamWriter.X_EVENT_NAME;
    private static final String X_TTL = "x-ttl";
    private static final String X_STREAM_ID = "x-stream-id";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String SET_COOKIE = "set-cookie";
    private static final String CACHE_CONTROL = "Cache-Control";
    private static final String NO_CACHE = "no-cache";
    private static final String TEXT_EVENT_STREAM = "text/event-stream";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";
    private static final String TEXT_HTML = "text/html";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String ACCEPT_ANY = "*/*";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";
    private static final String TYPE = "type";
    private static final String ERROR = "error";
    private static final String DONE = "done";
    private static final String EVENT_PREFIX = "event: ";
    private static final String DATA_PREFIX = "data: ";
    private static final String LF = "\n";
    private static final byte[] SSE_PING = ": ping\n\n".getBytes(StandardCharsets.UTF_8);
    // slow-client guard: writes queue here when the socket back-pressures, bounded by this cap
    private static final long PENDING_CAP_BYTES = 1024 * 1024L;
    private static final long KEEP_ALIVE_MS = resolveKeepAlive();
    // available reply lanes (LIFO stack): a streaming request checks out a dedicated
    // single-instance route for its lifetime and returns it when the request context
    // closes - the "ready" signal pattern of the reactive manager/worker design
    private static final ConcurrentLinkedDeque<String> LANE_POOL = new ConcurrentLinkedDeque<>();

    private EventStreamRenderer() {}

    private static long resolveKeepAlive() {
        // SSE keep-alive comment interval; 0 disables (default 30 seconds)
        AppConfigReader config = AppConfigReader.getInstance();
        int seconds = util.getDurationInSeconds(config.getProperty("event.stream.keep.alive", "30s"));
        return seconds * 1000L;
    }

    /**
     * Check out a dedicated ordered reply lane for one streaming request
     *
     * @return the lane's route name, or null when the pool is exhausted
     */
    public static String checkoutLane() {
        return LANE_POOL.poll();
    }

    /**
     * Return a reply lane to the pool - called when the owning request context closes,
     * and at startup to fill the pool
     *
     * @param route the lane's route name
     */
    public static void releaseLane(String route) {
        LANE_POOL.push(route);
    }

    /**
     * @return the number of reply lanes currently available for checkout
     */
    public static int getAvailableLanes() {
        return LANE_POOL.size();
    }

    /**
     * Validate the x-event-stream marker; an unknown value is dropped with a warning
     *
     * @param requestId the request correlation id
     * @param signal the x-event-stream marker value
     * @return true when the signal is not a valid stream marker
     */
    private static boolean invalidSignal(String requestId, String signal) {
        if (DATA.equals(signal) || EOF.equals(signal) || EXCEPTION.equals(signal)) {
            return false;
        }
        log.warn("Dropping event for {} - invalid {} signal '{}'", requestId, X_EVENT_STREAM, signal);
        return true;
    }

    /**
     * Extract the x-event-stream marker from an envelope (case-insensitive), if any
     *
     * @param event the incoming envelope
     * @return the marker value or null
     */
    public static String getSignal(EventEnvelope event) {
        for (Map.Entry<String, String> kv : event.getHeaders().entrySet()) {
            if (X_EVENT_STREAM.equalsIgnoreCase(kv.getKey())) {
                return kv.getValue().toLowerCase();
            }
        }
        return null;
    }

    /**
     * Handle one streaming event for an open HTTP request context
     *
     * @param requestId the request correlation id
     * @param holder the request context
     * @param event the incoming envelope
     * @param signal the x-event-stream marker value
     */
    public static void handle(String requestId, AsyncContextHolder holder, EventEnvelope event, String signal) {
        if (invalidSignal(requestId, signal)) {
            return;
        }
        if (holder.eventStream == null) {
            if (EXCEPTION.equals(signal)) {
                // failure before the head is committed - render a normal HTTP error
                sendErrorBeforeHead(requestId, holder, event);
                return;
            }
            openStream(requestId, holder, event);
        }
        switch (signal) {
            case DATA -> renderData(requestId, holder, event);
            case EOF -> renderEof(requestId, holder, event);
            default -> renderException(requestId, holder, event);
        }
    }

    /**
     * Envelope-mode rendering for the Event-over-HTTP streaming relay ("/api/event"
     * with a caller that accepts text/event-stream). The wire is the hybrid dialect:
     * envelope frames (SSE event name "envelope", one base64-encoded serialized
     * EventEnvelope per frame) wherever envelope semantics matter - the head, the
     * terminals and non-text segments - and raw SSE frames for plain text segments.
     * The terminals end the response cleanly; the engine-to-engine wire carries no
     * cosmetic done/error frames because the decoded terminal envelope is the signal.
     *
     * @param requestId the request correlation id
     * @param holder the request context
     * @param event the incoming envelope
     * @param signal the x-event-stream marker value
     */
    public static void handleEnvelopeMode(String requestId, AsyncContextHolder holder,
                                          EventEnvelope event, String signal) {
        if (invalidSignal(requestId, signal)) {
            return;
        }
        boolean first = holder.eventStream == null;
        if (first) {
            // uniform SSE, a pre-head failure included - the caller always receives
            // the exact envelope, while the transport head mirrors its status
            openEnvelopeStream(requestId, holder, event);
        }
        EventStreamState state = holder.eventStream;
        if (state.isClosed()) {
            return;
        }
        holder.touch();
        if (DATA.equals(signal)) {
            Buffer frame = envelopeModeDataFrame(holder, event, first);
            if (frame != null) {
                state.incrementEventCount();
                state.addByteCount(frame.length());
                writeOrQueue(requestId, holder, frame);
            }
        } else {
            writeOrQueue(requestId, holder, envelopeFrame(holder, event));
            finish(requestId, holder, signal);
        }
    }

    private static void openEnvelopeStream(String requestId, AsyncContextHolder holder, EventEnvelope event) {
        HttpServerResponse response = holder.request.response();
        if (event.getStatus() != 200) {
            response.setStatusCode(event.getStatus());
        }
        if (holder.cidHeaderName != null && holder.businessCorrelationId != null) {
            response.putHeader(holder.cidHeaderName, holder.businessCorrelationId);
        }
        // the target's own headers stay inside the envelope frames; only the endpoint's
        // response header transform (add rules) reaches the outer response
        applyTransformOnlyHeaders(response, holder);
        response.putHeader(CONTENT_TYPE, TEXT_EVENT_STREAM);
        if (!response.headers().contains(CACHE_CONTROL)) {
            response.putHeader(CACHE_CONTROL, NO_CACHE);
        }
        var state = new EventStreamState(EventStreamState.Mode.SSE);
        holder.eventStream = state;
        response.setChunked(true);
        applyIdleTimeoutOverride(holder, event);
        response.closeHandler(unused -> cleanup(requestId, holder));
        if (KEEP_ALIVE_MS > 0) {
            long timer = Platform.getInstance().getVertx().setPeriodic(KEEP_ALIVE_MS, t -> ping(holder));
            state.setKeepAliveTimer(timer);
        }
    }

    private static void applyTransformOnlyHeaders(HttpServerResponse response, AsyncContextHolder holder) {
        if (holder.resHeaderId != null) {
            var httpUtil = SimpleHttpUtility.getInstance();
            HeaderInfo hi = RoutingEntry.getInstance().getResponseHeaderInfo(holder.resHeaderId);
            Map<String, String> added = httpUtil.filterHeaders(hi, new HashMap<>());
            for (Map.Entry<String, String> kv : added.entrySet()) {
                String prettyHeader = httpUtil.getHeaderCase(kv.getKey());
                if (prettyHeader != null) {
                    response.putHeader(prettyHeader, kv.getValue());
                }
            }
        }
    }

    private static Buffer envelopeModeDataFrame(AsyncContextHolder holder, EventEnvelope event, boolean first) {
        if (first || hasEnvelopeSemantics(event)) {
            return envelopeFrame(holder, event);
        }
        Object body = event.getRawBody();
        // a bare no-op segment carries nothing - parity with raw-mode rendering
        return body == null ? null : sseFrame(getEventName(event), (String) body);
    }

    /**
     * A data segment must ride as an envelope frame when a raw SSE frame cannot carry
     * it losslessly: a non-text body, text containing a carriage return (SSE normalizes
     * line endings), a custom envelope header, a non-200 status, or a user event name
     * that collides with the reserved "envelope" word.
     *
     * @param event the data segment
     * @return true when the segment needs the envelope-frame escape hatch
     */
    private static boolean hasEnvelopeSemantics(EventEnvelope event) {
        if (event.getStatus() != 200) {
            return true;
        }
        for (Map.Entry<String, String> kv : event.getHeaders().entrySet()) {
            String key = kv.getKey().toLowerCase();
            boolean reserved = X_EVENT_STREAM.equals(key) || X_EVENT_NAME.equals(key) || X_TTL.equals(key);
            if (!reserved || (X_EVENT_NAME.equals(key) && ENVELOPE.equals(kv.getValue()))) {
                return true;
            }
        }
        Object body = event.getRawBody();
        return !(body == null || (body instanceof String text && !text.contains("\r")));
    }

    private static Buffer envelopeFrame(AsyncContextHolder holder, EventEnvelope event) {
        // server-internal addressing never leaks to the wire - the consuming relay
        // rewrites addressing to the original caller anyway
        EventEnvelope wire = event.copy().setTo(null).setReplyTo(null);
        EventEnvelope.Format format = holder.envelopeStreamFormat == null ?
                EventEnvelope.Format.STANDARD : holder.envelopeStreamFormat;
        return sseFrame(ENVELOPE, util.bytesToBase64(wire.toBytes(format)));
    }

    private static void sendErrorBeforeHead(String requestId, AsyncContextHolder holder, EventEnvelope event) {
        int status = event.getStatus() >= 400 ? event.getStatus() : 500;
        SimpleHttpUtility.getInstance().sendError(requestId, holder.request, status, errorMessage(event));
    }

    private static String errorMessage(EventEnvelope event) {
        Object body = event.getRawBody();
        if (body instanceof Map<?, ?> map && map.get(MESSAGE) != null) {
            return String.valueOf(map.get(MESSAGE));
        }
        return body == null ? "Stream failed" : String.valueOf(body);
    }

    private static void openStream(String requestId, AsyncContextHolder holder, EventEnvelope event) {
        HttpServerResponse response = holder.request.response();
        if (event.getHeaders().keySet().stream().anyMatch(X_STREAM_ID::equalsIgnoreCase)) {
            // mutual exclusivity rule: x-event-stream wins over a stray x-stream-id
            log.warn("Ignoring {} on a streaming response for {}", X_STREAM_ID, requestId);
        }
        if (event.getStatus() != 200) {
            response.setStatusCode(event.getStatus());
        }
        if (holder.cidHeaderName != null && holder.businessCorrelationId != null) {
            response.putHeader(holder.cidHeaderName, holder.businessCorrelationId);
        }
        String contentType = applyResponseHeaders(response, holder, event);
        if (contentType == null) {
            contentType = negotiateContentType(holder.accept);
        }
        response.putHeader(CONTENT_TYPE, contentType);
        var state = new EventStreamState(contentType.startsWith(TEXT_EVENT_STREAM) ?
                EventStreamState.Mode.SSE : EventStreamState.Mode.CHUNKED);
        holder.eventStream = state;
        if (state.getMode() == EventStreamState.Mode.SSE && !response.headers().contains(CACHE_CONTROL)) {
            // default for SSE - an explicit event header or response header transform wins
            response.putHeader(CACHE_CONTROL, NO_CACHE);
        }
        response.setChunked(true);
        applyIdleTimeoutOverride(holder, event);
        // a disconnected client ends the stream; late segments become no-op drops
        response.closeHandler(unused -> cleanup(requestId, holder));
        if (state.getMode() == EventStreamState.Mode.SSE && KEEP_ALIVE_MS > 0) {
            long timer = Platform.getInstance().getVertx().setPeriodic(KEEP_ALIVE_MS, t -> ping(holder));
            state.setKeepAliveTimer(timer);
        }
    }

    /**
     * Copy public headers from the head event to the HTTP response, skipping the
     * reserved envelope headers that never appear on the wire, and apply the
     * endpoint's response header transform (rest.yaml "headers.response"
     * add/keep/drop) exactly as a single-shot response would
     *
     * @param response the HTTP response
     * @param holder the request context
     * @param event the head event
     * @return the content type declared by the event, if any
     */
    private static String applyResponseHeaders(HttpServerResponse response, AsyncContextHolder holder,
                                               EventEnvelope event) {
        var httpUtil = SimpleHttpUtility.getInstance();
        String contentType = null;
        Map<String, String> resHeaders = new HashMap<>();
        for (Map.Entry<String, String> kv : event.getHeaders().entrySet()) {
            String key = kv.getKey().toLowerCase();
            switch (key) {
                case X_EVENT_STREAM, X_EVENT_NAME, X_TTL, X_STREAM_ID -> { /* reserved - never on the wire */ }
                case SET_COOKIE -> httpUtil.setCookies(response, kv.getValue());
                default -> {
                    if (CONTENT_TYPE.equalsIgnoreCase(key)) {
                        contentType = kv.getValue().toLowerCase();
                    } else {
                        resHeaders.put(key, kv.getValue());
                    }
                }
            }
        }
        if (holder.resHeaderId != null) {
            HeaderInfo hi = RoutingEntry.getInstance().getResponseHeaderInfo(holder.resHeaderId);
            resHeaders = httpUtil.filterHeaders(hi, resHeaders);
        }
        for (Map.Entry<String, String> kv : resHeaders.entrySet()) {
            String prettyHeader = httpUtil.getHeaderCase(kv.getKey());
            if (prettyHeader != null) {
                response.putHeader(prettyHeader, kv.getValue());
            }
        }
        return contentType;
    }

    private static void applyIdleTimeoutOverride(AsyncContextHolder holder, EventEnvelope event) {
        for (Map.Entry<String, String> kv : event.getHeaders().entrySet()) {
            if (X_TTL.equalsIgnoreCase(kv.getKey())) {
                long seconds = util.str2long(kv.getValue());
                if (seconds > 0) {
                    holder.setTimeout(seconds * 1000L);
                }
            }
        }
    }

    private static String negotiateContentType(String accept) {
        if (accept == null || accept.contains(ACCEPT_ANY) || accept.contains(APPLICATION_JSON)) {
            return APPLICATION_JSON;
        } else if (accept.contains(TEXT_EVENT_STREAM)) {
            return TEXT_EVENT_STREAM;
        } else if (accept.contains(TEXT_HTML)) {
            return TEXT_HTML;
        } else if (accept.contains(APPLICATION_XML)) {
            return APPLICATION_XML;
        } else {
            return TEXT_PLAIN;
        }
    }

    private static void ping(AsyncContextHolder holder) {
        EventStreamState state = holder.eventStream;
        if (state == null) {
            return;
        }
        // pings are best-effort and must not touch the context (they would defeat the
        // idle timeout); tryLock keeps them from interleaving with a flush in progress
        if (state.getLock().tryLock()) {
            try {
                HttpServerResponse response = holder.request.response();
                if (!state.isClosed() && !response.ended() && !response.writeQueueFull()) {
                    response.write(Buffer.buffer(SSE_PING));
                }
            } finally {
                state.getLock().unlock();
            }
        }
    }

    private static void renderData(String requestId, AsyncContextHolder holder, EventEnvelope event) {
        EventStreamState state = holder.eventStream;
        if (state.isClosed()) {
            return;
        }
        holder.touch();
        Object body = event.getRawBody();
        if (body == null) {
            return;
        }
        final Buffer buffer;
        if (state.getMode() == EventStreamState.Mode.SSE) {
            buffer = sseFrame(getEventName(event), toText(body));
        } else {
            buffer = chunkedContent(body);
        }
        if (buffer.length() > 0) {
            state.incrementEventCount();
            state.addByteCount(buffer.length());
            writeOrQueue(requestId, holder, buffer);
        }
    }

    private static String getEventName(EventEnvelope event) {
        for (Map.Entry<String, String> kv : event.getHeaders().entrySet()) {
            if (X_EVENT_NAME.equalsIgnoreCase(kv.getKey())) {
                return kv.getValue();
            }
        }
        return null;
    }

    private static Buffer chunkedContent(Object body) {
        return switch (body) {
            case String text -> Buffer.buffer(util.getUTF(text));
            case byte[] bytes -> Buffer.buffer(bytes);
            case Map<?, ?> map -> ndJson(map);
            case List<?> list -> ndJson(list);
            default -> Buffer.buffer(util.getUTF(String.valueOf(body)));
        };
    }

    private static Buffer ndJson(Object structured) {
        // one COMPACT JSON object per line (NDJSON) for structured segments in chunked mode
        return Buffer.buffer(util.getUTF(
                SimpleMapper.getInstance().getCompactGson().toJson(structured) + LF));
    }

    private static String toText(Object body) {
        // structured segments render as compact one-line JSON - stream framing is line-oriented
        return switch (body) {
            case String text -> text;
            case byte[] bytes -> new String(bytes, StandardCharsets.UTF_8);
            case Map<?, ?> map -> SimpleMapper.getInstance().getCompactGson().toJson(map);
            case List<?> list -> SimpleMapper.getInstance().getCompactGson().toJson(list);
            default -> String.valueOf(body);
        };
    }

    private static Buffer sseFrame(String eventName, String text) {
        StringBuilder frame = new StringBuilder();
        if (eventName != null && !eventName.isEmpty()) {
            frame.append(EVENT_PREFIX).append(eventName).append(LF);
        }
        // multi-line data becomes successive "data:" lines per the SSE specification
        for (String line : text.split(LF, -1)) {
            frame.append(DATA_PREFIX).append(line).append(LF);
        }
        frame.append(LF);
        return Buffer.buffer(util.getUTF(frame.toString()));
    }

    private static void renderEof(String requestId, AsyncContextHolder holder, EventEnvelope event) {
        EventStreamState state = holder.eventStream;
        if (state.isClosed()) {
            return;
        }
        holder.touch();
        if (state.getMode() == EventStreamState.Mode.SSE) {
            Object body = event.getRawBody();
            String text = body == null ? "{}" : toText(body);
            writeOrQueue(requestId, holder, sseFrame(DONE, text));
        }
        finish(requestId, holder, EOF);
    }

    private static void renderException(String requestId, AsyncContextHolder holder, EventEnvelope event) {
        EventStreamState state = holder.eventStream;
        if (state.isClosed()) {
            return;
        }
        // the head is already committed - fail in-band (SSE) or truncate (chunked)
        if (state.getMode() == EventStreamState.Mode.SSE) {
            int status = event.getStatus() >= 400 ? event.getStatus() : 500;
            var error = Map.of(STATUS, status, MESSAGE, errorMessage(event), TYPE, ERROR);
            writeOrQueue(requestId, holder, sseFrame(ERROR, toText(error)));
        }
        finish(requestId, holder, EXCEPTION);
    }

    private static void writeOrQueue(String requestId, AsyncContextHolder holder, Buffer buffer) {
        EventStreamState state = holder.eventStream;
        state.getLock().lock();
        try {
            if (!state.offer(buffer, PENDING_CAP_BYTES)) {
                log.error("Closing event stream for {} - client too slow ({} bytes pending)",
                        requestId, state.getPendingBytes());
                hardClose(requestId, holder);
                return;
            }
            flushPending(requestId, holder);
        } finally {
            state.getLock().unlock();
        }
    }

    /**
     * Flush queued buffers to the socket - the caller must hold the stream lock.
     * Offers come only from the request's ordered reply lane, so FIFO of the pending
     * queue is FIFO of the stream; the lock keeps this flusher and the vert.x drain
     * handler from interleaving.
     */
    private static void flushPending(String requestId, AsyncContextHolder holder) {
        EventStreamState state = holder.eventStream;
        HttpServerResponse response = holder.request.response();
        if (state.isClosed() || response.ended()) {
            return;
        }
        while (state.hasPending() && !response.writeQueueFull()) {
            response.write(state.poll());
        }
        if (state.hasPending()) {
            if (!state.isDrainHandlerSet()) {
                state.setDrainHandlerSet(true);
                response.drainHandler(unused -> {
                    state.getLock().lock();
                    try {
                        flushPending(requestId, holder);
                    } finally {
                        state.getLock().unlock();
                    }
                });
            }
        } else if (state.isEndAfterFlush()) {
            end(requestId, holder);
        }
    }

    private static void finish(String requestId, AsyncContextHolder holder, String outcome) {
        EventStreamState state = holder.eventStream;
        log.debug("Event stream for {} completed - outcome={} events={} bytes={}",
                requestId, outcome, state.getEventCount(), state.getByteCount());
        state.getLock().lock();
        try {
            if (state.hasPending()) {
                state.setEndAfterFlush(true);
            } else {
                end(requestId, holder);
            }
        } finally {
            state.getLock().unlock();
        }
    }

    private static void end(String requestId, AsyncContextHolder holder) {
        EventStreamState state = holder.eventStream;
        state.getLock().lock();
        try {
            state.setClosed(true);
            cancelKeepAlive(state);
            HttpRouter.closeContext(requestId);
            HttpServerResponse response = holder.request.response();
            if (!response.ended()) {
                response.end();
            }
        } finally {
            state.getLock().unlock();
        }
    }

    private static void hardClose(String requestId, AsyncContextHolder holder) {
        EventStreamState state = holder.eventStream;
        state.getLock().lock();
        try {
            state.setClosed(true);
            cancelKeepAlive(state);
            HttpRouter.closeContext(requestId);
            // terminate the connection - truncation is the only honest signal after the head is committed
            holder.request.connection().close();
        } finally {
            state.getLock().unlock();
        }
    }

    private static void cleanup(String requestId, AsyncContextHolder holder) {
        EventStreamState state = holder.eventStream;
        if (state != null && !state.isClosed()) {
            state.getLock().lock();
            try {
                log.debug("Client disconnected from event stream {}", requestId);
                state.setClosed(true);
                cancelKeepAlive(state);
                HttpRouter.closeContext(requestId);
            } finally {
                state.getLock().unlock();
            }
        }
    }

    private static void cancelKeepAlive(EventStreamState state) {
        if (state.getKeepAliveTimer() != -1) {
            Platform.getInstance().getVertx().cancelTimer(state.getKeepAliveTimer());
            state.setKeepAliveTimer(-1);
        }
    }
}
