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

import org.junit.jupiter.api.Test;
import org.platformlambda.core.util.Utility;
import org.platformlambda.postgres.models.PgQueryStatement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pure unit tests (no database) for {@link org.platformlambda.db.SqlPreparedStatement}'s parameter-type
 * machinery: value data-type detection, string decode/restore for every supported type, null-type to
 * JDBC-class / SQL-type mapping, and the fail-fast error paths.
 */
class SqlStatementTypesTest {

    private static final Utility util = Utility.getInstance();

    @Test
    void detectsDataTypeForEveryValueKind() {
        var stmt = new PgQueryStatement("SELECT 1");
        stmt.bindParameter("s", "text");
        stmt.bindParameter("bool", true);
        stmt.bindParameter("by", (byte) 1);
        stmt.bindParameter("sh", (short) 2);
        stmt.bindParameter("fl", 1.5f);
        stmt.bindParameter("db", 2.5d);
        stmt.bindParameter("in", 3);
        stmt.bindParameter("lo", 4L);
        stmt.bindParameter("bi", new BigInteger("5"));
        stmt.bindParameter("bd", new BigDecimal("6.5"));
        stmt.bindParameter("ts", new Timestamp(0));
        stmt.bindParameter("sd", java.sql.Date.valueOf("2026-01-02"));
        stmt.bindParameter("st", java.sql.Time.valueOf("03:04:05"));
        stmt.bindParameter("ld", LocalDate.of(2026, 1, 2));
        stmt.bindParameter("lt", LocalTime.of(3, 4, 5));
        stmt.bindParameter("ldt", LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        stmt.bindParameter("odt", OffsetDateTime.of(2026, 1, 2, 3, 4, 5, 0, ZoneOffset.UTC));
        stmt.bindParameter("ud", new Date());
        stmt.bindParameter("bytes", "x".getBytes());
        var m = stmt.getClassMapping();
        assertEquals("string", m.get("s"));
        assertEquals("boolean", m.get("bool"));
        assertEquals("number", m.get("by"));
        assertEquals("number", m.get("sh"));
        assertEquals("number", m.get("fl"));
        assertEquals("number", m.get("db"));
        assertEquals("number", m.get("in"));
        assertEquals("number", m.get("lo"));
        assertEquals("big-integer", m.get("bi"));
        assertEquals("big-decimal", m.get("bd"));
        assertEquals("timestamp", m.get("ts"));
        assertEquals("sql-date", m.get("sd"));
        assertEquals("sql-time", m.get("st"));
        assertEquals("local-date", m.get("ld"));
        assertEquals("local-time", m.get("lt"));
        assertEquals("local-datetime", m.get("ldt"));
        assertEquals("offset-datetime", m.get("odt"));
        assertEquals("date", m.get("ud"));
        assertEquals("bytes", m.get("bytes"));
    }

    /** Reconstruct the transported (string-encoded) form of a value using its recorded data type. */
    private Object restore(Object encoded, String dataType) {
        var stmt = new PgQueryStatement("SELECT 1");
        var named = new HashMap<String, Object>();
        named.put("v", encoded);
        stmt.setNamedParams(named);
        stmt.getClassMapping().put("v", dataType);
        return stmt.getOriginalParameter("v");
    }

    @Test
    void restoresEveryEncodedType() {
        assertEquals("text", restore("text", "string"));
        assertArrayEquals("hi".getBytes(), (byte[]) restore(util.bytesToBase64("hi".getBytes()), "bytes"));
        assertEquals(new BigInteger("123"), restore("123", "big-integer"));
        assertEquals(new BigDecimal("1.5"), restore("1.5", "big-decimal"));
        assertInstanceOf(Timestamp.class, restore("2026-01-02 03:04:05", "timestamp"));
        assertInstanceOf(java.sql.Date.class, restore("2026-01-02", "sql-date"));
        assertInstanceOf(java.sql.Time.class, restore("03:04:05", "sql-time"));
        assertInstanceOf(LocalDate.class, restore("2026-01-02", "local-date"));
        assertInstanceOf(LocalTime.class, restore("03:04:05", "local-time"));
        assertInstanceOf(LocalDateTime.class, restore("2026-01-02T03:04:05", "local-datetime"));
        assertInstanceOf(OffsetDateTime.class, restore("2026-01-02T03:04:05Z", "offset-datetime"));
        assertInstanceOf(Date.class, restore(util.date2str(new Date()), "date"));
        // an unknown data type falls through and returns the string unchanged
        assertEquals("as-is", restore("as-is", "mystery"));
    }

    @Test
    void restoresBoundBytesAndPassesThroughNonStrings() {
        var stmt = new PgQueryStatement("SELECT 1 WHERE b = :b AND n = :n");
        stmt.bindParameter("b", "hello".getBytes());
        stmt.bindParameter("n", 42);
        // byte[] is base64-encoded on bind and decoded on restore
        assertArrayEquals("hello".getBytes(), (byte[]) stmt.getOriginalParameter("b"));
        // a non-string value is returned verbatim (no decode)
        assertEquals(42, stmt.getOriginalParameter("n"));
    }

    private void assertNullType(Class<?> clazz, Class<?> expectedClass, int expectedSqlType) {
        var stmt = new PgQueryStatement("SELECT 1 WHERE x = :v");
        stmt.bindNullParameter("v", clazz);
        assertEquals(expectedClass, stmt.getNullClass("v"));
        assertEquals(expectedSqlType, stmt.getNullSqlType("v"));
    }

    @Test
    void mapsEveryNullTypeToClassAndSqlType() {
        assertNullType(String.class, String.class, Types.VARCHAR);
        assertNullType(Boolean.class, Boolean.class, Types.BOOLEAN);
        assertNullType(byte[].class, byte[].class, Types.BLOB);
        assertNullType(BigInteger.class, BigInteger.class, Types.BIGINT);
        assertNullType(BigDecimal.class, BigDecimal.class, Types.DECIMAL);
        assertNullType(Timestamp.class, Timestamp.class, Types.TIMESTAMP);
        assertNullType(java.sql.Date.class, java.sql.Date.class, Types.DATE);
        assertNullType(java.sql.Time.class, java.sql.Time.class, Types.TIME);
        assertNullType(LocalDate.class, LocalDate.class, Types.DATE);
        assertNullType(LocalTime.class, LocalTime.class, Types.TIME);
        assertNullType(LocalDateTime.class, LocalDateTime.class, Types.TIMESTAMP);
        assertNullType(OffsetDateTime.class, OffsetDateTime.class, Types.TIMESTAMP_WITH_TIMEZONE);
        assertNullType(Date.class, Date.class, Types.TIMESTAMP_WITH_TIMEZONE);
        assertNullType(Byte.class, Byte.class, Types.SMALLINT);
        assertNullType(Short.class, Short.class, Types.SMALLINT);
        assertNullType(Float.class, Float.class, Types.FLOAT);
        assertNullType(Double.class, Double.class, Types.DOUBLE);
        assertNullType(Integer.class, Integer.class, Types.INTEGER);
        assertNullType(Long.class, Long.class, Types.BIGINT);
    }

    @Test
    void nullParameterByIndexAndImplicitNull() {
        var stmt = new PgQueryStatement("SELECT 1 WHERE x = ?");
        stmt.bindNullParameter(1, Integer.class);
        assertEquals(Integer.class, stmt.getNullClass(1));
        assertEquals(Types.INTEGER, stmt.getNullSqlType(1));
        // binding a null value falls back to a String null
        var stmt2 = new PgQueryStatement("SELECT 1 WHERE x = :v");
        stmt2.bindParameter("v", null);
        assertEquals(String.class, stmt2.getNullClass("v"));
        var stmt3 = new PgQueryStatement("SELECT 1 WHERE x = ?");
        stmt3.bindParameter(1, null);
        assertEquals(String.class, stmt3.getNullClass(1));
    }

    @Test
    void rejectsUnsupportedAndUnboundTypes() {
        var stmt = new PgQueryStatement("SELECT 1 WHERE x = :v");
        assertThrows(IllegalArgumentException.class, () -> stmt.bindParameter("v", 'c'));
        var stmt2 = new PgQueryStatement("SELECT 1 WHERE x = :v");
        assertThrows(IllegalArgumentException.class, () -> stmt2.bindNullParameter("v", Character.class));
        var stmt3 = new PgQueryStatement("SELECT 1");
        assertThrows(IllegalArgumentException.class, () -> stmt3.getNullClass("missing"));
        var stmt4 = new PgQueryStatement("SELECT 1");
        assertThrows(IllegalArgumentException.class, () -> stmt4.getNullSqlType("missing"));
    }

    @Test
    void rejectsMalformedListBindings() {
        // statement lacks $2 for the second position -> transform fails fast
        var q1 = new PgQueryStatement("SELECT * FROM t WHERE a = $1");
        var listAndScalar = List.of(1, 2);
        assertThrows(IllegalArgumentException.class, () -> q1.bindParameters(listAndScalar, "x"));
        // a list whose elements mix numbers and strings is rejected
        var q2 = new PgQueryStatement("SELECT * FROM t WHERE a IN ($1)");
        var mixedTypeList = List.of(1, "x");
        assertThrows(IllegalArgumentException.class, () -> q2.bindParameters(mixedTypeList));
        // negative start index is rejected
        var q3 = new PgQueryStatement("SELECT 1");
        assertThrows(IllegalArgumentException.class, () -> q3.bindParametersFrom(-1, "a"));
        // re-binding after parameters are already bound is rejected
        var q4 = new PgQueryStatement("SELECT 1 WHERE a = $1");
        q4.bindParameters("first");
        assertThrows(IllegalArgumentException.class, () -> q4.bindParameters("second"));
    }
}
