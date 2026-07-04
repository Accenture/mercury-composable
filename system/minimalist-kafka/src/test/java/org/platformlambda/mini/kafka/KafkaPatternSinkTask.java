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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Test sink: the task of the {@code kafka-pattern-sink-flow} a {@code topic-pattern} binding routes into.
 * Records the record's own topic/partition/offset (from {@code input.metadata.*}, mapped into headers by
 * the flow) so the test can assert a topic-pattern binding surfaces which concrete matched topic a message
 * actually came from - the reason {@code metadata.*} exists (see {@code KafkaFlowConsumer#toMetadata}).
 */
@PreLoad(route = "kafka.pattern.test.sink", instances = 5)
public class KafkaPatternSinkTask implements TypedLambdaFunction<byte[], Map<String, Object>> {

    static final BlockingQueue<Map<String, Object>> RECEIVED = new ArrayBlockingQueue<>(16);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, byte[] input, int instance) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("cid", headers.get(KafkaHeaders.CORRELATION_ID));
        entry.put("body", new String(input, StandardCharsets.UTF_8));
        entry.put("topic", headers.get("topic"));
        entry.put("partition", Integer.valueOf(headers.get("partition")));
        entry.put("offset", Long.valueOf(headers.get("offset")));
        RECEIVED.add(entry);
        return Map.of("status", "received");
    }
}
