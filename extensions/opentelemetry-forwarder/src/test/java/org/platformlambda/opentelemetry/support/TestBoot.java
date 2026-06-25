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

package org.platformlambda.opentelemetry.support;

import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.Platform;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Shared boot helper for the integration tests. Starts the platform once and waits for the REST
 * automation HTTP server to be ready by blocking on the {@code async.http.response} provider, which
 * the server registers only after {@code listen()} succeeds - a signal-based wait, not a poll.
 */
final class TestBoot {

    private TestBoot() { }

    static void start() throws InterruptedException {
        AutoStart.main(new String[0]);
        BlockingQueue<Boolean> ready = new ArrayBlockingQueue<>(1);
        Platform.getInstance().waitForProvider(AsyncHttpClient.ASYNC_HTTP_RESPONSE, 20).onSuccess(ready::add);
        if (!Boolean.TRUE.equals(ready.poll(20, TimeUnit.SECONDS))) {
            throw new IllegalStateException("REST automation HTTP server did not become ready");
        }
    }
}
