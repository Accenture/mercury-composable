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

import io.lettuce.core.RedisClient;
import org.platformlambda.core.annotations.MainApplication;
import org.platformlambda.core.models.EntryPoint;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.support.RedisConfig;
import org.platformlambda.support.SyncOverAsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Autoloads the cross-pod return-route engine at startup. It runs as a {@link MainApplication} - i.e.
 * AFTER the platform-core engine has registered every composable function.
 *
 * <p>Because the {@link ReturnRouteCoordinator} <b>eagerly connects to Redis</b> (command connection +
 * Pub/Sub subscription), this autoloader is <b>opt-in</b>: it does nothing unless
 * {@code sync.over.async.enabled=true}. When enabled it reads the discrete {@code redis.*} startup
 * parameters ({@link RedisConfig}) and the engine tunables ({@link SyncOverAsyncConfig}) from
 * {@code application.properties}, builds the {@link RedisClient} and the coordinator keyed by this pod's
 * {@link Platform#getOrigin() origin-id}, starts it, and publishes it via {@link SyncRuntime}.</p>
 */
@MainApplication
public class SyncOverAsyncAutoStart implements EntryPoint {

    private static final Logger log = LoggerFactory.getLogger(SyncOverAsyncAutoStart.class);
    private static final String ENABLED = "sync.over.async.enabled";

    @Override
    public void start(String[] args) {
        AppConfigReader config = AppConfigReader.getInstance();
        if (!"true".equalsIgnoreCase(config.getProperty(ENABLED, "false"))) {
            log.info("{} not true; sync-over-async return-route coordinator not started", ENABLED);
            return;
        }
        RedisConfig redisConfig = RedisConfig.from(config);
        SyncOverAsyncConfig syncConfig = SyncOverAsyncConfig.from(config);
        String originId = Platform.getInstance().getOrigin();

        RedisClient client = RedisClient.create(redisConfig.toUri());
        ReturnRouteCoordinator coordinator = new ReturnRouteCoordinator(client, originId, syncConfig);
        coordinator.start();
        SyncRuntime.set(coordinator, client);
        log.info("Return-route coordinator started for pod {} (redis {}:{}, ssl={})",
                originId, redisConfig.host(), redisConfig.port(), redisConfig.ssl());
    }
}
