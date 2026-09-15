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
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.serializers.MsgPack;

import java.io.IOException;
import java.util.Map;

/**
 * Serialise a profile Map into the opaque {@code byte[]} value the cache stores.
 *
 * <p>The profile (a Map of key-values) is packed with {@link MsgPack#packMapOrList(Object)} - the general
 * purpose codec, which adds neither an {@code EventEnvelope} id/header wrapper nor the {@code _T}/{@code _D}
 * type encoding that {@link MsgPack#pack(Object)} uses for event payloads. So the value is compact, a
 * profile is restored exactly as stored, and the same bytes are read back by {@link ProfileDecoder}, by the
 * Layer 1 function in code, and - once it ships - by the Rust port. Plain MsgPack key-values is the
 * storage-efficient, language-portable form: the compacted {@code EventEnvelope} encoding is a Java wire
 * format the Rust port does not read. The Layer 2 flows and Layer 3 graphs compose this helper with the
 * {@code v1.cache.redis} PUT; Layer 1 does the same inline in code.
 */
@PreLoad(route = "v1.profile.encode", instances = 10)
public class ProfileEncoder implements TypedLambdaFunction<Map<String, Object>, byte[]> {
    private static final MsgPack msgPack = new MsgPack();

    @Override
    public byte[] handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) throws IOException {
        return msgPack.packMapOrList(input);
    }
}
