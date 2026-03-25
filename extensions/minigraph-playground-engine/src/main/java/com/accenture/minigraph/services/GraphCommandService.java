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
import com.jayway.jsonpath.InvalidPathException;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleConnection;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@OptionalService("app.env=dev")
@PreLoad(route = GraphCommandService.ROUTE, instances=50)
public class GraphCommandService extends GraphLambdaFunction {
    public static final String ROUTE = "graph.command.service";
    private static final Logger log = LoggerFactory.getLogger(GraphCommandService.class);
    private static final String DEFAULT_TEMP_DIR = "/tmp/graph";
    private static final String DEFAULT_DEPLOY_DIR = "classpath:/graph";
    private static final String PLAYGROUND = "playground";
    private static final String INVALID_GRAPH_NAME = "Invalid filename - must be a-z, A-Z, 0-9 with optional hyphen";
    private static final long MAX_BUFFER_SIZE = 62 * 1024L;
    private final AtomicInteger counter = new AtomicInteger();
    private final File tempDir;
    private final String deployedGraphLocation;

    public GraphCommandService() {
        var config = AppConfigReader.getInstance();
        // load temp graph location
        var location = config.getProperty("location.graph.temp", DEFAULT_TEMP_DIR);
        if (location.startsWith(CLASSPATH_PREFIX)) {
            log.error("location.graph.temp must use local file system because of read/write requirements");
            log.error("location.graph.temp fallback to {}", DEFAULT_TEMP_DIR);
            location = DEFAULT_TEMP_DIR;
        }
        if (location.startsWith(FILE_PREFIX)) {
            location = location.substring(FILE_PREFIX.length());
        }
        if (location.contains(":")) {
            log.error("File path in location.graph.temp is invalid. Fallback to {}", DEFAULT_TEMP_DIR);
            location = DEFAULT_TEMP_DIR;
        }
        var parts = Utility.getInstance().split(location, "/");
        if (parts.size() < 2) {
            log.error("location.graph.temp must not use root. Fallback to {}", DEFAULT_TEMP_DIR);
            location = DEFAULT_TEMP_DIR;
        }
        this.tempDir = new File(location);
        if (!this.tempDir.exists()) {
            boolean created = this.tempDir.mkdirs();
            if (created) {
                log.info("Created temp folder {}", location);
            }
        }
        log.info("Playground temp folder (location.graph.temp) - {}", location);
        // load deploy graph location
        var deployLocation = config.getProperty("location.graph.deployed", DEFAULT_DEPLOY_DIR);
        if (deployLocation.startsWith(FILE_PREFIX) || deployLocation.startsWith(CLASSPATH_PREFIX)) {
            this.deployedGraphLocation = deployLocation;
        } else {
            log.error("location.graph.temp must start with file:/ or classpath:/. Fallback to {}", DEFAULT_DEPLOY_DIR);
            this.deployedGraphLocation = DEFAULT_DEPLOY_DIR;
        }
        log.info("Deployed graph model folder (location.graph.deployed) - {}", this.deployedGraphLocation);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        if (input.getBody() instanceof Map) {
            var po = new PostOffice(headers, instance);
            var data = (Map<String, Object>) input.getBody();
            try {
                handleCommand(po, data);
            } catch (IllegalArgumentException | IOException | InvalidPathException e) {
                if (data.get(OUT) instanceof String outRoute) {
                    po.send(new EventEnvelope().setTo(outRoute).setBody("ERROR: " + e.getMessage()));
                }
            }
        }
        return null;
    }

    private void handleCommand(PostOffice po, Map<String, Object> input)
            throws IOException {
        var type = input.get(TYPE);
        var in = input.get(IN);
        var out = input.get(OUT);
        var message = input.get(MESSAGE);
        if (OPEN.equals(type) && in instanceof String inRoute) {
            graphModels.put(inRoute, new MiniGraph());
        }
        if (CLOSE.equals(type) && in instanceof String inRoute) {
            graphModels.remove(inRoute);
            graphInstances.remove(inRoute);
            var filename = getTempGraphName(inRoute);
            var file = new File(tempDir, filename + JSON_EXT);
            if (file.exists()) {
                // delete draft graph model
                Files.delete(file.toPath());
            }
        }
        if (COMMAND.equals(type) && in instanceof String inRoute && out instanceof String outRoute &&
                message instanceof String text) {
            var command = text.trim();
            if (command.startsWith("{") && command.endsWith("}")) {
                handleJsonCommand(po, outRoute, command);
            } else if (command.contains("\n")) {
                handleMultiLineCommand(po, inRoute, outRoute, command, false);
            } else {
                handleSingleLineCommand(po, inRoute, outRoute, command);
            }
        }
    }

    private void handleSingleLineCommand(PostOffice po, String inRoute, String outRoute, String command)
            throws IOException {
        po.send(new EventEnvelope().setTo(outRoute).setBody("> " + command));
        var words = getWords(command);
        if (!words.isEmpty() && words.getFirst().equalsIgnoreCase(HELP)) {
            var helpText = getHelp(words);
            po.send(new EventEnvelope().setTo(outRoute).setBody(
                    Objects.requireNonNullElseGet(helpText, () -> "'" + command + "'"+NOT_FOUND)));
        } else if (words.size() > 1 && (words.getFirst().equalsIgnoreCase("create") ||
                    words.getFirst().equalsIgnoreCase(INSTANTIATE))) {
            // handle create command without type and properties OR instantiate without mock data
            handleMultiLineCommand(po, inRoute, outRoute, command, true);
        } else if (words.size() > 1 && words.getFirst().equalsIgnoreCase("describe")) {
            handleDescribeCommand(po, inRoute, outRoute, words);
        } else if (words.size() > 1 && words.getFirst().equalsIgnoreCase(EXECUTE)) {
            handleExecuteCommand(po, inRoute, outRoute, words);
        } else if (words.size() == 2 && words.getFirst().equalsIgnoreCase(INSPECT)) {
            handleInspectCommand(po, inRoute, outRoute, words.get(1));
        } else if (words.size() == 1 && words.getFirst().equalsIgnoreCase(RUN)) {
            handleRunCommand(inRoute, outRoute);
        } else {
            handleCommandPartTwo(po, inRoute, outRoute, words);
        }
    }

    private List<String> getWords(String command) {
        var words = util.split(command, " ");
        // handle aliases if any
        if (!words.isEmpty()) {
            if (words.getFirst().equalsIgnoreCase(START)) {
                words.set(0, INSTANTIATE);
            }
            if (words.getFirst().equalsIgnoreCase(CLEAR)) {
                words.set(0, DELETE);
            }
            if (words.size() > 1 && words.getFirst().equalsIgnoreCase(HELP)) {
                if (words.get(1).equalsIgnoreCase(START)) {
                    words.set(1, INSTANTIATE);
                }
                if (words.get(1).equalsIgnoreCase(CLEAR)) {
                    words.set(1, DELETE);
                }
            }
        }
        return words;
    }

    private void handleUploadMockCommand(PostOffice po, String inRoute, String outRoute) {
        var graphInstance = getGraphInstance(inRoute);
        if (graphInstance != null) {
            var name = getTempGraphName(inRoute);
            po.send(new EventEnvelope().setTo(outRoute).setBody("You may upload JSON payload -> POST /api/mock/" + name));
        }
    }

    public static boolean uploadContent(String id, Object content) {
        var route = id.replace('-', '.');
        var inRoute = route + ".in";
        var outRoute = route + ".out";
        var instance = graphInstances.get(inRoute);
        if (instance == null) {
            return false;
        } else {
            var stateMachine = instance.stateMachine;
            stateMachine.setElement(INPUT_BODY_NAMESPACE, content);
            var po = EventEmitter.getInstance();
            po.send(outRoute, "Mock data loaded into 'input.body' namespace");
            return true;
        }
    }

    public static Object downloadContent(String id, String key) {
        var route = id.replace('-', '.');
        var inRoute = route + ".in";
        var instance = graphInstances.get(inRoute);
        if (instance != null) {
            var stateMachine = instance.stateMachine;
            return stateMachine.getElement(key);
        } else {
            return null;
        }
    }

    private void handleRunCommand(String inRoute, String outRoute) {
        var cid = util.getUuid();
        var po = PostOffice.trackable("minigraph.playground", cid, "/graph/playground");
        po.send(new EventEnvelope().setTo(GraphTraveler.ROUTE).setHeader(IN, inRoute)
                .setReplyTo(outRoute).setCorrelationId(cid));

//        po.eRequest(new EventEnvelope().setTo(GraphTraveler.ROUTE)
//                        .setHeader(IN, inRoute).setHeader(OUT, outRoute), timeout)
//                .thenAccept(response -> {
//                    if (response.hasError()) {
//                        po.send(new EventEnvelope().setTo(outRoute).setBody(response.getBody()));
//                    } else {
//                        po.send(new EventEnvelope().setTo(outRoute).setBody(
//                                "Knowledge graph executed in " + response.getExecutionTime() + " ms"));
//                    }
//                });
    }

    private void handleCommandPartTwo(PostOffice po, String inRoute, String outRoute, List<String> words) {
        if (words.size() > 2 && words.getFirst().equalsIgnoreCase("connect")) {
            handleConnectCommand(po, inRoute, outRoute, words);
        } else if (words.size() > 1 && words.getFirst().equalsIgnoreCase(DELETE)) {
            handleDeleteCommand(po, inRoute, outRoute, words);
        } else if (words.size() == 4 && words.getFirst().equalsIgnoreCase("export") &&
                words.get(1).equalsIgnoreCase(GRAPH) &&
                words.get(2).equalsIgnoreCase("as")) {
            handleExportCommand(po, inRoute, outRoute, words.get(3));
        } else if (words.size() == 4 && words.getFirst().equalsIgnoreCase("import") &&
                words.get(1).equalsIgnoreCase(GRAPH) &&
                words.get(2).equalsIgnoreCase("from")) {
            handleImportGraphCommand(po, inRoute, outRoute, words.get(3));
        } else if (words.size() == 5 && words.getFirst().equalsIgnoreCase("import") &&
                words.get(1).equalsIgnoreCase(NODE) &&
                words.get(3).equalsIgnoreCase("from")) {
            handleImportNodeCommand(po, inRoute, outRoute, words.get(2), words.get(4));
        } else if (words.size() == 3 && words.getFirst().equalsIgnoreCase("edit") &&
                words.get(1).equalsIgnoreCase(NODE)) {
            handleEditCommand(po, inRoute, outRoute, words.get(2));
        } else if (words.size() == 2 && words.getFirst().equalsIgnoreCase("list")) {
            handleListCommand(po, inRoute, outRoute, words.get(1));
        } else {
            handleCommandPartThree(po, inRoute, outRoute, words);
        }
    }

    private void handleCommandPartThree(PostOffice po, String inRoute, String outRoute, List<String> words) {
        if (words.size() == 3 && words.getFirst().equalsIgnoreCase("upload") &&
                words.get(1).equalsIgnoreCase("mock") &&
                words.get(2).equalsIgnoreCase("data")) {
            handleUploadMockCommand(po, inRoute, outRoute);
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody(TRY_HELP));
        }
    }

    private void handleListCommand(PostOffice po, String inRoute, String outRoute, String type) {
        var graph = graphModels.get(inRoute);
        var sb = new StringBuilder();
        if ("nodes".equalsIgnoreCase(type)) {
            listNodes(graph, sb);
        } else if ("connections".equalsIgnoreCase(type)) {
            listConnections(graph, sb);
        } else {
            sb.append("Please use 'list nodes' or 'list connections'");
        }
        po.send(new EventEnvelope().setTo(outRoute).setBody(sb.toString()));
    }

    private void listNodes(MiniGraph graph, StringBuilder sb) {
        var nodes = graph.getNodes();
        if (nodes.isEmpty()) {
            sb.append("There are no nodes in this graph");
        } else {
            var root = graph.getRootNode();
            var end = graph.getEndNode();
            sb.append(root == null ? "root (does not exist)" : "root "+root.getTypes()).append("\n");
            var listing = getNodeListing(nodes, root, end);
            if (!listing.isEmpty()) {
                Collections.sort(listing);
                for (var name : listing) {
                    sb.append(name).append("\n");
                }
            }
            sb.append(end == null ? "end (does not exist)" : "end "+end.getTypes()).append("\n");
        }
    }

    private List<String> getNodeListing(List<SimpleNode> nodes, SimpleNode root, SimpleNode end) {
        var listing = new ArrayList<String>();
        for (var node : nodes) {
            if ((root != null && node.getId().equals(root.getId()) ||
                    (end != null && node.getId().equals(end.getId())))) {
                continue;
            }
            listing.add(node.getAlias() + " " + node.getTypes());
        }
        return listing;
    }

    private void listConnections(MiniGraph graph, StringBuilder sb) {
        var connections = graph.getConnections();
        if (connections.isEmpty()) {
            sb.append("There are no connections in this graph");
        } else {
            var rootList = new ArrayList<String>();
            var endList = new ArrayList<String>();
            var regularList = new ArrayList<String>();
            for (var connection : connections) {
                sortConnections(connection, rootList, regularList, endList);
            }
            Collections.sort(rootList);
            Collections.sort(regularList);
            Collections.sort(endList);
            for (var connection : rootList) {
                sb.append(connection).append("\n");
            }
            for (var connection : regularList) {
                sb.append(connection).append("\n");
            }
            for (var connection : endList) {
                sb.append(connection).append("\n");
            }
        }
    }

    private void sortConnections(SimpleConnection connection, List<String> rootList, List<String> regularList,
                                 List<String> endList) {
        var relations = connection.getRelations();
        var source = connection.getSource().getAlias();
        var target = connection.getTarget().getAlias();
        if (relations.isEmpty()) {
            var line = source + " --> " + target;
            if (ROOT.equalsIgnoreCase(source)) {
                rootList.add(line);
            } else if (END.equalsIgnoreCase(target)) {
                endList.add(line);
            } else {
                regularList.add(line);
            }
        } else {
            var relationships = new ArrayList<String>();
            relations.forEach(r -> relationships.add(r.getType()));
            var line = source + " -" + relationships + "-> " + target;
            if (ROOT.equalsIgnoreCase(source)) {
                rootList.add(line);
            } else if (END.equalsIgnoreCase(target)) {
                endList.add(line);
            } else {
                regularList.add(line);
            }
        }
    }

    private void handleEditCommand(PostOffice po, String inRoute, String outRoute, String nodeName) {
        var graph = graphModels.get(inRoute);
        var node = graph.findNodeByAlias(nodeName);
        if (node == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + NOT_FOUND);
        }
        var sb = new StringBuilder();
        sb.append("update node ").append(nodeName).append('\n');
        var types = node.getTypes();
        var type = types.isEmpty() ? UNTYPED : types.iterator().next();
        sb.append("with type ").append(type).append('\n');
        sb.append("with properties\n");
        var properties = node.getProperties();
        if (!properties.isEmpty()) {
            sb.append(getRawProperties(properties));
        }
        po.send(new EventEnvelope().setTo(outRoute).setBody(sb.toString()));
    }

    private String getRawProperties(Map<String, Object> properties) {
        var sb = new StringBuilder();
        var map = util.getFlatMap(properties);
        var items = new ArrayList<>(map.keySet());
        Collections.sort(items);
        // zero fill index keys to 3 digits to guarantee correct sorting order, thus [1] = [001]
        var nMap = new HashMap<String, Object>();
        for (var key : items) {
            var fixedSizeIndexKey = normalizeKey(key, true);
            nMap.put(fixedSizeIndexKey, map.get(key));
        }
        var nItems = new ArrayList<>(nMap.keySet());
        Collections.sort(nItems);
        for (var key : nItems) {
            var value = nMap.get(key);
            var lines = util.split(String.valueOf(value), "\n");
            if (!lines.isEmpty()) {
                sb.append(normalizeKey(key, false)).append("=");
                if (lines.size() == 1) {
                    sb.append(lines.getFirst()).append('\n');
                } else {
                    sb.append("'''\n");
                    for (var line : lines) {
                        sb.append(line).append('\n');
                    }
                    sb.append("'''\n");
                }
            }
        }
        return sb.toString();
    }

    private String normalizeKey(String key, boolean sequence) {
        if (key.contains("[") && key.contains("]")) {
            var segments = util.extractSegments(key, "[", "]");
            if (segments.size() == 1) {
                var first = segments.getFirst();
                if (sequence) {
                    var index = util.str2int(key.substring(first.start()+1, first.end()-1));
                    if (index != -1) {
                        var part1 = key.substring(0, first.start()+1);
                        var n = util.zeroFill(index, 999);
                        var part2 = key.substring(first.end()-1);
                        return part1 + n + part2;
                    } else {
                        return key;
                    }
                } else {
                    return key.substring(0, first.start() + 1) + key.substring(first.end()-1);
                }
            }
        }
        return key;
    }

    private void handleInspectCommand(PostOffice po, String inRoute, String outRoute, String key) {
        var stateMachine = getGraphInstance(inRoute).stateMachine;
        var value = stateMachine.getElement(key, false);
        if (value instanceof Map || value instanceof List) {
            var text = SimpleMapper.getInstance().getMapper().writeValueAsString(value);
            if (text.length() > MAX_BUFFER_SIZE) {
                var name = getTempGraphName(inRoute);
                po.send(new EventEnvelope().setTo(outRoute).setBody(
                        "Large payload (" + text.length() +") -> GET /api/inspect/"+ name+"/"+key));
            } else {
                po.send(new EventEnvelope().setTo(outRoute).setBody(Map.of(INSPECT, key, "outcome", value)));
            }
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody(Map.of(INSPECT, key, "outcome", value)));
        }
    }

    private void handleExecuteCommand(PostOffice po, String inRoute, String outRoute, List<String> words) {
        final String nodeName;
        if (words.size() == 2) {
            nodeName = words.get(1);
        } else if (words.size() == 3 && NODE.equalsIgnoreCase(words.get(1))) {
            nodeName = words.get(2);
        } else {
            throw new IllegalArgumentException("Invalid command. Please try EXECUTE NODE {name} or EXECUTE {node-name}");
        }
        var graphInstance = getGraphInstance(inRoute);
        var graph = graphInstance.graph;
        var timeout = getModelTtl(graphInstance);
        var node = graph.findNodeByAlias(nodeName);
        if (node == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + NOT_FOUND);
        }
        var skill = node.getProperty(SKILL);
        if (skill == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have a skill property");
        }
        var skillRoute = String.valueOf(skill);
        if (po.exists(skillRoute)) {
            po.eRequest(new EventEnvelope().setTo(skillRoute)
                            .setHeader(IN, inRoute).setHeader(TYPE, EXECUTE).setHeader(NODE, nodeName), timeout)
                    .thenAccept(response -> {
                        if (response.hasError()) {
                            po.send(new EventEnvelope().setTo(outRoute).setBody(response.getBody()));
                        } else {
                            po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName +
                                    " run for " + response.getExecutionTime() +
                                    " ms with exit path '" + response.getBody() + "'"));
                        }
                    });
        } else {
            throw new IllegalArgumentException(NODE_NAME+" is invalid - Skill '"+skill+"' does not exist");
        }
    }

    private void handleImportNodeCommand(PostOffice po, String inRoute, String outRoute, String nodeName, String filename) {
        if (validGraphFileName(filename)) {
            var graph = graphModels.get(inRoute);
            if (graph != null) {
                var file = new File(tempDir, filename+JSON_EXT);
                if (file.exists()) {
                    var json = util.file2str(file);
                    findNodeFromAnotherGraph(po, graph, outRoute, nodeName, filename, json);
                }
            }
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody(INVALID_GRAPH_NAME));
        }
    }

    @SuppressWarnings("unchecked")
    private void findNodeFromAnotherGraph(PostOffice po, MiniGraph graph, String outRoute, String nodeName,
                                          String filename, String json) {
        var map = SimpleMapper.getInstance().getMapper().readValue(json, Map.class);
        var anotherGraph = new MiniGraph();
        anotherGraph.importGraph(map);
        var anotherNode = anotherGraph.findNodeByAlias(nodeName);
        if (anotherNode == null) {
            po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName +
                    " does not exist in "+filename));
        } else {
            if (updateNodeFromAnotherGraph(graph, nodeName, anotherNode)) {
                po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName +
                        " overwritten by node from "+filename));
            } else {
                po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName +
                        " imported from "+filename));
            }
        }
    }

    private boolean updateNodeFromAnotherGraph(MiniGraph graph, String nodeName, SimpleNode anotherNode) {
        boolean overwritten = false;
        var types = anotherNode.getTypes();
        var type = types.isEmpty() ? UNTYPED : types.iterator().next();
        var node = graph.findNodeByAlias(nodeName);
        if (node != null) {
            node.getTypes().clear();
            node.addType(type);
            node.getProperties().clear();
            overwritten = true;
        } else {
            node = graph.createNode(anotherNode.getAlias(), type);
        }
        for (var kv : anotherNode.getProperties().entrySet()) {
            node.addProperty(kv.getKey(), kv.getValue());
        }
        return overwritten;
    }

    private void handleImportGraphCommand(PostOffice po, String inRoute, String outRoute, String filename) {
        if (validGraphFileName(filename)) {
            var graph = graphModels.get(inRoute);
            if (graph != null) {
                var file = new File(tempDir, filename+JSON_EXT);
                if (file.exists()) {
                    importGraphAsDraft(po, inRoute, outRoute, util.file2str(file));
                } else {
                    po.send(new EventEnvelope().setTo(outRoute).setBody("Graph model not found in "+file.getPath()));
                    var json = getDeployedGraphAsText(filename);
                    if (json != null) {
                        po.send(new EventEnvelope().setTo(outRoute).setBody("Found deployed graph model in " +
                                        deployedGraphLocation +
                                "\nPlease export an updated version and re-import to instantiate an instance model"));
                        importGraphAsDraft(po, inRoute, outRoute, json);
                    }
                }
            }
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody(INVALID_GRAPH_NAME));
        }
    }

    private String getDeployedGraphAsText(String filename) {
        var filePath = getNormalizedPath(deployedGraphLocation, filename);
        if (filePath.startsWith(CLASSPATH_PREFIX)) {
            var path = filePath.substring(CLASSPATH_PREFIX.length());
            InputStream in = this.getClass().getResourceAsStream(path);
            if (in != null) {
                return util.stream2str(in);
            }
        }
        if (filePath.startsWith(FILE_PREFIX)) {
            File f = new File(filePath.substring(FILE_PREFIX.length()));
            if (f.exists()) {
                return util.file2str(f);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void importGraphAsDraft(PostOffice po, String inRoute, String outRoute, String json) {
        var graph = graphModels.get(inRoute);
        var map = SimpleMapper.getInstance().getMapper().readValue(json, Map.class);
        graph.importGraph(map);
        if (graphInstances.containsKey(inRoute)) {
            po.send(new EventEnvelope().setTo(outRoute).setBody("Graph instance cleared"));
            graphInstances.remove(inRoute);
        }
        po.send(new EventEnvelope().setTo(outRoute).setBody("Graph model imported as draft"));
    }

    private void handleExportCommand(PostOffice po, String inRoute, String outRoute, String filename) {
        if (validGraphFileName(filename)) {
            var graph = graphModels.get(inRoute);
            if (graph != null) {
                // check if the filename is the same as the Root's name property
                var root = graph.getRootNode();
                if (root == null) {
                    po.send(new EventEnvelope().setTo(outRoute).setBody("Root node created because it does not exist"));
                    root = graph.createRootNode();
                    root.addType(ROOT);
                    root.addProperty(NAME, filename);
                } else {
                    var name = root.getProperty(NAME);
                    if (name == null) {
                        root.addProperty(NAME, filename);
                        po.send(new EventEnvelope().setTo(outRoute).setBody("Added name="+filename+" to Root node"));
                    } else if (!name.equals(filename)) {
                        po.send(new EventEnvelope().setTo(outRoute).setBody("Expect root node name="+
                                filename+ ", Actual: "+name+"\nPlease update root node to match exported graph name"));
                        return;
                    }
                }
                var file = new File(tempDir, filename+JSON_EXT);
                var text = SimpleMapper.getInstance().getMapper().writeValueAsString(graph.exportGraph());
                util.str2file(file, text);
                var n = getRandomCounter();
                po.send(new EventEnvelope().setTo(outRoute).setBody(
                        "Graph exported to "+file.getPath()+"\n"+"Described in /api/graph/model/"+filename+"/"+n));
            }
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody(INVALID_GRAPH_NAME));
        }
    }

    private void handleDeleteCommand(PostOffice po, String inRoute, String outRoute, List<String> words) {
        var graph = graphModels.get(inRoute);
        if (words.size() == 3 && words.get(1).equalsIgnoreCase(NODE)) {
            var nodeName = words.get(2);
            if (graph != null) {
                var node = graph.findNodeByAlias(nodeName);
                if (node == null) {
                    po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName + NOT_FOUND));
                } else {
                    graph.removeNode(nodeName);
                    po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName + " deleted"));
                }
            }
        } else if (words.size() == 5 && words.get(1).equalsIgnoreCase(CONNECTION) &&
                words.get(3).equalsIgnoreCase("and")) {
            if (graph != null) {
                var nodeA = words.get(2);
                var nodeB = words.get(4);
                findAndDeleteConnection(po, outRoute, graph, nodeA, nodeB);
            }
        } else if (words.size() == 2 && words.get(1).equalsIgnoreCase(CACHE)) {
            clearCache(po, inRoute, outRoute);
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody(TRY_HELP));
        }
    }

    private void clearCache(PostOffice po, String inRoute, String outRoute) {
        var graphInstance = getGraphInstance(inRoute);
        if (graphInstance != null) {
            graphInstance.stateMachine.removeElement(CACHE);
            po.send(new EventEnvelope().setTo(outRoute).setBody(CACHE + " cleared"));
        }
    }

    private void findAndDeleteConnection(PostOffice po, String outRoute,
                                         MiniGraph graph, String nodeA, String nodeB) {
        if (validSourceAndTargetNodes(po, outRoute, graph, nodeA, nodeB)) {
            var sb = new StringBuilder();
            deleteConnection(graph, nodeA, nodeB, sb);
            po.send(new EventEnvelope().setTo(outRoute).setBody(sb.toString()));
        }
    }

    private void deleteConnection(MiniGraph graph, String nodeA, String nodeB, StringBuilder sb) {
        var path1 = graph.findConnection(nodeA, nodeB);
        if (path1 != null) {
            graph.removeConnection(nodeA, nodeB);
            sb.append(nodeA).append(" -> ").append(nodeB).append(" removed\n");
        }
        var path2 = graph.findConnection(nodeB, nodeA);
        if (path2 != null) {
            graph.removeConnection(nodeB, nodeA);
            sb.append(nodeB).append(" -> ").append(nodeA).append(" removed\n");
        }
        if (path1 == null && path2 == null) {
            sb.append(nodeA).append(" has no connections with ").append(nodeB).append('\n');
        }
    }

    private void handleConnectCommand(PostOffice po, String inRoute, String outRoute, List<String> words) {
        if (words.size() == 6 && words.get(2).equalsIgnoreCase("to") &&
                words.get(4).equalsIgnoreCase("with")) {
            var nodeA = words.get(1);
            var nodeB = words.get(3);
            var relation = words.get(5);
            var graph = graphModels.get(inRoute);
            if (graph != null && validSourceAndTargetNodes(po, outRoute, graph, nodeA, nodeB)) {
                graph.connect(nodeA, nodeB).addRelation(relation);
                po.send(new EventEnvelope().setTo(outRoute)
                        .setBody(NODE_NAME + nodeA + " connected to " + nodeB));
            }
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody("Syntax: connect {node-A} to {node-B} with {relation}"));
        }
    }

    private void handleJsonCommand(PostOffice po, String outRoute, String command) {
        var map = SimpleMapper.getInstance().getMapper().readValue(command, Map.class);
        var msgType = map.get("type");
        if ("ping".equals(msgType)) {
            po.send(new EventEnvelope().setTo(outRoute).setBody(Map.of("type", "pong")));
        }
        if ("welcome".equals(msgType)) {
            po.send(new EventEnvelope().setTo(outRoute).setBody("Welcome to MiniGraph Playground!"));
        }
    }

    private void handleMultiLineCommand(PostOffice po, String inRoute, String outRoute, String command, boolean shown) {
        var lines = util.split(command, "\n");
        if (!shown) {
            po.send(new EventEnvelope().setTo(outRoute).setBody("> " + lines.getFirst() + "..."));
        }
        var words = getWords(lines.getFirst());
        if (words.size() > 2 && "create".equalsIgnoreCase(words.getFirst()) && NODE.equalsIgnoreCase(words.get(1))) {
            handleCreateNode(po, inRoute, outRoute, words.get(2), lines);
        } else if (words.size() > 2 && "update".equalsIgnoreCase(words.getFirst()) &&
                NODE.equalsIgnoreCase(words.get(1))) {
            handleUpdateNode(po, inRoute, outRoute, words.get(2), lines);
        } else if (words.size() == 2 && INSTANTIATE.equalsIgnoreCase(words.getFirst())
                && GRAPH.equalsIgnoreCase(words.get(1))) {
            handleInstantiateGraph(po, inRoute, outRoute, lines);
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody(TRY_HELP));
        }
    }

    private void handleInstantiateGraph(PostOffice po, String inRoute, String outRoute, List<String> lines) {
        var graph = graphModels.get(inRoute);
        if (graph != null) {
            var currentInstance = graphInstances.get(inRoute);
            if (currentInstance != null) {
                graphInstances.remove(inRoute);
            }
            var mapper = SimpleMapper.getInstance().getMapper();
            var filename = getTempGraphName(inRoute);
            var file = new File(tempDir, filename+JSON_EXT);
            var text = mapper.writeValueAsString(graph.exportGraph());
            util.str2file(file, text);
            // use config reader to resolve environment variables
            var reader = new ConfigReader(FILE_PREFIX+file.getPath());
            var graphInstance = new GraphInstance(PLAYGROUND);
            graphInstance.graph.importGraph(reader.getMap());
            // map node properties to state machine
            var nodeCount = initializeWithNodeProperties(graphInstance);
            // perform data mapping
            var count =  new AtomicInteger(0);
            if (lines.size() > 1) {
                for (int i = 1; i < lines.size(); i++) {
                    doInitialDataMapping(lines, i, graphInstance, count);
                }
            }
            var stateMachine = graphInstance.stateMachine;
            if (!stateMachine.exists(INPUT_BODY_NAMESPACE)) {
                stateMachine.setElement(INPUT_BODY_NAMESPACE, new HashMap<>());
            }
            stateMachine.setElement(OUTPUT_NAMESPACE, new HashMap<>());
            var timeout = getModelTtl(graphInstance);
            log.info("Instantiate graph with {} nodes, model.ttl = {} ms", nodeCount, timeout);
            graphInstances.put(inRoute, graphInstance);
            po.send(new EventEnvelope().setTo(outRoute).setBody("Graph instance created. Loaded "+
                    count.get()+ " mock " + (count.get() == 1? "entry" : "entries") + ", model.ttl = "+timeout+" ms"));
        }
    }

    private void doInitialDataMapping(List<String> lines, int i, GraphInstance instance, AtomicInteger count) {
        var root = instance.graph.getRootNode();
        if (root == null) {
            throw new IllegalArgumentException("Did you forget to create a root node?");
        }
        var end = instance.graph.getEndNode();
        if (end == null) {
            throw new IllegalArgumentException("Did you forget to create an end node?");
        }
        var line = lines.get(i);
        var sep = line.lastIndexOf(MAP_TO);
        if (sep == -1) {
            throw new IllegalArgumentException("Invalid data mapping entry. e.g. 'source -> target'");
        }
        var lhs = line.substring(0, sep).trim();
        var rhs = line.substring(sep + MAP_TO.length()).trim();
        var constant = helper.getConstantValue(lhs);
        if (constant != null) {
            if (rhs.startsWith(INPUT_HEADER_NAMESPACE) || rhs.startsWith(INPUT_BODY_NAMESPACE) ||
                    rhs.startsWith(MODEL_NAMESPACE)) {
                instance.stateMachine.setElement(rhs, constant);
                count.incrementAndGet();
            } else {
                throw new IllegalArgumentException("RHS must use input.body, input.header" +
                        " or model namespace. Actual: "+rhs);
            }
        } else {
            throw new IllegalArgumentException("LHS '"+lhs+"' does not resolve to a value");
        }
    }

    private void handleDescribeCommand(PostOffice po, String inRoute, String outRoute, List<String> words) {
        if (words.size() > 1 && words.get(1).equalsIgnoreCase(GRAPH)) {
            describeGraph(po, inRoute, outRoute);
        } else if (words.size() == 3 && words.get(1).equalsIgnoreCase(SKILL)) {
            describeSkill(po, outRoute, words.get(2));
        } else if (words.size() == 3 && words.get(1).equalsIgnoreCase(NODE)) {
            describeNode(po, inRoute, outRoute, words.get(2));
        } else if (words.size() == 5 && words.get(1).equalsIgnoreCase("connection") &&
                words.get(3).equalsIgnoreCase("and")) {
            describeConnection(po, inRoute, outRoute, words.get(2), words.get(4));
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody(TRY_HELP));
        }
    }

    private void describeSkill(PostOffice po, String outRoute, String skillRoute) {
        po.send(new EventEnvelope().setTo(outRoute).setBody(getSkillDoc(po, skillRoute)));
    }

    private void describeGraph(PostOffice po, String inRoute, String outRoute) {
        var graph = graphModels.get(inRoute);
        if (graph != null) {
            var filename = getTempGraphName(inRoute);
            var file = new File(tempDir, filename+JSON_EXT);
            var text = SimpleMapper.getInstance().getMapper().writeValueAsString(graph.exportGraph());
            util.str2file(file, text);
            var size = graph.getNodes().size();
            var n = getRandomCounter();
            po.send(new EventEnvelope().setTo(outRoute).setBody("Graph with "+size+ (size == 1? " node" : " nodes")+
                    " described in /api/graph/model/"+filename+"/"+n));
        }
    }

    private void describeConnection(PostOffice po, String inRoute, String outRoute, String nodeA, String nodeB) {
        var graph = graphModels.get(inRoute);
        if (graph != null && validSourceAndTargetNodes(po, outRoute, graph, nodeA, nodeB)) {
            var sb = new StringBuilder();
            var forward = graph.findConnection(nodeA, nodeB);
            var backward = graph.findConnection(nodeB, nodeA);
            if (forward != null) {
                sb.append(nodeA).append(" -").append(getRelations(forward)).append("-> ").append(nodeB)
                        .append('\n');
            }
            if (backward != null) {
                sb.append(nodeB).append(" -").append(getRelations(backward)).append("-> ").append(nodeA)
                        .append('\n');
            }
            if (forward == null && backward == null) {
                sb.append(nodeA).append(" is not connected to ").append(nodeB).append('\n');
            }
            po.send(new EventEnvelope().setTo(outRoute).setBody(sb.toString()));
        }
    }

    private boolean validSourceAndTargetNodes(PostOffice po, String outRoute,
                                              MiniGraph graph, String nodeA, String nodeB) {
        if (nodeA.equals(nodeB)) {
            po.send(new EventEnvelope().setTo(outRoute).setBody(SAME_SOURCE_TARGET));
        } else {
            var node1 = graph.findNodeByAlias(nodeA);
            var node2 = graph.findNodeByAlias(nodeB);
            if (node1 == null) {
                po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeA + NOT_FOUND));
            } else if (node2 == null) {
                po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeB + NOT_FOUND));
            } else {
                return true;
            }
        }
        return false;
    }

    private Set<String> getRelations(SimpleConnection connection) {
        var result = new HashSet<String>();
        var relations = connection.getRelations();
        for (var relation : relations) {
            result.add(relation.getType());
        }
        return result;
    }

    private void describeNode(PostOffice po, String inRoute, String outRoute, String nodeName) {
        var graph = graphModels.get(inRoute);
        if (graph != null) {
            var node = graph.findNodeByAlias(nodeName);
            if (node != null) {
                var map = getConnections(graph, nodeName);
                map.put(NODE, node);
                po.send(new EventEnvelope().setTo(outRoute).setBody(map));
            } else {
                po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName + NOT_FOUND));
            }
        }
    }

    private Map<String, Object> getConnections(MiniGraph graph, String nodeName) {
        var map = new HashMap<String, Object>();
        var forward = graph.getForwardLinks(nodeName);
        var forwardNodes = new ArrayList<String>();
        for (var f: forward) {
            forwardNodes.add(f.getAlias());
        }
        var backward = graph.getBackwardLinks(nodeName);
        var backwardNodes = new ArrayList<String>();
        for (var f: backward) {
            backwardNodes.add(f.getAlias());
        }
        if (!forwardNodes.isEmpty()) {
            map.put("to", forwardNodes);
        }
        if (!backwardNodes.isEmpty()) {
            map.put("from", backwardNodes);
        }
        return map;
    }

    private void handleUpdateNode(PostOffice po, String inRoute, String outRoute, String nodeName, List<String> lines) {
        var keyValues = getNodeProperties(lines);
        var type = getNodeType(lines);
        var graph = graphModels.get(inRoute);
        if (graph != null) {
            updateNode(po, graph, outRoute, nodeName, type, keyValues);
        }
    }

    private void updateNode(PostOffice po, MiniGraph graph, String outRoute,
                            String nodeName, String type, MultiLevelMap keyValues) {
        var node = graph.findNodeByAlias(nodeName);
        if (node == null) {
            po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName + NOT_FOUND));
        } else {
            node.getTypes().clear();
            node.getProperties().clear();
            node.addType(type == null? UNTYPED : type);
            var map = keyValues.getMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                node.addProperty(entry.getKey(), entry.getValue());
            }
            po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName + " updated"));
        }
    }

    private void handleCreateNode(PostOffice po, String inRoute, String outRoute, String nodeName, List<String> lines) {
        var keyValues = getNodeProperties(lines);
        var type = getNodeType(lines);
        var graph = graphModels.get(inRoute);
        if (graph != null) {
            createNode(po, graph, outRoute, nodeName, type, keyValues);
        }
    }

    private void createNode(PostOffice po, MiniGraph graph, String outRoute,
                            String nodeName, String type, MultiLevelMap keyValues) {
        var node = graph.findNodeByAlias(nodeName);
        if (node != null) {
            po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName + " already exists"));
        } else {
            node = graph.createNode(nodeName, type == null? UNTYPED : type);
            var map = keyValues.getMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                node.addProperty(entry.getKey(), entry.getValue());
            }
            po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName + " created"));
        }
    }

    private String getNodeType(List<String> lines) {
        for (String line : lines) {
            var lower = line.toLowerCase().trim();
            if (lower.startsWith(WITH_TYPE)) {
                var words = util.split(line, " ");
                if (words.size() > 2) {
                    return words.get(2);
                }
            }
        }
        return null;
    }

    private MultiLevelMap getNodeProperties(List<String> lines) {
        var result = new MultiLevelMap();
        var pLines = new ArrayList<String>();
        var found = false;
        for (var line : lines) {
            var lower = line.toLowerCase().trim();
            if (lower.startsWith(WITH_PROPERTIES)) {
                found = true;
            } else if (found) {
                if (lower.startsWith(WITH_TYPE)) {
                    break;
                }
                pLines.add(line);
            }
        }
        if (pLines.isEmpty()) {
            return result;
        }
        var multiline = new AtomicBoolean(false);
        var mlKey = new AtomicReference<String>();
        var sb = new StringBuilder();
        for (var pl: pLines) {
            if (multiline.get()) {
                prepareMultiLineProperty(multiline, mlKey, pl, sb, result);
            } else {
                prepareSingleLineProperty(multiline, mlKey, pl, sb, result);
            }
        }
        return result;
    }

    private void prepareMultiLineProperty(AtomicBoolean multiline, AtomicReference<String> mlKey, String pl,
                                          StringBuilder sb, MultiLevelMap result) {
        int ml = pl.indexOf(TRIPLE_QUOTE);
        if (ml != -1) {
            var v = pl.substring(0, ml).trim();
            if (!v.isEmpty()) {
                sb.append(v).append('\n');
            }
            if (mlKey.get() != null) {
                result.setElement(mlKey.get(), sb.toString().trim());
            }
            sb.setLength(0);
            multiline.set(false);
        } else {
            sb.append(pl).append('\n');
        }
    }

    private void prepareSingleLineProperty(AtomicBoolean multiline, AtomicReference<String> mlKey, String pl,
                                           StringBuilder sb, MultiLevelMap result) {
        int eq = pl.indexOf('=');
        var key = eq == -1? pl : pl.substring(0, eq).trim();
        var value = eq == -1? "" : pl.substring(eq + 1).trim();
        int ml = eq == -1? pl.indexOf(TRIPLE_QUOTE) : value.indexOf(TRIPLE_QUOTE);
        if (ml != -1) {
            multiline.set(true);
            mlKey.set(key);
            var v = value.substring(ml+3).trim();
            if (!v.isEmpty()) {
                sb.append(v).append("\n");
            }
        } else {
            result.setElement(key, value);
        }
    }

    private String getHelp(List<String> commandParts) throws IOException {
        var sb = new StringBuilder();
        for (String s: commandParts) {
            sb.append(s).append(" ");
        }
        var title = sb.toString().toLowerCase().trim();
        try (var in = this.getClass().getResourceAsStream(HELP_PREFIX + title + MARKDOWN_EXT)) {
            return in == null? null : util.stream2str(in);
        }
    }

    private String getSkillDoc(PostOffice po, String skill) {
        if (po.exists(skill)) {
            var filename = SKILL_PREFIX + skill.replace('.', '-') + MARKDOWN_EXT;
            try (var in = this.getClass().getResourceAsStream(filename)) {
                return in == null? "Did you forget to add "+filename+"?" : util.stream2str(in);
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            throw new IllegalArgumentException(SKILL_TAG + skill + NOT_FOUND);
        }
    }

    private String getRandomCounter() {
        var now = String.valueOf(System.currentTimeMillis());
        return now.substring(now.length()-3) + "-" + counter.incrementAndGet();
    }
}
