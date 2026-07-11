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

package org.platformlambda.quartz.common;

import org.junit.jupiter.api.BeforeAll;
import org.platformlambda.core.system.AutoStart;

/**
 * Boots the scheduler example once for the whole test run: AutoStart discovers MainApp,
 * loads the preloaded functions, the cron.yaml jobs and the demo flow.
 */
public abstract class TestBase {

    protected static final long RPC_TIMEOUT = 10000;

    @BeforeAll
    static void setup() {
        AutoStart.main(new String[0]);
    }
}
