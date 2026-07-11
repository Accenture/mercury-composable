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

package org.platformlambda.twin.kafka;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Test sink for the schema-enabled binding on the SECONDARY cluster: the adapter decodes the
 * Confluent-framed value against the secondary registry and hands the flow a Map - proving the
 * asymmetric real-world topology (no registry on the primary, Confluent registry on the secondary).
 */
@PreLoad(route = "schema.sink.b", instances = 5)
public class SchemaSinkB implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {

    static final BlockingQueue<Map<String, Object>> RECEIVED = new ArrayBlockingQueue<>(16);

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        RECEIVED.add(input);
        return Map.of("status", "received");
    }
}
