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

package org.platformlambda.core.serializers;

import org.msgpack.core.*;
import org.msgpack.value.ValueType;
import org.platformlambda.core.models.TypedPayload;
import org.platformlambda.core.util.Utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MsgPack {
    private static final Utility util = Utility.getInstance();
    private static final PayloadMapper converter = PayloadMapper.getInstance();
    private static final SimpleObjectMapper mapper = SimpleMapper.getInstance().getMapper();

    private static final String DATA = "_D";
    private static final String TYPE = "_T";
    /**
     * Unpack method for generic map or list object
     *
     * @param bytes - packed structure
     * @return result - Map, List or PoJo object
     *
     * @throws IOException for mapping exception
     */
    @SuppressWarnings("unchecked")
    public Object unpack(byte[] bytes) throws IOException  {
        Object result = unpackData(bytes);
        if (result instanceof Map) {
            // is this an encoded payload
            Map<String, Object> map = (Map<String, Object>) result;
            if (map.containsKey(TYPE)) {
                if (map.size() == 2 && map.containsKey(DATA)) {
                    return map.get(DATA);
                }
                if (map.size() == 1 && map.get(TYPE).equals(PayloadMapper.NOTHING)) {
                    return null;
                }
            }
        }
        return result;
    }
    /**
     * Optimized unpack method for generic map or list object
     *
     * @param bytes - packed structure
     * @return result - Map or List
     * @throws IOException for mapping exception
     */
    private Object unpackData(byte[] bytes) throws IOException  {
        MessageUnpacker handler = null;
        try {
            handler = MessagePack.newDefaultUnpacker(bytes, 0, bytes.length);
            if (handler.hasNext()) {
                MessageFormat mf = handler.getNextFormat();
                ValueType type = mf.getValueType();
                return switch (type) {
                    case ValueType.MAP -> unpack(handler, new HashMap<>());
                    case ValueType.ARRAY -> unpack(handler, new ArrayList<>());
                    default -> throw new MessageFormatException("Packed input should be Map or List, Actual: " + type);
                };
            }
            handler.close();
            handler = null;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
        // this should not occur
        return new HashMap<String, Object>();
    }

    private Map<String, Object> unpack(MessageUnpacker handler, Map<String, Object> map) throws IOException {
        int n = handler.unpackMapHeader();
        for (int i=0; i < n; i++) {
            String key = handler.unpackString();
            MessageFormat mf = handler.getNextFormat();
            ValueType type = mf.getValueType();
            if (type == ValueType.MAP) {
                Map<String, Object> submap = new HashMap<>();
                map.put(key, submap);
                unpack(handler, submap);
            } else if (type == ValueType.ARRAY) {
                List<Object> array = new ArrayList<>();
                map.put(key, array);
                unpack(handler, array);
            } else {
                // skip null value
                Object value = unpackValue(handler, mf);
                if (value != null) {
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    private List<Object> unpack(MessageUnpacker handler, List<Object> list) throws IOException {
        int len = handler.unpackArrayHeader();
        for (int i=0; i < len; i++) {
            MessageFormat mf = handler.getNextFormat();
            ValueType type = mf.getValueType();
            if (type == ValueType.MAP) {
                Map<String, Object> submap = new HashMap<>();
                list.add(submap);
                unpack(handler, submap);
            } else if (type == ValueType.ARRAY) {
                List<Object> array = new ArrayList<>();
                list.add(array);
                unpack(handler, array);
            } else {
                // null value is allowed to preserve the original sequence of the list
                list.add(unpackValue(handler, mf));
            }
        }
        return list;
    }

    private Object unpackValue(MessageUnpacker handler, MessageFormat mf) throws IOException {
        ValueType type = mf.getValueType();
        switch (type) {
            case STRING:
                return handler.unpackString();
            // best effort type matching
            case INTEGER:
                long n = handler.unpackLong();
                if (n > Integer.MAX_VALUE || n < Integer.MIN_VALUE) {
                    return n;
                } else {
                    return (int) n;
                }
            case FLOAT:
                if (mf == MessageFormat.FLOAT64) {
                    return handler.unpackDouble();
                } else {
                    return handler.unpackFloat();
                }
            case BINARY:
                int bytesLen = handler.unpackBinaryHeader();
                byte[] bytesValue = new byte[bytesLen];
                handler.readPayload(bytesValue);
                return bytesValue;
            case BOOLEAN:
                return handler.unpackBoolean();
            case NIL:
                handler.unpackNil();
                return null;
            default:
                // for simplicity, custom types are not supported
                handler.skipValue();
                return null;
        }
    }
    /**
     * Pack input into a byte array.
     *
     * @param obj - Map, List or a PoJo Object that contains get/set methods for variables
     * @return packed byte array
     *
     * @throws IOException for msgpack object mapping exception
     */
    public byte[] pack(Object obj) throws IOException {
        if (obj instanceof Map || obj instanceof List) {
            // select low level processing for faster performance
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MessagePacker packer = null;
            try {
                packer = MessagePack.newDefaultPacker(out);
                pack(packer, obj).close();
                packer = null;
            } finally {
                if (packer != null) {
                    packer.close();
                }
            }
            return out.toByteArray();
        } else {
            TypedPayload typed = converter.encode(obj, true);
            Map<String, Object> map = new HashMap<>();
            map.put(TYPE, typed.type());
            map.put(DATA, typed.payload());
            return pack(map);
        }
    }

    @SuppressWarnings({"rawtypes" })
    private MessagePacker pack(MessagePacker packer, Object o) throws IOException {
        switch (o) {
            case null -> packer.packNil();
            case Map map -> packMap(packer, map);
            case Collection list -> {
                packer.packArrayHeader(list.size());
                for (Object l : list) {
                    pack(packer, l);
                }
            }
            case Object[] objects -> {
                // Array is treated like a list
                packer.packArrayHeader(objects.length);
                for (Object l : objects) {
                    pack(packer, l);
                }
            }
            case String str -> packer.packString(str);
            case Short s -> packer.packShort(s);
            case Byte b -> packer.packByte(b);
            case Integer i -> packer.packInt(i);
            case AtomicInteger aInt -> packer.packInt(aInt.get());
            case Long l -> packer.packLong(l);
            case AtomicLong aLong -> packer.packLong(aLong.get());
            case Float f -> packer.packFloat(f);
            case Double d -> packer.packDouble(d);
            case BigInteger bInt ->
                // convert to string to preserve precision
                packer.packString(bInt.toString());
            case BigDecimal bDecimal ->
                // convert to string to preserve precision
                packer.packString(bDecimal.toPlainString());
            case Boolean bb -> packer.packBoolean(bb);
            case byte[] b -> {
                packer.packBinaryHeader(b.length);
                packer.writePayload(b);
            }
            case Date d ->
                // Date object will be packed as ISO-8601 string
                packer.packString(util.date2str(d));
            default -> {
                // handle pojo inside data structure
                if (util.isPoJo(o)) {
                    try {
                        var value = mapper.readValue(o, Map.class);
                        pack(packer, value);
                    } catch (Exception e) {
                        packer.packString(String.valueOf(o));
                    }
                } else {
                    // unknown object
                    packer.packString(String.valueOf(o));
                }
            }
        }
        return packer;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void packMap(MessagePacker packer, Map map) throws IOException {
        // In json, the key may not be a string
        int mapSize = map.size();
        List<Object> keys = new ArrayList<>(map.keySet());
        for (var k : keys) {
            // reduce map size if null value
            if (map.get(k) == null) {
                mapSize--;
            }
        }
        packer.packMapHeader(mapSize);
        if (mapSize > 0) {
            for (var k : keys) {
                // ignore null value
                Object value = map.get(k);
                if (value != null) {
                    // convert key to string
                    packer.packString(k instanceof String text ? text : String.valueOf(k));
                    pack(packer, value);
                }
            }
        }
    }
}
