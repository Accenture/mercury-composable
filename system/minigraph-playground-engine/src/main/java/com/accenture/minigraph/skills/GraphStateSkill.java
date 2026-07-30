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

import com.accenture.minigraph.common.GraphLambdaFunction;
import com.accenture.minigraph.models.GraphInstance;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;

import java.util.Map;
import java.util.Set;

/**
 * Common plumbing for the 'graph.suspend' and 'graph.resume' skills.
 * <p>
 * Both skills are a superset of graph.task: they invoke an attached composable function
 * (the "task" property - the pluggable state-store function) but encapsulate the
 * request/response mapping entirely, so the node needs no input/output data mapping.
 */
abstract class GraphStateSkill extends GraphLambdaFunction {
    private static final int INTERNAL_SERVER_ERROR = 500;
    // per-run engine metadata never crosses a suspension in either direction: graph.suspend
    // excludes these keys from the persistence envelope and graph.resume strips them from a
    // restored record - the resumed run's own values are authoritative ('run' is the
    // fresh/resume flag set by graph.resume; embalming it would let a later resume read a
    // stale condition, and the store is pluggable so a record is external input)
    protected static final Set<String> NON_PERSISTED_MODEL_KEYS =
            Set.of("cid", "instance", "flow", "ttl", "trace", "parent", "root", "none", "run");

    protected record SkillContext(PostOffice po, GraphInstance graphInstance, SimpleNode node, String route) {}

    protected SkillContext getContext(Map<String, String> headers, int instance, String skillRoute) {
        if (!EXECUTE.equals(headers.get(TYPE))) {
            throw new IllegalArgumentException("Type must be EXECUTE");
        }
        var po = PostOffice.trackable(headers, instance);
        var nodeName = headers.getOrDefault(NODE, "none");
        po.annotateTrace(NODE, nodeName);
        var graphInstance = getGraphInstance(headers.get(IN));
        var node = getNode(nodeName, graphInstance.graph);
        if (!skillRoute.equals(node.getProperty(SKILL))) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have skill - " + skillRoute);
        }
        var route = node.getProperty(TASK) instanceof String value && !value.isBlank()? value.trim() : null;
        if (route == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have a 'task' route");
        }
        if (!po.exists(route)) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " - task '" + route + "' does not exist");
        }
        // reset result to ensure execution is idempotent
        var stateMachine = graphInstance.stateMachine;
        stateMachine.removeElement(nodeName + "." + RESULT);
        stateMachine.removeElement(nodeName + "." + HEADER);
        stateMachine.removeElement(nodeName + "." + STATUS);
        stateMachine.removeElement(nodeName + "." + ERROR);
        stateMachine.setElement(nodeName + "." + TARGET, route);
        return new SkillContext(po, graphInstance, node, route);
    }

    protected String getRequiredCorrelationId(GraphInstance graphInstance, String nodeName) {
        // trim: a business correlation ID (e.g. an order number) may be entered by an
        // operator in a web UI - padding would otherwise split the store key space;
        // both engines trim identically so the mixed-fleet key stays one key
        if (graphInstance.stateMachine.getElement(MODEL_CID) instanceof String value && !value.isBlank()) {
            return value.trim();
        }
        throw new IllegalArgumentException(NODE_NAME + nodeName + " requires model.cid - " +
                "supply a business correlation ID (e.g. X-Correlation-Id header) or set model.cid");
    }

    protected String setError(MultiLevelMap stateMachine, SimpleNode node, EventEnvelope response) {
        var nodeName = node.getAlias();
        stateMachine.setElement(nodeName + "." + ERROR, response.getError());
        var errorHandler = node.getProperty(EXCEPTION);
        if (errorHandler == null) {
            stateMachine.setElement(OUTPUT_BODY, response.getBody());
            stateMachine.setElement(OUTPUT_NAMESPACE + HEADER, response.getHeaders());
            stateMachine.setElement(OUTPUT_NAMESPACE + STATUS, response.getStatus());
            return NEXT;
        } else {
            return String.valueOf(errorHandler);
        }
    }

    protected String recordFailure(MultiLevelMap stateMachine, SimpleNode node, String message) {
        // an invalid or corrupted store record fails the node so the walker's error
        // handling (or the node's exception handler) takes over - always an
        // internal server error because the record is engine-managed state
        stateMachine.setElement(node.getAlias() + "." + STATUS, INTERNAL_SERVER_ERROR);
        return setError(stateMachine, node,
                new EventEnvelope().setStatus(INTERNAL_SERVER_ERROR).setBody(message));
    }
}
