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

package org.platformlambda.core.system;

import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Producer helper for HTTP response streaming (and any other multi-shot reply route).
 * <p>
 * The platform's native streaming pattern is: the caller provides a reply_to address and
 * the callee continuously sends events to it until a signal declares end of transmission.
 * This class is thin sugar over plain event sends - each segment is one event to the
 * caller's reply_to, marked with the reserved envelope header {@code x-event-stream}.
 * The marker is internal protocol between the callee and the caller (for HTTP, the REST
 * automation edge consumes it like x-stream-id / x-ttl); it never appears on the wire.
 * <p>
 * Typical use inside an {@code @EventInterceptor} function serving a rest.yaml endpoint:
 * <pre>
 * var out = new EventStreamWriter(request.getReplyTo(), request.getCorrelationId());
 * out.first(200, "text/event-stream");
 * out.write("Hello");                          // data segment
 * out.write("tokens", Map.of("n", 2));         // named (typed) SSE event
 * out.close(Map.of("usage", usage));           // end of transmission with trailing metadata
 * // or out.fail(e);                           // in-band failure
 * </pre>
 * Writes after close/fail are dropped (debug log) - by design, symmetrical with the
 * edge dropping late segments after a timeout or client disconnect.
 */
public class EventStreamWriter {
    private static final Logger log = LoggerFactory.getLogger(EventStreamWriter.class);

    // reserved envelope header (internal protocol, never on the HTTP wire)
    public static final String X_EVENT_STREAM = "x-event-stream";
    // optional companion on a data event: maps to the SSE "event:" field
    public static final String X_EVENT_NAME = "x-event-name";
    // marker vocabulary - deliberately the framework's ObjectStream vocabulary
    public static final String DATA = "data";
    public static final String EOF = "eof";
    public static final String EXCEPTION = "exception";
    // reserved SSE event name of the Event-over-HTTP envelope-mode wire dialect:
    // a frame with this name carries one base64-encoded serialized EventEnvelope
    public static final String ENVELOPE = "envelope";

    private static final String X_TTL = "x-ttl";
    private static final String CONTENT_TYPE = "content-type";
    private static final String TYPE = "type";
    private static final String ERROR = "error";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";

    private final String replyTo;
    private final String correlationId;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private int firstStatus = 200;
    private String firstContentType = null;
    private long firstTtlSeconds = -1;
    private boolean headSent = false;

    /**
     * Create a stream writer for a caller-provided reply route
     *
     * @param replyTo the caller's reply_to address
     * @param correlationId the caller's correlation id
     */
    public EventStreamWriter(String replyTo, String correlationId) {
        if (replyTo == null || replyTo.isEmpty()) {
            throw new IllegalArgumentException("Missing reply_to - a stream writer needs the caller's return route");
        }
        this.replyTo = replyTo;
        this.correlationId = correlationId;
    }

    /**
     * Create a stream writer from the incoming request envelope
     * (interceptor-style functions receive the raw envelope with reply_to and correlation id)
     *
     * @param request the incoming event envelope
     */
    public EventStreamWriter(EventEnvelope request) {
        this(request.getReplyTo(), request.getCorrelationId());
    }

    /**
     * Optional head control - status and content type ride the FIRST outgoing event.
     * For the HTTP edge, the first event commits the response head.
     *
     * @param status HTTP status for the response head
     * @param contentType response content type (e.g. text/event-stream)
     * @return this
     */
    public EventStreamWriter first(int status, String contentType) {
        this.firstStatus = status;
        this.firstContentType = contentType;
        return this;
    }

    /**
     * Optional head control including an idle-timeout override
     *
     * @param status HTTP status for the response head
     * @param contentType response content type
     * @param ttlSeconds idle allowance between segments (overrides the endpoint timeout)
     * @return this
     */
    public EventStreamWriter first(int status, String contentType, long ttlSeconds) {
        this.firstTtlSeconds = ttlSeconds;
        return first(status, contentType);
    }

    /**
     * Send one segment (String, byte[] or Map)
     *
     * @param segment content of this batch
     */
    public void write(Object segment) {
        send(segment, null);
    }

    /**
     * Send one named segment - the name maps to the SSE "event:" field
     *
     * @param eventName typed event name
     * @param segment content of this batch
     */
    public void write(String eventName, Object segment) {
        send(segment, eventName);
    }

    /**
     * Declare end of transmission
     */
    public void close() {
        close(null);
    }

    /**
     * Declare end of transmission with trailing metadata
     * (rendered as the terminal SSE event's data; ignored in chunked mode)
     *
     * @param trailingMetadata optional final payload
     */
    public void close(Object trailingMetadata) {
        if (closed.compareAndSet(false, true)) {
            sendUnchecked(EOF, trailingMetadata, null);
        }
    }

    /**
     * Declare an in-band failure and end the stream
     *
     * @param e the failure
     */
    public void fail(Throwable e) {
        if (closed.compareAndSet(false, true)) {
            int status = e instanceof AppException appEx ? appEx.getStatus() : 500;
            // the standard error key-values: '{"type": "error", "status": n, "message": text}'
            Map<String, Object> error = new HashMap<>();
            error.put(TYPE, ERROR);
            error.put(STATUS, status);
            error.put(MESSAGE, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            var event = envelope(EXCEPTION, error, null).setStatus(status);
            EventEmitter.getInstance().send(event);
        }
    }

    /**
     * @return true when the stream has been closed or failed
     */
    public boolean isClosed() {
        return closed.get();
    }

    private void send(Object body, String eventName) {
        if (closed.get()) {
            log.debug("Segment to {} dropped - stream already closed", replyTo);
            return;
        }
        sendUnchecked(DATA, body, eventName);
    }

    private void sendUnchecked(String type, Object body, String eventName) {
        EventEmitter.getInstance().send(envelope(type, body, eventName));
    }

    private EventEnvelope envelope(String type, Object body, String eventName) {
        var event = new EventEnvelope().setTo(replyTo).setHeader(X_EVENT_STREAM, type);
        if (correlationId != null) {
            event.setCorrelationId(correlationId);
        }
        if (eventName != null && !eventName.isEmpty()) {
            event.setHeader(X_EVENT_NAME, eventName);
        }
        if (!headSent) {
            headSent = true;
            event.setStatus(firstStatus);
            if (firstContentType != null) {
                event.setHeader(CONTENT_TYPE, firstContentType);
            }
            if (firstTtlSeconds > 0) {
                event.setHeader(X_TTL, String.valueOf(firstTtlSeconds));
            }
        }
        if (body != null) {
            event.setBody(body);
        }
        return event;
    }
}
