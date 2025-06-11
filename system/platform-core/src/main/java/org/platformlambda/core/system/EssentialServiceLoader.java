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

package org.platformlambda.core.system;

import org.platformlambda.automation.http.AsyncHttpClient;
import org.platformlambda.core.annotations.BeforeApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.services.DistributedTrace;
import org.platformlambda.core.services.TemporaryInbox;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 * <p>
 * This module loads essential services before running application start-up code
 * so that services are available to them. It is therefore has a sequence number of 0.
 */
@BeforeApplication(sequence = 0)
public class EssentialServiceLoader implements EntryPoint {

    @Override
    public void start(String[] args) {
        Platform platform = Platform.getInstance();
        platform.registerPrivate(TemporaryInbox.TEMPORARY_INBOX, new TemporaryInbox(), 100);
        platform.registerPrivate(AsyncHttpClient.ASYNC_HTTP_REQUEST, new AsyncHttpClient(), 100);
        platform.registerPrivate(DistributedTrace.DISTRIBUTED_TRACING, new DistributedTrace(), 1);
    }
}
