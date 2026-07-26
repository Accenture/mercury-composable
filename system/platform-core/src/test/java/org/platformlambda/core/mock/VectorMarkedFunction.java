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

package org.platformlambda.core.mock;

import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * Registration-metadata conformance fixture (see test resource registration-vectors/core.json):
 * the marker annotations (@ZeroTracing, @EventInterceptor) stack with @PreLoad in any order -
 * exactly the order-freedom every carrier must preserve - and the @OptionalService condition
 * evaluates true under the assumed configuration, so this function registers.
 */
@PreLoad(route = "vector.marked")
@ZeroTracing
@EventInterceptor
@OptionalService("vector.feature.on")
public class VectorMarkedFunction implements TypedLambdaFunction<EventEnvelope, Void> {

    @Override
    public Void handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        // an interceptor's return value is ignored by the engine
        return null;
    }
}
