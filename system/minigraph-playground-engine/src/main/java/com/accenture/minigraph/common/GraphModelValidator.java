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

package com.accenture.minigraph.common;

import com.accenture.minigraph.skills.GraphApiFetcher;
import com.accenture.minigraph.skills.GraphExtension;
import com.accenture.minigraph.skills.GraphJs;
import com.accenture.minigraph.skills.GraphMath;
import com.accenture.minigraph.skills.GraphResume;
import com.accenture.minigraph.skills.GraphSuspend;
import com.accenture.minigraph.skills.GraphTask;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.util.Utility;

import java.util.List;
import java.util.Set;

/**
 * Reusable whole-graph contract checks, shared by two callers:
 * <p>
 * 1. CompileGraph - the deployment quality gate validates every manifest graph at
 *    startup; only graphs that pass become executable by GraphExecutor.
 * 2. The playground's "run" command - draft authoring deliberately allows partial
 *    models (a product owner builds a graph step by step and may save an incomplete
 *    draft). These rules are checked at the moment the author asks to execute,
 *    just before GraphTraveler takes over.
 * <p>
 * The rules here are whole-graph properties that per-command input validation cannot
 * express: the suspend/resume contract ('suspend' is a reserved alias bound to the
 * 'graph.suspend' skill in both directions; a suspensible node must not use a routing
 * skill, requires the suspend node and must draw its checkpoint edge to it; every
 * suspension point needs a continuation edge; the suspend node needs 'task', a valid
 * 'ttl' and an outgoing connection; a 'resume' node needs 'task'), the placement and
 * grammar of the per-node 'ttl' parameter (store expiry on suspend; child-call deadline
 * on graph.extension / graph.api.fetcher / graph.task; rejected elsewhere), and model
 * metadata immutability (no data mapping may write model.cid/ttl/... - the runtime
 * mapping guard in GraphLambdaFunction is the second layer of the same rule).
 */
public class GraphModelValidator {
    private static final String SKILL = "skill";
    private static final String TASK = "task";
    private static final String TTL = "ttl";
    private static final String SUSPEND = "suspend";
    private static final String NODE_NAME = "node ";
    private static final String MODEL_PREFIX = "model";
    private static final String STATEMENT = "statement";
    private static final String MAP_TO = "->";
    // skills whose 'ttl' node parameter is the DEADLINE override: a child-call deadline
    // on graph.extension / graph.api.fetcher / graph.task, and the script execution
    // deadline (GraalVM context interrupt) on graph.js
    private static final Set<String> DEADLINE_TTL_SKILLS =
            Set.of(GraphExtension.ROUTE, GraphApiFetcher.ROUTE, GraphTask.ROUTE, GraphJs.ROUTE);
    // the mapping-list node properties a data mapping can appear in
    private static final List<String> MAPPING_PROPERTIES = List.of("mapping", "input", "output", "for_each");

    private GraphModelValidator() {}

    /**
     * Validate the whole-graph contract of a complete graph model: the suspend/resume rules,
     * per-node ttl placement and grammar, and model metadata immutability.
     *
     * @param graph an imported MiniGraph
     * @throws IllegalArgumentException describing the first violated rule
     */
    public static void validate(MiniGraph graph) {
        validateSuspendResume(graph);
        validateNodeTtl(graph);
        validateModelMetadataImmutability(graph);
    }

    /**
     * Validate the suspend/resume contract of a complete graph model.
     *
     * @param graph an imported MiniGraph
     * @throws IllegalArgumentException describing the first violated rule
     */
    public static void validateSuspendResume(MiniGraph graph) {
        var suspendNode = graph.findNodeByAlias(SUSPEND);
        if (suspendNode != null) {
            validateSuspendNode(graph, suspendNode);
        }
        for (SimpleNode node : graph.getNodes()) {
            var alias = node.getAlias();
            var skill = node.getProperty(SKILL);
            if (GraphSuspend.ROUTE.equals(skill) && !SUSPEND.equals(alias)) {
                throw new IllegalArgumentException(NODE_NAME + alias +
                        " - a node with skill " + GraphSuspend.ROUTE + " must be named '" + SUSPEND + "'");
            }
            if (GraphResume.ROUTE.equals(skill)) {
                validateResumeNode(node);
            }
            if ("true".equalsIgnoreCase(String.valueOf(node.getProperty(SUSPEND)))) {
                validateSuspensibleNode(graph, node, suspendNode);
            }
            validateContinuationEdge(graph, node);
        }
    }

    private static void validateSuspendNode(MiniGraph graph, SimpleNode suspendNode) {
        if (!GraphSuspend.ROUTE.equals(suspendNode.getProperty(SKILL))) {
            throw new IllegalArgumentException("the '" + SUSPEND + "' node must use skill " + GraphSuspend.ROUTE);
        }
        if (withoutText(suspendNode.getProperty(TASK))) {
            throw new IllegalArgumentException(NODE_NAME + SUSPEND + " does not have a 'task' route");
        }
        // throws for a missing, blank, invalid or overflowing ttl (long-math guard)
        GraphSuspend.getValidTtlSeconds(suspendNode.getProperty(TTL), SUSPEND);
        // without a forward path the record persists and the run then stalls - the
        // caller would time out despite a successful checkpoint
        if (graph.getForwardLinks(SUSPEND).isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + SUSPEND +
                    " has no outgoing connection - the run must complete after the checkpoint (connect it to 'end')");
        }
    }

    private static void validateResumeNode(SimpleNode node) {
        if (withoutText(node.getProperty(TASK))) {
            throw new IllegalArgumentException(NODE_NAME + node.getAlias() + " does not have a 'task' route");
        }
    }

    private static void validateSuspensibleNode(MiniGraph graph, SimpleNode node, SimpleNode suspendNode) {
        var alias = node.getAlias();
        var skill = node.getProperty(SKILL);
        if (GraphMath.ROUTE.equals(skill) || GraphJs.ROUTE.equals(skill)) {
            throw new IllegalArgumentException(NODE_NAME + alias +
                    " cannot use 'suspend=true' with skill " + skill);
        }
        if (suspendNode == null) {
            throw new IllegalArgumentException(NODE_NAME + alias +
                    " is suspensible but the graph has no '" + SUSPEND + "' node");
        }
        for (SimpleNode next : graph.getForwardLinks(alias)) {
            if (SUSPEND.equals(next.getAlias())) {
                return;
            }
        }
        throw new IllegalArgumentException(NODE_NAME + alias +
                " is suspensible but has no connection to the '" + SUSPEND +
                "' node - the diagram must show the suspension path");
    }

    private static void validateContinuationEdge(MiniGraph graph, SimpleNode node) {
        // any node that routes to the checkpoint (suspend=true or a plain drawn edge)
        // is a suspension point: a resumed run continues along its forward links
        // excluding 'suspend', so at least one continuation edge must exist
        var routesToSuspend = false;
        var hasContinuation = false;
        for (SimpleNode next : graph.getForwardLinks(node.getAlias())) {
            if (SUSPEND.equals(next.getAlias())) {
                routesToSuspend = true;
            } else {
                hasContinuation = true;
            }
        }
        if (routesToSuspend && !hasContinuation) {
            throw new IllegalArgumentException(NODE_NAME + node.getAlias() +
                    " suspends but has no continuation edge - a resumed run could not continue");
        }
    }

    /**
     * The 'ttl' node parameter is skill-scoped: store-record expiry on the suspend node
     * (mandatory, checked by validateSuspendNode) and the child-call deadline override on
     * graph.extension / graph.api.fetcher / graph.task (optional). On any other skilled node it
     * is rejected rather than silently ignored. Grammar for the deadline form is the suspend
     * grammar (digits + s/m/h/d), validated here so a bad value fails the gate, not a live run.
     */
    private static void validateNodeTtl(MiniGraph graph) {
        for (SimpleNode node : graph.getNodes()) {
            var skill = node.getProperty(SKILL);
            if (skill == null || node.getProperty(TTL) == null || SUSPEND.equals(node.getAlias())) {
                continue;
            }
            if (DEADLINE_TTL_SKILLS.contains(String.valueOf(skill))) {
                // throws for a blank, invalid or overflowing duration (long-math guard)
                GraphSuspend.getValidTtlSeconds(node.getProperty(TTL), node.getAlias());
            } else {
                throw new IllegalArgumentException(NODE_NAME + node.getAlias()
                        + " - 'ttl' is only applicable to the suspend node or a node with skill "
                        + GraphExtension.ROUTE + ", " + GraphApiFetcher.ROUTE + ", " + GraphTask.ROUTE
                        + " or " + GraphJs.ROUTE);
            }
        }
    }

    /**
     * Model metadata (model.cid/instance/flow/ttl/trace/parent/root/none/run) is engine-managed
     * and immutable: reject any data mapping whose right-hand side writes to it - in the four
     * mapping-list properties AND in 'MAPPING:' lines embedded in graph.math / graph.js statements
     * (the same idiom the runtime guard sees). The runtime guard in GraphLambdaFunction enforces
     * the identical rule in both walker lanes; this compile-side twin fails the deployment gate
     * and the playground pre-run check early, so a statically detectable violation can never
     * abort a live traversal.
     */
    private static void validateModelMetadataImmutability(MiniGraph graph) {
        for (SimpleNode node : graph.getNodes()) {
            validateNodeMappings(node);
        }
    }

    private static void validateNodeMappings(SimpleNode node) {
        for (String property : MAPPING_PROPERTIES) {
            if (node.getProperty(property) instanceof List<?> entries) {
                for (Object entry : entries) {
                    assertNotMetadataWrite(node, String.valueOf(entry));
                }
            }
        }
        if (node.getProperty(STATEMENT) instanceof List<?> statements) {
            for (Object statement : statements) {
                var text = String.valueOf(statement).trim();
                if (text.toLowerCase().startsWith(GraphLambdaFunction.MAPPING_TAG)) {
                    assertNotMetadataWrite(node, text.substring(GraphLambdaFunction.MAPPING_TAG.length()));
                }
            }
        }
    }

    private static void assertNotMetadataWrite(SimpleNode node, String mapping) {
        int sep = mapping.lastIndexOf(MAP_TO);
        if (sep == -1) {
            return;
        }
        var rhs = mapping.substring(sep + MAP_TO.length()).trim();
        var segments = Utility.getInstance().split(rhs, ".[]");
        if (segments.size() > 1 && MODEL_PREFIX.equals(segments.getFirst())
                && GraphLambdaFunction.RESERVED_MODEL_METADATA.contains(segments.get(1))) {
            throw new IllegalArgumentException(NODE_NAME + node.getAlias()
                    + " - invalid mapping (" + mapping.trim() + "), model metadata is immutable");
        }
    }

    private static boolean withoutText(Object value) {
        return !(value instanceof String text) || text.isBlank();
    }
}
