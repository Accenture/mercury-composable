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

import com.accenture.minigraph.models.GraphInstance;
import com.accenture.util.DataMappingHelper;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.models.*;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class GraphLambdaFunction implements TypedLambdaFunction<EventEnvelope, Object> {
    protected static final ConcurrentMap<String, MiniGraph> graphModels = new ConcurrentHashMap<>();
    protected static final ConcurrentMap<String, GraphInstance> graphInstances = new ConcurrentHashMap<>();
    protected static final DataMappingHelper helper = DataMappingHelper.getInstance();
    protected static final Utility util = Utility.getInstance();
    protected static final String ASYNC_HTTP_CLIENT = "async.http.request";
    protected static final String PLUGIN_PREFIX = "f:";
    protected static final String FILE_PREFIX = "file:";
    protected static final String CLASSPATH_PREFIX = "classpath:";
    protected static final String OPEN = "open";
    protected static final String CLOSE = "close";
    protected static final String COMMAND = "command";
    protected static final String UNTYPED = "untyped";
    protected static final String NAME = "name";
    protected static final String ROOT = "root";
    protected static final String END = "end";
    protected static final String IN = "in";
    protected static final String OUT = "out";
    protected static final String NEXT = "next";
    protected static final String MAPPING = "mapping";
    protected static final String JS = "js";
    protected static final String STATEMENT = "statement";
    protected static final String FLOW_ID = "flow_id";
    protected static final String DD = ".dd.";
    protected static final String EACH = ".each.";
    protected static final String FETCH = ".fetch.";
    protected static final String CACHE_NAMESPACE = "cache.";
    protected static final String CACHE = "cache";
    protected static final String DOT_RESPONSE = ".response";
    protected static final String RESPONSE_DOT = ".response.";
    protected static final String RESULT = "result";
    protected static final String URL =  "url";
    protected static final String METHOD = "method";
    protected static final String FEATURE = "feature";
    protected static final String EXTENSION = "extension";
    protected static final String MAP_TO = "->";
    protected static final String INPUT = "input";
    protected static final String OUTPUT = "output";
    protected static final String BODY = "body";
    protected static final String BODY_NAMESPACE = "body.";
    protected static final String PROVIDER = "provider";
    protected static final String DICTIONARY = "dictionary";
    protected static final String HEADER_PARAMETER = "header.";
    protected static final String QUERY_PARAMETER = "query.";
    protected static final String PATH_PARAMETER = "path_parameter.";
    protected static final String INPUT_BODY_NAMESPACE = "input.body";
    protected static final String INPUT_HEADER_NAMESPACE = "input.header";
    protected static final String OUTPUT_BODY_NAMESPACE = "output.body";
    protected static final String OUTPUT_HEADER_NAMESPACE = "output.header";
    protected static final String MODEL = "model";
    protected static final String MODEL_NAMESPACE = "model.";
    protected static final String RESULT_NAMESPACE = "result.";
    protected static final String RESPONSE_NAMESPACE = "response.";
    protected static final String OUTPUT_NAMESPACE = "output.";
    protected static final String OUTPUT_BODY = "output.body";
    protected static final String OUTPUT_ARRAY = "output[";
    protected static final String MESSAGE = "message";
    protected static final String TYPE = "type";
    protected static final String HELP_PREFIX = "/help/";
    protected static final String MARKDOWN_EXT = ".md";
    protected static final String TRIPLE_QUOTE = "'''";
    protected static final String WITH_TYPE = "with type";
    protected static final String WITH_PROPERTIES = "with properties";
    protected static final String NODE = "node";
    protected static final String NODE_NAME = "node ";
    protected static final String CONNECTION = "connection";
    protected static final String SKILL_TAG = "skill ";
    protected static final String NOT_FOUND = " not found";
    protected static final String TRY_HELP = "Please try 'help' for details";
    protected static final String SAME_SOURCE_TARGET = "source and target node names cannot be the same";
    protected static final String JSON_EXT = ".json";
    protected static final String GRAPH = "graph";
    protected static final String EXECUTE = "execute";
    protected static final String SKILL = "skill";
    protected static final String SKILL_PREFIX = "/skills/";
    protected static final String SINK = ".sink";
    protected static final String RUN = "run";
    protected static final String LIVE = "live";
    protected static final String MODEL_TTL = "model.ttl";
    protected static final String STATUS = "status";
    protected static final String HEADER = "header";
    protected static final String ERROR = "error";
    protected static final String CONTINUE = "continue";
    protected static final String INSTANTIATE = "instantiate";
    protected static final String DELETE = "delete";
    protected static final String START = "start";
    protected static final String CLEAR = "clear";
    protected static final String HELP = "help";
    protected static final String FOR_EACH = "for_each";
    protected static final String CONCURRENCY = "concurrency";
    protected static final String PURPOSE = "purpose";
    protected static final String MAPPING_TAG = "mapping:";
    protected static final String COMPUTE_TAG = "compute:";
    protected static final String EXECUTE_TAG = "execute:";
    protected static final String IF_TAG = "if:";
    protected static final String THEN_TAG = "then:";
    protected static final String ELSE_TAG = "else:";
    protected static final String INSPECT = "inspect";
    private static final Set<String> RESERVED_PARAMETERS = Set.of(SKILL, MAPPING, STATEMENT, INPUT, OUTPUT, FEATURE,
                                                        STATUS, ERROR, DICTIONARY, FOR_EACH, CONCURRENCY, PURPOSE);

    protected GraphInstance getGraphInstance(String id) {
        var instance = graphInstances.get(id);
        if (instance == null) {
            throw new IllegalArgumentException("Graph instance " + id + " not started");
        } else {
            return instance;
        }
    }

    protected SimpleNode getNode(String nodeName, MiniGraph graph) {
        var node = graph.findNodeByAlias(nodeName);
        if (node == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + NOT_FOUND);
        } else {
            return node;
        }
    }

    protected boolean validGraphFileName(String str) {
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

    protected String getTempGraphName(String inRoute) {
        var name = inRoute.contains(".")? inRoute.substring(0,inRoute.lastIndexOf(".")) : inRoute;
        return name.replace('.', '-');
    }

    protected String substituteVarIfAny(String text, MultiLevelMap stateMachine, boolean logical) {
        var jsonPathOperator = text.startsWith("$.") && (hasBooleanOperator(text) || text.contains("@"));
        int leftBrace = text.indexOf('{');
        int rightBrace = text.lastIndexOf('}');
        if (leftBrace != -1 && rightBrace != -1 && rightBrace > leftBrace) {
            var start = 0;
            var sb = new StringBuilder();
            var segments = util.extractSegments(text, "{", "}");
            for (var segment : segments) {
                start = replaceWithParameter(segment, sb, start, text, stateMachine, logical || jsonPathOperator);
            }
            var lastSegment = text.substring(start);
            if (!lastSegment.isEmpty()) {
                sb.append(lastSegment);
            }
            return sb.toString();
        } else {
            return text;
        }
    }

    protected boolean hasBooleanOperator(String text) {
        return text.contains("&&") || text.contains("||") || text.contains("!") || text.contains(">") ||
                text.contains("<") || text.contains(">=") || text.contains("<=") || text.contains("==");
    }

    private int replaceWithParameter(VarSegment segment, StringBuilder sb, int start, String text,
                                     MultiLevelMap stateMachine, boolean logical) {
        String heading = text.substring(start, segment.start());
        if (!heading.isEmpty()) {
            sb.append(heading);
        }
        var key = text.substring(segment.start() + 1, segment.end() - 1);
        if (key.contains("\n")) {
            // it is likely a JavaScript function instead of a variable
            sb.append(text, segment.start(), segment.end());
        } else {
            var parameter = helper.getLhsOrConstant(key, stateMachine);
            if (parameter == null) {
                sb.append("null");
            } else if (parameter instanceof Number || parameter instanceof Boolean) {
                sb.append(parameter);
            } else if (parameter instanceof Map || parameter instanceof List) {
                var value = SimpleMapper.getInstance().getCompactGson().toJson(parameter);
                sb.append(value);
            } else {
                if (logical) {
                    sb.append("'").append(escapeVar(parameter)).append("'");
                } else {
                    sb.append(parameter);
                }
            }
        }
        return segment.end();
    }
    
    private String escapeVar(Object parameter) {
        var v = String.valueOf(parameter);
        return v.contains("'")? v.replace("'", "\\'") : v;
    }

    protected void handleDataMappingEntry(String nodeName, String command, GraphInstance graphInstance) {
        int sep = command.lastIndexOf(MAP_TO);
        if (sep > 0) {
            var stateMachine = graphInstance.stateMachine;
            var lhs = substituteVarIfAny(command.substring(0, sep).trim(), stateMachine, false);
            var rhs = command.substring(sep + MAP_TO.length()).trim();
            var value = helper.getLhsOrConstant(lhs, stateMachine);
            validateRhs(nodeName, rhs, graphInstance.graph);
            if (value != null) {
                stateMachine.setElement(rhs, value);
            } else {
                if (rhs.endsWith("]") && rhs.contains("[")) {
                    stateMachine.setElement(rhs, null);
                } else {
                    stateMachine.removeElement(rhs);
                }
            }
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have '->' in '"+command+"'");
        }
    }

    protected int initializeWithNodeProperties(GraphInstance graphInstance) {
        var stateMachine = graphInstance.stateMachine;
        var nodes = graphInstance.graph.getNodes();
        for (var node: nodes) {
            var name = node.getAlias();
            var properties = node.getProperties();
            if (properties.containsKey(SKILL)) {
                for (Map.Entry<String, Object> kv: properties.entrySet()) {
                    if (!RESERVED_PARAMETERS.contains(kv.getKey())) {
                        stateMachine.setElement(name+"."+kv.getKey(), kv.getValue());
                    }
                }
            } else {
                stateMachine.setElement(name, properties);
            }
        }
        return nodes.size();
    }

    protected long getModelTtl(GraphInstance instance) {
        if (!instance.stateMachine.exists(MODEL)) {
            instance.stateMachine.setElement(MODEL, new HashMap<>());
        }
        var ttl = String.valueOf(instance.stateMachine.getElement(MODEL_TTL, "30000"));
        return Math.max(1000, util.str2long(ttl));
    }

    protected void fillFetcherApiParameters(String nodeName,
                                            String command, GraphInstance graphInstance, boolean isArray) {
        int sep = command.lastIndexOf(MAP_TO);
        if (sep > 0) {
            var stateMachine = graphInstance.stateMachine;
            var lhs = substituteVarIfAny(command.substring(0, sep).trim(), stateMachine, false);
            var rhs = command.substring(sep + MAP_TO.length()).trim();
            var target = rhs.startsWith(MODEL_NAMESPACE)? rhs : getFetcherTarget(nodeName, rhs, isArray);
            var value = helper.getLhsOrConstant(lhs, stateMachine);
            if (value != null) {
                stateMachine.setElement(target, value);
            } else {
                if (target.endsWith("]") && target.contains("[")) {
                    stateMachine.setElement(target, null);
                } else {
                    stateMachine.removeElement(target);
                }
            }
        } else {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have '->' in '"+command+"'");
        }
    }

    private String getFetcherTarget(String nodeName, String rhs, boolean isArray) {
        return isArray ? nodeName + EACH + rhs + "[]" : nodeName + FETCH + rhs;
    }

    protected List<String> getEntries(Object entries) {
        var result = new ArrayList<String>();
        if (entries instanceof List<?> list) {
            list.forEach(item -> result.add(String.valueOf(item)));
        }
        return result;
    }

    protected void mapHttpInput(AsyncHttpRequest request, String nodeName, String ddName,
                                MultiLevelMap stateMachine, List<String> mapping) {
        var body = new HashMap<String, Object>();
        List<Object> wholeBody = new ArrayList<>();
        for (var entry : mapping) {
            int sep = entry.lastIndexOf(MAP_TO);
            if (sep != -1) {
                var lhs = entry.substring(0, sep).trim();
                var rhs = entry.substring(sep + MAP_TO.length()).trim();
                var value = getValueBestEffort(nodeName, ddName, lhs, stateMachine);
                if (value != null) {
                    mapHttpParams(request, rhs, value, body, wholeBody);
                }
            }
        }
        request.setBody(wholeBody.isEmpty()? body : wholeBody.getFirst());
    }

    protected Map<String, List<?>> getForEachMapping(String nodeName, List<String> forEach, MultiLevelMap stateMachine) {
        int size = -1;
        Map<String, List<?>> mappings = new HashMap<>();
        for (var entry : forEach) {
            var sep = entry.lastIndexOf(MAP_TO);
            var lhs = substituteVarIfAny(entry.substring(0, sep).trim(), stateMachine, false);
            var rhs = entry.substring(sep+MAP_TO.length()).trim();
            if (!rhs.startsWith(MODEL_NAMESPACE)) {
                throw new IllegalArgumentException(NODE_NAME + nodeName +
                        " RHS of 'for_each' entry must use 'model.' namespace. Actual: " + entry);
            }
            var value = helper.getLhsOrConstant(lhs, stateMachine);
            if (value instanceof List<?> list) {
                if (size == -1) {
                    size = list.size();
                } else if (size != list.size()) {
                    throw new IllegalArgumentException(NODE_NAME + nodeName +
                            " LHS of 'for_each' contains inconsistent array sizes");
                }
                mappings.put(rhs, list);
            } else if (value != null) {
                stateMachine.setElement(rhs, value);
            }
        }
        return mappings;
    }

    protected int getModelArraySize(Map<String, List<?>> mappings) {
        var keys = new ArrayList<>(mappings.keySet());
        return mappings.get(keys.getFirst()).size();
    }

    protected Map<String, Object> getNextModelParamSet(Map<String, List<?>> mappings, int i) {
        var result = new HashMap<String, Object>();
        for (var entry : mappings.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get(i));
        }
        return result;
    }

    protected void performFetcherOutputMapping(String nodeName, MultiLevelMap stateMachine, List<String> mapping) {
        for (var output : mapping) {
            var text = String.valueOf(output).trim();
            int sep = text.lastIndexOf(MAP_TO);
            if (sep != -1) {
                var lhs = substituteVarIfAny(text.substring(0, sep).trim(), stateMachine, false);
                var rhs = text.substring(sep + MAP_TO.length()).trim();
                setFetcherOutputEntry(nodeName, lhs, rhs, stateMachine);
            } else {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " - invalid output mapping: "+text);
            }
        }
    }

    private void setFetcherOutputEntry(String nodeName, String lhs, String rhs, MultiLevelMap stateMachine) {
        var value = helper.getConstantValue(lhs);
        if (value == null) {
            if (!lhs.startsWith(PLUGIN_PREFIX)) {
                // reconstruct lhs with nodeName as namespace
                if (lhs.equals(RESULT) || lhs.startsWith(RESULT_NAMESPACE) || lhs.startsWith(RESULT + "[")) {
                    lhs = nodeName + "." + lhs;
                } else if (lhs.startsWith("$.result")) {
                    lhs = "$." + nodeName + lhs.substring(1);
                } else if (!lhs.startsWith(nodeName + ".") &&
                        !lhs.startsWith(MODEL_NAMESPACE) && !lhs.startsWith("$.model.")) {
                    throw new IllegalArgumentException("Invalid output data mapping in API fetcher " + nodeName +
                            " - LHS must start with 'model.', 'result.' namespace or '" + nodeName + ".'");
                }
            }
            value = helper.getLhsElement(lhs, stateMachine);
        }
        if (value != null) {
            if (!rhs.startsWith(MODEL_NAMESPACE) && !rhs.startsWith(OUTPUT_NAMESPACE)) {
                throw new IllegalArgumentException("Invalid output data mapping in data dictionary "+nodeName +
                        " - RHS must start with 'model.' or 'output.' namespace");
            }
            stateMachine.setElement(rhs, value);
        }
    }

    protected String getNormalizedPath(String folder, String graphId) {
        var sb = new StringBuilder();
        var parts = util.split(folder, "/");
        for (String part : parts) {
            sb.append('/').append(part);
        }
        sb.append('/').append(graphId);
        sb.append(JSON_EXT);
        return sb.substring(1);
    }

    private Object getValueBestEffort(String nodeName, String ddName, String lhs, MultiLevelMap stateMachine) {
        var constant = helper.getConstantValue(lhs);
        if (constant != null) {
            return constant;
        }
        // Found in state machine?
        var value = helper.getLhsElement(lhs, stateMachine);
        // Get value from API input
        return value != null? value : helper.getLhsElement(nodeName + DD + ddName + "." + lhs, stateMachine);
    }

    private void mapHttpParams(AsyncHttpRequest request, String rhs, Object value,
                               Map<String, Object> body, List<Object> wholeBody) {
        if (rhs.startsWith(PATH_PARAMETER)) {
            var key = rhs.substring(PATH_PARAMETER.length()).trim();
            request.setPathParameter(key, String.valueOf(value));
        } else if (rhs.startsWith(QUERY_PARAMETER)) {
            var key = rhs.substring(QUERY_PARAMETER.length()).trim();
            request.setQueryParameter(key, String.valueOf(value));
        } else if (rhs.startsWith(HEADER_PARAMETER)) {
            var key = rhs.substring(HEADER_PARAMETER.length()).trim();
            request.setHeader(key, String.valueOf(value));
        } else if (rhs.startsWith(BODY_NAMESPACE)) {
            var key = rhs.substring(BODY_NAMESPACE.length()).trim();
            if (!key.isEmpty()) {
                body.put(key, value);
            } else {
                wholeBody.add(value);
            }
        } else if (rhs.equals(BODY)) {
            wholeBody.add(value);
        } else {
            body.put(rhs, value);
        }
    }

    private void validateRhs(String nodeName, String rhs, MiniGraph graph) {
        if (rhs.startsWith(OUTPUT_ARRAY) || rhs.startsWith(OUTPUT_NAMESPACE) || rhs.startsWith(MODEL_NAMESPACE)) {
            return;
        }
        var parts = util.split(rhs, ".[]");
        if (rhs.startsWith(".") || parts.size() < 2) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " - invalid RHS ("+rhs+")");
        }
        var node = graph.findNodeByAlias(parts.getFirst());
        if (node == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " - RHS node '"+
                                                parts.getFirst()+"' does not exist");
        }
        if (RESERVED_PARAMETERS.contains(parts.get(1))) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " - invalid RHS ("+rhs+
                                                "), '"+parts.get(1)+"' is a reserved property");
        }
    }

    protected int countExecuteStatements(String nodeName, List<String> statements) {
        var execute = 0;
        var js = 0;
        var error = 0;
        for (var entry : statements) {
            var line = entry.trim().toLowerCase();
            if (line.startsWith(IF_TAG) || line.startsWith(COMPUTE_TAG)) {
                js++;
            } else if (line.startsWith(EXECUTE_TAG)) {
                execute++;
                js++;
            } else if (!line.startsWith(MAPPING_TAG)) {
                error++;
            }
        }
        if (js == 0) {
            throw new IllegalArgumentException(NODE_NAME + nodeName +
                    " must include 'IF:', 'COMPUTE:' or 'EXECUTE:' statements");
        }
        if (error > 0) {
            throw new IllegalArgumentException(NODE_NAME + nodeName +
                    " must use 'IF:', 'COMPUTE:', 'EXECUTE:' or 'MAPPING:' statements");
        }
        return execute;
    }

    protected String getFirstWord(String statement) {
        var space = statement.indexOf(' ');
        return space == -1? statement : statement.substring(0, space);
    }

    protected String getIfStatement(List<String> lines) {
        boolean found = false;
        var sb = new StringBuilder();
        for (String line : lines) {
            var lc = line.toLowerCase().trim();
            if (found) {
                if (lc.startsWith(THEN_TAG) || lc.startsWith(ELSE_TAG)) {
                    break;
                } else {
                    sb.append(line).append(' ');
                }
            }
            if (lc.startsWith(IF_TAG)) {
                sb.append(line.substring(3)).append(' ');
                found = true;
            }
        }
        return sb.toString().trim();
    }

    protected String getThenStatement(List<String> lines) {
        boolean found = false;
        var sb = new StringBuilder();
        for (String line : lines) {
            var lc = line.toLowerCase().trim();
            if (found) {
                if (lc.startsWith(IF_TAG) || lc.startsWith(ELSE_TAG)) {
                    break;
                } else {
                    sb.append(line).append(' ');
                }
            }
            if (lc.startsWith(THEN_TAG)) {
                sb.append(line.substring(5)).append(' ');
                found = true;
            }
        }
        return sb.toString().trim();
    }

    protected String getElseStatement(List<String> lines) {
        boolean found = false;
        var sb = new StringBuilder();
        for (String line : lines) {
            var lc = line.toLowerCase().trim();
            if (found) {
                if (lc.startsWith(IF_TAG) || lc.startsWith(THEN_TAG)) {
                    break;
                } else {
                    sb.append(line).append(' ');
                }
            }
            if (lc.startsWith(ELSE_TAG)) {
                sb.append(line.substring(5)).append(' ');
                found = true;
            }
        }
        return sb.toString().trim();
    }

    protected String getNext(MiniGraph graph, String statement) {
        if (!NEXT.equalsIgnoreCase(statement)) {
            var nextNode = getNode(statement, graph);
            if (nextNode == null) {
                throw new IllegalArgumentException(NODE_NAME + statement + NOT_FOUND);
            } else {
                return statement;
            }
        }
        return null;
    }
}
