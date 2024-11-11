/*

    Copyright 2018-2024 Accenture Technology

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

 package com.accenture.automation;

import com.accenture.models.Flow;
import com.accenture.models.FlowInstance;
import com.accenture.models.Flows;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@EventInterceptor
@PreLoad(route = "event.script.manager", envInstances = "flow.manager.instances", instances = 200)
public class EventScriptManager implements TypedLambdaFunction<EventEnvelope, Void> {
    private static final Logger log = LoggerFactory.getLogger(EventScriptManager.class);
    public static final String SERVICE_NAME = "event.script.manager";
    private static final String FIRST_TASK = "first_task";
    private static final String INPUT = "input";
    private static final String FLOW_ID = "flow_id";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance) throws IOException {
        EventEmitter po = EventEmitter.getInstance();
        try {
            if (!headers.containsKey(FLOW_ID)) {
                throw new IllegalArgumentException("Missing "+FLOW_ID);
            }
            processRequest(event, headers.get(FLOW_ID));
        } catch (IOException e) {
            if (event.getReplyTo() != null && event.getCorrelationId() != null) {
                EventEnvelope error = new EventEnvelope()
                        .setTo(event.getReplyTo()).setCorrelationId(event.getCorrelationId())
                        .setStatus(500).setBody(e.getMessage());
                po.send(error);
            } else {
                log.error("Unhandled exception. error={}", e.getMessage());
            }
        }
        return null;
    }

    private void processRequest(EventEnvelope event, String flowId) throws IOException {
        Flow template = Flows.getFlow(flowId);
        FlowInstance flowInstance = getFlowInstance(event, flowId, template);
        Flows.addFlowInstance(flowInstance);
        // Set the input event body into the flow dataset
        flowInstance.dataset.put(INPUT, event.getBody());
        // Execute the first task and use the unique flow instance as correlation ID during flow execution
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope firstTask = new EventEnvelope()
                .setTo(TaskExecutor.SERVICE_NAME).setCorrelationId(flowInstance.id)
                .setHeader(FIRST_TASK, flowInstance.getFlow().firstTask);
        po.send(firstTask);
    }

    private static FlowInstance getFlowInstance(EventEnvelope event, String flowId, Flow template) {
        if (template == null) {
            throw new IllegalArgumentException("Flow "+ flowId +" not found");
        }
        String cid = event.getCorrelationId();
        if (cid == null) {
            throw new IllegalArgumentException("Missing correlation ID for "+ flowId);
        }
        String replyTo = event.getReplyTo();
        // Save the original correlation-ID ("cid") from the calling party in a flow instance and
        // return this value to the calling party at the end of flow execution
        FlowInstance flowInstance = new FlowInstance(flowId, cid, replyTo, template);
        // Optional distributed trace
        String traceId = event.getTraceId();
        String tracePath = event.getTracePath();
        if (traceId != null && tracePath != null) {
            flowInstance.setTrace(traceId, tracePath);
        }
        return flowInstance;
    }

}
