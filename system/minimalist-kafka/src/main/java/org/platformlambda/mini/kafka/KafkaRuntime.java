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

package org.platformlambda.mini.kafka;

/**
 * Holds the process-wide singletons the minimalist Kafka building blocks share: the thread-safe
 * {@link KafkaRequestPublisher} used by {@code simple.kafka.notification}, and the running
 * {@link KafkaFlowAdapter}. Populated once at startup by {@link KafkaFlowAutoStart}.
 */
public final class KafkaRuntime {

    private static KafkaRequestPublisher publisher;
    private static KafkaFlowAdapter adapter;

    private KafkaRuntime() {}

    static void setPublisher(KafkaRequestPublisher instance) {
        publisher = instance;
    }

    public static KafkaRequestPublisher publisher() {
        return publisher;
    }

    static void setAdapter(KafkaFlowAdapter instance) {
        adapter = instance;
    }

    public static KafkaFlowAdapter adapter() {
        return adapter;
    }
}
