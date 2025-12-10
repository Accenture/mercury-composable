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

package com.accenture.automation;

import com.accenture.services.plugins.arithmetic.AddNumbers;
import io.github.classgraph.ClassInfo;
import org.junit.jupiter.api.Test;
import com.accenture.models.simplePlugin;
import com.accenture.models.PluginFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.SimpleClassScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimplePluginLoaderTest {
    private static final Logger log = LoggerFactory.getLogger(SimplePluginLoaderTest.class);

    private void doWarmup(Consumer<Void> func){
        for(int i=0; i < 2; i++){
            func.accept(null);
//            Object response = numbers.calculate(2,2);
        }
    }

    //TODO: Test out slower machines and different load
    //TODO: Test out in VDI
    @Test
    public void shouldBeSubmillisecondWhenRunningPlugins() {
        AddNumbers numbers = new AddNumbers();

        doWarmup((a) -> numbers.calculate(2, 2));

        long start = System.nanoTime();

        Object response = numbers.calculate(2,2);

        long delta = (System.nanoTime() - start) / 1000; // nano to micro

//        assertEquals(4L, response);

        log.info("Delta: {}", delta);

        // Sub-milliseconds
        assertTrue(delta < 1000, "Should be sub-millisecond response times");
    }
}
