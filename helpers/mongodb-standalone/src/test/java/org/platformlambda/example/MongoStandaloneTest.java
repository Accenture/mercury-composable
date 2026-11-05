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

package org.platformlambda.example;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test of the standalone MongoDB helper as a developer runs it: {@link MainApp#start} starts the server
 * ({@code mongodb.port=0} in the test configuration binds a free port), and the official MongoDB driver writes and
 * reads through it, including the conditional upsert that the scheduler example's state resolver relies on.
 */
class MongoStandaloneTest {

    private static MainApp app;
    private static MongoClient client;

    @BeforeAll
    static void start() {
        app = new MainApp();
        app.start(new String[0]);
        client = MongoClients.create("mongodb://127.0.0.1:" + app.mongo.getPort());
    }

    @AfterAll
    static void stop() {
        if (client != null) {
            client.close();
        }
        if (app != null && app.mongo != null) {
            app.mongo.stop();
        }
    }

    @Test
    void insertAndFindRoundTrip() {
        var greetings = client.getDatabase("demo").getCollection("greetings");
        greetings.insertOne(new Document("_id", "hello").append("text", "world"));
        var found = greetings.find(Filters.eq("_id", "hello")).first();
        assertNotNull(found);
        assertEquals("world", found.getString("text"));
    }

    @Test
    void aConditionalUpsertThatMissesMeetsTheUniqueId() {
        var states = client.getDatabase("demo").getCollection("job_state");
        var now = new Date();
        states.insertOne(new Document("_id", "demo-task").append("start", now).append("owner", "a"));
        // the filter misses (the record is newer than the cutoff and owned by another), so the upsert inserts and
        // MongoDB refuses a second document with the same _id: the refusal is the answer
        var filter = Filters.and(Filters.eq("_id", "demo-task"), Filters.or(
                Filters.lt("start", new Date(now.getTime() - 30_000)), Filters.eq("owner", "b")));
        var update = Updates.combine(Updates.set("start", now), Updates.set("owner", "b"));
        var options = new UpdateOptions().upsert(true);
        var refused = assertThrows(MongoWriteException.class, () -> states.updateOne(filter, update, options));
        assertEquals(ErrorCategory.DUPLICATE_KEY, refused.getError().getCategory());
        var owner = states.find(Filters.eq("_id", "demo-task")).first();
        assertNotNull(owner);
        assertEquals("a", owner.getString("owner"));
    }
}
