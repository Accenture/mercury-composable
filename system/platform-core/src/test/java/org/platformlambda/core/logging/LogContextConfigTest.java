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
import org.platformlambda.core.models.TraceInfo;
import org.platformlambda.core.util.ConfigReader;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogContextConfigTest extends TestBase {

    private LogContextConfig configFrom(Map<String, Object> contextSection) {
        Map<String, Object> root = new HashMap<>();
        root.put("context", contextSection);
        return new LogContextConfig(new ConfigReader().load(root));
    }

    @Test
    void builtInDefaultProvidesFullTraceContext() {
        // the default template shipped in platform-core's main/resources
        LogContextConfig config = new LogContextConfig(new ConfigReader("classpath:/default-log-context.yaml"));
        assertTrue(config.isEnabled());

        TraceInfo trace = new TraceInfo("my.func", "trace-1", "GET /api/x", "parent-span-1");
        Map<String, Object> out = config.render(new LogContext(trace, "cid-9"), 1_751_252_588_000L);
        assertEquals("cid-9", out.get("cid"));
        assertEquals("trace-1", out.get("traceId"));
        assertEquals("GET /api/x", out.get("tracePath"));
        assertEquals(trace.spanId, out.get("spanId"));
        assertEquals("parent-span-1", out.get("parentSpanId"));
        assertEquals("my.func", out.get("service"));
        assertNotNull(out.get("timestamp"));
    }

    @Test
    void enabledByDefaultWhenAppFileAbsent() {
        // this module's test classpath has no app-log-context.yaml, so a reload
        // falls back to the built-in default and the feature is on out of the box
        LogContextConfig.setInstanceForTest(null);
        assertTrue(LogContextConfig.getInstance().isEnabled());
    }

    @Test
    void optOutFlagDisablesFeature() {
        System.setProperty("app.log.context", "false");
        try {
            LogContextConfig.setInstanceForTest(null);
            assertFalse(LogContextConfig.getInstance().isEnabled());
        } finally {
            System.clearProperty("app.log.context");
            LogContextConfig.setInstanceForTest(null);
        }
    }

    @Test
    void disabledWhenReaderIsNull() {
        LogContextConfig config = new LogContextConfig(null);
        assertFalse(config.isEnabled());
    }

    @Test
    void disabledWhenNoContextSection() {
        LogContextConfig config = new LogContextConfig(new ConfigReader().load(Map.of("other", "value")));
        assertFalse(config.isEnabled());
    }

    @Test
    void loadsTemplateFromYamlFile() {
        // exercises the real YAML parse + ${ENV:default} substitution from a classpath file
        LogContextConfig config = new LogContextConfig(new ConfigReader("classpath:/test-log-context.yaml"));
        assertTrue(config.isEnabled());

        TraceInfo trace = new TraceInfo("my.interesting.function", "trace-abc", "GET /api/order", null);
        LogContext ctx = new LogContext(trace, "cid-123");
        ctx.put("orderId", "123-45678");
        Map<String, Object> out = config.render(ctx, 1_751_252_588_000L);

        assertEquals("cid-123", out.get("cid"));
        assertEquals("trace-abc", out.get("traceId"));
        assertEquals("GET /api/order", out.get("tracePath"));
        assertEquals("my.interesting.function", out.get("service"));
        assertEquals(trace.spanId, out.get("spanId"));
        assertNotNull(out.get("timestamp"));            // $utc resolves to a UTC ISO-8601 string
        assertEquals("dev", out.get("environment"));    // ${ENV_NAME:dev} with ENV_NAME unset -> default
        assertEquals("123-45678", out.get("orderId"));  // developer custom key
        // root span -> no parentSpanId -> key omitted (never "null")
        assertFalse(out.containsKey("parentSpanId"));
    }

    @Test
    void omitsNullValuedKeys() {
        Map<String, Object> section = new HashMap<>();
        section.put("cid", "$cid");
        section.put("traceId", "$traceId");
        section.put("parentSpanId", "$parentSpanId");
        section.put("service", "$service");
        LogContextConfig config = configFrom(section);

        // no cid, root span (null parentSpanId)
        TraceInfo trace = new TraceInfo("my.func", "t-1", "GET /x", null);
        LogContext ctx = new LogContext(trace, null);
        Map<String, Object> out = config.render(ctx, System.currentTimeMillis());

        assertFalse(out.containsKey("cid"));          // null cid omitted
        assertFalse(out.containsKey("parentSpanId")); // null parentSpanId omitted
        assertEquals("t-1", out.get("traceId"));
        assertEquals("my.func", out.get("service"));
    }

    @Test
    void resolvesParentSpanIdWhenPresent() {
        LogContextConfig config = configFrom(Map.of("parentSpanId", "$parentSpanId"));
        TraceInfo child = new TraceInfo("child.func", "t-1", "GET /x", "parent-span-99");
        Map<String, Object> out = config.render(new LogContext(child, "c"), System.currentTimeMillis());
        assertEquals("parent-span-99", out.get("parentSpanId"));
    }

    @Test
    void rejectsUnknownToken() {
        Map<String, Object> badSection = Map.of("bad", "$notAValidToken");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> configFrom(badSection));
        assertTrue(ex.getMessage().contains("notAValidToken"));
    }

    @Test
    void treatsLiteralAndEnvValuesAsConstants() {
        Map<String, Object> section = new HashMap<>();
        section.put("fixed", "literal-value");
        section.put("environment", "${ENV_NAME:staging}");
        LogContextConfig config = configFrom(section);

        Map<String, Object> out = config.render(new LogContext(new TraceInfo("r", "t", "p", null), "c"),
                System.currentTimeMillis());
        assertEquals("literal-value", out.get("fixed"));
        assertEquals("staging", out.get("environment"));
    }
}
