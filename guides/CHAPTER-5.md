# Build, Test and Deploy

The first step in writing an application is to create an entry point for your application.

## Main application

A minimalist main application template is shown as follows:

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

You must have at least one "main application" module because it is mandatory.

> *Note*: Please adjust the parameter "web.component.scan" in application.properties 
  to point to your user application package(s) in your source code project.

If your application does not require additional startup logic, you may just print a greeting message.

The `AutoStart.main()` statement in the "main" method is used when you want to start your application within the IDE.
You can "right-click" the main method and select "run".

You can also build and run the application from command line like this:

```shell
cd sandbox/mercury-composable/examples/lambda-example
mvn clean package
java -jar target/lambda-example-4.0.16.jar
```

The lambda-example is a sample application that you can use as a template to write your own code. Please review
the pom.xml and the source directory structure.

In the lambda-example project root, you will find the following directories:

```shell
src/main/java
src/test/java
```

Since all functions are connected using the in-memory event bus, you can test any function by sending events
from a unit test.

## Writing your functions

Please follow the step-by-step learning guide in [Chapter-1](CHAPTER-1.md) to write your own functions. You can then
configure new REST endpoints to use your new functions.

In [Chapter-2](CHAPTER-2.md), we have discussed the two function execution strategies to optimize your application
to the full potential of stability, performance and throughput.

## HTTP forwarding

In [Chapter-3](CHAPTER-3.md), we have presented the configuration syntax for the "rest.yaml" REST automation 
definition file. Please review the sample rest.yaml file in the lambda-example project. You may notice that
it has an entry for HTTP forwarding. The following entry in the sample rest.yaml file illustrates an HTTP 
forwarding endpoint. In HTTP forwarding, you can replace the "service" route name with a direct HTTP target host.
You can do "URL rewrite" to change the URL path to the target endpoint path. In the below example, 
`/api/v1/*` will be mapped to `/api/*` in the target endpoint.

```yaml
  - service: "http://127.0.0.1:${rest.server.port}"
    trust_all_cert: true
    methods: ['GET', 'PUT', 'POST']
    url: "/api/v1/*"
    url_rewrite: ['/api/v1', '/api']
    timeout: 20
    cors: cors_1
    headers: header_1
    tracing: true
```

## Sending HTTP request event to more than one service

One feature in REST automation "rest.yaml" configuration is that you can configure more than one function in the
"service" section. In the following example, there are two function route names ("hello.world" and "hello.copy"). 
The first one "hello.world" is the primary service provider. The second one "hello.copy" will receive a copy of 
the incoming event.

This feature allows you to write new version of a function without disruption to current functionality. Once you are
happy with the new version of function, you can route the endpoint directly to the new version by updating the
"rest.yaml" configuration file.

```yaml
  - service: ["hello.world", "hello.copy"]
```

## Writing your first unit test

Please refer to "rpcTest" method in the "HelloWorldTest" class in the lambda-example to get started.

In unit test, we want to start the main application so that all the functions are ready for tests.

First, we write a "TestBase" class to use the BeforeClass setup method to start the main application like this:

```java
public class TestBase {

    private static final AtomicInteger seq = new AtomicInteger(0);

    @BeforeClass
    public static void setup() {
        if (seq.incrementAndGet() == 1) {
            AutoStart.main(new String[0]);
        }
    }
}
```

The atomic integer "seq" is used to ensure the main application entry point is executed only once.

A typical unit test may look like this:

```java
@SuppressWarnings("unchecked")
@Test
public void rpcTest() throws InterruptedException, ExecutionException {
    Utility util = Utility.getInstance();
    String NAME = "hello";
    String ADDRESS = "world";
    String TELEPHONE = "123-456-7890";
    DemoPoJo pojo = new DemoPoJo(NAME, ADDRESS, TELEPHONE);
    PostOffice po = new PostOffice("unit.test", "12345", "POST /api/hello/world");
    EventEnvelope request = new EventEnvelope().setTo("hello.world").setBody(pojo.toMap());
    EventEnvelope response = po.request(request, 8000).get();
    assert response != null;
    assertInstanceOf(Map.class, response.getBody());
    MultiLevelMap map = new MultiLevelMap((Map<String, Object>) response.getBody());
    assertEquals(NAME, map.getElement("body.name"));
    assertEquals(ADDRESS, map.getElement("body.address"));
    assertEquals(TELEPHONE, map.getElement("body.telephone"));
    assertEquals(util.date2str(pojo.time), map.getElement("body.time"));
}
```

Note that the PostOffice instance can be created with tracing information in a Unit Test.
The above example tells the system that the sender is "unit.test", the trace ID is 12345
and the trace path is "POST /api/hello/world".

## Your second unit test

Let's do a unit test for PoJo. In this second unit test, it sends a RPC request to the "hello.pojo" function that
is designed to return a SamplePoJo object with some mock data.

Please refer to "pojoRpcTest" method in the "PoJoTest" class in the lambda-example for details.

The unit test verifies that the "hello.pojo" has correctly returned the SamplePoJo object with the pre-defined 
mock value.

```java
@Test
public void pojoRpcTest() throws InterruptedException {
    Integer ID = 1;
    String NAME = "Simple PoJo class";
    String ADDRESS = "100 World Blvd, Planet Earth";
    BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
    PostOffice po = new PostOffice("unit.test", "20001", "GET /api/hello/pojo");
    EventEnvelope request = new EventEnvelope().setTo("hello.pojo").setHeader("id", "1");
    po.asyncRequest(request, 8000).onSuccess(bench::add);
    EventEnvelope response = bench.poll(10, TimeUnit.SECONDS);
    assert response != null;
    assertEquals(HashMap.class, response.getBody().getClass());
    SamplePoJo pojo = response.getBody(SamplePoJo.class);
    assertEquals(ID, pojo.getId());
    assertEquals(NAME, pojo.getName());
    assertEquals(ADDRESS, pojo.getAddress());
}
```

Note that you can use the built-in serialization API to restore a PoJo like this:

```java
SamplePoJo pojo = response.getBody(SamplePoJo.class)
```

## Convenient utility classes

The `Utility` and `MultiLevelMap` classes are convenient tools for unit tests. In the above example, we use the
Utility class to convert a date object into a UTC timestamp. It is because date object is serialized as a UTC
timestamp in an event.

The `MultiLevelMap` supports reading an element using the convenient "dot and bracket" format.

For example, given a map like this:
```json
{
  "body":
  {
    "time": "2023-03-27T18:10:34.234Z",
    "hello": [1, 2, 3],
    "complex": [
      {"key": "value1"},
      {"key": "value2"}
    ]
  }
}
```

| Example | Command                                | Result                   |
|:-------:|:---------------------------------------|:-------------------------|
|    1    | map.getElement("body.time")            | 2023-03-27T18:10:34.234Z |
|    2    | map.getElement("body.hello[2]")        | 3                        |
|    3    | map.getElement("body.complex[1].key")  | value2                   |
|    4    | map.getElements("body.complex[*].key") | [ value1, value2 ]       |

Example-4 above uses the "getElements" in plural form to indicate that it is retrieving a list of elements
using a "wildcard" index. For simplicity, it does not support more than one wildcard index in the search path.

## Event Flow mocking framework

We recommend using Event Script to write Composable application for highest level of decoupling.
Event Script supports sophisticated event choreography by configuration.

In Event Script, you have a event flow configuration and a few Composable functions in an application.
Composable functions are self-contained with zero dependencies with other composable functions. 
You can invoke an event flow from an event flow adapter.

The most common flow adapter is the "HTTP flow adapter" and it is available as a built-in module in
the event-script-engine module in the system. You can associate many REST endpoints to the HTTP flow
adapter.

Since function routes for each composable function is defined in a event flow configuration and the same
function route may be used for more than one task in the flow, the system provides a mock helper class
called "EventScriptMock" to let your unit tests to override a task's function routes during test.

In the following unit test example for a "pipeline" test, we created a mock function "my.mock.function"
to override the "no.op" function that is associated with the first task "echo.one" in a pipeline.

The original "no.op" function is an echo function. The mocked function increments a counter
in addition to just echoing the input payload. In this fashion, the unit test can count the number
of iteration of a pipeline to validate the looping feature of a pipeline.

The unit test programmatically registers the mock function and then release it from the event loop
when the test finishes.

```java
    @SuppressWarnings("unchecked")
    @Test
    void pipelineForLoopTest() throws InterruptedException {
        Platform platform = Platform.getInstance();
        // The first task of the flow "for-loop-test" is "echo.one" that is using "no.op".
        // We want to override no.op with my.mock.function to demonstrate mocking a function
        // for a flow.
        var ECHO_ONE = "echo.one";
        var MOCK_FUNCTION = "my.mock.function";
        var iteration = new AtomicInteger(0);
        LambdaFunction f = (headers, body, instance) -> {
            var n = iteration.incrementAndGet();
            log.info("Iteration-{} {}", n, body);
            return body;
        };
        platform.registerPrivate(MOCK_FUNCTION, f, 1);
        // override the function for the task "echo.one" to the mock function
        var mock = new EventScriptMock("for-loop-test");
        var previousRoute = mock.getFunctionRoute(ECHO_ONE);
        var currentRoute = mock.assignFunctionRoute(ECHO_ONE, MOCK_FUNCTION).getFunctionRoute(ECHO_ONE);
        assertEquals("no.op", previousRoute);
        assertEquals(MOCK_FUNCTION, currentRoute);
        final BlockingQueue<EventEnvelope> bench = new ArrayBlockingQueue<>(1);
        final long TIMEOUT = 8000;
        String USER = "test-user";
        int SEQ = 100;
        AsyncHttpRequest request = new AsyncHttpRequest();
        request.setTargetHost(HOST).setMethod("GET").setHeader("accept", "application/json");
        request.setUrl("/api/for-loop/"+USER).setQueryParameter("seq", SEQ);
        EventEmitter po = EventEmitter.getInstance();
        EventEnvelope req = new EventEnvelope().setTo(HTTP_CLIENT).setBody(request);
        po.asyncRequest(req, TIMEOUT).onSuccess(bench::add);
        EventEnvelope res = bench.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assert res != null;
        assertInstanceOf(Map.class, res.getBody());
        Map<String, Object> result = (Map<String, Object>) res.getBody();
        assertTrue(result.containsKey("data"));
        PoJo pojo = SimpleMapper.getInstance().getMapper().readValue(result.get("data"), PoJo.class);
        assertEquals(SEQ, pojo.sequence);
        assertEquals(USER, pojo.user);
        assertEquals(3, result.get("n"));
        assertEquals(3, iteration.get());
        platform.release(MOCK_FUNCTION);
    }
```

When the event flow finishes, you will see an "end-of-flow" report like this. It shows that the function
route for the "echo.one" task has been changed to "my.mock.function". This end-of-flow log is useful
during application development and tests so that the developer knows exactly which function has been
executed.

```log
Flow for-loop-test (0afcf555fc4141f4a16393422e468dc9) completed. Run 11 tasks in 28 ms. 
[ sequential.one, 
  echo.one(my.mock.function), 
  echo.two(no.op), 
  echo.three(no.op), 
  echo.one(my.mock.function), 
  echo.two(no.op), 
  echo.three(no.op), 
  echo.one(my.mock.function), 
  echo.two(no.op), 
  echo.three(no.op), 
  echo.four(no.op) ]
```

## Inspecting the state machine using EventScriptMock

The state machine of a "flow instance" is not accessible directly by a user task. To inspect the state machine
in a unit test, you can use the `setMonitorBeforeTask` and `setMonitorAfterTask` methods. The former tells the
system to send a copy of the state machine to a composable function after "input data mapping" but before entering
a task. The latter sends a copy of the state machine to a composable function after a task is completed.

The following code segment from a unit test illustrates this feature. The function "before.task.monitor" will get
a copy of the state machine before "my.task" executes. Similarly, the function "after.task.monitor" will obtain
a copy of the state machine after "my.task" finishes execution.

```java
var platform = Platform.getInstance();
var mock = new EventScriptMock("parent-greetings");
TypedLambdaFunction<Map<String, Object>, Void> f1 =
        (headers, input, instance) -> {
            before.add(input);
            return null;
        };
platform.registerPrivate("before.task.monitor", f1, 1);
TypedLambdaFunction<Map<String, Object>, Void> f2 =
        (headers, input, instance) -> {
            after.add(input);
            return null;
        };
platform.registerPrivate("after.task.monitor", f2, 1);
mock.setMonitorBeforeTask("my.task", "before.task.monitor")
    .setMonitorAfterTask("my.task", "after.task.monitor");
```

## Deployment

The pom.xml is pre-configured to generate an executable JAR. The following is extracted from the pom.xml.

The main class is `AutoStart` that will load the "main application" and use it as the entry point to
run the application.

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <mainClass>org.platformlambda.core.system.AutoStart</mainClass>
    </configuration>
    <executions>
        <execution>
            <id>build-info</id>
            <goals>
                <goal>build-info</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Composable application is designed to be deployable using Kubernetes or serverless.

A sample Dockerfile for an executable JAR may look like this:

```shell
FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu
EXPOSE 8083
WORKDIR /app
COPY target/rest-spring-3-example-4.2.3.jar .
ENTRYPOINT ["java","-jar","rest-spring-3-example-4.2.3.jar"]
```

## Distributed tracing

The system has a built-in distributed tracing feature. You can enable tracing for any REST endpoint by adding
"tracing=true" in the endpoint definition in the "rest.yaml" configuration file.

You may also upload performance metrics from the distributed tracing data to your favorite telemetry system dashboard.

To do that, you can implement a custom metrics function with the route name `distributed.trace.forwarder`.

The input to the function will be a HashMap like this:

```shell
trace={path=/api/upload/demo, service=hello.upload, success=true, 
       origin=2023032731e2a5eeae8f4da09f3d9ac6b55fb0a4, 
       exec_time=77.462, start=2023-03-27T19:38:30.061Z, 
       from=http.request, id=12345, round_trip=132.296, status=200}
```

The system will detect if `distributed.trace.forwarder` is available. If yes, it will forward performance metrics
from distributed trace to your custom function.

## Request-response journaling

Optionally, you may also implement a custom audit function named `transaction.journal.recorder` to monitor 
request-response payloads.

To enable journaling, please add this to the application.properties file.

```properties
journal.yaml=classpath:/journal.yaml
```
and add the "journal.yaml" configuration file to the project's resources folder with content like this:

```yaml
journal:
  - "my.test.function"
  - "another.function"
```

In the above example, the "my.test.function" and "another.function" will be monitored and their request-response
payloads will be forwarded to your custom audit function. The input to your audit function will be a HashMap
containing the performance metrics data and a "journal" section with the request and response payloads in clear form.

> *IMPORTANT*: journaling may contain sensitive personally identifiable data and secrets. Please check
  security compliance before storing them into access restricted audit data store.

## Performance tuning

The composable framework is designed for high concurrency using virtual threads and new channel I/O (NIO).

As a result, it can generate a massive volume of outgoing traffic to your system of records. If not managed properly,
massive parallelism can actually degrade overall performance because it can become a form of unintended
"denial of service" attack.

Composable applications are, by definition, cloud native. You can scale your applications horizontally.

In addition, composable functions in each application instance can be scaled vertically using Java virtual thread
technology. Back-pressure is automated so your application usually does not need to do advanced coding to do flow
control.

For each composable function, you can define concurrency using the "instances" parameter in the "PreLoad" class
annotation. Note that if the function makes outgoing calls to an external dependency that is slow, it can consume
"worker" threads very quickly because the "workers" are waiting for a response from the external dependency.

Therefore, you should increase the concurrency count to adjust for your performance requirement.

If you configure parallel processing, especially when using the parallel pipeline method, it can spin up instances
of composable functions in an "uncontrolled" manner. It would result in making too many parallel requests to an
external dependency. For external system that runs using legacy technology, it can easily be overwhelmed,
resulting in performance bottleneck. It can block the calling functions from a composable application and
thus the outcome can be suboptimal.

When configuring parallel calls, please consider end-to-end connectivity and potential bottlenecks.

Since event flow instances are already running in parallel, configuring parallel requests within a single event flow
would reduce performance. An orderly executed pipeline would be faster than a parallel pipeline that is
"uncontrolled". On the other hands, fork-n-join uses a predetermined number of parallel tasks and it is easier
to control parallelism.

Performance tuning is an art than a science. A holistic view of end-to-end performance and careful configuration
of parallelism would yield good outcome.

## Performance metrics

The built-in telemetry system offers basic performance metrics that can be visualized with a telemetry dashboard.

For more advanced performance metrics, you may refer to the "end-of-flow" performance report. It states the
sequence of task execution and their elapsed time that includes execution time of a function, routing overheads
and all system overheads. It may look like this when you configure "log.format=json" in application.properties.

```json
{
  "level": "INFO",
  "time": "2025-08-27 18:39:25.683",
  "source": "org.platformlambda.core.services.Telemetry.handleEvent(Telemetry.java:81)",
  "thread": 336,
  "message": {
    "trace": {
      "path": "GET /api/profile/100",
      "service": "task.executor",
      "success": true,
      "origin": "20250828c022812c67294a63871942c568a9e277",
      "exec_time": 7.0,
      "start": "2025-08-28T01:39:25.674Z",
      "from": "event.script.manager",
      "id": "9c0934a98dcf4ab1ae4b5b7b389f6d31",
      "status": 200
    },
    "annotations": {
      "execution": "Run 2 tasks in 7 ms",
      "tasks": [
        {
          "name": "v1.get.profile",
          "spent": 2.982
        },
        {
          "name": "v1.decrypt.fields",
          "spent": 1.313
        }
      ],
      "flow": "get-profile"
    }
  }
}
```
<br/>

|              Chapter-4              |                   Home                    |          Chapter-6          |
|:-----------------------------------:|:-----------------------------------------:|:---------------------------:|
| [Event Script Syntax](CHAPTER-4.md) | [Table of Contents](TABLE-OF-CONTENTS.md) | [Spring Boot](CHAPTER-6.md) |
