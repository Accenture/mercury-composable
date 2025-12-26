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

package com.accenture.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.ManagedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This external state machine function is configured as a singleton. i.e. instances = 1.
 * Therefore, it would guarantee orderly execution of incoming requests.
 * <p>
 * This class can be used as a template to write your own implementation.
 */
@PreLoad(route = "v1.ext.state.machine")
public class ExternalStateMachine implements TypedLambdaFunction<Map<String, Object>, Object> {
    private static final Logger log = LoggerFactory.getLogger(ExternalStateMachine.class);
    // ManagedCache with automatic expiry is used so that memory will be released
    // if the stored object is not cleared programmatically.
    private static final ManagedCache store = ManagedCache.createCache("state.machine", 10000);
    private static final String TYPE = "type";
    private static final String PUT = "put";
    private static final String GET = "get";
    private static final String REMOVE = "remove";
    private static final String CLEAR = "clear";
    private static final String KEY = "key";
    private static final String DATA = "data";
    private static final String APPEND = "append";
    /**
     * In this sample implementation, it assumes that the external state machine is used within a single event flow.
     * Therefore, we are using the Trace-ID as a reference.
     * <p>
     * If your external state machine is used across multiple event flows and multiple application instances,
     * you should not use the Trace-ID as a reference. Instead, use an external database or cache store
     * as a persistent store for the key-values.
     *
     * @param headers of incoming event
     * @param input payload of event
     * @param instance of the event worker
     * @return boolean, null or retrieved object value
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var po = new PostOffice(headers, instance);
        var traceId = po.getTraceId();
        if (traceId == null) {
            throw new IllegalArgumentException("Tracing must be enabled for external state machine");
        }
        String type = headers.get(TYPE);
        // default behavior is to retrieve all keys under the same traceId
        String key = headers.getOrDefault(KEY, "*");
        /*
         * The PUT method must be implemented according to interface contract:
         * key is headers.get("key")
         * value is input.get("data")
         */
        if (PUT.equals(type) && !"*".equals(key)) {
            var data = input.get(DATA);
            if (data != null) {
                var map = (Map<String, Object>) store.get(traceId);
                if (map == null) {
                    map = new HashMap<>();
                }
                // "append" is a special key to append item to a list
                if (APPEND.equals(key)) {
                    var list = (List<Object>) map.getOrDefault(APPEND, new ArrayList<>());
                    list.add(data);
                    map.put(key, list);
                    log.info("append to store {}", traceId);
                } else {
                    map.put(key, data);
                    log.info("Save '{}' to store {}", key, traceId);
                }
                store.put(traceId, map);
                return true;
            }
        }
        // The GET, REMOVE and CLEAR methods are user application dependent and
        // you may implement according to your application needs.
        if (GET.equals(type)) {
            Object v = store.get(traceId);
            // get all keys?
            if ("*".equals(key)) {
                return v;
            } else {
                if (v instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) v;
                    log.info("Retrieve '{}' from store {}", key, traceId);
                    return map.get(key);
                } else {
                    return null;
                }
            }
        }
        if (REMOVE.equals(type)) {
            // removing all keys means CLEAR
            if ("*".equals(key)) {
                type = CLEAR;
            } else {
                Object v = store.get(traceId);
                if (v instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) v;
                    log.info("Remove '{}' from store {}", key, traceId);
                    map.remove(key);
                    return true;
                } else {
                    return false;
                }
            }
        }
        if (CLEAR.equals(type)) {
            if (store.exists(traceId)) {
                store.remove(traceId);
                log.info("Clear store {}", traceId);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
