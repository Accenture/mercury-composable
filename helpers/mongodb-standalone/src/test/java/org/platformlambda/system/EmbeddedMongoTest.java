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

package org.platformlambda.system;

import org.junit.jupiter.api.Test;

import java.net.BindException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EmbeddedMongo}'s lifecycle, its failure branch included: a port that is already in use is
 * a real bind failure here, and the exit that a standalone server asks for is recorded instead of performed.
 */
class EmbeddedMongoTest {

    @Test
    void stopWithoutStartIsSafe() {
        var mongo = new EmbeddedMongo(0);
        assertEquals(-1, mongo.getPort());
        assertDoesNotThrow(mongo::stop);
    }

    @Test
    void startBindsAFreePortAndStopReleasesIt() {
        var exitCode = new AtomicInteger(Integer.MIN_VALUE);
        var mongo = new EmbeddedMongo(0, exitCode::set);
        mongo.start();
        try {
            assertTrue(mongo.getPort() > 0);
            assertEquals(Integer.MIN_VALUE, exitCode.get(), "a successful start asks for no exit");
        } finally {
            mongo.stop();
        }
        assertEquals(-1, mongo.getPort());
    }

    @Test
    void aPortInUseFailsTheStartAndAsksForAnExit() {
        var first = new EmbeddedMongo(0);
        first.startServer();
        try {
            var port = first.getPort();
            var exitCode = new AtomicInteger(Integer.MIN_VALUE);
            var second = new EmbeddedMongo(port, exitCode::set);
            assertThrows(BindException.class, second::startServer);
            assertEquals(-1, second.getPort());
            second.start();
            assertEquals(-1, exitCode.get());
            assertEquals(-1, second.getPort());
        } finally {
            first.stop();
        }
    }
}
