# MongoDB standalone server

A convenient application to run a **MongoDB-compatible server for local development and testing**, with **no
Docker image and no binary download**. It uses [`mongo-java-server`](https://github.com/bwaldvogel/mongo-java-server),
a server written in Java that speaks the MongoDB wire protocol and keeps its databases in memory. An application
connects to it with the official MongoDB driver and an ordinary connection string, as it would connect to MongoDB.

Everything it needs comes from Maven Central, so it runs wherever the Mercury build runs, including a virtual
desktop that has no Docker support and cannot download a MongoDB distribution.

> **Purely for development and testing, in a dev environment without container support** — **not** for
> production use. It **emulates a subset of MongoDB's features only**: enough for an application that uses a small
> set of them, as the scheduler example does. Where a container runtime is available, run MongoDB itself (see
> [Prefer a real MongoDB?](#prefer-a-real-mongodb)).

This is a sibling of [`kafka-standalone`](../kafka-standalone) and [`redis-standalone`](../redis-standalone) under
`helpers/`. Its first use is the [scheduler example](../../examples/scheduler-example), whose state resolver and
environment store keep their records in MongoDB.

## What it is, and what it is not

The server **emulates a subset of MongoDB**; it does not run MongoDB's own code. The scheduler example uses a very
small set of features, and each behaves as it does on MongoDB: a find by `_id`, a listing with a projection, a
plain upsert, and the conditional upsert that claims a job, whose duplicate-key error on the job's `_id` refuses a
second claim - eight instances asking at the same instant get one winner. Not every command, query operator or
aggregation stage is implemented; it has no sessions, so no transactions, and it is one server, not a replica set.
It reports itself as MongoDB 5.0.

So test an application here, then verify it against a real MongoDB before it goes to production. The scheduler
example's code is ordinary MongoDB driver code, and its tests were also run against MongoDB 8.0.

The databases are in memory: each start begins with an empty server, and nothing survives a stop.

## Build and run

> **Note**: `x.y.z` denotes the current Mercury version shown in the root `pom.xml`.

```shell
cd helpers/mongodb-standalone
mvn clean package
java -jar target/mongodb-standalone-x.y.z.jar
```

The server listens on `127.0.0.1:27017`, MongoDB's own port, and logs its connection string:

```text
Standalone MongoDB server started - connection string mongodb://127.0.0.1:27017
```

Press `Ctrl-C` to stop it.

## Choosing a port

The port defaults to `27017`. Override it with the `mongodb.port` property, for example when a MongoDB already
runs on that port:

```shell
java -Dmongodb.port=27018 -jar target/mongodb-standalone-x.y.z.jar
```

The application then connects with `mongodb://127.0.0.1:27018`.

## Test-drive the scheduler example

Start this server, then the scheduler example, which connects to `mongodb://127.0.0.1:27017` unless
`MONGODB_URI` says otherwise:

```shell
cd examples/scheduler-example
mvn clean package
java -jar target/scheduler-example-x.y.z.jar
```

Start a second instance on another port to watch the two instances claim each job, one winner per firing:

```shell
java -Dserver.port=8084 -jar target/scheduler-example-x.y.z.jar
```

See the scheduler example's README for the records it keeps and the admin endpoints that show them.

## Prefer a real MongoDB?

Where Docker is available, run MongoDB itself instead:

```shell
docker run -p 27017:27017 mongo:8.0
```
