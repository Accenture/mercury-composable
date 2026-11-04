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

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;

import java.util.Map;

/**
 * A return route that publishes a graph run's progress events to a Kafka topic (RFC-0011 section 8.4): the generic
 * return path in one small function. A caller names {@code kafka.progress.publisher} in its {@code x-return-path},
 * the graph application declares it in {@code return.path.routes}, and each event the engine or {@code llm.run}
 * sends arrives here as a JSON object, which this function publishes through {@code simple.kafka.notification}.
 * <p>
 * One run's events stay in order on one partition: {@code simple.kafka.notification} publishes without a record
 * key, so the partition is computed from the event's {@code cid} - the content-based partitioning the
 * minimalist-kafka guide describes. A consumer orders a run's events by {@code seq} and finds a gap the same way.
 * Delivery belongs to Kafka: the engine sends each event and never waits.
 * <p>
 * Settings: {@code progress.kafka.topic} (default {@code demo.progress}) and {@code progress.kafka.partitions}, the
 * topic's partition count (default 10, as the demo creates its topics).
 */
@PreLoad(route = ProgressPublisher.ROUTE, instances = 10)
public class ProgressPublisher implements TypedLambdaFunction<Map<String, Object>, Void> {
    public static final String ROUTE = "kafka.progress.publisher";
    private static final String NOTIFICATION_ROUTE = "simple.kafka.notification";
    private static final String CID = "cid";

    @Override
    public Void handleEvent(Map<String, String> headers, Map<String, Object> event, int instance) {
        var config = AppConfigReader.getInstance();
        var topic = config.getProperty("progress.kafka.topic", "demo.progress");
        var partitions = Utility.getInstance().str2int(config.getProperty("progress.kafka.partitions", "10"));
        EventEmitter.getInstance().send(notification(event, topic, partitions));
        return null;
    }

    /**
     * The notification that publishes one event: the topic, the run's partition and its correlation id as headers,
     * the event as the body (published as JSON).
     *
     * @param event a progress, result or error event
     * @param topic the topic
     * @param partitions the topic's partition count
     * @return the event for {@code simple.kafka.notification}
     */
    static EventEnvelope notification(Map<String, Object> event, String topic, int partitions) {
        var cid = String.valueOf(event.get(CID));
        var partition = Math.floorMod(cid.hashCode(), Math.max(1, partitions));
        return new EventEnvelope().setTo(NOTIFICATION_ROUTE).setHeader("topic", topic)
                .setHeader("partition", String.valueOf(partition)).setHeader(CID, cid).setBody(event);
    }
}
