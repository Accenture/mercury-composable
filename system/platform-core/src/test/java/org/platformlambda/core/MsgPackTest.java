/*

    Copyright 2018-2025 Accenture Technology

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

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.PoJo;
import org.platformlambda.core.serializers.MsgPack;
import org.platformlambda.core.serializers.PayloadMapper;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class MsgPackTest {

    private static final MsgPack msgPack = new MsgPack();

    @SuppressWarnings("unchecked")
    @Test
    void dataIsMap() throws IOException {
        PoJo pojo = new PoJo();
        pojo.setName("hello world");
        String[] HELLO_WORLD = {"hello", "world"};
        Map<String, Object> input = new HashMap<>();
        input.put("hello", "world");
        input.put("boolean", true);
        input.put("array", HELLO_WORLD);
        input.put("integer", 12345L);
        input.put("long", 12345L);
        input.put("float", 12.345f);
        input.put("double", 12.345d);
        input.put("pojo", pojo);
        input.put(PayloadMapper.NOTHING, null);
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertInstanceOf(Map.class, o);
        Map<String, Object> result = (Map<String, Object>) o;
        // long number will be compressed into integer if applicable
        assertInstanceOf(Integer.class, result.get("integer"));
        assertInstanceOf(Integer.class, result.get("long"));
        assertInstanceOf(Float.class, result.get("float"));
        assertInstanceOf(Double.class, result.get("double"));
        // MsgPack does not transport null elements in a map
        assertFalse(result.containsKey(PayloadMapper.NOTHING));
        result.remove(PayloadMapper.NOTHING);
        assertEquals(o, result);
        // array is converted to list of objects
        assertEquals(Arrays.asList(HELLO_WORLD), result.get("array"));
        // embedded pojo in a map is converted to a map
        Object innerPoJo = result.get("pojo");
        assertInstanceOf(Map.class, innerPoJo);
        PoJo restored = SimpleMapper.getInstance().getMapper().readValue(innerPoJo, PoJo.class);
        assertEquals(pojo.getName(), restored.getName());
    }

    @Test
    void dataIsAtomicInteger() throws IOException {
        AtomicInteger input = new AtomicInteger(10000);
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input.get(), o);
    }

    @Test
    void dataIsAtomicLong() throws IOException {
        AtomicLong input = new AtomicLong(10000);
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // smaller number will be packed as integer
        assertEquals((int) input.get(), o);
    }

    @Test
    void dataIsSmallInteger() throws IOException {
        int input = 10000;
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input, o);
    }

    @Test
    void smallLongBecomesInteger() throws IOException {
        // msgpack compresses number and data type information will be lost
        Long input = 10L;
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input.intValue(), o);
    }

    @SuppressWarnings("unchecked")
    @Test
    void dataIsBigLong() throws IOException {
        long input = -5106534569952410475L;
        Map<String, Object> map = new HashMap<>();
        map.put("number", input);
        byte[] b = msgPack.pack(map);
        Object o = msgPack.unpack(b);
        assertInstanceOf(Map.class, o);
        Map<String, Object> restored = (Map<String, Object>) o;
        assertEquals(input, restored.get("number"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void dataIsBorderLineLong() throws IOException {
        Long input = Integer.MAX_VALUE + 1L;
        List<Long> value = new ArrayList<>();
        value.add(input);
        byte[] b = msgPack.pack(value);
        Object o = msgPack.unpack(b);
        assertInstanceOf(List.class, o);
        List<Long> restored = (List<Long>) o;
        assertEquals(input, restored.getFirst());
    }

    @Test
    void dataIsBigInteger() throws IOException {
        BigInteger input = new BigInteger("10");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input.toString(), o);
    }

    @Test
    void dataIsBigDecimal() throws IOException {
        BigDecimal input = new BigDecimal("0.0000000000012345");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input.toPlainString(), o);
    }

    @Test
    void dataIsNull() throws IOException {
        byte[] b = msgPack.pack(null);
        Object o = msgPack.unpack(b);
        assertNull(o);
    }

    @Test
    void dataIsBoolean() throws IOException {
        byte[] b = msgPack.pack(true);
        Object o = msgPack.unpack(b);
        assertEquals(true, o);
    }

    @Test
    void dataIsFloat() throws IOException {
        Float input = 3.2f;
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input, o);
    }

    @Test
    void dataIsSmallDouble() throws IOException {
        Double input = 3.2d;
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input, o);
    }

    @Test
    void dataIsDouble() throws IOException {
        Double input = Float.MAX_VALUE + 1.0d;
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(input, o);
    }

    @Test
    void dataIsDate() throws IOException {
        Date input = new Date();
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // date object is serialized as UTC string
        assertEquals(Utility.getInstance().date2str(input), o);
    }

    @Test
    void dataIsList() throws IOException {
        List<String> input = new ArrayList<>();
        input.add("hello");
        input.add("world");
        input.add(null);    // prove that null value in a list can be transported
        input.add("1");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // MsgPack transports null elements in an array list so that absolute sequencing can be preserved
        assertEquals(input, o);
    }

    @Test
    void dataIsArray() throws IOException {
        String[] input = {"hello", "world", null, "1"};
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        assertEquals(Arrays.asList(input), o);
    }

    @Test
    void dataIsShortNumber() throws IOException {
        Short number = 10;
        byte[] b = msgPack.pack(number);
        Object o = msgPack.unpack(b);
        assertEquals((int) number, o);
    }

    @Test
    void dataIsByte() throws IOException {
        byte number = 10;
        byte[] b = msgPack.pack(number);
        Object o = msgPack.unpack(b);
        // a single byte is converted to an integer
        assertEquals((int) number, o);
    }

    @Test
    void dataIsPoJo() throws IOException {
        PoJo input = new PoJo();
        input.setName("testing Integer transport");
        input.setNumber(12345);
        input.setAddress("123 Planet Earth");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // successfully restored to PoJo
        assertInstanceOf(Map.class, o);
        PoJo result = SimpleMapper.getInstance().getMapper().readValue(o, PoJo.class);
        assertEquals(input.getNumber(), result.getNumber());
        assertEquals(input.getName(), result.getName());
        assertEquals(input.getAddress(), result.getAddress());
    }

    @Test
    void dataIsPoJoWithLong() throws IOException {
        PoJo input = new PoJo();
        input.setName("testing Long number transport");
        input.setLongNumber(10L);
        input.setAddress("100 Planet Earth");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // successfully restored to PoJo when the intermediate value becomes an integer
        assertInstanceOf(Map.class, o);
        PoJo result = SimpleMapper.getInstance().getMapper().readValue(o, PoJo.class);
        assertEquals(input.getLongNumber(), result.getLongNumber());
        assertEquals(input.getName(), result.getName());
        assertEquals(input.getAddress(), result.getAddress());
    }

    @Test
    void dataIsPoJoWithBigInteger() throws IOException {
        PoJo input = new PoJo();
        input.setName("testing BigInteger transport");
        input.setBigInteger(new BigInteger("10"));
        input.setAddress("100 Planet Earth");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // successfully restored to PoJo when the intermediate value becomes an integer
        assertInstanceOf(Map.class, o);
        PoJo result = SimpleMapper.getInstance().getMapper().readValue(o, PoJo.class);
        assertEquals(input.getBigInteger(), result.getBigInteger());
        assertEquals(input.getName(), result.getName());
        assertEquals(input.getAddress(), result.getAddress());
    }

    @Test
    void dataIsPoJoWithBigDecimal() throws IOException {
        PoJo input = new PoJo();
        input.setName("testing BigInteger transport");
        input.setBigDecimal(new BigDecimal("0.00000012345"));
        input.setAddress("100 Planet Earth");
        byte[] b = msgPack.pack(input);
        Object o = msgPack.unpack(b);
        // successfully restored to PoJo when the intermediate value becomes an integer
        assertInstanceOf(Map.class, o);
        PoJo result = SimpleMapper.getInstance().getMapper().readValue(o, PoJo.class);
        assertEquals(input.getBigDecimal(), result.getBigDecimal());
        assertEquals(input.getName(), result.getName());
        assertEquals(input.getAddress(), result.getAddress());
    }

}
