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

import com.accenture.db2.models.Db2QueryStatement;
import org.platformlambda.postgres.models.PgQueryStatement;
import org.platformlambda.postgres.models.PgUpdateStatement;
import org.junit.jupiter.api.Test;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatementTest {

    @Test
    void convertNamedParametersPostGreSQL() {
        var sql = "select * from users where id = :id and name = :name";
        var statement = new PgQueryStatement(sql);
        statement.bindParameter("id", 100);
        statement.bindParameter("name", "John");
        statement.convertNamedParamsToIndex();
        assertEquals(sql.replace(":id", "$1").replace(":name", "$2"),
                        statement.getStatement());
        assertTrue(statement.getNamedParams().isEmpty());
        assertFalse(statement.getParameters().isEmpty());
        assertEquals(100, statement.getOriginalParameter(0));
        assertEquals("John", statement.getOriginalParameter(1));
        assertEquals(2, statement.getClassMapping().size());
    }

    @Test
    void convertNamedParametersDb2() {
        var sql = "select * from users where id = :id and name = :name and code = :id";
        var statement = new Db2QueryStatement(sql);
        statement.bindParameter("id", 100);
        statement.bindParameter("name", "John");
        statement.convertNamedParamsToIndex();
        assertEquals(sql.replace(":id", "?").replace(":name", "?"),
                        statement.getStatement());
        assertTrue(statement.getNamedParams().isEmpty());
        assertFalse(statement.getParameters().isEmpty());
        // prove that repeated parameter can be mapped correctly
        assertEquals(100, statement.getOriginalParameter(1));
        assertEquals("John", statement.getOriginalParameter(2));
        assertEquals(100, statement.getOriginalParameter(3));
        assertEquals(3, statement.getClassMapping().size());
    }

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
        query.bindParameter(0, hello);
        // the bindParameters method must be the last one of binding if you have bind individual parameter(s) earlier
        query.bindParameters(world, address, date);
        var ex = assertThrows(IllegalArgumentException.class, () -> query.bindParameters(hello));
        assertEquals("Parameters have been bound", ex.getMessage());
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

    @Test
    void numberedIndexParamWithListValueTest() {
        // the case for PostGreSQL
        var sql = "SELECT * FROM test WHERE name = $2 and seq IN ($1)";
        var query = new PgQueryStatement(sql);
        query.bindParameters(List.of(1, 2, 3), "hello");
        // position parameters for SQL statement is converted to named parameters and list values are pre-processed
        assertEquals("SELECT * FROM test WHERE name = :p2 and seq IN (1, 2, 3)", query.getStatement());
        assertEquals(Map.of ("p2", "hello"), query.getNamedParams());
    }

    @Test
    void positionIndexParamWithListValueTest() {
        // the case for DB2
        var sql = "SELECT * FROM test WHERE name = ? and seq IN (?)";
        var query = new Db2QueryStatement(sql);
        query.bindParameters("hello", List.of(1, 2, 3));
        // position parameters for SQL statement is first converted to named parameters for list value pre-processing
        // and then converted back to position parameters because DB2 does not support named parameters
        assertEquals("SELECT * FROM test WHERE name = ? and seq IN (1, 2, 3)", query.getStatement());
        assertEquals(Map.of(1, "hello"), query.getParameters());
    }
}
