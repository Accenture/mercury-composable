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

package org.platformlambda.tasks;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.mini.kafka.KafkaHeaders;
import org.platformlambda.sync.SyncRuntime;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Reusable reply-delivery task for the sync-over-async pattern. Wire it as the task of a flow bound (via
 * the Kafka flow adapter) to the response topic: it hands the asynchronous response to the return-route
 * coordinator, which completes the awaiting REST request - cross-pod via the Redis return route, so the
 * pod that consumed the reply need not be the one that originated the request.
 *
 * <p>This is generic boilerplate every sync-over-async application needs, so it ships with the extension
 * alongside {@link SyncPrepareTask} and {@link SyncAwaitTask}; an application only supplies its own
 * backend (system-of-record) logic. Sized for user-facing concurrency ({@code instances = 250}).</p>
 */
@PreLoad(route = "soa.reply", instances = 250)
public class SoaReplyTask implements TypedLambdaFunction<byte[], Map<String, Object>> {

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, byte[] input, int instance) {
        String cid = headers.get(KafkaHeaders.CORRELATION_ID);
        String payload = new String(input, StandardCharsets.UTF_8);
        boolean delivered = SyncRuntime.coordinator().deliver(cid, payload);
        return Map.of("cid", cid, "delivered", delivered);
    }
}
