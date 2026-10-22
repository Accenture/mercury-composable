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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.ProcessStatus;

import static org.junit.jupiter.api.Assertions.*;

class ProcessStatusTest {

    @Test
    void theExecutionTimeNeverClearsAnException() {
        ProcessStatus ps = new ProcessStatus().setException(400, "demo failure").setExecutionTime(1.5f);

        assertFalse(ps.isSuccess());
        assertEquals(400, ps.getStatus());
        assertEquals("demo failure", ps.getException());
        assertEquals(1.5f, ps.getExecutionTime());
    }

    @Test
    void aStatusWithoutAnExceptionIsASuccess() {
        ProcessStatus ps = new ProcessStatus().setExecutionTime(2.0f);

        assertTrue(ps.isSuccess());
        assertEquals(200, ps.getStatus());
        assertNull(ps.getException());
    }
}
