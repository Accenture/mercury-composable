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

package com.accenture.adapters;

import com.accenture.automation.EventScriptManager;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;

public class StartFlow {

    private static final String FLOW_ID = "flow_id";
    private static final String BODY = "body";
    private static final StartFlow INSTANCE = new StartFlow();

    private StartFlow() {
        // singleton
    }

    public static StartFlow getInstance() {
        return INSTANCE;
    }

    public void send(PostOffice po, String flowId, Map<String, Object> dataset,
                      String replyTo, String correlationId) throws IOException {
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
            if (replyTo != null) {
                forward.setReplyTo(replyTo);
            }
            po.send(forward);
        }
    }

    public Future<EventEnvelope> request(PostOffice po, String flowId, Map<String, Object> dataset,
                                         String correlationId, long timeout) throws IOException {
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
            return po.request(forward, timeout);
        } else {
            throw new IllegalArgumentException("Missing body in dataset");
        }
    }

}
