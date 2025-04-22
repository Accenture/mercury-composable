/*

    Copyright 2018-2025 Accenture Technology

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
import org.platformlambda.core.models.*;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.services.DistributedTrace;
import org.platformlambda.core.services.TemporaryInbox;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
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
    private static final String ASYNC = "async";
    private static final String ANNOTATIONS = "annotations";
    private static final String JOURNAL = "journal";
    private static final String MY_ROUTE = "my_route";
    private static final String MY_TRACE_ID = "my_trace_id";
    private static final String MY_TRACE_PATH = "my_trace_path";
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
        String ref = tracing? po.startTracing(parentRoute, event.getTraceId(), event.getTracePath(), instance) : "?";
        ProcessStatus ps = processEvent(event, rpc);
        TraceInfo trace = po.stopTracing(ref);
        if (tracing && trace != null && trace.id != null && trace.path != null) {
            try {
                boolean journaled = po.isJournaled(def.getRoute());
                if (journaled || rpc == null || !ps.isDelivered()) {
                    // Send tracing information to distributed trace logger
                    EventEnvelope dt = new EventEnvelope().setTo(DistributedTrace.DISTRIBUTED_TRACING);
                    Map<String, Object> payload = new HashMap<>();
                    payload.put(ANNOTATIONS, trace.annotations);
                    // send input/output dataset to journal if configured in journal.yaml
                    if (journaled) {
                        payload.put(JOURNAL, ps.getInputOutput());
                    }
                    Map<String, Object> metrics = new HashMap<>();
                    metrics.put(ORIGIN, myOrigin);
                    metrics.put(ID, trace.id);
                    metrics.put(PATH, trace.path);
                    metrics.put(SERVICE, def.getRoute());
                    metrics.put(START, trace.startTime);
                    metrics.put(SUCCESS, ps.isSuccess());
                    metrics.put(FROM, event.getFrom() == null ? UNKNOWN : event.getFrom());
                    metrics.put(EXEC_TIME, ps.getExecutionTime());
                    metrics.put(STATUS, ps.getStatus());
                    if (!ps.isSuccess()) {
                        metrics.put(EXCEPTION, ps.getException());
                    }
                    if (!ps.isDelivered()) {
                        metrics.put(STATUS, 500);
                        metrics.put(SUCCESS, false);
                        metrics.put(EXCEPTION, "Response not delivered - "+ps.getDeliveryError());
                    }
                    payload.put(TRACE, metrics);
                    po.send(dt.setBody(payload));
                }
            } catch (Exception e) {
                log.error("Unable to send to {}", DistributedTrace.DISTRIBUTED_TRACING, e);
            }
        } else {
            if (!ps.isDelivered()) {
                log.error("Delivery error - {}, from={}, to={}, type={}, exec_time={}",
                        ps.getDeliveryError(),
                        event.getFrom() == null? UNKNOWN : event.getFrom(), event.getTo(),
                        ps.isSuccess()? "response" : "exception("+ps.getStatus()+", "+ps.getException()+")",
                        ps.getExecutionTime());
            }
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ProcessStatus processEvent(EventEnvelope event, String rpc) {
        Map<String, String> eventHeaders = event.getHeaders();
        ProcessStatus ps = new ProcessStatus();
        EventEmitter po = EventEmitter.getInstance();
        Map<String, Object> inputOutput = new HashMap<>();
        Map<String, Object> input = new HashMap<>();
        input.put(HEADERS, eventHeaders);
        input.put(BODY, event.getRawBody());
        inputOutput.put(INPUT, input);
        TypedLambdaFunction f = def.getFunction();
        final long begin = System.nanoTime();
        try {
            /*
             * If the service is an interceptor or the input argument is EventEnvelope,
             * we will pass the original event envelope instead of the message body.
             */
            var serializer = def.getCustomSerializer();
            final Object inputBody;
            var cls = def.getInputClass() != null? def.getInputClass() : def.getPoJoClass();
            if (useEnvelope || (interceptor && cls == null)) {
                inputBody = event;
            } else {
                if (event.getRawBody() instanceof Map && cls != null) {
                    if (cls == AsyncHttpRequest.class) {
                        // handle special case
                        inputBody = new AsyncHttpRequest(event.getRawBody());
                    } else {
                        // convert Map to PoJo
                        inputBody = serializer != null? serializer.toPoJo(event.getRawBody(), cls) : event.getBody(cls);
                    }
                } else if (event.getRawBody() instanceof List && cls != null) {
                    // check if the input is a list of map
                    List<Object> inputList = (List<Object>) event.getRawBody();
                    // validate that the objects are PoJo or null
                    int n = 0;
                    for (Object o: inputList) {
                        if (o == null || o instanceof Map) {
                            n++;
                        }
                    }
                    if (n == inputList.size()) {
                        List<Object> updatedList = new ArrayList<>();
                        var mapper = SimpleMapper.getInstance().getMapper();
                        for (Object o: inputList) {
                            final Object pojo;
                            if (o == null) {
                                pojo = null;
                            } else {
                                pojo = serializer != null? serializer.toPoJo(o, cls) : mapper.readValue(o, cls);
                            }
                            updatedList.add(pojo);
                        }
                        inputBody = updatedList;
                    } else {
                        inputBody = event.getBody();
                    }
                } else {
                    inputBody = event.getBody();
                }
            }
            // Insert READ only metadata into function input headers
            Map<String, String> parameters = new HashMap<>(eventHeaders);
            parameters.put(MY_ROUTE, parentRoute);
            if (event.getTraceId() != null) {
                parameters.put(MY_TRACE_ID, event.getTraceId());
            }
            if (event.getTracePath() != null) {
                parameters.put(MY_TRACE_PATH, event.getTracePath());
            }
            Object result = f.handleEvent(parameters, inputBody, instance);
            float diff = getExecTime(begin);
            Map<String, Object> output = new HashMap<>();
            String replyTo = event.getReplyTo();
            if (replyTo != null) {
                long rpcTimeout = util.str2long(rpc);
                // if it is a callback instead of an RPC call, use default timeout of 30 minutes
                final long expiry = rpcTimeout < 0? DEFAULT_TIMEOUT : rpcTimeout;
                final EventEnvelope response = new EventEnvelope();
                response.setTo(replyTo).setTags(event.getTags()).setFrom(def.getRoute());
                /*
                 * Preserve correlation ID and extra information
                 *
                 * "Extra" is usually used by event interceptors.
                 * For example, to save some metadata from the original sender.
                 */
                if (event.getCorrelationId() != null) {
                    response.setCorrelationId(event.getCorrelationId());
                }
                // propagate the trace to the next service if any
                if (event.getTraceId() != null) {
                    response.setTrace(event.getTraceId(), event.getTracePath());
                }
                boolean skipResponse = false;
                // if response is a Mono, subscribe to it for a future response
                if (result instanceof Mono mono) {
                    skipResponse = true;
                    // set reactive to defer service acknowledgement until Mono is complete
                    ps.setReactive();
                    Platform platform = Platform.getInstance();
                    final AtomicLong timer = new AtomicLong(-1);
                    final AtomicBoolean completed = new AtomicBoolean(false);
                    // For non-blocking operation, use a new virtual thread for the subscription
                    final Disposable disposable = mono.doFinally(done -> {
                        long t1 = timer.get();
                        if (t1 > 0) {
                            platform.getVertx().cancelTimer(t1);
                        }
                        // finally, send service acknowledgement
                        platform.getEventSystem().send(def.getRoute(), READY + route);
                    }).subscribeOn(Schedulers.fromExecutor(platform.getVirtualThreadExecutor()))
                      .subscribe(data -> {
                        completed.set(true);
                        sendMonoResponse(response, data, begin);
                    }, e -> {
                        if (e instanceof Throwable ex) {
                            completed.set(true);
                            final EventEnvelope errorResponse = prepareErrorResponse(event, ex);
                            try {
                                po.send(encodeTraceAnnotations(errorResponse).setExecutionTime(getExecTime(begin)));
                            } catch (IOException e2) {
                                log.error("Unable to deliver exception from {} - {}", route, e2.getMessage());
                            }
                        }
                    }, () -> {
                          // When the Mono emitter sends a null payload, Mono will not return any result.
                          // Therefore, the system must return a null body for this normal use case.
                          if (!completed.get()) {
                              sendMonoResponse(response, null, begin);
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
                var resultSet = result;
                /*
                 * if response is a Flux, subscribe to it for a future response and immediately
                 * return x-stream-id and x-ttl so the caller can use a FluxConsumer to read the stream.
                 *
                 * The response contract is two headers containing x-stream-id and x-ttl.
                 * The response body is an empty map.
                 */
                if (result instanceof Flux flux) {
                    resultSet = Collections.EMPTY_MAP;
                    FluxPublisher<Object> fluxRelay = new FluxPublisher<>(flux, expiry);
                    if (serializer != null) {
                        fluxRelay.setCustomSerializer(serializer);
                    }
                    response.setHeader(X_TTL, expiry);
                    response.setHeader(X_STREAM_ID, fluxRelay.publish());
                }
                boolean simulatedStreamTimeout = !skipResponse && updateResponse(response, resultSet);
                if (!response.getHeaders().isEmpty()) {
                    output.put(HEADERS, response.getHeaders());
                }
                output.put(BODY, response.getRawBody() == null? "null" : response.getRawBody());
                output.put(STATUS, response.getStatus());
                inputOutput.put(OUTPUT, output);
                try {
                    if (!interceptor && !skipResponse && !simulatedStreamTimeout) {
                        po.send(encodeTraceAnnotations(response).setExecutionTime(diff));
                    }
                } catch (Exception e2) {
                    ps.setUnDelivery(e2.getMessage());
                }
            } else {
                EventEnvelope response = new EventEnvelope().setBody(result);
                output.put(BODY, response.getRawBody() == null? "null" : response.getRawBody());
                output.put(STATUS, response.getStatus());
                output.put(ASYNC, true);
                inputOutput.put(OUTPUT, output);
            }
            return ps.setExecutionTime(diff).setInputOutput(inputOutput);

        } catch (Exception e) {
            float diff = getExecTime(begin);
            final String replyTo = event.getReplyTo();
            final int status = getStatusFromException(e);
            String error = simplifyCastError(util.getRootCause(e));
            if (f instanceof MappingExceptionHandler handler) {
                try {
                    handler.onError(parentRoute, new AppException(status, error), event, instance);
                } catch (Exception e3) {
                    ps.setUnDelivery(e3.getMessage());
                }
                Map<String, Object> output = new HashMap<>();
                output.put(STATUS, status);
                output.put(EXCEPTION, error);
                inputOutput.put(OUTPUT, output);
                return ps.setException(status, error).setInputOutput(inputOutput);
            }
            Map<String, Object> output = new HashMap<>();
            if (replyTo != null) {
                final EventEnvelope errorResponse = prepareErrorResponse(event, e);
                try {
                    po.send(encodeTraceAnnotations(errorResponse).setExecutionTime(diff));
                } catch (Exception e4) {
                    ps.setUnDelivery(e4.getMessage());
                }
            } else {
                output.put(ASYNC, true);
                if (status >= 500) {
                    log.error("Unhandled exception for {}", route, e);
                } else {
                    log.warn("Unhandled exception for {} - {}", route, error);
                }
            }
            output.put(STATUS, status);
            output.put(EXCEPTION, error);
            inputOutput.put(OUTPUT, output);
            return ps.setException(status, error).setExecutionTime(diff).setInputOutput(inputOutput);
        }
    }

    private int getStatusFromException(Throwable e) {
        return switch (e) {
            case AppException ex -> ex.getStatus();
            case TimeoutException ignored -> 408;
            case IllegalArgumentException ignored -> 400;
            default -> 500;
        };
    }

    private void sendMonoResponse(EventEnvelope response, Object data, long begin) {
        EventEmitter po = EventEmitter.getInstance();
        updateResponse(response, data);
        try {
            po.send(encodeTraceAnnotations(response).setExecutionTime(getExecTime(begin)));
        } catch (IOException e1) {
            log.error("Unable to deliver async response from {} - {}", route, e1.getMessage());
        }
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
            Map<String, String> headers = resultEvent.getHeaders();
            if (headers.isEmpty() && resultEvent.getStatus() == 408 && resultEvent.getRawBody() == null) {
                // simulate a READ timeout for ObjectStreamService
                return true;
            } else {
                /*
                 * When EventEnvelope is used as a return type, the system will transport
                 * 1. payload
                 * 2. key-values (as headers)
                 */
                Object body = resultEvent.getRawBody();
                if (serializer != null && util.isPoJo(body)) {
                    response.setBody(serializer.toMap(body));
                } else {
                    response.setBody(body);
                }
                response.setType(resultEvent.getType());
                for (Map.Entry<String, String> kv: headers.entrySet()) {
                    String k = kv.getKey();
                    if (!MY_ROUTE.equals(k) && !MY_TRACE_ID.equals(k) && !MY_TRACE_PATH.equals(k)) {
                        response.setHeader(k, kv.getValue());
                    }
                }
                response.setStatus(resultEvent.getStatus());
            }
        } else {
            // when using custom serializer, the result will be converted to a Map
            if (serializer != null && util.isPoJo(result)) {
                response.setBody(serializer.toMap(result));
            } else {
                response.setBody(result);
            }
        }
        return false;
    }

    private float getExecTime(long begin) {
        float delta = (float) (System.nanoTime() - begin) / EventEmitter.ONE_MILLISECOND;
        // adjust precision to 3 decimal points
        return Float.parseFloat(String.format("%.3f", Math.max(0.0f, delta)));
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
        EventEmitter po = EventEmitter.getInstance();
        TraceInfo trace = po.getTrace(parentRoute, instance);
        if (trace != null) {
            response.setAnnotations(trace.annotations);
        }
        return response;
    }
}
