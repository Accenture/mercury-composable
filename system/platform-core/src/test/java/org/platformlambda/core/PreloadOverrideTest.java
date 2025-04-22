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
import org.platformlambda.common.TestBase;
import org.platformlambda.core.system.Platform;

import static org.junit.jupiter.api.Assertions.*;

class PreloadOverrideTest extends TestBase {

    @Test
    void validateOverrideResult() {
        // check the route names after the preloading process at start-up
        Platform platform = Platform.getInstance();
        // v1.dummy.one is kept in addition to v1.dummy.one.1 and v1.dummy.one.2
        assertTrue(platform.hasRoute("v1.dummy.one"));
        assertTrue(platform.hasRoute("v1.dummy.one.1"));
        assertTrue(platform.hasRoute("v1.dummy.one.2"));
        // v1.dummy.two is changed to v1.dummy.two.1 and v1.dummy.two.2
        // preload-more.yaml add v1.dummy.two.3 and v1.dummy.two.4
        assertFalse(platform.hasRoute("v1.dummy.two"));
        assertTrue(platform.hasRoute("v1.dummy.two.1"));
        assertTrue(platform.hasRoute("v1.dummy.two.2"));
        assertTrue(platform.hasRoute("v1.dummy.two.3"));
        assertTrue(platform.hasRoute("v1.dummy.two.4"));
        // v1.dummy.one.1's concurrency is changed to 20 by preload-override.yaml
        assertEquals(20, platform.getConcurrency("v1.dummy.one.1"));
        // v1.dummy.two.1's concurrency is changed to 30 by preload-more.yaml
        assertEquals(30, platform.getConcurrency("v1.dummy.two.1"));
    }
}
