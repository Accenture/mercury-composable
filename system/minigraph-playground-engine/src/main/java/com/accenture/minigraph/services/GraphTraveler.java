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

package com.accenture.minigraph.services;

import com.accenture.minigraph.common.GraphLambdaFunction;
import com.accenture.minigraph.models.GraphInstance;
import com.accenture.minigraph.models.Visits;
import com.accenture.minigraph.skills.GraphJoin;
import com.accenture.minigraph.skills.GraphJs;
import com.accenture.minigraph.skills.GraphMath;
import com.accenture.minigraph.skills.GraphSuspend;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.PostOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OptionalService("app.env=dev")
@ZeroTracing
@EventInterceptor
@PreLoad(route = GraphTraveler.ROUTE, instances=300)
public class GraphTraveler extends GraphLambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(GraphTraveler.class);
    public static final String ROUTE = "graph.traveler";
    private static final String RUN_TIMEOUT = "run_timeout";

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope event, int instance) {
        var po = PostOffice.trackable(headers, instance);
        var cid = event.getCorrelationId();
        if (cid != null) {
            if (cid.contains("@")) {
                handleSkillResponse(po, event);
            } else if (RUN_TIMEOUT.equals(headers.get(TYPE))) {
                handleRunTimeout(po, headers, event);
            } else if (event.getReplyTo() != null) {
                executeGraph(po, headers, event);
            }
        }
        return null;
    }

    private void executeGraph(PostOffice po, Map<String, String> headers, EventEnvelope event) {
        try {
            var in = headers.get(IN);
            var graphInstance = getGraphInstance(in);
            // disarm a previous run's watcher BEFORE the reset so it cannot observe the
            // half-reset state and abort the new run
            cancelRunWatcher(graphInstance);
            graphInstance.setWsInstance(in);
            graphInstance.setCorrelationId(event.getCorrelationId());
            graphInstance.setReplyTo(event.getReplyTo());
            graphInstance.nodeSeen.clear();
            graphInstance.skillRun.clear();
            graphInstance.hits.clear();
            graphInstance.complete.set(false);
            graphInstance.resetStartTime();
            // clean output for idempotent behavior because the traveler may be invoked multiple times by the operator
            graphInstance.stateMachine.setElement(OUTPUT, new HashMap<>());
            beginTraversal(po, graphInstance);
        } catch (Exception e) {
            var rc = e instanceof AppException ex? ex.getStatus() : 400;
            var error = new EventEnvelope().setTo(event.getReplyTo()).setStatus(rc).setBody(e.getMessage())
                                                .setCorrelationId(event.getCorrelationId());
            po.send(error);
            // Uniform end-of-transmission even when the traversal fails before it
            // starts (no graph instance yet, missing root/end) - emit the terminal
            // line directly to the reply route. When an instance DOES exist (the
            // failure happened after the run watcher was armed), claim the terminal
            // so the watcher cannot fire a second one later.
            var graphInstance = graphInstances.get(headers.get(IN));
            if (graphInstance != null) {
                claimTerminal(graphInstance);
            }
            po.send(new EventEnvelope().setTo(event.getReplyTo()).setStatus(400)
                    .setBody("Graph traversal aborted").setCorrelationId(event.getCorrelationId()));
        }
    }

    private void beginTraversal(PostOffice po, GraphInstance graphInstance) {
        var graph = graphInstance.graph;
        var root = graph.getRootNode();
        if (root == null) {
            throw new IllegalArgumentException("Root node does not exist");
        }
        var end = graph.getEndNode();
        if (end == null) {
            throw new IllegalArgumentException("End node does not exist");
        }
        armRunWatcher(po, graphInstance);
        walk(po, graphInstance, root, null);
    }

    /**
     * Dry-run mirror of the deployed lane's flow timer (a FlowInstance schedules one at
     * construction): a one-shot watcher that turns a hung or overlong traversal into the
     * canonical failure terminal at the model.ttl deadline, so the console - and the
     * synchronous companion drain - always receives an end-of-transmission line. Child
     * calls are already deadline-bounded by getEffectiveTtl in both lanes; this covers
     * what those cannot: total run duration and a skill that never replies. model.ttl
     * is immutable during a run, so the deadline armed here is the deadline reported.
     * The slot token carries the owning run's correlation id, so a stale watcher can
     * never act on a newer run.
     */
    private void armRunWatcher(PostOffice po, GraphInstance graphInstance) {
        // the traveler is re-runnable in the same session - a previous run's watcher
        // may still be pending and must not abort the new run
        cancelRunWatcher(graphInstance);
        var ttl = getModelTtl(graphInstance);
        var cid = graphInstance.getCorrelationId();
        var timeoutEvent = new EventEnvelope().setTo(ROUTE)
                .setHeader(TYPE, RUN_TIMEOUT).setHeader(IN, graphInstance.getWsInstance())
                .setCorrelationId(cid);
        graphInstance.setRunWatcher(cid + "|"
                + po.sendLater(timeoutEvent, new Date(System.currentTimeMillis() + ttl)));
    }

    private void cancelRunWatcher(GraphInstance graphInstance) {
        var token = graphInstance.getRunWatcher();
        // atomic removal: two racing cancellers act at most once, on the exact token read
        if (token != null && graphInstance.clearRunWatcher(token)) {
            EventEmitter.getInstance().cancelFutureEvent(token.substring(token.indexOf('|') + 1));
        }
    }

    /**
     * Exactly-one-terminal arbitration: every terminal path (success, error, timeout)
     * claims the run by flipping 'complete' - the winner emits its terminal and a loser
     * stays silent, so a run racing its own deadline can never emit both terminals
     * (which would misclassify a successful run as failed in the companion capture).
     *
     * @param graphInstance the graph instance
     * @return true when this caller owns the terminal
     */
    private boolean claimTerminal(GraphInstance graphInstance) {
        if (graphInstance.complete.compareAndSet(false, true)) {
            cancelRunWatcher(graphInstance);
            return true;
        }
        return false;
    }

    private void handleRunTimeout(PostOffice po, Map<String, String> headers, EventEnvelope event) {
        var graphInstance = graphInstances.get(headers.get(IN));
        if (graphInstance == null) {
            return;
        }
        var token = graphInstance.getRunWatcher();
        // the watcher may act only for the run that armed it: the slot token carries the
        // owning run's correlation id and the atomic removal is the claim - a stale
        // watcher (a newer run owns the slot, or a canceller already won) stays silent
        if (token == null || !token.startsWith(event.getCorrelationId() + "|")
                || !graphInstance.clearRunWatcher(token) || !claimTerminal(graphInstance)) {
            return;
        }
        var out = graphInstance.getReplyTo();
        try {
            po.send(new EventEnvelope().setTo(out).setCorrelationId(event.getCorrelationId())
                    .setStatus(408)
                    .setBody("Graph traversal timed out after " + getModelTtl(graphInstance) + " ms"));
            emitAborted(po, graphInstance);
        } catch (Exception e) {
            // best-effort: the reply route may be a released companion capture route -
            // the run is already marked complete, so bookkeeping stays consistent
            log.debug("Run-timeout terminal for {} not deliverable - {}",
                    graphInstance.graphId, e.getMessage());
        }
    }

    private void handleSkillResponse(PostOffice po, EventEnvelope response) {
        var compositeId = response.getCorrelationId();
        var at = compositeId.indexOf('@');
        var wsInstance = compositeId.substring(0, at);
        var nodeName = compositeId.substring(at+1);
        var graphInstance = graphInstances.get(wsInstance);
        // a late reply after the run reached a terminal (completed, aborted or timed
        // out) is dropped before any console send - the reply route may already be a
        // released companion capture route
        if (graphInstance == null || graphInstance.complete.get()) {
            return;
        }
        var stateMachine = graphInstance.stateMachine;
        var target = stateMachine.getElement(nodeName + "." + TARGET);
        // Unrecoverable error from the node itself
        if (response.hasError()) {
            if (target != null) {
                var eMap = getErrorMap(stateMachine.getElement(OUTPUT_BODY), target);
                stateMachine.setElement(OUTPUT_BODY, eMap);
            }
            handleErrorResponse(po, graphInstance, response);
            return;
        }
        handleSkillSuccess(po, graphInstance, nodeName, target, response);
    }

    private void handleSkillSuccess(PostOffice po, GraphInstance graphInstance, String nodeName,
                                    Object target, EventEnvelope response) {
        var stateMachine = graphInstance.stateMachine;
        var node = graphInstance.graph.findNodeByAlias(nodeName);
        checkFrequency(po, graphInstance, nodeName);
        // advise user that the node with skill has been executed
        var skill = node.getProperty(SKILL);
        var replyTo = graphInstance.getReplyTo();
        po.send(new EventEnvelope().setTo(replyTo).setBody("Executed " + nodeName + " with skill " + skill +
                " in " +response.getExecutionTime() + " ms"));
        // Skill handler would set status and error in its node properties
        // e.g. the HTTP response status code to the API fetcher >= 400
        var processStatus = stateMachine.getElement(nodeName + "." + STATUS);
        var resultError = stateMachine.getElement(nodeName + "." + ERROR);
        // Mark the skill complete only when it did NOT fail (status + error set,
        // e.g. an exception-routed fetcher): a join barrier consults skillRun,
        // so a failed branch must not satisfy the barrier while it retries.
        // GraphExecutor keeps identical semantics.
        if (!(processStatus instanceof Integer && resultError != null)) {
            graphInstance.skillRun.put(nodeName, true);
        }
        var errorHandler = node.getProperty(EXCEPTION);
        if (processStatus instanceof Integer rc && resultError != null && errorHandler == null) {
            if (claimTerminal(graphInstance)) {
                var errorMap = getErrorMap(resultError, target);
                var cid = graphInstance.getCorrelationId();
                var error = new EventEnvelope().setTo(replyTo).setCorrelationId(cid)
                                                .setBody(errorMap).setStatus(rc);
                po.send(error);
                emitAborted(po, graphInstance);
            }
        } else if (!graphInstance.complete.get()) {
            var next = String.valueOf(response.getBody());
            decideNext(po, node, next, graphInstance);
        }
    }

    private void checkFrequency(PostOffice po, GraphInstance graphInstance, String nodeName) {
        var frequency = graphInstance.hits.computeIfAbsent(nodeName, k -> new Visits());
        var now = System.currentTimeMillis();
        var last = frequency.lastVisit.get();
        if (now - last > getLoopInterval()) {
            frequency.lastVisit.set(now);
            frequency.hits.set(0);
        }
        var total = frequency.hits.incrementAndGet();
        if (total > getHighFrequency()) {
            log.error("Looping detected - {} hits in {} ms for {} in {}",
                    total, now - last, nodeName, graphInstance.graphId);
            var response = new EventEnvelope().setBody("Node " + nodeName + " executed too frequently").setStatus(400);
            handleErrorResponse(po, graphInstance, response);
        }
    }

    private void decideNext(PostOffice po, SimpleNode node, String next, GraphInstance graphInstance) {
        var graph = graphInstance.graph;
        var endNode = graph.getEndNode();
        if (endNode.getId().equals(node.getId())) {
            executionComplete(po, graphInstance);
        } else {
            nextOrJump(po, graphInstance, node, next);
        }
    }

    private void walk(PostOffice po, GraphInstance graphInstance, SimpleNode node, String from) {
        if (!graphInstance.complete.get()) {
            var nodeName = node.getAlias();
            String skill = node.getProperty(SKILL) != null ? String.valueOf(node.getProperty(SKILL)) : null;
            // atomic mark-and-test: concurrent branches converging on the same
            // non-join node must not dispatch it twice (a join always evaluates -
            // its barrier logic owns the dedup)
            var isJoin = GraphJoin.ROUTE.equals(skill);
            var seen = graphInstance.nodeSeen.putIfAbsent(nodeName, true) != null;
            var out = graphInstance.getReplyTo();
            if (isJoin || !seen) {
                po.send(new EventEnvelope().setTo(out).setBody("Walk to " + nodeName));
                walkTo(po, skill, graphInstance, node, from);
            }
        }
    }

    private void walkTo(PostOffice po, String skill, GraphInstance graphInstance, SimpleNode node, String from) {
        var graph = graphInstance.graph;
        var endNode = graph.getEndNode();
        if (endNode.getId().equals(node.getId())) {
            if (skill != null) {
                executeSkill(po, skill, graphInstance, node, from);
            } else {
                executionComplete(po, graphInstance);
            }
        } else {
            if (skill != null) {
                executeSkill(po, skill, graphInstance, node, from);
            } else if (isSuspensible(node)) {
                walkToSuspendNode(po, graphInstance, node);
            } else {
                walkNext(po, graphInstance, node, false);
            }
        }
    }

    private void executionComplete(PostOffice po, GraphInstance graphInstance) {
        if (!claimTerminal(graphInstance)) {
            return;
        }
        var stateMachine = graphInstance.stateMachine;
        var in = graphInstance.getWsInstance();
        var out = graphInstance.getReplyTo();
        var value = stateMachine.getElement(OUTPUT, false);
        if (value instanceof Map || value instanceof List) {
            var text = SimpleMapper.getInstance().getMapper().writeValueAsString(value);
            if (text.length() > MAX_BUFFER_SIZE) {
                var name = getTempGraphName(in);
                po.send(new EventEnvelope().setTo(out).setBody(
                        "Large payload (" + text.length() +") -> GET /api/inspect/"+name+"/"+OUTPUT));
            } else {
                po.send(new EventEnvelope().setTo(out).setBody(Map.of(OUTPUT, value)));
            }
        } else {
            po.send(new EventEnvelope().setTo(out).setBody(Map.of(OUTPUT, value)));
        }
        long elapsed = System.currentTimeMillis() - graphInstance.getStartTime();
        po.send(new EventEnvelope().setTo(out).setBody("Graph traversal completed in " + elapsed + " ms"));
    }

    private void executeSkill(PostOffice po, String skill, GraphInstance graphInstance, SimpleNode node, String from) {
        if (po.exists(skill)) {
            var wsInstanceId = graphInstance.getWsInstance();
            var nodeName = node.getAlias();
            var compositeId = wsInstanceId + "@" + nodeName;
            var event = new EventEnvelope().setTo(skill).setHeader(IN, wsInstanceId)
                    .setHeader(TYPE, EXECUTE).setHeader(NODE, nodeName)
                    .setReplyTo(GraphTraveler.ROUTE).setCorrelationId(compositeId);
            if (from != null) {
                event.setHeader(FROM, from);
            }
            // same business correlation-id propagation as GraphExecutor
            if (graphInstance.stateMachine.getElement(MODEL_CID) instanceof String businessCid
                    && !businessCid.isBlank()) {
                event.addTag(EventEmitter.BUSINESS_CID_TAG, businessCid.trim());
            }
            po.send(event);
        } else {
            sendError(po, graphInstance, "Skill " + skill + " does not exist");
        }
    }

    private void nextOrJump(PostOffice po, GraphInstance graphInstance, SimpleNode node, String next) {
        if (!SINK.equals(next)) {
            var graph = graphInstance.graph;
            if (next.startsWith(RESUME_PREFIX)) {
                resumeTraversal(po, graphInstance, next.substring(RESUME_PREFIX.length()));
            } else if (NEXT.equals(next)) {
                if (isSuspensible(node)) {
                    walkToSuspendNode(po, graphInstance, node);
                } else {
                    walkNext(po, graphInstance, node, false);
                }
            } else {
                var nextNode = graph.findNodeByAlias(next);
                if (nextNode != null) {
                    walk(po, graphInstance, nextNode, node.getAlias());
                } else {
                    sendError(po, graphInstance, "Next node '" + next + "' does not exist");
                }
            }
        }
    }

    private void walkToSuspendNode(PostOffice po, GraphInstance graphInstance, SimpleNode node) {
        var skill = node.getProperty(SKILL);
        if (GraphMath.ROUTE.equals(skill) || GraphJs.ROUTE.equals(skill)) {
            sendError(po, graphInstance, "Node '" + node.getAlias() +
                    "' cannot use 'suspend=true' with skill " + skill);
            return;
        }
        var suspendNode = graphInstance.graph.findNodeByAlias(SUSPEND);
        if (suspendNode == null) {
            sendError(po, graphInstance, "Node '" + node.getAlias() +
                    "' is suspensible but the graph has no '" + SUSPEND + "' node");
        } else if (!GraphSuspend.ROUTE.equals(suspendNode.getProperty(SKILL))) {
            sendError(po, graphInstance, "The '" + SUSPEND + "' node must use skill " + GraphSuspend.ROUTE);
        } else {
            walk(po, graphInstance, suspendNode, node.getAlias());
        }
    }

    private void resumeTraversal(PostOffice po, GraphInstance graphInstance, String alias) {
        var resumedNode = graphInstance.graph.findNodeByAlias(alias);
        if (resumedNode == null) {
            sendError(po, graphInstance, "Resumed node '" + alias + "' does not exist");
        } else {
            // the suspension point already ran before suspension - do not re-execute it
            graphInstance.nodeSeen.put(alias, true);
            graphInstance.skillRun.put(alias, true);
            walkNext(po, graphInstance, resumedNode, true);
        }
    }

    private void walkNext(PostOffice po, GraphInstance graphInstance, SimpleNode node, boolean afterResume) {
        if (!graphInstance.complete.get()) {
            var graph = graphInstance.graph;
            var nodes = graph.getForwardLinks(node.getAlias());
            var deadEnd = true;
            for (SimpleNode next : nodes) {
                // a resumed traversal continues along the normal path, never back into suspension
                if (afterResume && SUSPEND.equals(next.getAlias())) {
                    continue;
                }
                deadEnd = false;
                walk(po, graphInstance, next, node.getAlias());
            }
            if (afterResume && deadEnd) {
                sendError(po, graphInstance, "Resumed node '" + node.getAlias() +
                        "' has no forward path to continue");
            }
        }
    }

    private void handleErrorResponse(PostOffice po, GraphInstance graphInstance, EventEnvelope response) {
        if (!claimTerminal(graphInstance)) {
            return;
        }
        var out = graphInstance.getReplyTo();
        var error = new EventEnvelope().setTo(out).setCorrelationId(graphInstance.getCorrelationId())
                                        .setBody(response.getBody()).setStatus(response.getStatus());
        po.send(error);
        emitAborted(po, graphInstance);
    }

    /**
     * Canonical failure terminal — the mirror of the success terminal in
     * {@code executionComplete}. Emits the single end-of-transmission line the
     * synchronous companion endpoint drains on, so <b>every</b> {@code run} finishes
     * with either "Graph traversal completed in N ms" or "Graph traversal aborted" —
     * a deterministic signal, never a timeout. Callers own the terminal via
     * {@link #claimTerminal} before emitting.
     */
    private void emitAborted(PostOffice po, GraphInstance graphInstance) {
        po.send(new EventEnvelope().setTo(graphInstance.getReplyTo())
                .setCorrelationId(graphInstance.getCorrelationId())
                .setBody("Graph traversal aborted").setStatus(400));
    }

    /**
     * Emit a specific failure reason and then the canonical {@link #emitAborted}
     * terminal, so the human/companion sees <i>why</i> and any watcher (the sync
     * endpoint included) still gets the uniform end-of-transmission line last.
     */
    private void sendError(PostOffice po, GraphInstance graphInstance, String message) {
        if (!claimTerminal(graphInstance)) {
            return;
        }
        var error = new EventEnvelope().setTo(graphInstance.getReplyTo())
                            .setCorrelationId(graphInstance.getCorrelationId()).setBody(message).setStatus(400);
        po.send(error);
        emitAborted(po, graphInstance);
    }
}
