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

package org.platformlambda.async;

import org.platformlambda.sync.ReturnRouteCoordinator;

/**
 * Test-only holder for the return-route coordinator (Redis) used by {@code test.endpoint} and
 * {@code soa.reply}, plus the topic names shared between the entry function, the mock System-of-Record
 * task, and the test. The Kafka publisher and flow adapter live in the minimalist-kafka library's
 * {@code KafkaRuntime}, built by its {@code KafkaFlowAutoStart} autoloader.
 */
final class SyncRuntime {

    static final String REQUEST_TOPIC = "topic-1";
    static final String RESPONSE_TOPIC = "topic-2";

    static ReturnRouteCoordinator coordinator;

    private SyncRuntime() { }
}
