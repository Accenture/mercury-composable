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

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericRecord;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts between the plain Java the Event Script flows use ({@code Map}/{@code List}/primitives) and the
 * Avro {@link GenericRecord} graph the Confluent Avro serializer reads and writes.
 *
 * <p><b>Map &rarr; Avro</b> ({@link #toAvro}) walks the writer schema rather than the value, so a field that
 * is <i>absent</i> from the {@code Map} takes its <b>schema default</b> (and a missing field with no default
 * fails fast, as Avro requires). This is why an open/partial input - e.g. a request carrying only some of a
 * record's fields - serializes cleanly, which Avro's own strict JSON decoder would reject. It handles records,
 * arrays, maps, enums, the common {@code [null, X]} nullable union, and primitives (with numeric coercion so
 * JSON's {@code Integer}/{@code Double} fit Avro's {@code int}/{@code long}/{@code float}).</p>
 *
 * <p><b>Avro &rarr; Map</b> ({@link #fromAvro}) renders a decoded record back to a {@code Map} (and
 * {@code Utf8 -> String}, {@code GenericArray -> List}, etc.) for the flow body.</p>
 */
final class AvroConversions {

    private AvroConversions() { }

    /** Build the Avro representation of {@code value} against {@code schema}, applying field defaults. */
    static Object toAvro(Object value, Schema schema) {
        return switch (schema.getType()) {
            case RECORD -> toRecord(value, schema);
            case UNION -> toUnion(value, schema);
            case ARRAY -> toArray(value, schema);
            case MAP -> toMap(value, schema);
            case ENUM -> new GenericData.EnumSymbol(schema, String.valueOf(value));
            case BYTES, FIXED -> value instanceof byte[] bytes ? ByteBuffer.wrap(bytes) : value;
            case STRING -> value == null ? null : value.toString();
            case INT -> value instanceof Number n ? n.intValue() : value;
            case LONG -> value instanceof Number n ? n.longValue() : value;
            case FLOAT -> value instanceof Number n ? n.floatValue() : value;
            case DOUBLE -> value instanceof Number n ? n.doubleValue() : value;
            default -> value;   // BOOLEAN, NULL
        };
    }

    private static GenericRecord toRecord(Object value, Schema schema) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("expected a Map for Avro record " + schema.getFullName());
        }
        GenericRecord genericRecord = new GenericData.Record(schema);
        for (Schema.Field field : schema.getFields()) {
            if (map.containsKey(field.name())) {
                genericRecord.put(field.pos(), toAvro(map.get(field.name()), field.schema()));
            } else {
                // No value supplied: use the field's schema default (throws if the field has no default,
                // which is the correct Avro behavior - a required field must be present).
                genericRecord.put(field.pos(), GenericData.get().getDefaultValue(field));
            }
        }
        return genericRecord;
    }

    private static Object toUnion(Object value, Schema schema) {
        if (value == null) {
            return null;   // a null is valid only if the union has a NULL branch; the writer validates
        }
        // Resolve against the first non-null branch - covers the common nullable union [null, X].
        for (Schema branch : schema.getTypes()) {
            if (branch.getType() != Schema.Type.NULL) {
                return toAvro(value, branch);
            }
        }
        return value;
    }

    private static GenericData.Array<Object> toArray(Object value, Schema schema) {
        if (!(value instanceof Collection<?> items)) {
            throw new IllegalArgumentException("expected a Collection for Avro array");
        }
        GenericData.Array<Object> array = new GenericData.Array<>(items.size(), schema);
        for (Object item : items) {
            array.add(toAvro(item, schema.getElementType()));
        }
        return array;
    }

    private static Map<String, Object> toMap(Object value, Schema schema) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("expected a Map for Avro map");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        map.forEach((k, v) -> out.put(String.valueOf(k), toAvro(v, schema.getValueType())));
        return out;
    }

    /** Render a decoded Avro value back to plain Java ({@code Map}/{@code List}/{@code String}/primitive). */
    static Object fromAvro(Object value) {
        if (value instanceof GenericRecord genericRecord) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Schema.Field field : genericRecord.getSchema().getFields()) {
                map.put(field.name(), fromAvro(genericRecord.get(field.pos())));
            }
            return map;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((k, v) -> out.put(String.valueOf(k), fromAvro(v)));
            return out;
        }
        if (value instanceof Collection<?> items) {
            List<Object> list = new ArrayList<>(items.size());
            items.forEach(item -> list.add(fromAvro(item)));
            return list;
        }
        if (value instanceof GenericEnumSymbol<?> || value instanceof CharSequence) {
            return value.toString();   // Avro enum symbol / Utf8 -> String
        }
        if (value instanceof ByteBuffer buffer) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.duplicate().get(bytes);
            return bytes;
        }
        return value;   // numbers, booleans, null
    }
}
