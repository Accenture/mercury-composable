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
import org.platformlambda.system.EmbeddedRedis;

/**
 * Standalone Redis dev server. Runs as a {@link MainApplication} on the platform-core runtime (so the JVM
 * stays alive and logging/config conventions match the sibling {@code kafka-standalone} helper), then
 * starts a single embedded {@code redis-server} on the configured {@code redis.port} (default 6379).
 *
 * <p>For local development and testing only. Prefer Docker yourself if that fits your workflow better;
 * this exists to make life easier when you do not want to.</p>
 */
@MainApplication
public class MainApp implements EntryPoint {

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) {
        AppConfigReader config = AppConfigReader.getInstance();
        int port = Integer.parseInt(config.getProperty("redis.port", "6379"));
        new EmbeddedRedis(port).start();
    }
}
