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

 package com.accenture.demo.start;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.CryptoApi;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@MainApplication
public class MainApp implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);
    private static final Utility util = Utility.getInstance();
    private static final CryptoApi crypto = new CryptoApi();

    private static final String TEMP_KEY_STORE = "/tmp/keystore";
    private static final String DEMO_MASTER_KEY = "demo.txt";
    /**
     * This main class is only used when testing the app from the IDE.
     *
     * @param args - command line arguments
     */
    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) {
        // Create a demo encryption key if not exists
        File folder = new File(TEMP_KEY_STORE);
        if (!folder.exists() && folder.mkdirs()) {
            log.info("Folder {} created", folder);
        }
        File f = new File(folder, DEMO_MASTER_KEY);
        if (!f.exists()) {
            String b64Key = util.bytesToBase64(crypto.generateAesKey(256));
            util.str2file(f, b64Key);
            log.info("Demo encryption key {} created", f.getPath());
        }
        log.info("Started");
    }
}
