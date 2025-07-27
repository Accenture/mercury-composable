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

package com.accenture.adapters;

import com.accenture.automation.EventScriptManager;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;

import java.util.Map;
import java.util.concurrent.Future;

public class FlowExecutor {

    private static final String FLOW_ID = "flow_id";
    private static final String BODY = "body";
    private static final FlowExecutor INSTANCE = new FlowExecutor();

    private FlowExecutor() {
        // singleton
    }

    public static FlowExecutor getInstance() {
        return INSTANCE;
    }

    /**
     * A convenient method to start an asynchronous flow execution with no need for post office.
     * This is a convenience method for {@link #launch(PostOffice, String, Map, String)}.
     *
     * @param originator is the route name of the sender function
     * @param flowId of the event flow configuration script
     * @param dataset is a Map containing at least headers and body
     * @param correlationId must be a unique ID (e.g. UUID)
     * @throws IllegalArgumentException in case of routing error
     */
    public void launch(String originator, String flowId, Map<String, Object> dataset,
                       String correlationId) {
        if (originator == null) {
            throw new IllegalArgumentException("Missing originator's route name");
        }
        launch(new PostOffice(originator, null, null), flowId, dataset, correlationId);
    }

    /**
     * A convenient method to start an asynchronous flow execution with no need for post office.
     * Tracing information may be provided through method parameters.
     * This is a convenience method for {@link #launch(PostOffice, String, Map, String)}.
     *
     * @param originator is the route name of the sender function
     * @param traceId must be a unique ID
     * @param tracePath to indicate a path for the request (e.g. PUT /api/some/service)
     * @param flowId of the event flow configuration script
     * @param dataset is a Map containing at least headers and body
     * @param correlationId must be a unique ID (e.g. UUID)
     * @throws IllegalArgumentException in case of routing error
     */
    public void launch(String originator, String traceId, String tracePath, String flowId,
                       Map<String, Object> dataset, String correlationId) {
        if (originator == null) {
            throw new IllegalArgumentException("Missing originator's route name");
        }
        launch(new PostOffice(originator, traceId, tracePath), flowId, dataset, correlationId);
    }

    /**
     * A convenient method to start an asynchronous flow execution with no need for post office.
     * Callback function route name may be provided through method parameters.
     * This is a convenience method for {@link #launch(PostOffice, String, Map, String, String)}.
     *
     * @param originator is the route name of the sender function
     * @param flowId of the event flow configuration script
     * @param dataset is a Map containing at least headers and body
     * @param callback is the route of a composable function
     * @param correlationId must be a unique ID (e.g. UUID)
     * @throws IllegalArgumentException in case of routing error
     */
    public void launch(String originator, String flowId, Map<String, Object> dataset,
                       String callback, String correlationId) {
        if (originator == null) {
            throw new IllegalArgumentException("Missing originator's route name");
        }
        launch(new PostOffice(originator, null, null), flowId, dataset, callback, correlationId);
    }

    /**
     * A convenient method to start an asynchronous flow execution with no need for post office.
     * Tracing information and callback function route name may be provided through method parameters.
     * This is a convenience method for {@link #launch(PostOffice, String, Map, String, String)}.
     *
     * @param originator is the route name of the sender function
     * @param traceId must be a unique ID
     * @param tracePath to indicate a path for the request (e.g. PUT /api/some/service)
     * @param flowId of the event flow configuration script
     * @param callback is the route of a composable function
     * @param dataset is a Map containing at least headers and body
     * @param correlationId must be a unique ID (e.g. UUID)
     * @throws IllegalArgumentException in case of routing error
     */
    public void launch(String originator, String traceId, String tracePath, String flowId,
                       String callback, Map<String, Object> dataset, String correlationId) {
        if (originator == null) {
            throw new IllegalArgumentException("Missing originator's route name");
        }
        launch(new PostOffice(originator, traceId, tracePath), flowId, dataset, callback, correlationId);
    }

    /**
     * A convenient method to start an asynchronous flow execution by providing a custom constructed post office.
     * This is a convenience method for {@link #launch(PostOffice, String, Map, String, String)}.
     * <p>
     * To enable tracing, your PostOffice instance must have traceId and tracePath.
     * To disable tracing, you can set traceId and tracePath as null for the PostOffice instance.
     * <p>
     * @param po PostOffice
     * @param flowId of the event flow configuration script
     * @param dataset is a Map containing at least headers and body
     * @param correlationId must be a unique ID (e.g. UUID)
     * @throws IllegalArgumentException in case of routing error
     */
    public void launch(PostOffice po, String flowId, Map<String, Object> dataset,
                       String correlationId) {
        launch(po, flowId, dataset, null, correlationId);
    }

    /**
     * A convenient method to start an asynchronous flow execution by providing a custom constructed post office
     * and a callback function.
     * <p>
     * To enable tracing, your PostOffice instance must have traceId and tracePath.
     * To disable tracing, you can set traceId and tracePath as null for the PostOffice instance.
     * To invoke a callback function, you can set the route name of the function.
     * <p>
     * @param po PostOffice
     * @param flowId of the event flow configuration script
     * @param dataset is a Map containing at least headers and body
     * @param callback is the route of a composable function
     * @param correlationId must be a unique ID (e.g. UUID)
     * @throws IllegalArgumentException in case of routing error
     */
    public void launch(PostOffice po, String flowId, Map<String, Object> dataset,
                       String callback, String correlationId) {
        if (flowId == null) {
            throw new IllegalArgumentException("Missing flowId");
        }
        if (correlationId == null) {
            throw new IllegalArgumentException("Missing correlation ID");
        }
        if (dataset.containsKey(BODY)) {
            EventEnvelope forward = new EventEnvelope();
            forward.setTo(EventScriptManager.SERVICE_NAME).setHeader(FLOW_ID, flowId);
            forward.setCorrelationId(correlationId).setBody(dataset);
            if (callback != null) {
                forward.setReplyTo(callback);
            }
            po.send(forward);
        } else {
            throw new IllegalArgumentException("Missing body in dataset");
        }
    }

    /**
     * A convenient method to start an asynchronous flow execution no need for post office.
     * This method returns a future that represents the result of an asynchronous flow execution.
     * This is a convenience method for {@link #request(PostOffice, String, Map, String, long)}.
     *
     * @param originator is the route name of the sender function
     * @param flowId of the event flow configuration script
     * @param dataset is a Map containing at least headers and body
     * @param correlationId must be a unique ID (e.g. UUID)
     * @param timeout in milliseconds for the response to come back
     * @return future response
     * @throws IllegalArgumentException in case of routing error
     */
    public Future<EventEnvelope> request(String originator, String flowId, Map<String, Object> dataset,
                                         String correlationId, long timeout) {
        if (originator == null) {
            throw new IllegalArgumentException("Missing originator's route name");
        }
        return request(new PostOffice(originator, null, null),
                flowId, dataset, correlationId, timeout);
    }

    /**
     * A convenient method to start an asynchronous flow execution no need for post office.
     * This method returns a future that represents the result of an asynchronous flow execution.
     * Tracing information may be provided through method parameters.
     * This is a convenience method for {@link #request(PostOffice, String, Map, String, long)}.
     *
     * @param originator is the route name of the sender function
     * @param traceId must be a unique ID
     * @param tracePath to indicate a path for the request (e.g. PUT /api/some/service)
     * @param flowId of the event flow configuration script
     * @param dataset is a Map containing at least headers and body
     * @param correlationId must be a unique ID (e.g. UUID)
     * @param timeout in milliseconds for the response to come back
     * @return future response
     * @throws IllegalArgumentException in case of routing error
     */
    public Future<EventEnvelope> request(String originator, String traceId, String tracePath,
                                         String flowId, Map<String, Object> dataset,
                                         String correlationId, long timeout) {
        if (originator == null) {
            throw new IllegalArgumentException("Missing originator's route name");
        }
        return request(new PostOffice(originator, traceId, tracePath),
                flowId, dataset, correlationId, timeout);
    }

    /**
     * A convenient method to start an asynchronous flow execution by providing a custom constructed post office.
     * This method returns a future that represents the result of an asynchronous flow execution.
     *
     * @param po PostOffice
     * @param flowId of the event flow configuration script
     * @param dataset is a Map containing at least headers and body
     * @param correlationId must be a unique ID (e.g. UUID)
     * @param timeout in milliseconds for the response to come back
     * @return future response
     * @throws IllegalArgumentException in case of routing error
     */
    public Future<EventEnvelope> request(PostOffice po, String flowId, Map<String, Object> dataset,
                                         String correlationId, long timeout) {
        if (flowId == null) {
            throw new IllegalArgumentException("Missing flowId");
        }
        if (correlationId == null) {
            throw new IllegalArgumentException("Missing correlation ID");
        }
        if (dataset != null && dataset.containsKey(BODY)) {
            EventEnvelope forward = new EventEnvelope();
            forward.setTo(EventScriptManager.SERVICE_NAME).setHeader(FLOW_ID, flowId);
            forward.setCorrelationId(correlationId).setBody(dataset);
            return po.request(forward, timeout);
        } else {
            throw new IllegalArgumentException("Missing body in dataset");
        }
    }
}
