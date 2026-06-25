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

package org.platformlambda.opentelemetry.mock;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the mock-collector test app. Boot it from your IDE
 * ({@code MockOtlpAppMain.main(new String[0])}) and the REST automation server starts on
 * {@code server.port} (8299 in {@code src/test/resources/application.properties}), exposing the
 * composable mock collector at {@code POST /api/v2/otlp/v1/traces}. Point a real OTLP exporter
 * (or any backend) at it and watch the requests arrive.
 */
@MainApplication
public class MockOtlpAppMain implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(MockOtlpAppMain.class);

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    /**
     * REST automation + the mock collector are wired by annotation + rest.yaml;
     * this satisfies the "at least one MainApplication" requirement.
     * @param args ignored
     */
    @Override
    public void start(String[] args) {
        log.info("Mock OTLP collector app started - POST /api/v2/otlp/v1/traces");
    }
}
