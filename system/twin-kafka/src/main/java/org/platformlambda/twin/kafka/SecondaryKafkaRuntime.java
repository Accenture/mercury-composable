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

package org.platformlambda.twin.kafka;

import org.platformlambda.mini.kafka.KafkaFlowAdapter;
import org.platformlambda.mini.kafka.KafkaRequestPublisher;
import org.platformlambda.mini.kafka.schema.SchemaCodec;

/**
 * Holder for the SECONDARY Kafka cluster's shared runtime objects, mirroring minimalist-kafka's
 * {@code KafkaRuntime} (which stays dedicated to the primary cluster). The two runtimes are fully
 * separate - publisher, optional flow adapter, and optional schema codec each exist per cluster,
 * so dead-letter writes, subject resolution, and schema-id caches never cross clusters.
 */
public final class SecondaryKafkaRuntime {

    private static KafkaRequestPublisher publisher;
    private static KafkaFlowAdapter adapter;
    private static SchemaCodec schemaCodec;

    private SecondaryKafkaRuntime() {}

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

    static void setSchemaCodec(SchemaCodec instance) {
        schemaCodec = instance;
    }

    public static SchemaCodec schemaCodec() {
        return schemaCodec;
    }
}
