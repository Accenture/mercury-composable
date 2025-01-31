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

package org.platformlambda.cloud.services;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.Utility;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 * This is reserved for system use.
 * DO NOT use this directly in your application code.
 */
public class EchoInbox {
    private static final ConcurrentMap<String, EchoInbox> inboxes = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Platform.getInstance().getVirtualThreadExecutor();
    private final String cid = Utility.getInstance().getUuid();
    private final Future<EventEnvelope> future;
    private Promise<EventEnvelope> promise;
    private final long timeout;
    private long timer;

    public EchoInbox(long timeout) {
        this.timeout = Math.max(100, timeout);
        this.future = Future.future(p -> {
            Platform platform = Platform.getInstance();
            this.promise = p;
            inboxes.put(cid, this);
            timer = platform.getVertx().setTimer(timeout, t -> abort(this.cid));
        });
    }

    public static List<EchoInbox> getInboxes() {
        var list = new ArrayList<>(inboxes.keySet());
        List<EchoInbox> allInbox = new ArrayList<>();
        list.forEach(k -> allInbox.add(inboxes.get(k)));
        return allInbox;
    }

    public Future<EventEnvelope> getFuture() {
        return future;
    }

    public void handleEvent(EventEnvelope callback) {
        close();
        Platform.getInstance().getVertx().cancelTimer(timer);
        promise.complete(new EventEnvelope().setStatus(200).setBody(callback.getBody()));
    }

    private void abort(String inboxId) {
        EchoInbox holder = inboxes.get(inboxId);
        if (holder != null) {
            holder.close();
            String error = "Timeout for " + holder.timeout + " ms";
            executor.submit(() -> holder.promise.complete(new EventEnvelope().setStatus(408).setBody(error)));
        }
    }

    private void close() {
        inboxes.remove(cid);
    }
}
