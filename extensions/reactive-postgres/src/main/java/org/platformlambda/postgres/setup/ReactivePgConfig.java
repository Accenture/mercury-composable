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

package org.platformlambda.postgres.setup;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.client.SSLMode;
import io.r2dbc.spi.ConnectionFactory;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Configuration
@EnableR2dbcRepositories(basePackages = {"org.platformlambda", "${postgres.repository.scan}"})
public class ReactivePgConfig extends AbstractR2dbcConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ReactivePgConfig.class);

    private ConnectionFactory pool;

    @Bean
    @Override
    public ConnectionFactory connectionFactory() {
        Utility util = Utility.getInstance();
        AppConfigReader config = AppConfigReader.getInstance();
        String db = config.getProperty("postgres.database", "postgres");
        String host = config.getProperty("postgres.host");
        if (host == null) {
            throw new IllegalArgumentException("Missing postgres.host");
        }
        if (config.getProperty("postgres.port") == null) {
            throw new IllegalArgumentException("Missing postgres.port");
        }
        int dot = host.indexOf('.');
        if (dot == -1) {
            throw new IllegalArgumentException("Invalid postgres.host - "+host);
        }
        int port = util.str2int(config.getProperty("postgres.port"));
        String user = config.getProperty("postgres.user");
        String password = config.getProperty("postgres.password");
        if (user == null || password == null) {
            throw new IllegalArgumentException("Missing postgres.user or postgres.password");
        }
        boolean ssl = "true".equals(config.getProperty("postgres.ssl", "true"));
        var builder = PostgresqlConnectionConfiguration.builder().host(host).port(port).database(db)
                                                        .username(user).password(password);
        if (ssl) {
            builder.sslMode(SSLMode.REQUIRE);
        }
        PostgresqlConnectionConfiguration pgConfig = builder.build();
        var factory = new PostgresqlConnectionFactory(pgConfig);
        int maxSize = Math.max(5, util.str2int(config.getProperty("postgres.connection.pool", "20")));
        int initialSize = Math.max(1, maxSize / 4);
        initialSize = Math.min(initialSize, 8);
        ConnectionPoolConfiguration poolConfig = ConnectionPoolConfiguration.builder(factory)
                .initialSize(initialSize)
                .maxSize(maxSize)
                .maxIdleTime(Duration.ofMinutes(30))
                .maxAcquireTime(Duration.ofSeconds(8))
                .build();
        this.pool = new ConnectionPool(poolConfig);
        log.info("Starting PostGreSQL connection pool (initial {}, max {})", initialSize, maxSize);
        // Spring R2DBC will establish database connections according to initial size in the pool
        return this.pool;
    }

    @Override
    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        // Spring R2DBC guarantees that factory has been initialized at this point
        R2dbcDialect dialect = getDialect(this.pool);
        // Enable JSR-310
        var converters = Jsr310Converters.getConvertersToRegister();
        // OffsetDateTime is a database data type. Add custom converters here.
        converters.add(new OffsetDateTimeReadConverter());
        converters.add(new OffsetDateTimeWriteConverter());
        log.info("Total {} r2dbc data type converters loaded", converters.size());
        return R2dbcCustomConversions.of(dialect, converters);
    }

    @Bean
    public ConnectionFactoryInitializer initialize() {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        // Spring R2DBC guarantees that factory has been initialized at this point
        initializer.setConnectionFactory(this.pool);
        // tell initializer to use "pg-schema.sql" to ensure the health check table is created
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("pg-schema.sql"));
        initializer.setDatabasePopulator(populator);
        log.info("Running initialization script at classpath:/pg-schema.sql");
        return initializer;
    }

    @ReadingConverter
    private static class OffsetDateTimeReadConverter implements Converter<Date, OffsetDateTime> {
        @Override
        public OffsetDateTime convert(Date date) {
            return date.toInstant().atOffset(ZoneOffset.UTC);
        }
    }

    @WritingConverter
    private static class OffsetDateTimeWriteConverter implements Converter<OffsetDateTime, Date> {
        @Override
        public Date convert(OffsetDateTime dateTime) {
            return Date.from(dateTime.toInstant());
        }
    }
}
