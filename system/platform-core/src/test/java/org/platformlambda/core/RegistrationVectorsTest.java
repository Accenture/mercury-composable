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
import org.platformlambda.common.TestBase;
import org.platformlambda.core.annotations.EventInterceptor;
import org.platformlambda.core.annotations.OptionalService;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.annotations.ZeroTracing;
import org.platformlambda.core.mock.VectorAliasFunction;
import org.platformlambda.core.mock.VectorMarkedFunction;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.ServiceDef;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registration-metadata conformance for the FUNCTION kind: the golden vectors in
 * registration-vectors/core.json are shared verbatim with every engine repository. Each engine
 * declares the same fixture set through its own carrier (here: Java annotations on the
 * VectorAliasFunction / VectorMarkedFunction / VectorGatedOutFunction mocks) and must resolve to
 * exactly the golden entries at boot - declared metadata, boot-time env resolution, marker
 * stacking, optional-service gating and privacy all pinned in one place.
 * See docs/guides/registration-metadata-contract.md.
 */
class RegistrationVectorsTest extends TestBase {

    private static final Map<String, Class<?>> FIXTURES = Map.of(
            "vector.alias.one", VectorAliasFunction.class,
            "vector.marked", VectorMarkedFunction.class);

    @SuppressWarnings("unchecked")
    @Test
    void functionKindMatchesGoldenVectors() {
        Utility util = Utility.getInstance();
        InputStream in = this.getClass().getResourceAsStream("/registration-vectors/core.json");
        assertNotNull(in, "golden vectors file must exist");
        Map<String, Object> vectors = SimpleMapper.getInstance().getMapper()
                .readValue(util.stream2str(in), Map.class);
        MultiLevelMap map = new MultiLevelMap(vectors);
        List<Map<String, Object>> entries = (List<Map<String, Object>>) map.getElement("entries");
        assertEquals(FIXTURES.size(), entries.size(), "vector entry count");
        Platform platform = Platform.getInstance();
        for (Map<String, Object> expected : entries) {
            List<String> routes = (List<String>) expected.get("routes");
            String primary = routes.getFirst();
            Class<?> cls = FIXTURES.get(primary);
            assertNotNull(cls, "a fixture class must exist for " + primary);
            PreLoad annotation = cls.getAnnotation(PreLoad.class);
            // declared metadata matches the vectors
            List<String> declaredRoutes = util.split(annotation.route(), ", ").stream().sorted().toList();
            assertEquals(routes, declaredRoutes, primary + ": routes");
            // JSON integers surface as Long through the customized Gson - normalize before comparing
            assertEquals(util.str2int(String.valueOf(expected.get("declaredInstances"))),
                    annotation.instances(), primary + ": declaredInstances");
            assertEquals(expected.get("envInstances"), annotation.envInstances(), primary + ": envInstances");
            assertEquals(expected.get("isPrivate"), annotation.isPrivate(), primary + ": isPrivate");
            OptionalService condition = cls.getAnnotation(OptionalService.class);
            assertEquals(expected.get("optionalService"), condition == null? null : condition.value(),
                    primary + ": optionalService");
            assertEquals(expected.get("zeroTracing"), cls.getAnnotation(ZeroTracing.class) != null,
                    primary + ": zeroTracing");
            assertEquals(expected.get("eventInterceptor"), cls.getAnnotation(EventInterceptor.class) != null,
                    primary + ": eventInterceptor");
            // resolved registration matches the vectors, for every alias
            for (String route : routes) {
                assertTrue(platform.hasRoute(route), route + " must be registered");
                ServiceDef def = platform.getLocalRoutingTable().get(route);
                assertNotNull(def, route + " must be in the routing table");
                assertEquals(util.str2int(String.valueOf(expected.get("resolvedInstances"))),
                        def.getConcurrency(), route + ": resolvedInstances (envInstances resolved at boot)");
                assertEquals(expected.get("isPrivate"), def.isPrivate(), route + ": isPrivate resolved");
                assertEquals(expected.get("zeroTracing"), !def.isTrackable(), route + ": zeroTracing resolved");
                assertEquals(expected.get("eventInterceptor"), def.isInterceptor(),
                        route + ": eventInterceptor resolved");
            }
        }
        // gated-out fixtures must never register
        List<String> gatedOut = (List<String>) map.getElement("gatedOut");
        for (String route : gatedOut) {
            assertFalse(platform.hasRoute(route),
                    route + " carries a false optional-service condition and must not register");
        }
    }
}
