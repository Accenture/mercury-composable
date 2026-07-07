# Redis standalone server

A convenient application to run **Redis as a standalone server for local development and testing** — with
**no Docker image required**. It uses the [`embedded-redis`](https://github.com/codemonstur/embedded-redis)
library, which bundles a real `redis-server` binary (macOS and Linux, arm64 + amd64) and runs it as a
subprocess.

> For development and testing only — **not** for production use.

This is the sibling of [`kafka-standalone`](../kafka-standalone) under `helpers/`: the two give you a local
Kafka broker and a local Redis server without external infrastructure, which is all the
[`sync-over-async`](../../extensions/sync-over-async) extension needs to run.

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
