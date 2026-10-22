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

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.PathNotFoundException;
import org.platformlambda.mini.jsonpath.JsonPathException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The differential and benchmark program behind docs/test-reports/minimalist-jsonpath-differential.md (RFC-0010,
 * WP2): Jayway JsonPath 3.0.0, as MultiLevelMap called it until the swap, against system/minimalist-jsonpath, as it
 * calls it now. See README.md beside it for the classpath.
 * <p>
 * {@code java Harness repo <repository root> [documents per path]} evaluates every '$' path the repository uses over
 * documents grown from the path and randomly mutated, and lists each query on which the engines differ.
 * {@code java Harness cts <cts.json>} runs the compliance suite's valid queries through both.
 * {@code java Harness jayway-only} shows the Jayway-only forms the new engine refuses.
 * {@code java Harness bench <repository root>} times both engines on the repository's paths.
 */
public class Harness {
    private static final Object NOT_FOUND = new Object() {
        @Override
        public String toString() {
            return "<not found>";
        }
    };
    private static final String REFUSED = "<refused>";
    private static final long SEED = 20261022L;

    public static void main(String[] args) throws IOException {
        switch (args.length == 0 ? "" : args[0]) {
            case "repo" -> repository(Path.of(args[1]), args.length > 2 ? Integer.parseInt(args[2]) : 200);
            case "cts" -> suite(Path.of(args[1]));
            case "jayway-only" -> jaywayOnly();
            case "bench" -> bench(Path.of(args[1]));
            default -> System.out.println("usage: Harness repo <root> [n] | cts <cts.json> | jayway-only | bench <root>");
        }
    }

    // ---- the two engines, called the way MultiLevelMap calls them ----

    static Object jayway(Object document, String path) {
        try {
            return plain(com.jayway.jsonpath.JsonPath.read(document, path));
        } catch (PathNotFoundException e) {
            return NOT_FOUND;
        } catch (InvalidPathException e) {
            return REFUSED;
        } catch (RuntimeException e) {
            return "<Jayway threw " + e.getClass().getSimpleName() + ": " + e.getMessage() + ">";
        }
    }

    static Object mercury(Object document, String path) {
        try {
            return org.platformlambda.mini.jsonpath.JsonPath.compileLenient(path).read(document, NOT_FOUND);
        } catch (JsonPathException e) {
            return REFUSED;
        }
    }

    // json-smart's JSONArray is an ArrayList; MultiLevelMap copied the top one, the deeper ones are document values
    private static Object plain(Object value) {
        return value instanceof List<?> list ? new ArrayList<>(list) : value;
    }

    // ---- the repository's paths ----

    // a '$' path inside a flow, a graph model, a test, a guide or a help page, up to the end of the mapping source
    private static final Pattern PATH_START = Pattern.compile("\\$[.\\[]");

    static Set<String> repositoryPaths(Path root) throws IOException {
        Set<String> paths = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(Harness::scanned).toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                Matcher m = PATH_START.matcher(text);
                while (m.find()) {
                    String path = scanPath(text, m.start());
                    if (path.length() > 2 && !path.contains("{") && !path.contains("<")) {
                        paths.add(path);
                    }
                }
            }
        }
        return paths;
    }

    private static boolean scanned(Path file) {
        String p = file.toString().replace('\\', '/');
        boolean kind = p.endsWith(".yml") || p.endsWith(".yaml") || p.endsWith(".json") || p.endsWith(".md") ||
                p.endsWith(".java") || p.endsWith(".kt");
        boolean place = p.contains("/system/") || p.contains("/extensions/") || p.contains("/examples/") ||
                p.contains("/templates/") || p.contains("/docs/");
        boolean excluded = p.contains("/target/") || p.contains("/node_modules/") || p.contains("/assets/") ||
                p.contains("/minimalist-jsonpath/") || p.contains("/cts/") || p.contains("/benchmark/");
        return kind && place && !excluded && Files.isRegularFile(file);
    }

    // from '$' to the first blank, quote or delimiter outside brackets, quotes kept inside brackets
    private static String scanPath(String text, int start) {
        int depth = 0;
        char quote = 0;
        int i = start;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (depth > 0 && (c == '\'' || c == '"')) {
                quote = c;
            } else if (c == '[' || c == '(') {
                depth++;
            } else if (c == ']' || c == ')') {
                if (depth == 0) {
                    break;
                }
                depth--;
            } else if (depth == 0 && (Character.isWhitespace(c) || "'\"`,;<>|\\".indexOf(c) >= 0)) {
                break;
            } else if (c == '\n') {
                break;
            }
            i++;
        }
        String path = text.substring(start, i);
        return path.endsWith(".") || path.endsWith(":") ? path.substring(0, path.length() - 1) : path;
    }

    static void repository(Path root, int perPath) throws IOException {
        Set<String> paths = repositoryPaths(root);
        Random random = new Random(SEED);
        List<Object> shared = new ArrayList<>();
        List<String> vocabulary = Grow.vocabulary(paths);
        while (shared.size() < perPath) {
            shared.add(Grow.randomTree(random, vocabulary, 4));
        }
        int evaluations = 0;
        int nonEmpty = 0;
        Map<String, String> differences = new TreeMap<>();
        Set<String> bothRefuse = new LinkedHashSet<>();
        Map<String, Integer> kinds = new TreeMap<>();
        for (String path : paths) {
            List<Object> documents = new ArrayList<>(shared);
            for (int i = 0; i < perPath; i++) {
                documents.add(Grow.mutate(random, Grow.grow(random, path)));
            }
            for (Object document : documents) {
                // MultiLevelMap's root is a non-empty map: it answers not found before asking the engine otherwise
                if (!(document instanceof Map<?, ?> top) || top.isEmpty()) {
                    continue;
                }
                Object a = jayway(document, path);
                Object b = mercury(document, path);
                if (REFUSED.equals(a) && REFUSED.equals(b)) {
                    bothRefuse.add(path);
                    continue;
                }
                evaluations++;
                if (b != NOT_FOUND && b != REFUSED && !(b instanceof List<?> l && l.isEmpty())) {
                    nonEmpty++;
                }
                if (!same(a, b)) {
                    kinds.merge("Jayway " + kind(a) + ", RFC 9535 " + kind(b), 1, Integer::sum);
                    differences.putIfAbsent(path, "Jayway " + a + " | RFC 9535 " + b + " | on " + document);
                }
            }
        }
        System.out.println("repository paths: " + paths.size() + ", evaluations: " + evaluations +
                ", with a non-empty result: " + nonEmpty);
        System.out.println("fragments both engines refuse (prose, not paths): " + bothRefuse);
        System.out.println("paths on which the engines differ: " + differences.size());
        kinds.forEach((k, n) -> System.out.println("  differing evaluations, " + k + ": " + n));
        differences.forEach((path, how) -> System.out.println("  " + path + "\n      " + truncate(how)));
        System.out.println("paths:");
        paths.forEach(p -> System.out.println("  " + p));
    }

    private static String kind(Object result) {
        if (result == NOT_FOUND) {
            return "not found";
        }
        if (REFUSED.equals(result)) {
            return "refused";
        }
        if (result instanceof String s && s.startsWith("<Jayway threw")) {
            return "threw";
        }
        if (result instanceof List<?> list) {
            return list.isEmpty() ? "an empty list" : "a list";
        }
        return "a value";
    }

    private static boolean same(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private static String truncate(String s) {
        return s.length() > 400 ? s.substring(0, 400) + " ..." : s;
    }

    // ---- the compliance suite through Jayway ----

    @SuppressWarnings("unchecked")
    static void suite(Path cts) throws IOException {
        Map<String, Object> file = (Map<String, Object>) Json.parse(Files.readString(cts, StandardCharsets.UTF_8));
        Map<String, Integer> tally = new TreeMap<>();
        Map<String, List<String>> examples = new TreeMap<>();
        for (Map<String, Object> c : (List<Map<String, Object>>) file.get("tests")) {
            if (Boolean.TRUE.equals(c.get("invalid_selector"))) {
                String kind = REFUSED.equals(jayway(Map.of(), (String) c.get("selector"))) ?
                        "invalid: both refuse" : "invalid: Jayway accepts, RFC 9535 refuses";
                count(tally, examples, kind, (String) c.get("selector"));
                continue;
            }
            String selector = (String) c.get("selector");
            Object document = c.get("document");
            Object a = jayway(document, selector);
            Object b = mercury(document, selector);
            String kind;
            if (REFUSED.equals(a)) {
                kind = "valid: Jayway refuses";
            } else if (a instanceof String s && s.startsWith("<Jayway threw")) {
                kind = "valid: Jayway throws";
            } else if (same(a, b)) {
                kind = "valid: same answer";
            } else {
                kind = "valid: different answer";
            }
            count(tally, examples, kind, selector + "   Jayway " + a + " | RFC 9535 " + b);
        }
        tally.forEach((kind, n) -> {
            System.out.println(kind + ": " + n);
            examples.get(kind).stream().limit(kind.contains("same") || kind.contains("both") ? 0 : 400)
                    .forEach(e -> System.out.println("    " + truncate(e)));
        });
    }

    private static void count(Map<String, Integer> tally, Map<String, List<String>> examples, String kind,
                              String example) {
        tally.merge(kind, 1, Integer::sum);
        examples.computeIfAbsent(kind, k -> new ArrayList<>()).add(example);
    }

    // ---- the Jayway-only forms ----

    static void jaywayOnly() {
        Map<String, Object> document = Map.of("items", List.of(
                Map.of("code", "a", "tags", List.of("x", "y"), "n", 1),
                Map.of("code", "b", "tags", List.of("y"), "n", 2),
                Map.of("code", "c", "tags", List.of(), "n", 3)));
        String[] forms = {
                "$.items[?(@.code in ['a','b'])].n", "$.items[?(@.code nin ['a','b'])].n",
                "$.items[?(@.tags subsetof ['x','y'])].n", "$.items[?(@.tags anyof ['x'])].n",
                "$.items[?(@.tags noneof ['x'])].n", "$.items[?(@.tags size 1)].n", "$.items[?(@.tags empty true)].n",
                "$.items[?(@.code =~ /a|b/)].n", "$.items.length()", "$.items[*].n.min()", "$.items[*].n.max()",
                "$.items[*].n.avg()", "$.items[*].n.stddev()", "$.items[*].n.sum()", "$.items[0].keys()",
                "$.items[*].code.first()", "$.items[*].code.last()", "$.items[*].code.index(1)",
                "$.items[0].code.concat('!')", "$.items[*].n.append(4)", "$.items[?(@.n == [1])].code",
                "$.items[?(@.code == 'a' || @.code == 'b')].n", "$.items[?match(@.code, 'a|b')].n",
                "$.items[?length(@.tags) == 1].n", "$.items[?count(@.tags[*]) == 0].n"};
        for (String form : forms) {
            System.out.println(form + "\n    Jayway " + jayway(document, form) + " | RFC 9535 " +
                    mercury(document, form));
        }
    }

    // ---- the benchmark ----

    static void bench(Path root) throws IOException {
        // the paths both engines compile; the scan also finds prose fragments such as '$.input.body..'
        List<String> paths = repositoryPaths(root).stream().filter(p -> !REFUSED.equals(jayway(Map.of("x", 1), p)) &&
                !REFUSED.equals(mercury(Map.of("x", 1), p))).toList();
        Random random = new Random(SEED);
        List<Object> documents = new ArrayList<>();
        for (String path : paths) {
            documents.add(Grow.grow(random, path));
        }
        List<com.jayway.jsonpath.JsonPath> jaywayCompiled = new ArrayList<>();
        List<org.platformlambda.mini.jsonpath.JsonPath> mercuryCompiled = new ArrayList<>();
        for (String path : paths) {
            jaywayCompiled.add(com.jayway.jsonpath.JsonPath.compile(path));
            mercuryCompiled.add(org.platformlambda.mini.jsonpath.JsonPath.compileLenient(path));
        }
        Runnable jaywayRead = () -> {
            for (int i = 0; i < paths.size(); i++) {
                try {
                    jaywayCompiled.get(i).read(documents.get(i));
                } catch (PathNotFoundException e) {
                    // not found is an answer
                }
            }
        };
        Runnable mercuryRead = () -> {
            for (int i = 0; i < paths.size(); i++) {
                mercuryCompiled.get(i).read(documents.get(i), NOT_FOUND);
            }
        };
        Runnable jaywayCompile = () -> paths.forEach(com.jayway.jsonpath.JsonPath::compile);
        Runnable mercuryCompile = () -> paths.forEach(org.platformlambda.mini.jsonpath.JsonPath::compileLenient);
        System.out.println("paths: " + paths.size() + "; best of 5 rounds of 2 s after 3 s of warm-up, per path");
        report("evaluate, Jayway   ", jaywayRead, paths.size());
        report("evaluate, RFC 9535 ", mercuryRead, paths.size());
        report("compile,  Jayway   ", jaywayCompile, paths.size());
        report("compile,  RFC 9535 ", mercuryCompile, paths.size());
    }

    private static void report(String label, Runnable work, int per) {
        run(work, 3_000_000_000L);
        double best = Double.MAX_VALUE;
        for (int round = 0; round < 5; round++) {
            long start = System.nanoTime();
            long n = run(work, 2_000_000_000L);
            best = Math.min(best, (System.nanoTime() - start) / (double) n / per);
        }
        System.out.printf("%s %8.1f ns per path%n", label, best);
    }

    private static long run(Runnable work, long budget) {
        long end = System.nanoTime() + budget;
        long n = 0;
        while (System.nanoTime() < end) {
            work.run();
            n++;
        }
        return n;
    }

    // ---- documents ----

    /** Documents for a path: grown along its names, so it resolves, then mutated. */
    static final class Grow {
        private static final Pattern NAME = Pattern.compile("\\.\\.?([A-Za-z_][A-Za-z0-9_-]*)|\\['([^']*)'\\]");
        private static final Pattern FILTER_MEMBER = Pattern.compile("@\\.([A-Za-z_][A-Za-z0-9_]*)");
        private static final Pattern STRING_LITERAL = Pattern.compile("'([^']*)'");

        private Grow() { }

        static List<String> vocabulary(Set<String> paths) {
            Set<String> names = new LinkedHashSet<>();
            for (String path : paths) {
                Matcher m = NAME.matcher(path);
                while (m.find()) {
                    names.add(m.group(1) != null ? m.group(1) : m.group(2));
                }
            }
            return new ArrayList<>(names);
        }

        // a nested structure that follows the path's names, with a list wherever a bracket selects
        static Object grow(Random random, String path) {
            List<String> steps = new ArrayList<>();
            Matcher m = Pattern.compile("\\.\\.?([A-Za-z_][A-Za-z0-9_-]*)|\\[([^\\]]*)\\]").matcher(path);
            while (m.find()) {
                steps.add(m.group(1) != null ? "." + m.group(1) : "[" + m.group(2) + "]");
            }
            return growFrom(random, steps, 0, path);
        }

        private static Object growFrom(Random random, List<String> steps, int at, String path) {
            if (at == steps.size()) {
                return leaf(random, path);
            }
            String step = steps.get(at);
            if (step.startsWith(".")) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put(step.substring(1), growFrom(random, steps, at + 1, path));
                if (random.nextBoolean()) {
                    map.put("other", leaf(random, path));
                }
                return map;
            }
            String inside = step.substring(1, step.length() - 1);
            if (inside.startsWith("'")) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put(inside.replace("'", ""), growFrom(random, steps, at + 1, path));
                return map;
            }
            List<Object> list = new ArrayList<>();
            int size = random.nextInt(4);
            for (int i = 0; i < size; i++) {
                Object element = growFrom(random, steps, at + 1, path);
                if (inside.startsWith("?") && element instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) element;
                    Matcher f = FILTER_MEMBER.matcher(inside);
                    while (f.find()) {
                        map.put(f.group(1), leaf(random, inside));
                    }
                }
                list.add(element);
            }
            return list;
        }

        // a leaf: a literal of the path (so filters match), a number, a boolean, null or a word
        private static Object leaf(Random random, String path) {
            List<String> literals = new ArrayList<>();
            Matcher m = STRING_LITERAL.matcher(path);
            while (m.find()) {
                literals.add(m.group(1));
            }
            return switch (random.nextInt(6)) {
                case 0 -> literals.isEmpty() ? "red" : literals.get(random.nextInt(literals.size()));
                case 1 -> (long) random.nextInt(50);
                case 2 -> random.nextDouble() * 100;
                case 3 -> random.nextBoolean();
                case 4 -> null;
                default -> "w" + random.nextInt(5);
            };
        }

        // drop, null or retype one member somewhere, half of the time
        static Object mutate(Random random, Object document) {
            if (random.nextBoolean()) {
                return document;
            }
            Object node = document;
            for (int depth = 0; depth < 8; depth++) {
                if (node instanceof Map<?, ?> map && !map.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) map;
                    String key = new ArrayList<>(m.keySet()).get(random.nextInt(m.size()));
                    if (random.nextInt(3) == 0) {
                        switch (random.nextInt(3)) {
                            case 0 -> m.remove(key);
                            case 1 -> m.put(key, null);
                            default -> m.put(key, "retyped");
                        }
                        return document;
                    }
                    node = m.get(key);
                } else if (node instanceof List<?> list && !list.isEmpty()) {
                    node = list.get(random.nextInt(list.size()));
                } else {
                    return document;
                }
            }
            return document;
        }

        static Object randomTree(Random random, List<String> vocabulary, int depth) {
            if (depth == 0 || (depth < 4 && random.nextInt(4) == 0)) {
                return leaf(random, "");
            }
            if (depth < 4 && random.nextInt(3) == 0) {
                List<Object> list = new ArrayList<>();
                int size = random.nextInt(4);
                for (int i = 0; i < size; i++) {
                    list.add(randomTree(random, vocabulary, depth - 1));
                }
                return list;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            int size = 1 + random.nextInt(3);
            for (int i = 0; i < size; i++) {
                map.put(vocabulary.get(random.nextInt(vocabulary.size())), randomTree(random, vocabulary, depth - 1));
            }
            return map;
        }
    }

    /** A small JSON reader for cts.json: objects, arrays, strings, numbers as Long or Double, true, false, null. */
    static final class Json {
        private final String text;
        private int pos;

        private Json(String text) {
            this.text = text;
        }

        static Object parse(String text) {
            return new Json(text).value();
        }

        private Object value() {
            blank();
            char c = text.charAt(pos);
            if (c == '{') {
                Map<String, Object> map = new LinkedHashMap<>();
                pos++;
                blank();
                if (text.charAt(pos) == '}') {
                    pos++;
                    return map;
                }
                do {
                    blank();
                    String key = (String) value();
                    blank();
                    pos++;
                    map.put(key, value());
                    blank();
                } while (text.charAt(pos++) == ',');
                return map;
            }
            if (c == '[') {
                List<Object> list = new ArrayList<>();
                pos++;
                blank();
                if (text.charAt(pos) == ']') {
                    pos++;
                    return list;
                }
                do {
                    list.add(value());
                    blank();
                } while (text.charAt(pos++) == ',');
                return list;
            }
            if (c == '"') {
                return string();
            }
            for (String word : List.of("true", "false", "null")) {
                if (text.startsWith(word, pos)) {
                    pos += word.length();
                    return word.equals("null") ? null : Boolean.valueOf(word);
                }
            }
            int start = pos;
            while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
                pos++;
            }
            String number = text.substring(start, pos);
            return number.matches("-?\\d+") && number.length() < 19 ? (Object) Long.parseLong(number) :
                    (Object) Double.parseDouble(number);
        }

        private String string() {
            StringBuilder sb = new StringBuilder();
            pos++;
            while (true) {
                char c = text.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = text.charAt(pos++);
                    switch (e) {
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                        default -> sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        private void blank() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }
    }
}
