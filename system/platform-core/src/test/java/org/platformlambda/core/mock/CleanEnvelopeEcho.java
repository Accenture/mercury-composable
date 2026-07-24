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

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.PostOffice;

import java.util.HashMap;
import java.util.Map;

/**
 * Test probe for the worker's envelope-view sanitization: an EventEnvelope-typed function that
 * reports both of its header views - the delivered envelope's own headers (which must never
 * contain the engine's my_* / x-event-api keys, whatever a peer transported) and its injected
 * input copy (where the my_* metadata legitimately lives) - so a test can assert the boundary
 * between transported data and injected metadata.
 */
@PreLoad(route = "clean.envelope.echo", instances = 10)
public class CleanEnvelopeEcho implements TypedLambdaFunction<EventEnvelope, Object> {

    @Override
    public Object handleEvent(Map<String, String> headers, EventEnvelope input, int instance) {
        var po = new PostOffice(headers, instance);
        Map<String, Object> result = new HashMap<>();
        result.put("envelope_headers", input.getHeaders());
        result.put("injected_cid", po.getMyCorrelationId());
        result.put("injected_route", po.getRoute());
        return result;
    }
}
