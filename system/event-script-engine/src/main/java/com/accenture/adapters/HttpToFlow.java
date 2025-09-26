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

import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;

import java.util.HashMap;
import java.util.Map;

import static com.accenture.utils.ComposableConstants.ENV_INSTANCES_PREFIX;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
@EventInterceptor
@PreLoad(route = HttpToFlow.ROUTE, instances = 200, envInstances = HttpToFlow.ENV_INSTANCE_PROPERTY)
public class HttpToFlow implements TypedLambdaFunction<EventEnvelope, Void> {

    public static final String ROUTE = "http.flow.adapter";
    public static final String ENV_INSTANCE_PROPERTY = ENV_INSTANCES_PREFIX + HttpToFlow.ROUTE;

    private static final String TYPE = "type";
    private static final String ERROR = "error";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance) {
        PostOffice po = new PostOffice(headers, instance);
        try {
            processRequest(po, event);
        } catch (Exception e) {
            if (event.getReplyTo() != null && event.getCorrelationId() != null) {
                int status = e instanceof AppException appEx? appEx.getStatus() : 500;
                Map<String, Object> result = new HashMap<>();
                result.put(STATUS, status);
                result.put(MESSAGE, e.getMessage());
                result.put(TYPE, ERROR);
                EventEnvelope error = new EventEnvelope()
                        .setTo(event.getReplyTo()).setCorrelationId(event.getCorrelationId())
                        .setStatus(status).setBody(result);
                po.send(error);
            }
        }
        return null;
    }

    private void processRequest(PostOffice po, EventEnvelope event) throws AppException {
        AsyncHttpRequest request = new AsyncHttpRequest(event.getBody());
        String flowId = request.getHeader("x-flow-id");
        if (flowId == null) {
            throw new AppException(400, "Missing x-flow-id in HTTP request headers");
        }
        // convert HTTP context to flow "input" dataset
        Map<String, Object> dataset = new HashMap<>();
        dataset.put("header", request.getHeaders());
        dataset.put("body", request.getBody());
        dataset.put("cookie", request.getCookies());
        dataset.put("path_parameter", request.getPathParameters());
        dataset.put("method", request.getMethod());
        dataset.put("uri", request.getUrl());
        dataset.put("query", request.getQueryParameters());
        dataset.put("stream", request.getStreamRoute());
        dataset.put("ip", request.getRemoteIp());
        dataset.put("filename", request.getFileName());
        dataset.put("session", request.getSessionInfo());
        FlowExecutor.getInstance().launch(po, flowId, dataset, event.getReplyTo(), event.getCorrelationId());
    }
}
