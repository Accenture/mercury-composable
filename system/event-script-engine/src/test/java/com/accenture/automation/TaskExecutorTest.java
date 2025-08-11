package com.accenture.automation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.MultiLevelMap;

import static org.junit.jupiter.api.Assertions.*;

class TaskExecutorTest {

    private TaskExecutor executor;

    @BeforeEach
    void setup(){
        this.executor = new TaskExecutor();
    }


    @Test
    void shouldReplaceRuntimeVarIfValid() {
        MultiLevelMap source = new MultiLevelMap();
        source.setElement("model.foo", "bar");
        source.setElement("model.replacement", "foo");

        String text = "model.{model.replacement} -> baz";
        String expected = "model.foo -> baz";

        String result = executor.substituteRuntimeVarsIfAny(text, source);

        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void shouldSafelyIgnoreRuntimeVarIfNotInModelNamespace() {
        MultiLevelMap source = new MultiLevelMap();
        source.setElement("safe", "bar");

        String text = "model.{safe} -> baz";

        String result = executor.substituteRuntimeVarsIfAny(text, source);

        assertNotNull(result);
        assertEquals(text, result);
    }

    @Test
    void shouldSafelyIgnoreRuntimeVarIfMalformed() {
        MultiLevelMap source = new MultiLevelMap();
        source.setElement("model.foo", "bar");
        source.setElement("model.replacement", "foo");

        String text = "model.{model.replacement.} -> baz";

        String result = executor.substituteRuntimeVarsIfAny(text, source);

        assertNotNull(result);
        assertEquals(text, result);
    }
}