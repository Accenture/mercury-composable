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
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit-tests {@link AvroConversions} directly (no Kafka, no registry): a rich schema round-trips
 * Map &rarr; GenericRecord &rarr; Map across records, arrays, maps, enums, nullable unions, numeric coercion,
 * bytes - and field defaults are applied for absent fields while a missing required field fails fast.
 */
class AvroConversionsTest {

    private static final String RICH_SCHEMA = """
            {"type":"record","name":"Rich","namespace":"test","fields":[
              {"name":"name","type":"string"},
              {"name":"count","type":"int"},
              {"name":"big","type":"long"},
              {"name":"ratio","type":"double"},
              {"name":"active","type":"boolean"},
              {"name":"nickname","type":["null","string"],"default":null},
              {"name":"tags","type":{"type":"array","items":"string"}},
              {"name":"attrs","type":{"type":"map","values":"string"}},
              {"name":"color","type":{"type":"enum","name":"Color","symbols":["RED","GREEN","BLUE"]}},
              {"name":"child","type":["null",{"type":"record","name":"Child","fields":[
                  {"name":"value","type":"string"}]}],"default":null},
              {"name":"blob","type":"bytes"},
              {"name":"status","type":"string","default":"new"}
            ]}""";

    private static Schema schema() {
        return new Schema.Parser().parse(RICH_SCHEMA);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapRoundTripsThroughGenericRecord() {
        Map<String, Object> input = Map.of(
                "name", "alice",
                "count", 3,               // Integer -> Avro int
                "big", 10,                // Integer -> coerced to Avro long
                "ratio", 1.5,
                "active", true,
                "nickname", "al",         // resolves against the non-null union branch
                "tags", List.of("x", "y"),
                "attrs", Map.of("k", "v"),
                "color", "GREEN",         // enum symbol
                "blob", new byte[]{1, 2, 3});
        // 'child' and 'status' omitted on purpose: child defaults to null, status to "new".

        Object avro = AvroConversions.toAvro(input, schema());
        assertInstanceOf(GenericRecord.class, avro);

        Map<String, Object> out = (Map<String, Object>) AvroConversions.fromAvro(avro);
        assertEquals("alice", out.get("name"));
        assertEquals(3, out.get("count"));
        assertEquals(10L, out.get("big"), "Integer input coerced to Avro long");
        assertEquals(1.5, out.get("ratio"));
        assertEquals(true, out.get("active"));
        assertEquals("al", out.get("nickname"));
        assertEquals(List.of("x", "y"), out.get("tags"));
        assertEquals(Map.of("k", "v"), out.get("attrs"));
        assertEquals("GREEN", out.get("color"));
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) out.get("blob"));
        assertNull(out.get("child"), "absent nullable field takes its null default");
        assertEquals("new", out.get("status"), "absent field takes its schema default");
    }

    @Test
    @SuppressWarnings("unchecked")
    void nestedRecordRoundTrips() {
        Map<String, Object> input = Map.of(
                "name", "bob", "count", 1, "big", 1, "ratio", 0.0, "active", false,
                "tags", List.of(), "attrs", Map.of(), "color", "RED", "blob", new byte[0],
                "child", Map.of("value", "deep"));

        Object avro = AvroConversions.toAvro(input, schema());
        Map<String, Object> out = (Map<String, Object>) AvroConversions.fromAvro(avro);
        assertInstanceOf(Map.class, out.get("child"));
        assertEquals("deep", ((Map<String, Object>) out.get("child")).get("value"));
    }

    @Test
    void missingRequiredFieldFailsFast() {
        // 'name' has no default; omitting it must fail rather than silently produce a partial record.
        Map<String, Object> input = Map.of("count", 1, "big", 1, "ratio", 0.0, "active", false,
                "tags", List.of(), "attrs", Map.of(), "color", "RED", "blob", new byte[0]);
        var recordSchema = schema();
        assertThrows(RuntimeException.class, () -> AvroConversions.toAvro(input, recordSchema));
    }

    @Test
    void nonMapForRecordIsRejected() {
        var recordSchema = schema();
        assertThrows(IllegalArgumentException.class, () -> AvroConversions.toAvro("not-a-map", recordSchema));
    }
}
