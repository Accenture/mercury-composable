package com.accenture.minigraph.tasks;

import com.accenture.minigraph.base.GraphLambdaFunction;
import com.accenture.minigraph.models.GraphInstance;
import com.accenture.minigraph.skills.GraphJoin;
import com.accenture.models.Flows;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@PreLoad(route = GraphExecutor.ROUTE, instances=200)
public class GraphExecutor extends GraphLambdaFunction {
    public static final String ROUTE = "graph.executor";
    private static final Logger log = LoggerFactory.getLogger(GraphExecutor.class);
    private static final String FILE_PREFIX = "file:";
    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String DEFAULT_TEMP_DIR = "classpath:/graph";
    private static final int STATUS_CONTINUE = 100;
    private static final String DONE = "done";
    private final String graphLocation;

    public GraphExecutor() {
        var config = AppConfigReader.getInstance();
        var location = config.getProperty("location.graph.deployed", DEFAULT_TEMP_DIR);
        if (location.startsWith(FILE_PREFIX) || location.startsWith(CLASSPATH_PREFIX)) {
            this.graphLocation = location;
        } else {
            log.error("location.graph.temp must start with file:/ or classpath:/. Fallback to {}", DEFAULT_TEMP_DIR);
            this.graphLocation = DEFAULT_TEMP_DIR;
        }
        log.info("Deployed graph model folder (location.graph.deployed) - {}", this.graphLocation);
    }

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope event, int instance) throws InterruptedException {
        if (event.getReplyTo() != null && event.getCorrelationId() != null) {
            try {
                var graphInstance = runGraph(headers, event, instance);
                if (graphInstance.complete.get()) {
                    // send task completion signal to Flow's TaskExecutor
                    return new EventEnvelope().setStatus(STATUS_CONTINUE).setBody(DONE);
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (Exception e) {
                var rc = e instanceof AppException ex? ex.getStatus() : 400;
                if (rc != STATUS_CONTINUE || !DONE.equals(e.getMessage())) {
                    var po = PostOffice.trackable(headers, instance);
                    var error = new EventEnvelope().setTo(event.getReplyTo()).setStatus(rc).setBody(e.getMessage())
                            .setCorrelationId(event.getCorrelationId());
                    po.send(error);
                }
            }
        }
        throw new IllegalArgumentException("Graph instance does not resolve to an outcome");
    }

    @SuppressWarnings("unchecked")
    private GraphInstance runGraph(Map<String, String> headers, EventEnvelope event, int instance)
            throws ExecutionException, InterruptedException {
        var instanceId = headers.get("instance");
        if (instanceId == null) {
            throw new IllegalArgumentException("Missing instance ID in header");
        }
        var graphId = headers.get("graph");
        if (graphId == null) {
            throw new IllegalArgumentException("Missing graph ID in header");
        }
        var flowInstance = Flows.getFlowInstance(instanceId);
        if (flowInstance == null) {
            throw new IllegalArgumentException("Invalid flow instance " + instanceId);
        }
        flowInstance.setEndFlowListeners(GraphHousekeeper.ROUTE);
        String compositeCid = event.getCorrelationId();
        if (compositeCid == null) {
            throw new IllegalArgumentException("Missing correlation ID in header");
        }
        var map = getGraphModel(graphId);
        if (map.isEmpty()) {
            throw new IllegalArgumentException("Unable to load graph model '"+graphId+"' - missing or invalid");
        }
        GraphInstance graphInstance = new GraphInstance(graphId);
        var graph = graphInstance.graph;
        graph.importGraph(map);
        graphInstances.put(instanceId, graphInstance);
        var stateMachine = graphInstance.stateMachine;
        // make a copy of flow input and model to avoid accidentally changing the original values
        var inputCopy = util.deepCopy((Map<String, Object>) flowInstance.dataset.get(INPUT));
        var modelCopy = util.deepCopy((Map<String, Object>) flowInstance.dataset.get(MODEL));
        stateMachine.setElement(INPUT, inputCopy);
        stateMachine.setElement(MODEL, modelCopy);
        stateMachine.setElement(OUTPUT_BODY, "");
        // map node properties to state machine
        initializeWithNodeProperties(graphInstance);
        var root = graph.getRootNode();
        if (root == null) {
            throw new IllegalArgumentException("Root node does not exist");
        }
        var end = graph.getEndNode();
        if (end == null) {
            throw new IllegalArgumentException("End node does not exist");
        }
        var po = PostOffice.trackable(headers, instance);
        walk(po, instanceId, event, graphInstance, root, getModelTtl(graphInstance));
        return graphInstance;
    }

    private void walk(PostOffice po, String in, EventEnvelope event,
                      GraphInstance graphInstance, SimpleNode node, long timeout)
            throws ExecutionException, InterruptedException {
        if (!graphInstance.complete.get()) {
            var nodeName = node.getAlias();
            var properties = node.getProperties();
            String skill = properties.containsKey(SKILL) ? String.valueOf(properties.get(SKILL)) : null;
            var seen = graphInstance.hasSeen.get(nodeName);
            if (seen == null) {
                if (!GraphJoin.ROUTE.equals(skill)) {
                    graphInstance.hasSeen.put(nodeName, true);
                }
                walkTo(po, in, event, skill, graphInstance, node, timeout);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void walkTo(PostOffice po, String in, EventEnvelope event, String skill, GraphInstance graphInstance,
                        SimpleNode node, long timeout) throws ExecutionException, InterruptedException {
        var graph = graphInstance.graph;
        var endNode = graph.getEndNode();
        if (endNode.getId().equals(node.getId())) {
            if (skill != null) {
                execute(po, skill, in, event, graphInstance, node, timeout);
            }
            var body = graphInstance.stateMachine.getElement(OUTPUT_BODY_NAMESPACE);
            var hdr = graphInstance.stateMachine.getElement(OUTPUT_HEADER_NAMESPACE);
            var headers = hdr instanceof Map? (Map<String, Object>) hdr : new HashMap<String, Object>();
            var response = new EventEnvelope().setTo(event.getReplyTo()).setCorrelationId(event.getCorrelationId());
            for (Map.Entry<String, Object> kv : headers.entrySet()) {
                response.setHeader(kv.getKey(), kv.getValue());
            }
            po.send(response.setBody(body));
            graphInstance.complete.set(true);
        } else {
            if (skill == null) {
                walkNext(po, in, event, graphInstance, node, timeout);
            } else {
                execute(po, skill, in, event, graphInstance, node, timeout);
            }
        }
    }

    private void execute(PostOffice po, String skill, String in, EventEnvelope event, GraphInstance graphInstance,
                         SimpleNode node, long timeout) throws ExecutionException, InterruptedException {
        if (po.exists(skill)) {
            var nodeName = node.getAlias();
            var response = po.request(new EventEnvelope().setTo(skill).setHeader(IN, in)
                                        .setHeader(TYPE, EXECUTE).setHeader(NODE, nodeName), timeout).get();
            // check processing status
            var processStatus = graphInstance.stateMachine.getElement(nodeName+ PROCESS_STATUS);
            var resultError = graphInstance.stateMachine.getElement(nodeName+ RESULT_ERROR);
            if (processStatus instanceof Integer rc && resultError != null) {
                var error = new EventEnvelope().setTo(event.getReplyTo()).setCorrelationId(event.getCorrelationId())
                        .setBody(resultError).setStatus(rc);
                po.send(error);
                // tell executor that response has been sent
                graphInstance.complete.set(true);
                throw new AppException(STATUS_CONTINUE, DONE);
            }
            if (response.hasError()) {
                var error = new EventEnvelope().setTo(event.getReplyTo()).setCorrelationId(event.getCorrelationId())
                                .setBody(response.getBody()).setStatus(response.getStatus());
                po.send(error);
                // tell executor that response has been sent
                graphInstance.complete.set(true);
                throw new AppException(STATUS_CONTINUE, DONE);
            } else if (!graphInstance.complete.get()) {
                var graph = graphInstance.graph;
                var endNode = graph.getEndNode();
                if (!endNode.getId().equals(node.getId())) {
                    var next = String.valueOf(response.getBody());
                    nextOrJump(po, in, event, graphInstance, node, next, timeout);
                }
            }
        } else {
            throw new IllegalArgumentException("Skill " + skill + " does not exist");
        }
    }

    private void nextOrJump(PostOffice po, String in, EventEnvelope event, GraphInstance graphInstance, SimpleNode node,
                            String next, long timeout) throws ExecutionException, InterruptedException {
        if (!SINK.equals(next)) {
            var graph = graphInstance.graph;
            if (NEXT.equals(next)) {
                walkNext(po, in, event, graphInstance, node, timeout);
            } else {
                var nextNode = graph.findNodeByAlias(next);
                if (nextNode != null) {
                    walk(po, in, event, graphInstance, nextNode, timeout);
                } else {
                    throw new IllegalArgumentException("Next node '" + next + "' does not exist");
                }
            }
        }
    }

    private void walkNext(PostOffice po, String in, EventEnvelope event, GraphInstance graphInstance,
                          SimpleNode node, long timeout)
            throws ExecutionException, InterruptedException {
        if (!graphInstance.complete.get()) {
            var graph = graphInstance.graph;
            var nodes = graph.getForwardLinks(node.getAlias());
            for (SimpleNode next : nodes) {
                walk(po, in, event, graphInstance, next, timeout);
            }
        }
    }

    private Map<String, Object> getGraphModel(String graphId) {
        if (graphLocation.startsWith(FILE_PREFIX)) {
            return getGraphModelAsFile(graphId);
        }
        if (graphLocation.startsWith(CLASSPATH_PREFIX)) {
            return getGraphModelAsResource(graphId);
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getGraphModelAsFile(String graphId) {
        var folder = graphLocation.substring(FILE_PREFIX.length());
        var filePath = getNormalizedPath(folder, graphId);
        File f = new File(filePath);
        if (f.exists()) {
            return SimpleMapper.getInstance().getMapper().readValue(util.file2str(f), Map.class);
        } else {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getGraphModelAsResource(String graphId) {
        var folder = graphLocation.substring(CLASSPATH_PREFIX.length());
        var filePath = getNormalizedPath(folder, graphId);
        InputStream in = this.getClass().getResourceAsStream(filePath);
        if (in != null) {
            return SimpleMapper.getInstance().getMapper().readValue(util.stream2str(in), Map.class);
        } else {
            return Collections.emptyMap();
        }
    }

    private String getNormalizedPath(String folder, String graphId) {
        var sb = new StringBuilder();
        var parts = util.split(folder, "/");
        for (String part : parts) {
            sb.append('/').append(part);
        }
        sb.append('/').append(graphId);
        sb.append(JSON_EXT);
        return sb.toString();
    }
}
