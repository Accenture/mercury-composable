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

import com.accenture.minigraph.base.GraphLambdaFunction;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

@PreLoad(route = GraphApiFetcher.ROUTE, instances=300)
public class GraphApiFetcher extends GraphLambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(GraphApiFetcher.class);
    public static final String ROUTE = "graph.api.fetcher";
    private static final AtomicLong skillWarning = new AtomicLong(0);
    private static final long WARNING_INTERVAL = 5000;

    @SuppressWarnings("unchecked")
    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance)
            throws URISyntaxException, ExecutionException, InterruptedException {
        if (!EXECUTE.equals(headers.get(TYPE))) {
            throw new IllegalArgumentException("Type must be EXECUTE");
        }
        var in = headers.get(IN);
        var nodeName = headers.getOrDefault(NODE, "none");
        var graphInstance = getGraphInstance(in);
        var node = getNode(nodeName, graphInstance.graph);
        if (!ROUTE.equals(node.getProperty(SKILL))) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " does not have skill - "+ROUTE);
        }
        var dictionary = node.getProperty(DICTIONARY);
        if (!(dictionary instanceof List<?> dictionaryList)) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " - invalid or missing dictionary");
        }
        if (dictionaryList.isEmpty()) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " - missing dictionary");
        }
        var graph = graphInstance.graph;
        var dictionaryNodes = new ArrayList<SimpleNode>();
        for (Object dict : dictionaryList) {
            var d = graph.findNodeByAlias(String.valueOf(dict));
            if (d != null) {
                dictionaryNodes.add(d);
            } else {
                throw new IllegalArgumentException(NODE_NAME + nodeName +
                        " - data dictionary node '"+dict+"' does not exist");
            }
        }
        var mapping = node.getProperty(INPUT);
        if (mapping instanceof List<?> entries) {
            for (Object entry : entries) {
                fillApiParameters(nodeName, String.valueOf(entry), graphInstance);
            }
        } else if (mapping != null) {
            throw new IllegalArgumentException(NODE_NAME + nodeName + " - invalid 'input' entries");
        }
        var stateMachine = graphInstance.stateMachine;
        var apiParams = stateMachine.getElement(nodeName + API_DOT);
        var parameters = apiParams instanceof Map? apiParams : Map.of();
        var po = new PostOffice(headers, instance);
        return retrieveFromProviders(po, graphInstance, node, dictionaryNodes, (Map<String, Object>) parameters);
    }

    private Object retrieveFromProviders(PostOffice po, GraphInstance graphInstance, SimpleNode node,
                                        List<SimpleNode> dictionaryNodes, Map<String, Object> parameters)
            throws URISyntaxException, ExecutionException, InterruptedException {
        var nodeName = node.getAlias();
        var timeout = getModelTtl(graphInstance);
        var stateMachine = graphInstance.stateMachine;
        var graph = graphInstance.graph;
        for (SimpleNode dd : dictionaryNodes) {
            var inputParams = dd.getProperty(INPUT);
            if (inputParams instanceof List<?> entries) {
                for (Object input : entries) {
                    var text = String.valueOf(input).trim();
                    var colon = text.indexOf(':');
                    if (colon == -1) {
                        if (!parameters.containsKey(text)) {
                            throw new IllegalArgumentException("Missing input parameter '"+text+
                                        "' for data dictionary "+dd.getAlias());
                        }
                    } else {
                        // fill default value
                        var key = text.substring(0, colon).trim();
                        if (!parameters.containsKey(key)) {
                            var value = text.substring(colon+1).trim();
                            var updated = helper.getLhsOrConstant(value, stateMachine);
                            var resolved = updated != null? updated : value;
                            stateMachine.setElement(nodeName + API_DOT + key, resolved);
                            parameters.put(key, resolved);
                        }
                    }
                }
            }
            var provider = dd.getProperty(PROVIDER);
            if (provider instanceof String providerName) {
                var p = graph.findNodeByAlias(providerName);
                if (p != null) {
                    fetchFromEachProvider(po, nodeName, stateMachine, p, dd.getAlias(), timeout);
                    var outputMapping = dd.getProperty(OUTPUT);
                    if (outputMapping instanceof List<?> mapping) {
                        performDictionaryOutputMapping(nodeName, stateMachine, mapping);
                    }
                } else {
                    throw new IllegalArgumentException("Data provider "+providerName+" does not exist");
                }
            } else {
                throw new IllegalArgumentException("Missing provider in data dictionary "+dd.getAlias());
            }
        }
        var fetcherOutput = node.getProperty(OUTPUT);
        if (fetcherOutput instanceof List<?> mapping) {
            performFetcherOutputMapping(nodeName, stateMachine, mapping);
        }
        // response dataset is no longer required
        stateMachine.removeElement(nodeName + ".response");
        return NEXT;
    }

    private void performFetcherOutputMapping(String nodeName, MultiLevelMap stateMachine, List<?> mapping) {
        for (Object output : mapping) {
            var text = String.valueOf(output).trim();
            int sep = text.lastIndexOf(MAP_TO);
            if (sep != -1) {
                var lhs = text.substring(0, sep).trim();
                var rhs = text.substring(sep + MAP_TO.length()).trim();
                if (lhs.startsWith(RESULT_NAMESPACE)) {
                    lhs = nodeName + "." + lhs;
                } else if (lhs.startsWith("$.result")) {
                    lhs = "$."+nodeName + lhs.substring(1);
                } else if (!lhs.startsWith(MODEL_NAMESPACE) && !lhs.startsWith("$.model.")) {
                    throw new IllegalArgumentException("Invalid output data mapping in API fetcher "+nodeName +
                            " - LHS must start with 'model.' or 'result.' namespace");
                }
                var value = helper.getLhsElement(lhs, stateMachine);
                if (value != null) {
                    if (!rhs.startsWith(MODEL_NAMESPACE) && !rhs.startsWith(OUTPUT_NAMESPACE)) {
                        throw new IllegalArgumentException("Invalid output data mapping in data dictionary "+nodeName +
                                " - RHS must start with 'model.' or 'output.' namespace");
                    }
                    stateMachine.setElement(rhs, value);
                }
            } else {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " - invalid output mapping: "+text);
            }
        }
    }

    private void performDictionaryOutputMapping(String nodeName, MultiLevelMap stateMachine, List<?> mapping) {
        for (Object output : mapping) {
            var text = String.valueOf(output).trim();
            int sep = text.lastIndexOf(MAP_TO);
            if (sep != -1) {
                var lhs = text.substring(0, sep).trim();
                var rhs = text.substring(sep + MAP_TO.length()).trim();
                if (lhs.startsWith(RESPONSE_NAMESPACE)) {
                    lhs = nodeName + "." + lhs;
                } else if (lhs.startsWith("$.response")) {
                    lhs = "$."+nodeName + lhs.substring(1);
                } else if (!lhs.startsWith(MODEL_NAMESPACE) && !lhs.startsWith("$.model.")) {
                    throw new IllegalArgumentException("Invalid output data mapping in data dictionary "+nodeName +
                            " - LHS must start with 'model.' or 'response.' namespace");
                }
                var value = helper.getLhsElement(lhs, stateMachine);
                if (value != null) {
                    if (rhs.startsWith(RESULT_NAMESPACE)) {
                        rhs = nodeName + "." + rhs;
                    }  else if (!rhs.startsWith(MODEL_NAMESPACE)) {
                        throw new IllegalArgumentException("Invalid output data mapping in data dictionary "+nodeName +
                                " - RHS must start with 'model.' or 'result.' namespace");
                    }
                    stateMachine.setElement(rhs, value);
                }
            } else {
                throw new IllegalArgumentException(NODE_NAME + nodeName + " - invalid output mapping: "+text);
            }
        }
    }

    private void fetchFromEachProvider(PostOffice po, String nodeName, MultiLevelMap stateMachine,
                                       SimpleNode dp, String dictionaryName, long timeout)
            throws URISyntaxException, ExecutionException, InterruptedException {
        var url = dp.getProperty(URL);
        var method = dp.getProperty(METHOD);
        if (url != null && method != null) {
            var inputs = dp.getProperty(INPUT);
            var features = dp.getProperty(FEATURE);
            var target = new HostUri(String.valueOf(url));
            var request = new AsyncHttpRequest();
            request.setMethod(String.valueOf(method)).setTargetHost(target.host).setUrl(target.uri);
            if (inputs instanceof List<?> params) {
                var list = new ArrayList<String>();
                params.forEach(p -> list.add(String.valueOf(p)));
                mapHttpInput(request, nodeName, stateMachine, list);
            }
            // As a simple HTTP client, this does not have implementation for any authentication features.
            if (features instanceof List<?> featureList) {
                lackOfSkillAdvice(po.getRoute(), featureList);
            }
            log.info("{} {}, ttl={}", request.getMethod(), request.getUrl(), timeout);
            var event = new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request.toMap());
            var response = po.request(event, timeout, false).get();
            stateMachine.setElement(nodeName + "." + STATUS + "." + dictionaryName, response.getStatus());
            if (response.hasError()) {
                stateMachine.setElement(nodeName + RESPONSE_DOT + ERROR, response.getError());
            } else {
                stateMachine.setElement(nodeName + RESPONSE_DOT, response.getBody());
            }
        } else {
            throw new IllegalArgumentException("Missing url or method in data provider "+dp.getAlias());
        }
    }

    private void lackOfSkillAdvice(String myRoute, List<?> features) {
        // just print out an advice once a while
        var now = System.currentTimeMillis();
        if (now - skillWarning.get() > WARNING_INTERVAL) {
            skillWarning.set(now);
            log.warn("{} does not implement {} - perhaps you need another one with the right features",
                    myRoute, features);
        }
    }
}
