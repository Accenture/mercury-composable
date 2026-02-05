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

@PreLoad(route="v1.question.processor", instances=200)
public class QuestionProcessor extends DictionaryLambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(QuestionProcessor.class);
    private static final Utility util = Utility.getInstance();

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
        var helper = DataMappingHelper.getInstance();
        for (var entry: questionSpecs.answers) {
            int sep = entry.lastIndexOf(ARROW);
            var lhs = entry.substring(0, sep).trim();
            var rhs = entry.substring(sep + ARROW.length()).trim();
            var value = helper.getLhsElement(lhs, dataset);
            if (value != null) {
                dataset.setElement(rhs, value);
            }
        }
        var mappedResult = dataset.getElement(OUTPUT_BODY);
        if (mappedResult == null) {
            throw new IllegalArgumentException("No result resolved for " + questionId);
        }
        return mappedResult;
    }

    private void executeQuestions(PostOffice po, MultiLevelMap dataset, QuestionSpecs questionSpecs, long timeout)
            throws ExecutionException, InterruptedException {
        for (var question : questionSpecs.questions) {
            if (question.forEachElement == null) {
                doRegularQuestion(po, question, dataset, timeout);
            } else {
                doArrayQuestion(questionSpecs.concurrency, po, question, dataset, question.forEachElement, timeout);
            }
            if (dataset.exists(ERROR_CODE)) {
                break;
            }
        }
    }
    
    private void doArrayQuestion(int concurrency, PostOffice po, Question question,
                                 MultiLevelMap dataset, String forEachElement, long timeout)
            throws ExecutionException, InterruptedException {
        var helper = DataMappingHelper.getInstance();
        var sep = forEachElement.lastIndexOf(ARROW);
        var lhs = forEachElement.substring(0, sep).trim();
        var rhs = forEachElement.substring(sep+ARROW.length()).trim();
        var value = helper.getLhsElement(lhs, dataset);
        if (value instanceof List<?> valueList) {
            var each = new ServiceParameters();
            for (var element : valueList) {
                var key = rhs.startsWith(MODEL_NAMESPACE)? rhs : PARA_NAMESPACE + rhs;
                dataset.setElement(key, element);
                mapArrayQuestion(question, dataset, each);
            }
            Deque<EventEnvelope> stack = new ArrayDeque<>();
            for (Map<String, Object> required : each.parameters) {
                var request = new EventEnvelope().setTo(each.service);
                request.setHeader(PROVIDER, each.item.target).setHeader(TIMEOUT, timeout).setBody(required);
                stack.add(request);
            }
            runConcurrentRequests(concurrency, po, stack, dataset, each.item, timeout);
        } else {
            var clazz = value == null? "null" : value.getClass();
            throw new IllegalArgumentException("Expect " + lhs + " to be a list but got " + clazz);
        }
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
                    mapResponse(item.id, item.output, dataset, mm);
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
        var parameters = (Map<String, Object>) dataset.getElement(PARAMETER);
        var required = new HashMap<String, Object>();
        for (var k: item.input) {
            if (parameters.containsKey(k)) {
                required.put(k, parameters.get(k));
            }
        }
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
        var parameters = (Map<String, Object>) dataset.getElement(PARAMETER);
        var required = new HashMap<String, Object>();
        for (var k: item.input) {
            if (parameters.containsKey(k)) {
                required.put(k, parameters.get(k));
            }
        }
        var targetService = getTargetService(provider.protocol);
        if (targetService == null) {
            throw new IllegalArgumentException("Target service '" + provider.protocol + "' not deployed");
        }
        var cached = dataset.getElement(RESPONSE_NAMESPACE + item.target);
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
            dataset.setElement(RESPONSE_NAMESPACE + item.target, cached);
        }
        mapResponse(item.id, item.output, dataset, new MultiLevelMap(Map.of(RESPONSE, cached)));
    }

    private void mapResponse(String itemId, List<String> output, MultiLevelMap dataset, MultiLevelMap mm) {
        var helper = DataMappingHelper.getInstance();
        for (var k: output) {
            var entry = k.contains(ARROW)? k : k + ARROW + RESULT_NAMESPACE + itemId;
            var sep = entry.lastIndexOf(ARROW);
            var lhs = entry.substring(0, sep).trim();
            var rhs = entry.substring(sep+ARROW.length()).trim();
            var value = helper.getLhsElement(lhs, entry.startsWith("$") ||
                        entry.startsWith(RESPONSE_NAMESPACE) || entry.startsWith(RESPONSE_INDEX)? mm : dataset);
            mapResponseToResult(itemId, rhs, k, dataset, mm, value);
        }
    }

    private void mapResponseToResult(String itemId, String rhs, String entry,
                                     MultiLevelMap dataset, MultiLevelMap mm, Object value) {
        if (value != null) {
            if (MODEL.equals(rhs)) {
                throw new IllegalArgumentException("Using whole 'model' is not allowed in '" + entry + "'");
            }
            // Guarantee that it is a clean copy
            var clone = copyOf(value);
            if (mm.exists(FOR_EACH)) {
                // for resultset, automatically add array "append" command if not given
                dataset.setElement(rhs.startsWith(MODEL_NAMESPACE) || rhs.contains("[]")? rhs : rhs + "[]", clone);
            } else {
                dataset.setElement(rhs, clone);
            }
        } else {
            throw new IllegalArgumentException("Data dictionary '"+itemId+"' output '" + entry + "' resolved to null");
        }
    }

    private void loadInputParameters(Question question, MultiLevelMap dataset) {
        var helper = DataMappingHelper.getInstance();
        for (var text : question.input) {
            var sep = text.lastIndexOf(ARROW);
            var lhs = text.substring(0, sep).trim();
            var rhs = text.substring(sep+ARROW.length()).trim();
            var value = helper.getLhsElement(lhs, dataset);
            if (value != null) {
                dataset.setElement(rhs.startsWith(MODEL_NAMESPACE)? rhs : PARA_NAMESPACE + rhs, copyOf(value));
            }
        }
    }
}
