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

package com.accenture.minigraph.progress;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The declared return paths (RFC-0011 section 8.4): a prefix entry ends with {@code *}, any other entry is one route,
 * and an entry that would declare too much, or nothing a route could be, is refused by name.
 */
class ReturnPathsTest {

    @Test
    void aPrefixEntryMatchesWholeSegmentsAndAnExactEntryOneRoute() {
        var paths = new ReturnPaths("order.status.*, progress.relay, kafka.progress.publisher");
        assertTrue(paths.refusals().isEmpty(), String.valueOf(paths.refusals()));
        assertTrue(paths.isDeclared("order.status.kafka"));
        assertTrue(paths.isDeclared("order.status.mail.v2"));
        assertFalse(paths.isDeclared("order.status"), "the prefix itself is not a route in the family");
        assertFalse(paths.isDeclared("order.statuses"), "the dot before the '*' ends the prefix on a segment");
        assertTrue(paths.isDeclared("progress.relay"));
        assertFalse(paths.isDeclared("progress.relay.v2"), "an exact entry declares one route");
        assertTrue(paths.isDeclared("kafka.progress.publisher"));
        assertFalse(paths.isDeclared("audit.store"));
    }

    @Test
    void aRouteThatIsNotARouteNameIsNeverDeclared() {
        var paths = new ReturnPaths("order.status.*");
        assertFalse(paths.isDeclared(null));
        assertFalse(paths.isDeclared("order.status."));
        assertFalse(paths.isDeclared("ORDER.status.kafka"));
        assertFalse(paths.isDeclared("order.status.kafka@another-app"));
    }

    @Test
    void anEntryThatDeclaresTooMuchOrNothingIsRefusedByName() {
        var paths = new ReturnPaths("*, order.*.status, order*, Order.Status, order.status.*, good.route");
        assertEquals(List.of(
                "return.path.routes entry '*' refused - a bare '*' would declare every route",
                "return.path.routes entry 'order.*.status' refused - '*' may only end an entry",
                "return.path.routes entry 'order*' refused - 'order' is not a valid route name",
                "return.path.routes entry 'Order.Status' refused - 'Order.Status' is not a valid route name"),
                paths.refusals());
        assertFalse(paths.isDeclared("order.kafka"), "a refused entry declares nothing");
        assertTrue(paths.isDeclared("order.status.kafka"), "the valid entries still declare their routes");
        assertTrue(paths.isDeclared("good.route"));
    }

    @Test
    void anEmptyListDeclaresNothing() {
        assertFalse(new ReturnPaths("").isDeclared("any.route"));
        assertFalse(new ReturnPaths(null).isDeclared("any.route"));
    }
}
