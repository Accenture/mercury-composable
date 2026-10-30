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

package org.platformlambda.mini.kafka.schema;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.ConfigReader;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The retired {@code <prefix>.cache.ttl} reaches Confluent's registry client as {@code latest.cache.ttl.sec}: it
 * timed a ManagedCache in front of Confluent's own id and latest-version caches, and what it still decides is how
 * soon a subject's new {@code latest} version is picked up.
 */
class SchemaCodecRetiredCacheTtlTest {

    private static final String PREFIX = "schema.registry";
    private static final String LATEST = SchemaRegistryClientConfig.LATEST_CACHE_TTL_CONFIG;

    private static Map<String, Object> passed(Map<String, Object> appConfig, Map<String, Object> template) {
        Map<String, Object> srConfig = new HashMap<>(template);
        SchemaCodec.passRetiredCacheTtl(new ConfigReader().load(new HashMap<>(appConfig)), PREFIX, srConfig);
        return srConfig;
    }

    @Test
    void unsetLeavesConfluentsDefault() {
        assertFalse(passed(Map.of(), Map.of()).containsKey(LATEST), "unset: Confluent's own 60-second default");
        assertFalse(passed(Map.of(PREFIX + ".cache.ttl", " "), Map.of()).containsKey(LATEST),
                "blank (the ${ENV_VAR:} idiom) is unset");
    }

    @Test
    void aSetValueIsPassedInSeconds() {
        assertEquals(1800L, passed(Map.of(PREFIX + ".cache.ttl", "30m"), Map.of()).get(LATEST));
        assertEquals(0L, passed(Map.of(PREFIX + ".cache.ttl", "0s"), Map.of()).get(LATEST),
                "zero is passed: latest is then never cached");
    }

    @Test
    void theTemplatesOwnLatestCacheTtlWins() {
        assertEquals("120", passed(Map.of(PREFIX + ".cache.ttl", "30m"), Map.of(LATEST, "120")).get(LATEST));
    }

    @Test
    void aValueThatIsNotADurationIsNotPassed() {
        // a negative TTL would make Confluent cache latest forever
        assertFalse(passed(Map.of(PREFIX + ".cache.ttl", "soon"), Map.of()).containsKey(LATEST));
    }
}
