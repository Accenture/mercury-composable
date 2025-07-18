# REST Automation

The platform-core foundation library contains a built-in non-blocking HTTP server that you can use to create REST
endpoints. Behind the curtain, it is using the vertx web client and server libraries.

The REST automation system is not a code generator. The REST endpoints in the rest.yaml file are handled by
the system directly - "Config is the code".

We will use the "rest.yaml" sample configuration file in the "lambda-example" project to elaborate the configuration
approach.

The rest.yaml configuration has three sections:

1. REST endpoint definition
2. CORS header processing
3. HTTP header transformation

## Turn on the REST automation engine

REST automation is optional. To turn on REST automation, add or update the following parameters in the
application.properties file (or application.yml if you like).

```properties
rest.server.port=8085
rest.automation=true
yaml.rest.automation=classpath:/rest.yaml
```

When `rest.automation=true`, you can configure the server port using `rest.server.port` or `server.port`.

REST automation can co-exist with Spring Boot. Please use `rest.server.port` for REST automation and
`server.port` for Spring Boot.

The `yaml.rest.automation` tells the system the location of the rest.yaml configuration file.

## Support of multiple configuration files

You can configure more than one location and the system will search and merge them sequentially.
The following example tells the system to merge the rest.yaml config files in the /tmp/config folder
and the project's resources folder.

```properties
yaml.rest.automation=file:/tmp/config/rest.yaml, classpath:/rest.yaml
```

## Duplicated REST endpoints

The system will detect duplicated REST endpoint configuation. If there is a duplicated entry, it will
abort the REST endpoint rendering. Your unit tests will fail because REST endpoints are not enabled.

The application log may look like this:
```
INFO - Loading config from classpath:/rest.yaml
INFO - Loading config from classpath:/event-api.yaml
ERROR - REST endpoint rendering aborted due to duplicated entry 'POST /api/event' in classpath:/event-api.yaml
```

Please correct the rest.yaml configuration files and rebuild your application again.

## Duplicated static content, cors and headers sections

When duplicated entry is detected, the subsequent one will replace the prior one. A warning will be
shown in the application log like this:

```
WARN - Duplicated 'static-content' in classpath:/duplicated-endpoint.yaml will override a prior one
WARN - Duplicated 'cors' in classpath:/duplicated-endpoint.yaml will override a prior one 'cors_1'
WARN - Duplicated 'headers' in classpath:/duplicated-endpoint.yaml will override a prior one 'header_1'
```

## Defining a REST endpoint

The "rest" section of the rest.yaml configuration file may contain one or more REST endpoints.

A REST endpoint may look like this:

```yaml
  - service: ["hello.world"]
    methods: ['GET', 'PUT', 'POST', 'HEAD', 'PATCH', 'DELETE']
    url: "/api/hello/world"
    timeout: 10s
    cors: cors_1
    headers: header_1
    authentication: 'v1.api.auth'
    tracing: true
```

*Syntax*

| Parameter      | Usage                                                              | Example                                                     |
|:---------------|:-------------------------------------------------------------------|:------------------------------------------------------------|
| service        | List of one or two route names of a service                        | 'hello.world'<br>['primary.service', 'secondary.service']   |
| methods        | List of one or two HTTP methods                                    | ['GET']                                                     |
| url            | URI path of the service                                            | '/api/hello/world'                                          |
| timeout        | Maximum time to wait for a REST response                           | Default value is '30s' for 30 seconds.<br>("s" for seconds) |
| cors           | Reference ID of a CORS section                                     | 'cors_1'                                                    |
| headers        | Reference ID of a HEADERS transformation section                   | 'header_1'                                                  |
| authentication | *Optional*. Route the HTTP request for authentication is provided. | default is false                                            |
| tracing        | Enable distributed tracing when set to 'true'                      | default is false                                            |

When more than one service route name is provided, the first one is the primary service and the system will
deliver its output as HTTP response. The second one is the secondary service for listening to the REST endpoint.
Output from the secondary service will be ignored.

When content length is not given, the system will render payload as a stream of bytes.

The "timeout" value is the maximum time that REST endpoint will wait for a response from your function.
If there is no response within the specified time interval, the user will receive an HTTP-408 timeout exception.

The "authentication" parameter is optional. If configured, the route name given in the authentication parameter
will be used. The input event will be delivered to the authentication function with the route name. In this example,
it is "v1.api.auth".

Your custom authentication function may look like this:
```java
@PreLoad(route = "v1.api.auth", instances = 10)
public class SimpleAuthentication implements TypedLambdaFunction<AsyncHttpRequest, Object> {

    @Override
    public Object handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        // Your authentication logic here. The return value should be true or false.
        return result;
    }
}
```

Your authentication function can return a boolean value to indicate if the request should be accepted or rejected.

If true, the system will send the HTTP request to the service. In this example, it is the "hello.world" function.
If false, the user will receive an "HTTP-401 Unauthorized" exception.

Optionally, you can use the authentication function to return some session information after authentication.
For example, your authentication can forward the "Authorization" header of the incoming HTTP request to your
organization's OAuth 2.0 Identity Provider for authentication.

To return session information to the next function, the authentication function can return an EventEnvelope.
It can set the session information as key-values in the response event headers.

In the lambda-example application, there is a demo authentication function in the AuthDemo class with the 
"v1.api.auth" route name. To demonstrate passing session information, the AuthDemo class set the header
"user=demo" in the result EventEnvelope.

You can test this by visiting http://127.0.0.1:8085/api/hello/generic/1 to invoke the "hello.generic" function.

The console will print:
```shell
DistributedTrace:55 - trace={path=GET /api/hello/generic/1, service=v1.api.auth, success=true,
  origin=20230326f84dd5f298b64be4901119ce8b6c18be, exec_time=0.056, start=2023-03-26T20:08:01.702Z, 
  from=http.request, id=aa983244cef7455cbada03c9c2132453, round_trip=1.347, status=200}
HelloGeneric:56 - Got session information {user=demo}
DistributedTrace:55 - trace={path=GET /api/hello/generic/1, service=hello.generic, success=true, 
  origin=20230326f84dd5f298b64be4901119ce8b6c18be, start=2023-03-26T20:08:01.704Z, exec_time=0.506, 
  from=v1.api.auth, id=aa983244cef7455cbada03c9c2132453, status=200}
DistributedTrace:55 - trace={path=GET /api/hello/generic/1, service=async.http.response, 
  success=true, origin=20230326f84dd5f298b64be4901119ce8b6c18be, start=2023-03-26T20:08:01.705Z, 
  exec_time=0.431, from=hello.generic, id=aa983244cef7455cbada03c9c2132453, status=200}
```

This illustrates that the HTTP request has been processed by the "v1.api.auth" function. The "hello.generic" function
is wired to the "/api/hello/generic/{id}" endpoint as follows:

```yaml
  - service: "hello.generic"
    methods: ['GET']
    url: "/api/hello/generic/{id}"
    # Turn on authentication pointing to the "v1.api.auth" function
    authentication: "v1.api.auth"
    timeout: 20s
    cors: cors_1
    headers: header_1
    tracing: true
```

The `tracing` parameter tells the system to turn on "distributed tracing". In the console log shown above, you see
three lines of log from "distributed trace" showing that the HTTP request is processed by "v1.api.auth" and 
"hello.generic" before returning result to the browser using the "async.http.response" function.

> *Note*: The "async.http.response" is a built-in function to send the HTTP response to the browser.
          The term "browser" also refers to a caller from an application ("client"). Therefore,
          browser and client can be used interchangeably.

The optional `cors` and `headers` sections point to the specific CORS and HEADERS sections respectively.

## CORS section

For ease of development, you can define CORS headers using the CORS section like this.

This is a convenient feature for development. For cloud native production system, it is most likely that 
CORS processing is done at the API gateway level.

You can define different sets of CORS headers using different IDs.

```yaml
cors:
  - id: cors_1
    options:
      - "Access-Control-Allow-Origin: ${api.origin:*}"
      - "Access-Control-Allow-Methods: GET, DELETE, PUT, POST, PATCH, OPTIONS"
      - "Access-Control-Allow-Headers: Origin, Authorization, X-Session-Id, X-Correlation-Id,
                                       Accept, Content-Type, X-Requested-With"
      - "Access-Control-Max-Age: 86400"
    headers:
      - "Access-Control-Allow-Origin: ${api.origin:*}"
      - "Access-Control-Allow-Methods: GET, DELETE, PUT, POST, PATCH, OPTIONS"
      - "Access-Control-Allow-Headers: Origin, Authorization, X-Session-Id, X-Correlation-Id, 
                                       Accept, Content-Type, X-Requested-With"
      - "Access-Control-Allow-Credentials: true"
```

## HEADERS section

The HEADERS section is used to do some simple transformation for HTTP request and response headers.

You can add, keep or drop headers for HTTP request and response. Sample HEADERS section is shown below.

```yaml
headers:
  - id: header_1
    request:
      #
      # headers to be inserted
      #    add: ["hello-world: nice"]
      #
      # keep and drop are mutually exclusive where keep has precedence over drop
      # i.e. when keep is not empty, it will drop all headers except those to be kept
      # when keep is empty and drop is not, it will drop only the headers in the drop list
      # e.g.
      # keep: ['x-session-id', 'user-agent']
      # drop: ['Upgrade-Insecure-Requests', 'cache-control', 'accept-encoding', 'connection']
      #
      drop: ['Upgrade-Insecure-Requests', 'cache-control', 'accept-encoding', 'connection']

    response:
      #
      # the system can filter the response headers set by a target service,
      # but it cannot remove any response headers set by the underlying servlet container.
      # However, you may override non-essential headers using the "add" directive.
      # i.e. don't touch essential headers such as content-length.
      #
      #     keep: ['only_this_header_and_drop_all']
      #     drop: ['drop_only_these_headers', 'another_drop_header']
      #
      #      add: ["server: mercury"]
      #
      # You may want to add cache-control to disable browser and CDN caching.
      # add: ["Cache-Control: no-cache, no-store", "Pragma: no-cache", 
      #       "Expires: Thu, 01 Jan 1970 00:00:00 GMT"]
      #
      add:
        - "Strict-Transport-Security: max-age=31536000"
        - "Cache-Control: no-cache, no-store"
        - "Pragma: no-cache"
        - "Expires: Thu, 01 Jan 1970 00:00:00 GMT"
```

## Static content

Static content (HTML/CSS/JS bundle), if any, can be placed in the "resources/public" folder in your
application project root. It is because the default value for the "static.html.folder" parameter
in the application configuration is "classpath:/resources/public". If you want to place your
static content elsewhere, you may adjust this parameter. You may point it to the local file system
such as "file:/tmp/html".

For security reason, you may add the following configuration in the rest.yaml.
The following example is shown in the unit test section of the platform-core library module.

```yaml
#
# Optional static content handling for HTML/CSS/JS bundle
# -------------------------------------------------------
#
# no-cache-pages - tells the browser not to cache some specific pages
#
# The "filter" section is a programmatic way to protect certain static content.
#
# The filter can be used to inspect HTTP path, headers and query parameters.
# The typical use case is to check cookies and perform browser redirection
# for SSO login. Another use case is to selectively add security HTTP
# response headers such as cache control and X-Frame-Options. You can also
# perform HTTP to HTTPS redirection.
#
# Syntax for the "no-cache-pages", "path" and "exclusion" parameters are:
# 1. Exact match - complete path
# 2. Match "startsWith" - use a single "*" as the suffix
# 3. Match "endsWith" - use a single "*" as the prefix
#
# If filter is configured, the path and service parameters are mandatory
# and the exclusion parameter is optional.
#
# In the following example, it will intercept the home page, all contents
# under "/assets/" and any files with extensions ".html" and ".js".
# It will ignore all CSS files.
#
static-content:
  no-cache-pages: ["/", "/index.html"]
  filter:
    path: ["/", "/assets/*", "*.html", "*.js"]
    exclusion: ["*.css"]
    service: "http.request.filter"
```

The sample request filter function is available in the platform-core project like this:

```java
@PreLoad(route="http.request.filter", instances=100)
public class GetRequestFilter implements TypedLambdaFunction<AsyncHttpRequest, EventEnvelope> {

    @Override
    public EventEnvelope handleEvent(Map<String, String> headers, AsyncHttpRequest input, int instance) {
        return new EventEnvelope().setHeader("x-filter", "demo");
    }
}
```

In the above http.request.filter, it adds a HTTP response header "X-Filter" for the unit test
to validate.

If you set status code in the return EventEnvelope to 302 and add a header "Location", the system
will redirect the browser/client to the given URL in the location header. Please be careful to avoid
HTTP redirection loop.

Similarly, you can throw exception and the HTTP request will be rejected with the given status
code and error message accordingly.
<br/>

|                   Chapter-2                   |                   Home                    |              Chapter-4              |
|:---------------------------------------------:|:-----------------------------------------:|:-----------------------------------:|
| [Function Execution Strategies](CHAPTER-2.md) | [Table of Contents](TABLE-OF-CONTENTS.md) | [Event Script Syntax](CHAPTER-4.md) |
