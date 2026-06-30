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
import org.platformlambda.core.models.TraceInfo;

import static org.junit.jupiter.api.Assertions.*;

class LogContextManagerTest {

    private LogContext newContext() {
        return new LogContext(new TraceInfo("my.func", "t-1", "GET /x", null), "cid-1");
    }

    @Test
    void registerGetRemove() {
        long threadId = Thread.currentThread().threadId();
        assertNull(LogContextManager.get(threadId));
        LogContext ctx = newContext();
        LogContextManager.register(threadId, ctx);
        assertSame(ctx, LogContextManager.get(threadId));
        LogContextManager.remove(threadId);
        assertNull(LogContextManager.get(threadId));
    }

    @Test
    void contextsAreIsolatedByThreadId() {
        long id1 = 100_001L;
        long id2 = 100_002L;
        LogContext c1 = newContext();
        LogContext c2 = newContext();
        try {
            LogContextManager.register(id1, c1);
            LogContextManager.register(id2, c2);
            assertSame(c1, LogContextManager.get(id1));
            assertSame(c2, LogContextManager.get(id2));
            assertNotSame(LogContextManager.get(id1), LogContextManager.get(id2));
        } finally {
            LogContextManager.remove(id1);
            LogContextManager.remove(id2);
        }
    }

    @Test
    void customKeyPutAndRemoveOnNull() {
        LogContext ctx = newContext();
        ctx.put("orderId", "ord-9");
        assertEquals("ord-9", ctx.getCustomKeys().get("orderId"));
        ctx.put("orderId", null);
        assertFalse(ctx.getCustomKeys().containsKey("orderId"));
    }

    @Test
    void reservedKeysAreRecognized() {
        for (String key : LogContext.RESERVED_KEYS) {
            assertTrue(LogContext.isReservedKey(key), key + " should be reserved");
        }
        assertFalse(LogContext.isReservedKey("orderId"));
    }
}
