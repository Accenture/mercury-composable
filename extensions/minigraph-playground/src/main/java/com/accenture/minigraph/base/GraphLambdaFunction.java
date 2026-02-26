package com.accenture.minigraph.base;

import com.accenture.minigraph.models.GraphInstance;
import com.accenture.util.DataMappingHelper;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.models.VarSegment;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class GraphLambdaFunction implements TypedLambdaFunction<Map<String, Object>, Object> {
    protected static final ConcurrentMap<String, MiniGraph> graphModels = new ConcurrentHashMap<>();
    protected static final ConcurrentMap<String, GraphInstance> graphInstances = new ConcurrentHashMap<>();
    protected static final DataMappingHelper helper = DataMappingHelper.getInstance();
    protected static final Utility util = Utility.getInstance();
    protected static final String OPEN = "open";
    protected static final String CLOSE = "close";
    protected static final String COMMAND = "command";
    protected static final String IN = "in";
    protected static final String OUT = "out";
    protected static final String NEXT = "next";
    protected static final String MAPPING = "mapping";
    protected static final String JS = "js";
    protected static final String DECISION = "decision";
    protected static final String FLOW_ID = "flow_id";
    protected static final String API_DOT = ".api.";
    protected static final String RESULT_DOT = ".result.";
    protected static final String QUESTION = "question";
    protected static final String MAP_TO = "->";
    protected static final String INPUT_BODY_NAMESPACE = "input.body";
    protected static final String INPUT_HEADER_NAMESPACE = "input.header";
    protected static final String MODEL_NAMESPACE = "model.";
    protected static final String OUTPUT_NAMESPACE = "output.";
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
    protected static final String SKILL_NAME = "skill ";
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

    protected GraphInstance getGraphInstance(String id) {
        var instance = graphInstances.get(id);
        if (instance == null) {
            throw new IllegalArgumentException("Graph instance " + id + NOT_FOUND);
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

    protected String getJsWithParameters(String text, MultiLevelMap stateMachine) {
        var start = 0;
        var sb = new StringBuilder();
        var segments = util.extractSegments(text, "{", "}");
        for (var segment : segments) {
            start = replaceWithParameter(segment, sb, start, text, stateMachine);
        }
        var lastSegment = text.substring(start);
        if (!lastSegment.isEmpty()) {
            sb.append(lastSegment);
        }
        return sb.toString();
    }

    private int replaceWithParameter(VarSegment segment, StringBuilder sb, int start, String text,
                                     MultiLevelMap stateMachine) {
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
                sb.append("'").append(parameter).append("'");
            }
        }
        return segment.end();
    }
}
