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

package com.accenture.minigraph.skills;

import com.accenture.minigraph.models.GraphInstance;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * The 'graph.suspend' skill persists the workflow state of a graph instance to an external
 * state store so the transaction can resume later (see 'graph.resume'). It is a superset of
 * graph.task: the "task" property names the pluggable store function, but the persistence
 * envelope is assembled by the skill itself - the node needs no input/output data mapping.
 * <p>
 * The node carrying this skill MUST be named "suspend" - a reserved alias like root and
 * end - because traversal jumps to it by name: when a node with the "suspend=true"
 * property completes normally, the walker routes to the "suspend" node instead of the
 * node's normal forward path. A plain edge into the "suspend" node is an unconditional
 * suspension point.
 * <p>
 * The persistence envelope sent to the store function (headers type=put) is:
 * {cid, node, ttl, model, seen, run} - the business correlation ID (the retrieval key),
 * the suspension point (the node that routed here), the record's time-to-live in seconds
 * (from the node's "ttl" property, e.g. 20s/5m/2h/2d), the model namespace minus the
 * per-run reserved keys, and the traversal bookkeeping snapshots that let a resumed run
 * satisfy join barriers. The store must acknowledge with a 2xx reply before the graph
 * completes - a failed store call fails the node.
 * <p>
 * Unless the graph staged its own output before suspension, the skill stages a default
 * {"type": "suspended", "cid": ...} response body so the caller of the suspended run
 * receives a meaningful reply.
 */
@PreLoad(route = GraphSuspend.ROUTE, instances = 300)
public class GraphSuspend extends GraphStateSkill {
    private static final Logger log = LoggerFactory.getLogger(GraphSuspend.class);
    public static final String ROUTE = "graph.suspend";

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        var ctx = getContext(headers, instance, ROUTE);
        var node = ctx.node();
        var nodeName = node.getAlias();
        if (!SUSPEND.equals(nodeName)) {
            throw new IllegalArgumentException(NODE_NAME + nodeName +
                    " - a node with skill " + ROUTE + " must be named '" + SUSPEND + "'");
        }
        var graphInstance = ctx.graphInstance();
        var cid = getRequiredCorrelationId(graphInstance, nodeName);
        var from = headers.get(FROM);
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " - suspension point unknown; the '" +
                    SUSPEND + "' node must be reached from another node");
        }
        var ttlSeconds = getValidTtlSeconds(node.getProperty(TTL), nodeName);
        warnIfBranchesInFlight(graphInstance, from);
        var stateMachine = graphInstance.stateMachine;
        var timeout = getModelTtl(graphInstance);
        var request = new EventEnvelope().setTo(ctx.route()).setCorrelationId(util.getUuid())
                .setHeader(TYPE, PUT).setBody(getPersistenceEnvelope(graphInstance, cid, from, ttlSeconds));
        log.debug("Suspend at '{}' for cid {}, store={}, ttl={}s", from, cid, ctx.route(), ttlSeconds);
        ctx.po().annotateTrace(TASK, ctx.route());
        ctx.po().annotateTrace(CID, cid);
        // issue the request on the worker thread so the outbound event carries this span
        // as the store call's parent (the trace context is thread-keyed and would be gone
        // inside the Mono callback)
        var stored = ctx.po().eRequest(request, timeout, false);
        return Mono.create(sink ->
            stored.thenAccept(response -> {
                stateMachine.setElement(nodeName + "." + STATUS, response.getStatus());
                if (response.hasError()) {
                    sink.success(setError(stateMachine, node, response));
                } else {
                    // a meaningful default reply for the caller of the suspended run,
                    // unless the graph staged its own output before suspension
                    if (stateMachine.getElement(OUTPUT_BODY) == null) {
                        stateMachine.setElement(OUTPUT_BODY, Map.of(TYPE, SUSPENDED, CID, cid));
                    }
                    sink.success(NEXT);
                }
            }));
    }

    /**
     * Parse and validate a checkpoint ttl - the single implementation shared by this
     * skill and CompileGraph's static check.
     * <p>
     * Mirrors Utility.getDurationInSeconds (20s/5m/2h/2d) but computes in long
     * arithmetic: the int computation wraps for absurd values (e.g. a huge day
     * count), which could pass a naive "&lt; 1" guard and silently expire the
     * record far earlier than modeled.
     *
     * @param ttl the node's ttl property value
     * @param nodeAlias for the error message
     * @return the validated ttl in seconds
     */
    public static int getValidTtlSeconds(Object ttl, String nodeAlias) {
        if (ttl == null || String.valueOf(ttl).isBlank()) {
            throw new IllegalArgumentException(NODE_NAME + nodeAlias + " does not have a 'ttl' property");
        }
        var text = String.valueOf(ttl).trim();
        long multiplier = 1;
        var digits = text;
        var suffix = text.charAt(text.length() - 1);
        if (suffix == 's' || suffix == 'm' || suffix == 'h' || suffix == 'd') {
            digits = text.substring(0, text.length() - 1).trim();
            multiplier = switch (suffix) {
                case 'm' -> 60;
                case 'h' -> 3600;
                case 'd' -> 86400;
                default -> 1;
            };
        }
        var seconds = util.str2long(digits) * multiplier;
        if (seconds < 1 || seconds > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(NODE_NAME + nodeAlias + " - invalid ttl '" + ttl + "'");
        }
        return (int) seconds;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPersistenceEnvelope(GraphInstance graphInstance,
                                                       String cid, String from, int ttlSeconds) {
        var model = graphInstance.stateMachine.getElement(MODEL);
        var modelCopy = model instanceof Map?
                util.deepCopy((Map<String, Object>) model) : new HashMap<String, Object>();
        NON_PERSISTED_MODEL_KEYS.forEach(modelCopy::remove);
        var dataset = new HashMap<String, Object>();
        dataset.put(CID, cid);
        dataset.put(NODE, from);
        dataset.put(TTL, ttlSeconds);
        dataset.put(MODEL, modelCopy);
        dataset.put(SEEN, new HashMap<>(graphInstance.nodeSeen));
        dataset.put(RUN, new HashMap<>(graphInstance.skillRun));
        return dataset;
    }

    private void warnIfBranchesInFlight(GraphInstance graphInstance, String from) {
        // best-effort guard: a suspension point should be the sole active branch -
        // a node dispatched but not completed at suspension time cannot be persisted
        // (its callback will be orphaned when this run completes)
        for (var name : graphInstance.nodeSeen.keySet()) {
            if (!graphInstance.skillRun.containsKey(name) && !SUSPEND.equals(name) && !name.equals(from)) {
                var other = graphInstance.graph.findNodeByAlias(name);
                var skill = other == null? null : other.getProperty(SKILL);
                if (skill != null && !GraphJoin.ROUTE.equals(skill)) {
                    log.warn("Suspension while node '{}' may still be in flight - a suspension point " +
                            "should be the sole active branch in {}", name, graphInstance.graphId);
                }
            }
        }
    }
}
