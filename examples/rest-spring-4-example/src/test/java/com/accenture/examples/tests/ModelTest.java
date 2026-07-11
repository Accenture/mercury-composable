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

package com.accenture.examples.tests;

import org.junit.jupiter.api.Test;
import org.platformlambda.models.ObjectWithGenericType;
import org.platformlambda.models.SamplePoJo;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelTest {

    @Test
    void samplePoJoRoundTrip() {
        Date now = new Date();
        SamplePoJo pojo = new SamplePoJo(1, "Peter Parker", "20 Ingram Street, Queens");
        pojo.setDate(now);
        pojo.setInstance(3);
        pojo.setSeq(7);
        pojo.setOrigin("unit-test");
        assertEquals(1, pojo.getId());
        assertEquals("Peter Parker", pojo.getName());
        assertEquals("20 Ingram Street, Queens", pojo.getAddress());
        assertEquals(now, pojo.getDate());
        assertEquals(3, pojo.getInstance());
        assertEquals(7, pojo.getSeq());
        assertEquals("unit-test", pojo.getOrigin());
        SamplePoJo empty = new SamplePoJo();
        empty.setId(2);
        empty.setName("May Parker");
        empty.setAddress("same street");
        assertEquals(2, empty.getId());
        assertEquals("May Parker", empty.getName());
        assertEquals("same street", empty.getAddress());
    }

    @Test
    void genericHolderRoundTrip() {
        ObjectWithGenericType<SamplePoJo> holder = new ObjectWithGenericType<>();
        SamplePoJo pojo = new SamplePoJo(100, "name", "address");
        holder.setContent(pojo);
        holder.setId(100);
        assertEquals(100, holder.getId());
        assertEquals(pojo, holder.getContent());
    }
}
