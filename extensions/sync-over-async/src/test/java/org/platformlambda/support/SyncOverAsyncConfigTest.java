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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncOverAsyncConfigTest {

    @Test
    void defaultsAreSensible() {
        SyncOverAsyncConfig config = SyncOverAsyncConfig.defaults();
        assertEquals("svc-return", config.returnChannelPrefix());
        assertEquals(90, config.routeTtlSeconds());
        assertEquals(30, config.responseTtlSeconds());
        assertEquals(10_000, config.maxPendingRequests());
    }

    @Test
    void fromFallsBackToDefaults() {
        SyncOverAsyncConfig config = SyncOverAsyncConfig.from(new MapConfig(Map.of()));
        assertEquals(SyncOverAsyncConfig.defaults(), config);
    }

    @Test
    void fromReadsProperties() {
        SyncOverAsyncConfig config = SyncOverAsyncConfig.from(new MapConfig(Map.of(
                "sync.return.channel.prefix", "orders-return",
                "sync.route.ttl.seconds", "120",
                "sync.response.ttl.seconds", "45",
                "sync.max.pending.requests", "500")));
        assertEquals("orders-return", config.returnChannelPrefix());
        assertEquals(120, config.routeTtlSeconds());
        assertEquals(45, config.responseTtlSeconds());
        assertEquals(500, config.maxPendingRequests());
    }
}
