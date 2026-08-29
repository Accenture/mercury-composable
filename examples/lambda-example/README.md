# Composable application example

The lambda-example demonstrates REST automation (lightweight non-blocking HTTP server) that allows you
to create REST endpoints by configuration instead of code.

It illustrates building individual event-driven functions using Java (LambdaFunction and TypedLambdaFunction) and
Kotlin (suspend function that implements the KotlinLambdaFunction interface).

Unit test examples are also provided.

## Spring Boot

This sample application does not have Spring framework or Spring Boot dependencies so that it can be used
with Spring Boot or other frameworks.

The "spring-boot-parent" dependency in the pom.xml is a convenient way to fetch latest open sources libraries
that have been vetted by the Spring community.

## Application configuration

You can define application configuration parameters in either application.properties or application.yml.

If you have both application.properties and application.yml, the system will evaluate both configuration files.

When the same parameter is defined in both application.properties and application.yml, the parameter in
application.properties will be used.

While application.properties can also store text based key-values, application.yml supports text, numbers, boolean,
list and map values.

## Progressive result set rendering (SSE demo)

The `hello.sse` function ([HelloSse.java](src/main/java/org/platformlambda/services/HelloSse.java))
serves an HTTP endpoint with progressive result set rendering - it streams test messages
slowly as Server-Sent Events so you can watch them render one by one. The endpoint is
declared with `stream: true` in rest.yaml, which gives each request a dedicated ordered
reply lane for the multi-shot response.

Start the application:

```shell
java -jar target/lambda-example-4.11.12.jar
```

Then consume the endpoint with the companion script (Node.js 18+) that prints each
event with its arrival time:

```shell
node scripts/sse-client.mjs
```

or with curl:

```shell
curl -N -H 'accept: text/event-stream' http://127.0.0.1:8085/api/hello/sse
```

The `-N` (`--no-buffer`) flag matters: curl receives the events progressively either
way, but without `-N` it holds output in an internal buffer, so the messages would
appear all at once when the stream ends.

Optional query parameters: `delay` in milliseconds between messages (default 1000)
and `count` for the number of messages (default 10), e.g. `/api/hello/sse?delay=500&count=5`.
