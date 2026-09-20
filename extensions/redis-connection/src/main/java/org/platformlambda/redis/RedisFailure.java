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

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import org.platformlambda.core.exception.AppException;

import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;

/**
 * Classify a Redis client failure into the status a composable function should reply with, so a caller -
 * or a flow's / graph's exception handler, which passes the status through faithfully - sees the failure
 * for what it is instead of a generic 500:
 * <ul>
 *   <li><b>408</b> - the command timed out ({@link RedisCommandTimeoutException}); the message is Lettuce's
 *       ({@code Command timed out after N second(s)}), the same 408 the platform uses for an RPC timeout.</li>
 *   <li><b>503</b> - Redis is unreachable: a connect failure ({@link RedisConnectionException},
 *       {@link ConnectException}, {@link ClosedChannelException}) or a rejected/closed connection
 *       ({@code Currently not connected}, {@code Connection is closed}); the message is
 *       {@code Redis unavailable - <cause>}, the vocabulary of the {@code redis.health} check.</li>
 *   <li><b>unclassified (null)</b> - the server answered with a command error ({@link RedisCommandExecutionException},
 *       e.g. {@code WRONGTYPE}) or the failure is not Redis-related; the caller rethrows the original and the
 *       platform's default mapping applies.</li>
 * </ul>
 * The classification walks the cause chain, so a wrapper never hides the failure it carries, and an
 * {@link AppException} already on the chain is returned as-is.
 */
public final class RedisFailure {

    public static final int TIMEOUT = 408;
    public static final int UNAVAILABLE = 503;
    private static final String UNAVAILABLE_PREFIX = "Redis unavailable - ";

    private RedisFailure() {}

    /**
     * Classify a failure.
     *
     * @param failure the exception thrown by a Redis client call (possibly wrapped)
     * @return the AppException to throw (408 or 503), or null when the failure is not a timeout or an outage
     */
    public static AppException classify(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof AppException app) {
                return app;
            }
            if (current instanceof RedisCommandTimeoutException) {
                return new AppException(TIMEOUT, message(current));
            }
            if (current instanceof RedisCommandExecutionException) {
                // the server answered - a genuine command error is not an outage
                return null;
            }
            if (current instanceof RedisConnectionException || current instanceof ConnectException
                    || current instanceof ClosedChannelException) {
                return new AppException(UNAVAILABLE, UNAVAILABLE_PREFIX + message(current));
            }
            if (current instanceof RedisException && looksDisconnected(current.getMessage())) {
                return new AppException(UNAVAILABLE, UNAVAILABLE_PREFIX + message(current));
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return null;
    }

    private static boolean looksDisconnected(String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return text.contains("not connected") || text.contains("connection is closed")
                || text.contains("connection closed") || text.contains("connection reset")
                || text.contains("connection refused");
    }

    private static String message(Throwable failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }
}
