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

package org.platformlambda.mock;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The kafka-standalone MainApplication (sequence 10) starts the embedded broker first.
 * This test app runs after it (sequence 20), waits for the broker port and then
 * connects the platform to the kafka cloud connector.
 */
@MainApplication(sequence = 20)
public class TestApp implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(TestApp.class);

    @Override
    public void start(String[] args) {
        Utility util = Utility.getInstance();
        for (int i = 0; i < 60; i++) {
            if (util.portReady("127.0.0.1", 9092, 1000)) {
                break;
            }
            util.sleep(1000);
        }
        Platform.getInstance().connectToCloud();
        log.info("Test app started");
    }
}
