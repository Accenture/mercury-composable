# Spring Boot Integration

While the platform-core foundation code includes a lightweight non-blocking HTTP server, you can also turn your
application into an executable Spring Boot application.

There are two ways to do that:

1. Add dependency for Spring Boot version 3 and implement your Spring Boot main application
2. Add the `rest-spring-3` add-on library for a pre-configured Spring Boot experience

## Add platform-core to an existing Spring Boot application

For option 1, the platform-core library can co-exist with Spring Boot. You can write code specific to Spring Boot
and the Spring framework ecosystem. Please make sure you add the following startup code to your Spring Boot
main application like this:

```java
@ComponentScan({"org.platformlambda", "${web.component.scan}"})
@SpringBootApplication
public class MyMainApp extends SpringBootServletInitializer {

    public static void main(String[] args) {
        AutoStart.main(args);
        SpringApplication.run(MyMainApp.class, args);
    }

}
```

We suggest running `AutoStart.main` before the `SpringApplication.run` statement. This would allow the platform-core
foundation code to load the event-listener functions into memory before Spring Boot starts.

## Use the rest-spring library in your application

The `rest-spring-3` subproject is a pre-configured Spring Boot 3 library with WebFlux as the asynchronous
HTTP servlet engine.

You can add it to your application and turn it into a pre-configured Spring Boot 3 application.
It provides consistent behavior for XML and JSON serializaation and exception handling.

The RestServer class in the rest-spring-3 library is used to bootstrap a Spring Boot application.

However, Spring Boot is a sophisticated ecosystem by itself. If the simple RestServer bootstrap does not fit your
use cases, please implement your own Spring Boot initializer.

You can add the "spring.boot.main" parameter in the application.properties to point to your Spring Boot initializer
main class. Note that the default value is "org.platformlambda.rest.RestServer" that points to the system provided
Spring Boot initializer.

```shell
spring.boot.main=org.platformlambda.rest.RestServer
```

The Spring Boot initialization main class must have at least the following annotations:

```java
@ComponentScan({"org.platformlambda", "${web.component.scan}"})
@SpringBootApplication
```

The `ComponentScan` must include the package "org.platformlambda" to allow the system to load the pre-configured
serializers, actuator endpoints and exception handlers for consistent behavior as the built-in lightweight
non-blocking HTTP server.

If you want to disable the lightweight HTTP server, you can set `rest.automation=false` in application.properties.
The REST automation engine and the lightweight HTTP server will be turned off.

> *IMPORTANT*: When using Event Script, you must keep `rest.automation=true` because the HTTP flow adapter
               depends on the REST automation engine for incoming HTTP requests.

> *Note*: The platform-core library assumes the application configuration files to be either
          application.yml or application.properties. If you use custom Spring profile, please keep the
          application.yml or application.properties for the platform-core. If you use default Spring 
          profile, both platform-core and Spring Boot will use the same configuration files.

You can customize your error page using the default `errorPage.html` by copying it from the platform-core's or 
rest-spring's resources folder to your source project. The default page is shown below.

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <title>HTTP Error</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body>

<div>
    <h3>HTTP-${status}</h3>
    <div>${warning}</div><br/>
    <table>
        <tbody>
        <tr><td style="font-style: italic; width: 100px">Type</td><td>error</td></tr>
        <tr><td style="font-style: italic; width: 100px">Status</td><td>${status}</td></tr>
        <tr><td style="font-style: italic; width: 100px">Message</td><td>${message}</td></tr>
        </tbody>
    </table>

</div>
</body>
</html>
```

This is the HTML error page that the platform-core or rest-spring library uses. You can update it with
your corporate style guide. Please keep the parameters (status, message, path, warning) intact.

If you want to keep REST automation's lightweight HTTP server to co-exist with Spring Boot's Tomcat or other 
application server, please add the following to your application.properties file:

```properties
server.port=8083
rest.server.port=8085
rest.automation=true
```

The platform-core and Spring Boot will use `rest.server.port` and `server.port` respectively.

## Spring Autowiring
 
When using the `rest-spring-3` module, bean and value injection may be used:
 
```java
@PreLoad(route = "v1.demo.function")
public class DemoFunction implements LambdaFunction {
 
    @Autowired
    LegacyBean legacyBean;
 
    @Value("${injected.value}")
    String injectedValue;
 
    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) throws Exception {
       // ...
    }
}
```

> *Note*: This should be done judiciously - Composable functions should have very few dependencies
          and each injected dependency is a violation of that principle.  
 
### Limitations
 
* Only applies to `LambdaFunction` and `TypedLambdaFunction`.
* Only applies to instances loaded with `@PreLoad` - Functions registered programmatically will not undergo injection.
* Constructor injection is not viable - these instances are constructed before the Spring context is created.

## The rest-spring-3-example demo application

Let's review the `rest-spring-3-example` demo application in the "examples/rest-spring-3-example" project.

You can use the rest-spring-3-example as a template to create a Spring Boot application.

In addition to the REST automation engine that lets you create REST endpoints by configuration, you can also
programmatically create REST endpoints with the following methods:

1. Spring RestControllers with Mono/Flux
2. Servlet 3.1 WebServlets

We will examine asynchronous REST endpoint with the `AsyncHelloWorld` class.

```java
@RestController
public class AsyncHelloWorld {
  private static final AtomicInteger seq = new AtomicInteger(0);

  @GetMapping(value = "/api/hello/world", produces={"application/json", "application/xml"})
  public Mono<Map<String, Object>> hello(HttpServletRequest request) {
    String traceId = Utility.getInstance().getUuid();
    PostOffice po = new PostOffice("hello.world.endpoint", traceId, "GET /api/hello/world");
    Map<String, Object> forward = new HashMap<>();

    Enumeration<String> headers = request.getHeaderNames();
    while (headers.hasMoreElements()) {
      String key = headers.nextElement();
      forward.put(key, request.getHeader(key));
    }
    // As a demo, just put the incoming HTTP headers as a payload and a parameter showing the sequence counter.
    // The echo service will return both.
    int n = seq.incrementAndGet();
    EventEnvelope req = new EventEnvelope();
    req.setTo("hello.world").setBody(forward).setHeader("seq", n);
    return Mono.create(callback -> {
      try {
        po.asyncRequest(req, 3000)
                .onSuccess(event -> {
                  Map<String, Object> result = new HashMap<>();
                  result.put("status", event.getStatus());
                  result.put("headers", event.getHeaders());
                  result.put("body", event.getBody());
                  result.put("execution_time", event.getExecutionTime());
                  result.put("round_trip", event.getRoundTrip());
                  callback.success(result);
                })
                .onFailure(ex -> callback.error(new AppException(408, ex.getMessage())));
      } catch (IOException e) {
        callback.error(e);
      }
    });
  }
}
```

In this hello world REST endpoint, Spring Reactor runs the "hello" method asynchronously without waiting for a response.

The example code copies the HTTP requests and sends it as the request payload to the "hello.world" function.
The function is defined in the MainApp like this:

```java
Platform platform = Platform.getInstance();
LambdaFunction echo = (headers, input, instance) -> {
    Map<String, Object> result = new HashMap<>();
    result.put("headers", headers);
    result.put("body", input);
    result.put("instance", instance);
    result.put("origin", platform.getOrigin());
    return result;
};
platform.register("hello.world", echo, 20);
```

When "hello.world" responds, its result set will be returned to the `onSuccess` method as a "future response".

The "onSuccess" method then sends the response to the browser.

The `AsyncHelloConcurrent` is the same as the `AsyncHelloWorld` except that it performs a "fork-n-join" operation
to multiple instances of the "hello.world" function.

Unlike "rest.yaml" that defines tracing by configuration, you must turn on tracing programmatically in a Spring
RestController endpoint. To enable tracing, the function sets the trace ID and path in the PostOffice constructor. 

When you try the endpoint at http://127.0.0.1:8083/api/hello/world, it will echo your HTTP request headers. 
In the command terminal, you will see tracing information in the console log like this:

```text
DistributedTrace:67 - trace={path=GET /api/hello/world, service=hello.world, success=true, 
  origin=20230403364f70ebeb54477f91986289dfcd7b75, exec_time=0.249, start=2023-04-03T04:42:43.445Z, 
  from=hello.world.endpoint, id=e12e871096ba4938b871ee72ef09aa0a, round_trip=20.018, status=200}
```

## Lightweight non-blocking websocket server

If you want to turn on a non-blocking websocket server, you can add the following configuration to 
application.properties.

```properties
server.port=8083
websocket.server.port=8085
```

The above assumes Spring Boot runs on port 8083 and the websocket server runs on port 8085.

> *Note*: The "websocket.server.port" parameter is an alias of "rest.server.port"

You can create a websocket service with a Java class like this:

```java
@WebSocketService("hello")
public class WsEchoDemo implements LambdaFunction {

    @Override
    public Object handleEvent(Map<String, String> headers, Object body, int instance) {
        // handle the incoming websocket events (type = open, close, bytes or string)
    }
}
```

The above creates a websocket service at the URL "/ws/hello" server endpoint.

Please review the example code in the WsEchoDemo class in the rest-spring-3-example project for details.

If you want to use Spring Boot's Tomcat websocket server, you can disable the non-blocking websocket server feature
by removing the `websocket.server.port` configuration and any websocket service classes with the `WebSocketService`
annotation.

To try out the demo websocket server, visit http://127.0.0.1:8083 and select "Websocket demo".

<br/>

|               Chapter-5                |                   Home                    |            Chapter-7            |
|:--------------------------------------:|:-----------------------------------------:|:-------------------------------:|
| [Build, Test and Deploy](CHAPTER-4.md) | [Table of Contents](TABLE-OF-CONTENTS.md) | [Event over HTTP](CHAPTER-7.md) |
