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

package com.accenture.postgres.services;

import com.accenture.postgres.models.TempTestData;
import com.accenture.postgres.repository.TempRepo;
import org.platformlambda.postgres.support.RowParser;
import io.r2dbc.spi.Row;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.Map;

@PreLoad(route = "v1.db.demo", instances = 20)
public class DbDemoFunction implements TypedLambdaFunction<TempTestData, Object> {
    private static final String SQL_INSERT = "INSERT INTO temp_unit_test_table (id, name, address, created)" +
                                                " VALUES (:id, :name, :address, :created)";
    private static final String SQL_READ = "SELECT * FROM TEMP_UNIT_TEST_TABLE WHERE id = :id";
    private static final String TYPE = "type";
    private static final String READ_BY_REPO = "read_by_repo";
    private static final String READ_BY_CLIENT = "read_by_client";
    private static final String INSERT = "insert";
    private static final String UPDATE = "update";
    private static final String ID = "id";

    private TempRepo repo;
    private DatabaseClient client;

    @Autowired
    public void setTempRepo(TempRepo repo) {
        this.repo = repo;
    }

    @Autowired
    public void setDatabaseClient(DatabaseClient client) {
        this.client = client;
    }

    @Override
    public Object handleEvent(Map<String, String> headers, TempTestData input, int instance) {
        var type = headers.get(TYPE);
        if (READ_BY_REPO.equals(type) && headers.containsKey(ID)) {
            var id = headers.get(ID);
            // Convert a Flux streaming object into a single Mono for easy consumption by the caller.
            // It is because the method will return 0 or 1 record. The "next()" method picks the first item.
            // You can also use the collectList() method to return the complete list of records if there
            // are more than one.
            return repo.getRecordById(id).next();
        }
        // demonstrate using database client API
        if (READ_BY_CLIENT.equals(type) && headers.containsKey(ID)) {
            var id = headers.get(ID);
            return client.sql(SQL_READ).bind("id", id).map(r -> {
                if (r instanceof Row row) {
                    return RowParser.toPoJo(row, TempTestData.class);
                } else {
                    // this should never happen
                    throw new IllegalArgumentException("database record object is not a Row");
                }
            }).first();
        }
        if (INSERT.equals(type) && input != null) {
            if (input.id != null && input.name != null && input.address != null && input.created != null) {
                // demonstrate use of reactive database client
                return client.sql(SQL_INSERT)
                        .bind("id", input.id)
                        .bind("name", input.name)
                        .bind("address", input.address)
                        .bind("created", input.created)
                        .fetch().rowsUpdated().map(count -> Map.of("row_updated", count));
            }
        }
        if (UPDATE.equals(type) && input != null) {
            // IMPORTANT - the repo's default "save()" method cannot be used to insert record.
            // It can only update an existing record with the same ID.
            return repo.save(input);
        }
        throw new IllegalArgumentException("Invalid request");
    }
}
