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

package org.platformlambda.core.logging;

import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.EventEmitter;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.ConfigReader;
import org.platformlambda.core.util.Utility;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end coverage of the WorkerHandler log-context bracket and PostOffice.updateContext during a
 * real traced worker invocation.
 */
class WorkerLogContextTest extends TestBase {

    private static final Utility util = Utility.getInstance();

    private LogContextConfig enabledConfig() {
        Map<String, Object> section = new HashMap<>();
        section.put("service", "$service");
        section.put("traceId", "$traceId");
        section.put("cid", "$cid");
        Map<String, Object> root = new HashMap<>();
        root.put("context", section);
        return new LogContextConfig(new ConfigReader().load(root));
    }

    @Test
    @SuppressWarnings("unchecked")
    void workerRegistersContextAndUpdateContextWorks() throws InterruptedException {
        Platform platform = Platform.getInstance();
        String route = "log.context.test.function";
        String traceId = util.getUuid();
        String cid = "corr-" + util.getUuid();
        BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(1);
        AtomicLong workerThreadId = new AtomicLong();

        LambdaFunction f = (headers, input, instance) -> {
            PostOffice po = PostOffice.trackable(headers, instance);
            // reserved keys cannot be overridden
            boolean rejected = false;
            try {
                po.updateContext("cid", "should-not-apply");
            } catch (IllegalArgumentException e) {
                rejected = true;
            }
            po.updateContext("rejectedReserved", rejected);
            po.updateContext("orderId", "ord-1");
            workerThreadId.set(Thread.currentThread().threadId());
            LogContext ctx = LogContextManager.get(Thread.currentThread().threadId());
            bench.add(LogContextConfig.getInstance().render(ctx, System.currentTimeMillis()));
            return "ok";
        };
        platform.registerPrivate(route, f, 1);
        LogContextConfig.setInstanceForTest(enabledConfig());
        try {
            PostOffice po = new PostOffice("unit.test", traceId, "GET /api/log/ctx");
            po.send(new EventEnvelope().setTo(route).setBody("x")
                    .addTag(EventEmitter.BUSINESS_CID_TAG, cid));

            Map<String, Object> rendered = bench.poll(10, TimeUnit.SECONDS);
            assertNotNull(rendered, "function should have rendered the log context");
            assertEquals(route, rendered.get("service"));   // $service = the function's route
            assertEquals(traceId, rendered.get("traceId"));
            assertEquals(cid, rendered.get("cid"));
            assertEquals("ord-1", rendered.get("orderId"));
            assertEquals(true, rendered.get("rejectedReserved"));

            // the context must be removed once the worker returns
            long tid = workerThreadId.get();
            for (int i = 0; i < 40 && LogContextManager.get(tid) != null; i++) {
                Utility.getInstance().sleep(50);
            }
            assertNull(LogContextManager.get(tid), "log context must be removed after the worker returns");
        } finally {
            platform.release(route);
            LogContextConfig.setInstanceForTest(null);
        }
    }

    @Test
    void logContextCidIsTheBusinessCorrelationIdOnly() throws InterruptedException {
        // the log context 'cid' resolves exactly like the injected my_correlation_id:
        // the engine my_cid tag, else a legacy peer's transported header, else UNSET -
        // an internal routing id (an envelope correlation-id such as the graph engine's
        // composite skill-callback ids) must never appear under the 'cid' label
        Platform platform = Platform.getInstance();
        String route = "log.context.business.cid.function";
        String internalCid = "internal-" + util.getUuid();
        String businessCid = "order-" + util.getUuid();
        String legacyCid = "legacy-" + util.getUuid();
        BlockingQueue<Map<String, Object>> bench = new ArrayBlockingQueue<>(1);
        LambdaFunction f = (headers, input, instance) -> {
            LogContext ctx = LogContextManager.get(Thread.currentThread().threadId());
            bench.add(LogContextConfig.getInstance().render(ctx, System.currentTimeMillis()));
            return "ok";
        };
        platform.registerPrivate(route, f, 1);
        LogContextConfig.setInstanceForTest(enabledConfig());
        try {
            PostOffice po = new PostOffice("unit.test", util.getUuid(), "GET /api/log/business/cid");
            // 1. the business tag wins over the envelope correlation-id
            po.send(new EventEnvelope().setTo(route).setBody("x").setCorrelationId(internalCid)
                    .addTag(EventEmitter.BUSINESS_CID_TAG, businessCid));
            Map<String, Object> tagged = bench.poll(10, TimeUnit.SECONDS);
            assertNotNull(tagged, "function should have rendered the log context");
            assertEquals(businessCid, tagged.get("cid"), "the business correlation-id must win");
            // 2. a legacy pre-4.10.2 peer's transported header is honored
            po.send(new EventEnvelope().setTo(route).setBody("x").setCorrelationId(internalCid)
                    .setHeader("my_correlation_id", legacyCid));
            Map<String, Object> legacy = bench.poll(10, TimeUnit.SECONDS);
            assertNotNull(legacy, "function should have rendered the log context");
            assertEquals(legacyCid, legacy.get("cid"), "a legacy peer's business cid must be honored");
            // 3. no business context: the key is OMITTED - never the internal correlation-id
            po.send(new EventEnvelope().setTo(route).setBody("x").setCorrelationId(internalCid));
            Map<String, Object> unset = bench.poll(10, TimeUnit.SECONDS);
            assertNotNull(unset, "function should have rendered the log context");
            assertFalse(unset.containsKey("cid"),
                    "an unresolved business correlation-id must be omitted, not substituted: " + unset);
        } finally {
            platform.release(route);
            LogContextConfig.setInstanceForTest(null);
        }
    }

    @Test
    void updateContextRejectsReservedKeys() {
        PostOffice po = PostOffice.trackable("unit.test", "t-1", "GET /x");
        for (String reserved : LogContext.RESERVED_KEYS) {
            assertThrows(IllegalArgumentException.class, () -> po.updateContext(reserved, "value"),
                    "expected rejection for reserved key: " + reserved);
        }
        // a non-reserved key on a non-traced request is a silent no-op (no context registered)
        assertDoesNotThrow(() -> po.updateContext("orderId", "ord-1"));
    }
}
