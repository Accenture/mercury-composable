# Flow Configuration Schema Reference

*Reference: Complete field-level reference for Mercury Composable Event Script flow configuration YAML files.*

> **Note**: Due to the dynamic nature of data mapping expressions, a formal JSON Schema is
> not provided. Validation of flow configurations is performed at compile time by the Mercury
> flow engine.

This page documents every field, namespace, and syntax element available in flow
configuration files. For a tutorial-style introduction with worked examples, see
[Event Script Syntax](CHAPTER-4.md).

---

## Flow list file (`flows.yaml`)

The flow list file is an index that tells the engine which flow configuration files to load.
It is identified by the `yaml.flow.automation` application property.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `flows` | list of strings | Yes | File names of individual flow configuration YAML files to load. |
| `location` | string | No | Base directory for resolving file names. Default: resources folder root. Example: `classpath:/flows/`. |

Multiple flow list files can be specified as a comma-separated list:

```yaml
# application.properties
yaml.flow.automation=classpath:/flows.yaml, classpath:/more-flows.yaml
```

Example `flows.yaml`:

```yaml
location: 'classpath:/flows/'
flows:
  - 'get-profile.yml'
  - 'create-profile.yml'
  - 'delete-profile.yml'
```

---

## Flow-level fields

These fields appear at the root of every individual flow configuration file.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `flow.id` | string | **Yes** | Unique identifier for this flow. Referenced by REST config (`flow` field in `rest.yaml`) and subflow calls (`flow://flow-id`). Must be unique across all loaded flows. |
| `flow.description` | string | **Yes** | Human-readable description of the flow's purpose. Validated at compile time. |
| `flow.ttl` | duration | **Yes** | Maximum wall-clock time for the flow to complete. Minimum 1 second. Accepted units: `s` (seconds), `m` (minutes), `h` (hours). Example: `30s`. |
| `flow.exception` | string | No | Route name of the global exception handler function for this flow. Receives the error dataset when any task throws an unhandled exception. |
| `first.task` | string | **Yes** | Route name (or task `name`) of the first task to execute when the flow starts. |
| `external.state.machine` | string | Conditional | Route name (or `flow://flow-id`) of an external state machine service. **Required** when any task uses the `ext:` output namespace. |
| `tasks` | list | **Yes** | Ordered list of task definitions. See [Task-level fields](#task-level-fields). |

Minimal valid flow:

```yaml
flow:
  id: 'my-flow'
  description: 'Example flow'
  ttl: 30s
  exception: 'v1.exception.handler'

first.task: 'step.one'

tasks:
  - input:
      - 'input.body -> *'
    process: 'my.service'
    description: 'Call my service'
    output:
      - 'result -> output.body'
    execution: end
```

---

## Task-level fields

Each entry in the `tasks` list defines a step in the flow.

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `name` | string | Conditional | value of `process` | Unique task identifier within this flow. **Required** when the same `process` route is used more than once; otherwise the `process` value is used as the name. |
| `description` | string | **Yes** | — | Human-readable description of what this task does. Validated at compile time. |
| `input` | list of strings | **Yes** | — | Input data mapping rules. Use `[]` for no input. See [Data mapping syntax](#data-mapping-syntax). |
| `output` | list of strings | **Yes** | — | Output data mapping rules. Use `[]` for no output. See [Data mapping syntax](#data-mapping-syntax). |
| `process` | string | Conditional | — | Route name of the composable function to call, or `flow://flow-id` to invoke a subflow. Either `name` or `process` must be present. |
| `execution` | string | **Yes** | — | Task execution type. See [Execution types](#execution-types). |
| `next` | list of strings | Conditional | — | Route name(s) of subsequent task(s). Required for `sequential`, `parallel`, `fork`, `pipeline`. Not used for `end` or `sink`. Decision tasks: first entry = `false`/`1`, second = `true`/`2`, etc. |
| `exception` | string | No | _(flow exception)_ | Route name of a task-level exception handler. Overrides `flow.exception` for this task only. |
| `delay` | string or int | No | — | Delay before this task executes. Integer = milliseconds; string = model variable path (e.g. `model.wait_ms`). Must be less than `flow.ttl`. |
| `pipeline` | list of strings | Conditional | — | Ordered list of task route names. **Required** when `execution` is `pipeline`. |
| `loop` | object | No | — | Loop configuration for pipeline tasks. See [Pipeline configuration](#pipeline-configuration). |
| `join` | string | Conditional | — | Name of the task that collects fork results. **Required** when `execution` is `fork`. |
| `source` | string | No | — | Model variable path (e.g. `model.items`) containing a list to iterate over in a dynamic fork. The fork creates one branch per list item. |

> `name`, `process`, and `description` are validated: task name must not be empty, and
> `description` must not be blank.

---

## Execution types

### `sequential`

Executes the function and then passes control to exactly one next task.

| Required | Optional | Forbidden |
|----------|----------|-----------|
| `next` (exactly 1 entry) | `delay`, `exception` | — |

```yaml
- name: 'step.one'
  input:
    - 'input.body -> *'
  process: 'v1.my.service'
  description: 'Process the request body'
  output:
    - 'result -> model.data'
  execution: sequential
  next:
    - 'step.two'
```

---

### `parallel`

Fans out to multiple next tasks concurrently. Each task receives the same model state. There
is no join point — all branches run independently to their own `end` or `sink`.

| Required | Optional | Forbidden |
|----------|----------|-----------|
| `next` (2+ entries) | `delay`, `exception` | `join`, `pipeline`, `loop` |

```yaml
- input:
    - 'model.data -> *'
  process: 'v1.router'
  description: 'Fan out to parallel handlers'
  output: []
  execution: parallel
  next:
    - 'handler.a'
    - 'handler.b'
    - 'handler.c'
```

---

### `fork`

Fork-and-join pattern. All `next` tasks run concurrently; execution resumes at the `join`
task once all branches complete. Also supports iterating over a list via the `source` field.

| Required | Optional | Forbidden |
|----------|----------|-----------|
| `next` (1+ entries), `join` | `source`, `delay`, `exception` | `pipeline`, `loop` |

```yaml
- input: []
  process: 'no.op'
  description: 'Fork into parallel branches'
  output: []
  execution: fork
  next:
    - 'branch.a'
    - 'branch.b'
  join: 'collect.results'
```

**Dynamic fork** — iterates over a model list (`source`). Must have exactly one `next` task.
Inside that task, `model.<source>.ITEM` holds the current item and `model.<source>.INDEX`
holds the zero-based index:

```yaml
- input: []
  process: 'no.op'
  description: 'Fork over list items'
  output: []
  execution: fork
  source: 'model.items'
  next:
    - 'process.item'
  join: 'collect.results'

- name: 'process.item'
  input:
    - 'model.items.ITEM -> item'
    - 'model.items.INDEX -> idx'
  process: 'v1.item.processor'
  description: 'Process one list item'
  output: []
  execution: sink
```

---

### `decision`

Branches to one of the `next` tasks based on a value mapped to `decision` in the output.

- **Boolean decision**: `false` routes to `next[0]`, `true` routes to `next[1]`.
- **Numeric decision**: integer `1` routes to `next[0]`, `2` to `next[1]`, etc.

| Required | Optional | Forbidden |
|----------|----------|-----------|
| `next` (2+ entries) | `delay`, `exception` | `join`, `pipeline`, `loop`, `source` |

```yaml
- name: 'check.status'
  input:
    - 'model.status_code -> status'
  process: 'v1.status.checker'
  description: 'Decide next step based on status'
  output:
    - 'result -> decision'
  execution: decision
  next:
    - 'handle.failure'
    - 'handle.success'
```

The decision value `false` or `1` routes to `handle.failure`; `true` or `2` routes to
`handle.success`.

---

### `response`

Sends an HTTP response to the caller immediately, then continues executing subsequent tasks
asynchronously. Useful for long-running background processing after acknowledging a request.

| Required | Optional | Forbidden |
|----------|----------|-----------|
| `next` (exactly 1 entry) | `delay`, `exception` | `join`, `pipeline`, `loop`, `source` |

```yaml
- input:
    - 'input.body -> *'
  process: 'v1.create.record'
  description: 'Create record and respond immediately'
  output:
    - 'result -> output.body'
    - 'int(201) -> output.status'
  execution: response
  next:
    - 'v1.send.notification'
```

---

### `end`

Terminates the flow. Any output mappings set the final HTTP response.

| Required | Optional | Forbidden |
|----------|----------|-----------|
| — | `delay`, `exception` | `next`, `join`, `pipeline`, `loop`, `source` |

```yaml
- input:
    - 'model.profile -> *'
  process: 'v1.get.profile'
  description: 'Fetch and return the profile'
  output:
    - 'text(application/json) -> output.header.content-type'
    - 'result -> output.body'
  execution: end
```

---

### `sink`

A terminal task with no outbound connection. Used as a branch endpoint in `parallel` or
`fork` patterns. No response is sent.

| Required | Optional | Forbidden |
|----------|----------|-----------|
| — | `delay`, `exception` | `next`, `join`, `pipeline`, `loop`, `source` |

```yaml
- name: 'persist.audit'
  input:
    - 'model.event -> payload'
  process: 'v1.audit.writer'
  description: 'Persist audit record (fire and forget)'
  output: []
  execution: sink
```

---

### `pipeline`

Executes the tasks listed in `pipeline` sequentially within this task's context, then passes
control to the single `next` task. Supports optional `for` and `while` loop control. See
[Pipeline configuration](#pipeline-configuration) for loop syntax.

| Required | Optional | Forbidden |
|----------|----------|-----------|
| `next` (exactly 1 entry), `pipeline` | `loop`, `delay`, `exception` | `join`, `source` |

```yaml
- input:
    - 'int(0) -> model.n'
    - 'int(5) -> model.limit'
  process: 'no.op'
  description: 'Run pipeline loop'
  output: []
  execution: pipeline
  loop:
    statement: 'for (model.n = 0; model.n < model.limit; model.n++)'
  pipeline:
    - 'step.a'
    - 'step.b'
  next:
    - 'step.final'
```

---

## Data mapping syntax

Every `input` and `output` entry is a mapping rule string in the form:

```
'source -> destination'
```

The `->` operator assigns the value of `source` to `destination`. Source and destination
must be different.

### Whole-object mapping (`*`)

Use `*` as the destination to pass the entire source object as the function's input body:

```yaml
input:
  - 'input.body -> *'
  - 'text(json) -> header.type'  # additional input headers; must come BEFORE *
```

> `'* -> *'` passes the entire model as input. `'* -> *'` in output copies function result
> directly to the output body.

### Three-part mapping

A three-part rule decomposes into two rules at compile time:

```
'LHS -> model.var -> RHS'
```
is equivalent to:
```
'LHS -> model.var'
'model.var -> RHS'
```

Useful for applying a type conversion and storing the intermediate value:

```yaml
- 'boolean(true) -> !model.bool -> negate_value'
# compiled as:
# - 'boolean(true) -> model.bool:!'
# - 'model.bool -> negate_value'
```

### Type conversion suffixes

Append `:qualifier` to any source reference to convert the value before mapping:

| Suffix | Converts to | Example |
|--------|-------------|---------|
| `:text` | String | `model.count:text -> label` |
| `:int` | int | `model.str:int -> count` |
| `:long` | long | `model.str:long -> ts` |
| `:float` | float | `model.str:float -> price` |
| `:double` | double | `model.str:double -> ratio` |
| `:boolean` | boolean | `model.str:boolean -> flag` |
| `:binary` | byte[] | `model.text:binary -> bytes` |
| `:b64` | Base64 string | `model.bytes:b64 -> encoded` |
| `:!` | negated boolean | `model.flag:! -> inverted` |
| `:uuid` | UUID string | `model.none:uuid -> id` |
| `:length` | int (size) | `model.list:length -> count` |
| `:substring(start)` | String | `model.text:substring(6) -> tail` |
| `:substring(start, end)` | String | `model.text:substring(0, 5) -> head` |
| `:concat(args...)` | String | `model.a:concat(text( ), model.b) -> full` |
| `:and(model.key)` | boolean | `model.p:and(model.q) -> both` |
| `:or(model.key)` | boolean | `model.p:or(model.q) -> either` |
| `:boolean(value=true)` | boolean | `model.str:boolean(yes=true) -> flag` |

> `model.none` is the built-in null constant. Any mapping from `model.none` clears the destination.

---

### Input namespaces (LHS of `input` mappings)

| Namespace | Description | Example |
|-----------|-------------|---------|
| `input` | Entire HTTP input dataset as a map | `input -> *` |
| `input.body` | Parsed request body (JSON → Map, text → String) | `input.body -> *` |
| `input.body.<field>` | Specific field from the request body | `input.body.user_id -> id` |
| `input.header` | All request headers (lowercase keys) | `input.header -> headers` |
| `input.header.<name>` | Specific request header | `input.header.authorization -> token` |
| `input.query` | All query parameters | `input.query -> params` |
| `input.query.<name>` | Specific query parameter | `input.query.page -> page` |
| `input.path_parameter.<name>` | URL path parameter | `input.path_parameter.id -> id` |
| `input.cookie.<name>` | HTTP cookie value | `input.cookie.session -> session` |
| `input.method` | HTTP method string | `input.method -> method` |
| `input.ip` | Remote IP address | `input.ip -> client_ip` |
| `input.stream` | Input stream route ID (file uploads) | `input.stream -> stream_id` |
| `input.filename` | Filename (multipart uploads) | `input.filename -> name` |
| `input.session.<key>` | Authenticated session value | `input.session.user_id -> uid` |
| `input.uri` | Request URI path | `input.uri -> uri` |
| `model.<key>` | State machine variable for this flow | `model.profile -> *` |
| `model.<key>.<field>` | Nested field in a state variable | `model.user.name -> name` |
| `model.<key>[n]` | Indexed element of a list | `model.items[0] -> first` |
| `model.<key>[model.n]` | Dynamically-indexed element | `model.items[model.n] -> item` |
| `model.parent.<key>` | Parent flow's state machine (in subflows) | `model.parent.token -> token` |
| `model.root.<key>` | Alias for `model.parent.<key>` | `model.root.user -> user` |
| `model.none` | Null constant (clears the destination) | `model.none -> model.old_key` |
| `model.trace` | Current distributed trace ID | `model.trace -> trace_id` |
| `model.flow` | Current flow instance ID | `model.flow -> flow_id` |
| `model.instance` | Alternate alias for flow instance ID | `model.instance -> instance` |
| `model.{model.pointer}` | Dynamic model key (resolved at runtime) | `model.{model.pointer} -> value` |
| `error.task` | Route name of the task that threw (exception handlers) | `error.task -> failed_task` |
| `error.status` | HTTP status code of the error | `error.status -> status` |
| `error.message` | Error message string | `error.message -> message` |
| `error.stack` | Stack trace (if available) | `error.stack -> stack` |
| `$.path` | JSONPath expression | `$.input.body.list[*].id -> ids` |

---

### Function result sources (LHS of `output` mappings)

These namespaces are only valid on the left-hand side of `output` mapping rules.

| Namespace | Description | Example |
|-----------|-------------|---------|
| `result` | Entire function return value | `result -> output.body` |
| `result.<field>` | Specific field from return value | `result.count -> model.n` |
| `status` | HTTP status code from `EventEnvelope` | `status -> output.status` |
| `header` | All response headers from `EventEnvelope` | `header -> output.header` |
| `header.<name>` | Specific response header | `header.x-trace -> model.trace` |
| `datatype` | Fully-qualified class name of the result | `datatype -> output.header.x-type` |
| `model.<key>` | Current state machine variable | `model.cached -> output.body` |
| `input` | Pass-through of the task's input | `input -> model.saved` |
| `input.<field>` | Specific field from task input | `input.id -> model.id` |
| `$.path` | JSONPath expression on the result | `$.result.items[*].id -> ids` |

---

### Output destinations (RHS of `output` mappings)

| Namespace | Description | Example |
|-----------|-------------|---------|
| `output.body` | HTTP response body | `result -> output.body` |
| `output.body.<field>` | Set a specific field in the response body | `model.name -> output.body.user` |
| `output.header.<name>` | Set an HTTP response header | `text(application/json) -> output.header.content-type` |
| `output.header` | Set the entire header map | `header -> output.header` |
| `output.status` | Set the HTTP response status code (integer) | `int(201) -> output.status` |
| `model.<key>` | Store in flow's state machine | `result -> model.profile` |
| `model.<key>[]` | Append to a list in the state machine | `result.id -> model.id_list[]` |
| `model.parent.<key>` | Store in parent flow's state machine | `result -> model.parent.child_result` |
| `decision` | Set the decision value (decision tasks only) | `result -> decision` |
| `file(<path>)` | Write to a file at the given path | `result -> file(/tmp/output.json)` |
| `file(append:<path>)` | Append to a file | `text(line) -> file(append:/tmp/log.txt)` |
| `ext:<key>` | Write to external state machine | `result -> ext:/session/data` |
| `ext:append` | Append to external state machine list | `result.item -> ext:append` |

---

### Constant types (LHS sources)

Use these functions on the left-hand side of any input mapping to inject a literal value.

| Syntax | Java type | Example |
|--------|-----------|---------|
| `text(value)` | `String` | `text(application/json) -> output.header.content-type` |
| `text(Bearer {model.token})` | `String` with interpolation | `text(Bearer {model.token}) -> headers.Authorization` |
| `int(value)` | `int` | `int(201) -> output.status` |
| `long(value)` | `long` | `long(9223372036854775807) -> max` |
| `float(value)` | `float` | `float(3.14) -> pi` |
| `double(value)` | `double` | `double(3.14159265) -> pi` |
| `boolean(value)` | `boolean` | `boolean(true) -> enabled` |
| `map(k=v, k2=v2, ...)` | `Map<String,String>` | `map(type=json, ver=2) -> config` |
| `map(app.config.key)` | `Map` (from config) | `map(my.config.section) -> settings` |
| `file(text:<path>)` | `String` | `file(text:/tmp/template.txt) -> template` |
| `file(json:<path>)` | `Map` or `List` | `file(json:/tmp/config.json) -> config` |
| `file(binary:<path>)` | `byte[]` | `file(binary:/tmp/image.png) -> bytes` |
| `classpath(text:<path>)` | `String` | `classpath(text:templates/email.txt) -> body` |
| `classpath(json:<path>)` | `Map` or `List` | `classpath(json:defaults.json) -> defaults` |
| `classpath(binary:<path>)` | `byte[]` | `classpath(binary:certs/key.der) -> cert` |

> **Template interpolation in `text()`**: `{model.key}` is replaced at runtime with the
> current value of `model.key`. Also supports `${ENV_VAR}` for environment variables and
> `${app.property}` for application properties.

---

### JSONPath expressions

Use `$.` to apply a JSONPath expression to the input or result:

```yaml
input:
  - '$.input.body.users[*].id -> user_ids'   # Wildcard generates map of lists

output:
  - '$.result.items[*].name -> model.names'
```

> Wildcard searches (e.g. `[*]`) generate a `Map<String, List>` keyed by the last path
> segment. Use `f:listOfMap()` to normalize this into a `List<Map>`.

---

## HTTP input dataset

When a flow is triggered by an HTTP request, the following fields are available under the
`input.` namespace:

| Field | Type | Description |
|-------|------|-------------|
| `method` | String | HTTP method (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS) |
| `uri` | String | Request URI path |
| `header` | Map | Request headers (lowercase keys) |
| `cookie` | Map | HTTP cookies (lowercase keys) |
| `path_parameter` | Map | URL path parameters (as defined in `rest.yaml`) |
| `query` | Map | Query string parameters (lowercase keys) |
| `body` | Map / Object | Parsed request body; JSON → Map, text → String |
| `stream` | String | Stream route ID for file upload or large body streaming |
| `ip` | String | Remote IP address |
| `filename` | String | Original filename (multipart file upload) |
| `session` | Map | Session key-values set by an authentication function |

---

## Pipeline configuration

The `loop` sub-object controls iteration for `pipeline` tasks.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `loop.statement` | string | **Yes** (if `loop` present) | Loop control statement. Must be `for (...)` or `while (...)`. |
| `loop.condition` | string or list | No | Break or continue condition(s). One or more `if (model.key) break\|continue` strings. |

### For loop

```
for (<init>; <comparator>; <sequencer>)
```

| Part | Syntax | Operators |
|------|--------|-----------|
| init (optional) | `model.var = <integer>` | assignment |
| comparator | `model.var < model.max` | `<`, `<=`, `>`, `>=` |
| sequencer | `model.var++` or `model.var--` | `++`, `--` |

```yaml
loop:
  statement: 'for (model.n = 0; model.n < model.limit; model.n++)'
  condition:
    - 'if (model.quit) break'
    - 'if (model.skip) continue'
pipeline:
  - 'step.a'
  - 'step.b'
```

### While loop

```
while (model.key)
```

The loop continues while `model.key` is truthy. `model.key` must be a simple model variable
(no operators).

```yaml
loop:
  statement: 'while (model.running)'
  condition: 'if (model.abort) break'
pipeline:
  - 'step.a'
  - 'step.b'
```

### Loop conditions

Each condition has the form `if (model.key) break` or `if (model.key) continue`. Multiple
conditions can be a YAML list. Conditions are evaluated before each pipeline iteration.

---

## Subflows

A task can invoke another flow as a subroutine using the `flow://` protocol in `process`:

```yaml
- name: 'call.sub.flow'
  input:
    - 'model.data -> input.body'
    - 'model.token -> input.header.authorization'
  process: 'flow://child-flow-id'
  description: 'Delegate to child flow'
  output:
    - 'result -> model.child_result'
  execution: sequential
  next:
    - 'next.step'
```

| Aspect | Detail |
|--------|--------|
| Syntax | `process: 'flow://flow-id'` |
| Input | Data is mapped into `input.body` and `input.header.*` of the child flow |
| Output | The child flow's final `output.body` is available as `result` in the parent |
| Shared state | Parent state accessible via `model.parent.<key>` or `model.root.<key>` in child tasks |
| TTL | The parent flow's remaining TTL governs the subflow; no separate TTL applies |
| Nesting | Subflows can themselves call subflows |

> When the task name is omitted and `process` is a `flow://` URI, the flow ID becomes the
> task name.

---

## Exception handling

### Flow-level exception handler

Declared with `flow.exception`. Catches any unhandled exception from any task in the flow.
The error dataset is available through the `error.*` namespace.

```yaml
flow:
  id: 'my-flow'
  description: 'My flow'
  ttl: 30s
  exception: 'v1.exception.handler'

# ... other fields ...

tasks:
  # ... other tasks ...

  - input:
      - 'error.status -> status'
      - 'error.message -> message'
      - 'error.stack -> stack'
      - 'error.task -> failed_task'
    process: 'v1.exception.handler'
    description: 'Handle flow-level exceptions'
    output:
      - 'result.status -> output.status'
      - 'result -> output.body'
    execution: end
```

### Task-level exception handler

Declared with the `exception` field on an individual task. Overrides the flow-level handler
for that task only.

```yaml
- name: 'risky.task'
  input:
    - 'model.payload -> *'
  process: 'v1.risky.service'
  description: 'Risky operation with its own error handler'
  output:
    - 'result -> model.result'
  execution: sequential
  exception: 'v1.task.exception.handler'
  next:
    - 'next.step'
```

### Built-in resilience handler

The `resilience.handler` function implements retry-with-backoff. Use it as a task-level
exception handler combined with a `decision` execution type:

```yaml
- name: 'my.task'
  process: 'v1.service'
  description: 'Service call with retry'
  output:
    - 'result -> model.result'
  execution: end
  exception: 'resilience.handler'

- input:
    - 'error.status -> status'
    - 'error.message -> message'
    - 'model.attempt -> attempt'
    - 'int(3) -> max_attempts'
  process: 'resilience.handler'
  description: 'Retry decision'
  output:
    - 'result.attempt -> model.attempt'
    - 'result.decision -> decision'
  execution: decision
  next:
    - 'my.task | @retry'
    - 'abort.handler'
```

The `@retry` keyword in a `next` entry re-executes the original task. The `|` pipe operator
provides a fallback task name if `@retry` is not triggered.

---

## Plugin functions (`f:` prefix)

Plugins are used on the **left-hand side** of input mapping rules. They compute a value from
their arguments and place it at the destination.

Syntax: `f:<name>(arg1, arg2, ...) -> destination`

Arguments can be model variables, constant types, or nested plugin calls.

### Arithmetic

| Function | Description | Example |
|----------|-------------|---------|
| `f:add(a, b, ...)` | Sum of arguments | `f:add(model.x, model.y) -> sum` |
| `f:subtract(a, b, ...)` | `a` minus remaining args | `f:subtract(model.total, model.fee) -> net` |
| `f:multiply(a, b)` | Product | `f:multiply(model.price, model.qty) -> total` |
| `f:div(a, b)` | Division (`a / b`) | `f:div(model.total, model.n) -> avg` |
| `f:modulus(a, b)` | Remainder (`a % b`) | `f:modulus(model.n, int(2)) -> rem` |
| `f:increment(a)` | `a + 1` | `f:increment(model.count) -> next_count` |
| `f:decrement(a)` | `a - 1` | `f:decrement(model.count) -> prev_count` |

### Logical and comparison

| Function | Description | Example |
|----------|-------------|---------|
| `f:eq(a, b)` | `a == b` → boolean | `f:eq(model.status, text(ok)) -> is_ok` |
| `f:gt(a, b)` | `a > b` → boolean | `f:gt(model.n, int(10)) -> over_limit` |
| `f:lt(a, b)` | `a < b` → boolean | `f:lt(model.n, int(0)) -> negative` |
| `f:and(a, b, ...)` | Logical AND | `f:and(model.ready, model.valid) -> ok` |
| `f:or(a, b, ...)` | Logical OR | `f:or(model.err1, model.err2) -> has_error` |
| `f:not(a)` | Logical NOT | `f:not(model.flag) -> inverted` |
| `f:ternary(cond, a, b)` | `cond ? a : b` | `f:ternary(model.ok, text(pass), text(fail)) -> label` |
| `f:isNull(a)` | `a == null` → boolean | `f:isNull(model.opt) -> is_missing` |
| `f:notNull(a)` | `a != null` → boolean | `f:notNull(model.opt) -> is_present` |

### Type conversion

| Function | Description | Example |
|----------|-------------|---------|
| `f:text(a)` | Convert to String | `f:text(model.n) -> label` |
| `f:int(a)` | Convert to int | `f:int(model.str) -> count` |
| `f:long(a)` | Convert to long | `f:long(model.str) -> ts` |
| `f:float(a)` | Convert to float | `f:float(model.str) -> price` |
| `f:double(a)` | Convert to double | `f:double(model.str) -> ratio` |
| `f:boolean(a)` | Convert to boolean | `f:boolean(model.str) -> flag` |
| `f:binary(a)` | Convert to byte[] | `f:binary(model.text) -> bytes` |
| `f:b64(a)` | Base64 encode/decode | `f:b64(model.bytes) -> encoded` |

### String operations

| Function | Description | Example |
|----------|-------------|---------|
| `f:concat(a, b, ...)` | Concatenate strings | `f:concat(model.first, text( ), model.last) -> name` |
| `f:substring(str, start)` | Substring from start | `f:substring(model.text, int(6)) -> tail` |
| `f:substring(str, start, end)` | Substring range | `f:substring(model.text, int(0), int(5)) -> head` |
| `f:length(a)` | Length of string or list | `f:length(model.items) -> count` |

### Collection operations

| Function | Description | Example |
|----------|-------------|---------|
| `f:listOfMap(a)` | Normalize JSONPath wildcard result (`Map<String,List>`) to `List<Map>` | `f:listOfMap(model.rows) -> rows` |
| `f:updateListOfMap(list, extra)` | Merge additional fields into each map in the list | `f:updateListOfMap(model.rows, model.extra) -> augmented` |
| `f:removeKey(map, key)` | Remove a key from a map or each map in a list | `f:removeKey(model.obj, text(secret)) -> clean` |

### Generators

| Function | Description | Example |
|----------|-------------|---------|
| `f:uuid()` | Generate a random UUID string | `f:uuid() -> id` |
| `f:dateTime()` | Current date-time (ISO-8601) | `f:dateTime() -> now` |
| `f:date(format)` | Formatted date string | `f:date(text(yyyy-MM-dd)) -> today` |

> Plugins are validated at compile time. They may only use classes from `java.lang`,
> `java.util`, `java.math`, `java.time`, and Mercury framework packages.

---

## External state machine

When `external.state.machine` is declared at the flow level, tasks can read from and write to
a separate service that manages cross-flow or cross-instance state.

The `ext:` namespace is used in output mappings to write to the external state machine.
To read from it, invoke the external state machine service directly via a `sequential` task.

```yaml
flow:
  id: 'session-flow'
  description: 'Flow with external session store'
  ttl: 30s
  external.state.machine: 'v1.session.store'

# ...

  # Write to external state machine
  - input:
      - 'input.path_parameter.user -> ext:/session/user'
      - 'input.body -> ext:/session/payload'
      - 'model.none -> ext:/session/old_key'        # delete key
    process: 'no.op'
    description: 'Persist session data'
    output: []
    execution: sequential
    next:
      - 'next.step'
```

| Syntax | Action |
|--------|--------|
| `value -> ext:<key>` | Write value to external state at `<key>` |
| `model.none -> ext:<key>` | Delete `<key>` from external state |
| `result -> ext:append` | Append to a list in the external state |

> `external.state.machine` can be a service route (`v1.session.store`) or a subflow
> (`flow://session-flow`).

---

## Built-in special variables

These `model.*` variables are set by the framework automatically.

| Variable | Description |
|----------|-------------|
| `model.trace` | Current distributed trace ID |
| `model.flow` | Unique ID of this flow instance |
| `model.instance` | Alias for `model.flow` |
| `model.none` | Always `null`; use to clear model keys or delete file/ext destinations |
| `model.<source>.ITEM` | Current item in a dynamic fork iteration |
| `model.<source>.INDEX` | Zero-based index in a dynamic fork iteration |

---

## Built-in service routes

| Route | Purpose |
|-------|---------|
| `no.op` | No-operation function; passes input through unchanged. Useful as a routing-only task. |
| `resilience.handler` | Built-in retry-with-backoff handler. Use as `exception:` on a task. |
| `simple.exception.handler` | Default exception handler; returns error details as HTTP response. |
| `async.http.request` | Built-in HTTP client. Set `url`, `method`, `body`, `headers.*`, `parameters.query.*` as input. |

---

## Complete annotated example

A realistic flow that creates a user profile, responds immediately, then asynchronously
encrypts sensitive fields.

```yaml
# Flow-level metadata
flow:
  id: 'create-profile'
  description: 'Create a new user profile and encrypt sensitive fields'
  ttl: 30s                               # Total time budget for the flow
  exception: 'v1.exception.handler'      # Catches any unhandled task exception

first.task: 'v1.create.profile'          # Entry point

tasks:

  # Step 1: Store the profile
  - input:
      - 'input.body -> *'                # Map the entire request body as function input
    process: 'v1.create.profile'
    description: 'Persist the new profile to the data store'
    output:
      - 'result -> model.profile'        # Store result in flow state machine
    execution: response                  # Send HTTP response immediately, then continue
    output:
      - 'result -> output.body'
      - 'int(201) -> output.status'
      - 'text(application/json) -> output.header.content-type'
    next:
      - 'v1.encrypt.fields'              # Continue asynchronously

  # Step 2: Encrypt sensitive fields (runs after response is sent)
  - input:
      - 'model.profile -> *'             # Pass stored profile as function input
    process: 'v1.encrypt.fields'
    description: 'Encrypt address and telephone in the persisted record'
    output:
      - 'result -> model.encrypted'
    execution: sequential
    next:
      - 'v1.save.encrypted'

  # Step 3: Save the encrypted profile
  - input:
      - 'model.encrypted -> *'
    process: 'v1.save.encrypted'
    description: 'Persist the encrypted profile back to the data store'
    output: []
    execution: end                       # Terminate the flow; no response (already sent)

  # Exception handler task (referenced by flow.exception)
  - input:
      - 'error.status -> status'         # HTTP status from the thrown exception
      - 'error.message -> message'       # Error message
      - 'error.stack -> stack'           # Stack trace
      - 'error.task -> failed_task'      # Which task failed
    process: 'v1.exception.handler'
    description: 'Return a structured error response'
    output:
      - 'result.status -> output.status'
      - 'result -> output.body'
    execution: end
```

---

## See also

- [Event Script Syntax](CHAPTER-4.md) — tutorial-style walkthrough with worked examples
- [REST Automation](CHAPTER-3.md) — `rest.yaml` configuration for HTTP endpoint routing
- [Configuration Reference](CONFIGURATION-REFERENCE.md) — `application.properties` keys
  including `yaml.flow.automation`
