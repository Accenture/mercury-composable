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

package org.platformlambda.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.sync.ReturnRouteCoordinator;
import org.platformlambda.sync.SyncRuntime;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Last task of a composable sync-over-async REST facade flow. It blocks on its virtual thread until the
 * asynchronous backend's response arrives via the response topic and the Redis return route, then returns
 * the response body (HTTP 200). On timeout it raises {@link AppException} 408, which the flow's exception
 * handler maps to an HTTP 408.
 *
 * <p>The correlation-id was allocated and registered by {@code sync.prepare}; this task awaits by
 * correlation-id ({@link ReturnRouteCoordinator#awaitResponse(String, long)}), since the future from
 * {@code begin} is held by the coordinator rather than passed between tasks. Sized for user-facing
 * concurrency ({@code instances = 250}): each instance blocks on a virtual thread, so the carrier kernel
 * thread is released while it waits.</p>
 */
@PreLoad(route = "sync.await", instances = 250)
public class SyncAwaitTask implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final String TIMEOUT_HEADER = "x-sync-timeout";
    private static final long DEFAULT_TIMEOUT_MS = 10_000;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws InterruptedException {
        String businessCorrelationId = String.valueOf(input.get("cid"));
        long timeoutMillis = parseTimeout(headers.get(TIMEOUT_HEADER));
        try {
            String responseJson = SyncRuntime.coordinator().awaitResponse(businessCorrelationId, timeoutMillis);
            return SimpleMapper.getInstance().getMapper().readValue(responseJson, Map.class);   // HTTP 200 + body
        } catch (TimeoutException e) {
            throw new AppException(408, "Timed out awaiting response for " + businessCorrelationId);
        }
    }

    private static long parseTimeout(String value) {
        if (value == null) {
            return DEFAULT_TIMEOUT_MS;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return DEFAULT_TIMEOUT_MS;
        }
    }
}
