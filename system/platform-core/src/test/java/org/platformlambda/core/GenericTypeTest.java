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
import org.platformlambda.core.models.*;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GenericTypeTest {

    @Test
    void testListOfPoJo() {
        int number1 = 100;
        String name1 = "hello world";
        int number2 = 200;
        String name2 = "it is a nice day";
        PoJo pojo1 = new PoJo();
        pojo1.setNumber(number1);
        pojo1.setName(name1);
        PoJo pojo2 = new PoJo();
        pojo2.setNumber(number2);
        pojo2.setName(name2);
        List<PoJo> list = new ArrayList<>();
        list.add(pojo1);
        list.add(null);
        list.add(pojo2);
        EventEnvelope event = new EventEnvelope();
        event.setBody(list);
        byte[] b = event.toBytes();
        EventEnvelope result = new EventEnvelope();
        result.load(b);
        List<PoJo> pojoList = result.getBodyAsListOfPoJo(PoJo.class);
        assertEquals(3, pojoList.size());
        PoJo restored1 = pojoList.getFirst();
        assertEquals(name1, restored1.getName());
        assertEquals(number1, restored1.getNumber());
        assertNull(pojoList.get(1));
        PoJo restored2 = pojoList.get(2);
        assertEquals(name2, restored2.getName());
        assertEquals(number2, restored2.getNumber());
    }

    @Test
    void testArrayOfPoJo() {
        int number1 = 100;
        String name1 = "hello world";
        int number2 = 200;
        String name2 = "it is a nice day";
        PoJo pojo1 = new PoJo();
        pojo1.setNumber(number1);
        pojo1.setName(name1);
        PoJo pojo2 = new PoJo();
        pojo2.setNumber(number2);
        pojo2.setName(name2);
        PoJo[] array = new PoJo[3];
        array[0] = pojo1;
        array[1] = null;
        array[2] = pojo2;
        EventEnvelope event = new EventEnvelope();
        event.setBody(array);
        byte[] b = event.toBytes();
        EventEnvelope result = new EventEnvelope();
        result.load(b);
        assertInstanceOf(List.class, result.getBody());
        List<PoJo> pojoList = result.getBodyAsListOfPoJo(PoJo.class);
        assertEquals(3, pojoList.size());
        PoJo restored1 = pojoList.getFirst();
        assertEquals(name1, restored1.getName());
        assertEquals(number1, restored1.getNumber());
        assertNull(pojoList.get(1));
        PoJo restored2 = pojoList.get(2);
        assertEquals(name2, restored2.getName());
        assertEquals(number2, restored2.getNumber());
    }

    @Test
    void checkMixedTypes() {
        int number1 = 100;
        String name1 = "hello world";
        int number2 = 200;
        String name2 = "it is a nice day";
        PoJo pojo1 = new PoJo();
        pojo1.setNumber(number1);
        pojo1.setName(name1);
        PoJo pojo2 = new PoJo();
        pojo2.setNumber(number2);
        pojo2.setName(name2);
        List<Object> list = new ArrayList<>();
        list.add(pojo1);
        list.add(2);
        list.add(pojo2);
        EventEnvelope event = new EventEnvelope().setBody(list).setType(PoJo.class.getName());
        EventEnvelope restored = new EventEnvelope(event.toBytes());
        assertInstanceOf(List.class, restored.getBody());
        List<PoJo> pojoList = restored.getBodyAsListOfPoJo(PoJo.class);
        assertEquals(PoJo.class, pojoList.getFirst().getClass());
        // non-PoJo is converted to null
        assertNull(pojoList.get(1));
        assertEquals(PoJo.class, pojoList.get(2).getClass());
    }

    @Test
    void acceptListOfPrimitives() {
        List<Object> list = new ArrayList<>();
        list.add(true);
        list.add(null);
        list.add(2);
        EventEnvelope event = new EventEnvelope();
        event.setBody(list);
        byte[] b = event.toBytes();
        EventEnvelope result = new EventEnvelope();
        result.load(b);
        assertInstanceOf(List.class, result.getBody());
        assertEquals(list, result.getBody());
    }

    @Test
    void acceptArrayOfPrimitives() {
        Object[] array = new Object[3];
        array[0] = true;
        array[1] = null;
        array[2] = 2;
        EventEnvelope event = new EventEnvelope();
        event.setBody(array);
        byte[] b = event.toBytes();
        EventEnvelope result = new EventEnvelope();
        result.load(b);
        assertInstanceOf(List.class, result.getBody());
        assertEquals(Arrays.asList(array), result.getBody());
    }

    @Test
    void testEmptyList() {
        EventEnvelope event = new EventEnvelope();
        event.setBody(Collections.emptyList());
        byte[] b = event.toBytes();
        EventEnvelope result = new EventEnvelope();
        result.load(b);
        assertEquals(Collections.EMPTY_LIST, result.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    void restoreObjectWithParametric() {
        Utility util = Utility.getInstance();
        int id = 123;
        String name = "hello world";
        ObjectWithGenericType<PoJo> genericObject = new ObjectWithGenericType<>();
        PoJo pojo = new PoJo();
        pojo.setNumber(id);
        pojo.setName(name);
        genericObject.setContent(pojo);
        genericObject.setId(id);
        EventEnvelope event = new EventEnvelope();
        event.setBody(genericObject);
        byte[] b = event.toBytes();
        EventEnvelope result = new EventEnvelope();
        result.load(b);
        ObjectWithGenericType<PoJo> o = result.getBody(ObjectWithGenericType.class, PoJo.class);
        assertEquals(ObjectWithGenericType.class, o.getClass());
        Map<String, Object> restored = SimpleMapper.getInstance().getMapper().readValue(o, Map.class);
        MultiLevelMap map = new MultiLevelMap(restored);
        assertEquals(name, map.getElement("content.name"));
        // numbers are encoded as string in map
        assertEquals(id, util.str2int(map.getElement("id").toString()));
        assertEquals(id, util.str2int(map.getElement("content.number").toString()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void parametricHttpObjectTest() {
        int id = 100;
        String name = "hello world";
        ObjectWithGenericType<PoJo> genericObject = new ObjectWithGenericType<>();
        PoJo pojo = new PoJo();
        pojo.setName(name);
        pojo.setNumber(id);
        genericObject.setContent(pojo);
        genericObject.setId(id);
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setBody(genericObject);
        AsyncHttpRequest restored = new AsyncHttpRequest(request.toMap());
        ObjectWithGenericType<PoJo> o = restored.getBody(ObjectWithGenericType.class, PoJo.class);
        assertEquals(name, o.getContent().getName());
        assertEquals(id, o.getContent().getNumber());
        assertEquals(id, o.getId());
    }

    @SuppressWarnings("unchecked")
    @Test
    void parametricEnvelopeTest() {
        int id = 100;
        String name = "hello world";
        ObjectWithGenericType<PoJo> genericObject = new ObjectWithGenericType<>();
        PoJo pojo = new PoJo();
        pojo.setName(name);
        pojo.setNumber(id);
        genericObject.setContent(pojo);
        genericObject.setId(id);
        EventEnvelope event = new EventEnvelope();
        event.setBody(genericObject);
        byte[] b = event.toBytes();
        EventEnvelope restored = new EventEnvelope(b);
        ObjectWithGenericTypeVariance<PoJoVariance> o =
                restored.getBody(ObjectWithGenericTypeVariance.class, PoJoVariance.class);
        assertEquals(name, o.getContent().getName());
        assertEquals(id, o.getContent().getNumber());
        assertEquals(id, o.getId());
        assertInstanceOf(Map.class, restored.getRawBody());
    }

    @Test
    void remappingEnvelopeTest() {
        int id = 100;
        String name = "hello world";
        PoJo pojo = new PoJo();
        pojo.setName(name);
        pojo.setNumber(id);
        EventEnvelope event = new EventEnvelope();
        event.setBody(pojo);
        byte[] b = event.toBytes();
        EventEnvelope restored = new EventEnvelope(b);
        PoJoVariance o = restored.getBody(PoJoVariance.class);
        assertEquals(name, o.getName());
        assertEquals(id, o.getNumber());
        assertInstanceOf(Map.class, restored.getRawBody());
    }

    @Test
    void primitiveObjectTest() {
        String message = "Unable to convert a primitive into class " + PoJoVariance.class.getName();
        int id = 100;
        EventEnvelope event = new EventEnvelope();
        event.setBody(id);
        byte[] b = event.toBytes();
        EventEnvelope restored = new EventEnvelope(b);
        assertEquals(100, restored.getBody());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                                () -> restored.getBody(PoJoVariance.class));
        assertEquals(message, ex.getMessage());
    }
}
