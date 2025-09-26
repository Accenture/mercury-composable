/*

    Copyright 2018-2025 Accenture Technology

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

package org.platformlambda.core.services;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

import static com.accenture.utils.ComposableConstants.ENV_INSTANCES_PREFIX;

/**
 * This is a convenient no-operation function for event scripts.
 * It is effectively an echo function.
 */
@PreLoad(route = NoOpFunction.ROUTE, instances=500, envInstances = NoOpFunction.ENV_INSTANCE_PROPERTY)
public class NoOpFunction implements TypedLambdaFunction<Map<String, Object>, EventEnvelope> {

    public static final String ROUTE = "no.op";
    public static final String ENV_INSTANCE_PROPERTY = ENV_INSTANCES_PREFIX + NoOpFunction.ROUTE;

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        return new EventEnvelope().setHeaders(headers).setBody(input);
    }
}
