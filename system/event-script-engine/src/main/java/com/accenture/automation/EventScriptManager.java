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

import java.io.IOException;
import java.util.Map;

@EventInterceptor
@PreLoad(route = "event.script.manager", envInstances = "flow.manager.instances", instances = 200)
public class EventScriptManager implements TypedLambdaFunction<EventEnvelope, Void> {
    public static final String SERVICE_NAME = "event.script.manager";
    private static final String FIRST_TASK = "first_task";
    private static final String INPUT = "input";
    private static final String FLOW_ID = "flow_id";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance) throws IOException {
        EventEmitter po = EventEmitter.getInstance();
        try {
            if (!headers.containsKey(FLOW_ID)) {
                throw new AppException(400, "Missing "+FLOW_ID);
            }
            processRequest(event, headers.get(FLOW_ID));
        } catch (AppException | IOException e) {
            if (event.getReplyTo() != null && event.getCorrelationId() != null) {
                EventEnvelope error = new EventEnvelope()
                        .setTo(event.getReplyTo()).setCorrelationId(event.getCorrelationId())
                        .setStatus(e instanceof AppException appEx? appEx.getStatus() : 500).setBody(e.getMessage());
                po.send(error);
            }
        }
        return null;
    }

    private void processRequest(EventEnvelope event, String flowId) throws AppException, IOException {
        Flow template = Flows.getFlow(flowId);
        if (template == null) {
            throw new AppException(500, "Cannot process this flow - configuration "+flowId+" not found");
        }
        String cid = event.getCorrelationId();
        String replyTo = event.getReplyTo();
        FlowInstance flowInstance = new FlowInstance(flowId, cid, replyTo, template);
        // optional distributed trace
        String traceId = event.getTraceId();
        String tracePath = event.getTracePath();
        if (traceId != null && tracePath != null) {
            flowInstance.setTrace(traceId, tracePath);
        }
        Flows.addFlowInstance(flowInstance);
        // set the input event body into the flow dataset
        flowInstance.dataset.put(INPUT, event.getBody());
        // execute the first task
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope firstTask = new EventEnvelope()
                .setTo(TaskExecutor.SERVICE_NAME).setCorrelationId(cid)
                .setHeader(FIRST_TASK, flowInstance.getFlow().firstTask);
        po.send(firstTask);
    }

}
