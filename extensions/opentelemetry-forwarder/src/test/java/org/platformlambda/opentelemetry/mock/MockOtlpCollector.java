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

package org.platformlambda.opentelemetry.mock;

import org.platformlambda.core.annotations.PreLoad;
import org.platformlambda.core.models.AsyncHttpRequest;
import org.platformlambda.core.models.EventEnvelope;
import org.platformlambda.core.models.TypedLambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A mock OTLP/HTTP collector written the Mercury composable way: a plain function exposed at
 * {@code POST /api/v2/otlp/v1/traces} (and the Splunk path) by {@code rest.yaml}. Boot the test app
 * ({@code MockOtlpAppMain}) from an IDE and watch real exporters (or {@code curl}) hit the endpoint.
 * <p>
 * The HTTP body the OpenTelemetry SDK sends is OTLP <b>protobuf</b> ({@code application/x-protobuf}) -
 * a serialized {@code ExportTraceServiceRequest}. This collector <b>decodes</b> that payload and logs
 * the key span fields (trace/span IDs, name, timing, status, attributes). It also stashes the decoded
 * IDs in {@link #CAPTURED} so a test can assert that what we mapped survived the wire. It replies with
 * an empty HTTP 200 - a valid (zero-field) OTLP {@code ExportTraceServiceResponse}.
 * <p>
 * The transport headers (including {@code content-encoding}) are captured too. Note the REST automation
 * only surfaces an <em>identity</em> (uncompressed) body to the function - a {@code gzip}-encoded request
 * arrives with the {@code Content-Encoding: gzip} header but a {@code null} body - so the decoded
 * {@code wire.*} fields are only populated for uncompressed exports.
 * <p>
 * We decode with a tiny hand-rolled protobuf {@link ProtoReader} rather than the generated
 * {@code io.opentelemetry.proto.*} classes: those drag in the {@code com.google.protobuf} runtime,
 * which was retired from this module for security reasons. The OTLP wire format is stable and we only
 * need a handful of fields, so walking the bytes directly is cheap and dependency-free. Field numbers
 * below come from the OTLP {@code trace.proto}/{@code common.proto}/{@code resource.proto} schemas; the
 * encoding rules are at https://protobuf.dev/programming-guides/encoding/.
 */
@PreLoad(route = "mock.otlp.collector")
public class MockOtlpCollector implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {
    private static final Logger log = LoggerFactory.getLogger(MockOtlpCollector.class);

    // protobuf wire types (the low 3 bits of a field tag)
    private static final int WIRETYPE_VARINT = 0;
    private static final int WIRETYPE_FIXED64 = 1;
    private static final int WIRETYPE_LEN = 2;
    private static final int WIRETYPE_FIXED32 = 5;

    /** Captured requests (transport fields + decoded span fields) for assertions. */
    public static final BlockingQueue<Map<String, Object>> CAPTURED = new LinkedBlockingQueue<>();

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        Map<String, Object> received = new HashMap<>();
        received.put("method", input.getMethod());
        received.put("path", input.getUrl());
        received.put("authorization", input.getHeader("authorization"));
        received.put("content-type", input.getHeader("content-type"));
        received.put("content-encoding", input.getHeader("content-encoding"));

        Object raw = input.getBody();
        byte[] body = (raw instanceof byte[]) ? (byte[]) raw : null;
        decodeAndLog(body, received);

        CAPTURED.add(received);
        return new EventEnvelope().setStatus(200).setHeader("content-type", "application/x-protobuf");
    }

    /**
     * Decode the OTLP protobuf produced by the OpenTelemetry SDK and log it as human-readable
     * key-values. Best-effort: a non-protobuf or empty body is logged and skipped, never thrown.
     */
    private void decodeAndLog(byte[] body, Map<String, Object> received) {
        if (body == null || body.length == 0) {
            log.info("Mock OTLP received {} - empty/non-binary body", received.get("path"));
            return;
        }
        try {
            // ExportTraceServiceRequest { repeated ResourceSpans resource_spans = 1; }
            List<byte[]> resourceSpans = new ArrayList<>();
            ProtoReader r = new ProtoReader(body);
            while (r.hasMore()) {
                int tag = r.readTag();
                if (fieldNumber(tag) == 1 && wireType(tag) == WIRETYPE_LEN) {
                    resourceSpans.add(r.readBytes());
                } else {
                    r.skip(wireType(tag));
                }
            }
            log.info("Mock OTLP received POST {} - {} bytes, {} ResourceSpans",
                    received.get("path"), body.length, resourceSpans.size());
            int spanCount = 0;
            for (byte[] rs : resourceSpans) {
                spanCount += decodeResourceSpans(rs, received);
            }
            received.put("wire.span_count", spanCount);
        } catch (Exception e) {
            log.warn("Mock OTLP received {} - not a parseable OTLP payload: {}", received.get("path"), e.toString());
        }
    }

    /** ResourceSpans {@code { Resource resource = 1; repeated ScopeSpans scope_spans = 2; }} */
    private int decodeResourceSpans(byte[] data, Map<String, Object> received) {
        ProtoReader r = new ProtoReader(data);
        String serviceName = null;
        List<byte[]> scopeSpans = new ArrayList<>();
        while (r.hasMore()) {
            int tag = r.readTag();
            int field = fieldNumber(tag);
            int type = wireType(tag);
            if (field == 1 && type == WIRETYPE_LEN) {
                serviceName = decodeResourceServiceName(r.readBytes());
            } else if (field == 2 && type == WIRETYPE_LEN) {
                scopeSpans.add(r.readBytes());
            } else {
                r.skip(type);
            }
        }
        log.info("  resource: service.name={}", serviceName);
        received.put("wire.service.name", serviceName);
        int spanCount = 0;
        for (byte[] ss : scopeSpans) {
            spanCount += decodeScopeSpans(ss, received);
        }
        return spanCount;
    }

    /** Resource {@code { repeated KeyValue attributes = 1; }} - return the {@code service.name} value. */
    private String decodeResourceServiceName(byte[] data) {
        ProtoReader r = new ProtoReader(data);
        while (r.hasMore()) {
            int tag = r.readTag();
            if (fieldNumber(tag) == 1 && wireType(tag) == WIRETYPE_LEN) {
                KeyValue kv = decodeKeyValue(r.readBytes());
                if ("service.name".equals(kv.key())) {
                    return kv.value();
                }
            } else {
                r.skip(wireType(tag));
            }
        }
        return null;
    }

    /** ScopeSpans {@code { InstrumentationScope scope = 1; repeated Span spans = 2; }} */
    private int decodeScopeSpans(byte[] data, Map<String, Object> received) {
        ProtoReader r = new ProtoReader(data);
        int spanCount = 0;
        while (r.hasMore()) {
            int tag = r.readTag();
            if (fieldNumber(tag) == 2 && wireType(tag) == WIRETYPE_LEN) {
                decodeSpan(r.readBytes(), received);
                spanCount++;
            } else {
                r.skip(wireType(tag));
            }
        }
        return spanCount;
    }

    /**
     * Span {@code { bytes trace_id=1; bytes span_id=2; bytes parent_span_id=4; string name=5;
     * SpanKind kind=6; fixed64 start_time_unix_nano=7; fixed64 end_time_unix_nano=8;
     * repeated KeyValue attributes=9; Status status=15; }}
     */
    private void decodeSpan(byte[] data, Map<String, Object> received) {
        ProtoReader r = new ProtoReader(data);
        String traceId = null;
        String spanId = null;
        String parentSpanId = null;
        String name = null;
        long kind = 0;
        long startTime = 0;
        long endTime = 0;
        long statusCode = 0;
        Map<String, String> attrs = new LinkedHashMap<>();
        while (r.hasMore()) {
            int tag = r.readTag();
            int type = wireType(tag);
            switch (fieldNumber(tag)) {
                case 1 -> traceId = hex(r.readBytes());
                case 2 -> spanId = hex(r.readBytes());
                case 4 -> parentSpanId = hex(r.readBytes());
                case 5 -> name = r.readString();
                case 6 -> kind = r.readVarint();
                case 7 -> startTime = r.readFixed64();
                case 8 -> endTime = r.readFixed64();
                case 9 -> {
                    KeyValue kv = decodeKeyValue(r.readBytes());
                    attrs.put(kv.key(), kv.value());
                }
                case 15 -> statusCode = decodeStatusCode(r.readBytes());
                default -> r.skip(type);
            }
        }
        log.info("  span: name={} kind={} trace_id={} span_id={} parent_span_id={} start={} end={} status={}",
                name, kind, traceId, spanId, parentSpanId, startTime, endTime, statusCode);
        attrs.forEach((k, v) -> log.info("    attr {}={}", k, v));
        // stash the first span's decoded fields for round-trip assertions
        received.putIfAbsent("wire.trace_id", traceId);
        received.putIfAbsent("wire.span_id", spanId);
        received.putIfAbsent("wire.parent_span_id", parentSpanId);
        received.putIfAbsent("wire.span_name", name);
        received.putIfAbsent("wire.attributes", attrs);
    }

    /** Status {@code { string message = 2; StatusCode code = 3; }} - return the code. */
    private long decodeStatusCode(byte[] data) {
        ProtoReader r = new ProtoReader(data);
        long code = 0;
        while (r.hasMore()) {
            int tag = r.readTag();
            if (fieldNumber(tag) == 3 && wireType(tag) == WIRETYPE_VARINT) {
                code = r.readVarint();
            } else {
                r.skip(wireType(tag));
            }
        }
        return code;
    }

    /** KeyValue {@code { string key = 1; AnyValue value = 2; }} */
    private KeyValue decodeKeyValue(byte[] data) {
        ProtoReader r = new ProtoReader(data);
        String key = null;
        String value = "";
        while (r.hasMore()) {
            int tag = r.readTag();
            int field = fieldNumber(tag);
            int type = wireType(tag);
            if (field == 1 && type == WIRETYPE_LEN) {
                key = r.readString();
            } else if (field == 2 && type == WIRETYPE_LEN) {
                value = decodeAnyValue(r.readBytes());
            } else {
                r.skip(type);
            }
        }
        return new KeyValue(key, value);
    }

    /**
     * AnyValue oneof {@code { string string_value=1; bool bool_value=2; int64 int_value=3;
     * double double_value=4; ... }}. Only the scalar leaves are rendered; array/kvlist/bytes
     * variants (which the forwarder never emits) are skipped.
     */
    private String decodeAnyValue(byte[] data) {
        ProtoReader r = new ProtoReader(data);
        String result = "";
        while (r.hasMore()) {
            int tag = r.readTag();
            int type = wireType(tag);
            switch (fieldNumber(tag)) {
                case 1 -> result = r.readString();
                case 2 -> result = String.valueOf(r.readVarint() != 0);
                case 3 -> result = String.valueOf(r.readVarint());
                case 4 -> result = String.valueOf(Double.longBitsToDouble(r.readFixed64()));
                default -> r.skip(type);
            }
        }
        return result;
    }

    private static int fieldNumber(int tag) {
        return tag >>> 3;
    }

    private static int wireType(int tag) {
        return tag & 0x7;
    }

    /** OTLP carries IDs as raw bytes (trace_id 16 bytes, span_id 8 bytes); render them as hex. */
    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    /** Decoded {@code KeyValue} - key plus its scalar value rendered as a string. */
    private record KeyValue(String key, String value) {
    }

    /**
     * A minimal, read-only protobuf wire-format decoder - just enough to walk the OTLP message tree
     * without the {@code com.google.protobuf} runtime. Not general purpose: it assumes well-formed
     * input and reads sequentially; the caller catches any overrun and treats it as an unparseable body.
     */
    private static final class ProtoReader {
        private final byte[] buf;
        private final int limit;
        private int pos;

        ProtoReader(byte[] buf) {
            this(buf, 0, buf.length);
        }

        ProtoReader(byte[] buf, int offset, int length) {
            this.buf = buf;
            this.pos = offset;
            this.limit = offset + length;
        }

        boolean hasMore() {
            return pos < limit;
        }

        /** Read a field tag: {@code (field_number << 3) | wire_type}. */
        int readTag() {
            return (int) readVarint();
        }

        /** Base-128 varint (little-endian groups of 7 bits, high bit = "more bytes follow"). */
        long readVarint() {
            long result = 0;
            int shift = 0;
            while (true) {
                byte b = buf[pos++];
                result |= (long) (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    return result;
                }
                shift += 7;
            }
        }

        /** Little-endian 64-bit fixed (fixed64 / sfixed64 / double). */
        long readFixed64() {
            long v = 0;
            for (int i = 0; i < 8; i++) {
                v |= (long) (buf[pos++] & 0xFF) << (8 * i);
            }
            return v;
        }

        /** A length-delimited chunk: bytes, a string, or an embedded message. */
        byte[] readBytes() {
            int len = (int) readVarint();
            byte[] out = Arrays.copyOfRange(buf, pos, pos + len);
            pos += len;
            return out;
        }

        String readString() {
            int len = (int) readVarint();
            String s = new String(buf, pos, len, StandardCharsets.UTF_8);
            pos += len;
            return s;
        }

        /** Advance past a field whose value we do not need, honouring its wire type. */
        void skip(int wireType) {
            switch (wireType) {
                // read the length first: readVarint() advances pos, so a "pos += readVarint()"
                // compound-assignment would capture the stale pos and lose that advance.
                case WIRETYPE_LEN -> {
                    int len = (int) readVarint();
                    pos += len;
                }
                case WIRETYPE_FIXED64 -> pos += 8;
                case WIRETYPE_FIXED32 -> pos += 4;
                default -> readVarint();   // WIRETYPE_VARINT (and anything else varint-shaped)
            }
        }
    }
}
