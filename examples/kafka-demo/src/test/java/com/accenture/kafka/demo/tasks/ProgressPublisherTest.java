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

package com.accenture.kafka.demo.tasks;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code kafka.progress.publisher} turns each progress event into one {@code simple.kafka.notification}: one run's events go to one
 * partition, computed from the run's cid, and the event itself is the record's body.
 */
class ProgressPublisherTest {

    @Test
    void oneRunsEventsGoToOnePartition() {
        var start = Map.<String, Object>of("type", "progress", "event", "node.start", "seq", 1, "cid", "run-7");
        var end = Map.<String, Object>of("type", "result", "seq", 9, "cid", "run-7", "status", 200);
        var first = ProgressPublisher.notification(start, "demo.progress", 10);
        var last = ProgressPublisher.notification(end, "demo.progress", 10);
        assertEquals("simple.kafka.notification", first.getTo());
        assertEquals("demo.progress", first.getHeader("topic"));
        assertEquals("run-7", first.getHeader("cid"));
        assertEquals(first.getHeader("partition"), last.getHeader("partition"), "one run, one partition");
        assertEquals(start, first.getBody());
    }

    @Test
    void runsSpreadOverThePartitions() {
        var partitions = new HashSet<String>();
        for (int i = 0; i < 100; i++) {
            var event = Map.<String, Object>of("type", "progress", "cid", "run-" + i);
            var partition = ProgressPublisher.notification(event, "demo.progress", 10).getHeader("partition");
            var n = Integer.parseInt(partition);
            assertTrue(n >= 0 && n < 10, partition);
            partitions.add(partition);
        }
        assertTrue(partitions.size() > 5, "runs spread over the topic: " + partitions);
    }
}
