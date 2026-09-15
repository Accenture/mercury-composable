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
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;

import java.util.Map;

/**
 * Serialise a profile Map into the opaque {@code byte[]} value the cache stores.
 *
 * <p>The profile (a Map of key-values) is held in an {@link EventEnvelope} and serialised with
 * {@link EventEnvelope#toBytes()}. Using the envelope as the holder means the exact same wire format is
 * read back by {@link ProfileDecoder#restore}, by the Layer 1 function in code, and - once it ships - by the
 * Rust port: one MsgPack envelope, portable across layers and languages. The Layer 2 flows and Layer 3 graphs
 * compose this helper with the {@code v1.cache.redis} PUT; Layer 1 does the same inline in code.
 */
@PreLoad(route = "v1.profile.encode", instances = 10)
public class ProfileEncoder implements TypedLambdaFunction<Map<String, Object>, byte[]> {

    @Override
    public byte[] handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        return new EventEnvelope().setBody(input).toBytes();
    }
}
