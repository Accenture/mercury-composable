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

package org.platformlambda.core.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@PreLoad(route = "hello.world, hello.alias", instances = 10, isPrivate = false, envInstances = "instances.hello.world")
public class HelloWorld implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(HelloWorld.class);
    private static final String EXCEPTION = "exception";
    private static final String NEST_EXCEPTION = "nested_exception";
    private static final String SIMULATE_DELAY = "Simulate delay";
    private static final String SIMULATE_NESTED = "Simulate nested SQL exception";
    private static final String SIMULATE_EXCEPTION = "Simulate exception";
    private static final String JUST_A_TEST = "just a test";
    private static final String EMPTY = "empty";
    private static final String SQL_ERROR = "sql error";
    private static final long TIMEOUT = 1000L;

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) throws Exception {
        String body = input == null? EMPTY : String.valueOf(input);
        var counter = Utility.getInstance().str2int(body);
        if (counter % 2 == 0) {
            log.info("{} for {} ms", SIMULATE_DELAY, TIMEOUT);
            // Simulate slow response. Unlike Thread.sleep in Java, Kotlin's delay API is non-blocking
            Thread.sleep(TIMEOUT);
        }
        if (headers.containsKey(EXCEPTION)) {
            log.info(SIMULATE_EXCEPTION);
            throw new AppException(400, JUST_A_TEST);
        }
        if (headers.containsKey(NEST_EXCEPTION)) {
            log.info(SIMULATE_NESTED);
            throw new AppException(400, JUST_A_TEST, new SQLException(SQL_ERROR));
        }
        var result = new HashMap<>();
        result.put("headers", filterMetadata(headers));
        if (input != null) {
            result.put("body", input);
        }
        result.put("instance", instance);
        result.put("counter", counter);
        result.put("origin", Platform.getInstance().getOrigin());
        return result;
    }

    private Map<String, String> filterMetadata(Map<String, String> map) {
        Map<String, String> result = new HashMap<>();
        map.keySet().forEach(k -> {
            if (!k.startsWith("my_")) {
                result.put(k, map.get(k));
            }
        });
        return result;
    }
}
