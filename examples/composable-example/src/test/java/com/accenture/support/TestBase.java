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

package com.accenture.support;

import org.junit.jupiter.api.BeforeAll;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.CryptoApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class TestBase {
    private static final Logger log = LoggerFactory.getLogger(TestBase.class);

    private static final AtomicInteger startCounter = new AtomicInteger(0);
    protected static final CryptoApi crypto = new CryptoApi();
    protected static boolean strongCrypto;
    protected static String HOST;

    @BeforeAll
    public static void setup() {
        // execute only once
        if (startCounter.incrementAndGet() == 1) {
            AppConfigReader config = AppConfigReader.getInstance();
            HOST = "http://127.0.0.1:" + config.getProperty("rest.server.port", "8100");
            AutoStart.main(new String[0]);
            strongCrypto = crypto.strongCryptoSupported();
            if (!strongCrypto) {
                log.warn("Not using Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy");
                log.info("AES-128 supported");
            } else {
                log.info("Using Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy");
                log.info("AES-256 supported");
            }
        }
    }
}
