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

import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleRandomPartitionerTest {

    private static final String TOPIC = "random-partitioner-test";
    private static final int PARTITIONS = 10;

    private static Cluster cluster(int partitions, boolean withLeaders) {
        Node node = new Node(1, "127.0.0.1", 9092);
        List<PartitionInfo> info = new ArrayList<>();
        for (int i = 0; i < partitions; i++) {
            // leader == null marks a partition unavailable for the availablePartitionsForTopic view
            info.add(new PartitionInfo(TOPIC, i, withLeaders ? node : null, new Node[]{node}, new Node[]{node}));
        }
        return new Cluster("test-cluster", List.of(node), info, Set.of(), Set.of());
    }

    @Test
    @SuppressWarnings("resource")   // stateless partitioner - close() is a no-op
    void keylessRecordsSpreadAcrossPartitions() {
        SimpleRandomPartitioner partitioner = new SimpleRandomPartitioner();
        partitioner.configure(Map.of());
        Cluster cluster = cluster(PARTITIONS, true);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            int p = partitioner.partition(TOPIC, null, null, null, new byte[]{1}, cluster);
            assertTrue(p >= 0 && p < PARTITIONS, "partition must be in range");
            seen.add(p);
        }
        // 500 uniform draws over 10 partitions miss a given partition with probability (0.9)^500 ~ 1e-23,
        // so requiring all 10 is deterministic for practical purposes - this is the anti-sticky assertion
        assertEquals(PARTITIONS, seen.size(), "random distribution must reach every partition");
    }

    @Test
    @SuppressWarnings("resource")
    void keyedRecordsMapDeterministically() {
        SimpleRandomPartitioner partitioner = new SimpleRandomPartitioner();
        Cluster cluster = cluster(PARTITIONS, true);
        byte[] key = "profile-100".getBytes(StandardCharsets.UTF_8);
        int first = partitioner.partition(TOPIC, "profile-100", key, null, new byte[]{1}, cluster);
        for (int i = 0; i < 20; i++) {
            assertEquals(first, partitioner.partition(TOPIC, "profile-100", key, null, new byte[]{1}, cluster),
                    "a keyed record must always map to the same partition");
        }
    }

    @Test
    @SuppressWarnings("resource")
    void fallsBackToAllPartitionsWhenNoneAvailable() {
        SimpleRandomPartitioner partitioner = new SimpleRandomPartitioner();
        Cluster cluster = cluster(PARTITIONS, false);
        for (int i = 0; i < 50; i++) {
            int p = partitioner.partition(TOPIC, null, null, null, new byte[]{1}, cluster);
            assertTrue(p >= 0 && p < PARTITIONS, "must fall back to the full partition list");
        }
        partitioner.close();
    }
}
