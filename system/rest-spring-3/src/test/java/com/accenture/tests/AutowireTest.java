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

package com.accenture.tests;

import com.accenture.common.TestBase;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.PostOffice;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class AutowireTest extends TestBase {
    @Test
    void testAutoWirePreloadedFunction() throws IOException, InterruptedException, ExecutionException {
        var po = new PostOffice("unit.test", "12345", "TEST v1.autowire.test");
        var request = new EventEnvelope().setTo("v1.autowire.test").setBody(Map.of());
        var response = po.request(request, RPC_TIMEOUT).get();
        assert response != null;
        assertInstanceOf(Map.class, response.getBody());
        assertEquals(Map.of("success", true), response.getBody());
    }
}
