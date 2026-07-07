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

package org.platformlambda.support;

import io.lettuce.core.RedisURI;
import org.platformlambda.core.util.Utility;
import org.platformlambda.core.util.common.ConfigBase;

import java.time.Duration;

/**
 * Redis connection startup parameters, read from {@code application.properties} as discrete keys
 * (resolved through {@link ConfigBase}, so {@code ${ENV_VAR:default}} substitution applies - keep secrets
 * like {@code redis.password} out of the file via {@code ${REDIS_PASSWORD}}):
 *
 * <pre>
 * redis.host=127.0.0.1
 * redis.port=6379
 * redis.password=${REDIS_PASSWORD:}   # blank = no auth
 * redis.ssl=false
 * redis.database=0
 * redis.timeout.ms=5000               # default command timeout
 * </pre>
 *
 * @param host      Redis host.
 * @param port      Redis port.
 * @param password  auth password; blank/null = no authentication.
 * @param ssl       use TLS ({@code rediss://}).
 * @param database  logical database index.
 * @param timeoutMs default command timeout in milliseconds.
 */
public record RedisConfig(String host, int port, String password, boolean ssl, int database, long timeoutMs) {

    private static final String HOST_KEY = "redis.host";
    private static final String PORT_KEY = "redis.port";
    private static final String PASSWORD_KEY = "redis.password";
    private static final String SSL_KEY = "redis.ssl";
    private static final String DATABASE_KEY = "redis.database";
    private static final String TIMEOUT_MS_KEY = "redis.timeout.ms";

    public static RedisConfig from(ConfigBase config) {
        Utility util = Utility.getInstance();
        return new RedisConfig(
                config.getProperty(HOST_KEY, "127.0.0.1"),
                util.str2int(config.getProperty(PORT_KEY, "6379")),
                config.getProperty(PASSWORD_KEY, ""),
                "true".equalsIgnoreCase(config.getProperty(SSL_KEY, "false")),
                util.str2int(config.getProperty(DATABASE_KEY, "0")),
                util.str2long(config.getProperty(TIMEOUT_MS_KEY, "5000")));
    }

    /** Map the discrete parameters onto a Lettuce {@link RedisURI}. */
    public RedisURI toUri() {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withSsl(ssl)
                .withDatabase(database)
                .withTimeout(Duration.ofMillis(timeoutMs));
        if (password != null && !password.isBlank()) {
            builder.withPassword(password.toCharArray());
        }
        return builder.build();
    }
}
