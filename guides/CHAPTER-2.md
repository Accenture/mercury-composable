# Function Execution Strategies

## Define a function

In a composable application, each function is self-contained with zero dependencies with other user functions.

Only flow adapter, data adapter, notification function or gateway has a single external dependency such as
a network event system, a database or an external REST resource.

A "task" or "function" is a class that implements the LambdaFunction or TypedLambdaFunction interface. 
Within each function boundary, it may have private methods that are fully contained within the class.

As discussed in Chapter-1, a function may look like this:

```java
@PreLoad(route = "my.first.function", instances = 10)
public class MyFirstFunction implements TypedLambdaFunction<MyPoJo, AnotherPoJo> {

    @Override
    public AnotherPojo handleEvent(Map<String, String> headers, MyPoJo input, int instance) {
        // your business logic here
        return result;
    }
}
```

A function is an event listener with the "handleEvent" method. The data structures of input and output are defined
by API interface contract in an event flow configuration.

In the above example, the input is MyPoJo and the output is AnotherPoJo.

For event choreography, input body is represented as a PoJo or a Map of key-values so that you can use the
dot-bracket convention to map subset of a PoJo from one function to another if needed.

In addition to the input PoJo, you may pass additional parameters to the user function as event headers.
We will discuss this in [Chapter 4 - Event Script Syntax](CHAPTER-4.md).

## Non-blocking design

While you can apply sequential, object-oriented or reactive programming styles in your functions, you should pay
attention to making your function non-blocking and fast.

In a virtual thread, if you use Java Future, the ".get()" method is synchronous but it is non-blocking behind the
curtain. This is like using the "await" keyword in other programming language.

Virtual thread execution promotes performance and high concurrency. However, it would be suboptimal
if you mix blocking code in a user function. It will block the whole event loop, resulting in substantial
degradation of application performance. We therefore recommend your user function to be implemented in non-blocking
or reactive styles.

When you are using a reactive library in your function, your function can return a "Mono" or "Flux" reactive
response object using the Project-Reactor Core library.

For simplicity, we support only the Mono and Flux reactive response objects. If you use other types of reactive APIs,
please convert them into a Mono or Flux accordingly.

## User function that returns a Mono object

For Mono return value, a reactive user function may look like this:

```java
@PreLoad(route = "v1.reactive.mono.function")
public class MonoUserFunction implements TypedLambdaFunction<Map<String, Object>, Mono<Map<String, Object>>> {
    private static final Logger log = LoggerFactory.getLogger(MonoUserFunction.class);

    private static final String EXCEPTION = "exception";

    @Override
    public Mono<Map<String, Object>> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        log.info("GOT {} {}", headers, input);
        return Mono.create(callback -> {
            if (headers.containsKey(EXCEPTION)) {
                callback.error(new AppException(400, headers.get(EXCEPTION)));
            } else {
                callback.success(input);
            }
        });
    }
}
```

## User function that returns a Flux object

For Flux return value, it may look like this:

```java
@PreLoad(route = "v1.reactive.flux.function")
public class FluxUserFunction implements TypedLambdaFunction<Map<String, Object>, Flux<Map<String, Object>>> {
    private static final Logger log = LoggerFactory.getLogger(FluxUserFunction.class);

    private static final String EXCEPTION = "exception";
    @Override
    public Flux<Map<String, Object>> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        log.info("GOT {} {}", headers, input);
        return Flux.create(emitter -> {
            if (headers.containsKey(EXCEPTION)) {
                emitter.error(new AppException(400, headers.get(EXCEPTION)));
            } else {
                // just generate two messages
                emitter.next(Map.of("first", "message"));
                emitter.next(input);
                emitter.complete();
            }
        });
    }
}

```

## Handling a Flux stream

When your function returns a Flux stream object, the system will pass the stream ID of the underlying event stream
to the calling function.

The input arguments for the event stream ID and time-to-live parameters are provided in the event headers
to your function that implements the TypedLambdaFunction or LambdaFunction.

The following event headers will be provided to the calling function:

```yaml
x-stream-id: streamId
x-ttl: ttl
```

In the calling function, you can create a `FluxConsumer` to handle the incoming event stream like this:

```java
String streamId = headers.get("x-stream-id");
long ttl = Utility.getInstance().str2long(headers.get("x-ttl"));
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

The API signatures for FluxConsumer are as follows:

```java
// Consume the event stream when the payload is not a PoJo
public void consume(Consumer<T> consumer,
                    Consumer<Throwable> errorConsumer,
                    Runnable completeConsumer);

// Consume the event stream when the payload can be mapped as PoJo
public void consume(Consumer<T> consumer,
                    Consumer<Throwable> errorConsumer,
                    Runnable completeConsumer, Class<T> pojoClass);

// Consume the event stream when the payload can be mapped as PoJo using a custom serializer
public void consume(Consumer<T> consumer,
                    Consumer<Throwable> errorConsumer,
                    Runnable completeConsumer,
                    Class<T> pojoClass, CustomSerializer serializer);                                       
```

## Serialization consideration

If you use the FluxConsumer's consume method without pojoClass hint, the system will deliver
Java primitive and HashMap through an event stream. If you pass PoJo, HashMap or Java primitive such as
String or byte[], you do not need to do any serialization.

If the objects that your function streams over a Mono or Flux channel are not supported, you must perform
custom serialization. This can be achieved using the "map" method of the Mono or Flux class.

For example, your function obtains a stream of Flux result objects from a database call. You can serialize
the objects using a custom serializer like this:

```java
// "source" is the original Flux object
Flux<Map<String, Object> serializedStream = source.map(specialPoJo -> {
    return myCustomSerializer.toMap(specialPoJo);
});
return serializedStream;
```

Your customSerializer should implement the org.platformlambda.core.models.CustomSerializer interface.

```java
public interface CustomSerializer {
    public Map<String, Object> toMap(Object obj);
    public <T> T toPoJo(Object obj, Class<T> toValueType);
}
```

## Extensible authentication function

You can add authentication function using the optional `authentication` tag in a service. In "rest.yaml", a service
for a REST endpoint refers to a function in your application.

An authentication function can be written using a TypedLambdaFunction that takes the input as a "AsyncHttpRequest".
Your authentication function can return a boolean value to indicate if the request should be accepted or rejected.

A typical authentication function may validate an HTTP header or cookie. e.g. forward the "Bearer token" from the
"Authorization" header to your organization's OAuth 2.0 Identity Provider for validation.

To approve an incoming request, your custom authentication function can return `true`.

Optionally, you can add "session" key-values by returning an EventEnvelope like this:

```shell
return new EventEnvelope().setHeader("user_id", "A12345").setBody(true);
```

The above example approves the incoming request and returns a "session" variable ("user_id": "A12345") to the
next task.

If your authentication function returns `false`, the user will receive a "HTTP-401 Unauthorized" error response.

You can also control the status code and error message by throwing an `AppException` like this:

```shell
throw new AppException(401, "Invalid credentials");
```

Alternatively, you may implement authentication as a user function in the first step of an event flow. In this case,
the input to the function is defined by the "input data mapping" rules in the event flow configuration. 

The advantage of this approach is that authentication is shown as part of an event flow so that the application design
intention is clear.

A composable application is assembled from a collection of self-contained functions that are highly reusable.

## Number of workers for a function

In the following annotation, the parameter "instances" tells the system to reserve a number of workers for the function.
Workers are running on-demand to handle concurrent user requests.

```java
@PreLoad(route = "my.first.function", instances = 10)
```

Note that you can use smaller number of workers to handle many concurrent users if your function finishes
processing very quickly. If not, you should reserve more workers to handle the work load.

Concurrency requires careful planning for optimal performance and throughput. 
Let's review the strategies for function execution.

## Three strategies for function execution

A function is executed when an event arrives. There are three function execution strategies.

| Strategy         | Advantage                                                 | Disadvantage                                                                   |
|:-----------------|:----------------------------------------------------------|:-------------------------------------------------------------------------------|
| Virtual thread   | Higher throughput in terms of<br/>concurrent users        | N/A                                                                            |
| Kernel threads   | Higher performance in terms of<br/>operations per seconds | Lower number of concurrent threads<br/>due to high context switching overheads |

### Virtual thread

By default, the system will run your function as a virtual thread because this is the most efficient execution
strategy.

In a virtual thread, the "Thread" object in the standard library will operate in non-blocking mode. This means
it is safe to use the Thread.sleep() method. It will release control to the event loop when your function enters
into sleep, thus freeing CPU resources for other functions.

We have added the "request" methods in the PostOffice API to support non-blocking RPC that leverages this
suspend/resume feature of virtual thread management.

```java
Future<EventEnvelope> future = po.request(requestEvent, timeout);
EventEnvelope result = future.get();

// alternatively, you can do:
EventEnvelope result = po.request(requestEvent, timeout).get();
```

> *Note*: The PostOffice API is used when you want to do orchestration by code. If you are using Event Script, you can
  manage event flows using one or more configuration files.

### Kernel thread pool

When you add the annotation "KernelThreadRunner" in a function declared as LambdaFunction or TypedLambdaFunction, 
the function will be executed using a "kernel thread pool" and Java will run your function in native 
"preemptive multitasking" mode.

While preemptive multitasking fully utilizes the CPU, its context switching overheads increase as the number of
kernel threads grow. As a rule of thumb, you should control the maximum number of kernel threads to be less than 200.

The parameter `kernel.thread.pool` is defined with a default value of 100. You can change this value to adjust to
the actual CPU power in your environment. Keep the default value for best performance unless you have tested the
limit in your environment.

> *Note*: When you have more concurrent requests, your application may slow down because some functions
  are blocked when the number of concurrent kernel threads is reached.

You should reduce the number of "instances" (i.e. worker pool) for a function to a small number so that your
application does not exceed the maximum limit of the `kernel.thread.pool` parameter.

Kernel threads are precious and finite resources. When your function is computational intensive or making
external HTTP or database calls in a synchronous blocking manner, you may use it with a small number
of worker instances.

To rapidly release kernel thread resources, you should write "asynchronous" code. i.e. for event-driven programming,
you can use send event to another function asynchronously, and you can create a callback function to listen
to responses.

For RPC call, you can use the `asyncRequest` method to make asynchronous RPC calls. However, coding for asynchronous
pattern is more challenging. For example, you may want to return a "pending" result immediately using HTTP-202.
Your code will move on to execute using a "future" that will execute callback methods (`onSuccess` and `onFailure`).
Another approach is to annotate the function as an `EventInterceptor` so that your function can respond to the user
in a "future" callback.

For ease of programming, we recommend using virtual thread to handle synchronous RPC calls in a non-blocking manner.

## Solving the puzzle of multithreading performance

Before the availability of virtual thread technology in Java 21, Java VM has been using kernel threads for code
execution. If you have a lot of users hitting your service concurrently, multiple threads are created to serve
concurrent requests.

When your code serving the requests makes blocking call to other services, the kernel threads are busy while your
user functions wait for responses. Kernel threads that are in the wait state is still consuming CPU time.

If the blocking calls finish very quickly, this is not be an issue.

However, when the blocking calls take longer to complete, a lot of outstanding kernel threads that are waiting
for responses would compete for CPU resources, resulting in higher internal friction in the JVM that makes your
application running slower. This is not a productive use of computer resources.

This type of performance issue caused by internal friction is very difficult to avoid. While event driven and
reactive programming that uses asynchronous processing and callbacks would address this artificial bottleneck,
asynchronous code is harder to implement and maintain when the application complexity increases.

It would be ideal if we can write sequential code that does not block. Sequential code is much easier to write
and read because it communicates the intent of the code clearly.

Leveraging Java 21 virtual thread technology, Mercury Composable allows the developer to write code in a sequential
manner. When code in your function makes an RPC call to another service using the PostOffice's "request" API, it
returns a Java Future object but the "Future" object itself is running in a virtual thread. This means when your code
retrieves the RPC result using the "get" method, your code appears "blocked" while waiting for the response
from the target service.

Although your code appears to be "blocked", the virtual thread is “suspended”. It will wake up when the response
arrives. When a virtual thread is suspended, it does not consume CPU time and the memory structure for keeping
the thread in suspend mode is very small. Virtual thread technology is designed to support tens of thousands
of concurrent RPC requests in a single compute machine, container or serverless instance.

Mercury Composable supports mixed thread management - virtual threads and kernel threads.

Functions running in different types of threads are connected loosely in events. This functional isolation
and encapsulation mean that you can precisely control how your application performs for each functional logic block.
<br/>

|          Chapter-1           |                   Home                    |            Chapter-3            |
|:----------------------------:|:-----------------------------------------:|:-------------------------------:|
| [Introduction](CHAPTER-1.md) | [Table of Contents](TABLE-OF-CONTENTS.md) | [REST Automation](CHAPTER-3.md) |
