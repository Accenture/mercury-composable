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

package org.platformlambda.quartz.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.platformlambda.core.system.Platform;
import org.platformlambda.core.util.AppConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * The example's MongoDB database, shared by the state resolver, the environment store and the admin endpoints.
 * <p>
 * The connection string is the application property {@code mongodb.uri} and the database is {@code mongodb.database}.
 * The client is created when a collection is first used, not when the functions are loaded, and it is closed when
 * the application shuts down.
 * <p>
 * Both collections write with {@link WriteConcern#MAJORITY}, so a failover of the primary cannot lose a claim or
 * an active environment that the database acknowledged.
 */
public final class SchedulerDatabase {
    private static final Logger log = LoggerFactory.getLogger(SchedulerDatabase.class);
    /** the state resolver's records, one per job, with the job's name as the document's _id */
    public static final String JOB_STATE_COLLECTION = "job_state";
    /** the active environment, one document */
    public static final String ENVIRONMENT_COLLECTION = "environment";
    // the scheduler waits 3 seconds for each answer, so an unreachable database fails the question within that
    // time, as an error the scheduler logs, rather than after the driver's default of 30 seconds
    private static final int SERVER_SELECTION_SECONDS = 2;

    private SchedulerDatabase() {
        // static access only
    }

    public static MongoCollection<Document> jobStates() {
        return Holder.DATABASE.getCollection(JOB_STATE_COLLECTION).withWriteConcern(WriteConcern.MAJORITY);
    }

    public static MongoCollection<Document> environment() {
        return Holder.DATABASE.getCollection(ENVIRONMENT_COLLECTION).withWriteConcern(WriteConcern.MAJORITY);
    }

    /**
     * Creates the client at first use; the JVM runs this once, however many threads ask at the same time. The client
     * is a field, not a local variable, because it lives as long as the application, which closes it when it stops: a
     * try-with-resources would close it before the first collection is used.
     */
    private static final class Holder {
        private static final MongoClient CLIENT = newClient();
        private static final MongoDatabase DATABASE = CLIENT.getDatabase(databaseName());

        static {
            Platform.getInstance().onShutdown(CLIENT::close);
        }

        private static MongoClient newClient() {
            var uri = new ConnectionString(AppConfigReader.getInstance()
                    .getProperty("mongodb.uri", "mongodb://127.0.0.1:27017"));
            var settings = MongoClientSettings.builder().applyConnectionString(uri);
            if (uri.getServerSelectionTimeout() == null) {
                settings.applyToClusterSettings(cluster ->
                        cluster.serverSelectionTimeout(SERVER_SELECTION_SECONDS, TimeUnit.SECONDS));
            }
            var name = databaseName();
            // the hosts only: a connection string can carry a password
            log.info("MongoDB database {} at {}", name, uri.getHosts());
            return MongoClients.create(settings.build());
        }

        private static String databaseName() {
            return AppConfigReader.getInstance().getProperty("mongodb.database", "scheduler");
        }
    }
}
