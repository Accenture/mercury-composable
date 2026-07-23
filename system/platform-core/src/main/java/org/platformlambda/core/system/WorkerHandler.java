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
import org.platformlambda.core.logging.LogContext;
import org.platformlambda.core.logging.LogContextConfig;
import org.platformlambda.core.logging.LogContextManager;
import org.platformlambda.core.models.*;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.serializers.SimpleObjectMapper;
import org.platformlambda.core.services.Telemetry;
import org.platformlambda.core.services.TemporaryInbox;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class WorkerHandler {
    private static final Logger log = LoggerFactory.getLogger(WorkerHandler.class);
    private static final Utility util = Utility.getInstance();
    private static final String CASTING_ERROR = "cannot be cast to class org.platformlambda.core.models.EventEnvelope";
    private static final String ID = "id";
    private static final String PATH = "path";
    private static final String SUCCESS = "success";
    private static final String FROM = "from";
    private static final String UNKNOWN = "unknown";
    private static final String EXEC_TIME = "exec_time";
    private static final String ORIGIN = "origin";
    private static final String SERVICE = "service";
    private static final String START = "start";
    private static final String TRACE = "trace";
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";
    private static final String HEADERS = "headers";
    private static final String BODY = "body";
    private static final String STATUS = "status";
    private static final String EXCEPTION = "exception";
    private static final String SPAN_ID = "span_id";
    private static final String PARENT_SPAN_ID = "parent_span_id";
    private static final String ASYNC = "async";
    private static final String ANNOTATIONS = "annotations";
    private static final String JOURNAL = "journal";
    private static final String MY_ROUTE = "my_route";
    private static final String MY_TRACE_ID = "my_trace_id";
    private static final String MY_TRACE_PATH = "my_trace_path";
    private static final String MY_CORRELATION_ID = "my_correlation_id";
    private static final String X_EVENT_API = "x-event-api";
    private static final String X_STREAM_ID = "x-stream-id";
    private static final String X_TTL = "x-ttl";
    private static final String READY = "ready:";
    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
    private final boolean tracing;
    private final ServiceDef def;
    private final String route;
    private final String parentRoute;
    private final int instance;
    private final String myOrigin;
    private final boolean interceptor;
    private final boolean useEnvelope;

    public WorkerHandler(ServiceDef def, String route, int instance,
                         boolean tracing, boolean interceptor, boolean useEnvelope) {
        this.route = route;
        this.parentRoute = route.contains("#")? route.substring(0, route.lastIndexOf('#')) : route;
        this.instance = instance;
        this.def = def;
        this.tracing = tracing;
        this.interceptor = interceptor;
        this.useEnvelope = useEnvelope;
        this.myOrigin = Platform.getInstance().getOrigin();
    }

    public void executeFunction(EventEnvelope event) {
        if (!TemporaryInbox.TEMPORARY_INBOX.equals(def.getRoute())) {
            event.clearAnnotations();
        }
        String rpc = event.getTag(EventEmitter.RPC);
        EventEmitter po = EventEmitter.getInstance();
        String ref = tracing? po.startTracing(parentRoute, event.getTraceId(), event.getTracePath(), event.getSpanId(), instance) : "?";
        // Register the application log context for this worker thread, in lockstep with the trace
        // bracket. The JSON appenders look it up by thread id at log time. Gated on a real trace
        // having been created and on the log-context feature being enabled.
        long threadId = Thread.currentThread().threadId();
        if (tracing && LogContextConfig.getInstance().isEnabled()) {
            // tracing defaults on for every function, but a context only makes sense once a real
            // traceId is available (trace.id != null), per the log-context contract.
            TraceInfo logTrace = po.getTrace(parentRoute, instance);
            if (logTrace != null && logTrace.id != null) {
                LogContextManager.register(threadId, new LogContext(logTrace, event.getCorrelationId()));
            }
        }
        ProcessStatus ps = processEvent(event, rpc);
        TraceInfo trace = po.stopTracing(ref);
        // processEvent never throws (it has a catch-all), so this always runs - no leak. Cheap no-op
        // when nothing was registered. A Mono/Flux completion after this point has no context (see docs).
        LogContextManager.remove(threadId);
        if (tracing && trace != null && trace.id != null && trace.path != null) {
            sendTracingInfo(event.getFrom(), rpc, ps, trace);
        } else if (ps.isNotDelivered()) {
            log.error("Delivery error - {}, from={}, to={}, type={}, exec_time={}",
                    ps.getDeliveryError(),
                    event.getFrom() == null? UNKNOWN : event.getFrom(), event.getTo(),
                    ps.isSuccess()? "response" : "exception("+ps.getStatus()+", "+ps.getException()+")",
                    ps.getExecutionTime());
        }
        /*
         * If this response is not a Mono reactive object, send a ready signal to inform the system this worker
         * is ready for next event. Otherwise, defer it until the Mono result is realized.
         *
         * This guarantee that this future task is executed orderly.
         */
        if (!ps.isReactive()) {
            Platform.getInstance().getEventSystem().send(def.getRoute(), READY + route);
        }
    }

    private void sendTracingInfo(String from, String rpc, ProcessStatus ps, TraceInfo trace) {
        EventEmitter po = EventEmitter.getInstance();
        try {
            boolean journaled = po.isJournaled(def.getRoute());
            if (journaled || rpc == null || ps.isNotDelivered()) {
                // Send tracing information to distributed trace logger
                EventEnvelope dt = new EventEnvelope().setTo(Telemetry.DISTRIBUTED_TRACING);
                Map<String, Object> payload = new HashMap<>();
                payload.put(ANNOTATIONS, trace.annotations);
                // send input/output dataset to journal if configured in journal.yaml
                if (journaled) {
                    payload.put(JOURNAL, ps.getInputOutput());
                }
                Map<String, Object> metrics = getMetrics(from, ps, trace);
                payload.put(TRACE, metrics);
                po.send(dt.setBody(payload));
            }
        } catch (IllegalArgumentException e) {
            log.error("Unable to send to {}", Telemetry.DISTRIBUTED_TRACING, e);
        }
    }

    private Map<String, Object> getMetrics(String from, ProcessStatus ps, TraceInfo trace) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put(ORIGIN, myOrigin);
        metrics.put(ID, trace.id);
        metrics.put(PATH, trace.path);
        metrics.put(SERVICE, def.getRoute());
        metrics.put(START, trace.startTime);
        metrics.put(SUCCESS, ps.isSuccess());
        metrics.put(FROM, from == null ? UNKNOWN : from);
        metrics.put(EXEC_TIME, ps.getExecutionTime());
        metrics.put(STATUS, ps.getStatus());
        if (!ps.isSuccess()) {
            metrics.put(EXCEPTION, ps.getException());
        }
        if (ps.isNotDelivered()) {
            metrics.put(STATUS, 500);
            metrics.put(SUCCESS, false);
            metrics.put(EXCEPTION, "Response not delivered - "+ ps.getDeliveryError());
        }
        if (trace.spanId != null) {
            metrics.put(SPAN_ID, trace.spanId);
        }
        if (trace.parentSpanId != null) {
            metrics.put(PARENT_SPAN_ID, trace.parentSpanId);
        }
        return metrics;
    }

    @SuppressWarnings("rawtypes")
    private ProcessStatus processEvent(EventEnvelope event, String rpc) {
        ProcessStatus ps = new ProcessStatus();
        ProcessMetadata md = new ProcessMetadata();
        md.input.put(HEADERS, event.getHeaders());
        md.input.put(BODY, event.getRawBody());
        md.inputOutput.put(INPUT, md.input);
        TypedLambdaFunction f = def.getFunction();
        final long begin = System.nanoTime();
        try {
            final Object body = prepareInputBody(event);
            /*
             * A user function receives a COPY of the envelope headers with read-only metadata
             * injected at delivery time. Metadata is never transported in the event itself:
             * engine-internal keys are removed from the copy, and the my_* keys below are
             * derived from envelope fields, the business-cid tag and worker context.
             */
            Map<String, String> parameters = new HashMap<>(event.getHeaders());
            parameters.remove(X_EVENT_API);
            // a legacy peer (pre-4.10.2) transported the business correlation-id as an envelope header
            String legacyBusinessCid = parameters.remove(MY_CORRELATION_ID);
            parameters.put(MY_ROUTE, parentRoute);
            if (event.getTraceId() != null) {
                parameters.put(MY_TRACE_ID, event.getTraceId());
            }
            if (event.getTracePath() != null) {
                parameters.put(MY_TRACE_PATH, event.getTracePath());
            }
            String businessCid = event.getTag(EventEmitter.BUSINESS_CID_TAG) != null?
                    event.getTag(EventEmitter.BUSINESS_CID_TAG) : legacyBusinessCid;
            if (businessCid != null) {
                parameters.put(MY_CORRELATION_ID, businessCid);
            }
            Object result = invokeFunction(f, parameters, body, event);
            md.diff = getExecTime(begin);
            String replyTo = event.getReplyTo();
            if (replyTo != null) {
                setCorrelation(md, event, rpc);
                sendResponse(ps, result, event, md, begin);
            } else {
                saveInputOutput(md, result);
            }
            return ps.setExecutionTime(md.diff).setInputOutput(md.inputOutput);
        } catch (NoClassDefFoundError | AssertionError | Exception e) {
            return getErrorProcessStatus(f, e, ps, md, event, begin);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object invokeFunction(TypedLambdaFunction f, Map<String, String> parameters, Object body,
                                  EventEnvelope event) throws Exception {
        try {
            return f.handleEvent(parameters, body, instance);
        } catch (ClassCastException ce) {
            // special case when the TypedLambdaFunction is defined in-line instead of a class
            String message = ce.getMessage();
            if (message != null && message.contains(CASTING_ERROR)) {
                var holder = new EventEnvelope().setBody(body);
                if (event.getType() != null) {
                    holder.setType(event.getType());
                }
                return f.handleEvent(parameters, holder, instance);
            } else {
                throw ce;
            }
        }
    }

    private void setCorrelation(ProcessMetadata md, EventEnvelope event, String rpc) {
        String replyTo = event.getReplyTo();
        long rpcTimeout = util.str2long(rpc);
        // if it is a callback instead of an RPC call, use default timeout of 30 minutes
        md.expiry = rpcTimeout < 0 ? DEFAULT_TIMEOUT : rpcTimeout;
        md.response.setTo(replyTo).setTags(event.getTags()).setFrom(def.getRoute());
        /*
         * Preserve correlation ID and extra information
         *
         * "Extra" is usually used by event interceptors.
         * For example, to save some metadata from the original sender.
         */
        if (event.getCorrelationId() != null) {
            md.response.setCorrelationId(event.getCorrelationId());
        }
        // propagate the trace to the next service if any
        if (event.getTraceId() != null) {
            md.response.setTrace(event.getTraceId(), event.getTracePath());
        }
    }

    private void sendResponse(ProcessStatus ps, Object result, EventEnvelope event, ProcessMetadata md, long begin) {
        EventEmitter po = EventEmitter.getInstance();
        boolean skipResponse = false;
        if (result instanceof Mono<?> mono) {
            skipResponse = true;
            // set reactive to defer service acknowledgement until Mono is complete
            ps.setReactive();
            handleMonoResponse(mono, event, md.response, begin, md.expiry);
        }
        var resultSet = result;
        /*
         * if response is a Flux, subscribe to it for a future response and immediately
         * return x-stream-id and x-ttl so the caller can use a FluxConsumer to read the stream.
         *
         * The response contract is two headers containing x-stream-id and x-ttl.
         * The response body is an empty map.
         */
        if (result instanceof Flux<?> flux) {
            resultSet = Collections.emptyMap();
            handleFlexResponse(flux, md.response, md.expiry);
        }
        boolean simulatedStreamTimeout = !skipResponse && updateResponse(md.response, resultSet);
        if (!md.response.getHeaders().isEmpty()) {
            md.output.put(HEADERS, md.response.getHeaders());
        }
        md.output.put(BODY, md.response.getRawBody() == null? "null" : md.response.getRawBody());
        md.output.put(STATUS, md.response.getStatus());
        md.inputOutput.put(OUTPUT, md.output);
        try {
            if (!interceptor && !skipResponse && !simulatedStreamTimeout) {
                var encoded = encodeTraceAnnotations(md.response).setExecutionTime(md.diff);
                if (result instanceof EventEnvelope evt && evt.getType() != null) {
                    encoded.setType(evt.getType());
                    encoded.setBody(evt.getBody());
                }
                po.send(encoded);
            }
        } catch (Exception e2) {
            ps.setUnDelivery(e2.getMessage());
        }
    }

    private void saveInputOutput(ProcessMetadata md, Object result) {
        EventEnvelope response = new EventEnvelope();
        updateResponse(response, result);
        md.output.put(BODY, response.getRawBody() == null? "null" : response.getRawBody());
        md.output.put(STATUS, response.getStatus());
        md.output.put(ASYNC, true);
        md.inputOutput.put(OUTPUT, md.output);
    }

    private ProcessStatus getErrorProcessStatus(TypedLambdaFunction<?, ?> f, Throwable e, ProcessStatus ps,
                                                ProcessMetadata md, EventEnvelope event, long begin) {
        EventEmitter po = EventEmitter.getInstance();
        md.diff = getExecTime(begin);
        final String replyTo = event.getReplyTo();
        final int status = getStatusFromException(e);
        String error = simplifyCastError(util.getRootCause(e));
        if (f instanceof MappingExceptionHandler handler) {
            try {
                handler.onError(parentRoute, new AppException(status, error), event, instance);
            } catch (Exception e3) {
                ps.setUnDelivery(e3.getMessage());
            }
            md.output.put(STATUS, status);
            md.output.put(EXCEPTION, error);
            md.inputOutput.put(OUTPUT, md.output);
            return ps.setException(status, error).setInputOutput(md.inputOutput);
        }
        if (replyTo != null) {
            final EventEnvelope errorResponse = prepareErrorResponse(event, e);
            try {
                po.send(encodeTraceAnnotations(errorResponse).setExecutionTime(md.diff));
            } catch (Exception e4) {
                ps.setUnDelivery(e4.getMessage());
            }
        } else {
            md.output.put(ASYNC, true);
            if (status >= 500) {
                log.error("Unhandled exception for {}", route, e);
            } else {
                log.warn("Unhandled exception for {} - {}", route, error);
            }
        }
        md.output.put(STATUS, status);
        md.output.put(EXCEPTION, error);
        md.inputOutput.put(OUTPUT, md.output);
        return ps.setException(status, error).setExecutionTime(md.diff).setInputOutput(md.inputOutput);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void handleFlexResponse(Flux flux, EventEnvelope response, long expiry) {
        var serializer = def.getCustomSerializer();
        FluxPublisher<Object> fluxRelay = new FluxPublisher<>(flux, expiry);
        fluxRelay.enableVirtualThread(def.isVirtualThread());
        if (serializer != null) {
            fluxRelay.setCustomSerializer(serializer);
        }
        response.setHeader(X_TTL, expiry);
        response.setHeader(X_STREAM_ID, fluxRelay.publish());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void handleMonoResponse(Mono mono, EventEnvelope event, EventEnvelope response, long begin, long expiry) {
        EventEmitter po = EventEmitter.getInstance();
        Platform platform = Platform.getInstance();
        // Capture the trace context now, on the worker thread. getTrace is keyed by thread-id and its entry
        // is removed when processEvent returns - so the Mono's completion callbacks, which run later on a
        // different thread, cannot look it up. Capturing here lets the response still carry this function's
        // span_id, so the next flow task can parent to it (otherwise the next task's span is orphaned).
        final TraceInfo traceContext = po.getTrace(parentRoute, instance);
        final AtomicLong timer = new AtomicLong(-1);
        final AtomicBoolean completed = new AtomicBoolean(false);
        // For non-blocking operation, use a new virtual/kernel thread for the subscription
        var executor = def.isVirtualThread()? platform.getVirtualThreadExecutor() : platform.getKernelThreadExecutor();
        final Disposable disposable = mono.doFinally(done -> {
                    long t1 = timer.get();
                    if (t1 > 0) {
                        platform.getVertx().cancelTimer(t1);
                    }
                    // finally, send service acknowledgement
                    platform.getEventSystem().send(def.getRoute(), READY + route);
                }).subscribeOn(Schedulers.fromExecutor(executor))
                .subscribe(data -> {
                    completed.set(true);
                    sendMonoResponse(traceContext, response, data, begin);
                }, e -> {
                    if (e instanceof Throwable ex) {
                        completed.set(true);
                        final EventEnvelope errorResponse = prepareErrorResponse(event, ex);
                        po.send(applyTraceContext(traceContext, errorResponse).setExecutionTime(getExecTime(begin)));
                    }
                }, () -> {
                    // When the Mono emitter sends a null payload, Mono will not return any result.
                    // Therefore, the system must return a null body for this normal use case.
                    if (!completed.get()) {
                        sendMonoResponse(traceContext, response, null, begin);
                    }
                });
        // dispose a pending Mono if timeout
        timer.set(platform.getVertx().setTimer(expiry, t -> {
            timer.set(-1);
            if (!disposable.isDisposed()) {
                log.warn("Async response timeout after {} for {}", util.elapsedTime(expiry), route);
                disposable.dispose();
            }
        }));
    }

    private Object prepareInputBody(EventEnvelope event) {
        final SimpleObjectMapper inputMapper = getMapper(true);
        var cls = def.getInputClass() != null ? def.getInputClass() : def.getPoJoClass();
        return (useEnvelope || (interceptor && cls == null)) ? event : getInputBody(event, cls, inputMapper);
    }

    private Object getInputBody(EventEnvelope event, Class<?> cls, SimpleObjectMapper inputMapper) {
        if (event.getRawBody() instanceof Map && cls != null) {
            return getMapBody(event, cls, inputMapper);
        } else if (event.getRawBody() instanceof List && cls != null) {
            return getListBody(event, cls, inputMapper);
        } else {
            return event.getBody();
        }
    }

    private Object getMapBody(EventEnvelope event, Class<?> cls, SimpleObjectMapper inputMapper) {
        var serializer = def.getCustomSerializer();
        if (cls == AsyncHttpRequest.class) {
            return new AsyncHttpRequest(event.getRawBody());
        } else {
            // convert Map to PoJo
            var o = event.getRawBody();
            return serializer != null? serializer.toPoJo(o, cls) : inputMapper.readValue(o, cls);
        }
    }

    private Object getListBody(EventEnvelope event, Class<?> cls, SimpleObjectMapper inputMapper) {
        // check if the input is a list of map
        List<?> inputList = (List<?>) event.getRawBody();
        // validate that the objects are PoJo or null
        int n = 0;
        for (Object o: inputList) {
            if (o == null || o instanceof Map) {
                n++;
            }
        }
        if (n == inputList.size()) {
            return getListOfMap(inputList, cls, inputMapper);
        } else {
            return event.getBody();
        }
    }

    private Object getListOfMap(List<?> inputList, Class<?> cls, SimpleObjectMapper inputMapper) {
        var serializer = def.getCustomSerializer();
        List<Object> updatedList = new ArrayList<>();
        for (Object o: inputList) {
            final Object pojo;
            if (o == null) {
                pojo = null;
            } else {
                pojo = serializer != null? serializer.toPoJo(o, cls) : inputMapper.readValue(o, cls);
            }
            updatedList.add(pojo);
        }
        return updatedList;
    }

    private SimpleObjectMapper getMapper(boolean isInput) {
        final ServiceDef.SerializationStrategy strategy = isInput?  def.getInputSerializationStrategy() :
                                                                    def.getOutputSerializationStrategy();
        final SimpleObjectMapper mapper;
        switch(strategy) {
            case ServiceDef.SerializationStrategy.CAMEL -> mapper = SimpleMapper.getInstance().getCamelCaseMapper();
            case ServiceDef.SerializationStrategy.SNAKE -> mapper = SimpleMapper.getInstance().getSnakeCaseMapper();
            default -> mapper = SimpleMapper.getInstance().getMapper();
        }
        return mapper;
    }

    private int getStatusFromException(Throwable e) {
        return switch (e) {
            case AppException ex -> ex.getStatus();
            case TimeoutException ignored -> 408;
            case IllegalArgumentException ignored -> 400;
            default -> 500;
        };
    }

    private void sendMonoResponse(TraceInfo traceContext, EventEnvelope response, Object result, long begin) {
        EventEmitter po = EventEmitter.getInstance();
        updateResponse(response, result);
        var encoded = applyTraceContext(traceContext, response).setExecutionTime(getExecTime(begin));
        if (result instanceof EventEnvelope evt && evt.getType() != null) {
            encoded.setType(evt.getType());
            encoded.setBody(evt.getBody());
        }
        po.send(encoded);
    }

    private EventEnvelope prepareErrorResponse(EventEnvelope event, Throwable e) {
        final EventEnvelope response = new EventEnvelope();
        response.setTo(event.getReplyTo()).setFrom(def.getRoute()).setException(e);
        if (event.getCorrelationId() != null) {
            response.setCorrelationId(event.getCorrelationId());
        }
        response.setTags(event.getTags());
        // propagate the trace to the next service if any
        if (event.getTraceId() != null) {
            response.setTrace(event.getTraceId(), event.getTracePath());
        }
        return response;
    }

    private boolean updateResponse(EventEnvelope response, Object result) {
        CustomSerializer serializer = def.getCustomSerializer();
        if (result instanceof EventEnvelope resultEvent) {
            return applyEnvelopeResult(resultEvent, response, serializer);
        } else {
            renderOutputBody(result, serializer, response);
            return false;
        }
    }

    /**
     * Apply an EventEnvelope result onto the response.
     *
     * @return true to simulate a READ timeout for ObjectStreamService, false otherwise
     */
    private boolean applyEnvelopeResult(EventEnvelope resultEvent, EventEnvelope response, CustomSerializer serializer) {
        Map<String, String> headers = resultEvent.getHeaders();
        if (headers.isEmpty() && resultEvent.getStatus() == 408 && resultEvent.getRawBody() == null) {
            // simulate a READ timeout for ObjectStreamService
            return true;
        }
        /*
         * When EventEnvelope is used as a return type, the system will transport
         * 1. payload
         * 2. key-values (as headers)
         */
        if (resultEvent.getType() != null) {
            response.setType(resultEvent.getType());
        }
        renderOutputBody(resultEvent.getOriginalBody(), serializer, response);
        copyResponseHeaders(headers, response);
        response.setStatus(resultEvent.getStatus());
        return false;
    }

    private void copyResponseHeaders(Map<String, String> headers, EventEnvelope response) {
        // exit-side sanitization, symmetric with the entry-side injection: the read-only
        // metadata keys and engine-internal keys never leave a function as response headers
        for (Map.Entry<String, String> kv: headers.entrySet()) {
            String k = kv.getKey();
            if (!MY_ROUTE.equals(k) && !MY_TRACE_ID.equals(k) && !MY_TRACE_PATH.equals(k)
                    && !MY_CORRELATION_ID.equals(k) && !X_EVENT_API.equals(k)) {
                response.setHeader(k, kv.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void renderOutputBody(Object result, CustomSerializer serializer, EventEnvelope response) {
        if (result instanceof List) {
            Set<String> names = new HashSet<>();
            List<Object> normalized = new ArrayList<>();
            List<Object> items = (List<Object>) result;
            for (Object o: items) {
                if (util.isPoJo(o)) {
                    normalized.add(outputPojoToMap(o, serializer));
                    names.add(o.getClass().getName());
                } else {
                    normalized.add(o);
                }
            }
            response.setBody(normalized);
            if (names.size() == 1) {
                response.setType(names.stream().toList().getFirst());
            }
        } else if (util.isPoJo(result)) {
            response.setBody(outputPojoToMap(result, serializer));
            response.setType(result.getClass().getName());
        } else {
            response.setBody(result);
        }
    }

    private Object outputPojoToMap(Object result, CustomSerializer serializer) {
        if (serializer != null) {
            return serializer.toMap(result);
        } else {
            final SimpleObjectMapper outputMapper = getMapper(false);
            return outputMapper.readValue(result, Map.class);
        }
    }

    private float getExecTime(long begin) {
        float delta = (float) (System.nanoTime() - begin) / EventEmitter.ONE_MILLISECOND;
        // adjust precision to 3 decimal points
        return Float.parseFloat(String.format(java.util.Locale.US, "%.3f", Math.max(0.0f, delta)));
    }

    private String simplifyCastError(Throwable ex) {
        String error = ex.getMessage();
        if (error == null) {
            return "null";
        } else if (ex instanceof ClassCastException) {
            int sep = error.lastIndexOf(" (");
            return sep > 0 ? error.substring(0, sep) : error;
        } else {
            return error;
        }
    }

    private EventEnvelope encodeTraceAnnotations(EventEnvelope response) {
        return applyTraceContext(EventEmitter.getInstance().getTrace(parentRoute, instance), response);
    }

    /**
     * Apply a (possibly pre-captured) trace context to a response. Callers on the worker thread can pass
     * {@code getTrace(...)} directly; the Mono/async path must capture the trace on the worker thread first,
     * because {@code getTrace} is keyed by thread-id and the entry is gone by the time the Mono completes
     * on another thread.
     */
    private EventEnvelope applyTraceContext(TraceInfo trace, EventEnvelope response) {
        if (trace != null) {
            response.setAnnotations(trace.annotations);
            // Carry this function's own span_id so the caller can use it as parentSpanId for the next hop
            if (trace.spanId != null) {
                response.setSpanId(trace.spanId);
            }
        }
        return response;
    }

    private static class ProcessMetadata {
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> output = new HashMap<>();
        Map<String, Object> inputOutput = new HashMap<>();
        EventEnvelope response = new EventEnvelope();
        long expiry;
        float diff;
    }
}
