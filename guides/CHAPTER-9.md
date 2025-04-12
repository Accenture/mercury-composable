# API Overview

## Main Application

Each application has an entry point. You may implement an entry point in a main application like this:

```java
@MainApplication
public class MainApp implements EntryPoint {

   public static void main(String[] args) {
      AutoStart.main(args);
   }

   @Override
   public void start(String[] args) {
        // your startup logic here
      log.info("Started");
   }
}
```

In your main application, you must implement the `EntryPoint` interface to override the "start" method.
Typically, a main application is used to initiate some application start up procedure.

In some case when your application does not need any start up logic, you can just print a message to indicate
that your application has started.

You may want to keep the static "main" method which can be used to run your application inside an IDE.

The pom.xml build script is designed to run the `AutoStart` class that will execute your main application's
start method.

In some case, your application may have more than one main application module. You can decide the sequence of
execution using the "sequence" parameter in the `MainApplication` annotation. The module with the smallest
sequence number will run first. Duplicated sequence numbers are allowed. Normal startup sequence must be
between 1 and 999.

*Note*: It is the "start" method of each EntryPoint implementation that follows the execution sequence of the
`MainApplication` annotation. The optional "main" method is used only to kick off the application bootstrap and
it must include only the following statement:

```java
public static void main(String[] args) {
    AutoStart.main(args);
}
```

Therefore, even when the default sequence of the `MainApplication` annotation is 10 and you invoke the "main"
method from an IDE, the "start" method of each MainApplication modules will execute orderly.

## Setup before the Main Application

Sometimes, it may be required to set up some environment configuration before your main application starts.
You can implement a `BeforeApplication` module. Its syntax is similar to the `MainApplication`.

```java
@BeforeApplication
public class EnvSetup implements EntryPoint {

   @Override
   public void start(String[] args) {
        // your environment setup logic here
      log.info("initialized");
   }
}
```

The `BeforeApplication` logic will run before your `MainApplication` module(s). This is useful when you want to do
special handling of environment variables. For example, decrypt an environment variable secret, construct an X.509
certificate, and save it in the "/tmp" folder before your main application starts.

> *Note*: Sequence 0 is reserved by the EssentialServiceLoader and 2 reserved by Event Script.
          Your user functions should use a number between 3 and 999.

## Event envelope

Mercury is an event engine that encapsulates Eclipse Vertx and Kotlin coroutine and suspend function.

A composable application is a collection of functions that communicate with each other in events.
Each event is transported by an event envelope. Let's examine the envelope.

There are 3 elements in an event envelope:

| Element | Type     | Purpose                                                                                                           |
|:-------:|:---------|:------------------------------------------------------------------------------------------------------------------|
|    1    | metadata | Includes unique ID, target function name, reply address<br/> correlation ID, status, exception, trace ID and path |
|    2    | headers  | User defined key-value pairs                                                                                      |
|    3    | body     | Event payload (primitive, hash map or PoJo)                                                                       |

> *Note*: Headers and body are optional, but you should provide at least one of them.

## PoJo transport

Your function can implement the TypedLambdaFunction interface if you want to use PoJo as input and output
and the system will restore PoJo payload accordingly.

However, if you use the EventEnvelope as an input in the TypedLambdaFunction, PoJo payload is mapped as a HashMap
in the event's body.

The original class name of the PoJo payload is saved in the event's type attribute.
You can compare and restore the PoJo like this:

```java
if (SamplePoJo.class.getName().equals(input.getType())) {
    SamplePoJo pojo = input.getBody(SamplePoJo.class);
    // do something with your input PoJo
}
```

## List of Pojo transport

When sending events programmatically, you can send a list of PoJo to a user function. However, the list of pojo
will be converted as a list of maps as input to the target function.

Since type information is lost at runtime, you may add the `inputPojoClass` parameter in the `PreLoad` annotation
of the target function. The system will then render the list of pojo as input to the target function.

This applies to both untyped `LambdaFunction` and `TypedLambdaFunction`. In untyped LambdaFunction, the input is
an object. In TypedLambdaFunction, you should configure the input as list of pojo and add the "inputPojoClass"
hint in the PreLoad annotation. For example, the following unit test illustrates this:

```java
@PreLoad(route = "input.list.of.pojo.java", inputPojoClass = PoJo.class)
public class InputAsListOfPoJo implements TypedLambdaFunction<List<PoJo>, Object> {
    @Override
    public Object handleEvent(Map<String, String> headers, List<PoJo> input, int instance) throws Exception {
        List<String> names = new ArrayList<>();
        // prove that the list of pojo is correctly deserialized
        for (PoJo o: input) {
            if (o != null) {
                names.add(o.getName());
            } else {
                names.add("null");
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("names", names);
        return result;
    }
}
```

> *Note*: List of PoJo as input to a composable function is not supported by input data mapping
          of Event Script. This is only allowed when sending events programmatically for certain
          use cases.

## PoJo deserialization hints

The PoJo class definition in the TypedLambdaFunction has precedence over the "inputPojoClass" hint
in the PreLoad annotation.

For PoJo transport, the "inputPojoClass" parameter in the PreLoad annotation will only be used
when the untyped "LambdaFunction" is declared. This is for backward compatibility with
legacy version 2 and 3 where pojo transport restores pojo as input to user function written as
untyped LambdaFunction.

The list of pojo is handled differently as a deserialization hint in both the untyped LambdaFunction
and the TypedLambdaFunction use cases as discussed earlier.

## Custom exception using AppException

To reject an incoming request, you can throw an AppException like this:

```java
// example-1
throw new AppException(400, "My custom error message");
// example-2
throw new AppException(400, "My custom error message", ex);
```

Example-1 - a simple exception with status code (400) and an error message

Example-2 - includes a nested exception

As a best practice, we recommend using error codes that are compatible with HTTP status codes.

## Defining a user function in Java

You can write a function in Java like this:

```java
@PreLoad(route = "hello.simple", instances = 10)
public class SimpleDemoEndpoint implements TypedLambdaFunction<AsyncHttpRequest, Object> {
    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        // business logic here
        return result;
    }
}
```

The `PreLoad` annotation tells the system to preload the function into memory and register it into the event loop.
You must provide a "route name" and configure the number of concurrent workers ("instances").

Route name is used by the event loop to find your function in memory. A route name must use lower letters and numbers,
and it must have at least one dot as a word separator. e.g. "hello.simple" is a proper route name but "HelloSimple" 
is not.

You can implement your function using the LambdaFunction or TypedLambdaFunction. The latter allows you to define
the input and output classes.

The system will map the event body into the `input` argument and the event headers into the `headers` argument.
The `instance` argument informs your function which worker is serving the current request.

Similarly, you can also write a "suspend function" in Kotlin like this:

```java
@PreLoad(route = "hello.world", instances = 10, isPrivate = false, 
         envInstances = "instances.hello.world")
class HelloWorld : KotlinLambdaFunction<Any?, Map<String, Any>> {

    @Throws(Exception::class)
    override suspend fun handleEvent(headers: Map<String, String>, input: Any?, 
                                     instance: Int): Map<String, Any> {
        // business logic here
        return result;
    }
}
```

In the suspend function example above, you may notice the optional `envInstances` parameter. This tells the system
to use a parameter from the application.properties (or application.yml) to configure the number of workers for the
function. When the parameter defined in "envInstances" is not found, the "instances" parameter is used as the
default value.

## Inspect event metadata

There are some reserved metadata such as route name ("my_route"), trace ID ("my_trace_id") and trace path
("my_trace_path") in the "headers" argument. They do not exist in the incoming event envelope. Instead,
the system automatically insert them as read-only metadata.

They are used to create a trackable instance of the PostOffice. e.g.

```java
var po = new PostOffice(headers, instance);
```

To inspect all metadata, you can declare the input as "EventEnvelope" in a TypedLambdaFunction. The system will
map the whole event envelope into the "input" argument. You can retrieve the replyTo address and other useful items.

> *Note*: The "replyTo" address is optional. It is only required when the caller is making an RPC call.
          If the caller sends an asynchronous request, the "replyTo" value is null.

## Platform API

You can obtain a singleton instance of the Platform object to do the following:

### Register a function

We recommend using the `PreLoad` annotation in a class to declare the function route name, number of worker instances
and whether the function is public or private.

In some use cases where you want to create and destroy functions on demand, you can register them programmatically.

In the following example, it registers "my.function" using the MyFunction class as a public function and 
"another.function" with the AnotherFunction class as a private function. It then registers two kotlin functions
in public and private scope respectively.

```java
Platform platform = Platform.getInstance();

// register a public function
platform.register("my.function", new MyFunction(), 10);

// register a private function
platform.registerPrivate("another.function", new AnotherFunction(), 20);

// register a public suspend function
platform.registerKoltin("my.suspend.function", new MySuspendFunction(), 10);

// register a private suspend function
platform.registerKoltinPrivate("another.suspend.function", new AnotherSuspendFunction(), 10);
```

### What is a public function?

A public function is visible by any application instances in the same network. When a function is declared as
"public", the function is reachable through the Event-over-HTTP REST endpoint or a service mesh.

A private function is invisible outside the memory space of the application instance that it resides.
This allows application to encapsulate business logic according to domain boundary. You can assemble closely
related functions as a composable application that can be deployed independently.

### Release a function

In some use cases, you want to release a function on-demand when it is no longer required.

```text
platform.release("another.function");
```

The above API will unload the function from memory and release it from the "event loop".

### Check if a function is available

You can check if a function with the named route has been deployed.

```text
if (platform.hasRoute("another.function")) {
    // do something
}
```

### Wait for a function to be ready

Functions are registered asynchronously. For functions registered using the `PreLoad` annotation, they are available
to your application when the MainApplication starts.

For functions that are registered on-demand, you can wait for the function to get ready like this:

```text
platform.waitForProvider("cloud.connector", 10)
        .onSuccess(ready -> {
            // business logic when "cloud.connector" is ready 
        });
```
Note that the "onFailure" method is not required. The onSuccess will return true or false. In the above example,
your application waits for up to 10 seconds. If the function (i.e. the "provider") is available, the API will invoke
the "onSuccess" method immediately.

### Obtain the unique application instance ID

When an application instance starts, a unique ID is generated. We call this the "Origin ID".

```java
var originId = po.getOrigin();
```

When running the application in a minimalist service mesh using Kafka or similar network event stream system,
the origin ID is used to uniquely identify the application instance.

The origin ID is automatically appended to the "replyTo" address when making a RPC call over a network event stream
so that the system can send the response event back to the "originator" or "calling" application instance.

### Set application personality

An application may have one of the following personality:

1. REST - the deployed application is user facing
2. APP - the deployed application serves business logic
3. RESOURCES - this is a resource-tier service. e.g. database service, MQ gateway, legacy service proxy, utility, etc.

You can change the application personality like this:

```text
// the default value is "APP"
ServerPersonality.getInstance().setType(ServerPersonality.Type.REST);
```

The personality setting is for documentation purpose only. It does not affect the behavior of your application.
It will appear in the application "/info" endpoint.

## PostOffice API

You can obtain an instance of the PostOffice from the input "headers" and "instance" parameters in the input
arguments of your function.

```java
var po = new PostOffice(headers, instance);
```

The PostOffice is the event manager that you can use to send asynchronous events or to make RPC requests.
The constructor uses the READ only metadata in the "headers" argument in the "handleEvent" method of your function.

For end-to-end traceability, please use the PostOffice instance to make requests to a composable library.
It maintains the same traceId and tracePath in the traceability graph. If your handleEvent method calls another
method in your class, you should pass this PostOffice instance so that any event calls from the other method
can propagate the tracing information.

For Unit Tests, since a test does not start with the handleEvent of a LambdaFunction, you can use the following
to create a PostOffice with your own traceId. The "myRoute" is the caller's route name. In this case, you can
set it to "unit.test".

```java
public PostOffice(String myRoute, String myTraceId, String myTracePath);
```

### Send an asynchronous event to a function

You can send an asynchronous event like this.

```java
// example-1
po.send("another.function", "test message");

// example-2
po.send("another.function", new Kv("some_key", "some_value"), new kv("another_key", "another_value"));

// example-3
po.send("another.function", somePoJo, new Kv("some_key", "some_value"));

// example-4
EventEnvelope event = new EventEnvelope().setTo("another.function")
                            .setHeader("some_key", "some_value").setBody(somePoJo);
po.send(event)

// example-5
po.sendLater(event, new Date(System.currentTimeMillis() + 5000));
```

1. Example-1 sends the text string "test message" to the target service named "another.function".
2. Example-2 sends two key-values as "headers" parameters to the same service.
3. Example-3 sends a PoJo and a key-value pair to the same service.
4. Example-4 is the same as example-3. It is using an EventEnvelope to construct the request.
5. Example-5 schedules an event to be sent 5 seconds later.

The first 3 APIs are convenient methods and the system will automatically create an EventEnvelope to hold the
target route name, key-values and/or event payload.

### Make an asynchronous RPC call

You can make RPC call like this:

```java
// example-1
EventEnvelope request = new EventEnvelope().setTo("another.function")
                            .setHeader("some_key", "some_value").setBody(somePoJo);
Future<EventEnvelope> response = po.asyncRequest(request, 5000);
response.onSuccess(result -> {
    // result is the response event
});
response.onFailure(e -> {
    // handle timeout exception
});

// example-2
Future<EventEnvelope> response = po.asyncRequest(request, 5000, false);
response.onSuccess(result -> {
    // result is the response event
    // Timeout exception is returned as a response event with status=408
});

// example-3 with the "rpc" boolean parameter set to true
Future<EventEnvelope> response = po.asyncRequest(request, 5000, "http://peer/api/event", true);
response.onSuccess(result -> {
    // result is the response event
});
response.onFailure(e -> {
    // handle timeout exception
});
```

1. Example-1 makes a RPC call with a 5-second timeout to "another.function".
2. Example-2 sets the "timeoutException" to false, telling system to return timeout exception as a regular event.
3. Example-3 makes an "event over HTTP" RPC call to "another.function" in another application instance called "peer".

"Event over HTTP" is an important topic. Please refer to [Chapter 7](CHAPTER-7.md) for more details.

### Perform a fork-n-join RPC call to multiple functions

In a similar fashion, you can make a fork-n-join call that sends request events in parallel to more than one function.

```java
// example-1
EventEnvelope request1 = new EventEnvelope().setTo("this.function")
                            .setHeader("hello", "world").setBody("test message");
EventEnvelope request2 = new EventEnvelope().setTo("that.function")
                            .setHeader("good", "day").setBody(somePoJo);
List<EventEnvelope> requests = new ArrayList<>();
requests.add(request1);
requests.add(request2);
Future<List<EventEnvelope>> responses = po.asyncRequest(requests, 5000);
response.onSuccess(results -> {
    // results contains the response events
});
response.onFailure(e -> {
    // handle timeout exception
});

// example-2
Future<List<EventEnvelope>> responses = po.asyncRequest(requests, 5000, false);
response.onSuccess(results -> {
    // results contains the response events.
    // Partial result list is returned if one or more functions did not respond.
});
```

### Make a sequential non-blocking RPC call

You can make a sequential non-blocking RPC call from one function to another.

The most convenient method to make a sequential non-blocking RPC call is to use the PostOffice's request API.

```java
// for a single RPC call
PostOffice po = new PostOffice(headers, instance);
EventEnvelope result = po.request(requestEvent, timeoutInMills).get();

// for a fork-n-join call
PostOffice po = new PostOffice(headers, instance);
List<EventEnvelope> result = po.request(requestEvents, timeoutInMills).get();
```

### Check if a function with a named route exists

The PostOffice provides the "exists()" method that is similar to the "platform.hasRoute()" command.

The difference is that the "exists()" method can discover functions of another application instance when running
in the "service mesh" mode.

If your application is not deployed in a service mesh, the PostOffice's "exists" and Platform's "hasRoute" APIs
will provide the same result.

```java
boolean found = po.exists("another.function");
if (found) {
    // do something
}
```

### Retrieve trace ID and path

If you want to know the route name and optional trace ID and path, you can use the following APIs.

For example, if tracing is enabled, the trace ID will be available. You can put the trace ID in application log
messages. This would group log messages of the same transaction together when you search the trace ID from 
a centralized logging dashboard such as Splunk.

```java
String myRoute = po.getRoute();
String traceId = po.getTraceId();
String tracePath = po.getTracePath();
```

## Trace annotation

To annotate additional information in the trace of your function, please obtain a trackable PostOffice
instance using `new PostOffice(headers, instance)` and follow the following API signatures:

```java
// API signatures
public PostOffice annotateTrace(String key, String value);
public PostOffice annotateTrace(String key, Map<String, Object> value);
public PostOffice annotateTrace(String key, List<Object> value);

// For example,
var po = new PostOffice(headers, instance);
po.annotateTrace("hello", "world");
```

Annotations of key-values, if any, will be recorded in the trace and they are not accessible by
another function.

Please be moderate to attach only *small amount of transaction specific information* to the
performance metrics of your functions.

> *Note*: Don't annotate sensitive information or secrets such as PII, PHI, PCI data because 
          the trace is visible in the application log. It may also be forwarded to a centralized
          telemetry dashboard for visualization and analytics.

## Configuration API

Your function can access the main application configuration from the platform like this:

```java
AppConfigReader config = AppConfigReader.getInstance();
// the value can be string or a primitive
Object value = config.get("my.parameter");
// the return value will be converted to a string
String text = config.getProperty("my.parameter");
```

The system uses the standard dot-bracket format for a parameter name. e.g.

```properties
hello.world
some.key[2]
```

You can override the main application configuration at run-time using the Java argument "-D". e.g.

> java -Dserver.port=8080 -jar myApp.jar

Additional configuration files can be added with the `ConfigReader` constructor like this:

```java
// filePath should have location prefix "classpath:/" or "file:/"
ConfigReader reader = new ConfigReader(filePath);
```

The configuration system supports environment variable or reference to the main application configuration
using the dollar-bracket syntax `${reference:default_value}`. e.g.

```properties
some.key=${MY_ENV_VARIABLE}
another.key=${my.key:12345}
complex.key=first ${FIRST_ENV_VAR}, second ${SECOND_ENV_VAR}
```

In the above example, a parameter may contain references to more than one environment variable.

Default value, if not given, will be assumed to be an empty string.

## Custom serializer

We are using GSON as the underlying serializer to handle common use cases. However, there may be
situation that you want to use your own custom serialization library.

To do that, you may write a serializer that implements the CustomSerializer interface:

```java
public interface CustomSerializer {

    public Map<String, Object> toMap(Object obj);

    public <T> T toPoJo(Object obj, Class<T> toValueType);

}
```

You may configure a user function to use a custom serializer by adding the "customSerializer" parameter
in the `PreLoad` annotation. For example,

```java
@PreLoad(route="my.user.function", customSerializer = JacksonSerializer.class)
public class MyUserFunction implements TypedLambdaFunction<SimplePoJo, SimplePoJo> {
    @Override
    public SimplePoJo handleEvent(Map<String, String> headers, SimplePoJo input, int instance) {
        return input;
    }
}
```

If you register your function dynamically in code, you can use the following `platform API` to assign
a custom serializer.

```java
public void setCustomSerializer(String route, CustomSerializer mapper);
// e.g.
// platform.setCustomSerializer("my.function", new JacksonSerializer());
```

If you use the PostOffice to programmatically send event or make event RPC call and you need
custom serializer, you can create a PostOffice instance like this:

```java
// this should be the first statement in the "handleEvent" method.
PostOffice po = new PostOffice(headers, instance, new MyCustomSerializer());
```

The outgoing event using the PostOffice will use the custom serializer automatically.

To interpret an event response from a RPC call, you can use the following PostOffice API:

```java
MyPoJo result = po.getEventBodyAsPoJo(responseEvent, MyPoJo.class);
```

## Minimalist API design

As a best practice, we advocate a minimalist approach in API integration.
To build powerful composable applications, the above set of APIs is sufficient to perform
"event orchestration" where you write code to coordinate how the various functions work together as a
single "executable". Please refer to [Chapter-4](CHAPTER-4.md) for more details about event orchestration. 

Since Mercury is used in production installations, we will exercise the best effort to keep the core API stable.

Other APIs in the toolkits are used internally to build the engine itself, and they may change from time to time.
They are mostly convenient methods and utilities. The engine is fully encapsulated and any internal API changes
are not likely to impact your applications.

## Event Scripting

To further reduce coding effort, you can perform "event choreography" by configuration using "Event Script".
Please refer to Event Script syntax in [Chapter 4](CHAPTER-4.md)

## Co-existence with other development frameworks

Mercury libraries are designed to co-exist with your favorite frameworks and tools. Inside a class implementing
the `LambdaFunction`, `TypedLambdaFunction` or `KotlinLambdaFunction`, you can use any coding style and frameworks
as you like, including sequential, object-oriented and reactive programming styles.

The core-engine has a built-in lightweight non-blocking HTTP server, but you can also use Spring Boot and other
application server framework with it.

A sample Spring Boot integration is provided in the "rest-spring-3" project. It is an optional feature, and you can
decide to use a regular Spring Boot application with Mercury Composable or to pick the customized Spring Boot in the
"rest-spring-3" library.

## Application template for quick start

We recommend using the `composable-example` project as a template to start writing your own Composable applications.
You can follow the Composable methodology where you draw event flow diagrams to represent various use cases,
convert them into event scripts that carry out event choreography for your self-contained functions.

For more information, please refer to Event Script syntax in [Chapter 4](CHAPTER-4.md).

If you prefer to do low-level event-driven programming, you can use the `lambda-example` project as a template.
It is preconfigured to support kernel threads, coroutine and suspend function.

## Source code update frequency

This project is licensed under the Apache 2.0 open sources license. We will update the public codebase after
it passes regression tests and meets stability and performance benchmarks in our production systems.

Mercury Composable is developed as an engine for you to build the latest cloud native applications.

Composable technology is evolving rapidly. We would exercise best effort to keep the essential internals
and core APIs stable. Please browse the latest Developer Guide, release notes and Javadoc for any breaking
API changes.

## Technical support

For enterprise clients, technical support is available. Please contact your Accenture representative for details.
<br/>

|                Chapter-8                |                   Home                    |
|:---------------------------------------:|:-----------------------------------------:|
| [Minimalist Service Mesh](CHAPTER-8.md) | [Table of Contents](TABLE-OF-CONTENTS.md) | 
