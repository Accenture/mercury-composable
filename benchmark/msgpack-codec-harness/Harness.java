import org.platformlambda.core.serializers.MsgPack;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Differential and performance harness for platform-core's MsgPack, run once per backend (msgpack-core, minimalist):
 *   corpus <file> <count>   pack a seeded random corpus and write it to <file>
 *   verify <file> <count>   unpack a corpus written by the other backend and compare with the regenerated documents
 *   bench                   pack/unpack throughput on three payload shapes
 */
public class Harness {
    private static final MsgPack MSGPACK = new MsgPack();
    private static final long SEED = 20261006L;

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "corpus" -> corpus(Path.of(args[1]), Integer.parseInt(args[2]));
            case "verify" -> verify(Path.of(args[1]), Integer.parseInt(args[2]));
            case "bench" -> bench();
            default -> throw new IllegalArgumentException(args[0]);
        }
    }

    // ---------------------------------------------------------------- differential

    static void corpus(Path file, int count) throws IOException {
        var random = new Random(SEED);
        long total = 0;
        try (var out = new DataOutputStream(Files.newOutputStream(file))) {
            out.writeInt(count);
            for (int i = 0; i < count; i++) {
                byte[] bytes = MSGPACK.pack(document(random, i));
                out.writeInt(bytes.length);
                out.write(bytes);
                total += bytes.length;
            }
        }
        System.out.println("corpus: " + count + " documents, " + total + " bytes -> " + file);
    }

    static void verify(Path file, int count) throws IOException {
        var random = new Random(SEED);
        int mismatches = 0;
        try (var in = new DataInputStream(Files.newInputStream(file))) {
            int n = in.readInt();
            if (n != count) throw new IllegalStateException("count " + n + " != " + count);
            for (int i = 0; i < count; i++) {
                Object document = document(random, i);
                byte[] bytes = in.readNBytes(in.readInt());
                Object decoded = MSGPACK.unpack(bytes);
                if (!same(expected(document), decoded)) {
                    mismatches++;
                    if (mismatches <= 3) System.out.println("MISMATCH at document " + i + "\n expected " + expected(document) + "\n actual   " + decoded);
                }
            }
        }
        System.out.println("verify: " + count + " documents from " + file + ", mismatches: " + mismatches);
        if (mismatches > 0) System.exit(1);
    }

    /** A seeded random document; every value a MsgPack.pack call handles. */
    static Object document(Random random, int i) {
        return i % 2 == 0 ? randomMap(random, 4) : randomList(random, 4);
    }

    static Object randomValue(Random random, int depth) {
        int kind = random.nextInt(depth > 0 ? 10 : 8);
        return switch (kind) {
            case 0 -> null;
            case 1 -> random.nextBoolean();
            case 2 -> randomLong(random);
            case 3 -> (int) randomLong(random);
            case 4 -> random.nextBoolean() ? (Double) ((random.nextDouble() - 0.5) * 1e6) : (Double) (random.nextBoolean() ? Double.POSITIVE_INFINITY : -0.0d);
            case 5 -> (Float) (random.nextFloat() * 100f);
            case 6 -> randomString(random);
            case 7 -> randomBytes(random);
            case 8 -> randomList(random, depth - 1);
            default -> randomMap(random, depth - 1);
        };
    }

    static final long[] BOUNDARIES = {0, 1, 127, 128, 255, 256, 65535, 65536, 4294967295L, 4294967296L, Long.MAX_VALUE,
            -1, -32, -33, -128, -129, -32768, -32769, Integer.MIN_VALUE, Integer.MIN_VALUE - 1L, Long.MIN_VALUE};

    static long randomLong(Random random) {
        if (random.nextBoolean()) return BOUNDARIES[random.nextInt(BOUNDARIES.length)];
        int bits = 1 + random.nextInt(63);
        long magnitude = random.nextLong() & ((1L << bits) - 1);
        return random.nextBoolean() ? magnitude : -magnitude;
    }

    static final String[] ALPHABETS = {"abcdefghijklmnopqrstuvwxyz0123456789 _-.,:;/{}[]\"'", "café naïve über ñ",
            "€中文日本語ไทย", "😀🚀🌍"};

    static String randomString(Random random) {
        int length = random.nextInt(random.nextInt(8) == 0 ? 600 : 40);
        var sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String alphabet = ALPHABETS[random.nextInt(4) == 0 ? random.nextInt(ALPHABETS.length) : 0];
            sb.appendCodePoint(alphabet.codePointAt(random.nextInt(alphabet.length())));
        }
        // now and then an unpaired surrogate, which both backends must write the same way
        if (random.nextInt(50) == 0) sb.append('\ud800');
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
        for (int i = 0; i < count; i++) list.add(randomValue(random, depth));
        return list;
    }

    static Map<String, Object> randomMap(Random random, int depth) {
        int count = random.nextInt(random.nextInt(6) == 0 ? 40 : 8);
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < count; i++) {
            String suffix = randomString(random);
            map.put("k" + i + suffix.substring(0, Math.min(5, suffix.length())), randomValue(random, depth));
        }
        return map;
    }

    /** What MsgPack.unpack gives back for a document: map nulls dropped (null transport off), unpaired surrogates as '?'. */
    static Object expected(Object value) {
        if (value instanceof Map<?, ?> map) {
            var out = new LinkedHashMap<String, Object>();
            // keys travel as UTF-8 too: an unpaired surrogate in a key becomes '?' exactly like one in a value
            for (var e : map.entrySet()) if (e.getValue() != null) out.put((String) expected(e.getKey()), expected(e.getValue()));
            return out;
        }
        if (value instanceof List<?> list) {
            var out = new ArrayList<>();
            for (var item : list) out.add(expected(item));
            return out;
        }
        if (value instanceof String s) return new String(s.getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.charset.StandardCharsets.UTF_8);
        return value;
    }

    static boolean same(Object a, Object b) {
        if (a instanceof byte[] x && b instanceof byte[] y) return Arrays.equals(x, y);
        if (a instanceof Map<?, ?> x && b instanceof Map<?, ?> y) {
            if (x.size() != y.size()) return false;
            for (var e : x.entrySet()) if (!y.containsKey(e.getKey()) || !same(e.getValue(), y.get(e.getKey()))) return false;
            return true;
        }
        if (a instanceof List<?> x && b instanceof List<?> y) {
            if (x.size() != y.size()) return false;
            for (int i = 0; i < x.size(); i++) if (!same(x.get(i), y.get(i))) return false;
            return true;
        }
        if (a instanceof Number x && b instanceof Number y) {
            if (a instanceof Float || b instanceof Float) return a.equals(b);
            if (a instanceof Double || b instanceof Double) return a.equals(b);
            return x.longValue() == y.longValue();
        }
        return a == null ? b == null : a.equals(b);
    }

    // ---------------------------------------------------------------- benchmark

    static Map<String, Object> smallEnvelope() {
        var headers = new LinkedHashMap<String, Object>();
        headers.put("content-type", "application/json");
        headers.put("x-trace-id", "0af7651916cd43dd8448eb211c80319c");
        headers.put("x-correlation-id", "c0ffee-1234");
        var body = new LinkedHashMap<String, Object>();
        body.put("id", 1001);
        body.put("name", "Alice Example");
        body.put("active", true);
        body.put("balance", 1234.56d);
        var envelope = new LinkedHashMap<String, Object>();
        envelope.put("id", "e0b8e7a3c2d14f3e9a1b2c3d4e5f6a7b");
        envelope.put("to", "hello.world");
        envelope.put("from", "http.request");
        envelope.put("status", 200);
        envelope.put("headers", headers);
        envelope.put("body", body);
        envelope.put("trace_id", "0af7651916cd43dd8448eb211c80319c");
        envelope.put("trace_path", "GET /api/hello/world");
        envelope.put("reply_to", "r.0af7651916cd43dd");
        envelope.put("cid", "c0ffee-1234");
        envelope.put("exec_time", 1.25f);
        envelope.put("round_trip", 3L);
        return envelope;
    }

    static Map<String, Object> mediumRecords() {
        var records = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            var r = new LinkedHashMap<String, Object>();
            r.put("id", 100000 + i);
            r.put("sku", "SKU-" + (1000 + i) + "-XL");
            r.put("description", "A product description of moderate length " + i);
            r.put("price", 19.99d + i);
            r.put("quantity", (long) i * 3);
            r.put("in_stock", i % 3 != 0);
            r.put("tags", List.of("new", "sale", "popular"));
            records.add(r);
        }
        var body = new LinkedHashMap<String, Object>();
        body.put("records", records);
        body.put("total", 50);
        body.put("page", 1);
        var envelope = smallEnvelope();
        envelope.put("body", body);
        return envelope;
    }

    static Map<String, Object> largePayload() {
        var random = new Random(7);
        var bin = new byte[4096];
        random.nextBytes(bin);
        var numbers = new ArrayList<>();
        for (int i = 0; i < 200; i++) numbers.add(random.nextInt(1_000_000));
        var body = new LinkedHashMap<String, Object>();
        body.put("text", "lorem ipsum dolor sit amet ".repeat(80));
        body.put("blob", bin);
        body.put("numbers", numbers);
        for (int i = 0; i < 20; i++) body.put("field" + i, "value number " + i);
        var envelope = smallEnvelope();
        envelope.put("body", body);
        return envelope;
    }

    static void bench() throws IOException {
        Object[] payloads = {smallEnvelope(), mediumRecords(), largePayload()};
        String[] names = {"small", "medium", "large"};
        for (int p = 0; p < payloads.length; p++) {
            Object payload = payloads[p];
            byte[] packed = MSGPACK.pack(payload);
            System.out.printf("%-6s %6d bytes  pack %s  unpack %s  roundtrip %s%n", names[p], packed.length,
                    measure(() -> MSGPACK.pack(payload).length),
                    measure(() -> ((Map<?, ?>) MSGPACK.unpack(packed)).size()),
                    measure(() -> ((Map<?, ?>) MSGPACK.unpack(MSGPACK.pack(payload))).size()));
        }
    }

    interface Op { int run() throws IOException; }

    /** Warm up for 3 s, then take the best of 5 rounds of 2 s each; the result is ns per operation. */
    static String measure(Op op) throws IOException {
        long sink = 0;
        long end = System.nanoTime() + 3_000_000_000L;
        while (System.nanoTime() < end) sink += op.run();
        double best = Double.MAX_VALUE;
        for (int round = 0; round < 5; round++) {
            long ops = 0;
            long start = System.nanoTime();
            long stop = start + 2_000_000_000L;
            long now;
            do {
                for (int i = 0; i < 1000; i++) sink += op.run();
                ops += 1000;
                now = System.nanoTime();
            } while (now < stop);
            best = Math.min(best, (now - start) / (double) ops);
        }
        if (sink == 42) System.out.print("");
        return String.format("%8.0f ns/op", best);
    }
}
