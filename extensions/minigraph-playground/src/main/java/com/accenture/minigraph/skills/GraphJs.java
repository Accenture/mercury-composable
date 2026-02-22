package com.accenture.minigraph.skills;

import com.accenture.minigraph.base.GraphLambdaFunction;
import org.platformlambda.core.annotations.KernelThreadRunner;
import org.platformlambda.core.annotations.PreLoad;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.graalvm.polyglot.Context;

@KernelThreadRunner
@PreLoad(route = GraphJs.ROUTE, instances=50)
public class GraphJs extends GraphLambdaFunction {
    public static final String ROUTE = "graph.js";
    private static final Logger log = LoggerFactory.getLogger(GraphJs.class);
    private static String skillDoc;

    public GraphJs() throws IOException {
        var filename = SKILL_PREFIX + GraphJs.ROUTE.replace('.', '-') + MARKDOWN_EXT;
        try (var in = GraphJs.class.getResourceAsStream(filename)) {
            GraphJs.skillDoc = in == null? "Did you forget to add "+filename+"?" : util.stream2str(in);
        }
    }

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) throws Exception {
        var type = headers.get(TYPE);
        if (JS.equals(type) && input.get(JS) instanceof String script) {
            return execute(script);
        }
        if (MARKDOWN.equals(type)) {
            return GraphJs.skillDoc;
        }
        if (EXECUTE.equals(type)) {
            log.info("1--------{}", headers);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object execute(String script) {
        try (Context context = Context.create(JS)) {
            var result = context.eval(JS, script).as(Object.class);
            if (result instanceof List<?> list) {
                return util.deepCopy((List<Object>) list);
            } else if (result instanceof Map<?, ?> map) {
                return util.deepCopy((Map<String, Object>) map);
            } else if (isPrimitive(result)) {
                return result;
            } else {
                return String.valueOf(result);
            }
        }
    }

    private boolean isPrimitive(Object obj) {
        return (obj instanceof String || obj instanceof byte[] || obj instanceof Number || obj instanceof Boolean);
    }
}
