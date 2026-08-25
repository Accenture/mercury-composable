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

package com.accenture.services.plugins.types;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.AppConfigReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SetConfigParameterTest {

    private final SetConfigParameter plugin = new SetConfigParameter();

    @Test
    void shouldUseSetConfigAsPluginName() {
        assertEquals("setConfig", plugin.getName());
    }

    @Test
    void shouldSetConfigParameterAsSystemProperty() {
        assertNull(System.getProperty("set.config.plugin.direct"));
        assertEquals(true, plugin.calculate("set.config.plugin.direct", "hello"));
        assertEquals("hello", System.getProperty("set.config.plugin.direct"));
        // a system property overrides base configuration at run-time
        assertEquals("hello", AppConfigReader.getInstance().getProperty("set.config.plugin.direct"));
        // setting the same parameter again overrides the previous value
        assertEquals(true, plugin.calculate("set.config.plugin.direct", "world"));
        assertEquals("world", System.getProperty("set.config.plugin.direct"));
        // a non-string value is converted to text with String.valueOf(value)
        assertEquals(true, plugin.calculate("set.config.plugin.number", 8088));
        assertEquals("8088", System.getProperty("set.config.plugin.number"));
        assertEquals(true, plugin.calculate("set.config.plugin.flag", true));
        assertEquals("true", System.getProperty("set.config.plugin.flag"));
    }

    @Test
    void shouldReturnFalseForInvalidInputWithoutSideEffect() {
        assertEquals(false, plugin.calculate());
        assertEquals(false, plugin.calculate("key.only"));
        assertEquals(false, plugin.calculate("too", "many", "arguments"));
        assertEquals(false, plugin.calculate(100, "non-string-key"));
        assertEquals(false, plugin.calculate(null, "value"));
        assertEquals(false, plugin.calculate("", "empty-key"));
        assertEquals(false, plugin.calculate("   ", "blank-key"));
        assertEquals(false, plugin.calculate("set.config.plugin.null.value", (Object) null));
        assertNull(System.getProperty("set.config.plugin.null.value"));
    }
}
