# Event over HTTP

The in-memory event system allows functions to communicate with each other in the same application memory space.

In composable architecture, applications are modular components in a network. Some transactions may require
the services of more than one application. "Event over HTTP" extends the event system beyond a single application.

The Event API service (`event.api.service`) is a built-in function in the system.

## The Event API endpoint

To enable "Event over HTTP", you must first turn on the REST automation engine with the following parameters
in the application.properties file:

```properties
rest.server.port=8085
rest.automation=true
```

and then check if the following entry is configured in the "rest.yaml" endpoint definition file. 
If not, update "rest.yaml" accordingly. The "timeout" value is set to 60 seconds to fit common use cases.

```yaml
  - service: [ "event.api.service" ]
    methods: [ 'POST' ]
    url: "/api/event"
    timeout: 60s
    tracing: true
```

This will expose the Event API endpoint at port 8085 and URL "/api/event". 

In kubernetes, The Event API endpoint of each application is reachable through internal DNS and there is no need
to create "ingress" for this purpose.

## Test drive Event API

You may now test drive the Event API service.

First, build and run the lambda-example application in port 8085.

```shell
cd examples/lambda-example
java -jar target/lambda-example-3.1.2.jar
```

Second, build and run the rest-spring-example application.

```shell
cd examples/rest-spring-example-3
java -jar target/rest-spring-3-example-3.1.2.jar
```

The rest-spring-3-example application will run as a Spring Boot application in port 8083 and 8086.

These two applications will start independently.

You may point your browser to http://127.0.0.1:8083/api/pojo/http/1 to invoke the `HelloPojoEventOverHttp` 
endpoint service that will in turn makes an Event API call to the lambda-example's "hello.pojo" service.

You will see the following response in the browser. This means the rest-spring-example application has successfully
made an event API call to the lambda-example application using the Event API endpoint.

```json
{
  "id": 1,
  "name": "Simple PoJo class",
  "address": "100 World Blvd, Planet Earth",
  "date": "2023-03-27T23:17:19.257Z",
  "instance": 6,
  "seq": 66,
  "origin": "2023032791b6938a47614cf48779b1cf02fc89c4"
}
```

To examine how the application makes the Event API call, please refer to the `HelloPojoEventOverHttp` class
in the rest-spring-example. The class is extracted below:

```java
@RestController
public class HelloPoJoEventOverHttp {

    @GetMapping("/api/pojo/http/{id}")
    public Mono<SamplePoJo> getPoJo(@PathVariable("id") Integer id) {
        AppConfigReader config = AppConfigReader.getInstance();
        String remotePort = config.getProperty("lambda.example.port", "8085");
        String remoteEndpoint = "http://127.0.0.1:"+remotePort+"/api/event";
        String traceId = Utility.getInstance().getUuid();
        PostOffice po = new PostOffice("hello.pojo.endpoint", traceId, "GET /api/pojo/http");
        EventEnvelope req = new EventEnvelope().setTo("hello.pojo").setHeader("id", id);
        return Mono.create(callback -> {
            try {
                EventEnvelope response = po.request(req, 3000, Collections.emptyMap(), remoteEndpoint, true).get();
                if (response.getBody() instanceof SamplePoJo result) {
                    callback.success(result);
                } else {
                    callback.error(new AppException(response.getStatus(), String.valueOf(response.getError())));
                }
            } catch (IOException | ExecutionException | InterruptedException e) {
                callback.error(e);
            }
        });
    }
}
```

The method signatures of the Event API is shown as follows:

### Asynchronous API (Java)

```java
// io.vertx.core.Future
public Future<EventEnvelope> asyncRequest(final EventEnvelope event, long timeout,
                                          Map<String, String> headers,
                                          String eventEndpoint, boolean rpc) throws IOException;
```

### Sequential non-blocking API (virtual thread function)

```java
// java.util.concurrent.Future
public Future<EventEnvelope> request(final EventEnvelope event, long timeout,
                                          Map<String, String> headers,
                                          String eventEndpoint, boolean rpc) throws IOException;
```

### Sequential non-blocking API (Kotlin suspend function)

```java
suspend fun awaitRequest(request: EventEnvelope?, timeout: Long, 
                          headers: Map<String, String>,
                          eventEndpoint: String, rpc: Boolean): EventEnvelope
}
```

Optionally, you may add security headers in the "headers" argument. e.g. the "Authorization" header.

The eventEndpoint is a fully qualified URL. e.g. `http://peer/api/event`

The "rpc" boolean value is set to true so that the response from the service of the peer application instance 
will be delivered. For drop-n-forget use case, you can set the "rpc" value to false. It will immediately return
an HTTP-202 response.

## Event-over-HTTP using configuration

While you can call the "Event-over-HTTP" APIs programmatically, it would be more convenient to automate it with a
configuration. This service abstraction means that user applications do not need to know where the target services are.

You can enable Event-over-HTTP configuration by adding this parameter in application.properties:

```text
#
# Optional event-over-http target maps
#
yaml.event.over.http=classpath:/event-over-http.yaml
```

and then create the configuration file "event-over-http.yaml" like this:

```yaml
event:
  http:
  - route: 'hello.pojo2'
    target: 'http://127.0.0.1:${lambda.example.port}/api/event'
  - route: 'event.http.test'
    target: 'http://127.0.0.1:${server.port}/api/event'
    # optional security headers
    headers:
      authorization: 'demo'
  - route: 'event.save.get'
    target: 'http://127.0.0.1:${server.port}/api/event'
    headers:
      authorization: 'demo'
```

In the above example, there are three routes (hello.pojo2, event.http.test and event.save.get) with target URLs.
If additional authentication is required for the peer's "/api/event" endpoint, you may add a set of security
headers in each route.

When you send asynchronous event or make a RPC call to "event.save.get" service, it will be forwarded to the
peer's "event-over-HTTP" endpoint (`/api/event`) accordingly. If the route is a task in an event flow,
the event manager will make the "Event over HTTP" to the target service.

You may also add environment variable or base configuration references to the application.yaml file, such as
"server.port" in this example.

An example in the rest-spring-3-example subproject is shown below to illustrate this service abstraction.
In this example, the remote Event-over-HTTP endpoint address is resolved from the event-over-http.yaml
configuration.

```java
@RestController
public class HelloPoJoEventOverHttpByConfig {

    @GetMapping("/api/pojo2/http/{id}")
    public Mono<SamplePoJo> getPoJo(@PathVariable("id") Integer id) {
        String traceId = Utility.getInstance().getUuid();
        PostOffice po = new PostOffice("hello.pojo.endpoint", traceId, "GET /api/pojo2/http");
        /*
         * "hello.pojo2" resides in the lambda-example and is reachable by "Event-over-HTTP".
         * In HelloPojoEventOverHttp.java, it demonstrates the use of Event-over-HTTP API.
         * In this example, it illustrates the use of the "Event-over-HTTP by configuration" feature.
         * Please see application.properties and event-over-http.yaml files for more details.
         */
        EventEnvelope req = new EventEnvelope().setTo("hello.pojo2").setHeader("id", id);
        return Mono.create(callback -> {
            try {
                EventEnvelope response = po.request(req, 3000, false).get();
                if (response.getBody() instanceof SamplePoJo result) {
                    callback.success(result);
                } else {
                    callback.error(new AppException(response.getStatus(), String.valueOf(response.getError())));
                }
            } catch (IOException | ExecutionException | InterruptedException e) {
                callback.error(e);
            }
        });
    }
}
```

> *Note*: The target function must declare itself as PUBLIC in the preload annotation. Otherwise, you will get
  a HTTP-403 exception.

## Advantages

The Event API exposes all public functions of an application instance to the network using a single REST endpoint.

The advantages of Event API includes:

1. Convenient - you do not need to write or configure individual endpoint for each public service
2. Efficient - events are transported in binary format from one application to another
3. Secure - you can protect the Event API endpoint with an authentication service

The following configuration adds authentication service to the Event API endpoint:
```yaml
  - service: [ "event.api.service" ]
    methods: [ 'POST' ]
    url: "/api/event"
    timeout: 60s
    authentication: "v1.api.auth"
    tracing: true
```

This enforces every incoming request to the Event API endpoint to be authenticated by the "v1.api.auth" service
before passing to the Event API service. You can plug in your own authentication service such as OAuth 2.0 
"bearer token" validation.

Please refer to [Chapter-3 - REST automation](CHAPTER-3.md) for details.
<br/>

|          Chapter-6          |                   Home                    |                Chapter-8                |
|:---------------------------:|:-----------------------------------------:|:---------------------------------------:|
| [Spring Boot](CHAPTER-6.md) | [Table of Contents](TABLE-OF-CONTENTS.md) | [Minimalist Service Mesh](CHAPTER-8.md) |
