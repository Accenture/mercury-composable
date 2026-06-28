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

package org.platformlambda.helpers.registry.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Collections;
import java.util.Map;

/**
 * Health check endpoint for the Schema Registry mock.
 * Returns an empty JSON object which satisfies Confluent client startup checks.
 */
@PreLoad(route = "schema.registry.health")
public class HealthCheckFunction implements TypedLambdaFunction<EventEnvelope, Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        return Collections.emptyMap();
    }
}
