# JSONPath differential harness

The differential and benchmark program behind `docs/test-reports/minimalist-jsonpath-differential.md` (RFC-0010,
WP2). It calls Jayway JsonPath 3.0.0 the way `MultiLevelMap` called it until the swap (`JsonPath.read`, with
`PathNotFoundException` read as not found), and `system/minimalist-jsonpath` the way `MultiLevelMap` calls it now
(`compileLenient(...).read(document, notFound)`), on the same documents.

It is deliberately **not a Maven module**: Jayway needs json-smart on its classpath, and a manifest that declared it
would reappear in the field's dependency scanners. Compile and run it by hand, from the repository root:

```bash
mvn -q -o verify -pl system/minimalist-jsonpath -DskipTests
R=$HOME/.m2/repository
CP="benchmark/jsonpath-differential/classes:$R/com/jayway/jsonpath/json-path/3.0.0/json-path-3.0.0.jar"
CP="${CP}:$R/net/minidev/json-smart/2.6.0/json-smart-2.6.0.jar:$R/net/minidev/accessors-smart/2.6.0/accessors-smart-2.6.0.jar"
CP="${CP}:$R/org/ow2/asm/asm/9.11.1/asm-9.11.1.jar:$R/org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar"
CP="${CP}:system/minimalist-jsonpath/target/classes"
javac --release 21 -cp "$CP" -d benchmark/jsonpath-differential/classes benchmark/jsonpath-differential/Harness.java

java -cp "$CP" Harness repo . 1000      # every '$' path of the repository, 1000 documents each
java -cp "$CP" Harness cts system/minimalist-jsonpath/src/test/resources/cts/cts.json   # the suite through Jayway
java -cp "$CP" Harness jayway-only      # the Jayway-only forms the new engine refuses
java -cp "$CP" Harness bench .          # both engines on the repository's paths, compile once and evaluate many
```

(In zsh, keep the braces in `"${CP}:..."`: an unbraced `$CP:s...` is read as a substitution modifier.)

- **repo** scans the flows, graph models, fixtures, tutorials, guides and tests for `$` paths (a path holding a
  `{...}` substitution is skipped, because its text exists only at run time). Each path runs over documents grown
  along its own names, half of them mutated (a member dropped, set to null or retyped), and over a shared random
  corpus. As in `MultiLevelMap`, only a non-empty map is a document. Every differing evaluation is tallied by the
  kind of answer each engine gave, with the first example per path.
- **cts** runs the compliance suite's queries through Jayway and sorts them: refused by both, accepted by Jayway
  alone, refused by Jayway, the same answer, a different answer.
- The JSON reader for `cts.json` is a small one in the harness, so the harness needs no JSON library.

Mercury artifacts are not on Maven Central; the new engine comes from this checkout's `target/classes`. Jayway and
json-smart come from the local repository, where any build before the swap left them. `classes/` is a build output
and is not committed.
