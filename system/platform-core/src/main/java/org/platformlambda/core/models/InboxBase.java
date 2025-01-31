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

package org.platformlambda.core.models;

import io.vertx.core.Future;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public abstract class InboxBase {
    protected static final ExecutorService executor = Platform.getInstance().getVirtualThreadExecutor();
    protected static final ConcurrentMap<String, InboxBase> inboxes = new ConcurrentHashMap<>();
    protected static final String RPC = "rpc";
    protected static final String UNDERSCORE = "_";
    protected static final String ANNOTATIONS = "annotations";

    public final String cid = Utility.getInstance().getUuid();

    public static InboxBase getHolder(String inboxId) {
        return inboxes.get(inboxId);
    }

    public String getCorrelationId() {
        return cid;
    }

    public void close() {
        inboxes.remove(cid);
    }

    /**
     * To be overridden by various inbox implementations
     *
     * @param callback event
     * @return nothing
     */
    public Void handleEvent(EventEnvelope callback) {
        throw new IllegalArgumentException("Not implemented");
    }
}
