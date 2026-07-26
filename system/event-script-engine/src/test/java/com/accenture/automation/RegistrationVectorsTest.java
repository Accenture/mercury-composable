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
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registration-metadata conformance for the PLUGIN kind: the golden vectors in
 * registration-vectors/plugin.json are shared verbatim with every engine repository. The name
 * rules are the contract - VectorEcho registers under its EXPLICIT name and VectorDerived under
 * the name DERIVED from its declaration, and idiomatic declarations in every language must yield
 * the same registered names. See docs/guides/registration-metadata-contract.md.
 */
class RegistrationVectorsTest extends TestBase {

    @SuppressWarnings("unchecked")
    @Test
    void pluginKindMatchesGoldenVectors() {
        Utility util = Utility.getInstance();
        InputStream in = this.getClass().getResourceAsStream("/registration-vectors/plugin.json");
        assertNotNull(in, "golden vectors file must exist");
        Map<String, Object> vectors = SimpleMapper.getInstance().getMapper()
                .readValue(util.stream2str(in), Map.class);
        List<Map<String, Object>> entries =
                (List<Map<String, Object>>) new MultiLevelMap(vectors).getElement("entries");
        assertEquals(2, entries.size(), "vector entry count");
        for (Map<String, Object> expected : entries) {
            String name = (String) expected.get("name");
            assertTrue(SimplePluginLoader.containsSimplePlugin(name),
                    name + " must be registered under exactly this name");
        }
        // the two fixtures pin both halves of the naming rule
        assertEquals("vectorEcho", SimplePluginLoader.getSimplePluginByName("vectorEcho").getName(),
                "explicit name wins");
        assertEquals("vectorDerived", SimplePluginLoader.getSimplePluginByName("vectorDerived").getName(),
                "derived name: Java lowercases the first letter of the class simple name");
    }
}
