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

package org.platformlambda.example;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.system.EmbeddedMongo;

/**
 * Standalone MongoDB-compatible dev server. Runs as a {@link MainApplication} on the platform-core runtime (so the
 * JVM stays alive and logging and configuration match the sibling {@code redis-standalone} helper), then starts one
 * in-memory server on the configured {@code mongodb.port} (default 27017).
 *
 * <p>For local development and testing only, on a machine where Docker is not available.</p>
 */
@MainApplication
public class MainApp implements EntryPoint {

    // The server started by this app, retained so an embedding process (or a unit test) can stop it directly
    // instead of relying solely on the platform's shutdown hook.
    EmbeddedMongo mongo;

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) {
        AppConfigReader config = AppConfigReader.getInstance();
        int port = Integer.parseInt(config.getProperty("mongodb.port", "27017"));
        mongo = new EmbeddedMongo(port);
        mongo.start();
    }
}
