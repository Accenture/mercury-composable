package com.accenture.minigraph.services;

import com.accenture.minigraph.models.GraphModelAndInstance;
import com.accenture.util.DataMappingHelper;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleConnection;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@PreLoad(route = GraphCommandService.ROUTE, instances=10)
public class GraphCommandService implements TypedLambdaFunction<Map<String, Object>, Void> {
    public static final String ROUTE = "graph.command.service";
    private static final Logger log = LoggerFactory.getLogger(GraphCommandService.class);
    private static final DataMappingHelper helper = DataMappingHelper.getInstance();
    private static final Utility util = Utility.getInstance();
    private static final ConcurrentMap<String, GraphModelAndInstance> sessions = new ConcurrentHashMap<>();
    private static final String HELP_PREFIX = "/help/";
    private static final String SKILL_PREFIX = "/skills/";
    private static final String MARKDOWN = ".md";
    private static final String TRIPLE_QUOTE = "'''";
    private static final String WITH_TYPE = "with type";
    private static final String WITH_PROPERTIES = "with properties";
    private static final String NODE_NAME = "node ";
    private static final String SKILL_NAME = "skill ";
    private static final String NOT_FOUND = " not found";
    private static final String TRY_HELP = "Please try 'help' for details";
    private static final String SAME_SOURCE_TARGET = "source and target node names cannot be the same";
    private static final String JSON_EXT = ".json";
    private static final String GRAPH = "graph";
    private final File tempDir;

    public GraphCommandService() {
        var config = AppConfigReader.getInstance();
        var location = config.getProperty("location.graph.temp", "/tmp/graph");
        this.tempDir = new File(location);
        if (!this.tempDir.exists()) {
            boolean created = this.tempDir.mkdirs();
            if (created) {
                log.info("Created graph temp directory {}", location);
            }
        }
    }

    @Override
    public Void handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var po = new PostOffice(headers, instance);
        try {
            handleCommand(po, input);
        } catch (Exception e) {
            if (input.get("out") instanceof String outRoute) {
                po.send(new EventEnvelope().setTo(outRoute).setBody("ERROR: "+e.getMessage()));
            }
        }
        return null;
    }

    private void handleCommand(PostOffice po, Map<String, Object> input) throws IOException {
        var type = input.get("type");
        var in = input.get("in");
        var out = input.get("out");
        var message = input.get("message");
        if ("open".equals(type) && in instanceof String inRoute) {
            sessions.put(inRoute, new GraphModelAndInstance());
        }
        if ("close".equals(type) && in instanceof String inRoute) {
            sessions.remove(inRoute);
            var filename = getTempGraphName(inRoute);
            var file = new File(tempDir, filename + JSON_EXT);
            if (file.exists()) {
                // delete draft graph model
                Files.delete(file.toPath());
            }
        }
        if ("command".equals(type) && in instanceof String inRoute && out instanceof String outRoute &&
                message instanceof String text) {
            var command = text.trim();
            if (command.startsWith("{") && command.endsWith("}")) {
                handleJsonCommand(po, outRoute, command);
            } else if (command.contains("\n")) {
                handleMultiLineCommand(po, inRoute, outRoute, command);
            } else {
                handleSingleLineCommand(po, inRoute, outRoute, command);
            }
        }
    }

    private void handleSingleLineCommand(PostOffice po, String inRoute, String outRoute, String command)
            throws IOException {
        var words = util.split(command, " ");
        if (!words.isEmpty() && words.getFirst().equalsIgnoreCase("help")) {
            var helpText = getHelp(words);
            po.send(new EventEnvelope().setTo(outRoute).setBody(
                    Objects.requireNonNullElseGet(helpText, () -> "'" + command + "'"+NOT_FOUND)));
        } else if (words.size() > 2 && words.getFirst().equalsIgnoreCase("create")) {
            // handle create command without type and properties
            handleMultiLineCommand(po, inRoute, outRoute, command);
        } else if (words.size() > 1 && words.getFirst().equalsIgnoreCase("describe")) {
            handleDescribeCommand(po, inRoute, outRoute, words);
        } else if (words.size() > 2 && words.getFirst().equalsIgnoreCase("connect")) {
            handleConnectCommand(po, inRoute, outRoute, words);
        } else if (words.size() > 1 && words.getFirst().equalsIgnoreCase("delete")) {
            handleDeleteCommand(po, inRoute, outRoute, words);
        } else if (words.size() == 4 && words.getFirst().equalsIgnoreCase("export") &&
                    words.get(1).equalsIgnoreCase(GRAPH) &&
                    words.get(2).equalsIgnoreCase("as")) {
            handleExportCommand(po, inRoute, outRoute, words.get(3));
        } else if (words.size() == 4 && words.getFirst().equalsIgnoreCase("import") &&
                words.get(1).equalsIgnoreCase(GRAPH) &&
                words.get(2).equalsIgnoreCase("from")) {
            handleImportCommand(po, inRoute, outRoute, words.get(3));
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody(TRY_HELP));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleImportCommand(PostOffice po, String inRoute, String outRoute, String filename) {
        if (validGraphFileName(filename)) {
            var holder = sessions.get(inRoute);
            if (holder != null) {
                var graph = holder.graphModel;
                var file = new File(tempDir, filename+JSON_EXT);
                if (file.exists()) {
                    var map = SimpleMapper.getInstance().getMapper().readValue(util.file2str(file), Map.class);
                    graph.importGraph(map);
                    po.send(new EventEnvelope().setTo(outRoute).setBody("Graph model imported"));
                } else {
                    po.send(new EventEnvelope().setTo(outRoute).setBody("Graph model not found"));
                }
            }
        } else {
            po.send(new EventEnvelope().setTo(outRoute)
                    .setBody("ERROR: Invalid filename - must be lower case with optional hyphen"));
        }
    }

    private void handleExportCommand(PostOffice po, String inRoute, String outRoute, String filename) {
        if (validGraphFileName(filename)) {
            var holder = sessions.get(inRoute);
            if (holder != null) {
                var graph = holder.graphModel;
                var file = new File(tempDir, filename+JSON_EXT);
                var text = SimpleMapper.getInstance().getMapper().writeValueAsString(graph.exportGraph());
                util.str2file(file, text);
                po.send(new EventEnvelope().setTo(outRoute).setBody(
                        "Graph exported to '"+file.getPath()+"\n"+
                        "Described in /api/graph/model/"+filename));
            }
        } else {
            po.send(new EventEnvelope().setTo(outRoute)
                    .setBody("ERROR: Invalid filename - must be lower case with optional hyphen"));
        }
    }

    private void handleDeleteCommand(PostOffice po, String inRoute, String outRoute, List<String> words) {
        var holder = sessions.get(inRoute);
        if (words.size() == 3 && words.get(1).equalsIgnoreCase("node")) {
            var nodeName = words.get(2);
            if (holder != null) {
                var graph = holder.graphModel;
                var node = graph.findNodeByAlias(nodeName);
                if (node == null) {
                    po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName + NOT_FOUND));
                } else {
                    graph.removeNode(nodeName);
                    po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName + " deleted"));
                }
            }
        } else if (words.size() == 5 && words.get(1).equalsIgnoreCase("connection") &&
                words.get(3).equalsIgnoreCase("and")) {
            if (holder != null) {
                var nodeA = words.get(2);
                var nodeB = words.get(4);
                findAndDeleteConnection(po, outRoute, holder, nodeA, nodeB);
            }
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody(TRY_HELP));
        }
    }

    private void findAndDeleteConnection(PostOffice po, String outRoute,
                                         GraphModelAndInstance holder, String nodeA, String nodeB) {
        var graph = holder.graphModel;
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
            var holder = sessions.get(inRoute);
            if (holder != null) {
                var graph = holder.graphModel;
                if (validSourceAndTargetNodes(po, outRoute, graph, nodeA, nodeB)) {
                    graph.connect(nodeA, nodeB).addRelation(relation);
                    po.send(new EventEnvelope().setTo(outRoute)
                            .setBody(NODE_NAME + nodeA + " connected to " + nodeB));
                }
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

    private void handleMultiLineCommand(PostOffice po, String inRoute, String outRoute, String command) {
        var lines = util.split(command, "\n");
        var words = util.split(lines.getFirst(), " ");
        if (words.size() > 2 && "create".equalsIgnoreCase(words.getFirst()) && "node".equalsIgnoreCase(words.get(1))) {
            handleCreateNode(po, inRoute, outRoute, words.get(2), lines);
        } else if (words.size() > 2 && "update".equalsIgnoreCase(words.getFirst()) &&
                "node".equalsIgnoreCase(words.get(1))) {
            handleUpdateNode(po, inRoute, outRoute, words.get(2), lines);
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody(TRY_HELP));
        }
    }

    private void handleDescribeCommand(PostOffice po, String inRoute, String outRoute, List<String> words)
            throws IOException {
        if (words.size() > 1 && words.get(1).equalsIgnoreCase(GRAPH)) {
            describeGraph(po, inRoute, outRoute);
        } else if (words.size() == 3 && words.get(1).equalsIgnoreCase("skill")) {
            describeSkill(po, outRoute, words.get(2));
        } else if (words.size() == 3 && words.get(1).equalsIgnoreCase("node")) {
            describeNode(po, inRoute, outRoute, words.get(2));
        } else if (words.size() == 5 && words.get(1).equalsIgnoreCase("connection") &&
                words.get(3).equalsIgnoreCase("and")) {
            describeConnection(po, inRoute, outRoute, words.get(2), words.get(4));
        } else {
            po.send(new EventEnvelope().setTo(outRoute).setBody(TRY_HELP));
        }
    }

    private void describeSkill(PostOffice po, String outRoute, String skillRoute) throws IOException {
        po.send(new EventEnvelope().setTo(outRoute).setBody(getSkillDoc(skillRoute)));
    }


    private void describeGraph(PostOffice po, String inRoute, String outRoute) {
        var holder = sessions.get(inRoute);
        if (holder != null) {
            var graph = holder.graphModel;
            var filename = getTempGraphName(inRoute);
            var file = new File(tempDir, filename+JSON_EXT);
            var text = SimpleMapper.getInstance().getMapper().writeValueAsString(graph.exportGraph());
            util.str2file(file, text);
            po.send(new EventEnvelope().setTo(outRoute).setBody("Graph described in /api/graph/model/"+filename));
        }
    }

    private String getTempGraphName(String inRoute) {
        return inRoute.replace('.', '_');
    }

    private void describeConnection(PostOffice po, String inRoute, String outRoute, String nodeA, String nodeB) {
        var holder = sessions.get(inRoute);
        if (holder != null) {
            var graph = holder.graphModel;
            if (validSourceAndTargetNodes(po, outRoute, graph, nodeA, nodeB)) {
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
        var holder = sessions.get(inRoute);
        if (holder != null) {
            var graph = holder.graphModel;
            var node = graph.findNodeByAlias(nodeName);
            if (node != null) {
                var map = getConnections(graph, nodeName);
                map.put("node", node);
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
        var holder = sessions.get(inRoute);
        if (holder != null) {
            updateNode(po, holder, outRoute, nodeName, type, keyValues);
        }
    }

    private void updateNode(PostOffice po, GraphModelAndInstance holder, String outRoute,
                            String nodeName, String type, MultiLevelMap keyValues) {
        var graph = holder.graphModel;
        var node = graph.findNodeByAlias(nodeName);
        if (node == null) {
            po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName + NOT_FOUND));
        } else {
            node.getTypes().clear();
            node.getProperties().clear();
            node.addType(type == null? "untyped" : type);
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
        var holder = sessions.get(inRoute);
        if (holder != null) {
            createNode(po, holder, outRoute, nodeName, type, keyValues);
        }
    }

    private void createNode(PostOffice po, GraphModelAndInstance holder, String outRoute,
                            String nodeName, String type, MultiLevelMap keyValues) {
        var graph = holder.graphModel;
        var node = graph.findNodeByAlias(nodeName);
        if (node != null) {
            po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName + " already exists"));
        } else {
            node = graph.createNode(nodeName, type == null? "untyped" : type);
            var map = keyValues.getMap();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                node.addProperty(entry.getKey(), entry.getValue());
            }
            po.send(new EventEnvelope().setTo(outRoute).setBody(NODE_NAME + nodeName + " created"));
        }
    }

    private String getNodeType(List<String> lines) {
        for (String line : lines) {
            var lower = line.toLowerCase();
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
            var lower = line.toLowerCase();
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
            var constant = helper.getConstantValue(value);
            var v = constant != null? constant : value;
            result.setElement(key, v);
        }
    }

    private String getHelp(List<String> commandParts) throws IOException {
        var sb = new StringBuilder();
        for (String s: commandParts) {
            sb.append(s).append(" ");
        }
        var title = sb.toString().toLowerCase().trim();
        try (var in = this.getClass().getResourceAsStream(HELP_PREFIX + title + MARKDOWN)) {
            return in == null? null : util.stream2str(in);
        }
    }

    private String getSkillDoc(String skill) throws IOException {
        var title = skill.toLowerCase().replace(".", "-");
        try (var in = this.getClass().getResourceAsStream(SKILL_PREFIX + title + MARKDOWN)) {
            return in == null? SKILL_NAME + skill + NOT_FOUND : util.stream2str(in);
        }
    }

    public boolean validGraphFileName(String str) {
        if (str == null || str.isEmpty()) return false;
        for (int i=0; i < str.length(); i++) {
            if (!((str.charAt(i) >= '0' && str.charAt(i) <= '9') ||
                    (str.charAt(i) >= 'a' && str.charAt(i) <= 'z') ||
                    (str.charAt(i) >= 'A' && str.charAt(i) <= 'Z') ||
                    str.charAt(i) == '_' || str.charAt(i) == '-' )) {
                return false;
            }
        }
        return true;
    }
}
