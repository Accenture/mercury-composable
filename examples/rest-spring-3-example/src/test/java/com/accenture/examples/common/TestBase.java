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

package com.accenture.examples.common;

import org.junit.jupiter.api.BeforeAll;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

/**
 * Boots the example application once for the whole test run: AutoStart discovers MainApp
 * (@MainApplication), starts Spring Boot on server.port and loads the preloaded functions.
 */
public abstract class TestBase {

    protected static final String HTTP_REQUEST = "async.http.request";
    protected static final long RPC_TIMEOUT = 10000;

    protected static int springPort;

    @BeforeAll
    static void setup() {
        springPort = Utility.getInstance().str2int(
                AppConfigReader.getInstance().getProperty("server.port", "8083"));
        AutoStart.main(new String[0]);
    }
}
