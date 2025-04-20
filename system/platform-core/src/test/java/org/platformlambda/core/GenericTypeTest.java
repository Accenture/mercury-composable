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

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GenericTypeTest {

    @Test
    void testListOfPoJo() throws IOException {
        int NUMBER_1 = 100;
        String NAME_1 = "hello world";
        int NUMBER_2 = 200;
        String NAME_2 = "it is a nice day";
        PoJo pojo1 = new PoJo();
        pojo1.setNumber(NUMBER_1);
        pojo1.setName(NAME_1);
        PoJo pojo2 = new PoJo();
        pojo2.setNumber(NUMBER_2);
        pojo2.setName(NAME_2);
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
        assertEquals(NAME_1, restored1.getName());
        assertEquals(NUMBER_1, restored1.getNumber());
        assertNull(pojoList.get(1));
        PoJo restored2 = pojoList.get(2);
        assertEquals(NAME_2, restored2.getName());
        assertEquals(NUMBER_2, restored2.getNumber());
    }

    @Test
    void testArrayOfPoJo() throws IOException {
        int NUMBER_1 = 100;
        String NAME_1 = "hello world";
        int NUMBER_2 = 200;
        String NAME_2 = "it is a nice day";
        PoJo pojo1 = new PoJo();
        pojo1.setNumber(NUMBER_1);
        pojo1.setName(NAME_1);
        PoJo pojo2 = new PoJo();
        pojo2.setNumber(NUMBER_2);
        pojo2.setName(NAME_2);
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
        assertEquals(NAME_1, restored1.getName());
        assertEquals(NUMBER_1, restored1.getNumber());
        assertNull(pojoList.get(1));
        PoJo restored2 = pojoList.get(2);
        assertEquals(NAME_2, restored2.getName());
        assertEquals(NUMBER_2, restored2.getNumber());
    }

    @Test
    void checkMixedTypes() {
        int NUMBER_1 = 100;
        String NAME_1 = "hello world";
        int NUMBER_2 = 200;
        String NAME_2 = "it is a nice day";
        PoJo pojo1 = new PoJo();
        pojo1.setNumber(NUMBER_1);
        pojo1.setName(NAME_1);
        PoJo pojo2 = new PoJo();
        pojo2.setNumber(NUMBER_2);
        pojo2.setName(NAME_2);
        List<Object> list = new ArrayList<>();
        list.add(pojo1);
        list.add(2);
        list.add(pojo2);
        EventEnvelope event = new EventEnvelope();
        event.setBody(list);
        List<PoJo> pojoList = event.getBodyAsListOfPoJo(PoJo.class);
        assertEquals(PoJo.class, pojoList.getFirst().getClass());
        // non-PoJo is converted to null
        assertNull(pojoList.get(1));
        assertEquals(PoJo.class, pojoList.get(2).getClass());
    }

    @Test
    void acceptListOfPrimitives() throws IOException {
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
    void acceptArrayOfPrimitives() throws IOException {
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
    void testEmptyList() throws IOException {
        EventEnvelope event = new EventEnvelope();
        event.setBody(Collections.emptyList());
        byte[] b = event.toBytes();
        EventEnvelope result = new EventEnvelope();
        result.load(b);
        assertEquals(Collections.EMPTY_LIST, result.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    void restoreObjectWithParametric() throws IOException {
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
        assertEquals(o.getClass(), ObjectWithGenericType.class);
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
        int ID = 100;
        String NAME = "hello world";
        ObjectWithGenericType<PoJo> genericObject = new ObjectWithGenericType<>();
        PoJo pojo = new PoJo();
        pojo.setName(NAME);
        pojo.setNumber(ID);
        genericObject.setContent(pojo);
        genericObject.setId(ID);
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setBody(genericObject);
        AsyncHttpRequest restored = new AsyncHttpRequest(request.toMap());
        ObjectWithGenericType<PoJo> o = restored.getBody(ObjectWithGenericType.class, PoJo.class);
        assertEquals(NAME, o.getContent().getName());
        assertEquals(ID, o.getContent().getNumber());
        assertEquals(ID, o.getId());
    }

    @SuppressWarnings("unchecked")
    @Test
    void parametricEnvelopeTest() throws IOException {
        int ID = 100;
        String NAME = "hello world";
        ObjectWithGenericType<PoJo> genericObject = new ObjectWithGenericType<>();
        PoJo pojo = new PoJo();
        pojo.setName(NAME);
        pojo.setNumber(ID);
        genericObject.setContent(pojo);
        genericObject.setId(ID);
        EventEnvelope event = new EventEnvelope();
        event.setBody(genericObject);
        byte[] b = event.toBytes();
        EventEnvelope restored = new EventEnvelope(b);
        ObjectWithGenericTypeVariance<PoJoVariance> o =
                restored.getBody(ObjectWithGenericTypeVariance.class, PoJoVariance.class);
        assertEquals(NAME, o.getContent().getName());
        assertEquals(ID, o.getContent().getNumber());
        assertEquals(ID, o.getId());
        assertInstanceOf(Map.class, restored.getRawBody());
    }

    @Test
    void remappingEnvelopeTest() throws IOException {
        int ID = 100;
        String NAME = "hello world";
        PoJo pojo = new PoJo();
        pojo.setName(NAME);
        pojo.setNumber(ID);
        EventEnvelope event = new EventEnvelope();
        event.setBody(pojo);
        byte[] b = event.toBytes();
        EventEnvelope restored = new EventEnvelope(b);
        PoJoVariance o = restored.getBody(PoJoVariance.class);
        assertEquals(NAME, o.getName());
        assertEquals(ID, o.getNumber());
        assertInstanceOf(Map.class, restored.getRawBody());
    }

    @Test
    void primitiveObjectTest() throws IOException {
        String MESSAGE = "Unable to convert a primitive into class " + PoJoVariance.class.getName();
        int id = 100;
        EventEnvelope event = new EventEnvelope();
        event.setBody(id);
        byte[] b = event.toBytes();
        EventEnvelope restored = new EventEnvelope(b);
        assertEquals(100, restored.getBody());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                                () -> restored.getBody(PoJoVariance.class));
        assertEquals(MESSAGE, ex.getMessage());
    }

}
