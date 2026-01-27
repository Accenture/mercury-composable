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

import org.platformlambda.postgres.models.PgQueryStatement;
import org.platformlambda.postgres.models.PgUpdateStatement;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StatementTest {

    @SuppressWarnings("unchecked")
    @Test
    void queryStatementTest() {
        var sql = "select * from test where id = $1 and name = $2 and address = $3 and updated = $4";
        var hello = "hello";
        var world = "world";
        var address = "100 NY Blvd";
        var date = Timestamp.valueOf(LocalDateTime.now());
        var mapper = SimpleMapper.getInstance().getMapper();
        var query = new PgQueryStatement();
        query.setStatement(sql);
        query.bindParameters(hello, world, address, date);
        Map<String, Object> map = mapper.readValue(query, Map.class);
        var restored = mapper.readValue(map, PgQueryStatement.class);
        assertEquals(sql, restored.getStatement());
        // PostGreSQL R2DBC SQL statement's parameter index is zero-based
        // i.e. $1 = 0, $2 = 1 and so on
        assertEquals(Map.of(0, hello, 1, world, 2, address, 3, date.toString()), restored.getParameters());
        assertInstanceOf(Timestamp.class, restored.getOriginalParameter(3));
    }

    @SuppressWarnings("unchecked")
    @Test
    void updateStatementTest() {
        var util = Utility.getInstance();
        var sql = "UPDATE test SET updated = :updated WHERE id = :id and data = :data";
        var updated = new Timestamp(System.currentTimeMillis());
        var hello = "hello";
        var mapper = SimpleMapper.getInstance().getMapper();
        var query = new PgUpdateStatement(sql);
        query.bindParameter("updated", updated);
        query.bindParameter("id", hello);
        query.bindParameter("data", util.getUTF(hello));
        Map<String, Object> map = mapper.readValue(query, Map.class);
        var restored = mapper.readValue(map, PgUpdateStatement.class);
        assertEquals(sql, restored.getStatement());
        assertEquals(hello, restored.getNamedParams().get("id"));
        assertEquals(String.valueOf(updated), restored.getNamedParams().get("updated"));
        assertInstanceOf(Timestamp.class, restored.getOriginalParameter("updated"));
        assertInstanceOf(String.class, restored.getNamedParams().get("data"));
        assertEquals(util.bytesToBase64(util.getUTF(hello)), restored.getNamedParams().get("data"));
        assertInstanceOf(byte[].class, restored.getOriginalParameter("data"));
        assertEquals(hello, util.getUTF((byte[]) restored.getOriginalParameter("data")));
    }
}
