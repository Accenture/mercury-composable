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

package com.accenture.models;

import com.accenture.automation.TaskExecutor;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.Utility;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowInstance {
    private static final String MODEL = "model";
    private static final String FLOW = "flow";
    private static final String TIMEOUT = "timeout";
    private static final String INSTANCE = "instance";
    private static final String CID = "cid";

    // dataset is the state machine that holds the original input and the latest model
    public final ConcurrentMap<String, Object> dataset = new ConcurrentHashMap<>();
    public final AtomicInteger pipeCounter = new AtomicInteger(0);
    public final ConcurrentMap<Integer, PipeInfo> pipeMap = new ConcurrentHashMap<>();
    private final long start = System.currentTimeMillis();
    public final String id = Utility.getInstance().getUuid();
    public final String cid;
    public final String replyTo;
    public final String timeoutWatcher;
    private final Flow flow;
    private String traceId;
    private String tracePath;
    private boolean responded = false;
    private boolean running = true;

    public FlowInstance(String flowId, String cid, String replyTo, Flow flow) {
        this.flow = flow;
        this.cid = cid;
        this.replyTo = replyTo;
        // initialize the state machine
        ConcurrentMap<String, Object> model = new ConcurrentHashMap<>();
        model.put(INSTANCE, id);
        model.put(CID, cid);
        model.put(FLOW, flowId);
        this.dataset.put(MODEL, model);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope timeoutTask = new EventEnvelope();
        timeoutTask.setTo(TaskExecutor.SERVICE_NAME).setCorrelationId(id).setHeader(TIMEOUT, true);
        timeoutWatcher = po.sendLater(timeoutTask, new Date(System.currentTimeMillis() + flow.ttl));
    }

    public void setTrace(String traceId, String tracePath) {
        this.setTraceId(traceId);
        this.setTracePath(tracePath);
    }

    public long getStartMillis() {
        return start;
    }

    public void close() {
        if (running) {
            running = false;
            EventEmitter.getInstance().cancelFutureEvent(timeoutWatcher);
        }
    }

    public boolean isNotResponded() {
        return !responded;
    }

    public void setResponded(boolean responded) {
        this.responded = responded;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getTracePath() {
        return tracePath;
    }

    public void setTracePath(String tracePath) {
        this.tracePath = tracePath;
    }

    public Flow getFlow() {
        return flow;
    }
}
