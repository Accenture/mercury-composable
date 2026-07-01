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

/**
 * The result of resolving a {@code (subject, version)} to a registered schema: the global schema id used to
 * frame the Confluent wire format, and the schema type that selects the serializer. Both are derived from
 * the registry (the type authoritatively from the parsed schema, so it is never guessed).
 *
 * @param id   the global schema id
 * @param type the schema type
 */
public record ResolvedSchema(int id, SchemaType type) {}
