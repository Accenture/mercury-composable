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

package com.accenture.cache.demo.functions;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception handler for the Layer 2 flows and the Layer 3 graphs. The flow / graph maps {@code error.code},
 * {@code error.message} and {@code error.stack} into this function's input; it renders the standard error
 * body ({@code type=error}, {@code status}, {@code message}) which the caller receives with the failing HTTP
 * status. A cache miss surfaced by {@code v1.profile.decode} as HTTP 404 <i>Profile not found</i> is rendered
 * here. It also tags the app-context log ({@code updateContext}) so the failing request is attributable.
 */
@PreLoad(route = "v1.profile.exception", instances = 10)
public class ProfileExceptionHandler implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final Logger log = LoggerFactory.getLogger(ProfileExceptionHandler.class);

    private static final String TYPE = "type";
    private static final String ERROR = "error";
    private static final String STATUS = "status";
    private static final String MESSAGE = "message";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        var po = new PostOffice(headers, instance);
        po.updateContext("layer", "flow");
        if (input.containsKey(STATUS) && input.containsKey(MESSAGE)) {
            log.info("Profile flow exception - status={} message={}", input.get(STATUS), input.get(MESSAGE));
            Map<String, Object> error = new HashMap<>();
            error.put(TYPE, ERROR);
            error.put(STATUS, input.get(STATUS));
            error.put(MESSAGE, input.get(MESSAGE));
            return error;
        }
        return Collections.emptyMap();
    }
}
