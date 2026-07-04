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
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;
import org.platformlambda.mini.kafka.KafkaHeaders;
import org.platformlambda.sync.SyncRuntime;

import java.util.Map;

/**
 * First task of a composable sync-over-async REST facade flow. It registers the cross-pod return route in
 * Redis ({@code begin}) and serializes the request body into the outbound payload, keyed by the
 * correlation-id that will track the round-trip.
 *
 * <h3>Correlation-id sourcing</h3>
 * <p>The correlation-id is supplied via the {@code cid} header in the task's input mapping. The default
 * flow maps {@code model.cid -> header.cid}, where {@code model.cid} is the business correlation-id captured
 * at the edge: REST automation reads the configured {@code http.correlation.id.header} (default
 * {@code X-Correlation-Id}) and generates a fresh UUID when it is absent. So the upstream correlation-id
 * threads the round trip with no extra config.</p>
 *
 * <p>Deployments that use a proprietary correlation-id header can either set {@code http.correlation.id.header}
 * to that name, or override the mapping in the flow YAML. For example:
 * <pre>
 *   input:
 *     - 'input.header.x-request-id -> header.cid'   # proprietary upstream header
 *     - 'input.body -> *'
 * </pre>
 * </p>
 *
 * <p>It does <b>not</b> publish or wait: the publish is the next task (so a publish failure fails the flow
 * and is rejected to the caller — fail-fast), and the blocking await is the last task (so it runs after
 * the request is on the wire). Sized for user-facing concurrency ({@code instances = 250}); each instance
 * runs on a virtual thread.</p>
 */
@PreLoad(route = "sync.prepare", instances = 250)
public class SyncPrepareTask implements TypedLambdaFunction<Map<String, Object>, EventEnvelope> {

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        // model.cid is the business correlation-id captured at the edge (from http.correlation.id.header,
        // default X-Correlation-Id, or a fresh UUID). The flow maps it here as header.cid.
        String businessCorrelationId = headers.getOrDefault("cid", Utility.getInstance().getUuid());
        SyncRuntime.coordinator().begin(businessCorrelationId);   // register the return route before publishing
        byte[] payload = SimpleMapper.getInstance().getMapper().writeValueAsBytes(input);
        return new EventEnvelope().setHeader(KafkaHeaders.CORRELATION_ID, businessCorrelationId).setBody(payload);
    }
}
