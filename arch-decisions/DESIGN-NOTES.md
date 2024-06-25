# Design notes

## Orchestration by configuration

The recommended way to write a composable application is orchestration by configuration using "Event Script".

This would potentially reduce code size by half.

## Support sequential synchronous RPC in a non-blocking fashion

The foundation library (platform-core) has been integrated with Java 21 virtual thread and 
Kotlin suspend function features.

When a user function makes a RPC call using virtual thread or suspend function, 
the user function appears to be "blocked" so that the code can execute sequentially.
Behind the curtain, the function is actually "suspended".

This makes sequential code with RPC performs as good as reactive code. 
More importantly, the sequential code represents the intent of the application clearly,
thus making code easier to read and maintain.

## Low level control of function execution strategies

You can precisely control how your functions execute, using virtual threads, suspend functions
or kernel thread pools to yield the highest performance and throughput.

## Serialization

### Gson

We are using Gson for its minimalist design.

We have customized the serialization behavior to be similar to Jackson and other serializers. 
i.e. Integer and long values are kept without decimal points.

For API functional compatibility with Jackson, we have added the writeValueAsString,
writeValueAsBytes and readValue methods.

The convertValue method has been consolidated into the readValue method.

### MsgPack

For efficient and serialization performance, we use MsgPack as schemaless binary transport for
EventEnvelope that contains event metadata, headers and payload.

### User provided serializers

This provides more flexibility for user function to take full control of their PoJo serialization needs.

### Custom JSON and XML serializers

For consistency, we have customized Spring Boot and Servlet serialization and exception handlers.

## Reactive design

Mercury uses the temporary local file system (`/tmp`) as an overflow area for events when the
consumer is slower than the producer. This event buffering design means that user application
does not have to handle back-pressure logic directly.

However, it does not restrict you from implementing your flow-control logic.

## In-memory event system

In Mercury version 1, the Akka actor system is used as the in-memory event bus.
Since Mercury version 2, we have migrated from Akka to Eclipse Vertx.

In Mercury version 3, we extend the engine to be fully non-blocking with low-level control
of application performance and throughput.

In Mercury version 3.1, the platform core engine is fully integrated with Java 21 virtual thread.

## Spring Boot 3

The `platform-core` includes a non-blocking HTTP and websocket server for standalone operation without
Spring Boot. The `rest-spring-3` library is designed to turn your code to be a Spring Boot application.

You may also use the `platform-core` library with a regular Spring Boot application without the
`rest-spring-3` library if you prefer.
