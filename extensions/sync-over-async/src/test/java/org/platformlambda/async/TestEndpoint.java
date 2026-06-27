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

package org.platformlambda.async;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.Utility;
import org.platformlambda.mini.kafka.KafkaHeaders;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * The synchronous REST facade entry point and the only task of the {@code sync-to-async} flow.
 *
 * <p>It runs the full sync-over-async round-trip on its (virtual) thread: allocate a correlation-id,
 * register the cross-pod return route in Redis ({@code begin}), publish the request to the Kafka request
 * topic via the composable {@code simple.kafka.notification} function (drop-n-forget), then <b>block
 * waiting</b> for the response that the asynchronous backend returns through the Kafka response topic and
 * the Redis return route ({@code awaitResponse}). On success it returns the response body (HTTP 200); on
 * timeout it raises {@link AppException} 408, which the flow's exception handler maps to an HTTP 408.</p>
 *
 * <p>Trace context is carried automatically: {@code po.send} chains the notification onto this task's
 * span, and {@code simple.kafka.notification} stamps the outbound Kafka traceparent from that span.</p>
 */
@PreLoad(route = "test.endpoint", instances = 5)
public class TestEndpoint implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    private static final String TIMEOUT_HEADER = "x-sync-timeout";
    private static final long DEFAULT_TIMEOUT_MS = 10_000;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance)
            throws InterruptedException {
        String correlationId = Utility.getInstance().getUuid();
        long timeoutMillis = parseTimeout(headers.get(TIMEOUT_HEADER));
        byte[] payload = SimpleMapper.getInstance().getMapper().writeValueAsBytes(input);

        var coordinator = org.platformlambda.sync.SyncRuntime.coordinator();
        CompletableFuture<String> future = coordinator.begin(correlationId);
        publishRequest(headers, instance, correlationId, payload);

        try {
            String responseJson = coordinator.awaitResponse(correlationId, future, timeoutMillis);
            return SimpleMapper.getInstance().getMapper().readValue(responseJson, Map.class);   // HTTP 200 + body
        } catch (TimeoutException e) {
            throw new AppException(408, "Timed out awaiting response for " + correlationId);
        }
    }

    /** Drop-n-forget publish to the Kafka request topic via the composable notification function. */
    private static void publishRequest(Map<String, String> headers, int instance,
                                       String correlationId, byte[] payload) {
        EventEnvelope notification = new EventEnvelope().setTo("simple.kafka.notification")
                .setHeader(KafkaHeaders.TOPIC, SyncRuntime.REQUEST_TOPIC)
                .setHeader(KafkaHeaders.CORRELATION_ID, correlationId)
                .setBody(payload);
        new PostOffice(headers, instance).send(notification);
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
