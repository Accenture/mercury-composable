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
 * The Confluent schema types. These match the {@code schemaType} the registry reports
 * ({@code io.confluent.kafka.schemaregistry.ParsedSchema#schemaType()}) and the {@code schema-type} header
 * a producer may send.
 *
 * <p>{@link #JSON} and {@link #AVRO} are wired. {@link #PROTOBUF} is recognized (so the dispatcher fails
 * clearly, via {@code SchemaCodec}'s {@code UnsupportedOperationException}, rather than with a
 * {@code NullPointerException} or an unknown-enum error) but has <b>no registered serde</b>: Confluent's
 * {@code kafka-protobuf-provider} depends on {@code com.squareup.wire:wire-runtime-jvm}, a discontinued
 * artifact carrying an unpatched CVE (CVE-2026-45799 / GHSA-7xpr-hc2w-34m9, a crafted 10-byte payload can
 * crash any Wire-decoding consumer). Wire's maintainers will not patch that coordinate; the fix exists only
 * under the renamed {@code wire-runtime} artifact, which Confluent has not adopted (confirmed unchanged as
 * of {@code kafka-protobuf-provider:8.3.0}). Re-wiring Protobuf is tracked, not abandoned - see the project
 * backlog - and can also be reintroduced early for a specific field installation that explicitly accepts the
 * residual risk.</p>
 */
public enum SchemaType {
    JSON,
    AVRO,
    PROTOBUF;

    /**
     * Parse a type name case-insensitively.
     *
     * @param value the type name (e.g. {@code "AVRO"}); {@code null}/blank defaults to {@link #JSON}
     * @return the matching {@link SchemaType}
     * @throws IllegalArgumentException if {@code value} is non-blank but not a known type
     */
    public static SchemaType from(String value) {
        if (value == null || value.isBlank()) {
            return JSON;
        }
        return SchemaType.valueOf(value.trim().toUpperCase());
    }
}
