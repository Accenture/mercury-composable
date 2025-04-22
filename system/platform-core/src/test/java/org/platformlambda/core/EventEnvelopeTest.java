package org.platformlambda.core;

import org.junit.jupiter.api.Test;
import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.PoJo;
import org.platformlambda.core.util.MultiLevelMap;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class EventEnvelopeTest {
    private static final Logger log = LoggerFactory.getLogger(EventEnvelopeTest.class);
    private static final String SET_COOKIE = "set-cookie";

    @Test
    void cookieTest() {
        EventEnvelope event = new EventEnvelope();
        event.setHeader(SET_COOKIE, "a=100");
        event.setHeader(SET_COOKIE, "b=200");
        // set-cookie is a special case for composite value
        assertEquals("a=100|b=200", event.getHeader(SET_COOKIE));
    }

    @Test
    void headerTest() throws IOException {
        EventEnvelope event = new EventEnvelope();
        event.setHeader("hello", "world");
        event.setHeader("test", "hello\r\nworld");
        EventEnvelope restored = new EventEnvelope(event.toBytes());
        assertEquals("hello world", restored.getHeader("test"));
        assertEquals("world", restored.getHeader("hello"));
    }

    @Test
    void booleanTest() throws IOException {
        boolean HELLO = true;
        EventEnvelope source = new EventEnvelope();
        source.setBody(HELLO);
        byte[] b = source.toBytes();
        EventEnvelope target = new EventEnvelope(b);
        assertEquals(true, target.getRawBody());
        assertEquals(true, target.getBody());
    }

    @Test
    void integerTest() throws IOException {
        int VALUE = 100;
        EventEnvelope source = new EventEnvelope();
        source.setBody(VALUE);
        byte[] b = source.toBytes();
        EventEnvelope target = new EventEnvelope(b);
        assertEquals(VALUE, target.getRawBody());
        assertEquals(VALUE, target.getBody());
    }

    @Test
    void longTest() throws IOException {
        Long VALUE = 100L;
        EventEnvelope source = new EventEnvelope();
        source.setBody(VALUE);
        byte[] b = source.toBytes();
        EventEnvelope target = new EventEnvelope(b);
        // long will be compressed to integer by MsgPack
        assertEquals(VALUE.intValue(), target.getRawBody());
        assertEquals(VALUE.intValue(), target.getBody());
    }

    @Test
    void floatTest() throws IOException {
        float VALUE = 1.23f;
        EventEnvelope source = new EventEnvelope();
        source.setBody(VALUE);
        byte[] b = source.toBytes();
        EventEnvelope target = new EventEnvelope(b);
        assertEquals(VALUE, target.getRawBody());
        assertEquals(VALUE, target.getBody());
    }

    @Test
    void doubleTest() throws IOException {
        double VALUE = 1.23d;
        EventEnvelope source = new EventEnvelope();
        source.setBody(VALUE);
        byte[] b = source.toBytes();
        EventEnvelope target = new EventEnvelope(b);
        assertEquals(VALUE, target.getRawBody());
        assertEquals(VALUE, target.getBody());
    }

    @Test
    void bigDecimalTest() throws IOException {
        String VALUE = "1.23";
        BigDecimal HELLO = new BigDecimal(VALUE);
        EventEnvelope source = new EventEnvelope();
        source.setBody(HELLO);
        byte[] b = source.toBytes();
        EventEnvelope target = new EventEnvelope(b);
        // big decimal is converted to string if it is not encoded in a PoJo
        assertEquals(VALUE, target.getRawBody());
        assertEquals(VALUE, target.getBody());
    }

    @Test
    void dateTest() throws IOException {
        Utility util = Utility.getInstance();
        Date NOW = new Date();
        EventEnvelope source = new EventEnvelope();
        source.setBody(NOW);
        byte[] b = source.toBytes();
        EventEnvelope target = new EventEnvelope(b);
        assertEquals(util.date2str(NOW), target.getRawBody());
        assertEquals(util.date2str(NOW), target.getBody());
    }

    @SuppressWarnings("unchecked")
    @Test
    void pojoTest() throws IOException {
        String HELLO = "hello";
        PoJo pojo = new PoJo();
        pojo.setName(HELLO);
        EventEnvelope source = new EventEnvelope();
        source.setBody(pojo);
        byte[] b = source.toBytes();
        EventEnvelope target = new EventEnvelope(b);
        assertInstanceOf(Map.class, target.getRawBody());
        Map<String, Object> map = (Map<String, Object>) target.getRawBody();
        assertEquals(HELLO, map.get("name"));
        PoJo restored = target.getBody(PoJo.class);
        assertEquals(HELLO, restored.getName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void pojoListTest() throws IOException {
        String HELLO = "hello";
        PoJo pojo = new PoJo();
        pojo.setName(HELLO);
        List<PoJo> list = Collections.singletonList(pojo);
        EventEnvelope source = new EventEnvelope();
        source.setBody(list);
        byte[] b = source.toBytes();
        EventEnvelope target = new EventEnvelope(b);
        // when transporting list of PoJo, the PoJo class is saved as "type"
        assertEquals(PoJo.class.getName(), target.getType());
        assertInstanceOf(List.class, target.getBody());
        List<Object> restored = (List<Object>) target.getBody();
        Map<String, Object> map = new HashMap<>();
        map.put("list", restored);
        MultiLevelMap multi = new MultiLevelMap(map);
        assertEquals(HELLO, multi.getElement("list[0].name"));
        assertInstanceOf(List.class, target.getBody());
        List<PoJo> output = target.getBodyAsListOfPoJo(PoJo.class);
        assertEquals(1, output.size());
        PoJo restoredPoJo = output.getFirst();
        assertEquals(HELLO, restoredPoJo.getName());
    }

    @Test
    void taggingTest() {
        final String HELLO = "hello";
        final String WORLD = "world";
        final String ROUTING = "routing";
        final String DATA = "a->b";
        final String TAG_WITH_NO_VALUE = "tag-with-no-value";
        EventEnvelope event = new EventEnvelope();
        event.addTag(TAG_WITH_NO_VALUE).addTag(HELLO, WORLD).addTag(ROUTING, DATA);
        // When a tag is created with no value, the system will set it to "true"
        assertEquals("true", event.getTag(TAG_WITH_NO_VALUE));
        assertEquals(WORLD, event.getTag(HELLO));
        assertEquals(DATA, event.getTag(ROUTING));
        event.removeTag(HELLO).removeTag(ROUTING);
        assertNull(event.getTag(HELLO));
        assertNull(event.getTag(ROUTING));
        event.removeTag(TAG_WITH_NO_VALUE);
        assertTrue(event.getTags().isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void mapSerializationTest() throws IOException {
        String HELLO = "hello";
        PoJo pojo = new PoJo();
        pojo.setName(HELLO);
        pojo.setNumber(10);
        pojo.setLongNumber(Long.MAX_VALUE);
        EventEnvelope source = new EventEnvelope();
        source.setException(new IllegalArgumentException("hello"));
        // setException will put "hello" as body and setBody will override it with a PoJo in this test
        source.setBody(pojo);
        source.setFrom("unit.test");
        source.setTo("hello.world");
        source.setReplyTo("my.callback");
        source.setTrace("101", "PUT /api/unit/test");
        // use JSON instead of binary serialization
        source.setBinary(false).setBroadcastLevel(1);
        source.setCorrelationId("121");
        source.addTag("x", "y");
        source.setHeader("a", "b");
        source.setExecutionTime(1.23f);
        source.setRoundTrip(2.0f);
        EventEnvelope target = new EventEnvelope(source.toBytes());
        assertEquals(source.getStackTrace(), target.getStackTrace());
        // stack trace to Map
        Map<String, Object> stackList = Utility.getInstance().stackTraceToMap(target.getStackTrace());
        assertTrue(stackList.containsKey("stack"));
        assertInstanceOf(List.class, stackList.get("stack"));
        List<String> list = (List<String>) stackList.get("stack");
        assertTrue(list.getFirst().contains("IllegalArgumentException"));
        assertTrue(list.get(1).startsWith("at"));
        MultiLevelMap map = new MultiLevelMap(target.toMap());
        assertEquals(HELLO, map.getElement("body.name"));
        // when event envelope is serialized, it will become very compact
        assertEquals(10, map.getElement("body.number"));
        assertEquals(Long.MAX_VALUE, map.getElement("body.long_number"));
        assertEquals("y", target.getTag("x"));
        assertEquals(1.23f, target.getExecutionTime(), 0f);
        assertEquals(2.0f, target.getRoundTrip(), 0f);
        assertEquals("121", target.getCorrelationId());
        assertEquals(400, target.getStatus());
        assertEquals(1, target.getBroadcastLevel());
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getFrom(), target.getFrom());
        assertEquals(source.getReplyTo(), target.getReplyTo());
        assertEquals("101", target.getTraceId());
        assertEquals("PUT /api/unit/test", target.getTracePath());
        assertEquals("b", map.getElement("headers.a"));
        assertFalse(target.isBinary());
        // when it is not binary encoding (default), it should contain the tag "json"
        assertEquals("true", target.getTag("json"));
        PoJo output = target.getBody(PoJo.class);
        assertEquals(HELLO, output.getName());
        assertEquals(HELLO, target.getException().getMessage());
        assertEquals(IllegalArgumentException.class, target.getException().getClass());
        assertInstanceOf(byte[].class, map.getElement("exception"));
        byte[] b = (byte[]) map.getElement("exception");
        log.info("Stack trace = {}", target.getStackTrace().length());
        log.info("Serialized exception size = {}", b.length);
        log.info("Serialized event envelope size = {}", target.toBytes().length);
    }

    @SuppressWarnings("rawtypes")
    @Test
    void numberInMapTest() throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("float", 12.345f);
        map.put("double", 10.101d);
        EventEnvelope source = new EventEnvelope().setBody(map);
        EventEnvelope target = new EventEnvelope(source.toBytes());
        assertInstanceOf(Map.class, target.getBody());
        if (target.getBody() instanceof Map m) {
            assertInstanceOf(Float.class, m.get("float"));
            assertInstanceOf(Double.class, m.get("double"));
        }
    }

    @Test
    void optionalTransportTest() {
        EventEnvelope source = new EventEnvelope();
        source.setBody(Optional.of("hello"));
        EventEnvelope target = new EventEnvelope(source.toMap());
        assertEquals(Optional.of("hello"), target.getBody());
    }

    @Test void exceptionTransportTest() throws IOException {
        AppException ex = new AppException(400, "hello world");
        EventEnvelope source = new EventEnvelope().setException(ex);
        byte[] b = source.toBytes();
        EventEnvelope target = new EventEnvelope(b);
        assertTrue(target.isException());
        assertEquals(ex.getMessage(), target.getError());
        Throwable restored = target.getException();
        assertInstanceOf(AppException.class, restored);
        assertEquals(ex.getMessage(), restored.getMessage());
        assertEquals(400, target.getStatus());
    }

    @Test void stackTraceTransportTest() throws IOException {
        String stack = "hello\nworld";
        // transport by byte array
        EventEnvelope source = new EventEnvelope().setStackTrace(stack);
        byte[] b = source.toBytes();
        EventEnvelope target1 = new EventEnvelope(b);
        assertTrue(target1.isException());
        assertEquals(stack, target1.getStackTrace());
        // transport by map
        Map<String, Object> map = source.toMap();
        EventEnvelope target2 = new EventEnvelope();
        target2.fromMap(map);
        assertTrue(target2.isException());
        assertEquals(stack, target2.getStackTrace());
    }

    @Test
    void testBestEffortToRestoreException() throws IOException {
        String message = "hello world";
        String stack = "hello\nworld";
        // this emulates how Node.js Composable application sets exception
        EventEnvelope source = new EventEnvelope().setStackTrace(stack).setBody(message).setStatus(400);
        EventEnvelope target = new EventEnvelope(source.toBytes());
        assertTrue(target.isException());
        assertEquals(stack, target.getStackTrace());
        Throwable ex = target.getException();
        assertNotNull(ex);
        assertInstanceOf(RuntimeException.class, ex);
        assertEquals(400, target.getStatus());
        assertEquals(message, ex.getMessage());
    }
}
