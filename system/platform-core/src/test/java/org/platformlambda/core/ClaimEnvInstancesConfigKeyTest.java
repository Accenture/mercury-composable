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
import org.platformlambda.core.mock.ClaimEnvInstancesLiteralFunction;
import org.platformlambda.core.services.NoOpFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.ServiceDef;
import org.platformlambda.core.util.AppConfigReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Claims-fixture pin for claim id "envinstances-config-key":
 * PreLoad's envInstances names a configuration KEY that is looked up in
 * application.properties/yml at boot (AppStarter.getInstancesFromEnv). When the key
 * does not exist in the merged configuration, the annotation's instances value is used.
 * <p>
 * The key-lookup half is also pinned end-to-end by
 * EnvInstanceOverrideTest#shouldReplaceInstancesFromOverride; it is re-asserted here
 * so this single class pins the whole claim.
 * <p>
 * IMPORTANT nuance discovered while pinning: a raw "${...}" literal in envInstances
 * does NOT gracefully fall back - AppConfigReader.getProperty passes the literal to the
 * composite-key parser, which throws on the '$'-rooted key, and
 * AppStarter.getInstancesFromEnv does not catch it, so such an annotation is fatal at
 * application startup. The config-layer half of that behavior is pinned below; the
 * documentation claim must say "nonexistent key falls back", not "${...} falls back".
 */
class ClaimEnvInstancesConfigKeyTest extends TestBase {

    @Test
    void envInstancesIsConfigKeyLookupWithAnnotationFallback() {
        Platform platform = Platform.getInstance();
        AppConfigReader config = AppConfigReader.getInstance();
        // half 1 (key lookup): NoOpFunction declares instances=500 with
        // envInstances="worker.instances.no.op"; application.properties sets that key to 750,
        // so the configured value must replace the annotation's instances
        ServiceDef noOp = platform.getLocalRoutingTable().get(NoOpFunction.ROUTE);
        assertNotNull(noOp, NoOpFunction.ROUTE + " must be preloaded");
        int configured = Integer.parseInt(config.getProperty(NoOpFunction.ENV_INSTANCE_PROPERTY));
        assertNotEquals(500, configured,
                "fixture: the configured value must differ from the annotation's instances=500");
        assertEquals(configured, noOp.getConcurrency(),
                "envInstances must be resolved as a configuration key at boot");
        // half 2 (fallback): an envInstances key that exists nowhere in the merged
        // configuration must fall back to the annotation's instances value
        assertNull(config.getProperty(ClaimEnvInstancesLiteralFunction.NON_EXIST_KEY),
                "fixture: the fallback key must not exist in the merged configuration");
        ServiceDef fallback = platform.getLocalRoutingTable().get(ClaimEnvInstancesLiteralFunction.ROUTE);
        assertNotNull(fallback, ClaimEnvInstancesLiteralFunction.ROUTE + " must be preloaded");
        assertEquals(ClaimEnvInstancesLiteralFunction.ANNOTATION_INSTANCES, fallback.getConcurrency(),
                "a nonexistent envInstances key must fall back to the annotation's instances");
    }

    @Test
    void rawPlaceholderLiteralAsConfigKeyThrowsInsteadOfFallingBack() {
        // pins the actual (surprising) behavior behind the "${...} literal" wording:
        // the key lookup that AppStarter.getInstancesFromEnv performs is
        // AppConfigReader.getProperty(envInstances) - handing it a raw ${...} literal
        // throws from the composite-key parser rather than returning null, and the
        // preload path does not catch it, so it would be fatal at application startup
        AppConfigReader config = AppConfigReader.getInstance();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> config.getProperty("${NON_EXIST_CLAIM_ENV_KEY:99}"));
        assertTrue(String.valueOf(ex.getMessage()).contains("Illegal character"),
                "the composite-key parser must reject the '$'-rooted literal: " + ex);
    }
}
