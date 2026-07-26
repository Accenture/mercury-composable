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

package com.accenture.minigraph.features;

import com.accenture.minigraph.start.PlaygroundLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Registration-metadata conformance for the FEATURE kind: the golden vectors in
 * registration-vectors/feature.json are shared verbatim with every engine repository. Feature
 * names are explicit on the carrier and features honor optional-service gating - the gated-out
 * fixture must be absent from the registry. See docs/guides/registration-metadata-contract.md.
 */
class RegistrationVectorsTest {

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
    }

    @SuppressWarnings("unchecked")
    @Test
    void featureKindMatchesGoldenVectors() {
        Utility util = Utility.getInstance();
        InputStream in = this.getClass().getResourceAsStream("/registration-vectors/feature.json");
        assertNotNull(in, "golden vectors file must exist");
        Map<String, Object> vectors = SimpleMapper.getInstance().getMapper()
                .readValue(util.stream2str(in), Map.class);
        MultiLevelMap map = new MultiLevelMap(vectors);
        List<Map<String, Object>> entries = (List<Map<String, Object>>) map.getElement("entries");
        assertEquals(1, entries.size(), "vector entry count");
        for (Map<String, Object> expected : entries) {
            String name = (String) expected.get("name");
            assertNotNull(PlaygroundLoader.getFeature(name), name + " must be registered");
        }
        // the fixture's declared condition matches the vectors
        OptionalService condition = VectorFeature.class.getAnnotation(OptionalService.class);
        assertNotNull(condition);
        assertEquals(entries.getFirst().get("optionalService"), condition.value());
        // gated-out fixtures must never register
        List<String> gatedOut = (List<String>) map.getElement("gatedOut");
        for (String name : gatedOut) {
            assertNull(PlaygroundLoader.getFeature(name),
                    name + " carries a false optional-service condition and must not register");
        }
    }
}
