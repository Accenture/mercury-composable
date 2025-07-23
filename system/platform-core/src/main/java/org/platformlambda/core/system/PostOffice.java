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

import io.vertx.core.Future;
import org.platformlambda.core.models.CustomSerializer;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.Kv;
import org.platformlambda.core.models.TraceInfo;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PostOffice {
    private static final String MY_ROUTE = "my_route";
    private static final String MY_TRACE_ID = "my_trace_id";
    private static final String MY_TRACE_PATH = "my_trace_path";
    public static final String MISSING_EVENT = "Missing outgoing event";
    private final String myRoute;
    private final String myTraceId;
    private final String myTracePath;
    private final int instance;
    private final CustomSerializer serializer;

    private static final EventEmitter po = EventEmitter.getInstance();

    /**
     * Create a PostOffice instance
     *
     * @param headers in the input arguments to a user function
     * @param instance for the worker serving the current transaction
     */
    public PostOffice(Map<String, String> headers, int instance) {
        myRoute = headers.get(MY_ROUTE);
        myTraceId = headers.get(MY_TRACE_ID);
        myTracePath = headers.get(MY_TRACE_PATH);
        this.instance = instance;
        this.serializer = null;
    }

    /**
     * Create a PostOffice instance
     *
     * @param headers in the input arguments to a user function
     * @param instance for the worker serving the current transaction
     * @param serializer to do custom serialization for the target functions
     */
    public PostOffice(Map<String, String> headers, int instance, CustomSerializer serializer) {
        myRoute = headers.get(MY_ROUTE);
        myTraceId = headers.get(MY_TRACE_ID);
        myTracePath = headers.get(MY_TRACE_PATH);
        this.instance = instance;
        this.serializer = serializer;
    }

    /**
     * Create a PostOffice instance with hard coded route, traceId and tracePath.
     * <p>
     * IMPORTANT: This should only be used for unit test purpose, and you should not manually create a traceId
     *
     * @param myRoute to emulate the sender's route name
     * @param myTraceId to emulate a traceId
     * @param myTracePath to emulate a tracePath
     */
    public PostOffice(String myRoute, String myTraceId, String myTracePath) {
        this.myRoute = myRoute;
        this.myTraceId = myTraceId;
        this.myTracePath = myTracePath;
        this.instance = 0;
        this.serializer = null;
    }

    /**
     * Create a PostOffice instance with hard coded route, traceId and tracePath.
     * <p>
     * IMPORTANT: This should only be used for unit test purpose, and you should not manually create a traceId
     *
     * @param myRoute to emulate the sender's route name
     * @param myTraceId to emulate a traceId
     * @param myTracePath to emulate a tracePath
     * @param serializer to do custom serialization for the target functions
     */
    public PostOffice(String myRoute, String myTraceId, String myTracePath, CustomSerializer serializer) {
        this.myRoute = myRoute;
        this.myTraceId = myTraceId;
        this.myTracePath = myTracePath;
        this.instance = 0;
        this.serializer = serializer;
    }

    /**
     * Get my route name for the currently running service.
     * This is typically used in Role Based Access Control (RBAC) to restrict certain user roles to execute the service.
     * RBAC is a user application's responsibility.
     *
     * @return route name
     */
    public String getRoute() {
        return myRoute;
    }

    /**
     * User application may obtain the trace ID of the current transaction
     *
     * @return trace ID of the current transaction
     */
    public String getTraceId() {
        return myTraceId;
    }

    /**
     * User application may obtain the trace path of the current transaction
     *
     * @return trace path of the current transaction
     */
    public String getTracePath() {
        return myTracePath;
    }

    /**
     * User application may obtain the complete trace object for trace ID, path, start time and annotations.
     *
     * @return trace info
     */
    public TraceInfo getTrace() {
        return po.getTrace(myRoute, instance);
    }

    /**
     * Convert the event body into a PoJo using a custom serializer if any
     * <p>
     * Custom serializer at the PostOffice level is used when the user function is not
     * configured with a custom one.
     *
     * @param event that has a body
     * @param toValueType class
     * @return pojo
     * @param <T> pojo class
     */
    public <T> T getEventBodyAsPoJo(EventEnvelope event, Class<T> toValueType) {
        if (event.getRawBody() instanceof Map) {
            if (serializer == null) {
                return event.getBody(toValueType);
            } else {
                return serializer.toPoJo(event.getRawBody(), toValueType);
            }
        } else {
            throw new IllegalArgumentException("Event body is not a PoJo");
        }
    }

    /**
     * Set a pojo as an event body using a custom serializer if any
     * <p>
     * Custom serializer at the PostOffice level is used when the user function is not
     * configured with a custom one.
     *
     * @param event to be set with a pojo body
     * @param pojo input
     */
    public void setEventBodyAsPoJo(EventEnvelope event, Object pojo) {
        if (serializer == null) {
            event.setBody(pojo);
        } else {
            event.setBody(serializer.toMap(pojo));
            event.setType(pojo.getClass().getName());
        }
    }

    /**
     * Check if a function with the named route exists
     *
     * @param route name
     * @return true or false
     */
    public boolean exists(String... route) {
        return po.exists(route);
    }

    /**
     * User application may add key-values to a trace using this method
     * <p>
     * Please note that trace annotation feature is available inside a user function
     * that implements LambdaFunction, TypedLambdaFunction or KotlinLambdaFunction
     *
     * @param key of the annotation
     * @param value of the annotation
     * @return this PostOffice instance
     */
    public PostOffice annotateTrace(String key, String value) {
        annotateTraceValue(key, value);
        return this;
    }

    /**
     * User application may add a map of key-values to a trace using this method
     * <p>
     * Please note that trace annotation feature is available inside a user function
     * that implements LambdaFunction, TypedLambdaFunction or KotlinLambdaFunction
     *
     * @param key of the annotation
     * @param value of the annotation
     * @return this PostOffice instance
     */
    public PostOffice annotateTrace(String key, Map<String, Object> value) {
        annotateTraceValue(key, value);
        return this;
    }

    /**
     * User application may add a list of values to a trace using this method
     * <p>
     * Please note that trace annotation feature is available inside a user function
     * that implements LambdaFunction, TypedLambdaFunction or KotlinLambdaFunction
     *
     * @param key of the annotation
     * @param value of the annotation
     * @return this PostOffice instance
     */
    public PostOffice annotateTrace(String key, List<Object> value) {
        annotateTraceValue(key, value);
        return this;
    }

    private void annotateTraceValue(String key, Object value) {
        TraceInfo trace = getTrace();
        if (trace != null) {
            trace.annotate(key, value);
        }
    }

    /**
     * Broadcast an event to multiple servers holding the target route
     * <p>
     * This method is only relevant when running in a minimalist service mesh
     *
     * @param to target route
     * @param parameters for the event
     * @throws IllegalArgumentException in case of invalid route
     */
    public void broadcast(String to, Kv... parameters) {
        send(touch(po.asEnvelope(to, null, parameters)).setBroadcastLevel(1));
    }

    /**
     * Broadcast an event to multiple servers holding the target route
     * <p>
     * This method is only relevant when running in a minimalist service mesh
     *
     * @param to target route
     * @param body message payload
     * @throws IllegalArgumentException in case of invalid route
     */
    public void broadcast(String to, Object body) {
        if (body instanceof Kv kv) {
            // in case if a single KV is sent
            Kv[] keyValue = new Kv[1];
            keyValue[0] = kv;
            send(touch(po.asEnvelope(to, null, keyValue)).setBroadcastLevel(1));
        } else {
            send(touch(po.asEnvelope(to, body)).setBroadcastLevel(1));
        }
    }

    /**
     * Broadcast an event to multiple servers holding the target route
     * <p>
     * This method is only relevant when running in a minimalist service mesh
     *
     * @param to target route
     * @param body message payload
     * @param parameters for the event
     * @throws IllegalArgumentException in case of invalid route
     */
    public void broadcast(String to, Object body, Kv... parameters) {
        send(touch(po.asEnvelope(to, body, parameters)).setBroadcastLevel(1));
    }

    /**
     * Send an event to a target service in multiple servers
     *
     * @param to target route
     * @param parameters for the event
     * @throws IllegalArgumentException in case of invalid route
     */
    public void send(String to, Kv... parameters) {
        send(touch(po.asEnvelope(to, null, parameters)));
    }

    /**
     * Send an event to a target service
     *
     * @param to target route
     * @param body message payload
     * @throws IllegalArgumentException in case of invalid route
     */
    public void send(String to, Object body) {
        if (body instanceof Kv kv) {
            // in case if a single KV is sent
            Kv[] keyValue = new Kv[1];
            keyValue[0] = kv;
            send(touch(po.asEnvelope(to, null, keyValue)));
        } else {
            send(touch(po.asEnvelope(to, body)));
        }
    }

    /**
     * Send an event to a target service
     *
     * @param to target route
     * @param body message payload
     * @param parameters for the event
     * @throws IllegalArgumentException in case of invalid route
     */
    public void send(String to, Object body, Kv... parameters) {
        send(touch(po.asEnvelope(to, body, parameters)));
    }

    /**
     * Send an event to a target service
     *
     * @param event to the target
     * @throws IllegalArgumentException if invalid route or missing parameters
     */
    public void send(final EventEnvelope event) {
        po.send(touch(event));
    }

    /**
     * Schedule a future event
     *
     * @param event envelope
     * @param future time
     * @return eventId of the scheduled delivery
     */
    public String sendLater(final EventEnvelope event, Date future) {
        return po.sendLater(touch(event), future);
    }

    /**
     * Broadcast to multiple target services
     *
     * @param event to the target
     * @throws IllegalArgumentException if invalid route or missing parameters
     */
    public void broadcast(final EventEnvelope event) {
        po.broadcast(touch(event));
    }

    /**
     * This method allows your app to send an async or RPC request to another application instance
     * <p>
     * You can retrieve result from the future's
     * onSuccess(EventEnvelope event)
     * <p>
     *     Note that onFailure is not required because exceptions are returned as regular event.
     *
     * @param event to be sent to a peer application instance
     * @param timeout to abort the request
     * @param headers optional security headers such as "Authorization"
     * @param eventEndpoint fully qualified URL such as http: //domain:port/api/event
     * @param rpc if true, the target service will return a response.
     *            Otherwise, a response with status=202 will be returned to indicate that the event will be delivered.
     * @return response event
     * @throws IllegalArgumentException in case of routing error
     */
    public Future<EventEnvelope> asyncRequest(final EventEnvelope event, long timeout,
                                              Map<String, String> headers,
                                              String eventEndpoint, boolean rpc) {
        return po.asyncRequest(touch(event), timeout, headers, eventEndpoint, rpc);
    }

    /**
     * Send an RPC request with a future result
     * <p>
     * You can retrieve result using future.get()
     *
     * @param event to be sent to a peer application instance
     * @param timeout to abort the request
     * @param headers optional security headers such as "Authorization"
     * @param eventEndpoint fully qualified URL such as http: //domain:port/api/event
     * @param rpc if true, the target service will return a response.
     *            Otherwise, a response with status=202 will be returned to indicate that the event will be delivered.
     * @return response event
     * @throws IllegalArgumentException in case of routing error
     */
    public CompletableFuture<EventEnvelope> request(final EventEnvelope event, long timeout,
                                                    Map<String, String> headers,
                                                    String eventEndpoint, boolean rpc) {
        return po.request(touch(event), timeout, headers, eventEndpoint, rpc);
    }

    /**
     * Send a request asynchronously with a future result
     * <p>
     * You can retrieve result from the future's
     * onSuccess(EventEnvelope event)
     * onFailure(Throwable timeoutException)
     * <p>
     * IMPORTANT: This is an asynchronous RPC using Future.
     *
     * @param event to the target
     * @param timeout in milliseconds
     * @return future results
     * @throws IllegalArgumentException in case of routing error
     */
    public Future<EventEnvelope> asyncRequest(final EventEnvelope event, long timeout) {
        return asyncRequest(touch(event), timeout, true);
    }

    /**
     * Send a request asynchronously with a future result
     * <p>
     * You can retrieve result from the future's
     * onSuccess(EventEnvelope event)
     * onFailure(Throwable timeoutException)
     * <p>
     * IMPORTANT: This is an asynchronous RPC using Future.
     *
     * @param event to the target
     * @param timeout in milliseconds
     * @param timeoutException if true, return TimeoutException in onFailure method. Otherwise, return timeout event.
     * @return future result
     * @throws IllegalArgumentException in case of routing error
     */
    public Future<EventEnvelope> asyncRequest(final EventEnvelope event, long timeout, boolean timeoutException) {
        return po.asyncRequest(touch(event), timeout, timeoutException);
    }

    /**
     * Future request API for RPC
     * <p>
     * You can retrieve result using future.get()
     *
     * @param event to the target
     * @param timeout in milliseconds
     * @return future results
     * @throws IllegalArgumentException in case of routing error
     */
    public CompletableFuture<EventEnvelope> request(final EventEnvelope event, long timeout) {
        return request(touch(event), timeout, true);
    }

    /**
     * Future request API for RPC
     * <p>
     * You can retrieve result using future.get()
     *
     * @param event to the target
     * @param timeout in milliseconds
     * @param timeoutException if true, throws TimeoutException wrapped in an ExecutionException with future.get().
     *                         Otherwise, return timeout as a regular event.
     * @return future result
     * @throws IllegalArgumentException in case of routing error
     */
    public CompletableFuture<EventEnvelope> request(final EventEnvelope event, long timeout,
                                                              boolean timeoutException) {
        return po.request(touch(event), timeout, timeoutException);
    }

    /**
     * Send parallel requests asynchronously with a future result
     * <p>
     * You can retrieve result from the future's
     * onSuccess(EventEnvelope event)
     * onFailure(Throwable timeoutException)
     * <p>
     * IMPORTANT: This is an asynchronous RPC using Future
     *
     * @param events list of envelopes
     * @param timeout in milliseconds
     * @return future list of results
     * @throws IllegalArgumentException in case of error
     */
    public Future<List<EventEnvelope>> asyncRequest(final List<EventEnvelope> events, long timeout) {
        return asyncRequest(events, timeout, true);
    }

    /**
     * Send parallel requests asynchronously with a future result
     * <p>
     * You can retrieve result from the future's
     * onSuccess(EventEnvelope event)
     * onFailure(Throwable timeoutException)
     * <p>
     * IMPORTANT: This is an asynchronous RPC using Future
     *
     * @param events list of envelopes
     * @param timeout in milliseconds
     * @param timeoutException if true, throws TimeoutException wrapped in an ExecutionException with future.get().
     *                         Otherwise, return timeout as a regular event.
     * @return future list of results
     * @throws IllegalArgumentException in case of error
     */
    public Future<List<EventEnvelope>> asyncRequest(final List<EventEnvelope> events, long timeout,
                                                    boolean timeoutException) {
        events.forEach(this::touch);
        return po.asyncRequest(events, timeout, timeoutException);
    }

    /**
     * Future request API for sending parallel requests with a future result
     * <p>
     * You can retrieve result using future.get()
     *
     * @param events list of envelopes
     * @param timeout in milliseconds
     * @return future list of results
     * @throws IllegalArgumentException in case of error
     */
    public CompletableFuture<List<EventEnvelope>> request(final List<EventEnvelope> events, long timeout) {
        return request(events, timeout, true);
    }

    /**
     * Future request API for sending parallel requests with a future result
     * <p>
     * You can retrieve result using future.get()
     *
     * @param events list of envelopes
     * @param timeout in milliseconds
     * @param timeoutException if true, throws TimeoutException wrapped in an ExecutionException with future.get().
     *                         Otherwise, return timeout as a regular event.
     * @return future list of results
     * @throws IllegalArgumentException in case of error
     */
    public CompletableFuture<List<EventEnvelope>> request(final List<EventEnvelope> events, long timeout,
                                                          boolean timeoutException) {
        events.forEach(this::touch);
        return po.request(events, timeout, timeoutException);
    }

    private EventEnvelope touch(final EventEnvelope event) {
        if (event == null) {
            throw new IllegalArgumentException(MISSING_EVENT);
        }
        if (event.getFrom() == null) {
            event.setFrom(myRoute);
        }
        if (event.getTraceId() == null) {
            event.setTraceId(myTraceId);
        }
        if (event.getTracePath() == null) {
            event.setTracePath(myTracePath);
        }
        return event;
    }
}
