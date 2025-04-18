# Actuators, HTTP client and More

## Actuator endpoints

The following are actuator endpoints:

```
GET /info
GET /info/routes
GET /info/lib
GET /env
GET /health
GET /livenessprobe
```

| Endpoint       | Purpose                                                        | 
|:---------------|:---------------------------------------------------------------|
| /info          | Describe the application                                       |
| /info/routes   | List all private and public function route names               |
| /info/lib      | List libraries packed with this executable                     |
| /env           | Show selected environment variables and application parameters |
| /health        | Application health check endpoint                              |
| /livenessprobe | Check if application is running normally                       |

## System provided REST endpoints

When REST automation is turned on, the following essential REST endpoints will be provided if they are
not configured in rest.yaml. The "POST /api/event" is used for Event-Over-HTTP protocol and the others
are actuator endpoints.

To override the default parameters such as timeout, tracing and authentication, you can configure them
in rest.yaml.

```yaml
rest:
  - service: "event.api.service"
    methods: ['POST']
    url: "/api/event"
    timeout: 60s
    tracing: true

  - service: "info.actuator.service"
    methods: ['GET']
    url: "/info"
    timeout: 10s

  - service: "lib.actuator.service"
    methods: ['GET']
    url: "/info/lib"
    timeout: 10s

  - service: "routes.actuator.service"
    methods: ['GET']
    url: "/info/routes"
    timeout: 10s

  - service: "health.actuator.service"
    methods: ['GET']
    url: "/health"
    timeout: 10s

  - service: "liveness.actuator.service"
    methods: ['GET']
    url: "/livenessprobe"
    timeout: 10s

  - service: "env.actuator.service"
    methods: ['GET']
    url: "/env"
    timeout: 10s
```

> *Note*: When using the rest-spring-3 library, the actuator endpoints are always available from the
          Spring Boot's HTTP port and they cannot be changed.

## Custom health services

You can extend the "/health" endpoint by implementing and registering lambda functions to be added to the 
"health check" dependencies.

```properties
mandatory.health.dependencies=cloud.connector.health, demo.health
optional.health.dependencies=other.service.health
```

Your custom health service must respond to the following requests:

1. Info request (type=info) - it should return a map that includes service name and href (protocol, hostname and port)
2. Health check (type=health) - it should return a text string or a Map of the health check. e.g. read/write test result. 
   If health check fails, you can throw AppException with status code and error message.

> *Note*: The "href" entry in the health service's response should tell the operator about the target URL
          if the dependency connects to a cloud platform service such as Kafka, Redis, etc.

A sample health service is available in the `DemoHealth` class of the `composable-example` project as follows:

```java
@PreLoad(route="demo.health", instances=5)
public class DemoHealth implements LambdaFunction {

    private static final String TYPE = "type";
    private static final String INFO = "info";
    private static final String HEALTH = "health";

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        /*
         * The interface contract for a health check service includes both INFO and HEALTH responses.
         * It must return a Map.
         */
        if (INFO.equals(headers.get(TYPE))) {
            Map<String, Object> about = new HashMap<>();
            about.put("service", "demo.service");
            about.put("href", "http://127.0.0.1");
            return about;
        }
        if (HEALTH.equals(headers.get(TYPE))) {
            /*
             * This is a place-holder for checking a downstream service.
             *
             * Please implement your own logic to test if a downstream service is running fine.
             * If running, just return health status as a String or a Map.
             *
             * Otherwise,
             *      throw new AppException(status, message)
             */
            return Map.of("demo", "I am running fine");
        }
        throw new IllegalArgumentException("type must be info or health");
    }
}
```

## AsyncHttpClient service

The "async.http.request" function can be used as a non-blocking HTTP client.

To make an HTTP request to an external REST endpoint, you can create an HTTP request object using the
`AsyncHttpRequest` class and make an async RPC call to the "async.http.request" function like this:

```java
PostOffice po = new PostOffice(headers, instance);
AsyncHttpRequest req = new AsyncHttpRequest();
req.setMethod("GET");
req.setHeader("accept", "application/json");
req.setUrl("/api/hello/world?hello world=abc");
req.setQueryParameter("x1", "y");
List<String> list = new ArrayList<>();
list.add("a");
list.add("b");
req.setQueryParameter("x2", list);
req.setTargetHost("http://127.0.0.1:8083");
EventEnvelope request = new EventEnvelope().setTo("async.http.request").setBody(req);
EventEnvelope res = po.request(request, 5000).get();
// the response is a Java Future and the result is an EventEnvelope
```

By default, your user function is running in a virtual thread.
While the RPC call looks like synchronous, the po.request API will run in non-blocking mode in the same fashion
as the "async/await" pattern.

For reactive programming, you can use the "asyncRequest" API like this:

```java
PostOffice po = new PostOffice(headers, instance);
AsyncHttpRequest req = new AsyncHttpRequest();
req.setMethod("GET");
req.setHeader("accept", "application/json");
req.setUrl("/api/hello/world?hello world=abc");
req.setQueryParameter("x1", "y");
List<String> list = new ArrayList<>();
list.add("a");
list.add("b");
req.setQueryParameter("x2", list);
req.setTargetHost("http://127.0.0.1:8083");
EventEnvelope request = new EventEnvelope().setTo("async.http.request").setBody(req);
Future<EventEnvelope> res = po.asyncRequest(request, 5000);
res.onSuccess(response -> {
   // do something with the result 
});
```

## Send HTTP request body for HTTP PUT, POST and PATCH methods

For most cases, you can just set a HashMap into the request body and specify content-type as JSON or XML.
The system will perform serialization properly.

Example code may look like this:

```java
AsyncHttpRequest req = new AsyncHttpRequest();
req.setMethod("POST");
req.setHeader("accept", "application/json");
req.setHeader("content-type", "application/json");
req.setUrl("/api/book");
req.setTargetHost("https://service_provider_host");
req.setBody(mapOfKeyValues);
// where keyValues is a HashMap
```

## Send HTTP request body as a stream

For larger payload, you may use the streaming method. See sample code below:

```java
int len;
byte[] buffer = new byte[4096];
FileInputStream in = new FileInputStream(myFile);
EventPublisher publisher = new EventPublisher(timeoutInMIlls);
while ((len = in.read(buffer, 0, buffer.length)) != -1) {
    publisher.publish(buffer, 0, len);
}
// closing the output stream would send a EOF signal to the stream
publisher.publishCompletion();
// tell the HTTP client to read the input stream by setting the streamId in the AsyncHttpRequest object
req.setStreamRoute(publisher.getStreamId());
```

## Read HTTP response body stream

If content length is not given, the response body would arrive as a stream.

Your application should check if the HTTP response header "stream" exists. Its value is the input "streamId".

You can process the input stream using the FluxConsumer class like this.
Please note that the FluxConsumer is typed. If you do not know the data type for the stream content, use
`Object` for untyped read and test the object type of the incoming messages in the content stream.

```java
String streamId = headers.get("stream");
long ttl = 10000; // anticipated time in milliseconds to stream the content
FluxConsumer<Map<String, Object>> fc = new FluxConsumer<>(streamId, ttl);
fc.consume(
    data -> {
        // handle incoming message
    },
    e -> {
        // handle exception where e is a Throwable
    },
    () -> {
        // handle stream completion
    }
);
```

By default, a user function is executed in a virtual thread which effectively is an "async" function and
the PostOffice "request" API operates in the non-blocking "await" mode.

## Rendering a small payload of streaming content

If the streaming HTTP response is certain to be a small payload (i.e. Kilobytes), you can optimize
the rendering by adding the HTTP request header (X-Small-Payload-As-Bytes=true) in the AsyncHttpRequest object.

```java
AsyncHttpRequest req = new AsyncHttpRequest();
req.setMethod("GET");
req.setUrl("/api/some/binary/content");
req.setTargetHost("https://service_provider_host");
req.setHeader("X-Small-Payload-As-Bytes", "true");
```

Note that the AsyncHttpClient will insert a custom HTTP response header "X-Content-Length" to show the size
of the payload.

> IMPORTANT: This optimization does not validate the size of the streaming content. Therefore, it is possible for
             the streaming content to trigger an "out of memory" exception. You must make sure the streaming content
             is small enough before using the "X-Small-Payload-As-Bytes" HTTP request header.

## Content length for HTTP request

*IMPORTANT*: Do not set the "content-length" HTTP header because the system will automatically compute the
correct content-length for small payload. For large payload, it will use the chunking method.

## Using AsyncHttpClient by configuration

The "async.http.request" service can be used as a task in a flow. The following flow configuration example
illustrates using it as a task.

```yaml
flow:
  id: 'http-client-by-config'
  description: 'Demonstrate use of the Async HTTP client using configuration means'
  ttl: 10s

first.task: 'http.client'

tasks:
  - name: 'http.client'
    input:
      - 'text(/api/echo/test) -> url'
      - 'text(PUT) -> method'
      - 'text(http://127.0.0.1:${server.port}) -> host'
      - 'input.body -> body'
      - 'text(world) -> parameters.query.hello'
      - 'text(application/json) -> headers.content-type'
      - 'text(application/json) -> headers.accept'
    process: 'async.http.request'
    output:
      - 'text(application/json) -> output.header.content-type'
      - 'result -> output.body'
    description: 'Return result'
    execution: end
```

The interface contract for the AsyncHttpClient is the AsyncHttpRequest object. The following table lists
the parameters where parameters.query, body and cookies are optional.

| Parameter        | Usage                                     | Example                               |
|:-----------------|:------------------------------------------|:--------------------------------------|
| method           | HTTP method                               | GET                                   |
| host             | Protocol and domain name (or IP address)  | https://demo.platformlambda.org       |
| url              | URI path                                  | /api/hello/world                      |
| parameters.query | Query parameter key-value                 | parameters.query.hello=world          |
| headers          | HTTP request headers                      | headers.content-type=application/json |
| body             | HTTP request body for PUT, POST and PATCH | {"hello": "world"}                    |
| cookies          | Cookie key-value                          | cookies.session-id=12345              |

## Starting a flow programmatically

To start an "event" flow from a unit test, you may use the helper class "FlowExecutor" under the "Event Script" module.

Examples of some APIs are as follows:

```java
// launch a flow asychronously
public void launch(String originator, String flowId, Map<String, Object> dataset,
                       String correlationId) throws IOException;
// launch a flow asychronously with tracing
public void launch(String originator, String traceId, String tracePath, String flowId,
                       Map<String, Object> dataset, String correlationId) throws IOException
// launch a flow asychronously and tracing
public void launch(PostOffice po, String flowId, Map<String, Object> dataset,
                        String correlationId) throws IOException;
// launch a flow with callback and tracing
public void launch(PostOffice po, String flowId, Map<String, Object> dataset,
                        String replyTo, String correlationId) throws IOException;
// launch a flow and expect a future response
public Future<EventEnvelope> request(PostOffice po, String flowId, Map<String, Object> dataset,
                                     String correlationId, long timeout) throws IOException;
```

The following unit test emulates a HTTP request to the flow named "header-test".

```java
@Test
public void internalFlowTest() throws IOException, ExecutionException, InterruptedException {
    final long TIMEOUT = 8000;
    String traceId = Utility.getInstance().getUuid();
    String cid = Utility.getInstance().getUuid();
    PostOffice po = new PostOffice("unit.test", traceId, "INTERNAL /flow/test");
    String flowId = "header-test";
    Map<String, Object> headers = new HashMap<>();
    Map<String, Object> dataset = new HashMap<>();
    dataset.put("header", headers);
    dataset.put("body", Map.of("hello", "world"));
    headers.put("user-agent", "internal-flow");
    headers.put("accept", "application/json");
    headers.put("x-flow-id", flowId);
    FlowExecutor flowExecutor = FlowExecutor.getInstance();
    EventEnvelope result = flowExecutor.request(po, flowId, dataset, cid, TIMEOUT).get();
    assertInstanceOf(Map.class, result.getBody());
    Map<String, Object> body = (Map<String, Object>) result.getBody();
    // verify that input headers are mapped to the function's input body
    assertEquals("header-test", body.get("x-flow-id"));
    assertEquals("internal-flow", body.get("user-agent"));
    assertEquals("application/json", body.get("accept"));
}
```

The dataset must contain at least the "body" key-value so that input data mapping is possible in a flow.

For the built-in HTTP flow adapter, the dataset would contain the following:

```java
// convert HTTP context to flow "input" dataset
Map<String, Object> dataset = new HashMap<>();
dataset.put("header", request.getHeaders());
dataset.put("body", request.getBody());
dataset.put("cookie", request.getCookies());
dataset.put("path_parameter", request.getPathParameters());
dataset.put("method", request.getMethod());
dataset.put("uri", request.getUrl());
dataset.put("query", request.getQueryParameters());
dataset.put("stream", request.getStreamRoute());
dataset.put("ip", request.getRemoteIp());
dataset.put("filename", request.getFileName());
dataset.put("session", request.getSessionInfo());
```

If you write your own Kafka flow adapter, the dataset should contain headers and body mapped with a Kafka event.

For other flow adapters, you may use different set of key-values.

## Application log format

The system supports 3 types of log formats. You can set "log.format" parameter in application.properties to change
the log format or override it at runtime using the Java "-D" argument. e.g.

```shell
java -Dlog.format=json -jar myapp.jar
```

| Format  | Description                                                                   | 
|:--------|:------------------------------------------------------------------------------|
| text    | this is the default log format                                                |
| json    | application log will be printed in JSON format with line feed and indentation |
| compact | JSON format without line feed and indentation                                 |

text and json formats are for human readers and compact format is designed for log analytics system.

To leverge the advantage of json log format, your application may log JSON using the
parameter formatter `{}` with a single Map parameter like this:

```java
var message = new HashMap<>();
message.put("id", id);
message.put("status", "completed");
message.put("notes", "Just a demo");
log.info("{}", message);
```

## Customize log4j configuration

The log4j configuration templates are available in the main "resources" folder of the platform-core.
If you want to adjust the "loggers" section in log4j, please copy the required XML files to
the main "resources" folder in your application.

| File               | Description                                                       | 
|:-------------------|:------------------------------------------------------------------|
| log4j2.xml         | this is the default configuration file for logging in text format |
| log4j2-json.xml    | configuration file for logging in JSON format                     |
| log4j2-compact.xml | configuration file for logging in COMPACT format                  |

The default log4j2.xml configuration file looks like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="INFO">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger:%line - %msg%n" />
        </Console>
    </Appenders>
    <Loggers>
        <Root level="${env:LOG_LEVEL:-INFO}" additivity="false">
            <AppenderRef ref="Console" />
        </Root>

        <!-- Enable INFO logging for DistributedTrace -->
        <logger name="org.platformlambda.core.services.DistributedTrace" level="INFO" />
    </Loggers>
</Configuration>
```

In the "loggers" section, you can expand the class list to tell log4j which classes to log
and at what level.

Please note that the "AppenderRef" must point to the same "Appenders" in the XML file.

## Handling numbers in a Map

The system assumes each key of a Map object to be a text string. If you use integer as a key,
it will be converted to a text string. The assumed Map class is `Map<String, Object>`.

Numbers in a value are handled differently in two cases.

*Serialization of an event envelope*: this is done using the MsgPack protocol for binary
JSON. The serialization process is optimized for performance and payload size. As a result,
a small number that is declared as Long will be serialized as an Integer (Long uses 8 bytes
and Integer uses 2 or 4 bytes).

*Serialization of nested Map in a PoJo*: this is done using the GSON library. It is optimized
for type matching. Integers are treated as Long numbers.

If you want to enforce Integer or Long, please design a PoJo to fit your use case.

However, floating point numbers (Float and Double) are rendered without type matching.

For untyped numbers, you may use the convenient type conversion methods in the platform-core's
Utility class. For examples, util.str2int and util.str2long.
<br/>

|                 Appendix-II                  |                   Home                    | 
|:--------------------------------------------:|:-----------------------------------------:|
| [Reserved names and headers](APPENDIX-II.md) | [Table of Contents](TABLE-OF-CONTENTS.md) |
