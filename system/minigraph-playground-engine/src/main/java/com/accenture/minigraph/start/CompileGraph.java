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

import com.accenture.minigraph.common.GraphModelGate;
import com.accenture.minigraph.models.CompiledGraphs;
import org.platformlambda.core.annotations.BeforeApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * The four checks read the model alone, so they live in {@link GraphModelGate}, which the graph packager
 * shares: a graph set is refused at pack time for the reasons this gate would reject it at startup.
 * <p>
 * CompileGraph is the deployment gate: set "graph.model.automation" to a YAML manifest - or, since
 * 4.12.19, a comma-separated list of manifests, each with its own 'location', where the later manifest
 * wins for a duplicate graph id - listing the
 * graph IDs to compile at startup (mirroring "yaml.flow.automation" for event flows). Like
 * flows.yaml, the manifest carries the location of its own models in an optional "location"
 * entry (file:/ or classpath:/, default "classpath:/graph") - there is no separate
 * application.properties key. A deployed
 * graph model is executable ONLY when it is listed in the manifest and passes this gate - a
 * graph that fails, or is not listed, answers HTTP-404 as if it does not exist. This is the
 * CompileFlows precedent: an invalid flow never becomes executable, and there is no lazy
 * loading of unvalidated models.
 * <p>
 * A manifest may also list packaged graph sets (ADR-0027): 'sets' names packages read from its location as
 * '{set}.pack', and 'unpack' names a file:/ folder the application can write. GraphSetLoader unpacks each set
 * there and deploys it all or none, right after the manifest's own graphs.
 * Ad-hoc graphs created interactively through the dev playground are intentionally out of scope since
 * they are not known ahead of time (the playground dry-run runs from its own temp workspace).
 */
@BeforeApplication(sequence = 6)
public class CompileGraph implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(CompileGraph.class);
    private static final Utility util = Utility.getInstance();
    private static final String JSON_EXT = ".json";
    private static final String GRAPHS = "graphs";
    private static final String LOCATION = "location";
    private static final String DEFAULT_DEPLOY_DIR = "classpath:/graph";
    private static final String FILE_PREFIX = "file:/";
    private static final String CLASSPATH_PREFIX = "classpath:/";

    @Override
    public void start(String[] args) {
        GraphSetLoader.reset();
        AppConfigReader config = AppConfigReader.getInstance();
        if (!config.getProperty("location.graph.deployed", "").isBlank()) {
            log.warn("location.graph.deployed is obsolete - " +
                    "set 'location' in the graph manifest (graph.model.automation) instead");
        }
        String manifests = config.getProperty("graph.model.automation", "");
        if (manifests.isBlank()) {
            log.warn("No graph manifest configured (graph.model.automation) - " +
                    "no deployed graph models will be executable");
            return;
        }
        // Since 4.12.19 the property may name several manifests, comma-separated (the
        // yaml.flow.automation convention): the one bundled in the artifact and, for rapid
        // prototyping, an external one - each carries its own 'location'. Manifests compile
        // in the order listed, and a manifest that cannot be loaded is skipped with a warning
        // so the others still compile.
        for (String manifest : util.split(manifests, ", ")) {
            compileManifest(manifest);
        }
        log.info("Graph models compiled: {}", CompiledGraphs.getAllGraphs().size());
    }

    /**
     * Compile one manifest: its loose graphs, then its packaged sets (ADR-0027).
     *
     * @param manifest the manifest's path (file:/ or classpath:/)
     */
    public void compileManifest(String manifest) {
        try {
            var reader = new ConfigReader(manifest);
            log.info("Loading graph manifest {}", manifest);
            // like flows.yaml, the manifest carries the location of its own models
            var deployLocation = reader.getProperty(LOCATION, DEFAULT_DEPLOY_DIR);
            if (!deployLocation.startsWith(FILE_PREFIX) && !deployLocation.startsWith(CLASSPATH_PREFIX)) {
                log.warn("Graph manifest 'location' must start with file:/ or classpath:/. Fallback to {}",
                        DEFAULT_DEPLOY_DIR);
                deployLocation = DEFAULT_DEPLOY_DIR;
            }
            CompiledGraphs.addDeployedLocation(deployLocation);
            log.info("Deployed graph model folder - {}", deployLocation);
            Object allGraphs = reader.get(GRAPHS);
            if (allGraphs instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    var graphId = reader.getProperty(GRAPHS + "[" + i + "]");
                    compileOneGraph(deployLocation, graphId);
                }
            }
            // a manifest's sets compile right after its own graphs, before the next manifest
            GraphSetLoader.deploy(manifest, reader, deployLocation);
        } catch (IllegalArgumentException e) {
            log.warn("Unable to load graph manifest {} - {}", manifest, e.getMessage());
        }
    }

    private void compileOneGraph(String deployLocation, String graphId) {
        // later manifest wins: when a later manifest lists a graph id again, that manifest owns
        // the id - its copy replaces the earlier one, and if the new copy is rejected the id is
        // not executable (404) rather than silently served from the copy the operator meant to
        // replace (a curl test would otherwise pass against the old behavior)
        var previous = CompiledGraphs.getGraphLocation(graphId);
        if (previous != null && !previous.equals(deployLocation)) {
            var previousSet = CompiledGraphs.getGraphSet(graphId);
            if (previousSet == null) {
                log.warn("Graph {} from {} replaces the copy from {}", graphId, deployLocation, previous);
            } else if (log.isErrorEnabled()) {
                // a duplicate that involves a graph set is an error, the later copy still wins (ADR-0027)
                log.error("Graph {} from {} replaces the copy from {}", graphId, deployLocation,
                        GraphSetLoader.describe(previous, previousSet));
            }
            CompiledGraphs.removeGraph(graphId);
        }
        try {
            var reader = new ConfigReader(getNormalizedPath(deployLocation, graphId));
            Map<String, Object> model = reader.getMap();
            // the gate's static checks, shared with the graph packager - throws IllegalArgumentException
            // for a rejected model and converts deprecated mapping syntax in place
            GraphModelGate.validate(graphId, model);
            CompiledGraphs.addGraph(graphId, model, deployLocation);
            log.info("Compiled graph {}", graphId);
        } catch (IllegalArgumentException e) {
            // a rejected graph is simply not registered: deployed execution is served
            // exclusively from the compiled registry, so requests to it answer 404
            log.error("Rejected graph {} - {}", graphId, e.getMessage());
        }
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
