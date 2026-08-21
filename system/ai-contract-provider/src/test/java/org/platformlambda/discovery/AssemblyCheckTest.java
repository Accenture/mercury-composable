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

package org.platformlambda.discovery;

import org.junit.jupiter.api.Test;
import org.platformlambda.discovery.services.SkillSnapshot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The mercury_version served by this app is read from the platform-core dependency's own
 * pom.properties (no compile-time constant to bump at release), and startup refuses a
 * mixed assembly where the framework modules disagree.
 */
class AssemblyCheckTest {

    @Test
    void servedVersionComesFromThePlatformCoreDependency() {
        assertEquals(System.getProperty("mercury.version.under.test"),
                SkillSnapshot.getInstance().getMercuryVersion());
        assertEquals(SkillSnapshot.getInstance().getMercuryVersion(),
                SkillSnapshot.getInstance().getEventScriptVersion());
    }

    @Test
    void mixedAssemblyIsRefused() {
        assertDoesNotThrow(() -> AiContractProvider.assertConsistentAssembly("4.11.9", "4.11.9"));
        var e = assertThrows(IllegalStateException.class,
                () -> AiContractProvider.assertConsistentAssembly("4.11.9", "4.11.8"));
        assertEquals("Mixed Mercury assembly - platform-core 4.11.9 but event-script-engine 4.11.8",
                e.getMessage());
    }
}
