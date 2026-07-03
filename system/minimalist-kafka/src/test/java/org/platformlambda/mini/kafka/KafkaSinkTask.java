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

package org.platformlambda.mini.kafka;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Test sink: the task of the {@code kafka-sink-flow} the adapter routes a topic into. It records what it
 * received (correlation-id, current trace-id, body) so the test can assert delivery and trace continuity.
 */
@PreLoad(route = "kafka.test.sink", instances = 5)
public class KafkaSinkTask implements TypedLambdaFunction<byte[], Map<String, Object>> {

    static final BlockingQueue<Map<String, Object>> RECEIVED = new ArrayBlockingQueue<>(16);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, byte[] input, int instance) {
        PostOffice po = new PostOffice(headers, instance);
        Map<String, Object> entry = new HashMap<>();
        entry.put("cid", headers.get(KafkaHeaders.CORRELATION_ID));
        // the business correlation-id surfaced to the task as model.cid (KafkaFlowConsumer stamps the
        // inbound Kafka cid as the flow's correlation-id, which survives the RPC hop as this header)
        entry.put("myCid", po.getMyCorrelationId());
        entry.put("traceId", po.getTraceId());
        entry.put("body", new String(input, StandardCharsets.UTF_8));
        RECEIVED.add(entry);
        return Map.of("status", "received");
    }
}
