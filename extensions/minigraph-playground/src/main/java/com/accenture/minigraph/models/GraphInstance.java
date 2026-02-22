package com.accenture.minigraph.models;

import org.platformlambda.core.graph.MiniGraph;
import org.platformlambda.core.util.MultiLevelMap;

import java.util.concurrent.locks.ReentrantLock;

public class GraphInstance {
    public final MiniGraph graph = new MiniGraph();
    public final MultiLevelMap stateMachine = new MultiLevelMap();
    public static final ReentrantLock lock = new ReentrantLock();
}
