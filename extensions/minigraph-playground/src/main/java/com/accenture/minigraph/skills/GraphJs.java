package com.accenture.minigraph.skills;

import org.platformlambda.core.annotations.KernelThreadRunner;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.List;
import java.util.Map;

import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.graalvm.polyglot.Context;

@KernelThreadRunner
@PreLoad(route = "graph.js", instances=50)
public class GraphJs implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final Logger log = LoggerFactory.getLogger(GraphJs.class);
    private static final String JS = "js";
    private static final Utility util = Utility.getInstance();

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) throws Exception {
        var text = input.get(JS);
        if (text instanceof String script) {
            return execute(script);
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
