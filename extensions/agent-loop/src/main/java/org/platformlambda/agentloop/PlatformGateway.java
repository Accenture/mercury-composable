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

package org.platformlambda.agentloop;

import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.PostOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The loop's routes through the platform, the path {@code graph.task} uses: a local route is a local call, and a
 * language pack's route ({@code llm.chat}, an MCP binding) is one Event-over-HTTP hop. The PostOffice is the
 * {@code llm.run} function's own, so every request issued on its worker thread carries the {@code llm.run} span as its
 * parent.
 */
public class PlatformGateway implements LoopGateway {
    /** The model turn's route, served by a language pack's LLM helper. */
    public static final String LLM_CHAT = "llm.chat";
    /** The header a caller names its return route in, for progress events (RFC-0011 section 8.4). */
    public static final String X_RETURN_PATH = "x-return-path";
    private static final Logger log = LoggerFactory.getLogger(PlatformGateway.class);
    private static final String CID = "cid";
    private final PostOffice po;
    private final String returnPath;
    private final String businessCid;
    private final AtomicBoolean warned = new AtomicBoolean();

    public PlatformGateway(PostOffice po) {
        this(po, null, null);
    }

    /**
     * @param po the function's post office
     * @param returnPath the caller's return route for progress events, or null
     * @param cid the business correlation id the events carry, or null
     */
    public PlatformGateway(PostOffice po, String returnPath, String cid) {
        this.po = po;
        this.returnPath = returnPath == null || returnPath.isBlank()? null : returnPath.trim();
        this.businessCid = cid;
    }

    @Override
    public boolean reachable(String route) {
        try {
            return po.exists(route) || po.getEventHttpTarget(route) != null;
        } catch (IllegalArgumentException e) {
            // an invalid route name is served by nothing
            return false;
        }
    }

    @Override
    public CompletableFuture<EventEnvelope> chat(Map<String, Object> request, long timeoutMs) {
        return po.eRequest(new EventEnvelope().setTo(LLM_CHAT).setBody(request), timeoutMs, false);
    }

    @Override
    public CompletableFuture<EventEnvelope> call(String route, Object body, long timeoutMs) {
        return po.eRequest(new EventEnvelope().setTo(route).setBody(body), timeoutMs, false);
    }

    /** Untraced, so a run's progress adds no span per event; a failing return route is logged once. */
    @Override
    public void progress(Map<String, Object> event) {
        if (returnPath == null) {
            return;
        }
        if (businessCid != null) {
            event.putIfAbsent(CID, businessCid);
        }
        try {
            EventEmitter.getInstance().send(new EventEnvelope().setTo(returnPath).setBody(event));
        } catch (RuntimeException e) {
            if (warned.compareAndSet(false, true)) {
                log.warn("Progress to {} is not delivered - {}", returnPath, e.getMessage());
            }
        }
    }
}
