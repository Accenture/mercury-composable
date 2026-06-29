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

package org.platformlambda.sync;

import io.lettuce.core.RedisClient;

/**
 * Process-wide holder for the running {@link ReturnRouteCoordinator} and its {@link RedisClient}, populated
 * once at startup by {@link SyncOverAsyncAutoStart}. Composable functions (the synchronous facade entry and
 * the reply task) obtain the coordinator from here - the analogue of the minimalist-kafka {@code KafkaRuntime}.
 */
public final class SyncRuntime {

    private static ReturnRouteCoordinator coordinator;
    private static RedisClient client;

    private SyncRuntime() {}

    static void set(ReturnRouteCoordinator coordinatorInstance, RedisClient clientInstance) {
        coordinator = coordinatorInstance;
        client = clientInstance;
    }

    /** @return the running coordinator, or {@code null} if sync-over-async was not enabled at startup. */
    public static ReturnRouteCoordinator coordinator() {
        return coordinator;
    }

    /** Close the coordinator and shut down the Redis client (idempotent). */
    public static void shutdown() {
        if (coordinator != null) {
            coordinator.close();
            coordinator = null;
        }
        if (client != null) {
            client.shutdown();
            client = null;
        }
    }
}
