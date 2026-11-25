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

package com.accenture.minigraph.rest;

import com.accenture.minigraph.common.TaskServer;
import com.accenture.minigraph.models.CompiledGraphs;
import com.accenture.minigraph.start.HotDeployment;
import com.accenture.models.Flows;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * POST /api/graph-set/deploy - deploy a graph set to the running engine (RFC-0017). The body is the package, a
 * .pack file sent as application/octet-stream; an optional X-Graph-Set-SHA256 header is the digest the caller
 * expects, from the field's ledger. The set is read strictly, named after its one main graph, refused when a graph
 * id a manifest or another set owns (409), staged and gated, then registered all or none in one swap and recorded.
 * <p>
 * The answer is the deployment - set, version, sha256, graphs, deployed_at, replaced when the set was already
 * deployed, changed - and an advisory, never a refusal: the task routes the set names, each known on this instance,
 * unknown, or remote through server.url; and the graph.extension targets outside the set, each known, unknown, or
 * another set's private subgraph. A refusal answers with every reason in its message.
 */
@OptionalService(HotDeployment.DEPLOY_KEY)
@PreLoad(route = "deploy.graph.set", instances = 10)
public class DeployGraphSet implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {
    private static final String DIGEST_HEADER = "x-graph-set-sha256";
    private static final String NODES = "nodes";
    private static final String PROPERTIES = "properties";
    private static final String SKILL = "skill";
    private static final String TASK = "task";
    private static final String EXTENSION = "extension";
    private static final String FLOW_PROTOCOL = "flow://";
    private static final String KNOWN = "known";
    private static final String UNKNOWN = "unknown";
    private static final Set<String> TASK_SKILLS = Set.of("graph.task", "graph.suspend", "graph.resume");

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance)
            throws InterruptedException {
        var po = PostOffice.trackable(headers, instance);
        var bytes = UnpackGraphSet.packageBytes(input);
        final HotDeployment.Deployment deployment;
        try {
            deployment = HotDeployment.deploy(bytes, input.getHeader(DIGEST_HEADER));
        } catch (HotDeployment.Refused e) {
            throw new AppException(e.getStatus(), e.getMessage());
        }
        po.annotateTrace("set", deployment.set());
        return new EventEnvelope().setHeader("Content-Type", "application/json")
                .setBody(answer(po, deployment));
    }

    static Map<String, Object> answer(PostOffice po, HotDeployment.Deployment deployment) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("set", deployment.set());
        result.put("version", deployment.version());
        result.put("sha256", deployment.sha256());
        result.put("graphs", deployment.graphs());
        result.put("deployed_at", deployment.deployedAt());
        result.put("changed", deployment.changed());
        if (deployment.replaced() != null) {
            result.put("replaced", Map.of("version", deployment.replaced().version(),
                    "sha256", deployment.replaced().sha256()));
        }
        result.put("advisory", advisory(po, deployment.graphs()));
        return result;
    }

    /**
     * What the set reaches for, as this instance sees it today - a report, never a gate: a route known now can be
     * gone at run time, and a provider the set names decides for itself.
     */
    private static Map<String, Object> advisory(PostOffice po, List<String> graphs) {
        List<Map<String, String>> routes = new ArrayList<>();
        List<Map<String, String>> extensions = new ArrayList<>();
        var inSet = Set.copyOf(graphs);
        for (var graphId : graphs) {
            var model = CompiledGraphs.getGraph(graphId);
            if (model == null || !(model.get(NODES) instanceof List<?> nodes)) {
                continue;
            }
            for (var item : nodes) {
                if (item instanceof Map<?, ?> node && node.get(PROPERTIES) instanceof Map<?, ?> properties) {
                    describeNode(po, graphId, properties, inSet, routes, extensions);
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("routes", routes);
        result.put("extensions", extensions);
        return result;
    }

    private static void describeNode(PostOffice po, String graphId, Map<?, ?> properties, Set<String> inSet,
                                     List<Map<String, String>> routes, List<Map<String, String>> extensions) {
        var skill = String.valueOf(properties.get(SKILL));
        if (TASK_SKILLS.contains(skill) && properties.get(TASK) instanceof String route && !route.isBlank()) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("graph", graphId);
            row.put("route", route.trim());
            if (TaskServer.urlOf(properties) instanceof String server) {
                row.put("status", "remote");
                row.put("server", server);
            } else {
                row.put("status", po.exists(route.trim()) || po.getEventHttpTarget(route.trim()) != null?
                        KNOWN : UNKNOWN);
            }
            routes.add(row);
        }
        if ("graph.extension".equals(skill) && properties.get(EXTENSION) instanceof String target
                && !target.isBlank() && !inSet.contains(target.trim())) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("graph", graphId);
            row.put("target", target.trim());
            row.put("status", extensionStatus(target.trim()));
            extensions.add(row);
        }
    }

    private static String extensionStatus(String target) {
        if (target.startsWith(FLOW_PROTOCOL)) {
            return Flows.getFlow(target.substring(FLOW_PROTOCOL.length())) == null? UNKNOWN : KNOWN;
        }
        if (CompiledGraphs.getGraphLocation(target) == null) {
            return UNKNOWN;
        }
        return HotDeployment.isPrivateSubgraph(target)? "private" : KNOWN;
    }
}
