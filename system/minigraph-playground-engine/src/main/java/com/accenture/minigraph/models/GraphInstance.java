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

package com.accenture.minigraph.models;

import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.util.MultiLevelMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class GraphInstance {
    private static final String FLOW_INSTANCE = "flow_instance";
    private static final String REPLY_TO = "reply_to";
    private static final String WS_INSTANCE = "ws_instance";
    private static final String RUN_WATCHER = "run_watcher";
    private static final String CID = "cid";
    private static final String NONE = "none";
    private long startTime = System.currentTimeMillis();
    public final String graphId;
    public final MiniGraph graph = new MiniGraph();
    public final MultiLevelMap stateMachine = new MultiLevelMap();
    public final ConcurrentMap<String, Visits> hits = new ConcurrentHashMap<>();
    public final ConcurrentMap<String, Boolean> nodeSeen = new ConcurrentHashMap<>();
    public final ConcurrentMap<String, Boolean> skillRun = new ConcurrentHashMap<>();
    public final AtomicBoolean complete = new AtomicBoolean(false);
    private final ConcurrentMap<String, Object> metadata = new ConcurrentHashMap<>();

    public GraphInstance(String graphId) {
        this.graphId = graphId;
    }

    public String getCorrelationId() {
        return metadata.get(CID) instanceof String v? v : NONE;
    }

    public void setCorrelationId(String correlationId) {
        metadata.put(CID, correlationId);
    }

    public String getFlowInstanceId() {
        return metadata.get(FLOW_INSTANCE) instanceof String v? v : NONE;
    }

    public void setFlowInstanceId(String instanceId) {
        metadata.put(FLOW_INSTANCE, instanceId);
    }

    public String getReplyTo() {
        return metadata.get(REPLY_TO) instanceof String v? v : NONE;
    }

    public void setReplyTo(String replyTo) {
        metadata.put(REPLY_TO, replyTo);
    }

    public void setWsInstance(String instanceId) {
        metadata.put(WS_INSTANCE, instanceId);
    }

    public String getWsInstance() {
        return metadata.get(WS_INSTANCE) instanceof String v? v : NONE;
    }

    public void resetStartTime() {
        this.startTime = System.currentTimeMillis();
    }

    public long getStartTime() {
        return startTime;
    }

    /**
     * The dry-run watcher slot (see GraphTraveler.armRunWatcher) - the token is
     * "owner-run-correlation-id|timer-id", null when no watcher is pending.
     *
     * @param token the owner-tagged timer token, or null to clear unconditionally
     */
    public void setRunWatcher(String token) {
        if (token == null) {
            metadata.remove(RUN_WATCHER);
        } else {
            metadata.put(RUN_WATCHER, token);
        }
    }

    public String getRunWatcher() {
        return metadata.get(RUN_WATCHER) instanceof String v? v : null;
    }

    /**
     * Atomically clear the watcher slot only when it still holds the given token, so
     * two racing cancellers act at most once and never on a token they did not read.
     *
     * @param token the token previously read from getRunWatcher
     * @return true when this caller removed the token
     */
    public boolean clearRunWatcher(String token) {
        return metadata.remove(RUN_WATCHER, token);
    }
}
