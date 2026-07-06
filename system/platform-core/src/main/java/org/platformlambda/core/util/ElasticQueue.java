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
 * <p>This is a thin facade over an {@link ElasticStore} strategy selected by the {@code elastic.queue.store}
 * config (default {@code file} = {@link FileElasticStore}, the portable virtual-thread-friendly FIFO;
 * {@code bdb} = {@link BdbElasticStore}, the legacy Berkeley DB fallback). The public API is unchanged, so
 * ServiceQueue and applications are unaffected by the choice of store. See
 * draft-design-specs/elastic_queue_file_fifo_design.md.</p>
 */
public class ElasticQueue implements AutoCloseable {

    public static final int MEMORY_BUFFER = ElasticStore.MEMORY_BUFFER;
    private static final String STORE_CONFIG = "elastic.queue.store";
    private static final String FILE = "file";
    private static final String BDB = "bdb";

    private final ElasticStore store;

    /**
     * @param id service route path
     */
    public ElasticQueue(String id) {
        this.store = create(id);
    }

    private static ElasticStore create(String id) {
        String type = AppConfigReader.getInstance().getProperty(STORE_CONFIG, FILE);
        // default (and any unrecognized value) uses the file store; bdb is the explicit legacy fallback
        return BDB.equalsIgnoreCase(type) ? new BdbElasticStore(id) : new FileElasticStore(id);
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

    /**
     * @return true if the selected store is safe to drive from a per-route virtual thread (the {@code file}
     *         store), false if it must run inline on the event loop (the {@code bdb} store, which pins
     *         virtual-thread carriers). ServiceQueue uses this to pick its dispatch mode, so the store
     *         choice alone determines the (store, dispatch) pairing.
     */
    public boolean supportsVirtualThreadDispatch() {
        return store.supportsVirtualThreadDispatch();
    }
}
