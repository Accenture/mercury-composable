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

package com.accenture.postgres.tests;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import com.accenture.postgres.models.TempTestData;
import org.junit.jupiter.api.AfterAll;
import org.platformlambda.postgres.models.HealthCheck;
import org.platformlambda.postgres.support.PgRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.system.AutoStart;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.system.PostOffice;
import org.platformlambda.core.util.AppConfigReader;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class ReactiveDbTest {
    private static final Logger log = LoggerFactory.getLogger(ReactiveDbTest.class);
    private static final Utility util = Utility.getInstance();
    private static final String ASYNC_HTTP_CLIENT = "async.http.request";
    private static final String DEMO_FUNCTION = "v1.db.demo";
    private static final String TYPE = "type";
    private static final String READ_BY_REPO = "read_by_repo";
    private static final String READ_BY_CLIENT = "read_by_client";
    private static final String INSERT = "insert";
    private static final String UPDATE = "update";
    private static final String ID = "id";
    private static final String SQL_READ = "SELECT * FROM health_check WHERE id = $1";
    private static final String SQL_INSERT = "INSERT INTO health_check " +
                                                "(id, app_name, app_instance, created, updated)" +
                                                " VALUES ($1, $2, $3, $4, $5)";
    private static final String SQL_UPDATE = "UPDATE health_check SET updated = $1 WHERE id = $2";
    private static final String SQL_DELETE = "DELETE FROM health_check WHERE id = $1";
    private static final long TIMEOUT = 5000;

    private static EmbeddedPostgres embeddedPostgres;

    @BeforeAll
    static void setup() throws IOException {
        var config = AppConfigReader.getInstance();
        int port = util.str2int(config.getProperty("postgres.port", "5432"));
        EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder();
        builder.setPort(port);
        embeddedPostgres = builder.start();
        AutoStart.main(new String[0]);
    }

    @AfterAll
    static void teardown() throws IOException {
        embeddedPostgres.close();
    }

    /**
     * This unit test demonstrates making HTTP calls to the HttpTestEndpoint service.
     * Please review the HttpTestEndpoint class that illustrates the use of the repository pattern
     * and the direct DatabaseClient API calls.
     *
     * @throws ExecutionException in case of error
     * @throws InterruptedException in case of error
     */
    @Test
    void doDatabaseReadWriteViaRestEndpoint() throws ExecutionException, InterruptedException {
        var request = new AsyncHttpRequest();
        request.setMethod("GET").setTargetHost("http://127.0.0.1:"+getPort());
        request.setUrl("/api/tests").setHeader("accept", "application/json");
        // create a post office and set traceId and tracePath
        // use this approach for Unit Tests only
        var po = new PostOffice("unit.test", "101", "TEST /restendpoint");
        var result = po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request.toMap()), 5000).get();
        assertInstanceOf(List.class, result.getBody());
        var records = result.getBodyAsListOfPoJo(TempTestData.class);
        assertFalse(records.isEmpty());
        for (TempTestData d: records) {
            log.info("TempTestData - {}", d);
        }
        request.setUrl("api/tests/001");
        result = po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request.toMap()), 5000).get();
        assertInstanceOf(List.class, result.getBody());
        records = result.getBodyAsListOfPoJo(TempTestData.class);
        assertEquals(1, records.size());
        var rec = records.getFirst();
        assertEquals("001", rec.id);
        assertEquals("Mary", rec.name);
        assertEquals("100 World Blvd", rec.address);
        // since the table is using TIMESTAMP. It is using default time zone
        // for comparison, we should use LocalDateTime
        assertEquals(util.str2LocalDateTime("2024-12-22 10:10:30"),
                LocalDateTime.ofInstant(rec.created.toInstant(), ZoneId.systemDefault()));
        // insert a new record
        var data = new TempTestData().create("A1", "John", "10 New York Blvd");
        request.setMethod("POST").setUrl("/api/tests").setBody(data).setHeader("content-type", "application/json");
        result = po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request.toMap()), 5000).get();
        assertInstanceOf(Map.class, result.getBody());
        assertEquals(Map.of("row_updated", 1), result.getBody());
        // immediately read the newly created record
        var request2 = new AsyncHttpRequest().setMethod("GET").setTargetHost("http://127.0.0.1:"+getPort())
                                .setUrl("api/tests/A1").setHeader("accept", "application/json");
        var result2 = po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request2.toMap()), 5000).get();
        assertInstanceOf(List.class, result2.getBody());
        var records2 = result2.getBodyAsListOfPoJo(TempTestData.class);
        assertEquals(1, records2.size());
        var rec2 = records2.getFirst();
        assertEquals(data.id, rec2.id);
        assertEquals(data.name, rec2.name);
        assertEquals(data.address, rec2.address);
        assertEquals(data.created, rec2.created);
    }

    /**
     * This unit test demonstrates making PostOffice RPC calls to a function that uses the repository pattern
     * and direct DatabaseClient API calls.
     *
     * @throws ExecutionException in case of error
     * @throws InterruptedException in case of error
     */
    @Test
    void doDatabaseReadWriteViaRPC() throws ExecutionException, InterruptedException {
        var po = new PostOffice("unit.test", "202", "TEST /direct-call");
        // tell demo function to read record using repository pattern
        var request = new EventEnvelope().setTo(DEMO_FUNCTION).setHeader(TYPE, READ_BY_REPO).setHeader(ID, "001");
        var result = po.request(request, 5000).get();
        assertInstanceOf(Map.class, result.getBody());
        // the original class name is also transported by the RPC call
        assertEquals(TempTestData.class.getName(), result.getType());
        // therefore we can just restore it
        result.restoreBodyAsPoJo();
        // now the event body becomes a PoJo
        assertInstanceOf(TempTestData.class, result.getBody());
        var rec = (TempTestData) result.getBody();
        assertEquals("001", rec.id);
        assertEquals("Mary", rec.name);
        assertEquals("100 World Blvd", rec.address);
        assertEquals(util.str2LocalDateTime("2024-12-22 10:10:30"),
                LocalDateTime.ofInstant(rec.created.toInstant(), ZoneId.systemDefault()));
        // insert a new record
        var data = new TempTestData().create("B20", "David", "San Francisco Airport Blvd");
        var request2 = new EventEnvelope().setTo(DEMO_FUNCTION).setHeader(TYPE, INSERT).setBody(data);
        var result2 = po.request(request2, 5000).get();
        assertInstanceOf(Map.class, result2.getBody());
        assertEquals(Map.of("row_updated", 1), result2.getBody());
        // read the new record. Ask demo function to read record using database client API
        var request3 = new EventEnvelope().setTo(DEMO_FUNCTION).setHeader(TYPE, READ_BY_CLIENT).setHeader(ID, "B20");
        var result3 = po.request(request3, 5000).get();
        result3.restoreBodyAsPoJo();
        assertInstanceOf(TempTestData.class, result3.getBody());
        var rec3 = (TempTestData) result3.getBody();
        assertEquals(data.id, rec3.id);
        assertEquals(data.name, rec3.name);
        assertEquals(data.address, rec3.address);
        assertEquals(data.created, rec3.created);
        // do an update
        data.address = "500 World Blvd";
        data.created = new Date();
        var request4 = new EventEnvelope().setTo(DEMO_FUNCTION).setHeader(TYPE, UPDATE).setBody(data);
        var result4 = po.request(request4, 5000).get();
        // compare the updated record
        result4.restoreBodyAsPoJo();
        assertInstanceOf(TempTestData.class, result4.getBody());
        var rec4 = (TempTestData) result4.getBody();
        assertEquals(data.id, rec4.id);
        assertEquals(data.name, rec4.name);
        assertEquals(data.address, rec4.address);
        assertEquals(data.created, rec4.created);
    }

    /**
     * This unit test demonstrates hitting the /health actuator endpoint that invokes the "postgres.health"
     * service. You can review the PgHealth class to see how it uses the PgRequest helper class to do database
     * queries and updates.
     *
     * @throws ExecutionException in case of error
     * @throws InterruptedException in case of error
     */
    @SuppressWarnings("unchecked")
    @Test
    void healthTest() throws ExecutionException, InterruptedException {
        var platform = Platform.getInstance();
        var request = new AsyncHttpRequest();
        request.setMethod("GET").setTargetHost("http://127.0.0.1:"+getPort());
        request.setUrl("/health").setHeader("accept", "application/json");
        var po = new PostOffice("unit.test", "103", "TEST /health");
        var result = po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request.toMap()), 5000).get();
        assertInstanceOf(Map.class, result.getBody());
        var mm = new MultiLevelMap((Map<String, Object>) result.getBody());
        assertEquals("UP", mm.getElement("status"));
        assertEquals(platform.getOrigin(), mm.getElement("origin"));
        assertEquals(platform.getName(), mm.getElement("name"));
        var message = mm.getElement("dependency[0].message");
        assertInstanceOf(String.class, message);
        assertTrue(String.valueOf(message).contains("Write"));
        // a new health record should have been created
        var request2 = new AsyncHttpRequest();
        request2.setMethod("GET").setTargetHost("http://127.0.0.1:"+getPort());
        request2.setUrl("/api/tests").setHeader("accept", "application/json").setHeader("x-health-check", "true");
        var result2 = po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request2.toMap()), 5000).get();
        assertInstanceOf(List.class, result2.getBody());
        var id = platform.getOrigin();
        var records2 = result2.getBodyAsListOfPoJo(HealthCheck.class);
        assertFalse(records2.isEmpty());
        Set<String> healthIds = new HashSet<>();
        for (HealthCheck d: records2) {
            healthIds.add(d.id);
        }
        assertTrue(healthIds.contains(id));
        // retrieve the record by ID
        var request3 = new AsyncHttpRequest();
        request3.setMethod("GET").setTargetHost("http://127.0.0.1:"+getPort());
        request3.setUrl("/api/tests/{id}").setPathParameter("id", id)
                .setHeader("accept", "application/json").setHeader("x-health-check", "true");
        var result3 = po.request(new EventEnvelope().setTo(ASYNC_HTTP_CLIENT).setBody(request3.toMap()), 5000).get();
        assertInstanceOf(List.class, result3.getBody());
        var records3 = result3.getBodyAsListOfPoJo(HealthCheck.class);
        assertEquals(1, records3.size());
        var rec = records3.getFirst();
        assertEquals(id, rec.id);
        assertEquals(platform.getName(), rec.appName);
        log.info("Health check record - {}", rec);
    }

    /**
     * This unit test demonstrates the use of the PgRequest helper class to do queries and updates.
     * The PgRequest approach is the easiest way for database access.
     *
     * @throws ExecutionException in case of error
     * @throws InterruptedException in case of error
     */
    @Test
    void crudTest() throws ExecutionException, InterruptedException {
        var id = util.getUuid();
        var name = "unit test";
        var instance = "unit test instance";
        var now = new Date();
        var timestamp = new Timestamp(now.getTime());
        // Except for unit test where the PostOffice is hardcoded.
        // You should always instantiate a new instance of a PostOffice using "var po = new PostOffice(headers, instance)
        var po = PostOffice.trackable("unit.test", "200", "TEST /crud");
        var sql = new PgRequest(TIMEOUT);
        var count = sql.update(po, SQL_INSERT, id, name, instance, timestamp, timestamp);
        assertEquals(1, count);
        var records = sql.query(po, SQL_READ, id);
        assertEquals(1, records.size());
        var mapper = SimpleMapper.getInstance().getMapper();
        var rec = mapper.readValue(records.getFirst(), HealthCheck.class);
        assertEquals(id, rec.id);
        assertEquals(name, rec.appName);
        assertEquals(instance, rec.appInstance);
        assertEquals(now, rec.created);
        var minusOneMinute = now.getTime() - 60000;
        var revisedTimestamp = new Timestamp(minusOneMinute);
        var updated = sql.update(po, SQL_UPDATE, revisedTimestamp, id);
        assertEquals(1, updated);
        // read the updated record to validate the updated timestamp
        records = sql.query(po, SQL_READ, id);
        assertEquals(1, records.size());
        rec = mapper.readValue(records.getFirst(), HealthCheck.class);
        assertEquals(now, rec.created);
        assertEquals(new Date(minusOneMinute), rec.updated);
        // finally delete the record
        var deleted = sql.update(po, SQL_DELETE, id);
        assertEquals(1, deleted);
        // confirm the record has been deleted
        records = sql.query(po, SQL_READ, id);
        assertEquals(0, records.size());
    }

    private int getPort() {
        AppConfigReader config = AppConfigReader.getInstance();
        return util.str2int(config.getProperty("rest.server.port", "8080"));
    }
}
