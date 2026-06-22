---
title: Quickstart
summary: Clone, build, and run the example Mercury Composable app in under five minutes — create,
  retrieve, and delete a profile through its REST API.
layer: orientation
audience: [developer]
keywords: [quickstart, getting started, example app, build, run, profile api]
---

# Quickstart

*Tutorial: Get Mercury Composable running with a working example app in under 5 minutes.*

> **At a glance**
>
> - **What** — clone the repo, build it, run the `composable-example` app, and exercise its
>   profile REST API (create / retrieve / delete).
> - **Why it matters** — the fastest path from zero to a running composable app you can poke at.
> - **For** developers evaluating Mercury or starting a first project.

---

## Prerequisites

- **Java 21 or higher** — verify with `java --version`
- **Maven 3.9.7 or higher** — verify with `mvn --version`
- **Git** — verify with `git --version`

---

## 1. Clone and build

```shell
git clone https://github.com/Accenture/mercury-composable.git
cd mercury-composable
mvn clean install
```

> The first build downloads all dependencies and may take a few minutes.

---

## 2. Run the example app

```shell
cd examples/composable-example
java -jar target/composable-example-4.4.11.jar
```

Look for these lines in the startup output to confirm the app is running:

```log
CompileFlows - Loaded create-profile
CompileFlows - Loaded delete-profile
CompileFlows - Loaded get-profile
CompileFlows - Event scripts deployed: 3
AppStarter - Reactive HTTP server running on port-8100
```

---

## 3. Try it out

The example app is a simple profile management API. Run these commands in a new terminal while the app is running.

### Create a profile

```bash
curl -s -X POST http://127.0.0.1:8100/api/profile \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"id": 100, "name": "Hello World", "address": "100 World Blvd", "telephone": "123-456-7890"}' | jq .
```

> `jq` is optional — omit `| jq .` if you don't have it installed.

You should see a `201 Created` response:

```json
{
  "profile": {
    "address": "***",
    "name": "Hello World",
    "telephone": "***",
    "id": 100
  },
  "type": "CREATE",
  "secure": ["address", "telephone"]
}
```

### Retrieve the profile

```bash
curl -s http://127.0.0.1:8100/api/profile/100 | jq .
```

You should see a `200 OK` response:

```json
{
  "address": "100 World Blvd",
  "name": "Hello World",
  "telephone": "123-456-7890",
  "id": 100
}
```

### Delete the profile

```bash
curl -s -X DELETE http://127.0.0.1:8100/api/profile/100 | jq .
```

You should see a `200 OK` response:

```json
{
  "id": 100,
  "deleted": true
}
```

---

## What just happened?

The app loaded three event flow configurations (`create-profile`, `delete-profile`, `get-profile`) from
the `flows/` directory. Each HTTP request was routed through its matching event flow, which orchestrated
one or more self-contained Java functions. Notice that `address` and `telephone` appear as `***` in the
create response — the flow runs them through an encryption step before storing the profile. The GET request
decrypts them back. Every function is stateless; the flow's state machine tracks intermediate results
between steps. Explore the flow files at `examples/composable-example/src/main/resources/flows/`.

---

## See also

- [Getting Started](getting-started.md) — the full walkthrough with architecture diagrams.
- [Methodology](methodology.md) — the composable design philosophy behind what you ran.
- [Event Script Syntax](event-script/index.md) — write your own event flows.
- [Configuration Reference](configuration-reference.md) — every `application.properties` key with type, default, and description.
- [Architecture Overview](architecture.md) — the complete mental model of functions, flows, events, and APIs.
- [Mercury for Node.js](https://github.com/Accenture/mercury-nodejs) — the JavaScript/TypeScript version.
