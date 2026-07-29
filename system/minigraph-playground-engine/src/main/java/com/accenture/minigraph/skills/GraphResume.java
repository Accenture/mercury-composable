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
import org.platformlambda.core.models.SimpleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * The graph.resume skill restores the workflow state persisted by graph.suspend and
 * continues traversal from the recorded suspension point without re-executing it. It is
 * a superset of graph.task: the "task" property names the pluggable store function
 * (headers type=get, body {cid}), but restoration is encapsulated by the skill - the
 * node needs no input/output data mapping.
 * <p>
 * Place the resume node early in the traversal (conventionally named "resume", right
 * after root, or after setup nodes). When the store has a record for the business
 * correlation ID (model.cid), the skill merges the persisted model key-values into the
 * state machine - the current run's reserved keys (model.cid/instance/flow/ttl/trace)
 * always win because graph.suspend never persists them - restores the traversal
 * bookkeeping so downstream join barriers still see pre-suspension branches, and jumps
 * past the suspension point.
 * <p>
 * When there is no record - a fresh transaction (the normal first-run case) or an
 * expired one - traversal simply continues along the resume node's own forward path.
 * The optional "missing" property names a node to jump to instead, for workflows where
 * an absent record needs distinct handling (e.g. an expired-approval response).
 */
@PreLoad(route = GraphResume.ROUTE, instances = 300)
public class GraphResume extends GraphStateSkill {
    private static final Logger log = LoggerFactory.getLogger(GraphResume.class);
    public static final String ROUTE = "graph.resume";

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        var ctx = getContext(headers, instance, ROUTE);
        var node = ctx.node();
        var nodeName = node.getAlias();
        var graphInstance = ctx.graphInstance();
        var cid = getRequiredCorrelationId(graphInstance, nodeName);
        var stateMachine = graphInstance.stateMachine;
        var timeout = getModelTtl(graphInstance);
        var request = new EventEnvelope().setTo(ctx.route()).setCorrelationId(util.getUuid())
                .setHeader(TYPE, GET).setBody(Map.of(CID, cid));
        ctx.po().annotateTrace(TASK, ctx.route());
        ctx.po().annotateTrace(CID, cid);
        return Mono.create(sink ->
            ctx.po().eRequest(request, timeout, false).thenAccept(response -> {
                stateMachine.setElement(nodeName + "." + STATUS, response.getStatus());
                if (response.hasError()) {
                    sink.success(setError(stateMachine, node, response));
                } else if (response.getBody() instanceof Map<?, ?> received && !received.isEmpty()) {
                    sink.success(restoreAndJump(graphInstance, node, cid, received));
                } else {
                    // no suspension record: a fresh transaction is the normal case
                    var missing = node.getProperty(MISSING);
                    var target = missing instanceof String value && !value.isBlank()? value.trim() : null;
                    log.info("No suspension record for cid {} - {}", cid,
                            target == null? "fresh start" : "jump to '" + target + "'");
                    sink.success(target == null? NEXT : target);
                }
            }));
    }

    @SuppressWarnings("unchecked")
    private String restoreAndJump(GraphInstance graphInstance, SimpleNode node, String cid, Map<?, ?> received) {
        var record = (Map<String, Object>) received;
        var stateMachine = graphInstance.stateMachine;
        var suspendedAt = record.get(NODE) instanceof String value && !value.isBlank()? value.trim() : null;
        if (suspendedAt == null) {
            return recordFailure(stateMachine, node, 500, "Corrupted suspension record - missing 'node'");
        }
        if (graphInstance.graph.findNodeByAlias(suspendedAt) == null) {
            return recordFailure(stateMachine, node, 500, "Suspension record refers to unknown node '" +
                    suspendedAt + "' - the graph model may have changed");
        }
        if (record.get(MODEL) instanceof Map<?, ?> persisted &&
                stateMachine.getElement(MODEL) instanceof Map<?, ?> current) {
            // persisted keys are authoritative for the workflow; the current run's
            // reserved keys survive because graph.suspend never persists them
            ((Map<String, Object>) current).putAll((Map<String, Object>) persisted);
        }
        restoreMarks(record.get(SEEN), graphInstance.nodeSeen);
        restoreMarks(record.get(RUN), graphInstance.skillRun);
        log.info("Resume at '{}' for cid {}", suspendedAt, cid);
        return RESUME_PREFIX + suspendedAt;
    }

    private void restoreMarks(Object marks, ConcurrentMap<String, Boolean> target) {
        if (marks instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                var name = String.valueOf(k);
                // the suspend node's marks are per-run mechanics: restoring them would
                // block re-suspension at a later checkpoint in the resumed run
                if (!SUSPEND.equals(name) && (Boolean.TRUE.equals(v) || "true".equals(String.valueOf(v)))) {
                    target.put(name, true);
                }
            });
        }
    }
}
