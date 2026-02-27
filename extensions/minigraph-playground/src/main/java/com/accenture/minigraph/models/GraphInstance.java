package com.accenture.minigraph.models;

import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.util.MultiLevelMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GraphInstance {
    public final MiniGraph graph = new MiniGraph();
    public final MultiLevelMap stateMachine = new MultiLevelMap();
    public final ConcurrentMap<String, Boolean> hasSeen = new ConcurrentHashMap<>();
}
