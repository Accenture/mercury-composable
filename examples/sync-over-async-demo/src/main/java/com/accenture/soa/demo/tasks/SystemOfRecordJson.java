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
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * JSON Schema variant of {@link SystemOfRecord}, the task of the {@code system-of-record-json} flow on the
 * backend pod (bound to the {@code json-topic-1} request topic).
 *
 * <p>Because that binding sets {@code schema.enabled}, the Kafka flow adapter decodes the Confluent-framed
 * value and hands this task the request as a {@code Map} (not raw byte[]) - the only difference from
 * {@link SystemOfRecord}. It delegates to the shared {@link SystemOfRecord#process} logic; the flow then
 * re-encodes the reply with a schema id and publishes it to {@code json-topic-2}.</p>
 */
@PreLoad(route = "system.of.record.json", instances = 50)
public class SystemOfRecordJson implements TypedLambdaFunction<Map<String, Object>, EventEnvelope> {

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        return SystemOfRecord.process(input, headers, instance);
    }
}
