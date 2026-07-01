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
 * Today, GraphExecutor loads and parses a graph model's JSON file fresh on every request, and every
 * skill re-interprets each data-mapping string (LHS -&gt; RHS) on every node execution with no
 * upfront validation. CompileGraph optionally validates a declared set of graph models once at
 * startup instead:
 * <p>
 * 1. Structural validation - every node/connection is imported once via MiniGraph.importGraph(),
 *    which catches missing/duplicate alias, invalid types, and dangling connections early.
 * 2. Syntax conversion - the deprecated "simple type matching" syntax (model.someKey:type) found in
 *    "mapping", "input", "output" and "for_each" node properties is converted to the equivalent
 *    "simple plugin" syntax (f:type(model.someKey)) once, instead of being resolved on every node
 *    execution of every request.
 * <p>
 * CompileGraph is opt-in: set "graph.model.automation" to a YAML file listing the graph IDs to
 * compile at startup (mirroring "yaml.flow.automation" for event flows). Graph IDs not listed in the
 * manifest continue to be loaded lazily by GraphExecutor exactly as before, so this is purely additive.
 * Ad-hoc graphs created interactively through the dev playground are intentionally out of scope since
 * they are not known ahead of time.
 */
@BeforeApplication(sequence = 6)
public class CompileGraph implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(CompileGraph.class);
    private static final SimpleTypeMatchingConverter converter = SimpleTypeMatchingConverter.getInstance();
    private static final Utility util = Utility.getInstance();
    private static final String[] MAPPING_PROPERTIES = {"mapping", "input", "output", "for_each"};
    private static final String MAP_TO = "->";
    private static final String JSON_EXT = ".json";
    private static final String GRAPHS = "graphs";
    private static final String NODES = "nodes";
    private static final String PROPERTIES_SUFFIX = "].properties.";

    @Override
    public void start(String[] args) {
        AppConfigReader config = AppConfigReader.getInstance();
        String manifest = config.getProperty("graph.model.automation", "");
        if (manifest.isBlank()) {
            log.info("No graph manifest configured (graph.model.automation) - skipping graph compilation");
            return;
        }
        String deployLocation = config.getProperty("location.graph.deployed", "classpath:/graph");
        try {
            var reader = new ConfigReader(manifest);
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
            new MiniGraph().importGraph(model);
            CompiledGraphs.addGraph(graphId, model);
            log.info("Compiled graph {}", graphId);
        } catch (IllegalArgumentException e) {
            log.error("Skip invalid graph {} - {}", graphId, e.getMessage());
        }
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
                converted.add(converter.convert(line));
            } else {
                log.error("Invalid data mapping in graph {} node[{}].{} - missing '->' in '{}'",
                        graphId, nodeIndex, property, line);
                converted.add(line);
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
