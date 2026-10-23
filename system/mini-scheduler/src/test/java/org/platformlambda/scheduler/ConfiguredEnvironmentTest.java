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

package org.platformlambda.scheduler;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.ConfigReader;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Where this instance's environment name comes from, read through the real configuration reader: a key holds
 * a value or child keys, never both, so the name is a child of scheduler.environment like the store.
 */
class ConfiguredEnvironmentTest {

    @Test
    void theNameSitsBesideTheStore() {
        assertEquals("dr", ActiveEnvironment.configuredEnvironment(file("name-and-store")));
    }

    @Test
    void theNameOfEarlierReleasesIsStillRead() {
        assertEquals("dr", ActiveEnvironment.configuredEnvironment(file("legacy-name")));
    }

    @Test
    void aNameLostBesideTheStoreIsRefusedNotDefaulted() {
        Function<String, Object> config = file("legacy-name-and-store");
        var ex = assertThrows(IllegalArgumentException.class, () -> ActiveEnvironment.configuredEnvironment(config));
        assertEquals("Set scheduler.environment.name - scheduler.environment holds other settings, " +
                "so it cannot hold the name as well", ex.getMessage());
    }

    @Test
    void withoutANameTheInstanceIsProduction() {
        assertEquals(ActiveEnvironment.DEFAULT_ENVIRONMENT, ActiveEnvironment.configuredEnvironment(key -> null));
    }

    private static Function<String, Object> file(String name) {
        return new ConfigReader("classpath:/environment/" + name + ".properties")::get;
    }
}
