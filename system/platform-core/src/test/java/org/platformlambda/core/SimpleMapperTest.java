/*

    Copyright 2018-2025 Accenture Technology

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

package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.models.PoJo;
import org.platformlambda.core.models.nested.ChildPoJo;
import org.platformlambda.core.models.nested.ParentPoJo;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.serializers.SimpleObjectMapper;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimpleMapperTest {

    @SuppressWarnings("unchecked")
    @Test
    void nestPoJoTest() {
        // this test validates GSON's behavior of ToNumberPolicy.LONG_OR_DOUBLE
        var child = new ChildPoJo();
        child.number1 = 123L;
        child.number2 = 10.2d;
        child.number3 = 280;
        child.number4 = Utility.getInstance().str2float("620.1");
        var parent = new ParentPoJo();
        parent.children = new ArrayList<>();
        parent.children.add(child);
        Map<String, Object> map = SimpleMapper.getInstance().getMapper().readValue(parent, Map.class);
        MultiLevelMap mm = new MultiLevelMap(map);
        assertInstanceOf(Long.class, mm.getElement("children[0].number1"));
        assertInstanceOf(Double.class, mm.getElement("children[0].number2"));
        assertEquals(child.number1, mm.getElement("children[0].number1"));
        assertEquals(child.number2, mm.getElement("children[0].number2"));
        assertInstanceOf(Long.class, mm.getElement("children[0].number3"));
        assertInstanceOf(Double.class, mm.getElement("children[0].number4"));
        assertEquals((long) child.number3, mm.getElement("children[0].number3"));
        assertEquals(620.1d, mm.getElement("children[0].number4"));
    }

    @Test
    void returnOriginalClassIfSameTargetClass() {
        PoJo pojo = new PoJo();
        pojo.setName("hello");
        pojo.setNumber(123);
        Object o = SimpleMapper.getInstance().getMapper().readValue(pojo, PoJo.class);
        assertEquals(pojo, o);
    }

    @Test
    void primitiveDataTest() {
        final boolean bol = true;
        Object bolString = SimpleMapper.getInstance().getMapper().writeValueAsString(bol);
        assertEquals("true", bolString);
        final int n = 1;
        Object intString = SimpleMapper.getInstance().getMapper().writeValueAsString(n);
        assertEquals("1", intString);
    }

    @SuppressWarnings("unchecked")
    @Test
    void mapperSerializationTest() {
        Utility util = Utility.getInstance();
        SimpleObjectMapper mapper = SimpleMapper.getInstance().getMapper();
        Date now = new Date();
        LocalDateTime localDateTime = LocalDateTime.now();
        String iso8601 = util.date2str(now);
        String iso8601NoTimeZone = localDateTime.toString();
        LocalDate localDate = LocalDate.now();
        LocalTime localTime = LocalTime.now();
        Map<String, Object> map = new HashMap<>();
        map.put("integer", 100);
        map.put("date", now);
        map.put("time", localDateTime);
        map.put("local_date", localDate);
        map.put("local_time", localTime);
        map.put("sql_date", new java.sql.Date(now.getTime()));
        map.put("sql_timestamp", new java.sql.Timestamp(now.getTime()));
        Map<String, Object> converted = mapper.readValue(mapper.writeValueAsString(map), Map.class);
        // verify that java.util.Date, java.sql.Date and java.sql.Timestamp can be serialized to ISO-8601 string format
        assertEquals(iso8601, converted.get("date"));
        // LocalDateTime string will drop the "T" separator
        assertEquals(iso8601NoTimeZone.replace('T', ' '), converted.get("time"));
        assertEquals(localDate.toString(), converted.get("local_date"));
        assertEquals(localTime.toString(), converted.get("local_time"));
        // sql date is yyyy-mm-dd
        assertEquals(new java.sql.Date(now.getTime()).toString(), converted.get("sql_date"));
        assertEquals(new java.sql.Timestamp(now.getTime()).toString(), converted.get("sql_timestamp"));
        // OK - Integer becomes Long because of GSON's behavior of ToNumberPolicy.LONG_OR_DOUBLE
        assertEquals(Long.class, converted.get("integer").getClass());
        String name = "hello world";
        Map<String, Object> input = new HashMap<>();
        input.put("full_name", name);
        input.put("date", iso8601);
        input.put("local_date_time", iso8601NoTimeZone);
        input.put("local_date", localDate.toString());
        input.put("local_time", localTime.toString());
        input.put("sql_timestamp", new java.sql.Timestamp(now.getTime()));
        input.put("sql_date", new java.sql.Date(now.getTime()));
        input.put("sql_time", new java.sql.Time(now.getTime()));
        PoJo pojo = mapper.readValue(input, PoJo.class);
        // verify that the time is restored correctly
        assertEquals(now.getTime(), pojo.getDate().getTime());
        assertEquals(localDateTime, pojo.getLocalDateTime());
        assertEquals(localDate, pojo.getLocalDate());
        assertEquals(localTime, pojo.getLocalTime());
        // verify that snake case is deserialized correctly
        assertEquals(name, pojo.getFullName());
        // verify input timestamp can be in milliseconds too
        input.put("date", now.getTime());
        pojo = mapper.readValue(input, PoJo.class);
        assertEquals(new Date(now.getTime()).toString(), pojo.getDate().toString());
        assertEquals(new java.sql.Timestamp(now.getTime()).toString(), pojo.getSqlTimestamp().toString());
        assertEquals(new java.sql.Date(now.getTime()).toString(), pojo.getSqlDate().toString());
        assertEquals(new java.sql.Time(now.getTime()).toString(), pojo.getSqlTime().toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    void bigDecimalSerializationTests() {
        SimpleMapper mapper = SimpleMapper.getInstance();
        String NUMBER = "number";
        String ONE  = "0.00000001";
        String ZERO = "0.00000000";
        SimpleNumber one  = new SimpleNumber(ONE);
        SimpleNumber zero = new SimpleNumber(ZERO);
        // verify hash map result
        Map<String, Object> mapOne = mapper.getMapper().readValue(one, Map.class);
        // numeric value is preserved
        assertEquals(ONE, mapOne.get(NUMBER));
        // ensure that ZERO is converted to "0"
        Map<String, Object> mapZero = mapper.getMapper().readValue(zero, Map.class);
        assertEquals("0", mapZero.get(NUMBER));
        // verify PoJo class conversion behavior - this will return the original object because the class is the same
        SimpleNumber numberOne = mapper.getMapper().readValue(one, SimpleNumber.class);
        assertEquals(numberOne.number, one.number);
        // this will pass thru the serializer
        String zeroValue = mapper.getMapper().writeValueAsString(zero);
        SimpleNumber numberZero = mapper.getMapper().readValue(zeroValue, SimpleNumber.class);
        // the original number has the zero number with many zeros after the decimal
        assertEquals("0E-8", zero.number.toString());
        assertEquals(ZERO, zero.number.toPlainString());
        // the converted BigDecimal gets a zero number without zeros after the decimal
        assertEquals("0", numberZero.number.toString());
        // verify map to PoJo serialization behavior
        SimpleNumber number0 = mapper.getMapper().readValue(mapZero, SimpleNumber.class);
        assertTrue(mapper.isZero(number0.number));
        assertTrue(mapper.isZero(zero.number));
        // the two zero objects are different because of precision
        assertNotEquals(number0.number, zero.number);
        SimpleNumber number1 = mapper.getMapper().readValue(mapOne, SimpleNumber.class);
        // non-zero numbers are exactly the same
        assertEquals(number1.number, one.number);
    }

    @Test
    void bigDecimalTests() {
        String ZERO = "0.00000000";
        BigDecimal zero = new BigDecimal("0");
        BigDecimal zeroes = new BigDecimal(ZERO);
        BigDecimal result = zero.multiply(zeroes);
        // precision is preserved after multiplication
        assertEquals(zeroes, result);
        assertEquals(ZERO, result.toPlainString());
        // test zero values
        assertTrue(SimpleMapper.getInstance().isZero(zero));
        assertTrue(SimpleMapper.getInstance().isZero(zeroes));
        assertTrue(SimpleMapper.getInstance().isZero(result));
        assertTrue(SimpleMapper.getInstance().isZero(result.toPlainString()));
        assertTrue(SimpleMapper.getInstance().isZero(result.toString()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void caseMappingTest() {
        SimpleObjectMapper snakeMapper = SimpleMapper.getInstance().getSnakeCaseMapper();
        SimpleObjectMapper camelMapper = SimpleMapper.getInstance().getCamelCaseMapper();
        String NUMBER = "1.234567890";
        CaseDemo sn = new CaseDemo(NUMBER);
        Map<String, Object> snakeMap = snakeMapper.readValue(sn, Map.class);
        assertEquals(NUMBER, snakeMap.get("case_demo"));
        Map<String, Object> camelMap = camelMapper.readValue(sn, Map.class);
        assertEquals(NUMBER, camelMap.get("caseDemo"));
        CaseDemo restoredFromSnake = snakeMapper.readValue(snakeMap, CaseDemo.class);
        assertEquals(NUMBER, restoredFromSnake.caseDemo.toPlainString());
        CaseDemo restoredFromCamel = camelMapper.readValue(camelMap, CaseDemo.class);
        assertEquals(NUMBER, restoredFromCamel.caseDemo.toPlainString());
    }

    private static class SimpleNumber {
        public BigDecimal number;

        public SimpleNumber(String number) {
            this.number = new BigDecimal(number);
        }
    }

    private static class CaseDemo {
        public BigDecimal caseDemo;

        public CaseDemo(String caseDemo) {
            this.caseDemo = new BigDecimal(caseDemo);
        }
    }

}
