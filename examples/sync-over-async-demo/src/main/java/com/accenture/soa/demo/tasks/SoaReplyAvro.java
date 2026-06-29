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

package com.accenture.soa.demo.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.mini.kafka.KafkaHeaders;
import org.platformlambda.sync.SyncRuntime;

import java.util.Map;

/**
 * Avro variant of the extension's {@code soa.reply} task, for the {@code soa-reply-avro} flow on the facade
 * pod (bound to the {@code avro-topic-2} response topic).
 *
 * <p>Identical in behavior to {@link SoaReplyJson}: once the adapter has decoded the Confluent-framed reply
 * to a {@code Map} (this binding sets {@code schema.enabled}), the reply handling is schema-type-agnostic -
 * serialize the Map to a JSON string and hand it to the return-route coordinator, which completes the
 * awaiting REST request (cross-pod via the Redis return route), so {@code sync.await} returns that body.</p>
 */
@PreLoad(route = "soa.reply.avro", instances = 250)
public class SoaReplyAvro implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        String cid = headers.get(KafkaHeaders.CORRELATION_ID);
        String payload = SimpleMapper.getInstance().getCompactGson().toJson(input);
        boolean delivered = SyncRuntime.coordinator().deliver(cid, payload);
        return Map.of("cid", cid, "delivered", delivered);
    }
}
