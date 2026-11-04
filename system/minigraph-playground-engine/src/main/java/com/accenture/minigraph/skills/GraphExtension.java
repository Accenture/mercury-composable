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

package com.accenture.minigraph.skills;

import com.accenture.automation.EventScriptManager;
import com.accenture.minigraph.common.GraphLambdaFunction;
import com.accenture.minigraph.models.CompiledGraphs;
import com.accenture.minigraph.models.GraphInstance;
import com.accenture.minigraph.progress.ReturnChannel;
import com.accenture.models.Flows;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;

@PreLoad(route = GraphExtension.ROUTE, instances=300)
public class GraphExtension extends GraphLambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(GraphExtension.class);
    public static final String ROUTE = "graph.extension";
    private static final String GRAPH_EXECUTOR = "graph-executor";
    /** The node property that starts the target and returns at once (RFC-0011 section 8.1). */
    public static final String ASYNC = "async";
    // an asynchronous run is waited on (to start the next element of a fan-out and to log its end)
    // for its own TTL plus this margin; the target answers 408 itself when its TTL expires
    private static final long ASYNC_MARGIN_MS = 5000;
    private static final int DEFAULT_CONCURRENCY = 3;
    private static final int MAX_CONCURRENCY = 30;

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance)
                                throws ExecutionException, InterruptedException {
        if (!EXECUTE.equals(headers.get(TYPE))) {
            throw new IllegalArgumentException("Type must be EXECUTE");
        }
        var po = PostOffice.trackable(headers, instance);
        var nodeName = headers.getOrDefault(NODE, "none");
        po.annotateTrace(NODE, nodeName);
        var in = headers.get(IN);
        var graphInstance = getGraphInstance(in);
        var stateMachine = graphInstance.stateMachine;
        var node = getNode(nodeName, graphInstance.graph);
        if (!ROUTE.equals(node.getProperty(SKILL))) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have skill - "+ROUTE);
        }
        var graphId = node.getProperty(EXTENSION) instanceof String id? id : null;
        if (graphId == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have graph ID");
        }
        // reset result to ensure execution is idempotent
        stateMachine.removeElement(nodeName + "." + RESULT);
        stateMachine.removeElement(nodeName + "." + HEADER);
        var async = isAsync(node);
        var forEach = getEntries(node.getProperty(FOR_EACH));
        if (forEach.isEmpty()) {
            return async? startExtension(po, node, graphInstance, graphId) :
                            callExtension(po, node, graphInstance, graphId);
        }
        Map<String, List<Object>> forEachMapping = getForEachMapping(nodeName, forEach, stateMachine);
        // iterative API requests with an array of parameters
        if (forEachMapping.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName +
                    " - No data mapping resolved from 'for_each' entries. LHS must be a list.");
        }
        var mapping = getEntries(node.getProperty(INPUT));
        var size = getModelArraySize(forEachMapping);
        for (int i = 0; i < size; i++) {
            var x = getNextModelParamSet(forEachMapping, i);
            for (var kv : x.entrySet()) {
                stateMachine.setElement(kv.getKey(), kv.getValue());
            }
            for (var entry : mapping) {
                fillFetcherApiParameters(nodeName, entry, graphInstance, true);
            }
        }
        if (async) {
            return startExtensions(po, graphInstance, node, graphId, size);
        }
        callExtensionWithForkJoin(po, graphInstance, node, graphId, size);
        return nextPath(stateMachine, node);
    }

    /**
     * Whether a node starts its target and returns at once.
     *
     * @param node a {@code graph.extension} node
     * @return true when the node carries {@code async=true}
     */
    public static boolean isAsync(SimpleNode node) {
        return "true".equals(String.valueOf(node.getProperty(ASYNC)));
    }

    private void callExtensionWithForkJoin(PostOffice po, GraphInstance graphInstance,
                                             SimpleNode node, String extension, int size)
                                                throws ExecutionException, InterruptedException {
        var nodeName = node.getAlias();
        var concurrency = concurrencyOf(node);
        var timeout = getEffectiveTtl(graphInstance, node);
        var stateMachine = graphInstance.stateMachine;
        if (extension.startsWith(FLOW_PROTOCOL) && Flows.getFlow(extension.substring(FLOW_PROTOCOL.length())) == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " -  "+extension+" does not exist");
        }
        Deque<EventEnvelope> stack = new ArrayDeque<>();
        var returnPath = returnPathFor(graphInstance, node);
        // release one set of parameters from the array for each data dictionary item
        for (var parameters : forEachParameters(stateMachine, nodeName, size)) {
            // the iteration's position rides as a header, exactly as the business cid does:
            // it must not go in BODY, which is the author's own input-mapping contract. The
            // subgraph's suspend/resume key includes it, so N iterations of one subgraph no
            // longer collide on one record. Only the for_each path sets it - a single
            // delegation has no index and keeps its existing store key.
            stack.add(forwardEvent(graphInstance, extension, parameters, stack.size(), timeout, returnPath));
        }
        runConcurrentRequests(po, graphInstance, node, extension, concurrency, stack, timeout);
        var outputMapping = getEntries(node.getProperty(OUTPUT));
        performOutputMapping(nodeName, stateMachine, outputMapping);
        // clear temporary dataset
        stateMachine.removeElement(nodeName + FETCH);
        stateMachine.removeElement(nodeName + EACH);
    }

    private void runConcurrentRequests(PostOffice po, GraphInstance graphInstance, SimpleNode node,
                                       String extension, int concurrency, Deque<EventEnvelope> stack, long timeout)
                                        throws ExecutionException, InterruptedException {
        po.annotateTrace(EXTENSION, extension);
        var nameLoaded = false;
        var parameterNames = new HashSet<String>();
        var nodeName = node.getAlias();
        var stateMachine = graphInstance.stateMachine;
        List<EventEnvelope> batch = new ArrayList<>();
        var n = concurrency;
        while (!stack.isEmpty()) {
            n--;
            batch.add(stack.pop());
            if (stack.isEmpty() || n == 0) {
                n = concurrency;
                var body = batch.getFirst().getBody() instanceof Map<?, ?> map? map : Map.of();
                if (!nameLoaded) {
                    nameLoaded = true;
                    var names = body.get(BODY) instanceof Map<?, ?> map? map.keySet() : Set.of();
                    names.forEach(name -> parameterNames.add(String.valueOf(name)));
                    po.annotateTrace(FOR_EACH, String.valueOf(parameterNames));
                }
                log.info("Call extension {}, for each {}, parallel={}, ttl={}", extension,
                        parameterNames, batch.size(), timeout);
                stateMachine.setElement(nodeName + "." + TARGET, extension);
                doForkJoin(po, node, stateMachine, batch, timeout);
                batch.clear();
            }
        }
    }

    private void doForkJoin(PostOffice po, SimpleNode node, MultiLevelMap stateMachine,
                            List<EventEnvelope> batch, long timeout)
                            throws ExecutionException, InterruptedException {
        var nodeName = node.getAlias();
        var responses = po.request(batch, timeout, false).get();
        for (var response : responses) {
            stateMachine.setElement(nodeName + "." + STATUS, response.getStatus());
            if (!response.getHeaders().isEmpty()) {
                stateMachine.setElement(nodeName + "." + HEADER + "[]", response.getHeaders());
            }
            if (response.hasError()) {
                setError(stateMachine, node, response);
            } else {
                stateMachine.setElement(nodeName + "." + RESULT + "[]", response.getBody());
            }
        }
    }

    private Object callExtension(PostOffice po, SimpleNode node, GraphInstance graphInstance, String graphId) {
        var nodeName = node.getAlias();
        var mapping = getEntries(node.getProperty(INPUT));
        if (mapping.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have 'input' entries");
        }
        for (var entry : mapping) {
            fillFetcherApiParameters(nodeName, entry, graphInstance, false);
        }
        var timeout = getEffectiveTtl(graphInstance, node);
        return retrieveFromExtension(po, node, graphInstance, graphId, timeout);
    }

    private Object retrieveFromExtension(PostOffice po, SimpleNode node, GraphInstance graphInstance,
                                         String extension, long ttl) {
        log.info("Call extension {}, ttl={}", extension, ttl);
        po.annotateTrace(EXTENSION, extension);
        var nodeName = node.getAlias();
        var stateMachine = graphInstance.stateMachine;
        stateMachine.setElement(nodeName + "." + TARGET, extension);
        // construct dataset
        if (extension.startsWith(FLOW_PROTOCOL) && Flows.getFlow(extension.substring(FLOW_PROTOCOL.length())) == null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " -  "+extension+" does not exist");
        }
        var forward = forwardEvent(graphInstance, extension, fetchedParameters(stateMachine, nodeName), null, ttl,
                returnPathFor(graphInstance, node));
        // clean up working area
        stateMachine.removeElement(nodeName + FETCH);
        // issue the request on the worker thread so the outbound event carries this span
        // as the delegated call's parent (the trace context is thread-keyed and would be
        // gone inside the Mono callback)
        var delegated = po.eRequest(forward, ttl, false);
        // guardedCompletion: a throw inside this callback (e.g. an invalid output data mapping)
        // must surface as the node's error, never leave the Mono pending
        return guardedCompletion(delegated, response -> {
            stateMachine.setElement(nodeName + "." + STATUS, response.getStatus());
            if (!response.getHeaders().isEmpty()) {
                stateMachine.setElement(nodeName + "." + HEADER, response.getHeaders());
            }
            if (response.hasError()) {
                return setError(stateMachine, node, response);
            } else {
                stateMachine.setElement(nodeName + "." + RESULT, response.getBody());
                var outputMapping = getEntries(node.getProperty(OUTPUT));
                performOutputMapping(nodeName, stateMachine, outputMapping);
                return NEXT;
            }
        });
    }

    /**
     * Start the target with the node's input mapping and return at once (RFC-0011 section 8.1). Nothing comes
     * back: the target runs under its own TTL, and only a failure to start reaches this node's exception route.
     */
    private Object startExtension(PostOffice po, SimpleNode node, GraphInstance graphInstance, String extension)
                                    throws ExecutionException, InterruptedException {
        var nodeName = node.getAlias();
        var mapping = getEntries(node.getProperty(INPUT));
        if (mapping.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have 'input' entries");
        }
        for (var entry : mapping) {
            fillFetcherApiParameters(nodeName, entry, graphInstance, false);
        }
        var stateMachine = graphInstance.stateMachine;
        var parameters = fetchedParameters(stateMachine, nodeName);
        stateMachine.removeElement(nodeName + FETCH);
        return startRuns(po, graphInstance, node, extension, List.of(parameters), false);
    }

    /**
     * Start one run of the target per element of the {@code for_each} list and return at once. The runs go
     * in the background, at most {@code concurrency} of them active at a time.
     */
    private Object startExtensions(PostOffice po, GraphInstance graphInstance, SimpleNode node,
                                   String extension, int size) throws ExecutionException, InterruptedException {
        var nodeName = node.getAlias();
        var stateMachine = graphInstance.stateMachine;
        var elements = forEachParameters(stateMachine, nodeName, size);
        stateMachine.removeElement(nodeName + FETCH);
        stateMachine.removeElement(nodeName + EACH);
        return startRuns(po, graphInstance, node, extension, elements, true);
    }

    private Object startRuns(PostOffice po, GraphInstance graphInstance, SimpleNode node, String extension,
                             List<Map<String, Object>> elements, boolean indexed)
                                throws ExecutionException, InterruptedException {
        var nodeName = node.getAlias();
        var stateMachine = graphInstance.stateMachine;
        stateMachine.setElement(nodeName + "." + TARGET, extension);
        po.annotateTrace(EXTENSION, extension);
        po.annotateTrace(ASYNC, String.valueOf(elements.size()));
        var targetTtl = targetTtl(extension);
        if (targetTtl.isEmpty()) {
            return startFailure(stateMachine, node, 404, NODE_NAME + nodeName + " - " + extension + " does not exist");
        }
        // the node's own ttl, when it declares one, is the target's TTL; otherwise the target's own applies
        Long nodeTtl = node.getProperty(TTL) == null? null : getEffectiveTtl(graphInstance, node);
        var runs = new ArrayList<EventEnvelope>();
        for (var parameters : elements) {
            // an asynchronous target reports to nobody: it never inherits this run's return path
            runs.add(forwardEvent(graphInstance, extension, parameters, indexed? runs.size() : null, nodeTtl, null));
        }
        var refused = refusedInput(po, graphInstance, extension, runs, indexed);
        if (refused.isPresent()) {
            return startFailure(stateMachine, node, refused.get().getStatus(), refused.get().getError());
        }
        // a run started after this node returned has no trace context on its thread, so each run carries
        // this node's span as its parent explicitly
        var trace = po.getTrace();
        if (trace != null && trace.spanId != null) {
            runs.forEach(run -> run.setSpanId(trace.spanId));
        }
        var ttl = nodeTtl != null? nodeTtl : targetTtl.get();
        var concurrency = indexed? concurrencyOf(node) : 1;
        log.info("Start extension {}, async, runs={}, concurrency={}, ttl={}", extension, runs.size(), concurrency, ttl);
        new BackgroundRuns(po, extension, runs, ttl + ASYNC_MARGIN_MS).start(concurrency);
        return NEXT;
    }

    /**
     * The TTL a target runs under when the node declares none: a flow's own, and for a deployed graph the
     * TTL of the flow that runs graphs. Empty when the target does not exist.
     */
    private Optional<Long> targetTtl(String extension) {
        if (extension.startsWith(FLOW_PROTOCOL)) {
            var flow = Flows.getFlow(extension.substring(FLOW_PROTOCOL.length()));
            return flow == null? Optional.empty() : Optional.of(flow.ttl);
        }
        var executor = Flows.getFlow(GRAPH_EXECUTOR);
        return executor == null || !CompiledGraphs.graphExists(extension)? Optional.empty() : Optional.of(executor.ttl);
    }

    /**
     * A deployed graph with a contract validates its input at its root. An asynchronous run cannot tell this
     * node, so the input of every run is validated here before any run starts, the way the target's root
     * will validate it: a refused input is a failure to start.
     */
    @SuppressWarnings("unchecked")
    private Optional<EventEnvelope> refusedInput(PostOffice po, GraphInstance graphInstance, String extension,
                                                 List<EventEnvelope> runs, boolean indexed)
                                                    throws ExecutionException, InterruptedException {
        if (extension.startsWith(FLOW_PROTOCOL) || !(rootSchema(extension) instanceof Map<?, ?> schema)) {
            return Optional.empty();
        }
        var route = getSchemaValidatorRoute();
        if (!po.exists(route) && po.getEventHttpTarget(route) == null) {
            return Optional.of(new EventEnvelope().setStatus(500)
                                .setBody("Schema validator '" + route + "' does not exist"));
        }
        for (int i = 0; i < runs.size(); i++) {
            var dataset = (Map<String, Object>) runs.get(i).getBody();
            var request = new EventEnvelope().setTo(route).setCorrelationId(util.getUuid())
                    .setBody(Map.of(BODY, dataset.get(BODY), HEADER, dataset.get(HEADER), SCHEMA, schema));
            var response = po.eRequest(request, getModelTtl(graphInstance), false).get();
            if (response.hasError()) {
                var reason = response.getError();
                return Optional.of(new EventEnvelope().setStatus(response.getStatus())
                        .setBody(indexed && reason instanceof String text? "for_each element " + i + " - " + text :
                                reason));
            }
        }
        return Optional.empty();
    }

    private static Object rootSchema(String graphId) {
        if (CompiledGraphs.getGraph(graphId) instanceof Map<?, ?> model && model.get("nodes") instanceof List<?> nodes) {
            for (var item : nodes) {
                if (item instanceof Map<?, ?> n && ROOT.equals(n.get("alias")) && n.get("properties") instanceof Map<?, ?> p) {
                    return p.get(SCHEMA);
                }
            }
        }
        return null;
    }

    private String startFailure(MultiLevelMap stateMachine, SimpleNode node, int status, Object message) {
        stateMachine.setElement(node.getAlias() + "." + STATUS, status);
        return setError(stateMachine, node, new EventEnvelope().setStatus(status).setBody(message));
    }

    private int concurrencyOf(SimpleNode node) {
        var given = util.str2int(String.valueOf(node.getProperty(CONCURRENCY)));
        return Math.clamp(given < 0? DEFAULT_CONCURRENCY : given, 1, MAX_CONCURRENCY);
    }

    private static Map<String, Object> fetchedParameters(MultiLevelMap stateMachine, String nodeName) {
        var parameters = stateMachine.getElement(nodeName + FETCH);
        var result = new HashMap<String, Object>();
        if (parameters instanceof Map<?, ?> map) {
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> forEachParameters(MultiLevelMap stateMachine, String nodeName, int size) {
        var apiParams = (Map<String, List<Object>>) stateMachine.getElement(nodeName + EACH, new HashMap<>());
        var result = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < size; i++) {
            var parameters = new HashMap<String, Object>();
            for (var kv : apiParams.entrySet()) {
                parameters.put(kv.getKey(), kv.getValue().get(i));
            }
            result.add(parameters);
        }
        return result;
    }

    /**
     * The event that runs a target: a flow, or a deployed graph through the flow that runs graphs.
     *
     * @param graphInstance the calling graph
     * @param extension the target
     * @param parameters the target's input body
     * @param index the element's position in a for_each list, or null
     * @param ttl the target's TTL in milliseconds, or null for the target's own
     * @param returnPath the relay a synchronous subgraph returns its progress events through, or null
     * @return the event to send to the event script manager
     */
    private EventEnvelope forwardEvent(GraphInstance graphInstance, String extension, Map<String, Object> parameters,
                                       Integer index, Long ttl, String returnPath) {
        var forward = new EventEnvelope().setTo(EventScriptManager.SERVICE_NAME).setCorrelationId(util.getUuid());
        inheritBusinessCid(forward, graphInstance);
        var dataset = new HashMap<String, Object>();
        dataset.put(BODY, parameters);
        if (ttl != null) {
            dataset.put(TTL, ttl);
        }
        if (extension.startsWith(FLOW_PROTOCOL)) {
            dataset.put(HEADER, Map.of());
            forward.setHeader(FLOW_ID, extension.substring(FLOW_PROTOCOL.length()));
        } else {
            var header = new HashMap<String, Object>();
            if (index != null) {
                header.put(ITERATION_INDEX_HEADER, String.valueOf(index));
            }
            // a subgraph names itself in its events, which reach this run's caller through this node's relay
            if (returnPath != null) {
                header.put(ReturnChannel.X_RETURN_PATH, returnPath);
            }
            dataset.put(HEADER, header);
            dataset.put(PATH_PARAMETER, Map.of(GRAPH_ID, extension));
            forward.setHeader(FLOW_ID, GRAPH_EXECUTOR);
        }
        return forward.setBody(dataset);
    }

    /**
     * The runs of an asynchronous node after it returned: a sliding window that keeps at most
     * {@code concurrency} runs active and starts the next as an earlier one ends. Each run's end is logged
     * with its status, the only record of an outcome nobody waits for.
     */
    private static final class BackgroundRuns {
        private final PostOffice po;
        private final String extension;
        private final ConcurrentLinkedDeque<Run> pending = new ConcurrentLinkedDeque<>();
        private final int total;
        private final long waitMs;

        private record Run(int position, EventEnvelope event) { }

        BackgroundRuns(PostOffice po, String extension, List<EventEnvelope> runs, long waitMs) {
            this.po = po;
            this.extension = extension;
            var position = 0;
            for (var event : runs) {
                pending.add(new Run(++position, event));
            }
            this.total = runs.size();
            this.waitMs = waitMs;
        }

        void start(int concurrency) {
            for (int i = 0; i < concurrency; i++) {
                next();
            }
        }

        private void next() {
            var run = pending.poll();
            if (run == null) {
                return;
            }
            var begin = System.currentTimeMillis();
            try {
                po.eRequest(run.event(), waitMs, false).whenComplete((response, e) -> {
                    ended(run.position(), begin, response, e);
                    next();
                });
            } catch (RuntimeException e) {
                ended(run.position(), begin, null, e);
                next();
            }
        }

        private void ended(int position, long begin, EventEnvelope response, Throwable e) {
            var elapsed = System.currentTimeMillis() - begin;
            if (e != null) {
                log.warn("Extension {} run {} of {} not completed in {} ms - {}", extension, position, total, elapsed,
                        e.getMessage());
            } else if (response.hasError()) {
                log.warn("Extension {} run {} of {} ended with status {} in {} ms - {}", extension, position, total,
                        response.getStatus(), elapsed, response.getError());
            } else {
                log.info("Extension {} run {} of {} ended with status {} in {} ms", extension, position, total,
                        response.getStatus(), elapsed);
            }
        }
    }

    private void inheritBusinessCid(EventEnvelope forward, GraphInstance graphInstance) {
        // the child flow or graph inherits the parent's business correlation-id (model.cid),
        // exactly like an Event Script sub-flow launch. Therefore, a subgraph that suspends can be
        // resumed under the same business transaction id (its store record is keyed by
        // graph + cid, never by a per-call random id)
        if (graphInstance.stateMachine.getElement(MODEL_CID) instanceof String businessCid
                && !businessCid.isBlank()) {
            forward.setHeader(EventScriptManager.BUSINESS_CORRELATION_ID, businessCid.trim());
        }
    }

    private String setError(MultiLevelMap stateMachine, SimpleNode node, EventEnvelope response) {
        var nodeName = node.getAlias();
        stageNodeError(stateMachine, nodeName, response);
        var errorHandler = node.getProperty(EXCEPTION);
        if (errorHandler == null) {
            stateMachine.setElement(OUTPUT_BODY, response.getBody());
            stateMachine.setElement(OUTPUT_NAMESPACE+HEADER, response.getHeaders());
            stateMachine.setElement(OUTPUT_NAMESPACE+STATUS, response.getStatus());
            return NEXT;
        } else {
            return String.valueOf(errorHandler);
        }
    }

    private String nextPath(MultiLevelMap stateMachine, SimpleNode fetcher) {
        var nodeName = fetcher.getAlias();
        var processStatus = stateMachine.getElement(nodeName + "." + STATUS);
        var resultError = stateMachine.getElement(nodeName + "." + ERROR);
        var errorHandler = fetcher.getProperty(EXCEPTION);
        if (processStatus instanceof Integer && resultError != null && errorHandler != null) {
            return String.valueOf(errorHandler);
        } else {
            return NEXT;
        }
    }
}
