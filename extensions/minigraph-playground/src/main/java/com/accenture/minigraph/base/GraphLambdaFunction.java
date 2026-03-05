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

package com.accenture.minigraph.base;

import com.accenture.minigraph.models.GraphInstance;
import com.accenture.util.DataMappingHelper;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.models.*;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class GraphLambdaFunction implements TypedLambdaFunction<EventEnvelope, Object> {
    private static final Logger log = LoggerFactory.getLogger(GraphLambdaFunction.class);
    protected static final ConcurrentMap<String, MiniGraph> graphModels = new ConcurrentHashMap<>();
    protected static final ConcurrentMap<String, GraphInstance> graphInstances = new ConcurrentHashMap<>();
    protected static final DataMappingHelper helper = DataMappingHelper.getInstance();
    protected static final Utility util = Utility.getInstance();
    protected static final String ASYNC_HTTP_CLIENT = "async.http.request";
    protected static final String OPEN = "open";
    protected static final String CLOSE = "close";
    protected static final String COMMAND = "command";
    protected static final String UNTYPED = "untyped";
    protected static final String IN = "in";
    protected static final String OUT = "out";
    protected static final String NEXT = "next";
    protected static final String MAPPING = "mapping";
    protected static final String JS = "js";
    protected static final String STATEMENT = "statement";
    protected static final String FLOW_ID = "flow_id";
    protected static final String API_DOT = ".api.";
    protected static final String CACHED_DOT = ".cached.";
    protected static final String RESPONSE_DOT = ".response.";
    protected static final String RESULT_DOT = ".result.";
    protected static final String URL =  "url";
    protected static final String METHOD = "method";
    protected static final String HEADER = "header";
    protected static final String FEATURE = "feature";
    protected static final String QUESTION = "question";
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
    protected static final String MODEL_TTL = "model.ttl";
    protected static final String STATUS = "status";
    protected static final String ERROR = "error";
    private static final Set<String> RESERVED_PARAMETERS = Set.of(SKILL, MAPPING, STATEMENT, QUESTION);

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

    protected String getJsWithParameters(String text, MultiLevelMap stateMachine, boolean logical) {
        var start = 0;
        var sb = new StringBuilder();
        var segments = util.extractSegments(text, "{", "}");
        for (var segment : segments) {
            start = replaceWithParameter(segment, sb, start, text, stateMachine, logical);
        }
        var lastSegment = text.substring(start);
        if (!lastSegment.isEmpty()) {
            sb.append(lastSegment);
        }
        return sb.toString();
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
            } else if (parameter instanceof Number) {
                sb.append(parameter);
            } else if (parameter instanceof Map || parameter instanceof List) {
                var value = SimpleMapper.getInstance().getCompactGson().toJson(parameter);
                sb.append(value);
            } else {
                if (logical) {
                    sb.append("'").append(parameter).append("'");
                } else {
                    sb.append(parameter);
                }
            }
        }
        return segment.end();
    }

    protected void handleDataMappingEntry(String nodeName, String command, GraphInstance graphInstance) {
        int sep = command.indexOf(MAP_TO);
        if (sep > 0) {
            var stateMachine = graphInstance.stateMachine;
            var lhs = command.substring(0, sep).trim();
            var rhs = command.substring(sep + MAP_TO.length()).trim();
            var value = helper.getLhsOrConstant(lhs, stateMachine);
            if (!validRhs(rhs, graphInstance.graph)) {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " has invalid mapping '"+command+"'");
            }
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
                    if (!kv.getKey().equals(MAPPING) && !kv.getKey().equals(STATEMENT)) {
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

    protected void fillApiParameters(String nodeName, String command, GraphInstance graphInstance) {
        int sep = command.indexOf(MAP_TO);
        if (sep > 0) {
            var stateMachine = graphInstance.stateMachine;
            var lhs = command.substring(0, sep).trim();
            var rhs = command.substring(sep + MAP_TO.length()).trim();
            var value = helper.getLhsOrConstant(lhs, stateMachine);
            var target = rhs.startsWith(MODEL_NAMESPACE)? rhs : nodeName + API_DOT + rhs;
            if (value != null) {
                stateMachine.setElement(target, value);
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

    protected void mapHttpInput(AsyncHttpRequest request, String nodeName,
                                MultiLevelMap stateMachine, List<String> mapping) {
        var body = new HashMap<String, Object>();
        Object wholeBody = null;
        for (var entry : mapping) {
            int sep = entry.lastIndexOf(MAP_TO);
            if (sep != -1) {
                var lhs = entry.substring(0, sep).trim();
                var rhs = entry.substring(sep + MAP_TO.length()).trim();
                var source = nodeName + API_DOT + lhs;
                var value = helper.getLhsOrConstant(source, stateMachine);
                if (value != null) {
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
                            wholeBody = value;
                        }
                    } else if (rhs.equals(BODY)) {
                        wholeBody = value;
                    } else {
                        body.put(rhs, value);
                    }
                }
            }
        }
        request.setBody(wholeBody != null? wholeBody : body);
    }

    private boolean validRhs(String rhs, MiniGraph graph) {
        if (rhs.startsWith(OUTPUT_ARRAY) || rhs.startsWith(OUTPUT_NAMESPACE) || rhs.startsWith(MODEL_NAMESPACE)) {
            return true;
        }
        if (rhs.startsWith(".") || !rhs.contains(".")) {
            return false;
        }
        var parts = util.split(rhs, ".[]");
        if (parts.size() < 2) {
            return false;
        }
        var node = graph.findNodeByAlias(parts.getFirst());
        return node != null && !RESERVED_PARAMETERS.contains(parts.get(1));
    }
}
