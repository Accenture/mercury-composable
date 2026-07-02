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

package org.platformlambda.support;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.util.Utility;
import org.platformlambda.sync.SyncRuntime;

import java.util.HashMap;
import java.util.Map;

/**
 * Flow exception handler for {@code sync-to-async}. The flow maps {@code error.code -> status} and
 * {@code error.message -> message} into this task; the returned {@code status} is mapped back to the
 * HTTP status ({@code result.status -> output.status}).
 *
 * <p>Status policy lives here (the HTTP facade), not in the generic {@code simple.kafka.notification}
 * building block: a 408 from {@code sync.await} (timeout) passes through, while any 5xx - a Kafka publish
 * failure or a Redis return-route registration failure, i.e. the async backend is unreachable - is
 * re-mapped to <b>503 Service Unavailable</b>, so the caller can retry or take an alternate path.</p>
 */
@PreLoad(route = "sync.error.handler", instances = 5)
public class SyncErrorHandler implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final int SERVICE_UNAVAILABLE = 503;

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        // cancel the pending entry registered by sync.prepare when the publish step fails (fail-fast path)
        Object cid = input.get("cid");
        if (cid != null) {
            SyncRuntime.coordinator().abort(String.valueOf(cid));
        }
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
