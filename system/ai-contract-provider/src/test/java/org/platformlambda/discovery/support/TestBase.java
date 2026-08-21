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

package org.platformlambda.discovery.support;

import org.junit.jupiter.api.BeforeAll;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.AppConfigReader;

import java.util.concurrent.atomic.AtomicInteger;

public class TestBase {
    private static final AtomicInteger startCounter = new AtomicInteger(0);
    protected static String host;

    @BeforeAll
    static void setup() {
        if (startCounter.incrementAndGet() == 1) {
            AppConfigReader config = AppConfigReader.getInstance();
            host = "http://127.0.0.1:" + config.getProperty("rest.server.port", "8999");
            AutoStart.main(new String[0]);
        }
    }
}
