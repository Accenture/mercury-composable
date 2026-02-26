package com.accenture.minigraph.skills;

import com.accenture.minigraph.base.GraphLambdaFunction;
import org.platformlambda.core.annotations.PreLoad;

import java.util.Map;

@PreLoad(route = "graph.island", instances=100)
public class GraphIsland extends GraphLambdaFunction {

    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        return SINK;
    }
}
