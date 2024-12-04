/*

    Copyright 2018-2024 Accenture Technology

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
        Object result = unpack(bytes, 0, bytes.length);
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
     * @param offset of array
     * @param length from offset
     * @return result - Map or List
     * @throws IOException for mapping exception
     */
    public Object unpack(byte[] bytes, int offset, int length) throws IOException  {
        MessageUnpacker unpacker = null;
        try {
            unpacker = MessagePack.newDefaultUnpacker(bytes, offset, length);
            if (unpacker.hasNext()) {
                MessageFormat mf = unpacker.getNextFormat();
                ValueType type = mf.getValueType();
                if (type == ValueType.MAP) {
                    return unpack(unpacker, new HashMap<>());
                } else if (type == ValueType.ARRAY) {
                    return unpack(unpacker, new ArrayList<>());
                } else {
                    throw new MessageFormatException("Packed input should be Map or List, Actual: "+type);
                }
            }
            unpacker.close();
            unpacker = null;
        } finally {
            if (unpacker != null) {
                unpacker.close();
            }
        }
        // this should not occur
        return new HashMap<String, Object>();
    }

    private Map<String, Object> unpack(MessageUnpacker unpacker, Map<String, Object> map) throws IOException {
        int n = unpacker.unpackMapHeader();
        for (int i=0; i < n; i++) {
            String key = unpacker.unpackString();
            MessageFormat mf = unpacker.getNextFormat();
            ValueType type = mf.getValueType();
            if (type == ValueType.MAP) {
                Map<String, Object> submap = new HashMap<>();
                map.put(key, submap);
                unpack(unpacker, submap);
            } else if (type == ValueType.ARRAY) {
                List<Object> array = new ArrayList<>();
                map.put(key, array);
                unpack(unpacker, array);
            } else {
                // skip null value
                Object value = unpackValue(unpacker, mf);
                if (value != null) {
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    private List<Object> unpack(MessageUnpacker unpacker, List<Object> list) throws IOException {
        int len = unpacker.unpackArrayHeader();
        for (int i=0; i < len; i++) {
            MessageFormat mf = unpacker.getNextFormat();
            ValueType type = mf.getValueType();
            if (type == ValueType.MAP) {
                Map<String, Object> submap = new HashMap<>();
                list.add(submap);
                unpack(unpacker, submap);
            } else if (type == ValueType.ARRAY) {
                List<Object> array = new ArrayList<>();
                list.add(array);
                unpack(unpacker, array);
            } else {
                // null value is allowed to preserve the original sequence of the list
                list.add(unpackValue(unpacker, mf));
            }
        }
        return list;
    }

    private Object unpackValue(MessageUnpacker unpacker, MessageFormat mf) throws IOException {
        ValueType type = mf.getValueType();
        switch (type) {
            case STRING:
                return unpacker.unpackString();

            /*
             * Type information is lost for numbers.
             * This is the best effort to keep int/long and float/double value.
             *
             * Make int/long handling consistent between SimpleMapper and MsgPack
             *
             * Note: ternary conditional operator does not work due to different return types
             */
            case INTEGER:
                long asLong = unpacker.unpackLong();
                if (asLong > Integer.MAX_VALUE || asLong < Integer.MIN_VALUE) {
                    return asLong;
                } else {
                    return (int) asLong;
                }

            case FLOAT:
                if (mf == MessageFormat.FLOAT64) {
                    return unpacker.unpackDouble();
                } else {
                    return unpacker.unpackFloat();
                }

            case BINARY:
                int bytesLen = unpacker.unpackBinaryHeader();
                byte[] bytesValue = new byte[bytesLen];
                unpacker.readPayload(bytesValue);
                return bytesValue;

            case BOOLEAN:
                return unpacker.unpackBoolean();

            case NIL:
                unpacker.unpackNil();
                return null;

            default:
                // for simplicity, custom types are not supported
                unpacker.skipValue();
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
            map.put(TYPE, typed.getType());
            map.put(DATA, typed.getPayload());
            return pack(map);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private MessagePacker pack(MessagePacker packer, Object o) throws IOException {
        switch (o) {
            case null ->
                // preserving null element in an array list
                packer.packNil();
            case Map mapObject -> {
                // In json, the key may not be a string
                Map<Object, Object> map = (Map<Object, Object>) mapObject;
                int mapSize = map.size();
                for (Map.Entry<Object, Object> kv : map.entrySet()) {
                    // reduce map size if null value
                    if (kv.getValue() == null) {
                        mapSize--;
                    }
                }
                packer.packMapHeader(mapSize);
                if (mapSize > 0) {
                    for (Map.Entry<Object, Object> kv : map.entrySet()) {
                        // ignore null value
                        Object key = kv.getKey();
                        Object value = kv.getValue();
                        if (value != null) {
                            // convert key to string
                            packer.packString(key instanceof String str ? str : key.toString());
                            pack(packer, value);
                        }
                    }
                }
            }
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
            default ->
                // unknown object
                    packer.packString(o.toString());
        }
        return packer;
    }

}
