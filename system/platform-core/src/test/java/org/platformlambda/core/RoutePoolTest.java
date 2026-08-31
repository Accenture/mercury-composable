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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.common.TestBase;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.LambdaFunction;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.system.ServiceDef;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behavior of the route pool registration API (registerRoutePool / releaseRoutePool):
 * ordered private singleton members, symmetric release, house reload semantics on
 * re-registration, and tolerance of individual updates to pool members.
 * See draft-design-specs/register-route-pool.md.
 */
class RoutePoolTest extends TestBase {

    private static final LambdaFunction ECHO = (headers, input, instance) -> input;

    @Test
    void registersOrderedPrivateSingletonMembers() throws InterruptedException, ExecutionException {
        Platform platform = Platform.getInstance();
        List<String> members = platform.registerRoutePool("unit.test.pool.a", ECHO, 3);
        try {
            assertEquals(List.of("unit.test.pool.a.0", "unit.test.pool.a.1", "unit.test.pool.a.2"), members);
            for (String member : members) {
                assertTrue(platform.hasRoute(member), member + " must be registered");
                ServiceDef def = platform.getLocalRoutingTable().get(member);
                assertNotNull(def, member + " must be in the routing table");
                assertTrue(def.isPrivate(), member + " must be private");
                assertEquals(1, def.getConcurrency(), member + " must be a singleton lane");
            }
            // a lane is a normal function - prove liveness with one RPC
            PostOffice po = new PostOffice("unit.test", "100", "TEST /route/pool");
            EventEnvelope response = po.request(new EventEnvelope()
                    .setTo(members.getFirst()).setBody("hello"), 5000).get();
            assertEquals("hello", response.getBody());
        } finally {
            assertTrue(platform.releaseRoutePool("unit.test.pool.a"));
        }
        for (String member : members) {
            assertFalse(platform.hasRoute(member), member + " must be gone after pool release");
        }
    }

    @Test
    void releaseIsSymmetricAndAbsentPoolReturnsFalse() {
        Platform platform = Platform.getInstance();
        assertFalse(platform.releaseRoutePool("unit.test.no.such.pool"));
        platform.registerRoutePool("unit.test.pool.b", ECHO, 2);
        assertTrue(platform.releaseRoutePool("unit.test.pool.b"));
        assertFalse(platform.releaseRoutePool("unit.test.pool.b"));
        assertFalse(platform.hasRoute("unit.test.pool.b.0"));
        assertFalse(platform.hasRoute("unit.test.pool.b.1"));
    }

    @Test
    void reRegistrationReloadsToExactlyTheNewSet() {
        Platform platform = Platform.getInstance();
        platform.registerRoutePool("unit.test.pool.c", ECHO, 5);
        try {
            List<String> members = platform.registerRoutePool("unit.test.pool.c", ECHO, 3);
            assertEquals(3, members.size());
            assertTrue(platform.hasRoute("unit.test.pool.c.2"));
            // the reload must not leave orphans beyond the new count
            assertFalse(platform.hasRoute("unit.test.pool.c.3"));
            assertFalse(platform.hasRoute("unit.test.pool.c.4"));
        } finally {
            assertTrue(platform.releaseRoutePool("unit.test.pool.c"));
        }
    }

    @Test
    void invalidArgumentsAreRejected() {
        Platform platform = Platform.getInstance();
        assertThrows(IllegalArgumentException.class, () ->
                platform.registerRoutePool("unit.test.pool.d", null, 1));
        assertThrows(IllegalArgumentException.class, () ->
                platform.registerRoutePool("unit.test.pool.d", ECHO, 0));
        // non-canonical prefixes are rejected so member names are exactly "{prefix}.{n}"
        assertThrows(IllegalArgumentException.class, () ->
                platform.registerRoutePool("Unit.Test.Pool", ECHO, 1));
        assertFalse(platform.hasRoute("unit.test.pool.d.0"));
    }

    @Test
    void individualUpdatesToMembersAreToleratedAndCleanedUp() {
        Platform platform = Platform.getInstance();
        platform.registerRoutePool("unit.test.pool.e", ECHO, 3);
        // an individual release of a member is warned, never refused (house semantics)
        assertTrue(platform.release("unit.test.pool.e.1"));
        assertFalse(platform.hasRoute("unit.test.pool.e.1"));
        // an individual re-registration over a member reloads it, also warned
        platform.registerPrivate("unit.test.pool.e.2", ECHO, 1);
        assertTrue(platform.hasRoute("unit.test.pool.e.2"));
        // pool release still cleans the remainder, holes included
        assertTrue(platform.releaseRoutePool("unit.test.pool.e"));
        assertFalse(platform.hasRoute("unit.test.pool.e.0"));
        assertFalse(platform.hasRoute("unit.test.pool.e.2"));
    }

    @Test
    void neighborRoutesOutsideThePoolRangeAreUntouched() {
        Platform platform = Platform.getInstance();
        platform.registerPrivate("unit.test.pool.f.10", ECHO, 1);
        platform.registerRoutePool("unit.test.pool.f", ECHO, 3);
        assertTrue(platform.releaseRoutePool("unit.test.pool.f"));
        // "{prefix}.10" is outside the pool's range [0, 3) and is not a member
        assertTrue(platform.hasRoute("unit.test.pool.f.10"));
        assertTrue(platform.release("unit.test.pool.f.10"));
    }
}
