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

import com.accenture.minigraph.exception.FetchException;
import com.accenture.minigraph.start.PlaygroundLoader;
import com.accenture.minigraph.common.FeatureDef;
import com.accenture.minigraph.common.GraphLambdaFunction;
import com.accenture.minigraph.models.GraphInstance;
import com.accenture.minigraph.models.HostUri;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.SimpleNode;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@PreLoad(route = GraphApiFetcher.ROUTE, instances=300)
public class GraphApiFetcher extends GraphLambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(GraphApiFetcher.class);
    public static final String ROUTE = "graph.api.fetcher";
    private static final Map<String, Long> UNSUPPORTED_FEATURES = new HashMap<>();
    private static final long WARNING_INTERVAL = 30000;

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance)
            throws URISyntaxException, ExecutionException, InterruptedException {
        if (!EXECUTE.equals(headers.get(TYPE))) {
            throw new IllegalArgumentException("Type must be EXECUTE");
        }
        var po = PostOffice.trackable(headers, instance);
        var nodeName = headers.getOrDefault(NODE, "none");
        po.annotateTrace(NODE, nodeName);
        var in = headers.get(IN);
        var graphInstance = getGraphInstance(in);
        var stateMachine = graphInstance.stateMachine;
        var fetcher = getNode(nodeName, graphInstance.graph);
        if (!ROUTE.equals(fetcher.getProperty(SKILL))) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have skill - "+ROUTE);
        }
        if ("true".equals(headers.get(LIVE))) {
            stateMachine.setElement(nodeName + "." + LIVE, true);
        }
        // reset result to ensure execution is idempotent
        stateMachine.removeElement(nodeName + "." + RESULT);
        stateMachine.removeElement(nodeName + "." + HEADER);
        var dictionary = getEntries(fetcher.getProperty(DICTIONARY));
        var dictionaryNodes = getDictionaryNodes(dictionary, nodeName, graphInstance);
        var forEach = getEntries(fetcher.getProperty(FOR_EACH));
        Map<String, List<?>> forEachMapping = getForEachMapping(nodeName, forEach, stateMachine);
        if (forEach.isEmpty()) {
            return executeProviders(po, graphInstance, fetcher, dictionaryNodes);
        }
        // iterative API requests with an array of parameters
        if (forEachMapping.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName +
                    " - No data mapping resolved from 'for_each' entries. LHS must be a list.");
        }
        var mapping = getEntries(fetcher.getProperty(INPUT));
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
        return executeProvidersWithForkJoin(po, graphInstance, fetcher, dictionaryNodes, size);
    }

    private static ArrayList<SimpleNode> getDictionaryNodes(List<String> dictionary, String nodeName,
                                                            GraphInstance graphInstance) {
        if (dictionary.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " - missing dictionary");
        }
        var graph = graphInstance.graph;
        var dictionaryNodes = new ArrayList<SimpleNode>();
        for (Object dict : dictionary) {
            var d = graph.findNodeByAlias(String.valueOf(dict));
            if (d != null) {
                dictionaryNodes.add(d);
            } else {
                throw new IllegalArgumentException(NODE_NAME + nodeName +
                        " - data dictionary node '"+dict+"' does not exist");
            }
        }
        return dictionaryNodes;
    }

    private Object executeProviders(PostOffice po, GraphInstance graphInstance, SimpleNode fetcher,
                                    List<SimpleNode> dictionaryNodes)
            throws URISyntaxException, ExecutionException, InterruptedException {
        var nodeName = fetcher.getAlias();
        var timeout = getModelTtl(graphInstance);
        var stateMachine = graphInstance.stateMachine;
        var graph = graphInstance.graph;
        var parameterMapping = getEntries(fetcher.getProperty(INPUT));
        for (SimpleNode dd : dictionaryNodes) {
            var parameters = getFetcherApiParameters(nodeName, graphInstance, parameterMapping);
            var required = getEntries(dd.getProperty(INPUT));
            if (!required.isEmpty()) {
                fillDictionaryApiParameters(nodeName, stateMachine, dd, required, parameters);
            }
            var provider = dd.getProperty(PROVIDER);
            if (provider instanceof String providerName) {
                var p = graph.findNodeByAlias(providerName);
                if (p != null) {
                    fetchFromEachProvider(po, stateMachine, fetcher, dd, p, timeout);
                    var mapping = getEntries(dd.getProperty(OUTPUT));
                    performDictionaryOutputMapping(nodeName, stateMachine, dd.getAlias(), mapping, false);
                } else {
                    throw new IllegalArgumentException("Data provider "+providerName+" does not exist");
                }
            } else {
                throw new IllegalArgumentException("Missing provider in data dictionary "+dd.getAlias());
            }
        }
        var outputMapping = getEntries(fetcher.getProperty(OUTPUT));
        performFetcherOutputMapping(nodeName, stateMachine, outputMapping);
        // clear temporary dataset
        stateMachine.removeElement(nodeName + DD);
        stateMachine.removeElement(nodeName + DOT_RESPONSE);
        return NEXT;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getFetcherApiParameters(String nodeName, GraphInstance graphInstance,
                                                        List<String> parameterMapping) {
        for (var entry : parameterMapping) {
            fillFetcherApiParameters(nodeName, entry, graphInstance, false);
        }
        var stateMachine = graphInstance.stateMachine;
        var params = stateMachine.getElement(nodeName + FETCH, new HashMap<>());
        stateMachine.removeElement(nodeName + FETCH);
        return params instanceof Map? (Map<String, Object>) params : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Object executeProvidersWithForkJoin(PostOffice po, GraphInstance graphInstance, SimpleNode fetcher,
                                                List<SimpleNode> dictionaryNodes, int size)
            throws URISyntaxException, ExecutionException, InterruptedException {
        var graph = graphInstance.graph;
        var nodeName = fetcher.getAlias();
        var givenConcurrency = util.str2int(String.valueOf(fetcher.getProperty(CONCURRENCY)));
        var concurrency = Math.clamp(givenConcurrency < 0 ? 3 : givenConcurrency, 1, 30);
        var timeout = getModelTtl(graphInstance);
        var stateMachine = graphInstance.stateMachine;
        var apiParams = (Map<String, List<Object>>) stateMachine.getElement(nodeName + EACH, new HashMap<>());
        for (SimpleNode dd : dictionaryNodes) {
            Deque<HttpEventEnvelope> stack = new ArrayDeque<>();
            if (dd.getProperty(PROVIDER) == null) {
                throw new IllegalArgumentException("Data provider is not configured in dictionary "+dd.getAlias());
            }
            final SimpleNode p = graph.findNodeByAlias(String.valueOf(dd.getProperty(PROVIDER)));
            var md = new ProviderMetadata(po, fetcher, dd, p, stateMachine, timeout);
            if (p != null) {
                var url = p.getProperty(URL);
                var method = p.getProperty(METHOD);
                if (url != null && method != null) {
                    var target = new HostUri(String.valueOf(url));
                    md.features.addAll(getEntries(p.getProperty(FEATURE)));
                    md.target = target;
                    md.method = String.valueOf(method);
                    md.inputs = getEntries(p.getProperty(INPUT));
                    md.breakOnException = !CONTINUE.equalsIgnoreCase(String.valueOf(p.getProperty(ERROR)));
                } else {
                    throw new IllegalArgumentException("Missing url or method in data provider " + p.getAlias());
                }
            } else {
                throw new IllegalArgumentException("Data provider " + dd.getProperty(PROVIDER) + " does not exist");
            }
            // release one set of parameters from the array for each data dictionary item
            for (int i = 0; i < size; i++) {
                pushHttpEventToStack(md, stack, apiParams, i);
            }
            runConcurrentRequests(concurrency, stack, md);
        }
        var outputMapping = getEntries(fetcher.getProperty(OUTPUT));
        performFetcherOutputMapping(nodeName, stateMachine, outputMapping);
        // clear temporary dataset
        stateMachine.removeElement(nodeName + FETCH);
        stateMachine.removeElement(nodeName + EACH);
        stateMachine.removeElement(nodeName + DD);
        stateMachine.removeElement(nodeName + DOT_RESPONSE);
        return NEXT;
    }

    private void pushHttpEventToStack(ProviderMetadata md, Deque<HttpEventEnvelope> stack,
                                      Map<String, List<Object>> apiParams, int i) {
        var nodeName = md.fetcher.getAlias();
        var parameters = new HashMap<String, Object>();
        for (var kv : apiParams.entrySet()) {
            var key = kv.getKey();
            var value = kv.getValue().get(i);
            parameters.put(key, value);
            md.stateMachine.setElement(nodeName + FETCH + key, value);
        }
        var required = getEntries(md.dd.getProperty(INPUT));
        if (!required.isEmpty()) {
            fillDictionaryApiParameters(nodeName, md.stateMachine, md.dd, required, parameters);
        }
        var request = buildHttpRequest(md);
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request.toMap());
        stack.add(new HttpEventEnvelope(event, request, parameters.keySet()));
    }

    private void runConcurrentRequests(int concurrency, Deque<HttpEventEnvelope> stack, ProviderMetadata md)
                                        throws ExecutionException, InterruptedException {
        List<HttpEventEnvelope> batch = new ArrayList<>();
        var n = concurrency;
        while (!stack.isEmpty()) {
            n--;
            batch.add(stack.pop());
            if (stack.isEmpty() || n == 0) {
                n = concurrency;
                var events = new ArrayList<EventEnvelope>();
                for (var each : batch) {
                    events.add(each.event);
                }
                var parameterNames = batch.getFirst().parameterNames;
                var firstReq = batch.getFirst().request;
                var finalUri = firstReq.getFinalizedUrl();
                log.info("{} {}, for each {}, parallel={}, ttl={}", firstReq.getMethod(), finalUri,
                            parameterNames, batch.size(), md.timeout);
                md.po.annotateTrace(FOR_EACH, String.valueOf(parameterNames))
                        .annotateTrace(URL, firstReq.getMethod()+" "+firstReq.getTargetHost() + finalUri);
                var responses = md.po.request(events, md.timeout, false).get();
                for (int i = 0; i < responses.size(); i++) {
                    processApiResponses(md, responses, batch, i);
                }
                batch.clear();
            }
        }
    }

    private void processApiResponses(ProviderMetadata md, List<EventEnvelope> responses,
                                     List<HttpEventEnvelope> batch, int i) {
        var nodeName = md.fetcher.getAlias();
        var request = batch.get(i).request;
        var response = responses.get(i);
        for (FeatureDef f : md.after) {
            f.feature().execute(request, response, md.stateMachine, nodeName);
        }
        md.stateMachine.setElement(nodeName + "." + STATUS, response.getStatus());
        if (response.hasError()) {
            md.stateMachine.setElement(nodeName + "." + ERROR, response.getError());
            if (md.breakOnException) {
                md.stateMachine.setElement(OUTPUT_BODY, response.getBody());
                md.stateMachine.setElement(OUTPUT_NAMESPACE+HEADER, response.getHeaders());
                md.stateMachine.setElement(OUTPUT_NAMESPACE+STATUS, response.getStatus());
                var advice = md.stateMachine.exists(nodeName + "." + LIVE)?
                        response.getError() : "ERROR: See output with 'inspect output'";
                throw new FetchException(String.valueOf(advice));
            }
        } else {
            md.stateMachine.setElement(nodeName + RESPONSE_DOT, response.getBody());
        }
        var mapping = getEntries(md.dd.getProperty(OUTPUT));
        performDictionaryOutputMapping(nodeName, md.stateMachine, md.dd.getAlias(), mapping, true);
    }

    private void fillDictionaryApiParameters(String nodeName, MultiLevelMap stateMachine,
                                             SimpleNode dd, List<String> required, Map<String, Object> parameters) {
        var ddName = dd.getAlias();
        for (var input : required) {
            var key = input.trim();
            var colon = key.indexOf(':');
            if (colon == -1) {
                if (parameters.containsKey(key)) {
                    stateMachine.setElement(nodeName + DD + ddName + "." + key, parameters.get(key));
                }
            } else {
                var dk = key.substring(0, colon).trim();
                var dv = key.substring(colon+1).trim();
                fillDefaultApiParameter(nodeName, stateMachine, ddName, dk, dv, parameters);
            }
        }
    }

    private void fillDefaultApiParameter(String nodeName, MultiLevelMap stateMachine, String ddName,
                                         String dk, String dv, Map<String, Object> parameters) {
        if (parameters.containsKey(dk)) {
            stateMachine.setElement(nodeName + DD + ddName + "." + dk, parameters.get(dk));
        } else {
            // check if the default value can be used to map a value from the state machine
            var mappedValue = helper.getLhsOrConstant(dv, stateMachine);
            var resolved = mappedValue != null? mappedValue : dv;
            stateMachine.setElement(nodeName + DD + ddName + "." + dk, resolved);
            parameters.put(dk, resolved);
        }
    }

    private void performDictionaryOutputMapping(String nodeName, MultiLevelMap stateMachine,
                                                String dictionaryName, List<String> mapping, boolean isArray) {
        for (var output : mapping) {
            var text = output.trim();
            int sep = text.lastIndexOf(MAP_TO);
            if (sep != -1) {
                var lhs = substituteVarIfAny(text.substring(0, sep).trim(), stateMachine);
                var rhs = text.substring(sep + MAP_TO.length()).trim();
                var constant = helper.getConstantValue(lhs);
                if (constant == null && !lhs.startsWith(PLUGIN_PREFIX)) {
                    // reconstruct lhs with nodeName as namespace
                    if (lhs.startsWith(RESPONSE_NAMESPACE)) {
                        lhs = nodeName + "." + lhs;
                    } else if (lhs.startsWith("$.response")) {
                        lhs = "$." + nodeName + lhs.substring(1);
                    } else if (!lhs.startsWith(MODEL_NAMESPACE) && !lhs.startsWith("$.model.")) {
                        throw new IllegalArgumentException("Invalid output data mapping in data dictionary " +
                                dictionaryName + " - LHS must start with 'model.' or 'response.' namespace");
                    }
                }
                setDictionaryOutputEntry(nodeName, lhs, rhs, constant, stateMachine, isArray);
            } else {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " - invalid output mapping: "+text);
            }
        }
    }

    private void setDictionaryOutputEntry(String nodeName, String lhs, String rhs, Object constant,
                                          MultiLevelMap stateMachine, boolean isArray) {
        var value = constant != null? constant : helper.getLhsElement(lhs, stateMachine);
        if (value != null) {
            if (rhs.startsWith(RESULT_NAMESPACE)) {
                rhs = nodeName + "." + rhs + (isArray ? "[]" : "");
            }  else if (!rhs.startsWith(MODEL_NAMESPACE)) {
                throw new IllegalArgumentException("Invalid output data mapping in data dictionary "+nodeName +
                        " - RHS must start with 'model.' or 'result.' namespace");
            }
            stateMachine.setElement(rhs, value);
        }
    }

    private void fetchFromEachProvider(PostOffice po, MultiLevelMap stateMachine, SimpleNode fetcher,
                                       SimpleNode dd, SimpleNode provider, long timeout)
                                        throws URISyntaxException, ExecutionException, InterruptedException {
        var url = provider.getProperty(URL);
        var method = provider.getProperty(METHOD);
        if (url != null && method != null) {
            var target = new HostUri(String.valueOf(url));
            var md = new ProviderMetadata(po, fetcher, dd, provider, stateMachine, timeout);
            md.features.addAll(getEntries(provider.getProperty(FEATURE)));
            md.target = target;
            md.method = String.valueOf(method);
            md.inputs = getEntries(provider.getProperty(INPUT));
            md.breakOnException = !CONTINUE.equalsIgnoreCase(String.valueOf(provider.getProperty(ERROR)));
            makeRegularHttpCall(md);
        } else {
            throw new IllegalArgumentException("Missing url or method in data provider "+provider.getAlias());
        }
    }

    private AsyncHttpRequest buildHttpRequest(ProviderMetadata md) {
        var nodeName = md.fetcher.getAlias();
        var request = new AsyncHttpRequest();
        request.setMethod(md.method).setTargetHost(md.target.host).setUrl(md.target.uri);
        if (!md.inputs.isEmpty()) {
            mapHttpInput(request, nodeName, md.dd.getAlias(), md.stateMachine, md.inputs);
        }
        loadFeatures(md);
        for (FeatureDef f : md.before) {
            f.feature().execute(request, null, md.stateMachine, nodeName);
        }
        return request;
    }

    private void makeRegularHttpCall(ProviderMetadata md) throws ExecutionException, InterruptedException {
        var nodeName = md.fetcher.getAlias();
        var request = buildHttpRequest(md);
        var params = md.stateMachine.getElement(nodeName + DD + md.dd.getAlias() +".", new HashMap<>());
        // multiple data dictionary items may use the same data provider with the same parameter key-values
        // so we want to cache it to avoid repeated API calls.
        var provider = md.provider.getAlias();
        var cached = getCachedResult(provider, md.stateMachine, params);
        if (cached != null) {
            md.stateMachine.setElement(nodeName + RESPONSE_DOT, cached);
            return;
        }
        var parameterNames = params instanceof Map<?, ?> map? map.keySet(): Set.of();
        log.info("{} {}, with {}, ttl={}", request.getMethod(), request.getFinalizedUrl(), parameterNames, md.timeout);
        md.po.annotateTrace("parameters", String.valueOf(parameterNames))
                .annotateTrace(URL, request.getMethod()+" "+request.getTargetHost() + request.getFinalizedUrl());
        var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request.toMap());
        var response = md.po.request(event, md.timeout, false).get();
        for (FeatureDef f : md.after) {
            f.feature().execute(request, response, md.stateMachine, nodeName);
        }
        md.stateMachine.setElement(nodeName + "." + STATUS, response.getStatus());
        if (response.hasError()) {
            md.stateMachine.setElement(nodeName + "." + ERROR, response.getError());
            if (md.breakOnException) {
                md.stateMachine.setElement(OUTPUT_BODY, response.getBody());
                md.stateMachine.setElement(OUTPUT_NAMESPACE+HEADER, response.getHeaders());
                md.stateMachine.setElement(OUTPUT_NAMESPACE+STATUS, response.getStatus());
                var advice = md.stateMachine.exists(nodeName + "." + LIVE)?
                                    response.getError() : "ERROR: See output with 'inspect output'";
                throw new FetchException(String.valueOf(advice));
            }
        } else {
            md.stateMachine.setElement(nodeName + RESPONSE_DOT, response.getBody());
        }
        // save request and response in cache of the current graph instance
        md.stateMachine.setElement(CACHE_NAMESPACE + provider + "[]",
                Map.of(INPUT, params, OUTPUT, response.getBody()));
    }

    private void loadFeatures(ProviderMetadata md) {
        // ensure loading once
        if (md.before.isEmpty() && md.after.isEmpty()) {
            for (var f : md.features) {
                FeatureDef def = PlaygroundLoader.getFeature(f);
                if (def != null) {
                    var feature = def.feature();
                    if (feature.runBefore()) {
                        md.before.add(def);
                    } else {
                        md.after.add(def);
                    }
                } else {
                    lackOfSkillAdvice(f);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object getCachedResult(String provider, MultiLevelMap stateMachine, Object params) {
        var cached = stateMachine.getElement(CACHE_NAMESPACE + provider);
        if (cached instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map) {
                    var map = (Map<String, Object>) o;
                    var input = map.get(INPUT);
                    if (input != null && input.equals(params)) {
                        return map.get(OUTPUT);
                    }
                }
            }
        }
        return null;
    }

    private void lackOfSkillAdvice(String feature) {
        // just print out an advice once a while
        var now = System.currentTimeMillis();
        var t1 = UNSUPPORTED_FEATURES.getOrDefault(feature, 0L);
        if (now - t1 > WARNING_INTERVAL) {
            UNSUPPORTED_FEATURES.put(feature, now);
            log.warn("'{}' not implemented. " +
                "Please implement FeatureRunner interface with @FetchFeature", feature);
        }
    }

    private static class ProviderMetadata {
        final PostOffice po;
        final MultiLevelMap stateMachine;
        final SimpleNode fetcher;
        final SimpleNode dd;
        final SimpleNode provider;
        final long timeout;
        final List<FeatureDef> before =  new ArrayList<>();
        final List<FeatureDef> after = new ArrayList<>();
        final List<String> features = new ArrayList<>();
        HostUri target;
        String method;
        List<String> inputs;
        boolean breakOnException = true;

        public ProviderMetadata(PostOffice po, SimpleNode fetcher, SimpleNode dd,
                                SimpleNode provider, MultiLevelMap stateMachine, long timeout) {
            this.po = po;
            this.fetcher = fetcher;
            this.dd = dd;
            this.provider = provider;
            this.stateMachine = stateMachine;
            this.timeout = timeout;
        }
    }

    private record HttpEventEnvelope(EventEnvelope event, AsyncHttpRequest request, Set<String> parameterNames) { }
}
