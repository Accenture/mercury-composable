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

package com.accenture.minigraph.packager;

import com.accenture.minigraph.common.GraphModelGate;
import com.accenture.minigraph.common.GraphSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.platformlambda.core.serializers.SimpleMapper;
import org.platformlambda.core.util.ConfigReader;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The graph packager: a command line over the engine's canonical packager (ADR-0026) and the deployment gate's
 * checks, for build pipelines that deliver graph sets (RFC-0005). It never starts the platform.
 * <pre>
 * graph-packager pack    --set &lt;name&gt; [--manifest key=value]... [--out &lt;dir&gt;] &lt;graph.json&gt;... | &lt;folder&gt;
 * graph-packager pack    --set &lt;name&gt; --from-manifest &lt;graphs.yaml&gt; [--manifest key=value]... [--out &lt;dir&gt;]
 * graph-packager unpack  &lt;file.pack&gt; --out &lt;dir&gt;
 * graph-packager inspect &lt;file.pack&gt; [--json]
 * </pre>
 * A graph is read with the engine's JSON reader, so what is packed is what an application would have loaded, and
 * the gate's checks run before anything is written: a set holding a graph that the gate would reject at startup is
 * refused here, with every reason. Exit codes: 0 success, 1 a refused input (a rule the set breaks, or a usage
 * error), 2 an I/O or format error (a file that cannot be read or written, a graph file that is not a JSON object,
 * a package that fails the strict read).
 */
public class GraphPackager {
    public static final int OK = 0;
    public static final int REFUSED = 1;
    public static final int FAILED = 2;
    private static final String SET = "--set";
    private static final String MANIFEST = "--manifest";
    private static final String OUT = "--out";
    private static final String FROM_MANIFEST = "--from-manifest";
    private static final String JSON = "--json";
    private static final String JSON_EXT = ".json";
    private static final String FILE = "file:";
    private static final String CLASSPATH = "classpath:";
    private static final String DEFAULT_LOCATION = "classpath:/graph";
    private static final String NODES = "nodes";
    private static final String CONNECTIONS = "connections";
    private static final Gson PRETTY = new GsonBuilder().disableHtmlEscaping().serializeNulls().setPrettyPrinting()
                                            .create();
    private static final String USAGE = """
            Usage:
              graph-packager pack    --set <name> [--manifest key=value]... [--out <dir>] <graph.json>... | <folder>
              graph-packager pack    --set <name> --from-manifest <graphs.yaml> [--manifest key=value]... [--out <dir>]
              graph-packager unpack  <file.pack> --out <dir>
              graph-packager inspect <file.pack> [--json]

            Exit codes: 0 success, 1 a refused input, 2 an I/O or format error""";

    private final PrintStream out;
    private final PrintStream err;

    public GraphPackager(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public static void main(String[] args) {
        System.exit(new GraphPackager(System.out, System.err).run(args));
    }

    /**
     * Run one command.
     *
     * @param args the command and its arguments
     * @return the exit code
     */
    public int run(String... args) {
        try {
            if (args.length == 0) {
                throw new UsageException("Name a command");
            }
            var rest = Arrays.copyOfRange(args, 1, args.length);
            return switch (args[0]) {
                case "pack" -> pack(rest);
                case "unpack" -> unpack(rest);
                case "inspect" -> inspect(rest);
                case "help", "--help", "-h" -> {
                    out.println(USAGE);
                    yield OK;
                }
                default -> throw new UsageException("Unknown command '" + args[0] + "'");
            };
        } catch (UsageException e) {
            err.println(e.getMessage());
            err.println(USAGE);
            return REFUSED;
        } catch (GraphSet.RefusedException e) {
            err.println("Refused:");
            e.getReasons().forEach(reason -> err.println("  " + reason));
            return REFUSED;
        } catch (IllegalArgumentException e) {
            err.println("Refused: " + e.getMessage());
            return REFUSED;
        } catch (NoSuchFileException e) {
            err.println("Error: no such file - " + e.getMessage());
            return FAILED;
        } catch (IOException e) {
            err.println("Error: " + e.getMessage());
            return FAILED;
        }
    }

    private int pack(String[] args) throws UsageException, IOException {
        var a = parse(args, Set.of(SET, MANIFEST, OUT, FROM_MANIFEST), Set.of(MANIFEST), Set.of());
        var setName = a.single(SET);
        if (setName == null) {
            throw new UsageException("pack needs --set <name>");
        }
        var fields = manifestFields(a.all(MANIFEST));
        var fromManifest = a.single(FROM_MANIFEST);
        Map<String, Path> files;
        if (fromManifest != null) {
            if (!a.positional().isEmpty()) {
                throw new UsageException("Give the graphs or --from-manifest, not both");
            }
            files = filesListedIn(Path.of(fromManifest));
        } else {
            if (a.positional().isEmpty()) {
                throw new UsageException("Name the graph files or a folder to pack");
            }
            files = filesNamed(a.positional());
        }
        Map<String, Map<String, Object>> graphs = new TreeMap<>();
        for (var file : files.entrySet()) {
            graphs.put(file.getKey(), readGraph(file.getValue()));
        }
        // every rule is checked before a path is built from the set name
        var bytes = GraphSet.pack(setName, fields, graphs);
        var dir = Path.of(Objects.requireNonNullElse(a.single(OUT), "."));
        Files.createDirectories(dir);
        var target = dir.resolve(setName + GraphSet.EXTENSION);
        Files.write(target, bytes);
        out.println("Packed " + count(graphs.size(), "graph") + " into " + target + " (" + bytes.length + " bytes)");
        out.println("SHA-256 " + sha256(bytes));
        return OK;
    }

    private int unpack(String[] args) throws UsageException, IOException {
        var a = parse(args, Set.of(OUT), Set.of(), Set.of());
        if (a.positional().size() != 1) {
            throw new UsageException("unpack needs one .pack file");
        }
        var dirName = a.single(OUT);
        if (dirName == null) {
            throw new UsageException("unpack needs --out <dir>");
        }
        var file = Path.of(a.positional().getFirst());
        // the names are checked before any of them becomes a path
        var contents = GraphSet.read(readPackage(file));
        var dir = Path.of(dirName);
        Files.createDirectories(dir);
        for (var graph : contents.graphs().entrySet()) {
            Files.writeString(dir.resolve(graph.getKey() + JSON_EXT), GraphSet.toJson(graph.getValue()),
                    StandardCharsets.UTF_8);
        }
        out.println("Unpacked " + count(contents.graphs().size(), "graph") + " from " + file + " into " + dir);
        contents.graphs().keySet().forEach(id -> out.println("  " + id + JSON_EXT));
        return OK;
    }

    private int inspect(String[] args) throws UsageException, IOException {
        var a = parse(args, Set.of(), Set.of(), Set.of(JSON));
        if (a.positional().size() != 1) {
            throw new UsageException("inspect needs one .pack file");
        }
        var file = Path.of(a.positional().getFirst());
        var bytes = readPackage(file);
        var contents = GraphSet.read(bytes);
        var sha = sha256(bytes);
        if (a.has(JSON)) {
            // keys in sorted order, so both engines' packagers print the same report
            Map<String, Object> report = new TreeMap<>();
            report.put("file", file.toString());
            report.put("size", bytes.length);
            report.put("sha256", sha);
            report.put("manifest", contents.manifest());
            List<Map<String, Object>> graphs = new ArrayList<>();
            contents.graphs().forEach((id, model) -> {
                Map<String, Object> graph = new TreeMap<>();
                graph.put("id", id);
                graph.put(NODES, size(model, NODES));
                graph.put(CONNECTIONS, size(model, CONNECTIONS));
                graphs.add(graph);
            });
            report.put("graphs", graphs);
            out.println(PRETTY.toJson(report));
        } else {
            out.println("File      " + file);
            out.println("Size      " + bytes.length + " bytes");
            out.println("SHA-256   " + sha);
            out.println("Manifest");
            var keyWidth = contents.manifest().keySet().stream().mapToInt(String::length).max().orElse(0);
            contents.manifest().forEach((k, v) -> out.println("  " + pad(k, keyWidth) + "  " + v));
            out.println("Graphs    " + contents.graphs().size());
            var idWidth = contents.graphs().keySet().stream().mapToInt(String::length).max().orElse(0);
            contents.graphs().forEach((id, model) -> out.println("  " + pad(id, idWidth) + "  " +
                    count(size(model, NODES), "node") + ", " + count(size(model, CONNECTIONS), "connection")));
        }
        return OK;
    }

    private Map<String, Path> filesNamed(List<String> names) throws IOException {
        Map<String, Path> files = new TreeMap<>();
        for (var name : names) {
            var path = Path.of(name);
            if (Files.isDirectory(path)) {
                List<Path> found;
                try (Stream<Path> list = Files.list(path)) {
                    found = list.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(JSON_EXT))
                                .sorted().toList();
                }
                for (var file : found) {
                    addGraphFile(files, file);
                }
            } else if (Files.isRegularFile(path)) {
                if (!path.getFileName().toString().endsWith(JSON_EXT)) {
                    throw new IllegalArgumentException(name + " is not a .json graph file");
                }
                addGraphFile(files, path);
            } else {
                throw new NoSuchFileException(name);
            }
        }
        return files;
    }

    private static void addGraphFile(Map<String, Path> files, Path file) {
        var fileName = file.getFileName().toString();
        var id = fileName.substring(0, fileName.length() - JSON_EXT.length());
        var previous = files.put(id, file);
        if (previous != null) {
            throw new IllegalArgumentException("graph id '" + id + "' is given twice - " + previous + " and " + file);
        }
    }

    private Map<String, Path> filesListedIn(Path manifest) throws IOException {
        if (!Files.isRegularFile(manifest)) {
            throw new NoSuchFileException(manifest.toString());
        }
        ConfigReader reader;
        try {
            reader = new ConfigReader(FILE + manifest.toAbsolutePath());
        } catch (IllegalArgumentException e) {
            throw new IOException("Unable to read " + manifest + " - " + e.getMessage());
        }
        // the manifest's own location, as the deployment reads it; a classpath location lives inside an
        // application, which a pipeline does not have, so the folder must be on the file system
        var location = reader.getProperty("location", DEFAULT_LOCATION);
        if (location.startsWith(CLASSPATH)) {
            throw new IllegalArgumentException("the location of " + manifest + " is '" + location +
                    "', inside an application - pass the folder that holds the graphs instead, " +
                    "or a manifest whose location is a file: folder");
        }
        var folder = location.startsWith(FILE) ? Path.of(location.substring(FILE.length())) :
                manifest.toAbsolutePath().getParent().resolve(location);
        Map<String, Path> files = new TreeMap<>();
        List<String> reasons = new ArrayList<>();
        if (reader.get("graphs") instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                var id = reader.getProperty("graphs[" + i + "]");
                // the id is checked before it becomes a path
                if (!GraphModelGate.isValidGraphId(id)) {
                    reasons.add("graph id '" + id + "' in " + manifest + " - use letters, digits, '_' and '-' only");
                } else if (files.put(id, folder.resolve(id + JSON_EXT)) != null) {
                    reasons.add("graph id '" + id + "' is listed twice in " + manifest);
                }
            }
        }
        if (!reasons.isEmpty()) {
            throw new GraphSet.RefusedException(reasons);
        }
        for (var file : files.values()) {
            if (!Files.isRegularFile(file)) {
                throw new NoSuchFileException(file.toString());
            }
        }
        return files;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readGraph(Path file) throws IOException {
        var text = Files.readString(file, StandardCharsets.UTF_8);
        Object parsed;
        try {
            parsed = SimpleMapper.getInstance().getMapper().readValue(text, Map.class);
        } catch (RuntimeException e) {
            throw new IOException(file + " is not a JSON graph model - " + e.getMessage());
        }
        if (parsed instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IOException(file + " is not a JSON object");
    }

    private static byte[] readPackage(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new NoSuchFileException(file.toString());
        }
        return Files.readAllBytes(file);
    }

    private static Map<String, String> manifestFields(List<String> pairs) throws UsageException {
        Map<String, String> fields = new LinkedHashMap<>();
        for (var pair : pairs) {
            var eq = pair.indexOf('=');
            if (eq < 1) {
                throw new UsageException("--manifest takes key=value, not '" + pair + "'");
            }
            var key = pair.substring(0, eq);
            if (fields.put(key, pair.substring(eq + 1)) != null) {
                throw new UsageException("The manifest field '" + key + "' is given twice");
            }
        }
        return fields;
    }

    private static int size(Map<String, Object> model, String key) {
        return model.get(key) instanceof List<?> list ? list.size() : 0;
    }

    private static String count(int n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }

    private static String pad(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static Arguments parse(String[] args, Set<String> valued, Set<String> repeatable, Set<String> flags)
            throws UsageException {
        Map<String, List<String>> options = new HashMap<>();
        List<String> positional = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            var arg = args[i];
            if (!arg.startsWith("--")) {
                positional.add(arg);
            } else if (flags.contains(arg)) {
                if (options.put(arg, List.of()) != null) {
                    throw new UsageException("Give " + arg + " once");
                }
            } else if (valued.contains(arg)) {
                if (i + 1 >= args.length) {
                    throw new UsageException(arg + " needs a value");
                }
                var values = options.computeIfAbsent(arg, k -> new ArrayList<>());
                if (!values.isEmpty() && !repeatable.contains(arg)) {
                    throw new UsageException("Give " + arg + " once");
                }
                values.add(args[++i]);
            } else {
                throw new UsageException("Unknown option '" + arg + "'");
            }
        }
        return new Arguments(options, positional);
    }

    private record Arguments(Map<String, List<String>> options, List<String> positional) {
        String single(String name) {
            var values = options.get(name);
            return values == null || values.isEmpty() ? null : values.getFirst();
        }

        List<String> all(String name) {
            return options.getOrDefault(name, List.of());
        }

        boolean has(String name) {
            return options.containsKey(name);
        }
    }

    private static class UsageException extends Exception {
        UsageException(String message) {
            super(message);
        }
    }
}
