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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Test sink for the schema-decoding path: unlike {@link KafkaSinkTask} (raw byte[]), this receives the
 * <b>decoded Map</b> that the adapter produced from a Confluent-framed message. Records the body + trace
 * so the test can assert decoding and trace continuity.
 */
@PreLoad(route = "schema.test.sink", instances = 5)
public class SchemaSinkTask implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    static final BlockingQueue<Map<String, Object>> RECEIVED = new ArrayBlockingQueue<>(16);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        PostOffice po = new PostOffice(headers, instance);
        Map<String, Object> entry = new HashMap<>();
        entry.put("cid", headers.get(KafkaHeaders.CORRELATION_ID));
        // the business correlation-id surfaced to the task as model.cid (see KafkaSinkTask)
        entry.put("myCid", po.getMyCorrelationId());
        entry.put("traceId", po.getTraceId());
        entry.put("body", input);
        RECEIVED.add(entry);
        return Map.of("status", "received");
    }
}
