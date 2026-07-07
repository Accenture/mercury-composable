---
title: Minimalist Service Mesh
summary: Set up service discovery and inter-instance routing using Kafka as a connector, with a
  built-in presence monitor — no sidecars. Opt-in only when sync-over-async RPC or service
  discovery is genuinely needed.
layer: operate
audience: [architect, developer]
keywords: [service mesh, kafka, service discovery, presence monitor, connectors, routing, distributed monolith]
---

# Minimalist Service Mesh

*Guide: When to use the service mesh, and how to set it up with Kafka and a presence monitor.*

> **At a glance**
>
> - **What** — an opt-in Kafka + presence-monitor layer that adds (1) synchronous request-response
>   across application instances and (2) service discovery between pods.
> - **Default is no mesh.** `cloud.connector=none` is the default. Most applications that scale
>   horizontally as independent instances do not need a service mesh at all.
> - **Enabling the mesh is a deliberate architectural choice** — only when your application genuinely
>   needs cross-instance sync RPC or peer discovery.
> - **Risk** — sync over async creates cross-instance coupling; without discipline it turns a cloud
>   application into a distributed monolith.
> - **For** architects deciding whether to introduce the mesh, and developers setting it up.

## When to use — and when not to {#when-to-use}

The service mesh solves exactly **two problems**:

1. **Synchronous request-response across application instances.** When application A must call a
   function in application B and wait for the result over Kafka — which is inherently asynchronous.
   This is the same pattern as IBM MQ or Redis pub/sub for RPC: a synchronous semantic layered over
   an asynchronous transport.

2. **Service discovery.** Knowing which instances are running, detecting peers joining and leaving,
   and building distributed resilience patterns — leader selection, failover, pod-aware broadcast.

**If your application does not need either of these, do not enable the service mesh.**

Cloud-native design means each application instance is fully self-contained. Functions communicate
over the in-memory event bus. The infrastructure — a load balancer or Kubernetes ingress — distributes
inbound HTTP load across identical, stateless instances. No instance needs to know about another.

**The risk.** Superimposing synchronous calls over Kafka is architecturally expensive. Cross-instance
synchronous RPC creates latency dependencies between scaling units: if one pod is slow, callers are
slow; errors propagate across instance boundaries; horizontal scaling no longer provides isolation.
Overuse of this pattern degrades a cloud application into a **distributed monolith** — all the
operational cost of distribution with the tight coupling of a monolith. See [ADR-0006](../arch-decisions/ADR.md#adr-0006) for the full rationale.

`cloud.connector=none` is the framework default because most applications do not need a service mesh.
Enabling `cloud.connector=kafka` is a deliberate, opt-in architectural decision.

---

## Using Kafka as a minimalist service mesh {#setup}

When the use case does warrant a service mesh, the framework provides a Kafka-based implementation
without sidecars. Each application instance maintains a distributed routing table; the presence
monitor is the lightweight control plane that keeps it current.

Typically, a service mesh system uses a "side-car" to sit next to the application container in the same POD to provide
service discovery and network proxy services.

Instead of using a side-car proxy, the system maintains a distributed routing table in each application instance.
When a function requests the service of another function which is not in the same memory space, the "cloud.connector"
module will bridge the event to the peer application through a network event system like Kafka.

As shown in the following table, if "service.1" and "service.2" are in the same memory space of an application,
they will communicate using the in-memory event bus.

If they are in different applications and the applications are configured with Kafka, the two functions will 
communicate via the "cloud.connector" service. 

|    In-memory event bus     |              Network event stream               |
|:--------------------------:|:-----------------------------------------------:|
| "service.1" -> "service.2" | "service.1" -> "cloud.connector" -> "service.2" |

The system supports Kafka out of the box. For example, to select kafka, you can configure application.properties like this:

```properties
cloud.connector=kafka
```

The "cloud.connector" parameter can be set to "none" or "kafka".
The default parameter of "cloud.connector" is "none". This means the application is not using
any network event system "connector", thus running independently.

Let's set up a minimalist service mesh with Kafka to see how it works.

## Set up a standalone Kafka server for development

You need a Kafka cluster as the network event stream system. For development and testing, you can build
and run a standalone Kafka server like this. Note that the `mvn clean package` command is optional because
the executable JAR should be available after the `mvn clean install` command in [Getting Started](getting-started.md).

> **Note**: `x.y.z` denotes the current Mercury version shown in the root `pom.xml`.

```shell
cd helpers/kafka-standalone
mvn clean package
java -jar target/kafka-standalone-x.y.z-exec.jar
```

The standalone Kafka server will start at port 9092. You may adjust the "server.properties" in the standalone-kafka
project when necessary.

When the kafka server is started, it will create a temporary directory "/tmp/kafka-logs".

> *Note*: The kafka server is designed for development purpose only. The kafka message log store
          will be cleared when the server is restarted.

## Prepare the kafka-presence application

The "kafka-presence" is a "presence monitor" application. It is a minimalist "control plane" in service mesh
terminology.

*What is a presence monitor?* A presence monitor is the control plane that assigns unique "topic" for each
user application instance.

It monitors the "presence" of each application. If an application fails or stops, the presence monitor will 
advertise the event to the rest of the system so that each application container will update its corresponding
distributed routing table, thus bypassing the failed application and its services.

If an application has more than one container instance deployed, they will work together to share load evenly.

You will start the presence monitor like this:

```shell
cd connectors/adapters/kafka/kafka-presence
java -jar target/kafka-presence-x.y.z.jar
```

By default, the kafka-connector will run at port 8080. Partial start-up log is shown below:

```text
AppStarter:344 - Modules loaded in 2,370 ms
AppStarter:334 - Websocket server running on port-8080
ServiceLifeCycle:73 - service.monitor, partition 0 ready
HouseKeeper:72 - Registered monitor (me) 2023032896b12f9de149459f9c8b71ad8b6b49fa
```

The presence monitor will use the topic "service.monitor" to connect to the Kafka server and register itself
as a presence monitor.

Presence monitor is resilient. You can run more than one instance to back up each other.
If you are not using Docker or Kubernetes, you need to change the "server.port" parameter of the second instance
to 8081 so that the two application instances can run in the same laptop.

## Launch the rest-spring-3-example and lambda-example with kafka

Let's run the rest-spring-3-example and lambda-example applications with Kafka connector turned on.

For demo purpose, the rest-spring-3-example and lambda-example are pre-configured with "kafka-connector". 
If you do not need these libraries, please remove them from the pom.xml built script.

Since kafka-connector is pre-configured, we can build and start the two demo applications like this. The
examples are not part of the top-level reactor build, so build each one with `mvn clean package` first
(the `mvn clean install` in [Getting Started](getting-started.md) installs the libraries they depend on):

```text
cd examples/rest-spring-3-example
mvn clean package
java -Dcloud.connector=kafka -Dmandatory.health.dependencies=cloud.connector.health 
     -jar target/rest-spring-3-example-x.y.z.jar
```

```text
cd examples/lambda-example
mvn clean package
java -Dcloud.connector=kafka -Dmandatory.health.dependencies=cloud.connector.health 
     -jar target/lambda-example-x.y.z.jar
```

The above command uses the "-D" parameters to configure the "cloud.connector" and "mandatory.health.dependencies".

The parameter `mandatory.health.dependencies=cloud.connector.health` tells the system to turn on the health check
endpoint for the application.
               
For the rest-spring-3-example, the start-up log may look like this:

```text
AppStarter:344 - Modules loaded in 2,825 ms
PresenceConnector:155 - Connected pc.abb4a4de.in, 127.0.0.1:8080, 
                        /ws/presence/202303282583899cf43a49b98f0522492b9ca178
EventConsumer:160 - Subscribed multiplex.0001.0
ServiceLifeCycle:73 - multiplex.0001, partition 0 ready
```

This means that the rest-spring-3-example has successfully connected to the presence monitor at port 8080.
It has subscribed to the topic "multiplex.0001" partition 0.

For the lambda-example, the log may look like this:

```text
AppStarter:344 - Modules loaded in 2,742 m
PresenceConnector:155 - Connected pc.991a2be0.in, 127.0.0.1:8080, 
                        /ws/presence/2023032808d82ebe2c0d4e5aa9ca96b3813bdd25
EventConsumer:160 - Subscribed multiplex.0001.1
ServiceLifeCycle:73 - multiplex.0001, partition 1 ready
ServiceRegistry:242 - Peer 202303282583899cf43a49b98f0522492b9ca178 joins (rest-spring-3-example x.y.z)
ServiceRegistry:383 - hello.world (rest-spring-3-example, WEB.202303282583899cf43a49b98f0522492b9ca178) registered
```

You notice that the lambda-example has discovered the rest-spring-3-example through Kafka and added the 
"hello.world" to the distributed routing table.

At this point, the rest-spring-3-example will find the lambda-example application as well:

```text
ServiceRegistry:242 - Peer 2023032808d82ebe2c0d4e5aa9ca96b3813bdd25 joins (lambda-example x.y.z)
ServiceRegistry:383 - hello.world (lambda-example, 
                                   APP.2023032808d82ebe2c0d4e5aa9ca96b3813bdd25) registered
ServiceRegistry:383 - hello.pojo (lambda-example, 
                                   APP.2023032808d82ebe2c0d4e5aa9ca96b3813bdd25) registered
```

This is real-time service discovery coordinated by the "kafka-presence" monitor application.

Now you have created a minimalist event-driven service mesh.

## Send an event request from rest-spring-3-example to lambda-example

In [Event over HTTP](event-over-http.md), you have sent a request from the rest-spring-3-example to the lambda-example using 
"Event over HTTP" without a service mesh.

In this section, you can make the same request using service mesh.

Please point your browser to http://127.0.0.1:8083/api/pojo/mesh/1
You will see the following response in your browser.

```json
{
  "id": 1,
  "name": "Simple PoJo class",
  "address": "100 World Blvd, Planet Earth",
  "date": "2023-03-28T17:53:41.696Z",
  "instance": 1,
  "seq": 1,
  "origin": "2023032808d82ebe2c0d4e5aa9ca96b3813bdd25"
}
```

## Presence monitor info endpoint

You can check the service mesh status from the presence monitor's "/info" endpoint. 

You can visit http://127.0.0.1:8080/info and it will show something like this:

```json
{
  "app": {
    "name": "kafka-presence",
    "description": "Presence Monitor",
    "version": "x.y.z"
  },
  "personality": "RESOURCES",
  "more": {
    "total": {
      "topics": 2,
      "virtual_topics": 2,
      "connections": 2
    },
    "topics": [
      "multiplex.0001 (32)",
      "service.monitor (11)"
    ],
    "virtual_topics": [
      "multiplex.0001-000 -> 202303282583899cf43a49b98f0522492b9ca178, rest-spring-3-example vx.y.z",
      "multiplex.0001-001 -> 2023032808d82ebe2c0d4e5aa9ca96b3813bdd25, lambda-example vx.y.z"
    ],
    "connections": [
      {
        "elapsed": "25 minutes 12 seconds",
        "created": "2023-03-28T17:43:13Z",
        "origin": "2023032808d82ebe2c0d4e5aa9ca96b3813bdd25",
        "name": "lambda-example",
        "topic": "multiplex.0001-001",
        "monitor": "2023032896b12f9de149459f9c8b71ad8b6b49fa",
        "type": "APP",
        "updated": "2023-03-28T18:08:25Z",
        "version": "x.y.z",
        "seq": 65,
        "group": 1
      },
      {
        "elapsed": "29 minutes 42 seconds",
        "created": "2023-03-28T17:38:47Z",
        "origin": "202303282583899cf43a49b98f0522492b9ca178",
        "name": "rest-spring-3-example",
        "topic": "multiplex.0001-000",
        "monitor": "2023032896b12f9de149459f9c8b71ad8b6b49fa",
        "type": "WEB",
        "updated": "2023-03-28T18:08:29Z",
        "version": "x.y.z",
        "seq": 75,
        "group": 1
      }
    ],
    "monitors": [
      "2023032896b12f9de149459f9c8b71ad8b6b49fa - 2023-03-28T18:08:46Z"
    ]
  },
  "java": {
    "version": "18.0.2.1+1",
  },
  "origin": "2023032896b12f9de149459f9c8b71ad8b6b49fa",
  "time": {
    "current": "2023-03-28T18:08:47.613Z",
    "start": "2023-03-28T17:31:23.611Z"
  }
}
```

In this example, it shows that there are two user applications (rest-spring-3-example and lambda-example) connected.

## Presence monitor health endpoint

The presence monitor has a "/health" endpoint.

You can visit http://127.0.0.1:8080/health and it will show something like this:

```json
{
  "dependency": [
    {
      "route": "cloud.connector.health",
      "status_code": 200,
      "service": "kafka",
      "topics": "on-demand",
      "href": "127.0.0.1:9092",
      "message": "Loopback test took 3 ms; System contains 2 topics",
      "required": true
    }
  ],
  "origin": "2023032896b12f9de149459f9c8b71ad8b6b49fa",
  "name": "kafka-presence",
  "status": "UP"
}
```

## User application health endpoint

Similarly, you can check the health status of the rest-spring-3-example application with http://127.0.0.1:8083/health

```json
{
  "dependency": [
    {
      "route": "cloud.connector.health",
      "status_code": 200,
      "service": "kafka",
      "topics": "on-demand",
      "href": "127.0.0.1:9092",
      "message": "Loopback test took 4 ms",
      "required": true
    }
  ],
  "origin": "202303282583899cf43a49b98f0522492b9ca178",
  "name": "rest-spring-example",
  "status": "UP"
}
```

It looks similar to the health status of the presence monitor. However, only the presence monitor shows the total
number of topics because it handles topic issuance to each user application instance.

## Actuator endpoints

Additional actuator endpoints includes:

1. library endpoint ("/info/lib") - you can check the packaged libraries for each application
2. distributed routing table ("/info/routes") - this will display the distributed routing table for public functions
3. environment ("/env") - it shows all functions (public and private) with number of workers.
4. livenessproble ("/livenessprobe") - this should display "OK" to indicate the application is running

## Stop an application

You can press "control-C" to stop an application. Let's stop the lambda-example application.

Once you stopped lamdba-example from the command line, the rest-spring-3-example will detect it:

```text
ServiceRegistry:278 - Peer 2023032808d82ebe2c0d4e5aa9ca96b3813bdd25 left (lambda-example x.y.z)
ServiceRegistry:401 - hello.world 2023032808d82ebe2c0d4e5aa9ca96b3813bdd25 unregistered
ServiceRegistry:401 - hello.pojo 2023032808d82ebe2c0d4e5aa9ca96b3813bdd25 unregistered
```

The rest-spring-3-example will update its distributed routing table automatically.

You will also find log messages in the kafka-presence application like this:

```text
MonitorService:120 - Member 2023032808d82ebe2c0d4e5aa9ca96b3813bdd25 left
TopicController:250 - multiplex.0001-001 released by 2023032808d82ebe2c0d4e5aa9ca96b3813bdd25,
                                                     lambda-example, x.y.z
```

When an application instance stops, the presence monitor will detect the event, remove it from the registry and
release the topic associated with the disconnected application instance.

The presence monitor is using the "presence" feature in websocket, thus we call it "presence" monitor.

## Broadcast vs multicast {#broadcast-vs-multicast}

With the service mesh running, `PostOffice.broadcast()` becomes meaningful. It delivers an event to
**every pod / container** that has the target route registered — not just one instance selected by
load balancing.

```java
// Delivers to ALL instances of "cache.invalidate" across all running pods
var po = new PostOffice(headers, instance);
po.broadcast(new EventEnvelope().setTo("cache.invalidate").setBody(Map.of("key", "session-42")));
```

The cloud connector reads the broadcast flag, queries the distributed routing table (maintained by the
presence monitor), and publishes the event to every known instance. Without `cloud.connector=kafka` and
presence-monitor running, `po.broadcast()` degrades silently to unicast on the local instance.

**Multicast is a separate, unrelated feature.** It operates entirely within the local JVM's in-memory
event bus — no service mesh required. A multicast entry in `multicast.yaml` registers a `LocalPublisher`
function at the source route that relays each incoming message to all listed target routes **within the
same application instance**:

```yaml
# multicast.yaml — local fan-out only
multicast:
  - source: "order.placed"
    targets:
      - "inventory.handler"
      - "notification.handler"
      - "audit.handler"
```

| | Multicast | Broadcast |
|:---|:---|:---|
| Scope | Single JVM | All pods in the service mesh |
| Config | `multicast.yaml` | `po.broadcast()` API call |
| Requires | Nothing extra | `cloud.connector=kafka` + presence-monitor |
| Without mesh | Works (always local) | Silently degrades to unicast |

## See also

- [Event over HTTP](event-over-http.md) — cross-instance event communication.
- [API Overview](api-overview.md) — the PostOffice / Platform APIs.
- [Configuration Reference](configuration-reference.md) — connector and presence-monitor settings.
