# MsgPack codec harness

The differential and benchmark program behind `docs/test-reports/minimalist-msgpack-benchmark.md`: it drives
platform-core's `MsgPack` (whose API did not change) and is run once per codec backend, in a separate JVM, with that
backend's classpath - `org.msgpack:msgpack-core` 0.9.12 under the released `platform-core` 4.12.20, or
`system/minimalist-msgpack` under the current `platform-core`.

It is deliberately **not a Maven module**: the msgpack-core backend needs the flagged library on its classpath, and a
manifest that declared it would reappear in the field's dependency scanners. Compile and run it by hand:

```bash
# from the repository root; platform-core's runtime dependencies with and without msgpack-core
mvn -q dependency:build-classpath -f system/platform-core/pom.xml -Dmdep.outputFile=/tmp/cp.txt -Dmdep.includeScope=runtime
OLD="benchmark/msgpack-codec-harness/classes:$(cat /tmp/cp.txt):$HOME/.m2/repository/org/platformlambda/platform-core/4.12.20/platform-core-4.12.20.jar:$HOME/.m2/repository/org/msgpack/msgpack-core/0.9.12/msgpack-core-0.9.12.jar"
NEW="benchmark/msgpack-codec-harness/classes:$(tr ':' '\n' < /tmp/cp.txt | grep -v msgpack-core | paste -sd: -):system/platform-core/target/classes"
javac --release 21 -cp "$OLD" -d benchmark/msgpack-codec-harness/classes benchmark/msgpack-codec-harness/Harness.java

java -cp "$OLD" Harness corpus /tmp/corpus-old.bin 20000   # pack a seeded corpus with msgpack-core
java -cp "$NEW" Harness corpus /tmp/corpus-new.bin 20000   # the same corpus with minimalist-msgpack
cmp /tmp/corpus-old.bin /tmp/corpus-new.bin && echo identical
java -cp "$NEW" Harness verify /tmp/corpus-old.bin 20000   # each backend decodes the other's file
java -cp "$OLD" Harness verify /tmp/corpus-new.bin 20000
java -cp "$OLD" Harness bench                              # three payload shapes, best of 5 rounds of 2 s
java -cp "$NEW" Harness bench
```

Mercury artifacts are not on Maven Central, so the msgpack-core backend needs `platform-core` 4.12.20 in the local
repository (`mvn install -DskipTests` on a checkout of tag `v4.12.20`, the last release that used msgpack-core). A reactor
`mvn install` of a branch at the same version number replaces that jar; point `OLD` at a msgpack-core checkout's
`system/platform-core/target/classes` instead when that has happened. `classes/` is a build output and is not committed.
