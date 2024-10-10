# Event Script Syntax

Event Script uses YAML to represent an end-to-end transaction flow. A transaction is a business use case, and
the flow can be an API service, a batch job or a real-time transaction.

## flows.yml

This configuration file sits in the project "resources" project and contains a list of filenames.

It may look like this.
```yaml
flows:
  - 'get-profile.yml'
  - 'create-profile.yml'
  - 'delete-profile.yml'

location: 'classpath:/flows/'
```

The "location" tag is optional. If present, you can tell the system to load the flow config files from
another folder location.

## Writing new REST endpoint and function

You can use the "flow-demo" subproject as a template to write your own composable application.

For each filename in the flows.yml, you should create a corresponding configuration file under the
"resources/flows" folder.

Let's write a new flow called "greetings". You can copy-n-paste the following into a file called "greetings.yml"
under the "resources/flows" folder.

```yaml
flow:
  id: 'greetings'
  description: 'Simplest flow'
  ttl: 10s

first:
  task: 'greeting.demo'

tasks:
    - input:
        - 'input.path_parameter.user -> user'
      process: 'greeting.demo'
      output:
        - 'text(application/json) -> output.header.content-type'
        - 'result -> output.body'
      description: 'Hello World'
      execution: end
```

In the application.properties, you can specify the following parameter:

```
yaml.flow.automation=classpath:/flows.yaml
```

and update the "flows.yaml" file in the resources folder as follows:

```yaml
flows:
  - 'get-profile.yml'
  - 'create-profile.yml'
  - 'delete-profile.yml'
  - 'greetings.yml'
```

Then, you can add a new REST endpoint in the "rest.yaml" configuration file like this.

```yaml
  - service: "http.flow.adapter"
    methods: ['GET']
    url: "/api/greeting/{user}"
    flow: 'greetings'
    timeout: 10s
    cors: cors_1
    headers: header_1
```

The above REST endpoint takes the path parameter "user". The task executor will map the path parameter to the
input arguments (headers and body) in your function. Now you can write your new function with the named route
"greeting.demo". Please copy-n-paste the following into a Java class called "Greetings" and save in the package
under "my.organization.tasks" in the source project.

> "my.organization" package name is an example. Please replace it with your actual organization package path.

```java
@PreLoad(route="greeting.demo", instances=10, isPrivate = false)
public class Greetings implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    private static final String USER = "user";

    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        if (input.containsKey(USER)) {
            String user = input.get(USER).toString();
            Map<String, Object> result = new HashMap<>();
            result.put(USER, user);
            result.put("message", "Welcome");
            result.put("time", new Date());
            return result;
        } else {
            throw new IllegalArgumentException("Missing path parameter 'user'");
        }
    }
}
```

For the flow-engine to find your new function, please update the key-value for "web.component.scan" in
application.properties:

```properties
web.component.scan=com.accenture,my.organization
```

To test your new REST endpoint, flow configuration and function, please point your browser to

```text
http://127.0.0.1:8100/api/greeting/my_name
```

You can replace "my_name" with your first name to see the response to the browser.

## Flow configuration syntax

In your "greetings.yml" file above, you find the following key-values:

`flow.id` - Each flow must have a unique flow ID. The flow ID is usually originated from a user facing endpoint
through an event adapter. For example, you may write an adapter to listen to a cloud event in a serverless deployment.
In The most common one is the HTTP adapter.

The flow ID is originated from the "rest.yaml". The `flow-engine` will find the corresponding flow configuration
and create a new flow instance to process the user request.

`flow.description` - this describes the purpose of the flow

`flow.ttl` - "Time to live (TTL)" timer for each flow. You can define the maximum time for a flow to finish processing.
All events are delivered asynchronously and there is no timeout value for each event. The TTL defines the time budget
for a complete end-to-end flow. Upon expiry, an unfinished flow will be aborted.

`first.task` - this points to the route name of a function (aka "task") to which the flow engine will deliver
the incoming event.

The configuration file contains a list of task entries where each task is defined by "input", "process", "output"
and "execution" type. In the above example, the execution type is "end", meaning that it is the end of a transaction
and its result set can be sent to the user.

## Underlying Event System

The Event Script system uses platform-core as the event system where it encapsulates Java Virtual Threads,
Eclipse Vertx, Kotlin coroutine and suspend function.

The integration points are intentionally minimalist. For most use cases, the user application does not need
to make any API calls to the underlying event system.

## REST automation and HTTP adapter

The most common transaction entry point is a REST endpoint. The event flow may look like this:

```text
Request -> "http.request" -> "task.executor" -> user defined tasks -> "async.http.response" -> Response
```

REST automation is part of the Mercury platform-core library. It contains a non-blocking HTTP server that converts
HTTP requests and responses into events.

It routes an HTTP request event to the HTTP adapter if the "flow" tag is provided.

In the following example, the REST endpoint definition is declared in a "rest.yaml" configuration. It will route
the URI "/api/decision" to the HTTP adapter that exposes its service route name as "http.flow.adapter".

```yaml
rest:
  - service: "http.flow.adapter"
    methods: ['GET']
    url: "/api/decision?decision=_"
    flow: 'decision-test'
    timeout: 10s
    cors: cors_1
    headers: header_1
    tracing: true
```

The "cors" and "headers" tags are optional. When specified, the REST endpoint will insert CORS headers and HTTP request
headers accordingly.

For rest.yaml syntax, please refer to https://accenture.github.io/mercury-composable/guides/CHAPTER-3

The HTTP adapter maps the HTTP request dataset and the flow ID into a standard event envelope for delivery to the
flow engine.

The HTTP request dataset, addressable with the "input." namespace, contains the following:

| Key            | Values                                         |
|:---------------|:-----------------------------------------------|
| method         | HTTP method                                    |
| uri            | URI path                                       |
| header         | HTTP headers                                   |
| cookie         | HTTP cookies                                   |
| path_parameter | Path parameters if any                         |
| query          | HTTP query parameters if any                   |
| body           | HTTP request body if any                       |
| stream         | input stream route ID if any                   |
| ip             | remote IP address                              |
| filename       | filename if request is a multipart file upload |
| session        | authenticated session key-values if any        |

For easy matching, keys of headers, cookies, query and path parameters are case-insensitive.

Regular API uses JSON and XML and they will be converted to a hashmap in the event's body.

For special use cases like file upload/download, your application logic may invoke a streaming API to retrieve
the binary payload. Please refer to the following mercury 3.0 guide for details.

> https://accenture.github.io/mercury-composable/guides/APPENDIX-III/#send-http-request-body-as-a-stream

> https://accenture.github.io/mercury-composable/guides/APPENDIX-III/#read-http-response-body-stream

## Task and its corresponding function

Each task in a flow must have a corresponding function. You can assign a task name to the function using the
`Preload` annotation like this.

```java
@PreLoad(route="greeting.demo", instances=10)
public class Greetings implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    @Override
    public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
        // business logic here
        return someOutput;
    }
}
```

The "route" in the Preload annotation is the task name. The "instances" define the maximum number of "workers" that
the function can handle concurrently. The system is designed to be reactive and the function does not consume memory
and CPU resources until an event arrives.

You may also define concurrency using environment variable. You can replace the "instances" with `envInstances` using
standard environment variable syntax like `${SOME_ENV_VARIABLE:default_value}`.

## Assigning multiple route names to a single function

When the same function is reused in a single event flow configuration script, you would need multiple route names for
the same function. It is because a unique route name is required to define a "task" that is associated with a function.

You can use a comma separated list as the route name like this:

```java
@PreLoad(route="greeting.case.1, greeting.case.2", instances=10)
public class Greetings implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>>
```

## Overriding the route name of a reusable composable library

However, if you want to publish your function as a reusable library in the artifactory, you should use a single
route name and use a "preload-override.yaml" file to override the default route name to a list of route names
that are used in your event flow configuration.

```yaml
preload:
  - original: 'greeting.demo'
    routes:
      - 'greeting.case.1'
      - 'greeting.case.2'
    # "instances" tag is optional
    instances: 20
  - original: 'v1.another.reusable.function'
    routes:
      - 'v1.reusable.1'
      - 'v1.reusable.2'
```

In the above example, the function associated with "greeting.demo" will be preloaded as "greeting.case.1"
and "greeting.case.2". The number of maximum concurrent instances is also changed from 10 to 20.

Note that the second example "v1.another.reusable.function" is overridden as "v1.reusable.1" and "v1.reusable.2"
and the number of concurrent instances is not changed.

Assuming the above file is "preload-override.yaml" in the "resources" folder of the application source code project, 
you should add the following parameter in application.properties to activate this preload override feature.

```properties
yaml.preload.override=classpath:/preload-override.yaml
```

## Hierarchy of flows

Inside a flow, you can run one or more sub-flows.

To do this, you can use the flow protocol identifier (`flow://`) to indicate that the task is a flow.

For example, when running the following task, the sub-flow "my-sub-flow" will be executed.

```yaml
tasks:
  - input:
      - 'input.path_parameter.user -> header.user'
      - 'input.body -> body'
    process: 'flow://my-sub-flow'
    output:
      - 'result -> model.pojo'
    description: 'Execute a sub-flow'
    execution: sequential
    next:
      - 'my.next.function'
```

If the sub-flow is not available, the system will throw an error stating that it is not found.

Hierarchy of flows would reduce the complexity of a single flow configuration file. The "time-to-live (TTL)"
value of the parent flow should be set to a value that covers the complete flow including the time used in
the sub-flows. To avoid the additional routing overheads, use this feature only when necessary.

For simplicity, the input data mapping for a sub-flow should contain only the "header" and "body" arguments.

## Task list

All tasks for a flow are defined in the "tasks" section.

### Input/Output data mapping

A function is self-contained. This modularity reduces application complexity because the developer only needs
interface contract details for a specific function.

To handle this level of modularity, the system provides configurable input/output data mapping.

Namespaces for I/O data mapping

| Type                              | Keyword and/or namespace     |
|:----------------------------------|:-----------------------------|
| Flow input dataset                | `input.` or `http.input.`    |
| Flow output dataset               | `output.` or `http.output.`  |
| Function input body               | no namespace required        |
| Function input or output headers  | `header` or `header.`        |
| Function output result set        | `result.`                    |
| Function output status code       | `status`                     |
| State machine dataset             | `model.`                     |
| Decision value from a task        | `decision`                   |

Constants for input data mapping (Left-hand-side argument)

| Type      | Keyword and/or namespace                                     |
|:----------|:-------------------------------------------------------------|
| String    | `text(example_value)`                                        |
| Integer   | `int(number)`                                                |
| Long      | `long(number)`                                               |
| Float     | `float(number)`                                              |
| Double    | `double(number)`                                             |
| Boolean   | `boolean(true or false)`                                     |
| File      | `file(text:file_path)`<br>`file(binary:file_path)`           |
| Classpath | `classpath(text:file_path)`<br>`classpath(binary:file_path)` |

For input data mapping, the "file" constant type is used to load some file content as an argument of a user function.
You can tell the system to render the file as "text" or "binary". Similarly, the "classpath" constant type refers
to static file in the application source code's "resources" folder.

Special content type for output data mapping (Right-hand-side argument)

| Type   | Keyword            |
|:-------|:-------------------|
| File   | `file(file_path)`  |

For output data mapping, the "file" content type is used to save some data from the output of a user function
to a file in the local file system.

For HTTP Flow Adapter, "http.input." and "http.output." namespaces are aliases of "input." and "output." respectively.

The "decision" keyword applies to "right hand side" of output data mapping statement in a decision task only
(See "Decision" in the task section).

Each flow has its end-to-end input and output.

Each function has its input headers, input body and output result set.
Optionally, a function can return an EventEnvelope object to hold its result set in the "body", a "status" code
and one or more header key-values.

Since each function is stateless, a state machine (with namespace `model.`) is available as a temporary memory store
for transaction states that can be passed from one task to another.

All variables are addressable using the standard dot-bracket convention.

For example, "hello.world" will retrieve the value `100` from this data structure:
```json
{
  "hello":  {
    "world": 100
  }
}
```

and "numbers[1]" will retrieve the value `200` below:
```json
{ "numbers":  [100, 200] }
```

The assignment is done using the "->" syntax.

In the following example, the HTTP input query parameter 'amount' is passed as input body argument 'amount'
to the task 'simple.decision'. The result (function "return value") from the task will be mapped to the
special "decision" variable that the flow engine will evaluate. This assumes the result is a boolean or
numeric value.

The "decision" value is also saved to the state machine (`model`) for subsequent tasks to evaluate.

```yaml
  - input:
      - 'input.query.amount -> amount'
    process: 'simple.decision'
    output:
      - 'result -> decision'
      - 'result -> model.decision'
```

### Special handling for header

When function input keyword `header` is specified in the "right hand side" of an input data mapping statement,
it refers to the input event envelope's headers. Therefore, it assumes the "left hand side" to resolve into
a Map object of key-values. Otherwise, it will reject the input data mapping statement with an error like this:

```text
Invalid input mapping 'text(ok) -> header', expect: Map, Actual: String
```

When function input namespace `header.` is used, the system will map the value resolved from the "left hand side"
statement into the specific header.

For example, the input data mapping statement `text(ok) -> header.demo` will set "demo=ok" into input event
envelope's headers.

When function output keyword `header` is specified in the "left hand side" of an output data mapping statement,
it will resolve as a Map from the function output event envelope's headers.

Similarly, when function output namespace `header.` is used, the system will resolve the value from a specific
key of the function output event envelope's headers.

### Function input and output

To support flexible input data mapping, the input to a function must be either `Map<String, Object>` or `PoJo`.
The output (i.e. result set) of a function can be Map, PoJo or Java primitive.

Your function can implement the `TypedLambdaFunction` interface to configure input and output.

Since a data structure is passed to your function's input argument as key-values, you may create a PoJo class
to deserialize the data structure.

To tell the system that your function is expecting input as a PoJo, you can use the special notation "*" in
the right hand side.

For example, the following entry tells the system to set the value in "model.dataset" as a PoJo input.

```yaml
  - input:
      - 'model.dataset -> *'
```

> If the value from the left hand side is not a map, the system will ignore the input mapping command and
print out an error message in the application log.

### Setting function input headers

When function input body is used to hold a PoJo, we may use function input headers to pass other arguments
to the function without changing the data structure of a user defined PoJo.

In the following example, the HTTP query parameter "userid" will be mappped to the function input header
key "user" and the HTTP request body will be mapped to the function input body.

```yaml
  - input:
      - 'input.query.userid -> header.user'
      - 'input.body -> *'
    process: 'my.user.function'
    output:
      - 'text(application/json) -> output.header.content-type'
      - 'result -> output.body'
```

## Task types

### Decision task

A decision task makes decision to select the next task to execute. It has the tag `execution=decision`.

In the output data mapping section, it must map the corresponding result set or its key-value to the `decision` object.

The "next" tag contains a list of tasks to be selected based on the decision value.

If decision value is boolean, a `true` value will select the first task. Otherwise, the second task will be selected.

If decision value is an integer, the number should *start from 1* where the corresponding "next" task
will be selected.

```yaml
tasks:
  - input:
      - 'input.query.decision -> decision'
    process: 'simple.decision'
    output:
      - 'result.data -> model.decision'
      - 'result.data -> decision'
    description: 'Simple decision test'
    execution: decision
    next:
      - 'decision.case.one'
      - 'decision.case.two'
```

### Response task

A response task will provide result set as a flow output or "response". A response task allows the flow to respond
to the user or caller immediately and then move on to the next task asynchronously. For example, telling the user
that it has accepted a request and then moving on to process the request that may take longer time to run.

A response task has the tag `execution=response` and a "next" task.

```yaml
tasks:
  - input:
      - 'input.path_parameter.user -> user'
      - 'input.query.seq -> sequence'
    process: 'sequential.one'
    output:
      - 'result -> model.pojo'
      - 'result -> output.body'
    description: 'Pass a pojo to another task'
    execution: response
    next:
      - 'sequential.two'
```

### End task

An end task indicates that it is the last task of the transaction processing in a flow. If the flow has not executed
a response task, the end task will generate the response. Response is defined by output data mapping.

This task has the tag `execution=end`.

For example, the greeting task in the unit tests is an end task.
```yaml
    - input:
        - 'input.path_parameter.user -> user'
      process: 'greeting.demo'
      output:
        - 'text(application/json) -> output.header.content-type'
        - 'result -> output.body'
      description: 'Hello World'
      execution: end
```

### Sequential task

Upon completion of a sequential task, the next task will be executed.

This task has the tag `execution=sequential`.

In the following example, `sequential.two` will be executed after `sequential.one`.
```yaml
tasks:
  - input:
      - 'input.path_parameter.user -> user'
      - 'input.query.seq -> sequence'
    process: 'sequential.one'
    output:
      - 'result -> model.pojo'
    description: 'Pass a pojo to another task'
    execution: sequential
    next:
      - 'sequential.two'
```

### Parallel task

Upon completion of a `parallel` task, all tasks in the "next" task list will be executed in parallel.

This task has the tag `execution=parallel`.

In this example, `parallel.one` and `parallel.two` will run after `begin.parallel.test`
```yaml
tasks:
  - input:
      - 'int(2) -> count'
    process: 'begin.parallel.test'
    output: []
    description: 'Setup counter for two parallel tasks'
    execution: parallel
    next:
      - 'parallel.one'
      - 'parallel.two'
```

### Fork-n-join task

Fork-n-join is a parallel processing pattern.

A "fork" task will execute multiple "next" tasks in parallel and then consolidate the result sets before running
the "join" task.

This task has the tag `execution=fork`. It must have a list of "next" tasks and a "join" task.

It may look like this:
```yaml
tasks:
  - input:
      - 'input.path_parameter.user -> user'
      - 'input.query.seq -> sequence'
    process: 'sequential.one'
    output:
      - 'result -> model.pojo'
    description: 'Pass a pojo to another task'
    execution: fork
    next:
      - 'echo.one'
      - 'echo.two'
    join: 'join.task'
```

### Sink task

A sink task is a task without any next tasks. Sink tasks are used by fork-n-join and pipeline tasks as reusable modules.

This task has the tag `execution=sink`.
```yaml
  - input:
      - 'text(hello-world-two) -> key2'
    process: 'echo.two'
    output:
      - 'result.key2 -> model.key2'
    description: 'Hello world'
    execution: sink
```

# Pipeline Feature

Pipeline is an advanced feature of Event Script.

## Pipeline task

A pipeline is a list of tasks that will be executed orderly within the current task.

When the pipeline is done, the system will execute the "next" task.

This task has the tag `execution=pipeline`.
```yaml
tasks:
  - input:
      - 'input.path_parameter.user -> user'
      - 'input.query.seq -> sequence'
    process: 'sequential.one'
    output:
      - 'result -> model.pojo'
    description: 'Pass a pojo to another task'
    execution: pipeline
    pipeline:
      - 'echo.one'
      - 'echo.two'
    next:
      - 'echo.three'
```

### Advanced pipeline features

Some special uses of pipelines include "if-then-else" and "for/while loops". This is like running a mini-flow
inside a pipeline.

### Simple for-loop

In the following example, the `loop.statement` contains a for-loop that uses a variable in the state machine to
evaluate the loop.

In this example, the pipeline will be executed three times before passing control to the "next" task.
```yaml
tasks:
  - input:
      - 'input.path_parameter.user -> user'
      - 'input.query.seq -> sequence'
    process: 'sequential.one'
    output:
      - 'result -> model.pojo'
    description: 'Pass a pojo to another task'
    execution: pipeline
    loop:
      statement: 'for (model.n = 0; model.n < 3; model.n++)'
    pipeline:
      - 'echo.one'
      - 'echo.two'
      - 'echo.three'
    next:
      - 'echo.four'
```

### Simple while loop

The `loop.statement` may use a "while loop" syntax like this:

```yaml
    loop:
      statement: 'while (model.running)'
```

To exit the above while loop, one of the functions in the pipeline should return a boolean "false" value with
output "data mapping" to the `model.running` variable.

### For loop with break/continue decision

In the following example, the system will evaluate if the `model.quit` variable exists.
If yes, the `break` or `continue` condition will be executed.

The state variable is obtained after output data mapping and any task in the pipeline can set a key-value for
mapping into the state variable.

```yaml
tasks:
  - input:
      - 'input.path_parameter.user -> user'
      - 'input.query.seq -> sequence'
    process: 'sequential.one'
    output:
      - 'result -> model.pojo'
    description: 'Pass a pojo to another task'
    execution: pipeline
    loop:
      statement: 'for (model.n = 0; model.n < 3; model.n++)'
      condition:
        mode: sequential
        statements:
          - 'if (model.quit) break'
    pipeline:
      - 'echo.one'
      - 'echo.two'
      - 'echo.three'
    next:
      - 'echo.four'
```

### For loop with if-then-else statement

The condition statement may use a "if-then-else" statement. Instead of break or continue, the statement will evaluate
if another task will be executed.

The `condition.mode` may be sequential or parallel. When it is set to sequential, the selected task will be executed,
and the pipeline will resume.

When it is set to parallel, the pipeline will resume immediately without waiting for the selected task to finish.
The selected task will therefore run in parallel by itself. This allows a pipeline to spin up another parallel
task for some asynchronous processing. e.g. sending a notification message to the user in the middle of
a transaction.

```yaml
tasks:
  - input:
      - 'input.path_parameter.user -> user'
      - 'input.query.seq -> sequence'
    process: 'sequential.one'
    output:
      - 'result -> model.pojo'
    description: 'Pass a pojo to another task'
    execution: pipeline
    loop:
      statement: 'for (model.n = 0; model.n < 4; model.n++)'
      condition:
        mode: parallel
        statements:
          - 'if (model.jump) echo.ext1 else echo.ext2'
    pipeline:
      - 'echo.one'
      - 'echo.two'
      - 'echo.three'
    next:
      - 'echo.four'
```

# Handling Exception

You can define exception handler at the top level or at the task level.

## What is an exception?

Exception is said to occur when a user function throws exception or returns an EventEnvelope object with
a status code equals to or larger than 400.

The event status uses the same numbering scheme as HTTP exception status code.
Therefore, status code less than 400 is not considered an exception.

## Top-level exception handler

Top-level exception handler is a "catch-all" handler. You can define it like this:

```yaml
flow:
  id: 'greetings'
  description: 'Simplest flow of one task'
  ttl: 10s
  exception: 'v1.my.exception.handler'
```

In this example, the `v1.my.exception.handler` should point to a corresponding exception handler that you provide.

The following input arguments will be delivered to your function when exception happens.

| Key     | Description                  |
|:--------|:-----------------------------|
| status  | Exception status code        |
| message | Error message                |
| stack   | Stack trace in a text string |

The exception handler function can be an "end" task to abort the transaction or a decision task
to take care of the exception. For example, the exception handler can be a "circuit-breaker" to retry a request.

## Task-level exception handler

You can attach an exception handler to a task. One typical use is the "circuit breaker" pattern.
In the following example, the user function "breakable.function" may throw an exception for some error condition.
The exception will be caught by the "v1.circuit.breaker" function.

```yaml
  - input:
      - 'input.path_parameter.accept -> accept'
      - 'model.attempt -> attempt'
    process: 'breakable.function'
    output:
      - 'int(0) -> model.attempt'
      - 'text(application/json) -> output.header.content-type'
      - 'result -> output.body'
    description: 'This demo function will break until the "accept" number is reached'
    execution: end
    exception: 'v1.circuit.breaker'
```

The configuration for the circuit breaker function may look like this:

```yaml
  - input:
      - 'model.attempt -> attempt'
      - 'int(2) -> max_attempts'
      - 'error.code -> status'
      - 'error.message -> message'
      - 'error.stack -> stack'
    process: 'v1.circuit.breaker'
    output:
      - 'result.attempt -> model.attempt'
      - 'result.decision -> decision'
      - 'result.status -> model.status'
      - 'result.message -> model.message'
    description: 'Just a demo circuit breaker'
    execution: decision
    next:
      - 'breakable.function'
      - 'abort.request'
```

An exception handler will be provided with the "error" object that contains error code, error message and an exception
stack trace. The exception handler can inspect the error object to make decision of the next step.

For circuit breaker, we can keep the number of retry attempts in the state machine under "model.attempt" or any
key name that you prefer. In the above example, it sets an integer constant of 2 for the maximum attempts.

The circuit breaker can then evaluate if the number of attempts is less than the maximum attempts. If yes, it will
return a decision of "true" value to tell the system to route to the "breakable.function" again. Otherwise, it will
return a decision of "false" value to abort the request.

A more sophisticated circuit breaker may be configured with "alternative execution paths" depending on the error
status and stack trace. In this case, the decision value can be a number from 1 to n that corresponds to the "next"
task list.

Exception handlers may be used in both queries and transactions. For a complex transaction, the exception handler
may implement some data rollback logic or recovery mechanism.

## IMPORTANT

When a task-level exception handler throws exception, it will be caught by the top-level exception handler, if any.

A top-level exception handler should not throw exception. Otherwise it may go into an exception loop.

Therefore, we recommend that an exception handler should return regular result set in a PoJo or a Map object.

An example of task-level exception handler is shown in the "HelloException.class" in the unit test section of
the event script engine where it set the status code in the result set so that the system can map the status code
from the result set to the next task or to the HTTP output status code.

# Other Features

## Future task scheduling

You may add a “delay” tag in a task so that it will be executed later.
This feature is usually used for unit tests or "future task scheduling".

Since the system is event-driven and non-blocking, the delay is simulated by event scheduling.
It does not block the processing flow.

| Type           | Value                  | Example           |
|:---------------|:-----------------------|:------------------|
| Fixed delay    | Milliseconds           | delay=1000        |
| Variable delay | State machine variable | delay=model.delay |

When delay is set to a state variable that its value is not configured by a prior data mapping,
the delay command will be ignored.

An example task that has an artificial delay of 2 seconds:
```yaml
tasks:
  - input:
      - 'input.path_parameter.user -> user'
      - 'input.query.ex -> exception'
      - 'text(hello world) -> greeting'
    process: 'greeting.test'
    output:
      - 'text(application/json) -> output.header.content-type'
      - 'result -> output.body'
    description: 'Hello World'
    execution: end
    delay: 2000
```
<br/>

|            Chapter-3            |                   Home                    |               Chapter-5                |
|:-------------------------------:|:-----------------------------------------:|:--------------------------------------:|
| [REST Automation](CHAPTER-3.md) | [Table of Contents](TABLE-OF-CONTENTS.md) | [Build, Test and Deploy](CHAPTER-5.md) |
