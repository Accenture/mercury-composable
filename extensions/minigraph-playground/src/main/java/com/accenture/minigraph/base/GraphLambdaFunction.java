package com.accenture.minigraph.base;

import com.accenture.minigraph.models.GraphInstance;
import com.accenture.util.DataMappingHelper;
import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;

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
    protected static final String MESSAGE = "message";
    protected static final String TYPE = "type";
    protected static final String MARKDOWN = "markdown";
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
    protected static final String JS = "js";

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
}
