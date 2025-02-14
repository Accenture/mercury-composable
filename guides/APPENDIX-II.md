# Reserved names

The system reserves some route names and headers for routing purpose.

## System route names

The Mercury foundation code is written using the same core API and each function has a route name.

The following route names are reserved. Please DO NOT overload them in your application functions
to avoid breaking the system unintentionally.

| Route                        | Purpose                               | Modules          |
|:-----------------------------|:--------------------------------------|:-----------------|
| actuator.services            | Actuator endpoint services            | platform-core    |
| info.actuator.service        | Info actuator endpoint                | platform-core    |
| lib.actuator.service         | Info actuator endpoint                | platform-core    |
| routes.actuator.service      | Info actuator endpoint                | platform-core    |
| env.actuator.service         | Info actuator endpoint                | platform-core    |
| health.actuator.service      | Info actuator endpoint                | platform-core    |
| liveness.actuator.service    | Info actuator endpoint                | platform-core    |
| elastic.queue.cleanup        | Elastic event buffer clean up task    | platform-core    |
| distributed.tracing          | Distributed tracing logger            | platform-core    |
| system.ws.server.cleanup     | Websocket server cleanup service      | platform-core    |
| http.auth.handler            | REST automation authentication router | platform-core    |
| event.api.service            | Event API service                     | platform-core    |
| temporary.inbox              | Event listener for RPC                | platform-core    |
| event.script.manager         | Instantiate new event flow instance   | event-script     |
| task.executor                | Perform event choreography            | event-script     |
| http.flow.adapter            | Built-in flow adapter                 | event-script     |
| no.op                        | no-operation placeholder function     | event-script     |
| system.service.registry      | Distributed routing registry          | Connector        |
| system.service.query         | Distributed routing query             | Connector        |
| cloud.connector.health       | Cloud connector health service        | Connector        |
| cloud.health.inbox           | Event listerner for loopback test     | Connector        |
| cloud.manager                | Cloud manager service                 | Connector        |
| presence.service             | Presence signal service               | Connector        |
| presence.housekeeper         | Presence keep-alive service           | Connector        |
| cloud.connector              | Cloud event emitter                   | Connector        |
| init.multiplex.*             | reserved for event stream startup     | Connector        |
| completion.multiplex.*       | reserved for event stream clean up    | Connector        |
| async.http.request           | HTTP request event handler            | REST automation  |
| async.http.response          | HTTP response event handler           | REST automation  |
| cron.scheduler               | Cron job scheduler                    | Simple Scheduler |
| init.service.monitor.*       | reserved for event stream startup     | Service monitor  |
| completion.service.monitor.* | reserved for event stream clean up    | Service monitor  |

## Optional user defined functions

The following optional route names will be detected by the system for additional user defined features.

| Route                        | Purpose                                                                               |
|:-----------------------------|:--------------------------------------------------------------------------------------|
| additional.info              | User application function to return information<br/> about your application status    |
| distributed.trace.forwarder  | Custom function to forward performance metrics<br/> to a telemetry system             |
| transaction.journal.recorder | Custom function to record transaction request-response<br/> payloads into an audit DB |

The `additional.info` function, if implemented, will be invoked from the "/info" endpoint and its response
will be merged into the "/info" response.

For `distributed.trace.forwarder` and `transaction.journal.recorder`, please refer to [Chapter-5](CHAPTER-5.md)
for details.

## No-op function

The "no.op" function is used as a placeholder for building skeleton or simple decision function for
an event flow use case.

## Reserved event header names

The following event headers are injected by the system as READ only metadata. They are available from the
input "headers". However, they are not part of the EventEnvelope.

| Header        | Purpose                                    | 
|:--------------|:-------------------------------------------|
| my_route      | route name of your function                |
| my_trace_id   | trace ID, if any, for the incoming event   |
| my_trace_path | trace path, if any, for the incoming event | 

You can create a trackable PostOffice using the "headers" and the "instance" parameters in the input arguments
of your function.

```java
var po = new PostOffice(headers, instance);
```

## Reserved HTTP header names

| Header                   | Purpose                                                                     | 
|:-------------------------|:----------------------------------------------------------------------------|
| X-Stream-Id              | Temporal route name for streaming content                                   |
| X-TTL                    | Time to live in milliseconds for a streaming content                        |
| X-Small-Payload-As-Bytes | This header, if set to true, tells system to render stream content as bytes |
| X-Event-Api              | The system uses this header to indicate that the request is sent over HTTP  |
| X-Async                  | This header, if set to true, indicates it is a drop-n-forget request        |
| X-Trace-Id               | This allows the system to propagate trace ID                                |
| X-Correlation-Id         | Alternative to X-Trace-Id                                                   |
| X-Content-Length         | If present, it is the expected length of a streaming content                |
| X-Raw-Xml                | This header, if set to true, tells to system to skip XML rendering          |
| X-Flow-Id                | This tells the event manager to select a flow configuration by ID           |
| X-App-Instance           | This header is used by some protected actuator REST endpoints               |

To support traceId that is stored in X-Correlation-Id HTTP header, set this in application.properties.

```properties
# list of supported traceId headers where the first one is the default label
trace.http.header=X-Correlation-Id, X-Trace-Id

## Transient data store

The system uses a temp folder in "/tmp/composable/java/temp-streams" to hold temporary data blocks for streaming I/O.
```
<br/>

|                 Appendix-I                 |                   Home                    |                    Appendix-III                    |
|:------------------------------------------:|:-----------------------------------------:|:--------------------------------------------------:|
| [Application Configuration](APPENDIX-I.md) | [Table of Contents](TABLE-OF-CONTENTS.md) | [Actuators, HTTP client and More](APPENDIX-III.md) |
