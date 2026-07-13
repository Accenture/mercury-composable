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

package com.accenture.twin.demo;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.AppConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the twin-kafka worked example. The same jar runs in one of three roles, selected by
 * the active Spring profile ({@code -Dspring.profiles.active=rest|bridge|sor}):
 *
 * <ul>
 *   <li><b>rest</b> - the HTTP edge on the on-prem side. Accepts READ / UPSERT / DELETE profile requests,
 *       acknowledges immediately (202), and publishes each request to the on-prem {@code OP_PROFILE_REQUEST}
 *       topic in the Confluent JSON Schema wire format via {@code simple.kafka.notification}.</li>
 *   <li><b>bridge</b> - the twin-kafka dual-cluster bridge. Consumes {@code OP_PROFILE_REQUEST} from the
 *       on-prem cluster (schema-decoded), forwards it to the cloud {@code C_PROFILE_REQUEST} topic as plain
 *       JSON bytes via {@code secondary.kafka.notification}; and in the reverse direction consumes
 *       {@code C_PROFILE_RESPONSE} from the cloud and re-encodes it onto the on-prem
 *       {@code OP_PROFILE_RESPONSE} topic with the response schema. Both flows are pure YAML - no Java.</li>
 *   <li><b>sor</b> - the system-of-record on the cloud side. It consumes {@code C_PROFILE_REQUEST},
 *       applies the command to the temp store {@code /tmp/twin-kafka-demo} (one JSON file per profile id),
 *       and publishes the outcome to {@code C_PROFILE_RESPONSE} through {@code secondary.kafka.notification}
 *       so the response stays on the cloud cluster until the bridge consumes it.</li>
 * </ul>
 *
 * <p>This demo packages all three roles in one jar for convenience. The secondary flow adapter starts only
 * in the bridge profile because only application-bridge.properties sets {@code yaml.secondary.kafka.flow.adapter};
 * the SOR profile uses only the secondary notification publisher.</p>
 */
@MainApplication
public class MainApp implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) {
        String profiles = AppConfigReader.getInstance().getProperty("spring.profiles.active", "(none)");
        log.info("twin-kafka-demo started; active profile(s)={}", profiles);
    }
}
