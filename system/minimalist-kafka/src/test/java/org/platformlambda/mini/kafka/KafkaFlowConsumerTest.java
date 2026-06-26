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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The poll loop, flow routing, trace propagation, and commit-after-process are exercised end-to-end in
 * {@code KafkaFlowAdapterTest}; here we unit-test the record-to-dataset decoding that feeds the flow.
 */
class KafkaFlowConsumerTest {

    @Test
    @SuppressWarnings("unchecked")
    void decodesRecordIntoFlowDataset() {
        ConsumerRecord<String, byte[]> consumerRecord =
                new ConsumerRecord<>("topic-1", 0, 0L, "k", "{\"a\":1}".getBytes(UTF_8));
        consumerRecord.headers().add("cid", "cid-1".getBytes(UTF_8));
        consumerRecord.headers().add("traceparent",
                "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01".getBytes(UTF_8));

        Map<String, Object> dataset = KafkaFlowConsumer.toDataset(consumerRecord);

        Map<String, String> headers = (Map<String, String>) dataset.get("header");
        assertEquals("cid-1", headers.get("cid"));
        assertEquals("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01", headers.get("traceparent"));
        assertEquals("{\"a\":1}", new String((byte[]) dataset.get("body"), UTF_8));
    }
}
