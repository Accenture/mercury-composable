# Design notes

## Event choreography by configuration

The recommended way to write a composable application is event choreography by configuration using "Event Script".

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

### Handling numbers in a Map

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

## Input using Map or PoJo

The input to a TypedLambdaFunction should be a Map or PoJo. A map allows you to use flexible data
structure and a PoJo would enforce the interface contract. List of PoJo is not supported.

First, this design improves the readability of "input data mapping" configuration in Event Script.
Second, this avoids edge cases in serialization.

## Keys of Map

The system enforces the use of strings as keys in a map for reliable serialization.

For example, the configuration management module will convert integers and other types as strings
for keys in a configuration.

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

Since Mercury version 4, the event script engine is integrated with the platform-core. This adds
event choreography capability directly in the event system. Event script describes a transaction as
an event flow configuration that drives composable functions to work together as a single application.
A composable function, by design, is self-contained with I/O immutability.

## Spring Boot 3

The `platform-core` includes a non-blocking HTTP and websocket server for standalone operation without
Spring Boot. The `rest-spring-3` library is designed to turn your code to be a Spring Boot application.

You may also use the `platform-core` library with a regular Spring Boot application without the
`rest-spring-3` library if you prefer.

## Support of Mono and Flux results

A user function may return a regular result that can be a PoJo, HashMap or Java primitive.

It can also return a Mono or Flux reactive response object for a future result or a future series of
results. Other reactive response objects must be converted to a Mono or Flux object.
