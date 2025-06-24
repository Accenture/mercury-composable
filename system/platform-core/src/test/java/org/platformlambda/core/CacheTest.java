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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.ManagedCache;
import org.platformlambda.core.util.SimpleCache;

import static org.junit.jupiter.api.Assertions.*;

class CacheTest {
    // note that the cache expiry has a minimum value of 1000 ms
    private static final ManagedCache cache1 = ManagedCache.createCache("hello.world", 1000, 100);
    private static final SimpleCache cache2 = SimpleCache.createCache("simple.cache", 500);

    @Test
    void cacheBehavior() throws InterruptedException {
        String key = "key1";
        String data = "hello";
        cache1.put(key, data);
        Object o = cache1.get(key);
        assertEquals(data, o);
        long n = cache1.size();
        assertEquals(1, n);
        // test expiry
        Thread.sleep(1050);
        // cached item will disappear in one second
        Object o2 = cache1.get(key);
        assertNull(o2);
        // test removal
        cache1.put(key, data);
        cache1.remove(key);
        assertFalse(cache1.exists(key));
        cache1.cleanUp();
        cache1.clear();
    }

    /**
     * SimpleCache is reserved for internal use
     * <p>
     * Please DO NOT use it at application level
     */
    @Test
    void simpleCacheTest() throws InterruptedException {
        String key = "key1";
        String data = "hello";
        long expiry = cache2.getExpiry();
        // test minimum expiry to be one second
        assertEquals(1000, expiry);
        cache2.put(key, data);
        Object o = cache2.get(key);
        assertEquals(data, o);
        long n = cache2.size();
        assertEquals(1, n);
        cache2.remove(key);
        Object o2 = cache2.get(key);
        assertNull(o2);
        cache2.put(key, data);
        cache2.remove(key);
        assertFalse(cache2.exists(key));
        cache2.put(key, data);
        assertTrue(cache2.exists(key));
        Thread.sleep(500);
        // since minimum expiry is 1000 ms, the item should still be there
        assertTrue(cache2.exists(key));
        Thread.sleep(600);
        // test expiry timer accuracy
        assertFalse(cache2.exists(key));
        // test clean up
        cache2.cleanUp();
        // test clear cache
        cache2.clear();
    }
}
