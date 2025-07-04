/*

    Copyright 2018-2025 Accenture Technology

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

package org.platformlambda.core.system;

import io.vertx.core.eventbus.MessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public abstract class WorkerQueues {
    private static final Logger log = LoggerFactory.getLogger(WorkerQueues.class);

    protected static final ExecutorService kernelExecutor = Platform.getInstance().getKernelThreadExecutor();
    protected static final ExecutorService vThreadExecutor = Platform.getInstance().getVirtualThreadExecutor();
    protected static final String READY = "ready:";
    protected final ServiceDef def;
    protected final String route;
    protected final String parentRoute;
    protected MessageConsumer<byte[]> consumer = null;
    protected boolean stopped = false;

    protected WorkerQueues(ServiceDef def, String route) {
        this.def = def;
        this.route = route;
        this.parentRoute = route.contains("#")? route.substring(0, route.lastIndexOf('#')) : route;
    }

    protected void started() {
        log.debug("{} started", route);
    }

    protected void stop() {
        if (consumer != null && consumer.isRegistered()) {
            consumer.unregister();
            stopped = true;
            log.debug("{} stopped", route);
        }
    }
}
