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

package org.platformlambda.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityTest {

    @Test
    void sleepBlocksForRoughlyTheRequestedTime() {
        Utility util = Utility.getInstance();
        long start = System.currentTimeMillis();
        util.sleep(60);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 40, "sleep should block for roughly the requested duration, was " + elapsed);
    }

    @Test
    void sleepReturnsImmediatelyForNonPositiveDuration() {
        Utility util = Utility.getInstance();
        long start = System.currentTimeMillis();
        util.sleep(0);
        util.sleep(-10);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 40, "a zero or negative sleep should return immediately, was " + elapsed);
    }
}
