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

package com.accenture.dictionary.tasks;

import com.accenture.dictionary.base.DictionaryLambdaFunction;
import com.accenture.dictionary.models.DataDictionary;
import com.accenture.dictionary.models.Question;
import com.accenture.dictionary.models.QuestionSpecs;
import com.accenture.dictionary.models.ServiceParameters;
import com.accenture.models.Flows;
import com.accenture.util.DataMappingHelper;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

@PreLoad(route="data.question.processor", instances=200)
public class QuestionProcessor extends DictionaryLambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(QuestionProcessor.class);
    private static final Utility util = Utility.getInstance();
    private static final DataMappingHelper helper = DataMappingHelper.getInstance();

    @SuppressWarnings("unchecked")
    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws ExecutionException, InterruptedException {
        var instanceId = headers.get("instance");
        if (instanceId == null) {
            throw new IllegalArgumentException("Missing instance ID in header");
        }
        var questionId = headers.get("question");
        if (questionId == null) {
            throw new IllegalArgumentException("Missing question ID in header");
        }
        var flowInstance = Flows.getFlowInstance(instanceId);
        if (flowInstance == null) {
            throw new IllegalArgumentException("Invalid flow instance " + instanceId);
        }
        // load question specs
        var questionSpecs = getQuestionSpecs(questionId);
        var expectedDictionary = questionSpecs.getExpectedOutput();
        // load data dictionary items
        var targets = new HashSet<String>();
        for (var id : expectedDictionary) {
            var dataDict = getDataDict(id);
            targets.add(dataDict.target);
        }
        // load providers
        for (var target : targets) {
            getDataProvider(target);
        }
        // create a trackable PostOffice for data fetcher
        var po = new PostOffice(headers, instance);
        // make a copy of flow input and model to avoid accidentally changing the original values
        var inputCopy = util.deepCopy((Map<String, Object>) flowInstance.dataset.get(INPUT));
        var modelCopy = util.deepCopy((Map<String, Object>) flowInstance.dataset.get(MODEL));
        var dataset = new MultiLevelMap();
        dataset.setElement(INPUT, inputCopy);
        dataset.setElement(MODEL, modelCopy);
        var timeout = dataset.getElement(MODEL_TTL) instanceof Long ttl? ttl : 30000;
        executeQuestions(po, dataset, questionSpecs, timeout);
        if (dataset.getElement(ERROR_CODE) instanceof Integer rc) {
            var error = dataset.getElement(ERROR_MESSAGE);
            return new EventEnvelope().setStatus(rc).setBody(error);
        }
        var result = dataset.getElement(RESULT);
        if (result == null) {
            throw new IllegalArgumentException("No result resolved for " + questionId);
        }
        for (var entry: questionSpecs.answers) {
            int sep = entry.lastIndexOf(ARROW);
            var lhs = entry.substring(0, sep).trim();
            var rhs = entry.substring(sep + ARROW.length()).trim();
            var value = helper.getLhsOrConstant(lhs, dataset);
            if (value != null) {
                dataset.setElement(rhs, value);
            }
        }
        var mappedResult = dataset.getElement(OUTPUT_BODY);
        if (mappedResult == null) {
            throw new IllegalArgumentException("No result resolved for " + questionId);
        }
        var response = new EventEnvelope().setBody(mappedResult);
        var mappedHeaders = dataset.getElement(OUTPUT_HEADER);
        if (mappedHeaders instanceof Map<?, ?> map) {
            map.forEach((key, value) -> response.setHeader(String.valueOf(key), value));
        }
        return response;
    }

    private void executeQuestions(PostOffice po, MultiLevelMap dataset, QuestionSpecs questionSpecs, long timeout)
            throws ExecutionException, InterruptedException {
        for (var question : questionSpecs.questions) {
            if (question.forEach.isEmpty()) {
                doRegularQuestion(po, question, dataset, timeout);
            } else {
                doArrayQuestion(questionSpecs.concurrency, po, question, dataset, timeout);
            }
            if (dataset.exists(ERROR_CODE)) {
                break;
            }
        }
    }
    
    private void doArrayQuestion(int concurrency, PostOffice po, Question question,
                                 MultiLevelMap dataset, long timeout)
            throws ExecutionException, InterruptedException {
        Map<String, List<?>> mappings = getForEachMapping(question, dataset);
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException("No data mapping resolved using the 'for_each' entries " + question.id);
        }
        // when there is more than one for_each entry, the list values resolved are of the same size
        var each = new ServiceParameters();
        var rhsList = new ArrayList<>(mappings.keySet());
        var size = mappings.get(rhsList.getFirst()).size();
        for (int i=0; i < size; i++) {
            for (var rhs : rhsList) {
                Object value = mappings.get(rhs).get(i);
                var key = rhs.startsWith(MODEL_NAMESPACE) ? rhs : PARA_NAMESPACE + rhs;
                dataset.setElement(key, value);
            }
            mapArrayQuestion(question, dataset, each);
        }
        Deque<EventEnvelope> stack = new ArrayDeque<>();
        for (Map<String, Object> required : each.parameters) {
            var request = new EventEnvelope().setTo(each.service);
            request.setHeader(PROVIDER, each.item.target).setHeader(TIMEOUT, timeout).setBody(required);
            stack.add(request);
        }
        runConcurrentRequests(concurrency, po, stack, dataset, each.item, timeout);
    }

    private Map<String, List<?>> getForEachMapping(Question question, MultiLevelMap dataset) {
        int size = -1;
        Map<String, List<?>> mappings = new HashMap<>();
        for (var entry : question.forEach) {
            var sep = entry.lastIndexOf(ARROW);
            var lhs = entry.substring(0, sep).trim();
            var rhs = entry.substring(sep+ARROW.length()).trim();
            var value = helper.getLhsOrConstant(lhs, dataset);
            if (value instanceof List<?> list) {
                if (size == -1) {
                    size = list.size();
                } else if (size != list.size()) {
                    throw new IllegalArgumentException("Inconsistent array size for LHS values of 'for_each' entries");
                }
                mappings.put(rhs, list);
            } else if (value != null) {
                var key = rhs.startsWith(MODEL_NAMESPACE) ? rhs : PARA_NAMESPACE + rhs;
                dataset.setElement(key, value);
            }
        }
        return mappings;
    }

    private void runConcurrentRequests(int concurrency, PostOffice po, Deque<EventEnvelope> stack,
                                       MultiLevelMap dataset, DataDictionary item, long timeout)
            throws ExecutionException, InterruptedException {
        List<EventEnvelope> batch = new ArrayList<>();
        var n = concurrency;
        while (!stack.isEmpty()) {
            n--;
            batch.add(stack.pop());
            if (stack.isEmpty() || n == 0) {
                n = concurrency;
                log.info("Sending {} parallel event{}", batch.size(), batch.size() == 1 ? "" : "s");
                var responses = po.request(batch, timeout, false).get();
                for (var response : responses) {
                    if (response.hasError()) {
                        dataset.setElement(ERROR_CODE, response.getStatus());
                        dataset.setElement(ERROR_MESSAGE, response.getBody());
                        return;
                    }
                    dataset.setElement(RESPONSE_NAMESPACE + item.target, response.getBody());
                    var mm = new MultiLevelMap(Map.of(RESPONSE, response.getBody(), FOR_EACH, true));
                    mapResponse(item.output, dataset, mm);
                }
                batch.clear();
            }
        }
    }

    private void mapArrayQuestion(Question question, MultiLevelMap dataset, ServiceParameters paramMap) {
        loadInputParameters(question, dataset);
        // array question should have one and only one output
        var entry = question.output.getFirst();
        var item = getDataDict(entry);
        if (item == null) {
            throw new IllegalArgumentException("Data dictionary item '" + entry + "' not found");
        }
        mapArrayData(item, dataset, paramMap);
    }

    @SuppressWarnings("unchecked")
    private void mapArrayData(DataDictionary item, MultiLevelMap dataset, ServiceParameters paramMap) {
        // for each data dictionary item, fetch data from target
        var provider = getDataProvider(item.target);
        if (provider == null) {
            throw new IllegalArgumentException("Data provider '" + item.target + "' not found");
        }
        var required = mapRequiredParameters(item, (Map<String, Object>) dataset.getElement(PARAMETER));
        var targetService = getTargetService(provider.protocol);
        if (targetService == null) {
            throw new IllegalArgumentException("Target service '" + provider.protocol + "' not configured");
        }
        if (paramMap.service == null) {
            paramMap.service = targetService;
        }
        if (paramMap.item == null) {
            paramMap.item = item;
        }
        paramMap.addParameterMap(required);
    }

    private Map<String, Object> mapRequiredParameters(DataDictionary item, Map<String, Object> parameters) {
        var required = new HashMap<String, Object>();
        for (var k: item.input) {
            // any default value? syntax below:
            // parameter:default_value
            int sep = k.indexOf(':');
            var key = sep == -1 ? k : k.substring(0, sep).trim();
            if (parameters.containsKey(key)) {
                required.put(key, parameters.get(key));
            } else if (sep != -1) {
                // use default value if not set
                var rhs = k.substring(sep + 1).trim();
                var constant = helper.getConstantValue(rhs);
                required.put(key, Objects.requireNonNullElse(constant, rhs));
            }
        }
        return required;
    }

    private void doRegularQuestion(PostOffice po, Question question, MultiLevelMap dataset, long timeout)
            throws ExecutionException, InterruptedException {
        loadInputParameters(question, dataset);
        for (var entry : question.output) {
            var item = getDataDict(entry);
            if (item == null) {
                throw new IllegalArgumentException("Data dictionary item '" + entry + "' not found");
            }
            fetchRegularData(po, item, dataset, timeout);
            if (dataset.exists(ERROR_CODE)) {
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void fetchRegularData(PostOffice po, DataDictionary item, MultiLevelMap dataset, long timeout)
            throws ExecutionException, InterruptedException {
        // for each data dictionary item, fetch data from target
        var provider = getDataProvider(item.target);
        if (provider == null) {
            throw new IllegalArgumentException("Data provider '" + item.target + "' not found");
        }
        var required = mapRequiredParameters(item, (Map<String, Object>) dataset.getElement(PARAMETER));
        var targetService = getTargetService(provider.protocol);
        if (targetService == null) {
            throw new IllegalArgumentException("Target service '" + provider.protocol + "' not deployed");
        }
        var responseKey = RESPONSE_NAMESPACE + item.target;
        var cached = getCachedData(responseKey, required, dataset);
        if (cached == null) {
            var request = new EventEnvelope().setTo(targetService);
            request.setHeader(PROVIDER, item.target).setHeader(TIMEOUT, timeout).setBody(required);
            var response = po.request(request, timeout, false).get();
            if (response.hasError()) {
                dataset.setElement(ERROR_CODE, response.getStatus());
                dataset.setElement(ERROR_MESSAGE, response.getBody());
                return;
            }
            cached = response.getBody();
            dataset.setElement(responseKey + APPEND, Map.of(INPUT, required, OUTPUT, cached));
        }
        mapResponse(item.output, dataset, new MultiLevelMap(Map.of(RESPONSE, cached)));
    }

    @SuppressWarnings("unchecked")
    private Object getCachedData(String responseKey, Map<String, Object> required, MultiLevelMap dataset) {
        var cached = dataset.getElement(responseKey);
        if (cached instanceof List<?> responses) {
            for (var response : responses) {
                var map = (Map<String, Object>) response;
                var input = (Map<String, Object>) map.get(INPUT);
                var output = map.get(OUTPUT);
                if (sameInputParameters(input, required)) {
                    return output;
                }
            }
        }
        return null;
    }

    private boolean sameInputParameters(Map<String, Object> parameters, Map<String, Object> required) {
        if (parameters.size() != required.size()) {
            return false;
        }
        for (var kv: required.entrySet()) {
            var k = kv.getKey();
            Object v1 = kv.getValue();
            Object v2 = parameters.get(k);
            if (!v1.equals(v2)) return false;
        }
        return true;
    }

    private void mapResponse(List<String> output, MultiLevelMap dataset, MultiLevelMap mm) {
        for (var entry: output) {
            var sep = entry.lastIndexOf(ARROW);
            if (sep == -1) {
                throw new IllegalArgumentException("Missing RHS of data dictionary output mapping in '" + entry + "'");
            }
            var lhs = entry.substring(0, sep).trim();
            var rhs = entry.substring(sep+ARROW.length()).trim();
            var value = helper.getLhsOrConstant(lhs, entry.startsWith("$") || entry.equals(RESPONSE) ||
                        entry.startsWith(RESPONSE_NAMESPACE) || entry.startsWith(RESPONSE_INDEX)? mm : dataset);
            mapResponseToResult(rhs, entry, dataset, mm, value);
        }
    }

    private void mapResponseToResult(String rhs, String entry,
                                     MultiLevelMap dataset, MultiLevelMap mm, Object value) {
        if (value != null) {
            if (MODEL.equals(rhs)) {
                throw new IllegalArgumentException("Using whole 'model' is not allowed in '" + entry + "'");
            }
            // Guarantee that it is a clean copy
            var clone = copyOf(value);
            if (mm.exists(FOR_EACH)) {
                // for resultset, automatically add array "append" command if not given
                dataset.setElement(rhs.startsWith(MODEL_NAMESPACE) || rhs.contains(APPEND)? rhs : rhs + APPEND, clone);
            } else {
                dataset.setElement(rhs, clone);
            }
        }
    }

    private void loadInputParameters(Question question, MultiLevelMap dataset) {
        for (var text : question.input) {
            var sep = text.lastIndexOf(ARROW);
            var lhs = text.substring(0, sep).trim();
            var rhs = text.substring(sep+ARROW.length()).trim();
            var value = helper.getLhsOrConstant(lhs, dataset);
            if (value != null) {
                dataset.setElement(rhs.startsWith(MODEL_NAMESPACE)? rhs : PARA_NAMESPACE + rhs, copyOf(value));
            }
        }
    }
}
