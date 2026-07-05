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

package org.platformlambda.common;

import org.platformlambda.core.models.CustomSerializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

// Test-only CustomSerializer that plugs a Jackson mapper into platform-core's serializer extension point
// (the default is Gson). Uses Jackson 3 (tools.jackson), which Spring Boot 4 already puts on the compile
// classpath - so this needs no separate Jackson dependency and keeps Jackson 2 off platform-core entirely.
public class JacksonSerializer implements CustomSerializer {

    // Jackson 3's builder is the immutable configuration API; FAIL_ON_UNKNOWN_PROPERTIES is off by default
    // in Jackson 3, but we disable it explicitly so the intent survives any future default change.
    private static final JsonMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> toMap(Object obj) {
        return (Map<String, Object>) mapper.convertValue(obj, Map.class);
    }

    @Override
    public <T> T toPoJo(Object obj, Class<T> toValueType) {
        return mapper.convertValue(obj, toValueType);
    }
}
