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

package com.accenture.soa.demo.support;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception handler for the {@code sync-to-async} flow (referenced by the flow's {@code exception:} tag).
 * The flow maps {@code error.code -> status} and {@code error.message -> message} into this task; the
 * returned {@code status} becomes the HTTP status ({@code result.status -> output.status}).
 *
 * <p>Status policy lives here (the HTTP facade), not in the generic building blocks: a 408 from
 * {@code sync.await} (the backend did not reply in time) passes through, while any 5xx - a Kafka publish
 * failure or a Redis registration failure, i.e. the async backend is unreachable - is re-mapped to
 * <b>503 Service Unavailable</b> so the caller can retry or take an alternate path.</p>
 */
@PreLoad(route = "sync.error.handler", instances = 10)
public class SyncErrorHandler implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final int SERVICE_UNAVAILABLE = 503;

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        int status = Utility.getInstance().str2int(String.valueOf(input.getOrDefault("status", 500)));
        if (status >= 500) {
            status = SERVICE_UNAVAILABLE;   // backend unreachable (publish/registration failure) -> retriable 503
        }
        Map<String, Object> error = new HashMap<>();
        error.put("type", "error");
        error.put("status", status);
        error.put("message", input.getOrDefault("message", "Internal error"));
        return error;
    }
}
