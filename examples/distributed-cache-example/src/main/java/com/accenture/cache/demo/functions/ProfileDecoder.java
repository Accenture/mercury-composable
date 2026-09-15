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

package com.accenture.cache.demo.functions;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;

import java.util.Map;

/**
 * Restore a profile Map from the cache's opaque {@code byte[]} value - the inverse of {@link ProfileEncoder}.
 *
 * <p>The input is the value from a {@code v1.cache.redis} GET: the stored bytes, or {@code null} on a cache
 * miss. Bytes are restored with {@link EventEnvelope#of(byte[])} and the profile Map is returned; a miss
 * (null / empty) raises HTTP 404 <i>Profile not found</i>, which the flow's exception handler renders. The
 * Layer 2 flows and Layer 3 graphs compose this helper after the cache GET; Layer 1 does the same inline.
 */
@PreLoad(route = "v1.profile.decode", instances = 10)
public class ProfileDecoder implements LambdaFunction {

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) throws AppException {
        if (input instanceof byte[] bytes && bytes.length > 0) {
            return EventEnvelope.of(bytes).getBody();
        }
        throw new AppException(404, "Profile not found");
    }
}
