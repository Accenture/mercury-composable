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
 * The back-pressure overflow buffer's storage strategy: a per-route two-tier FIFO (memory head, then
 * transient disk spill). {@link ElasticQueue} is a thin facade that delegates to one implementation,
 * selected by the {@code elastic.queue.store} config:
 * <ul>
 *   <li>{@code bdb} (default) — {@link BdbElasticStore}, backed by Berkeley DB JE (the long-standing impl);</li>
 *   <li>{@code file} — {@link FileElasticStore}, a dependency-free per-route segmented append FIFO.</li>
 * </ul>
 * Both are transient (not durable across restart), FIFO, and single-threaded per route (each route's
 * Vert.x consumer is one event-loop thread). See draft-design-specs/elastic_queue_file_fifo_design.md.
 */
interface ElasticStore extends AutoCloseable {

    /** First this many events are held in memory before spilling to the disk tier. */
    int MEMORY_BUFFER = 20;

    String getId();

    long getReadCounter();

    long getWriteCounter();

    void write(byte[] event);

    byte[] peek();

    byte[] read();

    /** Narrows {@link AutoCloseable#close()} to not throw a checked exception. */
    @Override
    void close();

    void destroy();

    boolean isClosed();
}
