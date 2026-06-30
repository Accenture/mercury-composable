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

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts between the plain Java the Event Script flows use ({@code Map}/{@code List}/primitives) and the
 * protobuf {@link DynamicMessage} graph the Confluent Protobuf serializer reads and writes - <b>no generated
 * classes</b>, the message is built reflectively against the registered schema's {@link Descriptor}.
 *
 * <p><b>Map &rarr; message</b> ({@link #toMessage}) walks the descriptor's fields; a field <i>absent</i> from
 * the {@code Map} is simply not set, so proto3's implicit default ({@code 0}/{@code ""}/{@code false}) applies
 * - the same "partial input serializes cleanly" behavior as the Avro path, but free in proto3. It handles
 * scalars (with numeric coercion), enums, nested messages, and repeated fields. <b>message &rarr; Map</b>
 * ({@link #fromMessage}) renders a decoded message back to a {@code Map} (proto3 unset scalars read as their
 * default; an unset singular message is {@code null}).</p>
 */
final class ProtobufConversions {

    private ProtobufConversions() { }

    /** Build a {@link DynamicMessage} for {@code value} against {@code descriptor}. */
    static DynamicMessage toMessage(Object value, Descriptor descriptor) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("expected a Map for protobuf message " + descriptor.getFullName());
        }
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(descriptor);
        for (FieldDescriptor field : descriptor.getFields()) {
            Object fieldValue = map.get(field.getName());
            if (fieldValue == null) {
                continue;   // proto3: leave unset -> implicit default
            }
            if (field.isRepeated()) {
                if (!(fieldValue instanceof Collection)) {
                    throw new IllegalArgumentException("expected a Collection for repeated field " + field.getName());
                }
                for (Object item : (Collection<?>) fieldValue) {
                    builder.addRepeatedField(field, toField(item, field));
                }
            } else {
                builder.setField(field, toField(fieldValue, field));
            }
        }
        return builder.build();
    }

    private static Object toField(Object value, FieldDescriptor field) {
        return switch (field.getJavaType()) {
            case INT -> value instanceof Number n ? n.intValue() : value;
            case LONG -> value instanceof Number n ? n.longValue() : value;
            case FLOAT -> value instanceof Number n ? n.floatValue() : value;
            case DOUBLE -> value instanceof Number n ? n.doubleValue() : value;
            case STRING -> String.valueOf(value);
            case BYTE_STRING -> value instanceof byte[] bytes ? ByteString.copyFrom(bytes) : value;
            case ENUM -> field.getEnumType().findValueByName(String.valueOf(value));
            case MESSAGE -> toMessage(value, field.getMessageType());
            default -> value;   // BOOLEAN
        };
    }

    /** Render a decoded protobuf {@link Message} back to a plain {@code Map}. */
    static Map<String, Object> fromMessage(Message message) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (FieldDescriptor field : message.getDescriptorForType().getFields()) {
            if (field.isRepeated()) {
                int count = message.getRepeatedFieldCount(field);
                List<Object> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    list.add(fromField(message.getRepeatedField(field, i)));
                }
                out.put(field.getName(), list);
            } else if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE && !message.hasField(field)) {
                out.put(field.getName(), null);   // unset singular message
            } else {
                out.put(field.getName(), fromField(message.getField(field)));
            }
        }
        return out;
    }

    private static Object fromField(Object value) {
        if (value instanceof Message nested) {
            return fromMessage(nested);
        }
        if (value instanceof com.google.protobuf.Descriptors.EnumValueDescriptor enumValue) {
            return enumValue.getName();
        }
        if (value instanceof ByteString bytes) {
            return bytes.toByteArray();
        }
        return value;   // Integer, Long, Float, Double, Boolean, String
    }
}
