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

package org.platformlambda.mini.msgpack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * A value-level codec over the streaming reader and writer, for the tests: it writes and reads nested maps, lists and
 * scalars, generates random documents, and compares two documents deeply (byte arrays by content).
 */
final class TestCodec {
    static final long[] INTEGER_BOUNDARIES = {
            0, 1, 127, 128, 255, 256, 65535, 65536, 4294967295L, 4294967296L, Long.MAX_VALUE,
            -1, -32, -33, -128, -129, -32768, -32769, Integer.MIN_VALUE, Integer.MIN_VALUE - 1L, Long.MIN_VALUE};
    private static final int[][] ALPHABETS = {
            "abcdefghijklmnopqrstuvwxyz0123456789 _-.,:;/{}[]\"'".codePoints().toArray(),   // ASCII
            "café naïve über ñ".codePoints().toArray(),                                     // Latin-1 (two UTF-8 bytes)
            "€中文日本語ไทย".codePoints().toArray(),                                             // BMP (three UTF-8 bytes)
            "😀🚀🌍".codePoints().toArray()};                                               // supplementary (four bytes)

    private TestCodec() {
    }

    static byte[] encode(Object value) {
        var writer = new MsgPackWriter();
        write(writer, value);
        return writer.toByteArray();
    }

    static Object decode(byte[] bytes) throws MsgPackException {
        var reader = new MsgPackReader(bytes);
        Object value = read(reader);
        if (reader.hasNext()) {
            throw new MsgPackException("Unexpected bytes after the value at offset " + reader.position());
        }
        return value;
    }

    static void write(MsgPackWriter writer, Object value) {
        switch (value) {
            case null -> writer.writeNil();
            case Boolean b -> writer.writeBoolean(b);
            case Integer i -> writer.writeLong(i);
            case Long l -> writer.writeLong(l);
            case Float f -> writer.writeFloat(f);
            case Double d -> writer.writeDouble(d);
            case String s -> writer.writeString(s);
            case byte[] b -> writer.writeBinary(b);
            case List<?> list -> {
                writer.writeArrayHeader(list.size());
                for (var item : list) {
                    write(writer, item);
                }
            }
            case Map<?, ?> map -> {
                writer.writeMapHeader(map.size());
                for (var entry : map.entrySet()) {
                    writer.writeString((String) entry.getKey());
                    write(writer, entry.getValue());
                }
            }
            default -> throw new IllegalArgumentException("Unsupported " + value.getClass().getName());
        }
    }

    static Object read(MsgPackReader reader) throws MsgPackException {
        MsgPackFormat format = reader.nextFormat();
        switch (format.getType()) {
            case NIL -> {
                reader.readNil();
                return null;
            }
            case BOOLEAN -> {
                return reader.readBoolean();
            }
            case INTEGER -> {
                if (format == MsgPackFormat.UINT64) {
                    // as the value layer does: a Long when it fits, a BigInteger above Long.MAX_VALUE
                    var big = reader.readBigInteger();
                    return big.bitLength() < 64 ? (Object) big.longValue() : big;
                }
                return reader.readLong();
            }
            case FLOAT -> {
                return format == MsgPackFormat.FLOAT32 ? (Object) reader.readFloat() : reader.readDouble();
            }
            case STRING -> {
                return reader.readString();
            }
            case BINARY -> {
                return reader.readBinary();
            }
            case ARRAY -> {
                int count = reader.readArrayHeader();
                var list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    list.add(read(reader));
                }
                return list;
            }
            case MAP -> {
                int count = reader.readMapHeader();
                var map = new LinkedHashMap<String, Object>();
                for (int i = 0; i < count; i++) {
                    map.put(reader.readString(), read(reader));
                }
                return map;
            }
            default -> {
                reader.skipValue();
                return "<ext>";
            }
        }
    }

    /**
     * Deep equality where a byte array compares by content and a Long written as a uint64 above Long.MAX_VALUE is not
     * generated, so numbers compare by their boxed equality.
     */
    static boolean same(Object a, Object b) {
        if (a instanceof byte[] x && b instanceof byte[] y) {
            return Arrays.equals(x, y);
        }
        if (a instanceof List<?> x && b instanceof List<?> y) {
            if (x.size() != y.size()) {
                return false;
            }
            for (int i = 0; i < x.size(); i++) {
                if (!same(x.get(i), y.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof Map<?, ?> x && b instanceof Map<?, ?> y) {
            if (x.size() != y.size()) {
                return false;
            }
            var xi = x.entrySet().iterator();
            var yi = y.entrySet().iterator();
            while (xi.hasNext()) {
                var p = xi.next();
                var q = yi.next();
                if (!p.getKey().equals(q.getKey()) || !same(p.getValue(), q.getValue())) {
                    return false;
                }
            }
            return true;
        }
        return Objects.equals(a, b);
    }

    static Object randomDocument(Random random) {
        return random.nextBoolean() ? randomMap(random, 4) : randomList(random, 4);
    }

    static Object randomValue(Random random, int depth) {
        int kind = random.nextInt(depth > 0 ? 9 : 7);
        return switch (kind) {
            case 0 -> null;
            case 1 -> random.nextBoolean();
            case 2 -> randomLong(random);
            case 3 -> randomDouble(random);
            case 4 -> random.nextBoolean() ? (Float) random.nextFloat() : (Float) (random.nextFloat() * 1e30f);
            case 5 -> randomString(random, random.nextInt(8) == 0 ? 300 : 40);
            case 6 -> randomBytes(random);
            case 7 -> randomList(random, depth - 1);
            default -> randomMap(random, depth - 1);
        };
    }

    static long randomLong(Random random) {
        if (random.nextBoolean()) {
            return INTEGER_BOUNDARIES[random.nextInt(INTEGER_BOUNDARIES.length)];
        }
        // a random bit length, so every width is exercised
        int bits = 1 + random.nextInt(63);
        long magnitude = random.nextLong() & ((1L << bits) - 1);
        return random.nextBoolean() ? magnitude : -magnitude;
    }

    static double randomDouble(Random random) {
        return switch (random.nextInt(6)) {
            case 0 -> 0.0d;
            case 1 -> -0.0d;
            case 2 -> Double.POSITIVE_INFINITY;
            case 3 -> Double.MIN_VALUE;
            default -> (random.nextDouble() - 0.5) * Math.pow(10, random.nextInt(40) - 20);
        };
    }

    static String randomString(Random random, int maxLength) {
        int length = random.nextInt(maxLength + 1);
        var sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            // mostly ASCII, with the other alphabets mixed in
            // pick a code point, never a char index: a char index may land on the low half of a surrogate pair
            int[] alphabet = ALPHABETS[random.nextInt(4) == 0 ? random.nextInt(ALPHABETS.length) : 0];
            sb.appendCodePoint(alphabet[random.nextInt(alphabet.length)]);
        }
        return sb.toString();
    }

    static byte[] randomBytes(Random random) {
        var bytes = new byte[random.nextInt(random.nextInt(10) == 0 ? 400 : 20)];
        random.nextBytes(bytes);
        return bytes;
    }

    static List<Object> randomList(Random random, int depth) {
        int count = random.nextInt(random.nextInt(6) == 0 ? 40 : 8);
        var list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(randomValue(random, depth));
        }
        return list;
    }

    static Map<String, Object> randomMap(Random random, int depth) {
        int count = random.nextInt(random.nextInt(6) == 0 ? 40 : 8);
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < count; i++) {
            map.put("k" + i + randomString(random, 12), randomValue(random, depth));
        }
        return map;
    }
}
