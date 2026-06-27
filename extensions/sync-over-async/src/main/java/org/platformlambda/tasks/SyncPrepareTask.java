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
 * First task of a composable sync-over-async REST facade flow. It allocates a correlation-id, registers
 * the cross-pod return route in Redis ({@code begin}), and serializes the request body into the outbound
 * payload. It returns the payload as the body and the correlation-id as the {@code cid} header; a flow
 * stores both in {@code model} so the next task ({@code simple.kafka.notification}) can publish them and
 * the final task ({@code sync.await}) can block on the same correlation-id.
 *
 * <p>It does <b>not</b> publish or wait: the publish is the next task (so a publish failure fails the flow
 * and is rejected to the caller - fail-fast), and the blocking await is the last task (so it runs after
 * the request is on the wire). Sized for user-facing concurrency ({@code instances = 250}); each instance
 * runs on a virtual thread.</p>
 */
@PreLoad(route = "sync.prepare", instances = 250)
public class SyncPrepareTask implements TypedLambdaFunction<Map<String, Object>, EventEnvelope> {

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String correlationId = Utility.getInstance().getUuid();
        SyncRuntime.coordinator().begin(correlationId);   // register the return route before publishing
        byte[] payload = SimpleMapper.getInstance().getMapper().writeValueAsBytes(input);
        return new EventEnvelope().setHeader(KafkaHeaders.CORRELATION_ID, correlationId).setBody(payload);
    }
}
