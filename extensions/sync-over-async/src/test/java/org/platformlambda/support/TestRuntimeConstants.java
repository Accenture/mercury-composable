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

package org.platformlambda.support;

/**
 * Test-only holder for the topic names shared between the test (which creates the embedded Kafka topics)
 * and the mock System-of-Record task. The {@code sync-to-async} flow now carries the request topic as a
 * {@code text(topic-1)} mapping in its YAML, so this constant is no longer referenced by the REST facade.
 * The return-route coordinator lives in the production {@code org.platformlambda.sync.SyncRuntime}, built by
 * the {@code SyncOverAsyncAutoStart} autoloader; the Kafka publisher and flow adapter live in the
 * minimalist-kafka library's {@code KafkaRuntime}.
 */
public class TestRuntimeConstants {

    public static final String REQUEST_TOPIC = "topic-1";
    public static final String RESPONSE_TOPIC = "topic-2";

    private TestRuntimeConstants() { }
}
