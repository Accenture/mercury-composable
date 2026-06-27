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

package org.platformlambda.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.mini.kafka.KafkaHeaders;
import org.platformlambda.sync.SyncRuntime;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The task of the {@code soa-reply} flow, reached when the Kafka flow adapter routes a message from the
 * response topic. It bridges the asynchronous response back into the synchronous facade by handing the
 * payload to the return-route coordinator, which completes the awaiting REST request (cross-pod via the
 * Redis return route).
 */
@PreLoad(route = "soa.reply", instances = 10)
public class SoaReplyTask implements TypedLambdaFunction<byte[], Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, byte[] input, int instance) {
        String cid = headers.get(KafkaHeaders.CORRELATION_ID);
        String payload = new String(input, StandardCharsets.UTF_8);
        boolean delivered = SyncRuntime.coordinator().deliver(cid, payload);
        return Map.of("cid", cid, "delivered", delivered);
    }
}
