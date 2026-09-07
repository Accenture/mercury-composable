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
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.ConfigReader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Claims-fixture pin for claim id "config-yml-wins":
 * when both application.properties and application.yml define the same key,
 * the application.yml value wins because app-config-reader.yml lists
 * application.yml last and AppConfigReader.mergeConfig merges each file's
 * flat map with putAll (last file wins).
 * <p>
 * The conflicting fixture key "claim.merge.winner" is deliberately defined in BOTH
 * test resource files with different values.
 */
class ClaimConfigYmlWinsTest {

    private static final String KEY = "claim.merge.winner";

    @SuppressWarnings("unchecked")
    @Test
    void baseConfigListsApplicationYmlAfterApplicationProperties() {
        // the shipped merge manifest (main resources) defines the merge order
        ConfigReader manifest = new ConfigReader("classpath:/app-config-reader.yml");
        Object resources = manifest.get("resources");
        assertInstanceOf(List.class, resources, "app-config-reader.yml must have a resources list");
        List<String> files = (List<String>) resources;
        int propertiesIndex = files.indexOf("classpath:/application.properties");
        int ymlIndex = files.indexOf("classpath:/application.yml");
        assertTrue(propertiesIndex >= 0, "application.properties must be a base configuration file");
        assertTrue(ymlIndex >= 0, "application.yml must be a base configuration file");
        assertTrue(ymlIndex > propertiesIndex,
                "application.yml must merge after application.properties so its values win");
    }

    @Test
    void ymlValueWinsOverPropertiesValueForTheSameKey() {
        // fixture guards: each file, loaded alone, must still carry its side of the conflict
        // (otherwise the merged assertion below would be vacuous)
        ConfigReader propertiesOnly = new ConfigReader().load("classpath:/application.properties", false);
        assertEquals("properties-value", propertiesOnly.getProperty(KEY),
                "fixture: application.properties must define " + KEY);
        ConfigReader ymlOnly = new ConfigReader().load("classpath:/application.yml", false);
        assertEquals("yml-value", ymlOnly.getProperty(KEY),
                "fixture: application.yml must define " + KEY);
        // the pin: the merged base configuration resolves the conflict in favor of application.yml
        AppConfigReader merged = AppConfigReader.getInstance();
        assertEquals("yml-value", merged.getProperty(KEY),
                "application.yml merges last, so its value must win over application.properties");
    }
}
