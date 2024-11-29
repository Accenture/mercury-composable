/*

    Copyright 2018-2024 Accenture Technology

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
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.util.ManagedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@PreLoad(route = "v1.ext.state.machine, v1.ext.state.machine.2")
public class ExternalStateMachine implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(ExternalStateMachine.class);

    private static final ManagedCache store = ManagedCache.createCache("state.machine", 5000);
    private static final String TYPE = "type";
    private static final String PUT = "put";
    private static final String GET = "get";
    private static final String REMOVE = "remove";
    private static final String KEY = "key";

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        if (!headers.containsKey(KEY)) {
            throw new IllegalArgumentException("Missing key in headers");
        }
        String type = headers.get(TYPE);
        String key = headers.get(KEY);
        if (PUT.equals(type) && input != null) {
            log.info("Saving {} to store", key);
            store.put(key, input);
            return true;
        }
        if (GET.equals(type)) {
            Object v = store.get(key);
            if (v != null) {
                log.info("Retrieve {} from store", key);
                return v;
            } else {
                return null;
            }
        }
        if (REMOVE.equals(type)) {
            if (store.exists(key)) {
                store.remove(key);
                log.info("Removed {} from store", key);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
