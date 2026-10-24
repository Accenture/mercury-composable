# Dynatrace OneAgent lab

A local lab for the OneAgent integration (RFC-0009), run from a dev machine against a Dynatrace environment:
a Kafka broker and two Mercury applications run in containers on Colima, with Dynatrace OneAgent installed
full-stack in Colima's Linux VM. One request enters over REST, runs an Event Script flow, is published to
Kafka, and runs a second flow in the other application.

| Container | Role |
|---|---|
| `lab-broker` | the repository's `kafka-standalone` broker |
| `lab-edge` | the **REST entry point**: `POST /api/lab/orders` runs the `lab-order` flow |
| `lab-worker` | the **Kafka entry point**: the `minimalist-kafka` flow adapter consumes `lab.orders` and runs `lab-fulfil` |

The `lab-order` flow covers each case the experiment checks: a plain hop (`lab.validate`), an RPC from inside
a function (`lab.lookup`), a `Mono` that completes 200 ms later on another thread (`lab.price`), the negative
control (`lab.audit`, listed in `invocation.observer.unlinked`), the Kafka publish and the answer
(`lab.receipt`). Every function writes a log line; the logs are JSON lines (`log.format=compact`), so each
carries the log context.

## Prerequisites

- Colima with the Docker runtime. This lab was built on an `aarch64` VM (Ubuntu 24.04).
- OneAgent installed full-stack **inside Colima's VM** (`colima ssh`), from the *Install OneAgent* page of
  your Dynatrace environment, signature verified. OneAgent then injects itself into every JVM a container
  starts, so the images carry no agent.
- The base image, built once:

  ```bash
  docker build -t mercury-lab/ubuntu-java:26.04-25 base
  ```

Keep tokens and your environment's address out of the repository: `.env` is ignored by git. OneAgent is
commercially licensed, so its installer is never committed: the root `.gitignore` ignores the `oneagent/`
download folder and any OneAgent installer (`Dynatrace-OneAgent-Linux*`, `-Windows*`, `-AIX*`) elsewhere in the tree.

## Run

```bash
./lab.sh build            # the jars (with the lab's own pom.xml, see below) and the three images
./lab.sh up oneagent      # or: up none (the baseline: OneAgent without the adapter)
./lab.sh order            # one order; repeat a few times
./lab.sh logs lab-edge    # or lab-worker | lab-broker
./lab.sh down
```

Each hop's endpoint is named after the application and the route by default (`Topic: lab-edge - lab.validate`);
`DESTINATION=application ./lab.sh up oneagent` names it after the application only, and `DESTINATION=route` after
the route only.

## Notes

- **The lab is not in the root reactor, on purpose.** It is a dev-machine tool: never deployed, and never to
  be registered by the field's static code analysis, which scans every module the root `pom.xml` declares
  and keeps scanning it once registered. So the lab has its own aggregator, `pom.xml` in this folder. It
  builds the lab, the broker and the Mercury modules they use from this checkout, and installs nothing.
  Without it, Maven takes the released platform-core of the same version from `~/.m2`, which may lack what
  the branch adds (`NoClassDefFoundError: org/platformlambda/core/spi/InvocationObserver`). Its version
  follows the root pom, so the release version sweep covers these poms too.
- **The adapter is switched by environment variables** that `lab.sh up` sets (`DYNATRACE_ONEAGENT_ENABLED`,
  `DYNATRACE_ONEAGENT_DESTINATION`), which both applications' `application.properties` read.
- **One network namespace.** The broker advertises `127.0.0.1:9092`, so the two applications share the
  broker container's network namespace and the broker container publishes `lab-edge`'s port 8100.
  `lab-worker` runs no HTTP server.
- **What to check in Dynatrace:** the procedure in `docs/guides/observability.md#live-tracing` - one
  connected tree per request, the `dt.trace_id` and `dt.span_id` on every log line, the negative control as a
  separate fragment, and the `Mono` hop's duration.
