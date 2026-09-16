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

/**
 * Reactive back-pressure overflow buffer behind every ServiceQueue: a per-route two-tier FIFO that holds
 * the first {@link #MEMORY_BUFFER} events in memory and spills the overflow to a transient disk store.
 *
 * <p>This is a thin facade over the {@link ElasticStore} strategy, implemented by
 * {@link FileElasticStore} - a portable, dependency-free, virtual-thread-friendly segmented FIFO. The
 * Berkeley DB store it replaced was retired in v4.12.10 after the file store ran clean in the field. See
 * draft-design-specs/elastic_queue_file_fifo_design.md.</p>
 */
public class ElasticQueue implements AutoCloseable {

    public static final int MEMORY_BUFFER = ElasticStore.MEMORY_BUFFER;

    private final ElasticStore store;

    /**
     * @param id service route path
     */
    public ElasticQueue(String id) {
        this.store = new FileElasticStore(id);
    }

    public String getId() {
        return store.getId();
    }

    public long getReadCounter() {
        return store.getReadCounter();
    }

    public long getWriteCounter() {
        return store.getWriteCounter();
    }

    public void write(byte[] event) {
        store.write(event);
    }

    public byte[] peek() {
        return store.peek();
    }

    public byte[] read() {
        return store.read();
    }

    @Override
    public void close() {
        store.close();
    }

    /**
     * This method may be called when the route supported by this elastic queue is no longer in service
     */
    public void destroy() {
        store.destroy();
    }

    public boolean isClosed() {
        return store.isClosed();
    }
}
