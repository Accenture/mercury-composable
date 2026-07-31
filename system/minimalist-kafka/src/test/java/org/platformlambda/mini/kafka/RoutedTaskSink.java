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
 * Test sink for the {@code task://} routing target: the adapter invokes this function DIRECTLY (no flow)
 * with all inbound Kafka record headers copied verbatim onto the input headers and the whole payload as
 * the body - a decoded Map when {@code serializer: 'json'} parsed the record, or the raw byte[] when it
 * could not (the best-effort contract). The {@code Object} input type accepts both shapes. Records what
 * it received so the test can assert the dispatch contract, {@code my_correlation_id} injection from the
 * {@code my_cid} tag, and trace continuity.
 */
@PreLoad(route = "routed.task.sink", instances = 5)
public class RoutedTaskSink implements TypedLambdaFunction<Object, Map<String, Object>> {

    static final BlockingQueue<Map<String, Object>> RECEIVED = new ArrayBlockingQueue<>(16);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Object input, int instance) {
        PostOffice po = new PostOffice(headers, instance);
        Map<String, Object> entry = new HashMap<>();
        entry.put("headers", new HashMap<>(headers));
        entry.put("myCid", po.getMyCorrelationId());
        entry.put("traceId", po.getTraceId());
        entry.put("body", input);
        RECEIVED.add(entry);
        return Map.of("status", "received");
    }
}
