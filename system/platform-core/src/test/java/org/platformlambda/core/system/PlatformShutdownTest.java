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

package org.platformlambda.core.system;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The platform's lightweight shutdown lifecycle ({@link Platform#onShutdown}). The callback-running logic is
 * exercised directly through the package-private {@code runShutdownHooks} so no real JVM shutdown is needed;
 * the registration guard is checked through the public API.
 */
class PlatformShutdownTest {

    @Test
    void runsEveryHookInReverseRegistrationOrder() {
        // last registered runs first, so a resource opened later is released before the one it depends on
        List<String> order = new ArrayList<>();
        Platform.runShutdownHooks(List.of(
                () -> order.add("first"),
                () -> order.add("second"),
                () -> order.add("third")));
        assertEquals(List.of("third", "second", "first"), order);
    }

    @Test
    void isolatesAFailingHookSoTheRestStillRun() {
        // one throwing callback must not block the others - each is isolated
        AtomicInteger ran = new AtomicInteger();
        Platform.runShutdownHooks(List.of(
                ran::incrementAndGet,
                () -> {
                    throw new IllegalStateException("boom");
                },
                ran::incrementAndGet));
        assertEquals(2, ran.get(), "a throwing hook must not stop the remaining hooks");
    }

    @Test
    void nullCallbackIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> Platform.getInstance().onShutdown(null));
        assertEquals("Shutdown callback cannot be null", error.getMessage());
    }
}
