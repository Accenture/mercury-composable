# Event Script Syntax

Event Script uses YAML to represent an end-to-end transaction flow. A transaction is a business use case, and
the flow can be an API service, a batch job or a real-time transaction.

## Flow list

This configuration file sits in the project "resources" project and contains a list of filenames.

The default flow list is "flows.yaml" under the "resources" folder. It may look like this.

```yaml
flows:
  - 'get-profile.yml'
  - 'create-profile.yml'
  - 'delete-profile.yml'

location: 'classpath:/flows/'
```

The "location" tag is optional. If present, you can tell the system to load the flow config files from
another folder location.

## Multiple flow lists

You can provide more than one flow list to your application and it can become very handy under different
situations. For instance, to achieve better modularity in complex application, flows can be grouped to
multiple categories based on development team's choice and these flows can be managed in multiple flow
lists. Another great place to use multiple flow list is to include external libraries which contain
pre-defined flow lists. The following example demonstrates that an application loads a list of flows
defined in "flows.yaml" and additional flows defined in "more-flows.yaml" file of a composable library.

```properties
yaml.flow.automation=classpath:/flows.yaml, classpath:/more-flows.yaml
```

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

first.task: 'greeting.demo'

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

```properties
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

> "my.organization" package name is an example. Please replace it with your organization package path.

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
web.component.scan=my.organization
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

## Unique task naming

Composable functions are designed to be reusable. By changing some input data mapping to feed different parameters and
payload, your function can behave differently.

Therefore, it is quite common to use the same function ("process") more than once in a single event flow.

When a task is not named, the "process" tag is used to name the task.

Since each task must have a unique name for event routing, we cannot use the same "process" name more than once in an
event flow. To handle this use case, you can create unique names for the same function (i.e. "process") like this:

```yaml
flow:
  id: 'greetings'
  description: 'Simplest flow'
  ttl: 10s

first.task: 'my.first.task'

tasks:
  - name: 'my.first.task'
    input:
      - 'input.path_parameter.user -> user'
    process: 'greeting.demo'
    output:
      - 'text(application/json) -> output.header.content-type'
      - 'result -> output.body'
    description: 'Hello World'
    execution: sequential
    next:
      - 'another.task'
```

The above event flow configuration uses "my.first.task" as a named route for "greeting.demo" by adding the
"name" tag to the composable function.

For configuration simplicity, the "name" tag is optional. If not provided, the process name is assumed to be
the unique "task" name.

> Important: The Event Manager performs event choreography using the unique task name.
  Therefore, when the "process" name for the function is not unique, you must create unique task "names"
  for the same function to ensure correct routing.

## Assigning multiple route names to a single function

The built-in distributed tracing system tracks the actual composable functions using the "process" name
and not the task names.

When there is a need to track the task names in distributed trace, you can tell the system to create
additional instances of the same function with different route names.

You can use a comma separated list as the route name like this:

```java
@PreLoad(route="greeting.case.1, greeting.case.2", instances=10)
public class Greetings implements TypedLambdaFunction<Map<String, Object>, Map<String, Object>> {
    
  @Override
  public Map<String, Object> handleEvent(Map<String, String> headers, Map<String, Object> input, int instance) {
      // business logic here
      return someResult;
  }
}
```

> Note: The "unique task naming" method is more memory efficient than creating additional route names

## Preload overrides

Once a composable function is published as a reusable library in the artifactory, its route name and
number of instances are fixed using the "PreLoad" annotation in the function class.

Without refactoring your libary, you can override its route name and instances using a preload override
file like this:

```yaml
preload:
  - original: 'greeting.demo'
    routes:
      - 'greeting.case.1'
      - 'greeting.case.2'
    # the "instances" tag is optional
    instances: 20
  - original: 'v1.another.reusable.function'
    keep-original: true
    routes:
      - 'v1.reusable.1'
      - 'v1.reusable.2'
```

In the above example, the function associated with "greeting.demo" will be preloaded as "greeting.case.1"
and "greeting.case.2". The number of maximum concurrent instances is also changed from 10 to 20.

In the second example, "v1.another.reusable.function" is updated as "v1.reusable.1" and "v1.reusable.2"
and the number of concurrent instances is not changed. The original route "v1.another.reusable.function" is
preserved when the "keep-original" parameter is set to true.

Assuming the above file is "preload-override.yaml" in the "resources" folder of the application source code
project, you should add the following parameter in application.properties to activate this preload override
feature.

```properties
yaml.preload.override=classpath:/preload-override.yaml
```

## Multiple preload override config files

When you publish a composable function as a library, you may want to ensure the route names of the functions are
merged properly. In this case, you can bundle a library specific preload override config file.

For example, your library contains a "preload-kafka.yaml" to override some route names, you can add it to the
yaml.preload.override parameter like this:

```properties
yaml.preload.override=classpath:/preload-override.yaml, classpath:/preload-kafka.yaml
```

The system will then merge the two preload override config files.

The concurrency value of a function is overwritten using the "instances" parameter in the first preload override file.
Subsequent override of the "instances" parameter is ignored. i.e. the first preload override file will take precedence.

## Hierarchy of flows

Inside a flow, you can run one or more sub-flows.

To do this, you can use the flow protocol identifier (`flow://`) to indicate that the task is a flow.

For example, when running the following task, "flow://my-sub-flow" will be executed like a regular task.

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
the sub-flows.

For simplicity, the input data mapping for a sub-flow should contain only the "header" and "body" arguments.

## Tasks and data mapping

All tasks for a flow are defined in the "tasks" section.

### Input/Output data mapping

A function is self-contained. This modularity reduces application complexity because the developer only needs
interface contract details for a specific function.

To handle this level of modularity, the system provides configurable input/output data mapping.

*Namespaces for I/O data mapping*

| Type                              | Keyword and/or namespace     | LHS / RHS  | Mappings |
|:----------------------------------|:-----------------------------|------------|----------|
| Flow input dataset                | `input.`                     | left       | input    |
| Flow output dataset               | `output.`                    | right      | output   |
| Function input body               | no namespace required        | right      | input    |
| Function input or output headers  | `header` or `header.`        | right      | I/O      |
| Function output result set        | `result.`                    | left       | output   |
| Function output status code       | `status`                     | left       | output   |
| Decision value                    | `decision`                   | right      | output   |
| State machine dataset             | `model.`                     | left/right | I/O      |
| External state machine key-value  | `ext:`                       | right      | I/O      |

Note that external state machine namespace uses ":" to indicate that the key-value is external.

*Constants for input data mapping*

| Type      | Keyword for the left-hand-side argument                      |
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

*Special content type for output data mapping*

| Type   | Keyword for the right-hand-side argument |
|:-------|:-----------------------------------------|
| File   | `file(file_path)`                        |

For output data mapping, the "file" content type is used to save some data from the output of a user function
to a file in the local file system.

The "decision" keyword applies to "right hand side" of output data mapping statement in a decision task only
(See "Decision" in the task section).

*Each flow has its end-to-end input and output.*

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

The assignment is done using the `->` syntax.

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

### Metadata for each flow instance

For each flow instance, the state machine in the "model" namespace provides the following metadata that
you can use in the input/output data mapping. For example, you can set this for an exceptional handler to
log additional information.

| Type             | Keyword          | Comment                                    |
|:-----------------|:-----------------|:-------------------------------------------|
| Flow ID          | `model.flow`     | The ID of the event flow config            |
| Trace ID         | `model.trace`    | Optional traceId when tracing is turned on |
| Correlation ID   | `model.cid`      | Correlation ID of the inbound request      |

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

To tell the system that your function is expecting input as a PoJo, you can use the special notation `*` in
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
      - 'result -> model.decision'
      - 'result -> decision'
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

## Pipeline feature

Pipeline is an advanced feature of Event Script.

### Pipeline task

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

Some special uses of pipelines include "for/while-loop" and "continue/break" features.

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

In the following example, the system will evaluate if the `model.quit` variable is true.
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
        - 'if (model.quit) break'
    pipeline:
      - 'echo.one'
      - 'echo.two'
      - 'echo.three'
    next:
      - 'echo.four'
```

## Handling exception

You can define exception handler at the top level or at the task level.

Exception is said to occur when a user function throws exception or returns an EventEnvelope object with
a status code equals to or larger than 400.

The event status uses the same numbering scheme as HTTP exception status code.
Therefore, status code less than 400 is not considered an exception.

### Top-level exception handler

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

### Task-level exception handler

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

### Best practice

When a task-level exception handler throws exception, it will be caught by the top-level exception handler, if any.

A top-level exception handler should not throw exception. Otherwise it may go into an exception loop.

Therefore, we recommend that an exception handler should return regular result set in a PoJo or a Map object.

An example of task-level exception handler is shown in the "HelloException.class" in the unit test section of
the event script engine where it set the status code in the result set so that the system can map the status code
from the result set to the next task or to the HTTP output status code.

## Advanced features

### Simple type matching and conversion

Event script's state machine supports simple type matching and conversion. This "impedance matching" feature
allows us to accommodate minor interface contract changes without refactoring business logic of a user function.

This is supported in both the left-hand-side and right-hand-side of both input and output data mappings.

For the left-hand-side, the state machine's model value is matched or converted to the target data type before
setting the value of the right-hand-side. The state machine values are unchanged.

For the right-hand-side, the matched or converted value is applied to the state machine's model value.

The syntax is `model.somekey:type` where "type" is one of the following:

| Type                  | Match value as               | Example                               |
|:----------------------|:-----------------------------|:--------------------------------------|
| text                  | text string                  | model.someKey:text                    |
| binary                | byte array                   | model.someKey:binary                  |
| int                   | integer or -1 if not numeric | model.someKey:int                     |
| long                  | long or -1 if not numeric    | model.someKey:long                    |
| float                 | float or -1 if not numeric   | model.someKey:float                   |
| double                | double or -1 if not numeric  | model.someKey:double                  |
| boolean               | true or false                | model.someKey:boolean                 |
| boolean(value)        | true if value matches        | model.someKey:boolean(positive)       |
| boolean(value=true)   | true if value matches        | model.someKey:boolean(positive=true)  |
| boolean(value=false)  | false if value matches       | model.someKey:boolean(negative=false) |
| and(model.key)        | boolean AND of 2 model keys  | model.someKey:and(model.another)      |
| or(model.key)         | boolean OR of 2 model keys   | model.someKey:or(model.another)       |
| substring(start, end) | extract a substring          | model.someKey:substring(0, 5)         |
| substring(start)      | extract a substring          | model.someKey:substring(5)            |
| b64                   | byte-array to Base64 text    | model.someKey:b64                     |
| b64                   | Base64 text to byte-array    | model.someKey:b64                     |

For boolean with value matching, the value can be null. This allows your function to test if the
key-value in the left-hand-side is a null value.

For Base64 type matching, if the key-value is a text string, the system will assume it is a
Base64 text string and convert it to a byte-array. If the key-value is a byte-array, the system
will encode it into a Base64 text string.

An interesting use case of type matching is a simple decision task using the built-in no-op function.
For example, when a control file for the application is not available, your application will switch
to run in dev mode.

A sample task may look like this:

```yaml
first.task: 'no.op'

tasks:
- input:
    - 'file(binary:/tmp/interesting-config-file) -> model.is-local:boolean(null=true)'
  process: 'no.op'
  output:
    - 'model.is-local -> decision'
  execution: decision
  next:
    - 'start.in.dev.mode'
    - 'start.in.cloud'
```

### External state machine

The in-memory state machine is created for each query or transaction flow and it is temporal.

For complex transactions or long running work flows, you would typically want to externalize some transaction
states to a persistent store such as a distributed cache system or a high performance key-value data store.

In these use cases, you can implement an external state machine function and configure it in a flow.

Below is an example from a unit test. When you externalize a key-value to an external state machine,
you must configure the route name (aka level-3 functional topic) of the external state machine.

Note that when passing a `null` value to a key of an external state machine means "removal".

```yaml
external.state.machine: 'v1.ext.state.machine'

tasks:
  - input:
      # A function can call an external state machine using input or output mapping.
      # In this example, it calls external state machine from input data mapping.
      - 'input.path_parameter.user -> ext:/${app.id}/user'
      - 'input.body -> model.body'
      # demonstrate saving constant to state machine and remove it using model.none
      - 'text(world) -> ext:hello'
      - 'model.none -> ext:hello'
    process: 'no.op'
    output:
      - 'text(application/json) -> output.header.content-type'
      # It calls external state machine again from output data mapping
      - 'input.body -> ext:/${app.id}/body'
      - 'input.body -> output.body'
      - 'text(message) -> ext:test'
      - 'model.none -> ext:test'
    description: 'Hello World'
    execution: end
```

The "external.state.machine" parameter is optional.

When present, the system will send a key-value from the current flow instance's state machine
to the function implementing the external state machine. The system uses the "ext:" namespace
to externalize a state machine's key-value.

Note that the delivery of key-values to the external state machine is asynchronous.
Therefore, please assume eventual consistency.

You should implement a user function as the external state machine.

The input interface contract to the external state machine for saving a key-value is:

```
header.type = 'put'
header.key = key
body = value
```

Your function should save the input key-value to a persistent store.

In another flow that requires the key-value, you can add an initial task
to retrieve from the persistent store and do "output data mapping" to
save to the in-memory state machine so that your transaction flow can
use the persisted key-values to continue processing.

In the unit tests of the event-script-engine subproject, these two flows work together:

```
externalize-put-key-value
externalize-get-key-value
```

IMPORTANT: Events to an external state machine are delivered asynchronously. If you want to guarantee
message sequencing, please do not set the "instances" parameter in the PreLoad annotation.

To illustrate a minimalist implementation, below is an example of an external state machine in the
event-script-engine's unit test section.

```java
@PreLoad(route = "v1.ext.state.machine")
public class ExternalStateMachine implements LambdaFunction {
    private static final Logger log = LoggerFactory.getLogger(ExternalStateMachine.class);

    private static final ManagedCache store = ManagedCache.createCache("state.machine", 5000);
    private static final String TYPE = "type";
    private static final String PUT = "put";
    private static final String GET = "get";
    private static final String REMOVE = "remove";
    private static final String KEY = "key";

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        if (!headers.containsKey(KEY)) {
            throw new IllegalArgumentException("Missing key in headers");
        }
        String type = headers.get(TYPE);
        String key = headers.get(KEY);
        if (PUT.equals(type) && input != null) {
            log.info("Saving {} to store", key);
            store.put(key, input);
            return true;
        }
        if (GET.equals(type)) {
            Object v = store.get(key);
            if (v != null) {
                log.info("Retrieve {} from store", key);
                return v;
            } else {
                return null;
            }
        }
        if (REMOVE.equals(type)) {
            if (store.exists(key)) {
                store.remove(key);
                log.info("Removed {} from store", key);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
```

### Future task scheduling

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

|             Chapter-3             |                    Home                     |                Chapter-5                 |
|:---------------------------------:|:-------------------------------------------:|:----------------------------------------:|
|  [REST Automation](CHAPTER-3.md)  |  [Table of Contents](TABLE-OF-CONTENTS.md)  |  [Build, Test and Deploy](CHAPTER-5.md)  |
