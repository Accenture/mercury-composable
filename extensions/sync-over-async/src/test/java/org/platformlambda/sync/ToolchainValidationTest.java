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

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validates the Phase-1 toolchain: embedded Redis starts on this platform and Lettuce can do the two
 * operations the return-route mechanism relies on - a TTL'd value round-trip ({@code SETEX}/{@code GET})
 * and a Pub/Sub wake-up.
 */
class ToolchainValidationTest extends RedisTestBase {

    @Test
    void setexAndGetRoundTrip() {
        try (StatefulRedisConnection<String, String> conn = redisClient.connect()) {
            RedisCommands<String, String> redis = conn.sync();
            redis.setex("response:cid-1", 30, "payload");
            assertEquals("payload", redis.get("response:cid-1"));
        }
    }

    @Test
    void pubSubWakeUp() throws InterruptedException {
        String channel = "svc-return:origin-1";
        BlockingQueue<String> received = new ArrayBlockingQueue<>(1);
        StatefulRedisPubSubConnection<String, String> subscriber = redisClient.connectPubSub();
        subscriber.addListener(new RedisPubSubAdapter<>() {
            @Override
            public void message(String ch, String msg) {
                if (channel.equals(ch)) {
                    received.add(msg);   // capacity-1 queue holds the single expected wake-up
                }
            }
        });
        subscriber.sync().subscribe(channel);
        try (StatefulRedisConnection<String, String> publisher = redisClient.connect()) {
            publisher.sync().publish(channel, "cid-1");
        }
        assertEquals("cid-1", received.poll(5, TimeUnit.SECONDS));
        subscriber.close();
    }
}
