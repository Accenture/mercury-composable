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
import com.accenture.minigraph.skills.GraphIsland;
import com.accenture.minigraph.skills.GraphJs;
import com.accenture.minigraph.skills.GraphMath;
import com.accenture.minigraph.skills.GraphResume;
import com.accenture.minigraph.skills.GraphSuspend;
import com.accenture.minigraph.skills.GraphTask;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 'graph.suspend' skill in both directions; a node with a drawn edge to 'suspend' is an
 * edge-mode suspension point and needs a continuation edge; a routing-skill node must
 * not draw an edge to 'suspend' - a decision reaches the checkpoint by jumping and is
 * re-executed on resume; the 'suspend' node cannot be an exception handler; the suspend
 * node needs 'task', a valid 'ttl' and an outgoing connection; a 'resume' node needs
 * 'task'; the retired 'suspend=true' property is a deprecation WARN), the placement and
 * grammar of the per-node 'ttl' parameter (store expiry on suspend; child-call deadline
 * on 'graph.extension' / 'graph.api.fetcher' / 'graph.task'; rejected elsewhere), and model
 * metadata immutability (no data mapping may write model.cid/ttl/... - the runtime
 * mapping guard in GraphLambdaFunction is the second layer of the same rule).
 */
public class GraphModelValidator {
    private static final Logger log = LoggerFactory.getLogger(GraphModelValidator.class);
    private static final String SKILL = "skill";
    private static final String TASK = "task";
    private static final String TTL = "ttl";
    private static final String SUSPEND = "suspend";
    private static final String EXCEPTION = "exception";
    private static final String NODE_NAME = "node ";
    private static final String MODEL_PREFIX = "model";
    private static final String STATEMENT = "statement";
    private static final String MAP_TO = "->";
    // the skills that CONSUME a 'task' route. graph.suspend and graph.resume are documented
    // supersets of graph.task - their 'task' names the pluggable state-store function - so all
    // three legitimately carry one, and no other skill does.
    private static final Set<String> TASK_CONSUMING_SKILLS =
            Set.of(GraphTask.ROUTE, GraphSuspend.ROUTE, GraphResume.ROUTE);
    // skills whose 'ttl' node parameter is the DEADLINE override: a child-call deadline
    // on graph.extension / graph.api.fetcher / graph.task, and the script execution
    // deadline (GraalVM context interrupt) on graph.js
    private static final Set<String> DEADLINE_TTL_SKILLS =
            Set.of(GraphExtension.ROUTE, GraphApiFetcher.ROUTE, GraphTask.ROUTE, GraphJs.ROUTE);
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";
    // the mapping-list node properties a data mapping can appear in
    private static final List<String> MAPPING_PROPERTIES = List.of("mapping", INPUT, OUTPUT, "for_each");

    private GraphModelValidator() {}

    /**
     * Validate the whole-graph contract of a complete graph model: the suspend/resume rules,
     * per-node ttl placement and grammar, and model metadata immutability.
     * <p>
     * Reserved aliases (including 'error', the generic exception-context namespace) need
     * no rule here: MiniGraph rejects them at node creation, so neither lane can even
     * import such a model.
     *
     * @param graph an imported MiniGraph
     * @throws IllegalArgumentException describing the first violated rule
     */
    public static void validate(MiniGraph graph) {
        validateWorkingNodeHasSkill(graph);
        validateSuspendResume(graph);
        validateNodeTtl(graph);
        validateModelMetadataImmutability(graph);
    }

    /**
     * A node that names a composable function must say which skill runs it. A node carrying a
     * {@code task} route but no {@code skill} used to compile and pass silently as a structural
     * node: the graph traverses it, nothing executes, and nothing is reported. That cost a
     * debugging round in the E0 round (2026-09-01), where the model looked correct and did nothing.
     * <p>
     * Hard error, not a gate warning (Eric's ruling, 2026-09-16): a {@code task} route is a
     * composable function the author intends to call, so the node is <em>incomplete</em> rather than
     * unusual, and a warning on a silently-inert node is easy to scroll past - which is how the
     * original case survived.
     * <p>
     * <b>Only {@code task} is the signal - deliberately not {@code input}/{@code output}.</b> Those
     * are descriptor fields on non-executing node types: a {@code Provider} node uses {@code input}
     * for its HTTP request shape and a {@code Dictionary} node uses {@code output} for its
     * projection, and neither carries a skill by design. Broadening this check to the mapping
     * properties rejects valid models - it was tried, and the tutorial graphs caught it. Across
     * every graph model in this repository, {@code task} without {@code skill} occurs zero times.
     * <p>
     * The second rule is the same defect one step later: a {@code task} paired with a skill that
     * never reads one is equally inert. Only {@link GraphTask}, {@link GraphSuspend} and
     * {@link GraphResume} consume a task route - the latter two are documented supersets of
     * graph.task whose {@code task} names the pluggable state-store function.
     * <p>
     * The rule is <b>bidirectional</b> (Eric, 2026-09-16): those three skills each call a composable
     * function, so each one <em>requires</em> a task route as well. A {@code graph.task} node with no
     * task is the same inert node arrived at from the other side. Both directions hold without
     * exception across every model in this repository - 67 nodes carry one of the three skills and
     * all 67 carry a task; no node carries a task under any other skill.
     *
     * @param graph an imported MiniGraph
     * @throws IllegalArgumentException naming the node and its unreachable task route
     */
    private static void validateWorkingNodeHasSkill(MiniGraph graph) {
        for (SimpleNode node : graph.getNodes()) {
            var task = node.getProperty(TASK);
            var skill = node.getProperty(SKILL);
            boolean consumesTask = skill != null && TASK_CONSUMING_SKILLS.contains(String.valueOf(skill));
            if (task != null && skill == null) {
                throw new IllegalArgumentException(NODE_NAME + node.getAlias() + " has task '"
                        + task + "' but no '" + SKILL + "' - a node that names a composable function "
                        + "must name the skill that calls it, or the graph traverses the node and "
                        + "nothing runs");
            }
            if (task != null && !consumesTask) {
                throw new IllegalArgumentException(NODE_NAME + node.getAlias() + " has task '"
                        + task + "' but skill '" + skill + "' does not call a task - use "
                        + taskConsumingSkills() + "; the route would be ignored");
            }
            if (task == null && consumesTask) {
                throw new IllegalArgumentException(NODE_NAME + node.getAlias() + " uses skill '"
                        + skill + "' but has no '" + TASK + "' - " + taskConsumingSkills()
                        + " each call a composable function, so the route is required");
            }
        }
    }

    /** The task-consuming skills, rendered for an error message. */
    private static String taskConsumingSkills() {
        return GraphTask.ROUTE + " (or " + GraphSuspend.ROUTE + " / " + GraphResume.ROUTE
                + ", whose task names the state-store function)";
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
            warnIfRetiredSuspendProperty(graph, node);
            validateNoRoutingSkillSuspendEdge(graph, node);
            validateExceptionTarget(node);
            validateContinuationEdge(graph, node);
        }
    }

    /**
     * The 'suspend=true' property is retired: a drawn edge to the 'suspend' node is the
     * suspension declaration (edge mode), and a decision jumps to the checkpoint instead
     * (jump mode). The property is accepted and ignored for one deprecation window so
     * v4.11.x models deploy unmodified - every valid v4.11.x suspensible node already
     * draws the checkpoint edge, which now declares the same behavior.
     */
    private static void warnIfRetiredSuspendProperty(MiniGraph graph, SimpleNode node) {
        if ("true".equalsIgnoreCase(String.valueOf(node.getProperty(SUSPEND)))) {
            var alias = node.getAlias();
            if (hasEdgeToSuspend(graph, alias)) {
                log.warn("Node '{}' uses the retired 'suspend=true' property - it is ignored; " +
                        "the drawn edge to the '{}' node already declares the suspension point " +
                        "(remove the property)", alias, SUSPEND);
            } else {
                log.warn("Node '{}' uses the retired 'suspend=true' property and has no drawn " +
                        "edge to the '{}' node - it will NOT suspend; draw the edge from a " +
                        "working node, or jump from a decision's IF-THEN-ELSE", alias, SUSPEND);
            }
        }
    }

    /**
     * A decision's forward links are outcome alternatives, not branches: if a
     * routing-skill node drew an edge to 'suspend', a resumed run would fan out its
     * alternatives as if they were parallel branches. A decision reaches the checkpoint
     * by jumping (return 'suspend' from IF-THEN-ELSE) and is re-executed on resume.
     */
    private static void validateNoRoutingSkillSuspendEdge(MiniGraph graph, SimpleNode node) {
        var skill = node.getProperty(SKILL);
        if ((GraphMath.ROUTE.equals(skill) || GraphJs.ROUTE.equals(skill))
                && hasEdgeToSuspend(graph, node.getAlias())) {
            throw new IllegalArgumentException(NODE_NAME + node.getAlias() +
                    " has a drawn edge to the '" + SUSPEND + "' node but uses routing skill " + skill +
                    " - a decision reaches the checkpoint by jumping: return '" + SUSPEND +
                    "' from its IF-THEN-ELSE and draw edges to '" + SUSPEND + "' only from working nodes");
        }
    }

    /**
     * The suspend node cannot be an exception handler - checkpoint-on-failure would give
     * a failed node retry-on-resume semantics through the back door. Route failures to a
     * handler node.
     */
    private static void validateExceptionTarget(SimpleNode node) {
        if (SUSPEND.equals(node.getProperty(EXCEPTION))) {
            throw new IllegalArgumentException(NODE_NAME + node.getAlias() +
                    " routes its 'exception' to the '" + SUSPEND + "' node - the suspend node cannot " +
                    "be an exception handler; route failures to a handler node");
        }
    }

    private static boolean hasEdgeToSuspend(MiniGraph graph, String alias) {
        for (SimpleNode next : graph.getForwardLinks(alias)) {
            if (SUSPEND.equals(next.getAlias())) {
                return true;
            }
        }
        return false;
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

    private static void validateContinuationEdge(MiniGraph graph, SimpleNode node) {
        // a node with a drawn edge to the checkpoint is an edge-mode suspension point:
        // a resumed run continues along its forward links excluding 'suspend', so at
        // least one continuation edge must exist - a suspend-only node would loop on
        // resume. Shape-only rule: it applies regardless of skill (inspecting a
        // decision's IF-THEN-ELSE logic is deliberately out of scope). The one
        // exemption is also shape-level: an island's outgoing edges are never traversed
        // (the branch stops there), so an island-to-suspend edge is the ANCHOR that
        // keeps a jump-only suspend node non-orphan, not a checkpoint path
        if (GraphIsland.ROUTE.equals(node.getProperty(SKILL))) {
            return;
        }
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
     * 'graph.extension' / 'graph.api.fetcher' / 'graph.task' (optional). On any other skilled node it
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
     * mapping-list properties AND in 'MAPPING:' lines embedded in 'graph.math' / 'graph.js' statements
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
