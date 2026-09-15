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

package com.accenture.cache.demo;

import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.AutoStart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the distributed-cache worked example. The app exposes the same profile GET/POST/DELETE
 * CRUD three times - one route family per layer, all backed by ONE shared Redis cache ({@code v1.cache.redis}):
 *
 * <ul>
 *   <li><b>Layer 1 (Platform Core / PostOffice)</b> - {@code /api/l1/profile/{profile_id}}: a single function
 *       ({@code v1.profile.l1}) calls the cache with the PostOffice RPC API in code.</li>
 *   <li><b>Layer 2 (Event Script)</b> - {@code /api/l2/profile/{profile_id}}: a flow composes the cache task
 *       with the encode/decode helper functions declaratively.</li>
 *   <li><b>Layer 3 (Knowledge Graph)</b> - {@code /api/graph/profile-cache}: a graph node drives the same
 *       route through {@code graph.task}. Layer 3 needs no endpoint of its own - the standard graph API
 *       {@code /api/graph/{graph_id}} serves every graph deployed in the app.</li>
 * </ul>
 *
 * A profile POSTed through one layer is readable through the other two - the cache key is the profile id and
 * the value is the profile Map packed as plain MsgPack (one wire format across all layers, and across the
 * Rust port). Run a standalone Redis first (helpers/redis-standalone); see README.md.
 */
@MainApplication
public class MainApp implements EntryPoint {
    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        AutoStart.main(args);
    }

    @Override
    public void start(String[] args) {
        log.info("distributed-cache-example started - profile CRUD over v1.cache.redis at Layers 1, 2 and 3");
    }
}
