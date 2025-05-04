# Application Configuration

The following parameters are used by the system. You can define them in either the application.properties or
application.yml file.

When you use both application.properties and application.yml, the parameters in application.properties will take
precedence.

| Key                                    | Value (example)                                                 | Required    |
|:---------------------------------------|:----------------------------------------------------------------|:------------|
| application.name                       | Application name                                                | Yes         |
| spring.application.name                | Alias for application name                                      | Yes*1       |
| info.app.version                       | major.minor.build (e.g. 1.0.0)                                  | Yes         |
| info.app.description                   | Something about your application                                | Yes         |
| web.component.scan                     | your own package path or parent path                            | Yes         |
| server.port                            | e.g. 8083                                                       | Yes*1       |
| rest.server.port                       | e.g. 8085                                                       | Optional    |
| websocket.server.port                  | Alias for rest.server.port                                      | Optional    |
| rest.automation                        | true if you want to enable automation                           | Optional    |
| yaml.rest.automation                   | Config location e.g. classpath:/rest.yaml                       | Optional    |
| yaml.event.over.http                   | Config location classpath:/event-over-http.yaml                 | Optional    |
| yaml.multicast                         | Config location classpath:/multicast.yaml                       | Optional    |
| yaml.journal                           | Config location classpath:/journal.yaml                         | Optional    |
| yaml.route.substitution                | Config location                                                 | Optional    |
| yaml.topic.substitution                | Config location                                                 | Optional    |
| yaml.cron                              | Config location                                                 | Optional    |
| yaml.flow.automation                   | Config location. e.g. classpath:/flows.yaml                     | EventScript |
| static.html.folder                     | classpath:/public/                                              | Yes         |
| spring.web.resources.static-locations  | (alias for static.html.folder)                                  | Yes*1       |
| mime.types                             | Map of file extensions to MIME types<br/>(application.yml only) | Optional    |
| spring.mvc.static-path-pattern         | /**                                                             | Yes*1       |
| show.env.variables                     | comma separated list of variable names                          | Optional    |
| show.application.properties            | comma separated list of property names                          | Optional    |
| cloud.connector                        | kafka, none, etc.                                               | Optional    |
| cloud.services                         | e.g. some.interesting.service                                   | Optional    |
| snake.case.serialization               | true (recommended)                                              | Optional    |
| trace.http.header                      | comma separated list. Default "X-Trace-Id"                      | Optional    |
| hsts.feature                           | default is true                                                 | Optional*   |
| protect.info.endpoints                 | true to disable actuators. Default: true                        | Optional*   |
| application.feature.route.substitution | default is false                                                | Optional    |
| application.feature.topic.substitution | default is false                                                | Optional    |
| kafka.replication.factor               | 3                                                               | Kafka       |
| cloud.client.properties                | e.g. classpath:/kafka.properties                                | Connector   |
| user.cloud.client.properties           | e.g. classpath:/second-kafka.properties                         | Connector   |
| default.app.group.id                   | groupId for the app instance.<br/>Default: appGroup             | Connector   |
| default.monitor.group.id               | groupId for the presence-monitor.<br/>Default: monitorGroup     | Connector   |
| monitor.topic                          | topic for the presence-monitor.<br/>Default: service.monitor    | Connector   |
| app.topic.prefix                       | Default: multiplex (DO NOT change)                              | Connector   |
| app.partitions.per.topic               | Max Kafka partitions per topic.<br/>Default: 32                 | Connector   |
| max.virtual.topics                     | Max virtual topics = partitions * topics.<br/> Default: 288     | Connector   |
| max.closed.user.groups                 | Number of closed user groups. <br/>Default: 10, range: 3 - 30   | Connector   |
| closed.user.group                      | Closed user group. Default: 1                                   | Connector   |
| transient.data.store                   | Default is "/tmp/reactive"                                      | Optional    |
| running.in.cloud                       | Default is false (set to true if containerized)                 | Optional    |
| deferred.commit.log                    | Default is false (for unit tests only)                          | Optional    |
| kernel.thread.pool                     | Default 100. Not more than 200.                                 | Optional    |
| modules.autostart                      | list of composable functions to start                           | Optional    |
| spring.boot.main                       | Default "org.platformlambda.rest.RestServer"                    | Spring Boot |

`*` - applies to the "rest-spring" library only

## Base configuration files

By default, the system assumes the following application configuration files:

1. application.properties
2. application.yml

The bootstrap.properties and bootstrap.yml are optional.

You can change this behavior by adding the `app-config-reader.yml` in your project's `resources` folder.
The default configuration is shown as below.

```yaml
resources:
  - classpath:/bootstrap.properties
  - classpath:/bootstrap.yml
  - classpath:/application.properties
  - classpath:/application.yml

profiles: 'classpath:/application-'
```

For compatibility with Spring Boot configuration system, keep the default bootstrap and application
configuration files. You may add more configuration files as needed.

To load configuration file from the local file system, use "file:/" instead of "classpath:/".

The "profiles" parameter defines the file prefix to load profile related configuration files.

> *Note*: The order of the filenames defines the loading sequence where subsequent configuration
          parameters will override prior ones.

## Partial support of Spring Active Profiles

With JVM runtime parameter "-Dspring.profiles.active" or environment variable
"SPRING_PROFILES_ACTIVE", the AppConfigReader will try to load the additional configuration files.

For example, if "spring.profiles.active=dev", the system will load "application-dev.properties"
and "application-dev.yml" accordingly.

When more than one active profile is needed, you can use a comma separated list of profiles in
"spring.profiles.active".

For Spring Boot compatibility, the filename prefix "application-" is fixed. This is defined
in the app-config-reader.yml file above.

## Special handling for PROPERTIES file

Since application.properties and application.yml can be used together, 
the system must enforce keyspace uniqueness because YAML keyspaces are hierarchical.

For example, if you have x.y and x.y.z, x.y is the parent of x.y.z.

Therefore, you cannot set a value for the parent key since the parent is a key-value container.

This hierarchical rule is enforced for PROPERTIES files.
If you have x.y=3 and x.y.z=2 in the same PROPERTIES file, x.y will become a parent of x.y.z and its intended
value of 3 will be lost.

## Optional Service

The `OptionalService` annotation may be used with the following class annotations:

1. BeforeApplication
2. MainApplication
3. PreLoad
4. WebSocketService

When the OptionalService annotation is available, the system will evaluate the annotation value as a
conditional statement where it supports one or more simple condition using a key-value in the application
configuration.

For examples:

OptionalService("rest.automation") - the class will be loaded when rest.automation=true

OptionalService("!rest.automation") - the class will be loaded when rest.automation is false or non-exist

OptionalService("interesting.key=100") - the system will load the class when "interesting.key" is set to 100
in application configuration.

To specify more than one condition, use a comma separated list as the value like this:
OptionalService("web.socket.enabled, rest.automation") - this tells the system to load the class when 
either web.socket.enabled or rest.automation is true.

## Static HTML contents

You can place static HTML files (e.g. the HTML bundle for a UI program) in the "resources/public" folder or
in the local file system using the "static.html.folder" parameter.

## MIME types

The system supports a bare minimal list of file extensions to MIME types in the `mime-types.yml` configuration
file in the platform-core's resources folder. If your use case requires additional MIME type mapping, you may
define them in the `application.yml` configuration file under the `mime.types` section like this:

```yaml
mime.types:
  pdf: 'application/pdf'
  doc: 'application/msword'
```

> *Note*: application.properties file cannot be used for the "mime.types" section because it only supports text
  key-values.

You may also provide a mime.types section in the `mime-types.yml` configuration under the resources folder
to override the default configuration in the platform-core library.

## Custom content types

If you use custom content types in your application, you may add the following section in the application.yml
configuration file. For example,

```yaml
custom.content.types:
  - 'application/vnd.my.org-v2.0+json -> application/json'
  - 'application/vnd.my.org-v2.1+xml -> application/xml'
```

In the "custom.content.types" section, you can configure a list of content-type mappings.
The left-hand-side is the custom content-type and the right-hand-side is a standard content-type.

The content-type mapping tells the system to treat the custom content type as if it is the standard content
type.

In the above example, the HTTP payload with the custom content type "application/vnd.my.org-v2.0+json" is
treated as a regular JSON content.

If you want to put the custom content types in a separate configuration file, please put them in a file named
`custom-content-types.yml` under your application `resources` folder.

## HTTP and websocket port assignment

If `rest.automation=true` and `rest.server.port or server.port` are configured, the system will start
a lightweight non-blocking HTTP server. If `rest.server.port` is not available, it will fall back to `server.port`.

If `rest.automation=false` and you have a websocket server endpoint annotated as `WebsocketService`, the system
will start a non-blocking Websocket server with a minimalist HTTP server that provides actuator services.
If `websocket.server.port` is not available, it will fall back to `rest.server.port` or `server.port`.

If you add Spring Boot dependency, Spring Boot will use `server.port` to start Tomcat or similar HTTP server.

The built-in lightweight non-blocking HTTP server and Spring Boot can co-exist when you configure
`rest.server.port` and `server.port` to use different ports.

Note that the `websocket.server.port` parameter is an alias of `rest.server.port`.

## Transient data store

The system handles back-pressure automatically by overflowing events from memory to a transient data store. 
As a cloud native best practice, the folder must be under "/tmp". The default is "/tmp/reactive". 
The "running.in.cloud" parameter must be set to false when your apps are running in IDE or in your laptop. 
When running in kubernetes, it can be set to true.

## Snake or Camel case serializers

Serialization and de-serialization of events are performed automatically.

If there is a genuine need to programmatically perform serialization, you may use the pre-configured serializer 
so that the serialization behavior is consistent.

You can get an instance of the serializer with `SimpleMapper.getInstance().getMapper()`.

The serializer may perform snake case or camel serialization depending on the parameter `snake.case.serialization`.

If you want to ensure snake case or camel, you can select the serializer like this:

```text
SimpleObjectMapper snakeCaseMapper = SimpleMapper.getInstance().getSnakeCaseMapper();
SimpleObjectMapper camelCaseMapper = SimpleMapper.getInstance().getCamelCaseMapper();
```

## The trace.http.header parameter

The `trace.http.header` parameter sets the HTTP header for trace ID. When configured with more than one label,
the system will retrieve trace ID from the corresponding HTTP header and propagate it through the transaction
that may be served by multiple services.

If trace ID is presented in an HTTP request, the system will use the same label to set HTTP response traceId header.

```yaml
X-Trace-Id: a9a4e1ec-1663-4c52-b4c3-7b34b3e33697
or
X-Correlation-Id: a9a4e1ec-1663-4c52-b4c3-7b34b3e33697
```

## Kafka specific configuration

If you use the kafka-connector (cloud connector) and kafka-presence (presence monitor), you may want to 
externalize kafka.properties like this:

```properties
cloud.client.properties=file:/tmp/config/kafka.properties
```

Note that "classpath" refers to embedded config file in the "resources" folder in your source code and "file" 
refers to an external config file.

You want also use the embedded config file as a backup like this:

```properties
cloud.client.properties=file:/tmp/config/kafka.properties, classpath:/kafka.properties
```

## Distributed trace

To enable distributed trace logging, please set this in log4j2.xml:

```text
<logger name="org.platformlambda.core.services.DistributedTrace" level="INFO" />
```

## Built-in XML serializer

The platform-core includes built-in serializers for JSON and XML in the AsyncHttpClient and
Spring RestController. The XML serializer is designed for simple use cases. If you need to handle more
complex XML data structure, you can disable the built-in XML serializer by adding the following HTTP
request header.

```properties
X-Raw-Xml=true
```

<br/>

|          Chapter-9           |                   Home                    |                 Appendix-II                  |
|:----------------------------:|:-----------------------------------------:|:--------------------------------------------:|
| [API Overview](CHAPTER-9.md) | [Table of Contents](TABLE-OF-CONTENTS.md) | [Reserved names and headers](APPENDIX-II.md) |
