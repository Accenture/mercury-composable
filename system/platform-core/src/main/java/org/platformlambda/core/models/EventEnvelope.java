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

package org.platformlambda.core.models;

import org.platformlambda.core.exception.AppException;
import org.platformlambda.core.serializers.MsgPack;
import org.platformlambda.core.serializers.PayloadMapper;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class EventEnvelope {
    private static final Logger log = LoggerFactory.getLogger(EventEnvelope.class);
    private static final Utility util = Utility.getInstance();
    private static final MsgPack msgPack = new MsgPack();
    private static final PayloadMapper converter = PayloadMapper.getInstance();
    private static final String ID_FIELD = "id";
    private static final String TO_FIELD = "to";
    private static final String FROM_FIELD = "from";
    private static final String REPLY_TO_FIELD = "reply_to";
    private static final String TRACE_ID_FIELD = "trace_id";
    private static final String TRACE_PATH_FIELD = "trace_path";
    private static final String CID_FIELD = "cid";
    private static final String TAG_FIELD = "tags";
    private static final String ANNOTATION_FIELD = "annotations";
    private static final String STATUS_FIELD = "status";
    private static final String HEADERS_FIELD = "headers";
    private static final String BODY_FIELD = "body";
    private static final String EXCEPTION_FIELD = "exception";
    private static final String STACK_FIELD = "stack";
    private static final String OBJ_TYPE_FIELD = "obj_type";
    private static final String EXECUTION_FIELD = "exec_time";
    private static final String ROUND_TRIP_FIELD = "round_trip";
    private static final String OPTIONAL = "optional";
    private static final String JSON = "json";
    private static final String BROADCAST = "broadcast";
    private static final String MESSAGE = "message";
    // message-ID
    private static final String ID_FLAG = "0";
    private static final String EXECUTION_FLAG = "1";
    private static final String ROUND_TRIP_FLAG = "2";
    // extra flag "3" has been retired
    private static final String EXCEPTION_FLAG = "4";
    private static final String STACK_FLAG = "5";
    private static final String ANNOTATION_FLAG = "6";
    private static final String TAG_FLAG = "7";
    // route paths
    private static final String TO_FLAG = "T";
    private static final String REPLY_TO_FLAG = "R";
    private static final String FROM_FLAG = "F";
    // status
    private static final String STATUS_FLAG = "S";
    // message headers and body
    private static final String HEADERS_FLAG = "H";
    private static final String BODY_FLAG = "B";
    // distributed trace ID for tracking a transaction from the edge to multiple levels of services
    private static final String TRACE_ID_FLAG = "t";
    private static final String TRACE_PATH_FLAG = "p";
    // optional correlation ID
    private static final String CID_FLAG = "X";
    // object type for automatic serialization
    private static final String OBJ_TYPE_FLAG = "O";
    // special header for setting HTTP cookie for rest-automation
    private static final String SET_COOKIE = "set-cookie";
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> tags = new HashMap<>();
    private final Map<String, Object> annotations = new HashMap<>();
    private String id;
    private String from;
    private String to;
    private String replyTo;
    private String traceId;
    private String tracePath;
    private String cid;
    // type: Map = "M", List = "L", Primitive = "P", Nothing = "N" or body class name
    private String type;
    private Integer status;
    private Object body;
    private byte[] exceptionBytes;
    private Throwable exception;
    private String stackTrace;
    private Float executionTime;
    private Float roundTrip;
    private boolean exRestored = false;

    public EventEnvelope() {
        this.id = util.getUuid();
    }

    public EventEnvelope(byte[] event) throws IOException {
        load(event);
    }

    public EventEnvelope(Map<String, Object> map) {
        fromMap(map);
    }

    public String getId() {
        return id;
    }

    public String getTo() {
        return to;
    }

    public String getFrom() {
        return from;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getTracePath() {
        return tracePath;
    }

    public String getCorrelationId() {
        return cid;
    }

    public int getStatus() {
        return status == null? 200 : status;
    }

    public float getExecutionTime() {
        return executionTime == null? 0.0f : executionTime;
    }

    public float getRoundTrip() {
        return roundTrip == null? 0.0f : roundTrip;
    }

    public int getBroadcastLevel() {
        String broadcast = getTag(BROADCAST);
        return broadcast != null? Math.max(0, util.str2int(broadcast)) : 0;
    }

    public boolean isBinary() {
        return !tags.containsKey(JSON);
    }

    /**
     * Since we use the same HTTP status code numbering,
     * an error condition is defined as status code >= 400.
     *
     * @return true if the event is considered as an error.
     */
    public boolean hasError() {
        return status != null && status >= 400;
    }

    @SuppressWarnings("rawtypes")
    public Object getError() {
        if (hasError()) {
            switch (body) {
                case null -> {
                    return "null";
                }
                case String str -> {
                    return str;
                }
                case Map error -> {
                    // extract error message if error message signature is found
                    return "error".equals(error.get("type")) &&
                            error.containsKey(STATUS_FIELD) && error.containsKey(MESSAGE)?
                            String.valueOf(error.get(MESSAGE)) : body;
                }
                case byte[] ignored -> {
                    return "***";
                }
                default -> {
                    return body;
                }
            }
        } else {
            return null;
        }
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getType() {
        return type;
    }

    public boolean isOptional() {
        return tags.containsKey(OPTIONAL);
    }

    /**
     * Get raw form of event body in Map or Java primitive
     * <p>
     * @return body in map or primitive form
     */
    public Object getRawBody() {
        return body;
    }

    /**
     * Get event body
     *
     * @return body or optional.of(body)
     */
    public Object getBody() {
        return isOptional()? Optional.ofNullable(body) : body;
    }

    /**
     * Convert body to another class type
     * (Best effort conversion - some fields may be lost if interface contracts are not compatible)
     * <p>
     * @param toValueType target class type
     * @param <T> class type
     * @return converted body
     */
    public <T> T getBody(Class<T> toValueType) {
        return SimpleMapper.getInstance().getMapper().readValue(body, toValueType);
    }

    /**
     * Convert body to a list of PoJo
     *
     * @param toValueType target class type
     * @return list of pojo
     * @param <T> class type
     */
    @SuppressWarnings("rawtypes")
    public <T> List<T> getBodyAsListOfPoJo(Class<T> toValueType) {
        List pojoList = null;
        List<T> result = new ArrayList<>();
        // for compatibility with older version 2
        if (body instanceof Map m) {
            Object o = m.get("list");
            if (o instanceof List oList) {
                pojoList = oList;
            }
        } else if (body instanceof List oList) {
            pojoList = oList;
        }
        if (pojoList != null) {
            var mapper = SimpleMapper.getInstance().getMapper();
            for (Object o: pojoList) {
                if (o instanceof Map) {
                    result.add(mapper.readValue(o, toValueType));
                } else {
                    result.add(null);
                }
            }
        }
        return result;
    }

    /**
     * Convert body to another class type
     * (Best effort conversion - some fields may be lost if interface contracts are not compatible)
     * <p>
     * @param toValueType target class type
     * @param parameterClass one or more parameter classes
     * @param <T> class type
     * @return converted body
     */
    public <T> T getBody(Class<T> toValueType, Class<?>... parameterClass) {
        if (parameterClass.length == 0) {
            throw new IllegalArgumentException("Missing parameter class");
        }
        return SimpleMapper.getInstance().getMapper().restoreGeneric(body, toValueType, parameterClass);
    }

    public EventEnvelope setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Set the target route
     *
     * @param to target route
     * @return event envelope
     */
    public EventEnvelope setTo(String to) {
        this.to = to;
        return this;
    }

    /**
     * Optionally provide the sender
     *
     * @param from the sender
     * @return event envelope
     */
    public EventEnvelope setFrom(String from) {
        this.from = from;
        return this;
    }

    /**
     * Optionally set the replyTo address
     *
     * @param replyTo route
     * @return event envelope
     */
    public EventEnvelope setReplyTo(String replyTo) {
        this.replyTo = replyTo;
        return this;
    }

    /**
     * A transaction trace should only be created by the rest-automation application
     * or a service at the "edge".
     *
     * @param traceId unique ID for distributed tracing
     * @param tracePath path at the edge such as HTTP method and URI
     * @return event envelope
     */
    public EventEnvelope setTrace(String traceId, String tracePath) {
        this.traceId = traceId;
        this.tracePath = tracePath;
        return this;
    }

    /**
     * A transaction trace should only be created by the rest-automation application
     * or a service at the "edge".
     *
     * @param traceId unique ID for distributed tracing
     * @return event envelope
     */
    public EventEnvelope setTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    /**
     * A transaction trace should only be created by the rest-automation application
     * or a service at the "edge".
     *
     * @param tracePath path at the edge such as HTTP method and URI
     * @return event envelope
     */
    public EventEnvelope setTracePath(String tracePath) {
        this.tracePath = tracePath;
        return this;
    }

    /**
     * Optionally set a correlation-ID
     *
     * @param cid correlation-ID
     * @return event envelope
     */
    public EventEnvelope setCorrelationId(String cid) {
        this.cid = cid;
        return this;
    }

    /**
     * Add a tag without value
     *
     * @param key without value
     * @return event envelope
     */
    public EventEnvelope addTag(String key) {
        return addTag(key, true);
    }

    /**
     * Add a tag with key-value
     * (Note: input value will be converted to a text string)
     *
     * @param key for a new tag
     * @param value for a new tag
     * @return event envelope
     */
    public EventEnvelope addTag(String key, Object value) {
        if (key != null && !key.isEmpty()) {
            tags.put(key, value == null? "true" : String.valueOf(value));
        }
        return this;
    }

    /**
     * Remove a tag
     *
     * @param key for a tag
     * @return event envelope
     */
    public EventEnvelope removeTag(String key) {
        if (key != null && !key.isEmpty()) {
            tags.remove(key);
        }
        return this;
    }

    /**
     * Retrieve a tag value
     *
     * @param key for a tag
     * @return event envelope
     */
    public String getTag(String key) {
        return tags.get(key);
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public EventEnvelope setTags(Map<String, String> tags) {
        this.tags.clear();
        this.tags.putAll(tags);
        return this;
    }

    public Map<String, Object> getAnnotations() {
        return annotations;
    }

    public EventEnvelope setAnnotations(Map<String, Object> annotations) {
        this.annotations.clear();
        this.annotations.putAll(annotations);
        return this;
    }

    public EventEnvelope clearAnnotations() {
        this.annotations.clear();
        return this;
    }

    /**
     * Optionally set status code (use HTTP compatible response code) if the return object is an event envelope.
     *
     * @param status 200 for normal response
     * @return event envelope
     */
    public EventEnvelope setStatus(int status) {
        this.status = status;
        return this;
    }

    /**
     * Retrieve a header value using case-insensitive key
     *
     * @param key of the header
     * @return header value
     */
    public String getHeader(String key) {
        if (key != null && !this.headers.isEmpty()) {
            // since the number of headers is very small, it is fine to scan the list
            String lc = key.toLowerCase();
            for (Map.Entry<String, String> kv : this.headers.entrySet()) {
                if (lc.equals(kv.getKey().toLowerCase())) {
                    return kv.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Optionally set a parameter
     *
     * @param key of a parameter
     * @param value of a parameter
     * @return event envelope
     */
    public EventEnvelope setHeader(String key, Object value) {
        if (key != null) {
            String v = switch (value) {
                case null -> "";
                case String str -> str;
                case Date d -> util.date2str(d);
                default -> String.valueOf(value);
            };
            // guarantee CR/LF are filtered out
            v = v.replace("\r", "").replace("\n", " ");
            // null value is transported as an empty string
            if (SET_COOKIE.equalsIgnoreCase(key)) {
                if (this.headers.containsKey(key)) {
                    String composite = this.headers.get(key) + "|" + v;
                    this.headers.put(key, composite);
                } else {
                    this.headers.put(key, v);
                }
            } else {
                this.headers.put(key, v);
            }
        }
        return this;
    }

    public EventEnvelope setHeaders(Map<String, String> headers) {
        // make a shallow copy
        if (headers != null) {
            headers.forEach(this::setHeader);
        }
        return this;
    }

    /**
     * Set payload
     *
     * @param body Usually a PoJo, a Map or Java primitive
     * @return event envelope
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public EventEnvelope setBody(Object body) {
        final Object payload;
        switch (body) {
            case Optional optionalBody -> {
                addTag(OPTIONAL);
                Optional<Object> o = (Optional<Object>) optionalBody;
                payload = o.orElse(null);
            }
            case EventEnvelope nested -> {
                log.warn("Setting body from nested EventEnvelope is discouraged - system will remove the outer envelope");
                return setBody(nested.getBody());
            }
            case AsyncHttpRequest request -> {
                return setBody(request.toMap());
            }
            case Date d -> payload = util.date2str(d);
            case null, default -> payload = body;
        }
        // encode body and save object type
        TypedPayload typed = converter.encode(payload, isBinary());
        this.body = typed.getPayload();
        this.type = typed.getType();
        return this;
    }

    /**
     * Get event exception if any
     *
     * @return exception cause
     */
    public Throwable getException() {
        if (!exRestored) {
            if (exceptionBytes != null) {
                try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(exceptionBytes))) {
                    exception = (Throwable) in.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    // ignore it because there is nothing we can do
                }
            }
            // best effort to recreate an exception
            if (exception == null && stackTrace != null) {
                exception = new RuntimeException(String.valueOf(body));
            }
            exRestored = true;
        }
        return exception;
    }

    /**
     * Set exception
     *
     * @param ex is the exception
     * @return event envelope
     */
    public EventEnvelope setException(Throwable ex) {
        if (ex != null) {
            if (ex instanceof AppException appEx) {
                setStatus(appEx.getStatus());
            } else if (ex instanceof IllegalArgumentException) {
                setStatus(400);
            } else {
                setStatus(500);
            }
            setBody(util.getRootCause(ex).getMessage());
            setStackTrace(getStackTrace(ex));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ObjectOutputStream stream = new ObjectOutputStream(out)) {
                stream.writeObject(ex);
                exceptionBytes = out.toByteArray();
            } catch (IOException e) {
                // Ignore the specific exception object because it is not serializable.
                // The exception is not transported but the stack trace should be available.
            }
        }
        return this;
    }

    public boolean isException() {
        return stackTrace != null;
    }

    /**
     * DO NOT use this method directly in your user application code.
     * <p>
     * This is reserved for unit test to simulate getting stack trace
     * from an external node.js composable application.
     *
     * @param stackTrace as a text message with linefeed
     * @return event envelope
     */
    public EventEnvelope setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
        return this;
    }

    /**
     * Retrieve stack trace returned from an external node.js composable application
     * (For efficiency, stack trace will be limited to maximum of 10 lines)
     *
     * @return stackTrace if any
     */
    public String getStackTrace() {
        return stackTrace;
    }

    /**
     * Limit stack trace to a maximum of 10 lines
     * @param ex is the exception object
     * @return stack trace trimmed
     */
    private String getStackTrace(Throwable ex) {
        String stack = util.getStackTrace(ex);
        // limit stack trace to 10 lines
        List<String> lines = util.split(stack, "\r\n");
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < 10 && i < lines.size(); i++) {
            sb.append(lines.get(i).trim());
            sb.append('\n');
        }
        if (lines.size() > 10) {
            sb.append("...(").append(lines.size()).append(")");
        }
        return sb.toString();
    }

    /**
     * DO NOT set this manually. The system will set it.
     *
     * @param milliseconds spent
     * @return event envelope
     */
    public EventEnvelope setExecutionTime(float milliseconds) {
        this.executionTime = Float.parseFloat(String.format("%.3f", milliseconds));
        return this;
    }

    /**
     * DO NOT set this manually. The system will set it.
     *
     * @param milliseconds spent
     * @return event envelope
     */
    public EventEnvelope setRoundTrip(float milliseconds) {
        this.roundTrip = Float.parseFloat(String.format("%.3f", milliseconds));
        return this;
    }

    /**
     * DO NOT set this manually. The system will set it.
     *
     * @param level 0 to 3
     * @return event envelope
     */
    public EventEnvelope setBroadcastLevel(int level) {
        if (level > 0 && level < 4) {
            addTag(BROADCAST, level);
        } else {
            removeTag(BROADCAST);
        }
        return this;
    }

    /**
     * By default, payload will be encoded as binary.
     * In some rare case where the built-in MsgPack binary serialization
     * does not work, you may turn off binary mode by setting it to false.
     * <p>
     * When it is set to false, the Java object in the payload will be encoded
     * using JSON bytes. This option should be used as the last resort because
     * it would reduce performance and increase encoded payload size.
     *
     * @param binary true or false
     * @return this EventEnvelope
     */
    public EventEnvelope setBinary(boolean binary) {
        if (binary) {
            tags.remove(JSON);
        } else {
            addTag(JSON);
        }
        return this;
    }

    /**
     * You should not set the object type normally.
     * It will be automatically set when a PoJo is set as the body.
     * This method is used by unit tests or other use cases.
     *
     * @param type of the body object
     * @return this EventEnvelope
     */
    public EventEnvelope setType(String type) {
        this.type = type;
        return this;
    }

    public EventEnvelope copy() {
        var event = new EventEnvelope();
        event.setTo(this.getTo())
            .setHeaders(this.getHeaders())
            .setType(this.getType())
            .setFrom(this.getFrom())
            .setCorrelationId(this.getCorrelationId());
        event.setStatus(this.getStatus())
            .setReplyTo(this.getReplyTo())
            .setTraceId(this.getTraceId())
            .setTracePath(this.getTracePath());
        event.id = this.id;
        event.body = this.body;
        event.stackTrace = this.stackTrace;
        event.exceptionBytes = this.exceptionBytes;
        event.executionTime = this.executionTime;
        event.roundTrip = this.roundTrip;
        event.tags.putAll(this.tags);
        event.annotations.putAll(this.annotations);
        return event;
    }

    /**
     * DeSerialize the EventEnvelope from a byte array
     *
     * @param bytes encoded payload
     * @throws IOException in case of decoding errors
     */
    @SuppressWarnings("unchecked")
    public void load(byte[] bytes) throws IOException {
        Object o = msgPack.unpack(bytes);
        if (o instanceof Map) {
            Map<String, Object> message = (Map<String, Object>) o;
            if (message.containsKey(ID_FLAG)) {
                id = (String) message.get(ID_FLAG);
            }
            if (message.containsKey(TO_FLAG)) {
                to = (String) message.get(TO_FLAG);
            }
            if (message.containsKey(FROM_FLAG)) {
                from = (String) message.get(FROM_FLAG);
            }
            if (message.containsKey(REPLY_TO_FLAG)) {
                replyTo = (String) message.get(REPLY_TO_FLAG);
            }
            if (message.containsKey(TRACE_ID_FLAG)) {
                traceId = (String) message.get(TRACE_ID_FLAG);
            }
            if (message.containsKey(TRACE_PATH_FLAG)) {
                tracePath = (String) message.get(TRACE_PATH_FLAG);
            }
            if (message.containsKey(CID_FLAG)) {
                cid = (String) message.get(CID_FLAG);
            }
            if (message.containsKey(TAG_FLAG)) {
                Object tf = message.get(TAG_FLAG);
                if (tf instanceof Map) {
                    Map<String, String> map = (Map<String, String>) tf;
                    tags.putAll(map);
                }
            }
            if (message.containsKey(ANNOTATION_FLAG)) {
                Object tf = message.get(ANNOTATION_FLAG);
                if (tf instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) tf;
                    annotations.putAll(map);
                }
            }
            if (message.containsKey(STATUS_FLAG)) {
                status = Math.max(0, util.str2int(String.valueOf(message.get(STATUS_FLAG))));
            }
            if (message.containsKey(HEADERS_FLAG)) {
                setHeaders((Map<String, String>) message.get(HEADERS_FLAG));
            }
            if (message.containsKey(BODY_FLAG)) {
                body = message.get(BODY_FLAG);
            }
            if (message.containsKey(EXCEPTION_FLAG)) {
                exceptionBytes = (byte[]) message.get(EXCEPTION_FLAG);
            }
            if (message.containsKey(STACK_FLAG)) {
                stackTrace = (String) message.get(STACK_FLAG);
            }
            if (message.containsKey(OBJ_TYPE_FLAG)) {
                type = (String) message.get(OBJ_TYPE_FLAG);
            }
            if (message.containsKey(EXECUTION_FLAG)) {
                executionTime = Math.max(0, util.str2float(String.valueOf(message.get(EXECUTION_FLAG))));
            }
            if (message.containsKey(ROUND_TRIP_FLAG)) {
                roundTrip = Math.max(0, util.str2float(String.valueOf(message.get(ROUND_TRIP_FLAG))));
            }
        }
    }

    /**
     * Serialize the EventEnvelope as a byte array
     *
     * @return byte array
     * @throws IOException in case of encoding errors
     */
    public byte[] toBytes() throws IOException {
        Map<String, Object> message = new HashMap<>();
        if (id != null) {
            message.put(ID_FLAG, id);
        }
        if (to != null) {
            message.put(TO_FLAG, to);
        }
        if (from != null) {
            message.put(FROM_FLAG, from);
        }
        if (replyTo != null) {
            message.put(REPLY_TO_FLAG, replyTo);
        }
        if (traceId != null) {
            message.put(TRACE_ID_FLAG, traceId);
        }
        if (tracePath != null) {
            message.put(TRACE_PATH_FLAG, tracePath);
        }
        if (cid != null) {
            message.put(CID_FLAG, cid);
        }
        if (status != null) {
            message.put(STATUS_FLAG, status);
        }
        if (!headers.isEmpty()) {
            message.put(HEADERS_FLAG, headers);
        }
        if (!tags.isEmpty()) {
            message.put(TAG_FLAG, tags);
        }
        if (!annotations.isEmpty()) {
            message.put(ANNOTATION_FLAG, annotations);
        }
        if (body != null) {
            message.put(BODY_FLAG, body);
        }
        if (exceptionBytes != null) {
            message.put(EXCEPTION_FLAG, exceptionBytes);
        }
        if (stackTrace != null) {
            message.put(STACK_FLAG, stackTrace);
        }
        if (type != null) {
            message.put(OBJ_TYPE_FLAG, type);
        }
        if (executionTime != null) {
            message.put(EXECUTION_FLAG, executionTime);
        }
        if (roundTrip != null) {
            message.put(ROUND_TRIP_FLAG, roundTrip);
        }
        return msgPack.pack(message);
    }

    @SuppressWarnings("unchecked")
    public void fromMap(Map<String, Object> message) {
        if (message.containsKey(ID_FIELD)) {
            id = (String) message.get(ID_FIELD);
        }
        if (message.containsKey(TO_FIELD)) {
            to = (String) message.get(TO_FIELD);
        }
        if (message.containsKey(FROM_FIELD)) {
            from = (String) message.get(FROM_FIELD);
        }
        if (message.containsKey(REPLY_TO_FIELD)) {
            replyTo = (String) message.get(REPLY_TO_FIELD);
        }
        if (message.containsKey(TRACE_ID_FIELD)) {
            traceId = (String) message.get(TRACE_ID_FIELD);
        }
        if (message.containsKey(TRACE_PATH_FIELD)) {
            tracePath = (String) message.get(TRACE_PATH_FIELD);
        }
        if (message.containsKey(CID_FIELD)) {
            cid = (String) message.get(CID_FIELD);
        }
        if (message.containsKey(TAG_FIELD)) {
            Object tf = message.get(TAG_FIELD);
            if (tf instanceof Map) {
                Map<String, String> map = (Map<String, String>) tf;
                tags.putAll(map);
            }
        }
        if (message.containsKey(ANNOTATION_FIELD)) {
            Object tf = message.get(ANNOTATION_FIELD);
            if (tf instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) tf;
                annotations.putAll(map);
            }
        }
        if (message.containsKey(STATUS_FIELD)) {
            status = Math.max(0, util.str2int(String.valueOf(message.get(STATUS_FIELD))));
        }
        if (message.containsKey(HEADERS_FIELD)) {
            setHeaders((Map<String, String>) message.get(HEADERS_FIELD));
        }
        if (message.containsKey(BODY_FIELD)) {
            body = message.get(BODY_FIELD);
        }
        if (message.containsKey(EXCEPTION_FIELD)) {
            exceptionBytes = (byte[]) message.get(EXCEPTION_FIELD);
        }
        if (message.containsKey(STACK_FIELD)) {
            stackTrace = (String) message.get(STACK_FIELD);
        }
        if (message.containsKey(OBJ_TYPE_FIELD)) {
            type = (String) message.get(OBJ_TYPE_FIELD);
        }
        if (message.containsKey(EXECUTION_FIELD)) {
            executionTime = Math.max(0f, util.str2float(String.valueOf(message.get(EXECUTION_FIELD))));
        }
        if (message.containsKey(ROUND_TRIP_FIELD)) {
            roundTrip = Math.max(0, util.str2float(String.valueOf(message.get(ROUND_TRIP_FIELD))));
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> message = new HashMap<>();
        if (id != null) {
            message.put(ID_FIELD, id);
        }
        if (to != null) {
            message.put(TO_FIELD, to);
        }
        if (from != null) {
            message.put(FROM_FIELD, from);
        }
        if (replyTo != null) {
            message.put(REPLY_TO_FIELD, replyTo);
        }
        if (traceId != null) {
            message.put(TRACE_ID_FIELD, traceId);
        }
        if (tracePath != null) {
            message.put(TRACE_PATH_FIELD, tracePath);
        }
        if (cid != null) {
            message.put(CID_FIELD, cid);
        }
        if (!tags.isEmpty()) {
            message.put(TAG_FIELD, tags);
        }
        if (!annotations.isEmpty()) {
            message.put(ANNOTATION_FIELD, annotations);
        }
        if (status != null) {
            message.put(STATUS_FIELD, status);
        }
        if (!headers.isEmpty()) {
            message.put(HEADERS_FIELD, headers);
        }
        if (body != null) {
            message.put(BODY_FIELD, body);
        }
        if (exceptionBytes != null) {
            message.put(EXCEPTION_FIELD, exceptionBytes);
        }
        if (stackTrace != null) {
            message.put(STACK_FIELD, stackTrace);
        }
        if (type != null) {
            message.put(OBJ_TYPE_FIELD, type);
        }
        if (executionTime != null) {
            message.put(EXECUTION_FIELD, executionTime);
        }
        if (roundTrip != null) {
            message.put(ROUND_TRIP_FIELD, roundTrip);
        }
        return message;
    }
}