# Actuators, HTTP client and More

## Actuator endpoints

The following admin endpoints are available.

```
GET /info
GET /info/routes
GET /info/lib
GET /env
GET /health
GET /livenessprobe
POST /shutdown
```

| Endpoint       | Purpose                                                                             | 
|:---------------|:------------------------------------------------------------------------------------|
| /info          | Describe the application                                                            |
| /info/routes   | Show public routing table                                                           |
| /info/lib      | List libraries packed with this executable                                          |
| /env           | List all private and public function route names and selected environment variables |
| /health        | Application health check endpoint                                                   |
| /livenessprobe | Check if application is running normally                                            |
| /shutdown      | Operator may use this endpoint to do a POST command to stop the application         |

For the shutdown endpoint, you must provide an `X-App-Instance` HTTP header where the value is the "origin ID"
of the application. You can get the value from the "/info" endpoint.

## Custom health services

You can extend the "/health" endpoint by implementing and registering lambda functions to be added to the 
"health check" dependencies.

```properties
mandatory.health.dependencies=cloud.connector.health, demo.health
optional.health.dependencies=other.service.health
```

Your custom health service must respond to the following requests:

1. Info request (type=info) - it should return a map that includes service name and href (protocol, hostname and port)
2. Health check (type=health) - it should return a text string or a Map of the health check. e.g. read/write test result. 
   If health check fails, you can throw AppException with status code and error message.

A sample health service is available in the `DemoHealth` class of the `composable-example` project as follows:

```java
@PreLoad(route="demo.health", instances=5)
public class DemoHealth implements LambdaFunction {

    private static final String TYPE = "type";
    private static final String INFO = "info";
    private static final String HEALTH = "health";

    @Override
    public Object handleEvent(Map<String, String> headers, Object input, int instance) {
        /*
         * The interface contract for a health check service includes both INFO and HEALTH responses.
         * It must return a Map.
         */
        if (INFO.equals(headers.get(TYPE))) {
            Map<String, Object> about = new HashMap<>();
            about.put("service", "demo.service");
            about.put("href", "http://127.0.0.1");
            return about;
        }
        if (HEALTH.equals(headers.get(TYPE))) {
            /*
             * This is a place-holder for checking a downstream service.
             *
             * Please implement your own logic to test if a downstream service is running fine.
             * If running, just return health status as a String or a Map.
             *
             * Otherwise,
             *      throw new AppException(status, message)
             */
            return Map.of("demo", "I am running fine");
        }
        throw new IllegalArgumentException("type must be info or health");
    }
}
```

## AsyncHttpClient service

The "async.http.request" function can be used as a non-blocking HTTP client.

To make an HTTP request to an external REST endpoint, you can create an HTTP request object using the
`AsyncHttpRequest` class and make an async RPC call to the "async.http.request" function like this:

```java
PostOffice po = new PostOffice(headers, instance);
AsyncHttpRequest req = new AsyncHttpRequest();
req.setMethod("GET");
req.setHeader("accept", "application/json");
req.setUrl("/api/hello/world?hello world=abc");
req.setQueryParameter("x1", "y");
List<String> list = new ArrayList<>();
list.add("a");
list.add("b");
req.setQueryParameter("x2", list);
req.setTargetHost("http://127.0.0.1:8083");
EventEnvelope request = new EventEnvelope().setTo("async.http.request").setBody(req);
EventEnvelope res = po.request(request, 5000);
// the result is in res.getBody()
```

By default, your user function is running in a virtual thread.
While the RPC call looks like synchronous, the po.request API will run in non-blocking mode in the same fashion
as the "async/await" pattern.

For reactive programming, you can use the "asyncRequest" API like this:

```java
PostOffice po = new PostOffice(headers, instance);
AsyncHttpRequest req = new AsyncHttpRequest();
req.setMethod("GET");
req.setHeader("accept", "application/json");
req.setUrl("/api/hello/world?hello world=abc");
req.setQueryParameter("x1", "y");
List<String> list = new ArrayList<>();
list.add("a");
list.add("b");
req.setQueryParameter("x2", list);
req.setTargetHost("http://127.0.0.1:8083");
EventEnvelope request = new EventEnvelope().setTo("async.http.request").setBody(req);
Future<EventEnvelope> res = po.asyncRequest(request, 5000);
res.onSuccess(response -> {
   // do something with the result 
});
```

If you prefer writing in Kotlin, you can create a suspend function using KotlinLambdaFunction, 
the same logic may look like this:

```java
val fastRPC = FastRPC(headers)
val req = AsyncHttpRequest()
req.setMethod("GET")
req.setHeader("accept", "application/json")
req.setUrl("/api/hello/world?hello world=abc")
req.setQueryParameter("x1", "y")
val list: MutableList<String> = ArrayList()
list.add("a")
list.add("b")
req.setQueryParameter("x2", list)
req.setTargetHost("http://127.0.0.1:8083")
val request = EventEnvelope().setTo("async.http.request").setBody(req)
val response = fastRPC.awaitRequest(request, 5000)
// do something with the result
```

### Send HTTP request body for HTTP PUT, POST and PATCH methods

For most cases, you can just set a HashMap into the request body and specify content-type as JSON or XML.
The system will perform serialization properly.

Example code may look like this:

```java
AsyncHttpRequest req = new AsyncHttpRequest();
req.setMethod("POST");
req.setHeader("accept", "application/json");
req.setHeader("content-type", "application/json");
req.setUrl("/api/book");
req.setTargetHost("https://service_provider_host");
req.setBody(mapOfKeyValues);
// where keyValues is a HashMap
```

## Send HTTP request body as a stream

For larger payload, you may use the streaming method. See sample code below:

```java
int len;
byte[] buffer = new byte[4096];
FileInputStream in = new FileInputStream(myFile);
ObjectStreamIO stream = new ObjectStreamIO(timeoutInSeconds);
ObjectStreamWriter out = stream.getOutputStream();
while ((len = in.read(buffer, 0, buffer.length)) != -1) {
    out.write(buffer, 0, len);
}
// closing the output stream would send a EOF signal to the stream
out.close();
// tell the HTTP client to read the input stream by setting the streamId in the AsyncHttpRequest object
req.setStreamRoute(stream.getInputStreamId());
```

## Read HTTP response body stream

If content length is not given, the response body would arrive as a stream.

Your application should check if the HTTP response header "stream" exists. Its value is the input "streamId".

For simplicity and readability, we recommend using the PostOffice's "request" API to read the input byte-array stream.

It looks like this:

```java
PostOffice po = PostOffice(headers, instance);
EventEnvelope req = new EventEnvelope().setTo(streamId).setHeader("type", "read");
while (true) {
    EventEnvelope event = po.request(req, 5000).get();
    if (event.getStatus() == 400) {
        // handle input stream timeout
     }
     if ("eof".equals(event.getHeader("type"))) {
         log.info("Closing {}", streamId);
         po.send(streamId, new Kv("type", "close"));
         break;
     }
     if ("data".equals(event.getHeader("type"))) {
         Object block = event.getBody();
         if (block instanceof byte[] b) {
            // handle the byte array "b"
         }
     }
}
```

By default, a user function is executed in a virtual thread which effectively is an "async" function and
the PostOffice "request" API operates in the non-blocking "await" mode.

If you prefers writing in Kotlin, it may look like this:

```java
val po = PostOffice(headers, instance)
val fastRPC = FastRPC(headers)

val req = EventEnvelope().setTo(streamId).setHeader("type", "read")
while (true) {
    val event = fastRPC.awaitRequest(req, 5000)
    if (event.status == 408) {
        // handle input stream timeout
        break
    }
    if ("eof" == event.headers["type"]) {
        po.send(streamId, Kv("type", "close"))
        break
    }
    if ("data" == event.headers["type"]) {
        val block = event.body
        if (block is ByteArray) {
            // handle the data block from the input stream
        }
    }
}
```

## Rendering a small payload of streaming content

If the streaming HTTP response is certain to be a small payload (i.e. Kilobytes), you can optimize
the rendering by adding the HTTP request header (X-Small-Payload-As-Bytes=true) in the AsyncHttpRequest object.

```java
AsyncHttpRequest req = new AsyncHttpRequest();
req.setMethod("GET");
req.setUrl("/api/some/binary/content");
req.setTargetHost("https://service_provider_host");
req.setHeader("X-Small-Payload-As-Bytes", "true");
```

Note that the AsyncHttpClient will insert a custom HTTP response header "X-Content-Length" to show the size
of the payload.

> IMPORTANT: This optimization does not validate the size of the streaming content. Therefore, it is possible for
             the streaming content to trigger an "out of memory" exception. You must make sure the streaming content
             is small enough before using the "X-Small-Payload-As-Bytes" HTTP request header.

## Content length for HTTP request

IMPORTANT: Do not set the "content-length" HTTP header because the system will automatically compute the
correct content-length for small payload. For large payload, it will use the chunking method.

## Starting a flow programmatically

To start an "event" flow from a unit test, you may use the helper class "StartFlow" under the "Event Script" module.

Examples of some APIs are as follows:

```java
// launch a flow asychronously
public void launch(String originator, String flowId, Map<String, Object> dataset,
                       String correlationId) throws IOException;
// launch a flow asychronously with tracing
public void launch(String originator, String traceId, String tracePath, String flowId,
                       Map<String, Object> dataset, String correlationId) throws IOException
// launch a flow asychronously and tracing
public void launch(PostOffice po, String flowId, Map<String, Object> dataset,
                        String correlationId) throws IOException;
// launch a flow with callback and tracing
public void launch(PostOffice po, String flowId, Map<String, Object> dataset,
                        String replyTo, String correlationId) throws IOException;
public Future<EventEnvelope> request(PostOffice po, String flowId, Map<String, Object> dataset,
                                     String correlationId, long timeout) throws IOException;
```

The following unit test emulates a HTTP request to the flow named "header-test".

```java
@Test
public void internalFlowTest() throws IOException, ExecutionException, InterruptedException {
    final long TIMEOUT = 8000;
    String traceId = Utility.getInstance().getUuid();
    PostOffice po = new PostOffice("unit.test", traceId, "INTERNAL /flow/test");
    String flowId = "header-test";
    Map<String, Object> headers = new HashMap<>();
    Map<String, Object> dataset = new HashMap<>();
    dataset.put("header", headers);
    dataset.put("body", Map.of("hello", "world"));
    headers.put("user-agent", "internal-flow");
    headers.put("accept", "application/json");
    headers.put("x-flow-id", flowId);
    StartFlow startFlow = StartFlow.getInstance();
    EventEnvelope result = startFlow.request(po, flowId, dataset, traceId, TIMEOUT).get();
    assertInstanceOf(Map.class, result.getBody());
    Map<String, Object> body = (Map<String, Object>) result.getBody();
    // verify that input headers are mapped to the function's input body
    assertEquals("header-test", body.get("x-flow-id"));
    assertEquals("internal-flow", body.get("user-agent"));
    assertEquals("application/json", body.get("accept"));
}
```

The dataset must contain at least the "body" key-value so that input data mapping is possible in a flow.

For the built-in HTTP flow adapter, the dataset would contain the following:

```java
// convert HTTP context to flow "input" dataset
Map<String, Object> dataset = new HashMap<>();
dataset.put("header", request.getHeaders());
dataset.put("body", request.getBody());
dataset.put("cookie", request.getCookies());
dataset.put("path_parameter", request.getPathParameters());
dataset.put("method", request.getMethod());
dataset.put("uri", request.getUrl());
dataset.put("query", request.getQueryParameters());
dataset.put("stream", request.getStreamRoute());
dataset.put("ip", request.getRemoteIp());
dataset.put("filename", request.getFileName());
dataset.put("session", request.getSessionInfo());
```

If you write your own Kafka flow adapter, the dataset should contain headers and body mapped with a Kafka event.

For other flow adapters, you may use different set of key-values.

<br/>

|              Appendix-II               |                   Home                    | 
|:--------------------------------------:|:-----------------------------------------:|
| [Reserved route names](APPENDIX-II.md) | [Table of Contents](TABLE-OF-CONTENTS.md) |
