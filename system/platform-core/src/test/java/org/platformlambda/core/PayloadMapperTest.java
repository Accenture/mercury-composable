/*

    Copyright 2018-2024 Accenture Technology

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

package org.platformlambda.core;

import org.junit.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedPayload;
import org.platformlambda.core.serializers.PayloadMapper;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.models.PoJo;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class PayloadMapperTest {

    private static final PayloadMapper converter = PayloadMapper.getInstance();

    @Test
    @SuppressWarnings("unchecked")
    public void optionalTransport() throws IOException {
        String text = "hello world";
        Optional<Object> hello = Optional.of(text);
        EventEnvelope event1 = new EventEnvelope();
        event1.setBody(hello);
        byte[] b = event1.toBytes();
        EventEnvelope event2 = new EventEnvelope();
        event2.load(b);
        assertTrue(event2.getBody() instanceof Optional);
        Optional<Object> value = (Optional<Object>) event2.getBody();
        assertTrue(value.isPresent());
        assertEquals(text, value.get());
    }

    @Test
    public void pojoTransport() throws IOException {
        String name = "hello";
        PoJo pojo = new PoJo();
        pojo.setName(name);
        EventEnvelope event1 = new EventEnvelope();
        event1.setBody(pojo);
        byte[] b = event1.toBytes();
        EventEnvelope event2 = new EventEnvelope();
        event2.load(b);
        // pojo is transported as a Map
        PoJo o = SimpleMapper.getInstance().getMapper().readValue(event2.getBody(), PoJo.class);
        assertEquals(name, o.getName());
    }

    @Test
    public void pojoInEvent() throws IOException {
        int len1 = pojoInEventUsingMsgPack();
        int len2 = pojoInEventUsingGson();
        // transport size is larger when using JSON
        assertTrue(len2 > len1);
    }

    private int pojoInEventUsingMsgPack() throws IOException {
        PoJo input = new PoJo();
        input.setName("hello world");
        input.setNumber(12345);
        EventEnvelope event1 = new EventEnvelope();
        event1.setBody(input);
        byte[] b = event1.toBytes();
        EventEnvelope event2 = new EventEnvelope();
        event2.load(b);
        assertTrue(event2.isBinary());
        PoJo o = SimpleMapper.getInstance().getMapper().readValue(event2.getBody(), PoJo.class);
        assertEquals(input.getName(), o.getName());
        assertEquals(input.getNumber(), o.getNumber());
        return b.length;
    }

    private int pojoInEventUsingGson() throws IOException {
        PoJo input = new PoJo();
        input.setName("hello world");
        input.setNumber(12345);
        EventEnvelope event1 = new EventEnvelope();
        event1.setBody(input);
        event1.setBinary(false);
        byte[] b = event1.toBytes();
        EventEnvelope event2 = new EventEnvelope();
        event2.load(b);
        assertFalse(event2.isBinary());
        PoJo o = SimpleMapper.getInstance().getMapper().readValue(event2.getBody(), PoJo.class);
        assertEquals(input.getName(), o.getName());
        assertEquals(input.getNumber(), o.getNumber());
        return b.length;
    }

    @Test
    public void convertPoJoUsingMsgPack() throws ClassNotFoundException {
        PoJo input = new PoJo();
        input.setName("hello world");
        input.setNumber(12345);
        TypedPayload typed = converter.encode(input, true);
        assertEquals(input.getClass().getName(), typed.getType());
        assertTrue(typed.getPayload() instanceof Map);
        Object converted = converter.decode(typed);
        assertTrue(converted instanceof Map);
        PoJo o = SimpleMapper.getInstance().getMapper().readValue(converted, PoJo.class);
        assertEquals(input.getName(), o.getName());
        assertEquals(input.getNumber(), o.getNumber());
    }

    @Test
    public void convertPoJoUsingJson() throws ClassNotFoundException {
        PoJo input = new PoJo();
        input.setName("hello world");
        input.setNumber(12345);
        TypedPayload typed = converter.encode(input, false);
        assertEquals(input.getClass().getName(), typed.getType());
        assertTrue(typed.getPayload() instanceof byte[]);
        Object converted = converter.decode(typed);
        assertTrue(converted instanceof byte[]);
        PoJo o = SimpleMapper.getInstance().getMapper().readValue(converted, PoJo.class);
        assertEquals(input.getName(), o.getName());
        assertEquals(input.getNumber(), o.getNumber());
    }

    @Test
    public void datePayloadTest() throws IOException {
        Utility util = Utility.getInstance();
        Date now = new Date();
        EventEnvelope event = new EventEnvelope();
        event.setBody(now);
        Object o = event.getBody();
        // date object is serialized as ISO-8601 timestamp when the setBody method is called
        assertEquals(util.date2str(now), o);
        byte[] b = event.toBytes();
        EventEnvelope restored = new EventEnvelope(b);
        assertEquals(util.date2str(now), restored.getBody());
    }

    @Test
    public void convertMap() throws ClassNotFoundException {
        Map<String, Object> input = new HashMap<>();
        input.put("hello", "world");
        TypedPayload typed = converter.encode(input, true);
        assertEquals(PayloadMapper.MAP, typed.getType());
        assertEquals(input, typed.getPayload());
        Object converted = converter.decode(typed);
        assertEquals(input, converted);
    }

    @Test
    public void convertString() throws ClassNotFoundException {
        String input = "hello world";
        TypedPayload typed = converter.encode(input, true);
        assertEquals(PayloadMapper.PRIMITIVE, typed.getType());
        assertEquals(input, typed.getPayload());
        Object converted = converter.decode(typed);
        assertEquals(input, converted);
    }

    @Test
    public void convertBytes() throws ClassNotFoundException {
        byte[] input = "hello world".getBytes();
        TypedPayload typed = converter.encode(input, true);
        assertEquals(PayloadMapper.PRIMITIVE, typed.getType());
        assertEquals(input, typed.getPayload());
        Object converted = converter.decode(typed);
        assertEquals(input, converted);
    }

    @Test
    public void convertNull() throws ClassNotFoundException {
        TypedPayload typed = converter.encode(null, true);
        assertEquals(PayloadMapper.NOTHING, typed.getType());
        assertNull(typed.getPayload());
        Object converted = converter.decode(typed);
        assertNull(converted);
    }

    @Test
    public void convertBoolean() throws ClassNotFoundException {
        TypedPayload typed = converter.encode(true, true);
        assertEquals(PayloadMapper.PRIMITIVE, typed.getType());
        assertEquals(true, typed.getPayload());
        Object converted = converter.decode(typed);
        assertEquals(true, converted);
    }

    @Test
    public void convertInteger() throws ClassNotFoundException {
        Integer input = 12345;
        TypedPayload typed = converter.encode(input, true);
        assertEquals(PayloadMapper.PRIMITIVE, typed.getType());
        assertEquals(input, typed.getPayload());
        Object converted = converter.decode(typed);
        assertEquals(input, converted);
    }

    @Test
    public void convertLong() throws ClassNotFoundException {
        Long input = 123456L;
        TypedPayload typed = converter.encode(input, true);
        assertEquals(PayloadMapper.PRIMITIVE, typed.getType());
        assertEquals(input, typed.getPayload());
        Object converted = converter.decode(typed);
        assertEquals(input, converted);
    }

    @Test
    public void convertFloat() throws ClassNotFoundException {
        Float input = 12.34f;
        TypedPayload typed = converter.encode(input, true);
        assertEquals(PayloadMapper.PRIMITIVE, typed.getType());
        assertEquals(input, typed.getPayload());
        Object converted = converter.decode(typed);
        assertEquals(input, converted);
    }

    @Test
    public void convertDouble() throws ClassNotFoundException {
        Double input = 12.34d;
        TypedPayload typed = converter.encode(input, true);
        assertEquals(PayloadMapper.PRIMITIVE, typed.getType());
        assertEquals(input, typed.getPayload());
        Object converted = converter.decode(typed);
        assertEquals(input, converted);
    }

    @Test
    public void convertDate() throws ClassNotFoundException {
        Utility util = Utility.getInstance();
        Date input = new Date();
        TypedPayload typed = converter.encode(input, true);
        assertEquals(PayloadMapper.PRIMITIVE, typed.getType());
        assertEquals(util.date2str(input), typed.getPayload());
        Object converted = converter.decode(typed);
        assertEquals(util.date2str(input), converted);
    }

    @Test
    public void convertList() throws ClassNotFoundException {
        List<String> input = new ArrayList<>();
        input.add("hello");
        input.add("world");
        TypedPayload typed = converter.encode(input, true);
        assertEquals(PayloadMapper.LIST, typed.getType());
        assertEquals(input, typed.getPayload());
        Object converted = converter.decode(typed);
        assertEquals(input, converted);
    }

    @Test
    public void convertArray() throws ClassNotFoundException {
        String[] input = {"hello", "world"};
        TypedPayload typed = converter.encode(input, true);
        assertEquals(PayloadMapper.LIST, typed.getType());
        Object converted = converter.decode(typed);
        assertEquals(Arrays.asList(input), converted);
    }

}
