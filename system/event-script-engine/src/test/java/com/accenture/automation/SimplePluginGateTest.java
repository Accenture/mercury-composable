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

package com.accenture.automation;

import com.accenture.setup.TestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The allowlist gate scans the FULL plugin class - method bodies included - so
 * a plugin whose signature is clean cannot reach outside the allowlist from
 * inside calculate(). The fixtures live in com.accenture.services.plugins.gate
 * and are scanned by the real loader when the test application starts.
 */
class SimplePluginGateTest extends TestBase {

    @Test
    void gateBlocksBodyOnlyViolations() {
        // java.io reached directly from the method body
        assertFalse(SimplePluginLoader.containsSimplePlugin("disallowedIo"),
                "a plugin touching java.io in its body must not register");
        // java.net hidden inside a never-invoked lambda
        assertFalse(SimplePluginLoader.containsSimplePlugin("disallowedLambda"),
                "a disallowed reference inside a lambda body must not register");
    }

    @Test
    void gateKeepsLegitimatePlugins() {
        // nested classes and lambdas within the allowlist are analyzed, not flagged
        assertTrue(SimplePluginLoader.containsSimplePlugin("gateInnerOk"));
        // built-ins whose BODIES use the allowlisted engine helpers
        assertTrue(SimplePluginLoader.containsSimplePlugin("camelCase"));
        assertTrue(SimplePluginLoader.containsSimplePlugin("snakeCase"));
        // the whole built-in inventory passes through the gate (regression net)
        assertTrue(SimplePluginLoader.getLoadedSimplePlugins().size() >= 50,
                "built-in plugins must all pass the gate");
    }
}
