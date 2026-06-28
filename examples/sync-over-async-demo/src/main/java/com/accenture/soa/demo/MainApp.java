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

package com.accenture.soa.demo;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.util.AppConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the sync-over-async worked example. The same jar runs in one of two roles, selected by
 * the active Spring profile ({@code -Dspring.profiles.active=facade|backend}):
 *
 * <ul>
 *   <li><b>facade</b> - a synchronous REST endpoint ({@code POST /api/sync-to-async}) backed by the
 *       {@code sync-to-async} flow (sync.prepare -> simple.kafka.notification -> sync.await), plus the
 *       {@code soa-reply} flow and the Redis return-route coordinator. It publishes the request to the
 *       {@code soa.request} topic and blocks until the reply arrives on {@code soa.response}.</li>
 *   <li><b>backend</b> - the {@code system-of-record} flow: consumes {@code soa.request}, processes it,
 *       and publishes the reply to {@code soa.response}. No REST, no Redis.</li>
 * </ul>
 *
 * Run the backend and one (or more) facades as separate JVMs - the Redis return route delivers each reply
 * back to the exact facade pod that is awaiting it, which is the whole point of sync-over-async.
 */
@MainApplication
public class MainApp implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) {
        String profiles = AppConfigReader.getInstance().getProperty("spring.profiles.active", "(none)");
        log.info("sync-over-async-demo started; active profile(s)={}", profiles);
    }
}
