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

package org.platformlambda.demo;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.demo.common.TestBase;
import org.platformlambda.models.SamplePoJo;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PoJoTest extends TestBase {

    @Test
    void pojoRpcTest() throws InterruptedException {
        Integer id = 1;
        String name = "Simple PoJo class";
        String address = "100 World Blvd, Planet Earth";
        BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        PostOffice po = new PostOffice("unit.test", "20001", "GET /api/hello/pojo");
        EventEnvelope request = new EventEnvelope().setTo("hello.pojo").setHeader("id", "1");
        po.asyncRequest(request, 8000).onSuccess(bench::add);
        EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
        assert response != null;
        assertEquals(HashMap.class, response.getBody().getClass());
        SamplePoJo pojo = response.getBody(SamplePoJo.class);
        assertEquals(id, pojo.getId());
        assertEquals(name, pojo.getName());
        assertEquals(address, pojo.getAddress());
    }
}
