/*

    Copyright 2018-2025 Accenture Technology

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

package com.accenture.common;

import org.junit.jupiter.api.BeforeAll;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.system.AutoStart;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class TestBase {

    protected static final String HTTP_REQUEST = "async.http.request";
    protected static final long RPC_TIMEOUT = 10000;

    protected static int springPort;
    protected static int restPort;

    private static final AtomicInteger startCounter = new AtomicInteger(0);

    @BeforeAll
    public static void setup() {
        if (startCounter.incrementAndGet() == 1) {
            Utility util = Utility.getInstance();
            AppConfigReader config = AppConfigReader.getInstance();
            springPort = util.str2int(config.getProperty("server.port", "8085"));
            restPort = util.str2int(config.getProperty("rest.server.port", "8086"));
            AutoStart.main(new String[0]);
        }
    }
}
