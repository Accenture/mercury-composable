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

package org.platformlambda.mini.kafka.schema;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the {@link SchemaCodec} dispatch guard: a {@link SchemaType} with no registered {@link SchemaSerde}
 * fails clearly rather than silently mis-encoding. Uses the package-private constructor with an empty serde
 * map so no registry is needed. (All three types are wired in production via {@code fromConfig}; this guards
 * the path should a future type be requested before its serde lands.)
 */
class SchemaCodecDispatchTest {

    @Test
    void serializeWithNoSerdeForTypeThrows() {
        SchemaCodec codec = new SchemaCodec(null, new EnumMap<>(SchemaType.class));
        assertThrows(UnsupportedOperationException.class,
                () -> codec.serialize("topic", SchemaType.JSON, 1, Map.of("a", "b")));
    }
}
