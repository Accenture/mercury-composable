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
import org.platformlambda.postgres.repository.HealthCheckRepo;
import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;

import java.util.Map;

@PreLoad(route = "v1.rest.tests", instances = 20)
public class HttpTestEndpoint implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    private static final String SQL_INSERT = "INSERT INTO temp_unit_test_table (id, name, address, created)" +
                                                " VALUES (:id, :name, :address, :created)";
    private HealthCheckRepo healthRepo;
    private TempRepo repo;
    private DatabaseClient client;

    @Autowired
    public void setTempRepo(TempRepo repo) {
        this.repo = repo;
    }

    @Autowired
    public void setHealthRepo(HealthCheckRepo healthRepo) {
        this.healthRepo = healthRepo;
    }

    @Autowired
    public void setDatabaseClient(DatabaseClient client) {
        this.client = client;
    }

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        if ("GET".equals(input.getMethod())) {
            var id = input.getPathParameter("id");
            // demonstrate use of repository pattern for two repositories
            if ("true".equals(input.getHeader("x-health-check"))) {
                return id == null? healthRepo.findAll() : healthRepo.getRecordById(id);
            } else {
                return id == null? repo.findAll() : repo.getRecordById(id);
            }
        } else if ("POST".equals(input.getMethod())) {
            var data = input.getBody(TempTestData.class);
            if (data.id != null && data.name != null && data.address != null && data.created != null) {
                // demonstrate use of reactive database client
                return client.sql(SQL_INSERT)
                        .bind("id", data.id)
                        .bind("name", data.name)
                        .bind("address", data.address)
                        .bind("created", data.created)
                        .fetch().rowsUpdated().map(count -> Map.of("row_updated", count));
            }
        }
        throw new IllegalArgumentException("Invalid request");
    }
}
