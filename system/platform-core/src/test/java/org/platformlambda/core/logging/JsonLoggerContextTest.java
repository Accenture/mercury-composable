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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.models.TraceInfo;
import org.platformlambda.core.util.ConfigReader;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonLoggerContextTest extends TestBase {

    private final long threadId = Thread.currentThread().threadId();

    // a concrete JsonLogger so we can exercise the shared getJson() used by JsonAppender/CompactAppender
    private static final JsonLogger LOGGER = new JsonLogger("test", null, null, true, null) {
        @Override
        public void append(LogEvent event) {
            // not needed - we test getJson() directly
        }
    };

    @AfterEach
    void cleanup() {
        LogContextManager.remove(threadId);
        LogContextConfig.setInstanceForTest(null);   // next getInstance() reloads (feature absent -> disabled)
    }

    private void enableFeature() {
        Map<String, Object> section = new HashMap<>();
        section.put("service", "$service");
        section.put("traceId", "$traceId");
        Map<String, Object> root = new HashMap<>();
        root.put("context", section);
        LogContextConfig.setInstanceForTest(new LogContextConfig(new ConfigReader().load(root)));
    }

    private LogEvent infoEvent() {
        return Log4jLogEvent.newBuilder()
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("hello"))
                .setTimeMillis(1_751_252_588_000L)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void addsContextWhenEnabledAndContextPresent() {
        enableFeature();
        LogContextManager.register(threadId, new LogContext(new TraceInfo("my.func", "t-77", "GET /x", null), "c-1"));

        Map<String, Object> json = LOGGER.getJson(infoEvent());

        assertTrue(json.containsKey("context"));
        Map<String, Object> context = (Map<String, Object>) json.get("context");
        assertEquals("my.func", context.get("service"));
        assertEquals("t-77", context.get("traceId"));
        // base fields still present
        assertEquals("hello", json.get("message"));
        assertNotNull(json.get("time"));
    }

    @Test
    void omitsContextWhenNoContextForThread() {
        enableFeature();
        // no LogContext registered for this thread (e.g. framework boot log or async tail)
        Map<String, Object> json = LOGGER.getJson(infoEvent());
        assertFalse(json.containsKey("context"));
    }

    @Test
    void omitsContextWhenFeatureDisabled() {
        LogContextConfig.setInstanceForTest(new LogContextConfig(null));   // disabled
        LogContextManager.register(threadId, new LogContext(new TraceInfo("my.func", "t-1", "GET /x", null), "c"));
        Map<String, Object> json = LOGGER.getJson(infoEvent());
        assertFalse(json.containsKey("context"));
    }
}
