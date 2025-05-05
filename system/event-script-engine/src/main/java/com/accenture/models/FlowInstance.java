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

package com.accenture.models;

import com.accenture.automation.TaskExecutor;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowInstance {
    private static final Logger log = LoggerFactory.getLogger(FlowInstance.class);
    private static final String MODEL = "model";
    private static final String FLOW = "flow";
    private static final String TIMEOUT = "timeout";
    private static final String INSTANCE = "instance";
    private static final String CID_TAG = "cid";
    private static final String TRACE = "trace";
    private static final String PARENT = "parent";

    // dataset is the state machine that holds the original input and the latest model
    public final ConcurrentMap<String, Object> dataset = new ConcurrentHashMap<>();
    public final AtomicInteger pipeCounter = new AtomicInteger(0);
    public final ConcurrentMap<Integer, PipeInfo> pipeMap = new ConcurrentHashMap<>();
    public final Queue<String> tasks = new ConcurrentLinkedQueue<>();
    public final ConcurrentMap<String, Boolean> pendingTasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> shared = new ConcurrentHashMap<>();
    private final long start = System.currentTimeMillis();
    public final String id = Utility.getInstance().getUuid();
    public final String cid;
    public final String replyTo;
    private final String timeoutWatcher;
    private final Flow template;
    private String traceId;
    private String tracePath;
    private String parentId;
    private boolean responded = false;
    private boolean running = true;

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param flowId of the event flow configuration
     * @param cid correlation ID
     * @param replyTo of the caller to a flow adapter
     * @param template event flow configuration
     * @param parentId is the parent flow instance ID
     */
    public FlowInstance(String flowId, String cid, String replyTo, Flow template, String parentId) {
        this.template = template;
        this.cid = cid;
        this.replyTo = replyTo;
        // initialize the state machine
        ConcurrentMap<String, Object> model = new ConcurrentHashMap<>();
        model.put(INSTANCE, id);
        model.put(CID_TAG, cid);
        model.put(FLOW, flowId);
        // this is a sub-flow if parent flow instance is available
        if (parentId == null) {
            this.parentId = null;
            model.put(PARENT, shared);
        } else {
            var parent = resolveParent(parentId);
            if (parent != null) {
                model.put(PARENT, parent.shared);
                this.parentId = parent.id;
                log.info("{}:{} extends {}:{}", this.getFlow().id, this.id, parent.getFlow().id, parent.id);
            }
        }
        this.dataset.put(MODEL, model);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope timeoutTask = new EventEnvelope();
        timeoutTask.setTo(TaskExecutor.SERVICE_NAME).setCorrelationId(id).setHeader(TIMEOUT, true);
        this.timeoutWatcher = po.sendLater(timeoutTask, new Date(System.currentTimeMillis() + template.ttl));
    }

    private FlowInstance resolveParent(String parentId) {
        var parent = Flows.getFlowInstance(parentId);
        if (parent == null) {
            return null;
        }
        var pid = parent.parentId;
        if (pid == null) {
            return parent;
        } else {
            return resolveParent(pid);
        }
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param traceId for tracing
     * @param tracePath for tracing
     */
    @SuppressWarnings("unchecked")
    public void setTrace(String traceId, String tracePath) {
        this.setTraceId(traceId);
        this.setTracePath(tracePath);
        if (traceId != null) {
            ConcurrentMap<String, Object> model = (ConcurrentMap<String, Object>) dataset.get(MODEL);
            model.put(TRACE, traceId);
        }
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return start time of a flow instance
     */
    public long getStartMillis() {
        return start;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     */
    public void close() {
        if (running) {
            running = false;
            EventEmitter.getInstance().cancelFutureEvent(timeoutWatcher);
        }
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return true if event flow is outstanding
     */
    public boolean isNotResponded() {
        return !responded;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param responded if a response has been sent to the caller
     */
    public void setResponded(boolean responded) {
        this.responded = responded;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return trace ID
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param traceId for tracing
     */
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @return trace path
     */
    public String getTracePath() {
        return tracePath;
    }

    /**
     * This is reserved for system use.
     * DO NOT use this directly in your application code.
     *
     * @param tracePath for tracing
     */
    public void setTracePath(String tracePath) {
        this.tracePath = tracePath;
    }

    /**
     * Retrieve the event flow configuration
     *
     * @return event flow configuration
     */
    public Flow getFlow() {
        return template;
    }
}
