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

package com.accenture.dictionary.base;

import com.accenture.dictionary.loaders.DataDictLoader;
import com.accenture.dictionary.loaders.ProviderLoader;
import com.accenture.dictionary.loaders.QuestionSpecsLoader;
import com.accenture.dictionary.models.DataDictionary;
import com.accenture.dictionary.models.DataProvider;
import com.accenture.dictionary.models.QuestionSpecs;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class DictionaryLambdaFunction implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final Logger log = LoggerFactory.getLogger(DictionaryLambdaFunction.class);
    private static final Utility util = Utility.getInstance();
    private static final ConcurrentMap<String, String> targetServiceStore = new ConcurrentHashMap<>();
    protected static final ConcurrentMap<String, QuestionSpecs> questionStore = new ConcurrentHashMap<>();
    protected static final ConcurrentMap<String, DataDictionary> dataDictStore = new ConcurrentHashMap<>();
    protected static final ConcurrentMap<String, DataProvider> providerStore = new ConcurrentHashMap<>();
    protected static final String ARROW = "->";
    protected static final String MODEL = "model";
    protected static final String MODEL_NAMESPACE = "model.";
    protected static final String INPUT = "input";
    protected static final String OUTPUT_BODY = "output.body";
    protected static final String OUTPUT_HEADER = "output.header";
    protected static final String PARAMETER = "parameter";
    protected static final String PARA_NAMESPACE = "parameter.";
    protected static final String RESULT = "result";
    protected static final String RESPONSE = "response";
    protected static final String RESPONSE_INDEX = "response[";
    protected static final String RESPONSE_NAMESPACE = "response.";
    protected static final String PROVIDER = "provider";
    protected static final String PROVIDER_NAMESPACE = "provider.";
    protected static final String PROVIDER_DEFAULT = "provider.default";
    protected static final String FOR_EACH = "each";
    protected static final String ERROR_CODE = "error.code";
    protected static final String ERROR_MESSAGE = "error.message";
    protected static final String MODEL_TTL = "model.ttl";
    protected static final String TIMEOUT = "timeout";
    protected static final String HEADER_PARAMETER = "header.";
    protected static final String QUERY_PARAMETER = "query.";
    protected static final String PATH_PARAMETER = "path_parameter.";
    protected static final String ASYNC_HTTP_CLIENT = "async.http.request";

    protected QuestionSpecs getQuestionSpecs(String questionId) {
        var result = questionStore.get(questionId);
        if (result == null) {
            var specs = getSpecPath("location.questions", questionId);
            final ConfigReader config;
            try {
                config = new ConfigReader(specs);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Question '"+questionId+"' not found");
            }
            var loader = QuestionSpecsLoader.getInstance();
            var q = loader.loadQuestionSpecs(questionId, config);
            log.info("Loaded question specs '{}' from '{}'", questionId, specs);
            questionStore.put(questionId, q);
            return q;
        } else {
            return result;
        }
    }

    protected DataDictionary getDataDict(String dataId) {
        var result = dataDictStore.get(dataId);
        if (result == null) {
            var specs = getSpecPath("location.data.dictionary", dataId);
            final ConfigReader config;
            try {
                config = new ConfigReader(specs);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Data dictionary item '"+dataId+"' not found");
            }
            var loader = DataDictLoader.getInstance();
            var d = loader.loadDataDict(dataId, config);
            log.info("Loaded data dictionary '{}' from '{}'", dataId, specs);
            dataDictStore.put(dataId, d);
            return d;
        } else {
            return result;
        }
    }

    protected DataProvider getDataProvider(String providerId) {
        var result = providerStore.get(providerId);
        if (result == null) {
            var filename = providerId.replace("://", "-");
            var specs =getSpecPath("location.data.provider", filename);
            final ConfigReader config;
            try {
                config = new ConfigReader(specs);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Data provider '"+providerId+"' not found");
            }
            var loader = ProviderLoader.getInstance();
            var p = loader.loadProvider(providerId, config);
            log.info("Loaded provider '{}' from '{}'", providerId, specs);
            providerStore.put(providerId, p);
            return p;
        } else {
            return result;
        }
    }

    protected String getSpecPath(String location, String questionId) {
        var config = AppConfigReader.getInstance();
        var folder = config.getProperty(location, "file:/tmp/questions");
        return folder + "/" + questionId + ".yml";
    }

    protected void mapHttpInput(AsyncHttpRequest request, Map<String, Object> input, DataProvider provider) {
        var mm = new MultiLevelMap(input);
        var result = new HashMap<String, Object>();
        for (var entry : provider.input) {
            int sep = entry.lastIndexOf(ARROW);
            if (sep != -1) {
                var lhs = entry.substring(0, sep).trim();
                var rhs = entry.substring(sep + ARROW.length()).trim();
                var value = mm.getElement(lhs);
                if (value != null) {
                    if (rhs.startsWith(PATH_PARAMETER)) {
                        var key = rhs.substring(PATH_PARAMETER.length()).trim();
                        request.setPathParameter(key, String.valueOf(value));
                    } else if (rhs.startsWith(QUERY_PARAMETER)) {
                        var key = rhs.substring(QUERY_PARAMETER.length()).trim();
                        request.setQueryParameter(key, String.valueOf(value));
                    } else if (rhs.startsWith(HEADER_PARAMETER)) {
                        var key = rhs.substring(HEADER_PARAMETER.length()).trim();
                        request.setHeader(key, String.valueOf(value));
                    } else {
                        result.put(rhs, value);
                    }
                }
            }
        }
        request.setBody(result);
    }

    protected void mapHttpHeaders(AsyncHttpRequest request, List<String> headers) {
        for (var header : headers) {
            int colon = header.indexOf(':');
            if (colon == -1) {
                throw new IllegalArgumentException("Missing ':' in HTTP header");
            }
            var key = header.substring(0, colon).trim();
            var value = header.substring(colon + 1).trim();
            if (key.isEmpty() || value.isEmpty()) {
                throw new IllegalArgumentException("Invalid HTTP header - "+header);
            }
            request.setHeader(key, value);
        }
    }

    protected String getTargetService(String protocol) {
        var target = targetServiceStore.get(protocol);
        if (target == null) {
            var config = AppConfigReader.getInstance();
            var name = PROVIDER_NAMESPACE + protocol;
            var ts = config.getProperty(name, config.getProperty(PROVIDER_DEFAULT, "simple.http.service"));
            if (util.validServiceName(ts)) {
                targetServiceStore.put(protocol, ts);
                return ts;
            } else {
                throw new IllegalArgumentException("'" + ts + "' for provider '"+name+"' is not a valid route name");
            }
        } else {
            return target;
        }
    }

    @SuppressWarnings("unchecked")
    protected Object copyOf(Object value) {
        return switch (value) {
            case Map<?, ?> data -> util.deepCopy((Map<String, Object>) data);
            case List<?> data -> util.deepCopy((List<Object>) data);
            default -> value;
        };
    }
}
