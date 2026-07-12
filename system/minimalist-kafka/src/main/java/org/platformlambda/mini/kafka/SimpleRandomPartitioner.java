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

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

/**
 * A perfectly stateless partitioner: each keyless record goes to a uniformly random partition.
 *
 * <p>Kafka's built-in default is a <i>sticky</i> partitioner - it keeps writing to one partition until a
 * batch closes, which is throughput-friendly but skews low-volume traffic badly (an app publishing a few
 * messages at a time can land everything on a single partition, leaving a multi-instance consumer group
 * mostly idle). The built-in {@code RoundRobinPartitioner} fixes the skew but keeps per-topic counters and
 * has known unevenness when batches are recreated. Random distribution needs no state at all and converges
 * on uniform - the pattern proven in field deployments of this library.</p>
 *
 * <p>Semantics preserved from the default partitioner:</p>
 * <ul>
 *   <li>a record with an <b>explicit partition</b> (e.g. the {@code partition} header of
 *       {@code simple.kafka.notification}) never reaches any partitioner - explicit always wins</li>
 *   <li>a <b>keyed</b> record still maps by murmur2 key hash, so key-based ordering is not broken if a
 *       caller ever publishes with a record key (the bundled publisher currently does not)</li>
 * </ul>
 *
 * <p>{@link KafkaClientConfig#producerProperties} installs this class as the default
 * {@code partitioner.class}; a template that sets its own partitioner overrides it.</p>
 */
public class SimpleRandomPartitioner implements Partitioner {

    // SecureRandom is thread-safe and keeps no per-thread state - virtual thread friendly design
    // (ThreadLocalRandom is avoided for the same reason the repo avoids ThreadLocal in general)
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes,
                         Cluster cluster) {
        List<PartitionInfo> available = cluster.availablePartitionsForTopic(topic);
        List<PartitionInfo> candidates = available.isEmpty() ? cluster.partitionsForTopic(topic) : available;
        if (keyBytes != null) {
            // keyed records keep Kafka's key-hash semantics over ALL partitions (same as the default
            // partitioner), so a key always maps to the same partition regardless of leader availability
            return Utils.toPositive(Utils.murmur2(keyBytes)) % cluster.partitionsForTopic(topic).size();
        }
        return candidates.get(RANDOM.nextInt(candidates.size())).partition();
    }

    @Override
    public void close() {
        // stateless - nothing to release
    }

    @Override
    public void configure(Map<String, ?> configs) {
        // stateless - nothing to configure
    }
}
