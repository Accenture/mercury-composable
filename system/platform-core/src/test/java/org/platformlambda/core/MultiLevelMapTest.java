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
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiLevelMapTest {

    @SuppressWarnings("unchecked")
    @Test
    void readWriteTest() {
        Utility util = Utility.getInstance();
        var mm = getSampleMultiLevelMap();
        // retrieve the elements
        var one = mm.getElement("a.b.c[0]");
        assertEquals(1, one);
        var xy = mm.getElement("x.y");
        assertInstanceOf(HashMap.class, xy);
        assertEquals(100, ((Map<String, Object>) xy).get("z"));
        assertEquals("world", mm.getElement("hello"));
        // flatten it into single level using dot-bracket composite key format
        var flat = util.getFlatMap(mm.getMap());
        assertEquals(100, flat.get("x.y.z"));
        assertEquals(1, flat.get("a.b.c[0]"));
        assertEquals(3, flat.get("a.b.c[2]"));
        assertNull(flat.get("a.b.c[4]"));
        assertTrue(mm.keyExists("a.b.c[4]"));
        assertNull(mm.getElement("a.b.c[0][invalid format"));
        assertFalse(mm.keyExists("a.b.c[0][invalid format"));
        // add 2 key-values to a.b.c[4]
        mm.setElement("a.b.c[4].test1", "message1");
        mm.setElement("a.b.c[4].test2", "message2");
        // a.b.c is automatically expanded when the 4 array element is added, therefore 3rd element is null.
        assertNull(mm.getElement("a.b.c[3]"));
        assertInstanceOf(HashMap.class, mm.getElement("a.b.c[4]"));
        assertEquals("world", mm.getElement("a.b.c[4].hello"));
        assertEquals("message1", mm.getElement("a.b.c[4].test1"));
        assertEquals("message2", mm.getElement("a.b.c[4].test2"));
        assertTrue(mm.exists("test.boolean"));
        assertEquals(false, mm.getElement("test.boolean"));
        var helloWorldList = List.of(Map.of("hello", "world"), Map.of("hello", "world"));
        mm.setElement("a.b.c[5].array_key", helloWorldList);
        assertEquals("world", mm.getElement("a.b.c[5].array_key[1].hello"));
        jsonPathTest(mm, helloWorldList);
    }

    private void jsonPathTest(MultiLevelMap mm, List<?> helloWorldList) {
        assertEquals(List.of("world", "world"), mm.getElement("$.a.b.c[5].array_key[*].hello"));
        assertEquals(helloWorldList, mm.getElement("$.a.b.c[5].array_key"));
        mm.setElement("a.b.c[6].array_key[]", Map.of("hello", "single_array"));
        var searchResult1 = mm.getElement("$.a.b.c[6].array_key[*].hello");
        assertInstanceOf(List.class, searchResult1);
        assertEquals(List.of("single_array"), searchResult1);
        mm.setElement("a.b.X", 120);
        var searchResult2 = mm.getElement("$.a.b.*");
        assertInstanceOf(List.class, searchResult2);
        var mm2 = new MultiLevelMap(Map.of("result", searchResult2));
        assertEquals(120, mm2.getElement("result[1]"));
        assertEquals(mm.getElement("a.b.c"), mm2.getElement("result[0]"));
    }

    private MultiLevelMap getSampleMultiLevelMap() {
        var base = new HashMap<String, Object>();
        base.put("hello", "world");
        var mm = new MultiLevelMap(base);
        mm.setElement("test.boolean", false);
        mm.setElement("x.y.z", 100);
        var list1 = new ArrayList<Integer>();
        list1.add(1);
        list1.add(2);
        list1.add(3);
        mm.setElement("a.b.c", list1);
        mm.setElement("a.b.c[4].hello", "world");
        return mm;
    }

    @Test
    void multiLevelMapAppendTest() {
        var map = new MultiLevelMap();
        // setElement supports the syntax of "[]" to append a list element
        map.setElement("hello.world[]", "test1");
        map.setElement("hello.world[]", "test2");
        map.setElement("hello.world[]", "test3");
        map.setElement("hello.world[]", "test4");
        map.setElement("hello.world[]", "test5");
        assertTrue(map.keyExists("hello.world[4]"));
        assertFalse(map.keyExists("hello.world[5]"));
        assertEquals("test1", map.getElement("hello.world[0]"));
        assertEquals("test2", map.getElement("hello.world[1]"));
        assertEquals("test3", map.getElement("hello.world[2]"));
        assertEquals("test4", map.getElement("hello.world[3]"));
        assertEquals("test5", map.getElement("hello.world[4]"));
        var ex = assertThrows(IllegalArgumentException.class,
                                        () -> map.setElement("[]hello.world[]", "invalid"));
        assertEquals("Invalid composite path - missing first element", ex.getMessage());
    }
}
