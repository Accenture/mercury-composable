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

import org.junit.Assert;
import org.junit.Test;
import org.platformlambda.core.models.*;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.io.IOException;
import java.util.*;

public class GenericTypeTest {

    @Test
    public void testListOfPoJo() throws IOException {
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
        List<Object> pojoList = result.getBodyAsListOfPoJo(PoJo.class);
        Assert.assertEquals(3, pojoList.size());
        PoJo restored1 = (PoJo) pojoList.getFirst();
        Assert.assertEquals(NAME_1, restored1.getName());
        Assert.assertEquals(NUMBER_1, restored1.getNumber());
        Assert.assertNull(pojoList.get(1));
        PoJo restored2 = (PoJo) pojoList.get(2);
        Assert.assertEquals(NAME_2, restored2.getName());
        Assert.assertEquals(NUMBER_2, restored2.getNumber());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArrayOfPoJo() throws IOException {
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
        Assert.assertTrue(result.getBody() instanceof List);
        List<Object> pojoList = result.getBodyAsListOfPoJo(PoJo.class);
        Assert.assertEquals(3, pojoList.size());
        PoJo restored1 = (PoJo) pojoList.getFirst();
        Assert.assertEquals(NAME_1, restored1.getName());
        Assert.assertEquals(NUMBER_1, restored1.getNumber());
        Assert.assertNull(pojoList.get(1));
        PoJo restored2 = (PoJo) pojoList.get(2);
        Assert.assertEquals(NAME_2, restored2.getName());
        Assert.assertEquals(NUMBER_2, restored2.getNumber());
    }

    @Test
    public void checkMixedTypes() {
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
        List<Object> pojoList = event.getBodyAsListOfPoJo(PoJo.class);
        Assert.assertEquals(pojoList.getFirst().getClass(), PoJo.class);
        // non-PoJo is converted to null
        Assert.assertNull(pojoList.get(1));
        Assert.assertEquals(pojoList.get(2).getClass(), PoJo.class);
    }

    @Test
    public void acceptListOfPrimitives() throws IOException {
        List<Object> list = new ArrayList<>();
        list.add(true);
        list.add(null);
        list.add(2);
        EventEnvelope event = new EventEnvelope();
        event.setBody(list);
        byte[] b = event.toBytes();
        EventEnvelope result = new EventEnvelope();
        result.load(b);
        Assert.assertTrue(result.getBody() instanceof List);
        Assert.assertEquals(list, result.getBody());
    }

    @Test
    public void acceptArrayOfPrimitives() throws IOException {
        Object[] array = new Object[3];
        array[0] = true;
        array[1] = null;
        array[2] = 2;
        EventEnvelope event = new EventEnvelope();
        event.setBody(array);
        byte[] b = event.toBytes();
        EventEnvelope result = new EventEnvelope();
        result.load(b);
        Assert.assertTrue(result.getBody() instanceof List);
        Assert.assertEquals(Arrays.asList(array), result.getBody());
    }

    @Test
    public void testEmptyList() throws IOException {
        EventEnvelope event = new EventEnvelope();
        event.setBody(Collections.emptyList());
        byte[] b = event.toBytes();
        EventEnvelope result = new EventEnvelope();
        result.load(b);
        Assert.assertEquals(Collections.EMPTY_LIST, result.getBody());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void restoreObjectWithParametric() throws IOException {
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
        Assert.assertEquals(o.getClass(), ObjectWithGenericType.class);
        Map<String, Object> restored = SimpleMapper.getInstance().getMapper().readValue(o, Map.class);
        MultiLevelMap map = new MultiLevelMap(restored);
        Assert.assertEquals(name, map.getElement("content.name"));
        // numbers are encoded as string in map
        Assert.assertEquals(id, util.str2int(map.getElement("id").toString()));
        Assert.assertEquals(id, util.str2int(map.getElement("content.number").toString()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void parametricHttpObjectTest() {
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
        Assert.assertEquals(NAME, o.getContent().getName());
        Assert.assertEquals(ID, o.getContent().getNumber());
        Assert.assertEquals(ID, o.getId());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void parametricEnvelopeTest() throws IOException {
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
        Assert.assertEquals(NAME, o.getContent().getName());
        Assert.assertEquals(ID, o.getContent().getNumber());
        Assert.assertEquals(ID, o.getId());
        Assert.assertTrue(restored.getRawBody() instanceof Map);
    }

    @Test
    public void remappingEnvelopeTest() throws IOException {
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
        Assert.assertEquals(NAME, o.getName());
        Assert.assertEquals(ID, o.getNumber());
        Assert.assertTrue(restored.getRawBody() instanceof Map);
    }

    @Test
    public void primitiveObjectTest() throws IOException {
        String MESSAGE = "Unable to convert a primitive into class " + PoJoVariance.class.getName();
        int id = 100;
        EventEnvelope event = new EventEnvelope();
        event.setBody(id);
        byte[] b = event.toBytes();
        EventEnvelope restored = new EventEnvelope(b);
        Assert.assertEquals(100, restored.getBody());
        IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class,
                                                () -> restored.getBody(PoJoVariance.class));
        Assert.assertEquals(MESSAGE, ex.getMessage());
    }

}
