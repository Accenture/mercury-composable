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

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.DynamicMessage;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit-tests {@link ProtobufConversions} directly (no Kafka, no registry): a rich proto3 message round-trips
 * Map &rarr; DynamicMessage &rarr; Map across scalars, repeated, enum, nested message, bytes - and an absent
 * field reads back as its proto3 implicit default.
 */
class ProtobufConversionsTest {

    private static final String RICH_PROTO = """
            syntax = "proto3";
            package test;
            message Rich {
              string name = 1;
              int32 count = 2;
              int64 big = 3;
              double ratio = 4;
              bool active = 5;
              repeated string tags = 6;
              Color color = 7;
              Child child = 8;
              bytes blob = 9;
              enum Color { RED = 0; GREEN = 1; BLUE = 2; }
              message Child { string value = 1; }
            }""";

    private static Descriptor descriptor() {
        return new ProtobufSchema(RICH_PROTO).toDescriptor();
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapRoundTripsThroughDynamicMessage() {
        Map<String, Object> input = new HashMap<>();
        input.put("name", "alice");
        input.put("count", 3);                 // Integer -> int32
        input.put("big", 10);                  // Integer -> coerced to int64
        input.put("active", true);
        input.put("tags", List.of("x", "y"));
        input.put("color", "GREEN");           // enum by name
        input.put("child", Map.of("value", "deep"));
        input.put("blob", new byte[]{1, 2, 3});
        // 'ratio' omitted on purpose: proto3 reads it back as its default (0.0).

        DynamicMessage message = ProtobufConversions.toMessage(input, descriptor());
        Map<String, Object> out = ProtobufConversions.fromMessage(message);

        assertEquals("alice", out.get("name"));
        assertEquals(3, out.get("count"));
        assertEquals(10L, out.get("big"), "Integer input coerced to int64");
        assertEquals(0.0, out.get("ratio"), "omitted scalar reads back as the proto3 default");
        assertEquals(true, out.get("active"));
        assertEquals(List.of("x", "y"), out.get("tags"));
        assertEquals("GREEN", out.get("color"));
        assertInstanceOf(Map.class, out.get("child"));
        assertEquals("deep", ((Map<String, Object>) out.get("child")).get("value"));
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) out.get("blob"));
    }

    @Test
    void nonMapForMessageIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ProtobufConversions.toMessage("nope", descriptor()));
    }

    @Test
    void nonCollectionForRepeatedIsRejected() {
        Map<String, Object> input = Map.of("tags", "not-a-list");
        assertThrows(IllegalArgumentException.class, () -> ProtobufConversions.toMessage(input, descriptor()));
    }
}
