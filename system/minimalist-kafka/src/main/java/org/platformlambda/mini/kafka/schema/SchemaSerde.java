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
 * The per-{@link SchemaType} half of the {@link SchemaCodec}: it owns the Confluent serializer/deserializer
 * for one schema type and the conversion to/from the plain {@code Map} that flows carry. {@link SchemaCodec}
 * picks the implementation - by the {@code schema-type} header on produce, by the registered schema's type
 * (looked up from the embedded id) on consume - so adding a type (Avro, Protobuf) is just another
 * implementation, with no change to the dispatcher, producer, or consumer.
 */
interface SchemaSerde {

    /**
     * Serialize {@code value} (typically a {@code Map}) into the Confluent wire format for a pre-registered
     * global {@code schemaId}, using this type's Confluent serializer ({@code use.schema.id}, no auto-register).
     *
     * @param topic    the destination topic (a serde may derive its subject from it)
     * @param schemaId the pre-registered global schema id to frame with
     * @param value    the value to serialize
     * @return the Confluent-framed bytes
     */
    byte[] serialize(String topic, int schemaId, Object value);

    /**
     * Decode Confluent-framed bytes (the embedded id selected this serde) back into a plain {@code Map} for
     * the flow dataset body.
     *
     * @param topic the source topic
     * @param data  the Confluent-framed bytes
     * @return the decoded value (a {@code Map})
     */
    Object decode(String topic, byte[] data);
}
