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

package org.platformlambda.quartz.rest;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.scheduler.ActiveEnvironment;

import java.util.Date;
import java.util.Map;

/**
 * The operations dashboard's switch (RFC-0008): {@code GET /api/scheduler/environment} answers this
 * instance's environment, the active one and the mode; {@code POST /api/scheduler/environment} with
 * {@code {"active": "DR", "operator": "..."}} moves the active environment - persisted through the
 * environment store, so every instance of every site follows at its next scheduled job.
 */
@PreLoad(route = "v1.environment.admin", instances = 10)
public class EnvironmentAdmin implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {
    private static final String CONTENT_TYPE = "content-type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String ACTIVE = "active";
    private static final String OPERATOR = "operator";

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        var env = ActiveEnvironment.getInstance();
        if ("POST".equals(input.getMethod())) {
            if (input.getBody() instanceof Map<?, ?> data && data.get(ACTIVE) instanceof String name
                    && data.get(OPERATOR) instanceof String operator && !operator.isBlank()) {
                var result = env.activate(name, operator);
                result.put("message", "Active environment set to " + result.get(ACTIVE));
                result.put("time", new Date());
                return new EventEnvelope().setBody(result).setHeader(CONTENT_TYPE, APPLICATION_JSON);
            }
            throw new IllegalArgumentException("Missing 'active' or 'operator' parameter in request payload");
        }
        var result = env.status();
        result.put("message", "POST /api/scheduler/environment with {active, operator} to move the active environment");
        result.put("time", new Date());
        return new EventEnvelope().setBody(result).setHeader(CONTENT_TYPE, APPLICATION_JSON);
    }
}
