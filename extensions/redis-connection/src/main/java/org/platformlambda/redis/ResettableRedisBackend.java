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

package org.platformlambda.redis;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The shared, multiplexed command connection of both topologies - <b>reset after a command timeout</b>.
 * <p>
 * Lettuce reconnects a dropped connection on its own exponential backoff, capped at 30 seconds. After an
 * outage longer than that cap, a pod could serve up to ~30 s of command timeouts while {@code redis.health}
 * - which probes on a fresh connection - was already green (distributed-cache interop, Finding 4,
 * 2026-09-20). Maintainer ruling (2026-09-21): reset the shared connection when a command times out, so
 * recovery is bounded by the command timeout ({@code redis.timeout.ms}) instead of the reconnect backoff.
 * <p>
 * <b>The rule.</b> A timeout on a connection that is <em>not open</em> (Lettuce is between reconnect
 * attempts) resets it at once. A timeout on an <em>open</em> connection - a slow or hung server - resets it
 * on the {@value #OPEN_CONNECTION_TIMEOUTS_BEFORE_RESET}nd consecutive timeout, so one slow command on a
 * healthy connection does not drop every command in flight; a reply clears the count. A reset closes the old
 * connection (Lettuce stops its own reconnect attempts) and the <em>next</em> command opens a new one. While
 * Redis is still down that connect fails fast - {@code 503 Redis unavailable} from {@link RedisFailure} - a
 * more honest answer than waiting out another timeout; a connect attempt that just failed is not repeated
 * for {@value #CONNECT_RETRY_HOLD_MS} ms, so a burst of callers does not become a connect storm.
 * <p>
 * Consumers capture {@link #commands()} / {@link #async()} once at construction. Both are stable facades that
 * resolve the live connection per call, so a captured {@code RedisClusterCommands} keeps working across
 * resets. The blocking facade sees its own timeouts; a caller that awaits pipelined futures itself (the
 * cache's {@code MPUT}) reports one through {@link #onCommandTimeout()}, and the same rule applies.
 */
abstract class ResettableRedisBackend<V, C extends StatefulConnection<String, V>> implements RedisBackend<V> {
    private static final Logger log = LoggerFactory.getLogger(ResettableRedisBackend.class);
    /** A timeout on an OPEN connection resets it on this many consecutive timeouts. */
    static final int OPEN_CONNECTION_TIMEOUTS_BEFORE_RESET = 2;
    /** A failed connect attempt is not repeated within this window; callers get the recent failure. */
    static final long CONNECT_RETRY_HOLD_MS = 250;
    private static final long CONNECT_RETRY_HOLD_NANOS = TimeUnit.MILLISECONDS.toNanos(CONNECT_RETRY_HOLD_MS);

    /** One connection and its consecutive-timeout count - replaced as a unit on reset. */
    private record Slot<C>(C connection, AtomicInteger timeouts) {}

    private final Supplier<C> opener;
    private final Function<C, RedisClusterCommands<String, V>> syncApi;
    private final Function<C, RedisClusterAsyncCommands<String, V>> asyncApi;
    private final AtomicReference<Slot<C>> slot = new AtomicReference<>();
    private final AtomicInteger resets = new AtomicInteger();
    // a ReentrantLock, not 'synchronized': a virtual thread blocking in a synchronized block pins its carrier
    // on Java 21 (JEP 491 lifts that in JDK 24+), and a connect can block briefly
    private final ReentrantLock lock = new ReentrantLock();
    private final RedisClusterCommands<String, V> sync;
    private final RedisClusterAsyncCommands<String, V> async;
    private final AtomicReference<RuntimeException> lastConnectFailure = new AtomicReference<>();
    private volatile long lastConnectAttempt;
    private volatile boolean closed;

    /**
     * @param opener   opens a new command connection (the first one is opened here, eagerly, so construction
     *                 still fails fast when Redis is unreachable)
     * @param syncApi  the connection's blocking command API
     * @param asyncApi the connection's asynchronous command API
     */
    @SuppressWarnings("unchecked")
    protected ResettableRedisBackend(Supplier<C> opener,
                                     Function<C, RedisClusterCommands<String, V>> syncApi,
                                     Function<C, RedisClusterAsyncCommands<String, V>> asyncApi) {
        this.opener = opener;
        this.syncApi = syncApi;
        this.asyncApi = asyncApi;
        this.slot.set(new Slot<>(opener.get(), new AtomicInteger()));
        ClassLoader loader = getClass().getClassLoader();
        this.sync = (RedisClusterCommands<String, V>) Proxy.newProxyInstance(loader,
                new Class<?>[] {RedisClusterCommands.class},
                (proxy, method, args) -> invoke(proxy, method, args, true));
        this.async = (RedisClusterAsyncCommands<String, V>) Proxy.newProxyInstance(loader,
                new Class<?>[] {RedisClusterAsyncCommands.class},
                (proxy, method, args) -> invoke(proxy, method, args, false));
    }

    @Override
    public RedisClusterCommands<String, V> commands() {
        return sync;
    }

    @Override
    public RedisClusterAsyncCommands<String, V> async() {
        return async;
    }

    @Override
    public void onCommandTimeout() {
        Slot<C> current = slot.get();
        if (current != null) {
            timedOut(current);
        }
    }

    /** @return how many times the shared connection has been reset (diagnostics and tests). */
    public int resets() {
        return resets.get();
    }

    /** @return {@code true} while the shared connection is open (channel active). */
    @SuppressWarnings("resource")   // the connection is long-lived and owned by its slot; reset and close() release it
    public boolean connected() {
        Slot<C> current = slot.get();
        return current != null && current.connection().isOpen();
    }

    @Override
    public void close() {
        closed = true;
        Slot<C> current = slot.getAndSet(null);
        if (current != null) {
            current.connection().close();
        }
    }

    private Object invoke(Object proxy, Method method, Object[] args, boolean blocking) {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "toString" -> (blocking ? "sync" : "async") + " command facade of " + this;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
        Slot<C> current = current();
        Object target = blocking ? syncApi.apply(current.connection()) : asyncApi.apply(current.connection());
        try {
            Object result = method.invoke(target, args);
            if (blocking) {
                // a reply arrived: the connection is answering, so the consecutive-timeout count restarts
                current.timeouts().set(0);
            }
            return result;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to invoke " + method.getName(), e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (blocking && RedisFailure.isTimeout(cause)) {
                timedOut(current);
            }
            throw rethrow(cause);
        }
    }

    /**
     * Rethrow the command's own exception unchanged - the facade is transparent, so a Lettuce timeout reaches
     * the caller as the {@code RedisCommandTimeoutException} it was, never wrapped. (Lettuce's command APIs
     * throw unchecked exceptions only; the type parameter lets the compiler accept the rethrow as such.)
     */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException rethrow(Throwable failure) throws T {
        throw (T) failure;
    }

    /** The live connection - opened on demand after a reset; double-checked under the lock. */
    private Slot<C> current() {
        Slot<C> current = slot.get();
        if (current != null) {
            return current;
        }
        lock.lock();
        try {
            current = slot.get();
            if (current != null) {
                return current;
            }
            if (closed) {
                throw new RedisConnectionException("Redis backend is closed");
            }
            RuntimeException recent = lastConnectFailure.get();
            if (recent != null && System.nanoTime() - lastConnectAttempt < CONNECT_RETRY_HOLD_NANOS) {
                // hold back a connect storm: one attempt per hold window, the others share its outcome
                throw new RedisConnectionException(recent.getMessage(), recent);
            }
            lastConnectAttempt = System.nanoTime();
            try {
                current = new Slot<>(opener.get(), new AtomicInteger());
            } catch (RuntimeException e) {
                lastConnectFailure.set(e);
                throw e;
            }
            lastConnectFailure.set(null);
            slot.set(current);
            log.info("Redis connection re-established after reset #{}", resets.get());
            return current;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("resource")   // the timed-out connection is released right here on reset
    private void timedOut(Slot<C> failed) {
        boolean open = failed.connection().isOpen();
        int count = failed.timeouts().incrementAndGet();
        if (open && count < OPEN_CONNECTION_TIMEOUTS_BEFORE_RESET) {
            log.warn("Redis command timed out on an open connection ({} of {}) - connection kept",
                    count, OPEN_CONNECTION_TIMEOUTS_BEFORE_RESET);
            return;
        }
        // one reset per connection instance, however many callers timed out on it concurrently
        if (slot.compareAndSet(failed, null)) {
            resets.incrementAndGet();
            log.warn("Redis connection reset after a command timeout ({}) - the next command reconnects",
                    open ? count + " consecutive timeouts on an open connection" : "connection not open");
            try {
                failed.connection().close();
            } catch (RuntimeException e) {
                log.debug("Ignorable error while closing the timed-out Redis connection - {}", e.getMessage());
            }
        }
    }
}
