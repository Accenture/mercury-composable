# Redis standalone server

A convenient application to run **Redis as a standalone server for local development and testing** — with
**no Docker image required**. It uses the [`embedded-redis`](https://github.com/codemonstur/embedded-redis)
library, which bundles a real `redis-server` binary for macOS, Linux and Windows and runs it as a
subprocess. One binary has a prerequisite: **on an Apple Silicon Mac it needs OpenSSL 3 from Homebrew** — see
[Platform prerequisites](#platform-prerequisites).

> For development and testing only — **not** for production use.

This is the sibling of [`kafka-standalone`](../kafka-standalone) under `helpers/`: the two give you a local
Kafka broker and a local Redis server without external infrastructure, which is all the
[`sync-over-async`](../../extensions/sync-over-async) extension needs to run.

## Platform prerequisites

The same `embedded-redis` library also starts the embedded Redis that the unit tests of the Redis-backed
modules use (`extensions/redis-connection`, `sync-over-async`, `minigraph-state-redis`, `distributed-cache`
and their examples), so these prerequisites apply to a full `mvn clean install` of the repository as well as
to this helper. The bundled binaries are what `embedded-redis` 1.4.3 ships; 1.4.4 ships the same binaries.

| Platform | Bundled `redis-server` | Prerequisite on the machine |
| --- | --- | --- |
| macOS on Apple Silicon (arm64) | 6.2.6 | **OpenSSL 3 from Homebrew** — the binary is dynamically linked against `/opt/homebrew/opt/openssl@3/lib/libssl.3.dylib` and `libcrypto.3.dylib`. Install it with `brew install openssl@3`. macOS 12 or later. |
| macOS on Intel (x86_64) | 6.2.6 | none — self-contained. macOS 12 or later. |
| Linux x86_64 | 6.2.6 | the OpenSSL 3 shared libraries (`libssl.so.3`, `libcrypto.so.3` — the `libssl3` / `openssl-libs` package) and glibc 2.34 or later, e.g. Ubuntu 22.04+, Debian 12+, RHEL 9+. GitHub's `ubuntu-latest` runners have both, which is why CI needs no extra step. |
| Linux arm64, Linux 32-bit x86 | 6.2.7 | none — statically linked. |
| Windows x86_64 | 5.0.14.1 | none — only Windows system DLLs (works on a locked-down Windows VDI). |

**Symptom when the library is missing:** the `redis-server` subprocess exits before it can print anything
and the helper (or the test) fails with

```text
Failed to start Redis service
  caused by: Redis-server process appears not to have started.
```

On a Mac, check with `brew list openssl@3`; on Linux, with `ldconfig -p | grep libssl.so.3`. Upgrading the
library does not remove the prerequisite — the upstream project documents the same dependency in its
[SSL/TLS troubleshooting](https://github.com/codemonstur/embedded-redis#ssltls-troubleshooting) section.

## Build and run

> **Note**: `x.y.z` denotes the current Mercury version shown in the root `pom.xml`.

```shell
cd helpers/redis-standalone
mvn clean package
java -jar target/redis-standalone-x.y.z.jar
```

The server starts on `127.0.0.1:6379`. Press `Ctrl-C` to stop (it shuts the `redis-server` subprocess down
cleanly).

## Choosing a port

The port defaults to `6379`. Override it with the `redis.port` property:

```shell
java -Dredis.port=6380 -jar target/redis-standalone-x.y.z.jar
```

## Prefer Docker?

You can of course run Redis with Docker instead — this helper simply removes that requirement when you do
not want it:

```shell
docker run -p 6379:6379 redis
```
