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

package com.accenture.minigraph.start;

import com.accenture.automation.SimpleTypeMatchingConverter;
import com.accenture.minigraph.common.GraphModelValidator;
import com.accenture.minigraph.models.CompiledGraphs;
import org.platformlambda.core.annotations.BeforeApplication;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 * <p>
 * CompileGraph is a quality gate for graph models, mirroring what CompileFlows does for event flows.
 * <p>
 * CompileGraph validates the declared set of deployed graph models once at startup:
 * <p>
 * 1. Structural validation - every node/connection is imported once via MiniGraph.importGraph(),
 *    which catches missing/duplicate alias, invalid types, and dangling connections early.
 * 2. Syntax conversion - the deprecated "simple type matching" syntax (model.someKey:type) found in
 *    "mapping", "input", "output" and "for_each" node properties is converted to the equivalent
 *    "simple plugin" syntax (f:type(model.someKey)) once, instead of being resolved on every node
 *    execution of every request. A mapping/output/for_each entry without "-&gt;" rejects the
 *    graph (it is guaranteed to fail at runtime); an "input" entry without "-&gt;" is skill
 *    vocabulary (e.g. the fetcher's dictionary parameter names) and passes through.
 * 3. Discovery contract and completeness - every deployable graph must document itself
 *    (the root node needs a non-empty 'purpose' property - what "list graphs" shows as
 *    living documentation) and must have an 'end' node so every run can complete.
 * 4. Suspend/resume contract - the static half of the workflow-suspension rules (reserved
 *    'suspend' alias bound to the 'graph.suspend' skill, no suspension on routing skills, the
 *    drawn checkpoint edge, mandatory 'ttl', a 'task' route on suspend/resume nodes); the
 *    runtime guards remain the enforcement floor for graphs not in the manifest.
 * <p>
 * CompileGraph is the deployment gate: set "graph.model.automation" to a YAML file listing the
 * graph IDs to compile at startup (mirroring "yaml.flow.automation" for event flows). Like
 * flows.yaml, the manifest carries the location of its own models in an optional "location"
 * entry (file:/ or classpath:/, default "classpath:/graph") - there is no separate
 * application.properties key. A deployed
 * graph model is executable ONLY when it is listed in the manifest and passes this gate - a
 * graph that fails, or is not listed, answers HTTP-404 as if it does not exist. This is the
 * CompileFlows precedent: an invalid flow never becomes executable, and there is no lazy
 * loading of unvalidated models.
 * Ad-hoc graphs created interactively through the dev playground are intentionally out of scope since
 * they are not known ahead of time (the playground dry-run runs from its own temp workspace).
 */
@BeforeApplication(sequence = 6)
public class CompileGraph implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(CompileGraph.class);
    private static final SimpleTypeMatchingConverter converter = SimpleTypeMatchingConverter.getInstance();
    private static final Utility util = Utility.getInstance();
    private static final String INPUT = "input";
    private static final String[] MAPPING_PROPERTIES = {"mapping", INPUT, "output", "for_each"};
    private static final String MAP_TO = "->";
    private static final String JSON_EXT = ".json";
    private static final String GRAPHS = "graphs";
    private static final String NODES = "nodes";
    private static final String PROPERTIES_SUFFIX = "].properties.";
    private static final String NODE_NAME = "node ";
    private static final String LOCATION = "location";
    private static final String DEFAULT_DEPLOY_DIR = "classpath:/graph";
    private static final String FILE_PREFIX = "file:/";
    private static final String CLASSPATH_PREFIX = "classpath:/";

    @Override
    public void start(String[] args) {
        AppConfigReader config = AppConfigReader.getInstance();
        if (!config.getProperty("location.graph.deployed", "").isBlank()) {
            log.warn("location.graph.deployed is obsolete - " +
                    "set 'location' in the graph manifest (graph.model.automation) instead");
        }
        String manifest = config.getProperty("graph.model.automation", "");
        if (manifest.isBlank()) {
            log.warn("No graph manifest configured (graph.model.automation) - " +
                    "no deployed graph models will be executable");
            return;
        }
        try {
            var reader = new ConfigReader(manifest);
            // like flows.yaml, the manifest carries the location of its own models
            var deployLocation = reader.getProperty(LOCATION, DEFAULT_DEPLOY_DIR);
            if (!deployLocation.startsWith(FILE_PREFIX) && !deployLocation.startsWith(CLASSPATH_PREFIX)) {
                log.warn("Graph manifest 'location' must start with file:/ or classpath:/. Fallback to {}",
                        DEFAULT_DEPLOY_DIR);
                deployLocation = DEFAULT_DEPLOY_DIR;
            }
            CompiledGraphs.setDeployedLocation(deployLocation);
            log.info("Deployed graph model folder - {}", deployLocation);
            Object allGraphs = reader.get(GRAPHS);
            if (allGraphs instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    var graphId = reader.getProperty(GRAPHS + "[" + i + "]");
                    compileOneGraph(deployLocation, graphId);
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unable to load graph manifest {} - {}", manifest, e.getMessage());
        }
        log.info("Graph models compiled: {}", CompiledGraphs.getAllGraphs().size());
    }

    private void compileOneGraph(String deployLocation, String graphId) {
        try {
            var reader = new ConfigReader(getNormalizedPath(deployLocation, graphId));
            Map<String, Object> model = reader.getMap();
            convertDataMappingEntries(graphId, model);
            // structural validation - throws IllegalArgumentException for a malformed graph
            var graph = new MiniGraph();
            graph.importGraph(model);
            // discovery contract: every deployable graph documents itself - the root
            // node's 'purpose' is what "list graphs" shows as living documentation
            if (!hasRootPurpose(model)) {
                throw new IllegalArgumentException("root node must define a non-empty 'purpose' property");
            }
            // every run must be able to complete - GraphExecutor trusts this at runtime
            if (graph.getEndNode() == null) {
                throw new IllegalArgumentException("graph must have an 'end' node");
            }
            GraphModelValidator.validate(graph);
            CompiledGraphs.addGraph(graphId, model);
            log.info("Compiled graph {}", graphId);
        } catch (IllegalArgumentException e) {
            // a rejected graph is simply not registered: deployed execution is served
            // exclusively from the compiled registry, so requests to it answer 404
            log.error("Rejected graph {} - {}", graphId, e.getMessage());
        }
    }

    private boolean hasRootPurpose(Map<String, Object> model) {
        if (model.get(NODES) instanceof List<?> nodes) {
            for (var n : nodes) {
                if (n instanceof Map<?, ?> node && "root".equals(node.get("alias"))) {
                    return node.get("properties") instanceof Map<?, ?> properties
                            && properties.get("purpose") instanceof String purpose && !purpose.isBlank();
                }
            }
        }
        return false;
    }

    private void convertDataMappingEntries(String graphId, Map<String, Object> model) {
        var mm = new MultiLevelMap(model);
        Object nodeList = model.get(NODES);
        if (nodeList instanceof List<?> nodes) {
            for (int i = 0; i < nodes.size(); i++) {
                for (String key : MAPPING_PROPERTIES) {
                    var path = NODES + "[" + i + PROPERTIES_SUFFIX + key;
                    if (mm.getElement(path) instanceof List<?> entries) {
                        mm.setElement(path, convertEntries(graphId, i, key, entries));
                    }
                }
            }
        }
    }

    private List<String> convertEntries(String graphId, int nodeIndex, String property, List<?> entries) {
        List<String> converted = new ArrayList<>();
        for (Object o : entries) {
            var line = String.valueOf(o);
            if (line.contains(MAP_TO)) {
                var convertedLine = converter.convert(line);
                if (!convertedLine.equals(line)) {
                    log.warn("Deprecated syntax in graph {} node[{}].{} - '{}' converted to '{}'",
                            graphId, nodeIndex, property, line, convertedLine);
                }
                converted.add(convertedLine);
            } else if (INPUT.equals(property)) {
                // an 'input' entry without '->' is skill vocabulary, not a data mapping -
                // e.g. the fetcher's dictionary parameter names and feature flags
                converted.add(line);
            } else {
                // a mapping/for_each/output entry is always a data mapping: a line
                // without '->' is guaranteed to fail at runtime, so reject the graph
                // (this class is a quality gate - a compiled graph must be runnable)
                throw new IllegalArgumentException(NODE_NAME + "[" + nodeIndex + "]." + property +
                        " - missing '" + MAP_TO + "' in '" + line + "'");
            }
        }
        return converted;
    }

    private String getNormalizedPath(String folder, String graphId) {
        var sb = new StringBuilder();
        for (String part : util.split(folder, "/")) {
            sb.append('/').append(part);
        }
        sb.append('/').append(graphId).append(JSON_EXT);
        return sb.substring(1);
    }
}
