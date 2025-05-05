# Changelog

## Release notes

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> *Note*: Some version numbers may be skipped to align feature set with the Node.js version.

---
## Version 4.2.39, 5/4/2025

### Added

1. Code example to illustrate how to write your own Flow Adapters
2. "modules.autostart" feature to invoke composable functions and libaries at start up.

### Removed

N/A

### Changed

N/A

---
## Version 4.2.38, 4/30/2025

### Added

Spring auto-wiring of composable functions when leveraging the rest-spring-3 module

### Removed

N/A

### Changed

N/A

---
## Version 4.2.37, 4/29/2025

### Added

N/A

### Removed

N/A

### Changed

Windows compatibility in static file handling - convert Windows file path to Unix path

---
## Version 4.2.35, 4/24/2025

### Added

N/A

### Removed

N/A

### Changed

Bugfix for pipeline that contains only one task

---
## Version 4.2.33, 4/24/2025

### Added

N/A

### Removed

Removed SimpleScheduler from the build script at root because SimpleScheduler has not reached
production quality

### Changed

1. Rename variable as per SonarCube quality gate
2. OSS version update - revert Kafka client to version 3.9.0 for compatibility with Confluent Kafka

---
## Version 4.2.32, 4/24/2025

### Added

N/A

### Removed

N/A

### Changed

1. Improvement in custom content-type resolver
2. OSS version update

```
vertx 4.5.14
guava 33.4.8-jre 
junit5-bom 5.12.2
kotlin 2.1.20
Kafka client 4.0.0
gson 2.13.0
```
---
## Version 4.2.31, 4/23/2025

### Added

N/A

### Removed

N/A

### Changed

Minor update to address code smells reported by SonarCube analyzer

---
## Version 4.2.30, 4/22/2025

### Added

1. Support "matrix parameters" and "hash parameters" in HTTP request URI in platform-core
2. "classpath" in LHS of output data mapping for event script

### Removed

N/A

### Changed

N/A

---
## Version 4.2.29, 4/20/2025

### Added

Perform "path traversal" avoidance when decoding incoming HTTP requests

### Removed

Remove "public" qualifier from unit tests since JUnit version 5 does not need it

### Changed

1. Minor refactoring to remove majority of code smells as per SonarQube static code analysis
2. Support custom error message in EventEnvelope

---
## Version 4.2.28, 4/17/2025

### Added

Unit test and updated developer guide to illustate use of AsyncHttpClient in Event Script.

### Removed

1. default application.properties files in platform-core, rest-spring-3 and event-script-engine
2. HTML escape characters in URI path handling in AsyncHttpClient

### Changed

N/A

---
## Version 4.2.27, 3/31/2025

### Added

N/A

### Removed

N/A

### Changed

1. Streamline error handling in TaskExecutor to sync up with Node.js version
2. Update developer guide's chapter-4 for the output data mapping paragraph about file

---
## Version 4.2.26, 3/25/2025

### Added

N/A

### Removed

N/A

### Changed

Use EventInterceptor pattern for the resilience handler for non-blocking deferred response

---
## Version 4.2.25, 3/24/2025

### Added

N/A

### Removed

N/A

### Changed

Rename "alternate" parameter in resilience handler to "alternative"

---
## Version 4.2.24, 3/24/2025

### Added

Generic resilience handler with alternative path and backoff features

### Removed

N/A

### Changed

1. The getError() method in EventEnvelope is updated to return encoded error message.
   This is required for distributed trace processing and proper error handling of subflows.
2. Delete file when mapping a null value from the LHS to the RHS that is defined as a file,
   thus allowing clearing of temporary data files in a flow.
3. OSS update - spring boot parent version 3.4.4 and io.project.reactor bom version 2024.0.4

---
## Version 4.2.23, 3/12/2025

### Added

N/A

### Removed

N/A

### Changed

For security, the parent state machine (namespace "model.parent") is a protected resource.
It can only be shared by the primary flow and all sub-flow instances that are instantiated from it.

---
## Version 4.2.22, 3/11/2025

### Added

N/A

### Removed

N/A

### Changed

All sub-flows instantiated from a primary flow can access the same parent state machine
using the "model.parent" namespace.

---
## Version 4.2.21, 3/8/2025

### Added

1. Support flow and function for external state machine
2. Parent state machine for sub-flow
3. Validation rules to reject access to the whole model or parent namespace

### Removed

N/A

### Changed

N/A

---
## Version 4.2.20, 2/28/2025

### Added

"spring.boot.main=org.platformlambda.rest.RestServer" added to application.properties
so that developer may override it with their own Spring Boot initializer.

### Removed

property setting for Netty version 4.1.118.Final is no longer required in pom.xml
because the updated spring boot parent version 3.4.3 will fetch 4.1.118 correctly.

### Changed

upgrade spring boot version 3.4.3

---
## Version 4.2.19, 2/26/2025

### Added

N/A

### Removed

N/A

### Changed

Allow developer to load base configuration files from the classpath or from the
local file system.

---
## Version 4.2.18, 2/21/2025

### Added

1. java.sql.Timestamp data type added to SimpleMapper
2. simple type matching feature is extended with a new string 'concat' method
3. default REST endpoints for /api/event and actuator services

### Removed

N/A

### Changed

1. Sort REST endpoints for orderly loading
2. Drop "async.http.request" RPC traces to reduce observability noise

---
## Version 4.2.17, 2/20/2025

### Added

LocalDate and LocalTime data type added to SimpleMapper

### Removed

N/A

### Changed

N/A

---
## Version 4.2.15, 2/15/2025

### Added

N/A

### Removed

N/A

### Changed

Update actuator output data structures to be consistent with Composable Node.js implementation

---
## Version 4.2.14, 2/14/2025

### Added

N/A

### Removed

N/A

### Changed

Use different route names for various actuator services to avoid hardcode of URLs

---
## Version 4.2.13, 2/13/2025

### Added

Actuator REST endpoints are now configurable in rest.yaml

### Removed

The feature to shutdown, suspend and resume of an application instance is retired

### Changed

Update actuator services to serve REST requests directly

---
## Version 4.2.12, 2/12/2025

### Added

N/A

### Removed

N/A

### Changed

1. Use ServerCookieEncoder.STRICT.encode() method to detect invalid cookie value
2. Update vertx to 4.5.13 and Netty to 4.1.118.Final to address security vulnerabilities

---
## Version 4.2.11, 2/11/2025

### Added

Support of Spring active profiles using JVM parameter "-Dspring.profiles.active" or
environment variable "SPRING_PROFILES_ACTIVE"

### Removed

N/A

### Changed

Developer Guide's Appendix-I updated for the Spring active profile feature

---
## Version 4.2.10, 2/10/2025

### Added

1. PoJo class hint and custom serializer in FluxConsumer
2. Optional custom serializer in FluxPublisher

### Removed

N/A

### Changed

1. WorkerHandler and WorkerQueue classes for FluxConsumer
2. Developer Guide's Chapter 2
3. Flow diagrams for sample app in Developer Guide

---
## Version 4.2.9, 2/8/2025

### Added

1. uuid generation feature in Event Script's simple type matching
2. inputPoJoClass parameter in PreLoad annotation

### Removed

N/A

### Changed

For feature completeness, the system now supports list of pojo as one type of event input
for sending event programmatically.

However, Event Script's input data mapping is configuration driven and thus list of pojo
is not permitted.

---
## Version 4.2.8, 2/6/2025

In this release, we have improved EventEnvelope and Trace Annotation features
and tested interoperability with applications written in version 2 and 3.

### Added

Support text, map and list in trace annotation

### Removed

The "extra" and "end-of-route" fields in EventEnvelope are retired

### Changed

Updated EventEnvelope data structure for direct support of tags and annotations

---
## Version 4.2.7, 2/4/2025

### Added

N/A

### Removed

redundant and inconsistent log4j2.xml config files from subprojects

### Changed

1. getBodyAsListOfPoJo method in EventEnvelope updated
2. endFlow method of TaskExecutor sends event to distributed trace instead of logging

---
## Version 4.2.6, 2/3/2025

### Added

N/A

### Removed

N/A

### Changed

Observability bugfix to set execution time.

In earlier iteration, input events to the PostOffice are set to be immutable.
However, the execution time in a reply event was missing in the cloned event.

---
## Version 4.2.5, 2/2/2025

### Added

Add 3-part syntax for Event Script's data mapping processing.

Supports the following data mapping syntax:

1. LHS -> RHS
2. LHS -> model.variable -> RHS

### Removed

N/A

### Changed

1. Make input event immutable to PostOffice's send and request API
2. Consistent temporary stream folder name for Java and Node.js under /tmp/composable

---
## Version 4.2.4, 1/30/2025

### Added

Temporary Inbox handler for optimization of request-response event processing

### Removed

FastRPC API removed from the Kotlin subsystem since Kotlin can use Mono/Flux for reactive
programming

### Changed

1. Developer guide updated accordingly
2. Essential services are started as the first "BeforeApplication" using sequence 0
3. Event Script runs as the second "BeforeApplication" using sequence 2
4. BeforeApplication sequence 1 is reserved to handle rare use case that a module must run before event script

---
## Version 4.2.3, 1/28/2025

### Added

Support of negate operator of a model value in event script added to the "simple type matching" feature

### Removed

N/A

### Changed

1. Use virtual thread to decouple JSON logging to optimize application performance.
2. Updated the following OSS versions

- Spring Boot version 3.4.2
- Reactor-bom 2024.0.2
- Vertx-core 4.5.12
- MsgPack 0.9.9

---
## Version 4.2.2, 1/22/2025

### Added

N/A

### Removed

N/A

### Changed

Simplify JSON logging using the parameter formatter `{}` with a map parameter.
The map will be printed as JSON when log.format=json is set in application.properties.

This also slightly improves logging performance.

---
## Version 4.2.1, 1/21/2025

### Added

N/A

### Removed

N/A

### Changed

reconfigure logger to json or compact format early when app starts

---
## Version 4.2.0, 1/20/2025

This is a milestone release for consistent features and behaviors between
Java and Node.js versions

### Added

Composable methodology in developer guide

### Removed

URI path from errorPage.html

### Changed

N/A

---
## Version 4.1.8, 1/17/2025

### Added

Notice in Javadoc to identify system reserved classes

### Removed

N/A

### Changed

log.format parameter changed to be case insensitive

---
## Version 4.1.7, 1/16/2025

### Added

Limit stack trace transport to a max of 10 lines for transport efficiency because
the original stack trace can be retrieved with the getException() method

Flow tests for composable-example application

### Removed

N/A

### Changed

1. Update composable-example to be consistent with Node.js version
2. Change distributed trace to log as pretty print JSON
3. Clean up log4j XML configuration templates in platform-core to be used as examples

---
## Version 4.1.6, 1/15/2025

### Added

Support 3 logging formats using the log.format parameter in application.properties

```text
text - default to text string output
compact - json output without linefeed for redirection to log analytics system
json - pretty print for human readers
```

### Removed

N/A

### Changed

N/A

---
## Version 4.1.5, 1/14/2025

### Added

N/A

### Removed

Custom Map/List deserializers

### Changed

Apply ToNumberPolicy.LONG_OR_DOUBLE to GSON serialization of untyped numbers
in hash map.

---
## Version 4.1.4, 1/13/2025

### Added

1. Task list included in "end of flow" logging
2. Pipeline-while-loop unit tests
3. EventScriptMock helper class to override a function route for a flow task

### Removed

Removed the "threshold" feature in variable HTTP payload in REST automation for
consistent syntax with Node.js Composable version

### Changed

1. Update the setException method in EventEnvelope to handle non-serializable exception
2. Improved event script's pipeline for-loop-continue feature
3. Normalize dataset when loading new configuration using ConfigReader

---
## Version 4.1.3, 1/1/2025

### Added

N/A

### Removed

N/A

### Changed

1. Improved configuration management and refactored AppConfigReader, ConfigReader and
   MultiLevelMap classes
2. Input to MultiLevelMap is now immutable
3. Simplified event script's pipeline condition syntax
4. Consistent exception transport for Java and Node.js composable applications
5. Bugfix to handle untyped map inside a PoJo
6. OSS updates as follows.

```
Spring Boot parent version 3.4.1
Kotlin version 2.1.0
Spring Project Reactor version 3.7.1 (BOM version 2024.0.1)
Google Guava version 33.4.0-jre
JUnit version 5.11.4
```

---
## Version 4.1.2, 12/20/2024

### Added

Unit test in event-script-engine to validate that the config management system can merge
parameters in application.yml and application.properties.

### Removed

N/A

### Changed

N/A

---
## Version 4.1.1, 12/18/2024

### Added

1. "map" constant type in input data mapping
2. AppConfigReader will resolve key-values from system properties and environment variables at startup

### Removed

N/A

### Changed

Updated Chapter-4 for the new "map" constant feature.

---
## Version 4.1.0, 12/11/2024

This milestone version achieves ideal event choreography by removing additional event routing
to and from the Event Manager. This would boost internal event routing performance by 50 percent.

### Added

Performance optimization for Event Script

### Removed

N/A

### Changed

The platform-core module uses virtual threads to execute event.script.manager and task.executor
directly to eliminate additional serialization overheads since the two functions are event routers
themselves.

---
## Version 4.0.33, 12/11/2024

### Added

Support of custom content types in application.yml

### Removed

N/A

### Changed

1. Improved websocket housekeeping logic
2. Use bench.add to replace bench.offer API

---
## Version 4.0.32, 12/9/2024

### Added

1. For completeness, added Boolean AND and OR operations for simple type matching.
2. Added traceId as metadata for a flow instance

### Removed

N/A

### Changed

1. Update Chapter-4 for the new AND/OR type matching feature
2. Consistent custom HTTP headers for event over http protocol and streaming content

---
## Version 4.0.31, 12/5/2024

### Added

N/A

### Removed

N/A

### Changed

1. The "keep.original" key is renamed as "keep-original" to comply with convention.
2. Continue processing if some preload override config files are missing.

---
## Version 4.0.30, 12/5/2024

### Added

Implemented unique task naming feature for event flow configuration.

### Removed

N/A

### Changed

1. The "keep_original" key is renamed as "keep.original" in preload override
2. Chapter-4 of developer guide updated with the new task alias feature

---
## Version 4.0.29, 12/3/2024

### Added

Added integer, long, float, double and boolean type matching for state machine.

### Removed

N/A

### Changed

N/A

---
## Version 4.0.28, 11/29/2024

### Added

1. Support for simple data type matching processing (text, substring, binary and b64)
2. Optional external state machine

### Removed

Removed "http.input." and "http.output." aliases from event script. Instead, use the
generic "input." and "output." namespaces.

### Changed

1. Bugfix for AsyncHttpClient to allow missing HTTP request body in POST, PUT or PATCH request
2. Mono reactive flow control

---
## Version 4.0.27, 11/27/2024

### Added

1. Support for Mono/Flux return type for KotlinLambdaFunction
2. Implemented Websocket handshake handler to adjust to API changes in vertx 4.5.11

### Removed

N/A

### Changed

N/A

---
## Version 4.0.26, 11/26/2024

### Added

N/A

### Removed

Remove pom.xml version override for netty and spring framework because
Spring Boot 3.4.0 fetches the correct versions of netty and spring framework.

Earlier override was done to avoid security vulnerabilities of older versions
of netty and spring framework.

### Changed

1. Handle the case that Mono will not return payload if the payload is null
2. OSS update: Classgraph 4.8.179, Vertx 4.5.11, Spring Boot 3.4.0, Kafka Client 3.9.0

---
## Version 4.0.25, 11/21/2024

### Added

Support more than one REST configuration files.

When a duplicated REST entry is detected, the system will abort REST endpoint rendering
and print out an error message in application log.

If you have unit tests to cover the REST endpoints, the unit tests will fail accordingly.

### Removed

N/A

### Changed

Improved environment variable parsing in config reader. System will skip entries with
invalid environment variable reference syntax.

---
## Version 4.0.24, 11/20/2024

### Added

N/A

### Removed

N/A

### Changed

Bugfix for an edge case in config reader to handle control character of brackets inside
an environment variable reference.

e.g. some.key=${ENV_VAR:something/{test1}/{test2}}

---
## Version 4.0.23, 11/19/2024

### Added

N/A

### Removed

ObjectStreamWriter and AsyncObjectStreamReader are removed

### Changed

1. Replace ObjectStreamWriter with FluxPublisher
2. Replace AsyncObjectStreamReader with FluxConsumer
3. Bugfix for FluxConsumer expiry - change type from "data" to "exception".

---
## Version 4.0.22, 11/18/2024

### Added

FluxPublisher and FluxConsumer for integration with Flux reactive response object

### Removed

N/A

### Changed

1. Unit tests in event streaming and post office to support Flux integration
2. Select reactor-core version 3.7.0 using dependency management (reactor-bom version 2024.0.0)

---
## Version 4.0.21, 11/14/2024

### Added

Support for user function to return a Mono reactive response object

### Removed

N/A

### Changed

1. Update netty to version 4.1.115.Final to address security vulnerability in 4.1.114
2. Move reactor-core library from rest-spring-3 to platform-core

---
## Version 4.0.20, 11/13/2024

### Added

For ease of configuration, added "com.accenture" to the base packages so that user applications
do not need to include it to use the event-script-engine module.

### Removed

if-then-else pipeline feature in event-script

### Changed

1. Update Event Script syntax for consistency
2. Fix error in counting number of compiled flows

---
## Version 4.0.16, 11/10/2024

### Added

Generate unique flow instance ID as reference during flow execution.

### Removed

N/A

### Changed

Save the original correlation-ID from the calling party in a flow instance and
return this value to the calling party at the end of flow execution.

---
## Version 4.0.15, 11/7/2024

### Added

N/A

### Removed

N/A

### Changed

renamed StartFlow to FlowExecutor

---
## Version 4.0.14, 11/7/2024

### Added

N/A

### Removed

N/A

### Changed

1. Health check function can return either a text string or a Map
2. StartFlow API updates

---
## Version 4.0.13, 11/5/2024

### Added

Added helper class "StartFlow" to start a flow, including internal flows without HTTP or Kafka.

### Removed

N/A

### Changed

1. Bugfix for empty YAML file to avoid null pointer exception
2. Sort event scripts for orderly logging in the CompileFlows validation process

---
## Version 4.0.12, 10/31/2024

### Added

New feature to support resolution of more than one environment variable for a parameter
using the ConfigReader

### Removed

N/A

### Changed

Update OSS modules
1. classgraph version 4.8.177
2. kotlin version 2.0.21
3. guava version 33.3.1-jre
4. jUnit version 5 jupiter

Adjusted all unit tests to use jUnit 5

---
## Version 4.0.11, 10/28/2024

### Added

New features to support:
1. multiple preload override config file
2. multiple flow list config files

### Removed

1. unused class "UnauthorizedObj" in platform-core
2. commons-io dependency in Kafka-Standalone subproject

### Changed

1. Unit test for the preload override feature
2. JavaDoc for the MainApplication

---
## Version 4.0.10, 10/24/2024

### Added

N/A

### Removed

N/A

### Changed

1. OSS update - Spring Boot 3.3.5
2. Security patch for CR/LF exploit for HTTP cookie

---
## Version 4.0.9, 10/18/2024

### Added

Added Kafka Raft for the Kafka-standalone app.

### Removed

Removed zookeeper from Kafka-standalone app.

### Changed

Update spring framework verison 6.1.14 to avoid vulnerability in webflux

---
## Version 4.0.8, 10/9/2024

### Added

1. Partial support of Active Profile using the "spring.profiles.active" parameter
2. Hierarchy of flows

### Removed

N/A

### Changed

N/A

---
## Version 4.0.7, 10/1/2024

### Added

A generic "no-op" function for use in event scripts.

### Removed

Feature to ping a function without payload and headers.

### Changed

Simplified api-playground application

---
## Version 4.0.6, 9/27/2024

### Added

1. HTTP request Cookie value filtering using RFC-6265 strict syntax

### Removed

1. Automatic index page redirection filter for Spring Boot

### Changed

1. Upgrade SHA-1 to SHA-512 algorithm in CryptoAPI utility
2. Fix security vulnerability associated with HTTP request header and cookie manipulation

---
## Version 4.0.5, 9/24/2024

### Added

N/A

### Removed

1. Feature for automatic PoJo transport in EventEnvelope and MsgPack
2. Feature for safe.data.model deserialization
3. Benchmark-server is no longer required

### Changed

1. Update OSS versions - vertx 4.5.10, kotlin 2.0.20, spring boot 3.3.4

---

## Version 4.0.4, 9/5/2024

### Added

New feature for AsyncHttpClient to render small streaming HTTP response (i.e. chunked binary data) as byte array.

For details, Please refer to [Appendix III, Developer Guide](https://accenture.github.io/mercury-composable/guides/APPENDIX-III/)

### Removed

N/A

### Changed

Bugfix for parsing default value of environment variable in ConfigReader. 
This resolves an issue when the special character colon (":") is used more than once in the default value.

---
## Version 4.0.3, 9/4/2024

### Added

The "preload override" feature is added. This allows overriding a reusable composable library with a set of new
route names that are unique for use in an event flow configuration script.

For details, Please refer to [Chapter 4, Developer Guide](https://accenture.github.io/mercury-composable/guides/CHAPTER-4/)

### Removed

N/A

### Changed

N/A

---
## Version 4.0.2, 8/31/2024

### Added

1. New "classpath" namespace for input data mapping
2. Support for input data mapping to handle subset of input request body as a Map or PoJo

### Removed

N/A

### Changed

1. Remove the class "type" variable from AsyncHttpRequest
2. Improve the "removeElement" method in MultiLevelMap
3. Make HTTP input request header labels key-insensitive
4. Update Spring Boot to version 3.3.3

---
## Version 4.0.1, 8/19/2024

### Added

new File read/write feature in Event Script's I/O data mapping

### Removed

N/A

### Changed

1. Update Spring Boot to version 3.3.2
2. Update Guava to version 33.3.0-jre
3. Update Vertx to version 4.5.9
4. Update Kotlin to version 2.0.10
5. Change "upstream" to "dependency" in the "/health" endpoint

---
## Version 4.0.0, 6/24/2024

This version merges Event Script into the Mercury Composable repository.

### Added

N/A

### Removed

N/A

### Changed

1. Update Spring Boot to version 3.3.1
2. Update Guava to version 33.2.1-jre
3. Update Vertx to version 4.5.8
4. Update Kotlin to version 2.0.0
5. Update classgraph to version 4.8.174
6. Optional reply event for a flow configuration

> Kafka-standalone is still using Spring Boot 3.2.5 due to compatibility issue

---
## Version 3.1.5, 5/1/2024

This version supercedes 3.1.4 due to updated data structure
for static content handling.

### Added

1. Added optional static-content.no-cache-pages in rest.yaml
2. AsyncHttpClientLoader

### Removed

N/A

### Changed

1. Updated data structure for static-content section in rest.yaml
2. Fixed bug for setting multiple HTTP cookies
3. Unified configuration file prefix "yaml."

---
## Version 3.1.4, 4/28/2024

### Added

Added optional static content HTTP-GET request filter in rest.yaml

### Removed

N/A

### Changed

Updated syntax for static-content-filter

---
## Version 3.1.3, 4/24/2024

### Added

N/A

### Removed

N/A

### Changed

Enhanced OptionalService annotation.

---
## Version 3.1.2, 4/17/2024

### Added

Added "app-config-reader.yml" file in the resources folder so that you can override
the default application configuration files.

### Removed

N/A

### Changed

1. Open sources library update (Spring Boot 3.2.5, Vertx 4.5.7)
2. Improve AppConfigReader and ConfigReader to use the app-config-reader.yml file.
3. Enhanced OptionalService annotation.

---
## Version 3.1.1, 2/8/2024

### Added

1. AutoStart to run application as Spring Boot if the rest-spring-3 library is packaged in app
2. Configurable "Event over HTTP" - automatic forward events over HTTP using a configuration
3. Support user defined serializer with PreLoad annotation and platform API

### Removed

1. Bugfix: removed websocket client connection timeout that causes the first connection to drop after one minute

### Changed

1. Open sources library update (Spring Boot 3.2.2, Vertx 4.5.3 and MsgPack 0.9.8)
2. Rename application parameter "event.worker.pool" to "kernel.thread.pool"

---
## Version 3.1.0, 1/5/2024

### Added

1. Full integration with Java 21 Virtual Thread
2. Default execution mode is set to "virtual thread"
3. KernelThreadRunner annotation added to provide optional support of kernel threads

### Removed

1. Retired Spring Boot version 2
2. Hazelcast and ActiveMQ network connectors

### Changed

platform-core engine updated with virtual thread

---
## Version 3.0.7, 12/23/2023

### Added

Print out basic JVM information before startup for verification of base container image.

### Removed

Removed Maven Shade packager

### Changed

Updated open sources libraries to address security vulnerabilities

1. Spring Boot 2/3 to version 2.7.18 and 3.2.1 respectively
2. Tomcat 9.0.84
3. Vertx 4.5.1
4. Classgraph 4.8.165
5. Netty 4.1.104.Final
6. slf4j API 2.0.9
7. log4j2 2.22.0
8. Kotlin 1.9.22
9. Artemis 2.31.2
10. Hazelcast 5.3.6
11. Guava 33.0.0-jre

---
## Version 3.0.6, 10/26/2023

### Added

Enhanced Benchmark tool to support "Event over HTTP" protocol to evaluate performance
efficiency for commmunication between application containers using HTTP.

### Removed

N/A

### Changed

Updated open sources libraries

1. Spring Boot 2/3 to version 2.7.17 and 3.1.5 respectively
2. Kafka-client 3.6.0

---
## Version 3.0.5, 10/21/2023

### Added

Support two executable JAR packaging system:
1. Maven Shade packager
2. Spring Boot packager

Starting from version 3.0.5, we have replaced Spring Boot packager with Maven Shade.
This avoids a classpath edge case for Spring Boot packager when running kafka-client
under Java 11 or higher.

Maven Shade also results in smaller executable JAR size.

### Removed

N/A

### Changed

Updated open sources libraries

1. Spring-Boot 2.7.16 / 3.1.4
2. classgraph 4.8.163
3. snakeyaml 2.2
4. kotlin 1.9.10
5. vertx 4.4.6
6. guava 32.1.3-jre
7. msgpack 0.9.6
8. slj4j 2.0.9
9. zookeeper 3.7.2

The "/info/lib" admin endpoint has been enhanced to list library dependencies for executable JAR
generated by either Maven Shade or Spring Boot Packager.

Improved ConfigReader to recognize both ".yml" and ".yaml" extensions and their uses are interchangeable.

---
## Version 3.0.4, 8/6/2023

### Added

N/A

### Removed

N/A

### Changed

Updated open sources libraries

1. Spring-Boot 2.7.14 / 3.1.2
2. Kafka-client 3.5.1
3. classgraph 4.8.161
4. guava 32.1.2-jre
5. msgpack 0.9.5

---
## Version 3.0.3, 6/27/2023

### Added

1. File extension to MIME type mapping for static HTML file handling

### Removed

N/A

### Changed

1. Open sources library update - Kotlin version 1.9.0

---
## Version 3.0.2, 6/9/2023

### Added

N/A

### Removed

N/A

### Changed

1. Consistent exception handling for Event API endpoint
2. Open sources lib update - Vertx 4.4.4, Spring Boot 2.7.13, Spring Boot 3.1.1, classgraph 4.8.160, guava 32.0.1-jre

---
## Version 3.0.1, 6/5/2023

In this release, we have replace Google HTTP Client with vertx non-blocking WebClient.
We also tested compatibility up to OpenJDK version 20 and maven 3.9.2.

### Added

When "x-raw-xml" HTTP request header is set to "true", the AsyncHttpClient will skip the built-in 
XML serialization so that your application can retrieve the original XML text.

### Removed

Retire Google HTTP client

### Changed

Upgrade maven plugin versions.


---
## Version 3.0.0, 4/18/2023

This is a major release with some breaking changes. Please refer to Chapter-10 (Migration guide) for details.
This version brings the best of preemptive and cooperating multitasking to Java (version 1.8 to 19) before
Java 19 virtual thread feature becomes officially available.

### Added

1. Function execution engine supporting kernel thread pool, Kotlin coroutine and suspend function
2. "Event over HTTP" service for inter-container communication
3. Support for Spring Boot version 3 and WebFlux
4. Sample code for a pre-configured Spring Boot 3 application

### Removed

1. Remove blocking APIs from platform-core
2. Retire PM2 process manager sample script due to compatibility issue

### Changed

1. Refactor "async.http.request" to use vertx web client for non-blocking operation
2. Update log4j2 version 2.20.0 and slf4j version 2.0.7 in platform-core
3. Update JBoss RestEasy JAX_RS to version 3.15.6.Final in rest-spring
4. Update vertx to 4.4.2
5. Update Spring Boot parent pom to 2.7.12 and 3.1.0 for spring boot 2 and 3 respectively
6. Remove com.fasterxml.classmate dependency from rest-spring

---
## Version 2.8.0, 3/20/2023


### Added

N/A

### Removed

N/A

### Changed

1. Improved load balancing in cloud-connector
2. Filter URI to avoid XSS attack
3. Upgrade to SnakeYaml 2.0 and patch Spring Boot 2.6.8 for compatibility with it
4. Upgrade to Vertx 4.4.0, classgraph 4.8.157, tomcat 9.0.73

---
## Version 2.7.1, 12/22/2022


### Added

1. standalone benchmark report app
2. client and server benchmark apps
3. add timeout tag to RPC events

### Removed

N/A

### Changed

1. Updated open sources dependencies
- Netty 4.1.86.Final
- Tomcat 9.0.69
- Vertx 4.3.6
- classgraph 4.8.152
- google-http-client 1.42.3

2. Improved unit tests to use assertThrows to evaluate exception
3. Enhanced AsyncHttpRequest serialization

---
## Version 2.7.0, 11/11/2022

In this version, REST automation code is moved to platform-core such that REST and Websocket
service can share the same port.

### Added

1. AsyncObjectStreamReader is added for non-blocking read operation from an object stream.
2. Support of LocalDateTime in SimpleMapper
3. Add "removeElement" method to MultiLevelMap
4. Automatically convert a map to a PoJo when the sender does not specify class in event body

### Removed

N/A

### Changed

1. REST automation becomes part of platform-core and it can co-exist with Spring Web in the rest-spring module
2. Enforce Spring Boot lifecycle management such that user apps will start after Spring Boot has loaded all components
3. Update netty to version 4.1.84.Final

---
## Version 2.6.0, 10/13/2022

In this version, websocket notification example code has been removed from the REST automation system.
If your application uses this feature, please recover the code from version 2.5.0 and refactor it as a
separate library.

### Added

N/A

### Removed

Simplify REST automation system by removing websocket notification example in REST automation.

### Changed

1. Replace Tomcat websocket server with Vertx non-blocking websocket server library
2. Update netty to version 4.1.79.Final
3. Update kafka client to version 2.8.2
4. Update snake yaml to version 1.33
5. Update gson to version 2.9.1

---
## Version 2.5.0, 9/10/2022

### Added

New Preload annotation class to automate pre-registration of LambdaFunction.

### Removed

Removed Spring framework and Tomcat dependencies from platform-core so that the core library can be applied
to legacy J2EE application without library conflict.

### Changed

1. Bugfix for proper housekeeping of future events.
2. Make Gson and MsgPack handling of integer/long consistent

Updated open sources libraries.

1. Eclipse vertx-core version 4.3.4
2. MsgPack version 0.9.3
3. Google httpclient version 1.42.2
4. SnakeYaml version 1.31

---
## Version 2.3.6, 6/21/2022

### Added

Support more than one event stream cluster. User application can share the same event stream cluster
for pub/sub or connect to an alternative cluster for pub/sub use cases.

### Removed

N/A

### Changed

Cloud connector libraries update to Hazelcast 5.1.2

---
## Version 2.3.5, 5/30/2022

### Added

Add tagging feature to handle language connector's routing and exception handling

### Removed

Remove language pack's pub/sub broadcast feature

### Changed

1. Update Spring Boot parent to version 2.6.8 to fetch Netty 4.1.77 and Spring Framework 5.3.20
2. Streamlined language connector transport protocol for compatibility with both Python and Node.js

---
## Version 2.3.4, 5/14/2022

### Added

N/A

### Removed

1. Remove swagger-ui distribution from api-playground such that developer can clone the latest version

### Changed

1. Update application.properties (from spring.resources.static-locations to spring.web.resources.static-locations)
2. Update log4j, Tomcat and netty library version using Spring parent 2.6.6

---
## Version 2.3.3, 3/30/2022

### Added

Enhanced AsyncRequest to handle non-blocking fork-n-join

### Removed

N/A

### Changed

Upgrade Spring Boot from 2.6.3 to 2.6.6

---
## Version 2.3.2, 2/21/2022

### Added

Add support of queue API in native pub/sub module for improved ESB compatibility

### Removed

N/A

### Changed

N/A

---

## Version 2.3.1, 2/19/2022

### Added

N/A

### Removed

N/A

### Changed

1. Update Vertx to version 4.2.4
2. Update Tomcat to version 5.0.58
3. Use Tomcat websocket server for presence monitors
4. Bugfix - Simple Scheduler's leader election searches peers correctly

---
## Version 2.3.0, 1/28/2022

### Added

N/A

### Removed

N/A

### Changed

1. Update copyright notice
2. Update Vertx to version 4.2.3
3. Bugfix - RSA key generator supporting key length from 1024 to 4096 bits
4. CryptoAPI - support different AES algorithms and custom IV
5. Update Spring Boot to version 2.6.3

---
## Version 2.2.3, 12/29/2021

### Added

1. Transaction journaling
2. Add parameter `distributed.trace.aggregation` in application.properties such that trace aggregation 
   may be disabled.

### Removed

N/A

### Changed

1. Update JBoss RestEasy library to 3.15.3.Final
2. Improved po.search(route) to scan local and remote service registries. Added "remoteOnly" selection.
3. Fix bug in releasing presence monitor topic for specific closed user group
4. Update Apache log4j to version 2.17.1
5. Update Spring Boot parent to version 2.6.1
6. Update Netty to version 4.1.72.Final
7. Update Vertx to version 4.2.2
8. Convenient class "UserNotification" for backend service to publish events to the UI when REST automation is deployed

---
## Version 2.2.2, 11/12/2021

### Added

1. User defined API authentication functions can be selected using custom HTTP request header
2. "Exception chaining" feature in EventEnvelope
3. New "deferred.commit.log" parameter for backward compatibility with older PowerMock in unit tests

### Removed

N/A

### Changed

1. Improved and streamlined SimpleXmlParser to handle arrays
2. Bugfix for file upload in Service Gateway (REST automation library)
3. Update Tomcat library from 9.0.50 to 9.0.54
4. Update Spring Boot library to 2.5.6
5. Update GSON library to 2.8.9

---
## Version 2.2.1, 10/1/2021

### Added

Callback function can implement ServiceExceptionHandler to catch exception. It adds the onError() method.

### Removed

N/A

### Changed

Open sources library update - Vert.x 4.1.3, Netty 4.1.68-Final

---
## Version 2.1.1, 9/10/2021

### Added

1. User defined PoJo and Generics mapping
2. Standardized serializers for default case, snake_case and camelCase
3. Support of EventEnvelope as input parameter in TypedLambdaFunction so application function can inspect event's 
   metadata
4. Application can subscribe to life cycle events of other application instances

### Removed

N/A

### Changed

1. Replace Tomcat websocket server engine with Vertx in presence monitor for higher performance
2. Bugfix for MsgPack transport of integer, long, BigInteger and BigDecimal

---
## Version 2.1.0, 7/25/2021

### Added

1. Multicast - application can define a multicast.yaml config to relay events to more than one target service.
2. StreamFunction - function that allows the application to control back-pressure

### Removed

"object.streams.io" route is removed from platform-core

### Changed

1. Elastic Queue - Refactored using Oracle Berkeley DB
2. Object stream I/O - simplified design using the new StreamFunction feature
3. Open sources library update - Spring Boot 2.5.2, Tomcat 9.0.50, Vert.x 4.1.1, Netty 4.1.66-Final

---
## Version 2.0.0, 5/5/2021

Vert.x is introduced as the in-memory event bus

### Added

1. ActiveMQ and Tibco connectors
2. Admin endpoints to stop, suspend and resume an application instance
3. Handle edge case to detect stalled application instances
4. Add "isStreamingPubSub" method to the PubSub interface

### Removed

1. Event Node event stream emulator has been retired. You may use standalone Kafka server as a replacement for 
   development and testing in your laptop.
2. Multi-tenancy namespace configuration has been retired. It is replaced by the "closed user group" feature.

### Changed

1. Refactored Kafka and Hazelcast connectors to support virtual topics and closed user groups.
2. Updated ConfigReader to be consistent with Spring value substitution logic for application properties
3. Replace Akka actor system with Vert.x event bus
4. Common code for various cloud connectors consolidated into cloud core libraries

---
## Version 1.13.0, 1/15/2021

Version 1.13.0 is the last version that uses Akka as the in-memory event system.

---
## Version 1.12.66, 1/15/2021

### Added

1. A simple websocket notification service is integrated into the REST automation system
2. Seamless migration feature is added to the REST automation system

### Removed

Legacy websocket notification example application

### Changed

N/A

---
## Version 1.12.65, 12/9/2020

### Added

1. "kafka.pubsub" is added as a cloud service
2. File download example in the lambda-example project
3. "trace.log.header" added to application.properties - when tracing is enabled, this inserts the trace-ID of the 
   transaction in the log context. For more details, please refer to the [Developer Guide](guides/CHAPTER-5.md)
4. Add API to pub/sub engine to support creation of topic with partitions
5. TypedLambdaFunction is added so that developer can predefine input and output classes in a service without casting

### Removed

N/A

### Changed

1. Decouple Kafka pub/sub from kafka connector so that native pub/sub can be used when application is running in 
   standalone mode
2. Rename "relay" to "targetHost" in AsyncHttpRequest data model
3. Enhanced routing table distribution by sending a complete list of route tables, thus reducing network admin traffic.

---
## Version 1.12.64, 9/28/2020

### Added

If predictable topic is set, application instances will report their predictable topics as "instance ID"
to the presence monitor. This improves visibility when a developer tests their application in "hybrid" mode.
i.e. running the app locally and connect to the cloud remotely for event streams and cloud resources.

### Removed

N/A

### Changed

N/A

---
## Version 1.12.63, 8/27/2020

### Added

N/A

### Removed

N/A

### Changed

Improved Kafka producer and consumer pairing

---
## Version 1.12.62, 8/12/2020

### Added

New presence monitor's admin endpoint for the operator to force routing table synchronization ("/api/ping/now")

### Removed

N/A

### Changed

Improved routing table integrity check

---
## Version 1.12.61, 8/8/2020

### Added

Event stream systems like Kafka assume topic to be used long term. 
This version adds support to reuse the same topic when an application instance restarts.

You can create a predictable topic using unique application name and instance ID.
For example, with Kubernetes, you can use the POD name as the unique application instance topic.

### Removed

N/A

### Changed

N/A

---
## Version 1.12.56, 8/4/2020

### Added

Automate trace for fork-n-join use case

### Removed

N/A

### Changed

N/A

---
## Version 1.12.55, 7/19/2020

### Added

N/A

### Removed

N/A

### Changed

Improved distributed trace - set the "from" address in EventEnvelope automatically.

---
## Version 1.12.54, 7/10/2020

### Added

N/A

### Removed

N/A

### Changed

Application life-cycle management - User provided main application(s) will be started after Spring Boot declares web
application ready. This ensures correct Spring autowiring or dependencies are available.

Bugfix for locale - String.format(float) returns comma as decimal point that breaks number parser. 
Replace with BigDecimal decimal point scaling.

Bugfix for Tomcat 9.0.35 - Change Async servlet default timeout from 30 seconds to -1 so the system can handle the 
whole life-cycle directly.

---
## Version 1.12.52, 6/11/2020

### Added

1. new "search" method in Post Office to return a list of application instances for a service
2. simple "cron" job scheduler as an extension project
3. add "sequence" to MainApplication annotation for orderly execution when more than one MainApplication is available
4. support "Optional" object in EventEnvelope so a LambdaFunction can read and return Optional

### Removed

N/A

### Changed

1. The rest-spring library has been updated to support both JAR and WAR deployment
2. All pom.xml files updated accordingly
3. PersistentWsClient will back off for 10 seconds when disconnected by remote host

---
## Version 1.12.50, 5/20/2020

### Added

1. Payload segmentation

   For large payload in an event, the payload is automatically segmented into 64 KB segments.
   When there are more than one target application instances, the system ensures that the segments of the same event 
   is delivered to exactly the same target.

2. PersistentWsClient added - generalized persistent websocket client for Event Node, Kafka reporter and Hazelcast
   reporter.

### Removed

N/A

### Changed

1. Code cleaning to improve consistency
2. Upgraded to hibernate-validator to v6.1.5.Final and Hazelcast version 4.0.1
3. REST automation is provided as a library and an application to handle different use cases

---
## Version 1.12.40, 5/4/2020

### Added

N/A

### Removed

N/A

### Changed

For security reason, upgrade log4j to version 2.13.2

---
## Version 1.12.39, 5/3/2020

### Added

Use RestEasy JAX-RS library

### Removed

For security reason, removed Jersey JAX-RS library

### Changed

1. Updated RestLoader to initialize RestEasy servlet dispatcher
2. Support nested arrays in MultiLevelMap

---
## Version 1.12.36, 4/16/2020

### Added

N/A

### Removed

For simplicity, retire route-substitution admin endpoint. Route substitution uses a simple static table in 
route-substitution.yaml.

### Changed

N/A

---
## Version 1.12.35, 4/12/2020

### Added

N/A

### Removed

SimpleRBAC class is retired

### Changed

1. Improved ConfigReader and AppConfigReader with automatic key-value normalization for YAML and JSON files
2. Improved pub/sub module in kafka-connector

---
## Version 1.12.34, 3/28/2020

### Added

N/A

### Removed

Retired proprietary config manager since we can use the "BeforeApplication" approach to load config from Kubernetes 
configMap or other systems of config record.

### Changed

1. Added "isZero" method to the SimpleMapper class
2. Convert BigDecimal to string without scientific notation (i.e. toPlainString instead of toString)
3. Corresponding unit tests added to verify behavior

---
## Version 1.12.32, 3/14/2020

### Added

N/A

### Removed

N/A

### Changed

Kafka-connector will shutdown application instance when the EventProducer cannot send event to Kafka. 
This would allow the infrastructure to restart application instance automatically.

---
## Version 1.12.31, 2/26/2020

### Added

N/A

### Removed

N/A

### Changed

1. Kafka-connector now supports external service provider for Kafka properties and credentials. 
   If your application implements a function with route name "kafka.properties.provider" before connecting to cloud, 
   the kafka-connector will retrieve kafka credentials on demand. This addresses case when kafka credentials change 
   after application start-up.
2. Interceptors are designed to forward requests and thus they do not generate replies. However, if you implement a 
   function as an EventInterceptor, your function can throw exception just like a regular function and the exception 
   will be returned to the calling function. This makes it easier to write interceptors.

---
## Version 1.12.30, 2/6/2020

### Added

1. Expose "async.http.request" as a PUBLIC function ("HttpClient as a service")

### Removed

N/A

### Changed

1. Improved Hazelcast client connection stability
2. Improved Kafka native pub/sub

---
## Version 1.12.29, 1/10/2020

### Added

1. Rest-automation will transport X-Trace-Id from/to Http request/response, therefore extending distributed trace 
   across systems that support the X-Trace-Id HTTP header.
2. Added endpoint and service to shutdown application instance.

### Removed

N/A

### Changed

1. Updated SimpleXmlParser with XML External Entity (XXE) injection prevention.
2. Bug fix for hazelcast recovery logic - when a hazelcast node is down, the app instance will restart the hazelcast 
   client and reset routing table correctly.
3. HSTS header insertion is optional so that we can disable it to avoid duplicated header when API gateway is doing it.

---
## Version 1.12.26, 1/4/2020

### Added

Feature to disable PoJo deserialization so that caller can decide if the result set should be in PoJo or a Map.

### Removed

N/A

### Changed

1. Simplified key management for Event Node
2. AsyncHttpRequest case insensitivity for headers, cookies, path parameters and session key-values
3. Make built-in configuration management optional

---
## Version 1.12.19, 12/28/2019

### Added

Added HTTP relay feature in rest-automation project

### Removed

N/A

### Changed

1. Improved hazelcast retry and peer discovery logic
2. Refactored rest-automation's service gateway module to use AsyncHttpRequest
3. Info endpoint to show routing table of a peer

---

## Version 1.12.17, 12/16/2019

### Added

1. Simple configuration management is added to event-node, hazelcast-presence and kafka-presence monitors
2. Added `BeforeApplication` annotation - this allows user application to execute some setup logic before the main 
   application starts. e.g. modifying parameters in application.properties
3. Added API playground as a convenient standalone application to render OpenAPI 2.0 and 3.0 yaml and json files
4. Added argument parser in rest-automation helper app to use a static HTML folder in the local file system if 
   arguments `-html file_path` is given when starting the JAR file.

### Removed

N/A

### Changed

1. Kafka publisher timeout value changed from 10 to 20 seconds
2. Log a warning when Kafka takes more than 5 seconds to send an event

---
## Version 1.12.14, 11/20/2019

### Added

1. getRoute() method is added to PostOffice to facilitate RBAC
2. The route name of the current service is added to an outgoing event when the "from" field is not present
3. Simple RBAC using YAML configuration instead of code

### Removed

N/A

### Changed

Updated Spring Boot to v2.2.1

---
## Version 1.12.12, 10/26/2019

### Added

Multi-tenancy support for event streams (Hazelcast and Kafka).
This allows the use of a single event stream cluster for multiple non-prod environments.
For production, it must use a separate event stream cluster for security reason.

### Removed

N/A

### Changed

1. logging framework changed from logback to log4j2 (version 2.12.1)
2. Use JSR-356 websocket annotated ClientEndpoint
3. Improved websocket reconnection logic

---
## Version 1.12.9, 9/14/2019

### Added

1. Distributed tracing implemented in platform-core and rest-automation
2. Improved HTTP header transformation for rest-automation

### Removed

N/A

### Changed

language pack API key obtained from environment variable 

---
## Version 1.12.8, 8/15/2019

### Added

N/A

### Removed

rest-core subproject has been merged with rest-spring

### Changed

N/A

---
## Version 1.12.7, 7/15/2019

### Added

1. Periodic routing table integrity check (15 minutes)
2. Set kafka read pointer to the beginning for new application instances except presence monitor
3. REST automation helper application in the "extensions" project
4. Support service discovery of multiple routes in the updated PostOffice's exists() method
5. logback to set log level based on environment variable LOG_LEVEL (default is INFO)

### Removed

N/A

### Changed

Minor refactoring of kafka-connector and hazelcast-connector to ensure that they can coexist if you want to include 
both of these dependencies in your project.

This is for convenience of dev and testing. In production, please select only one cloud connector library to reduce
memory footprint.

---

## Version 1.12.4, 6/24/2019

### Added

Add inactivity expiry timer to ObjectStreamIO so that house-keeper can clean up resources that are idle

### Removed

N/A

### Changed

1. Disable HTML encape sequence for GSON serializer
2. Bug fix for GSON serialization optimization
3. Bug fix for Object Stream housekeeper

By default, GSON serializer converts all numbers to double, resulting in unwanted decimal point for integer and long.
To handle custom map serialization for correct representation of numbers, an unintended side effect was introduced in 
earlier releases.

List of inner PoJo would be incorrectly serialized as map, resulting in casting exception. 
This release resolves this issue.

---

## Version 1.12.1, 6/10/2019

### Added

1. Store-n-forward pub/sub API will be automatically enabled if the underlying cloud connector supports it. e.g. kafka
2. ObjectStreamIO, a convenient wrapper class, to provide event stream I/O API.
3. Object stream feature is now a standard feature instead of optional.
4. Deferred delivery added to language connector.

### Removed

N/A

### Changed

N/A

---

## Version 1.11.40, 5/25/2019

### Added

1. Route substitution for simple versioning use case
2. Add "Strict Transport Security" header if HTTPS (https://tools.ietf.org/html/rfc6797)
3. Event stream connector for Kafka
4. Distributed housekeeper feature for Hazelcast connector

### Removed

System log service

### Changed

Refactoring of Hazelcast event stream connector library to sync up with the new Kafka connector.

---

## Version 1.11.39, 4/30/2019

### Added

Language-support service application for Python, Node.js and Go, etc.
Python language pack project is available at https://github.com/Accenture/mercury-python

### Removed

N/A

### Changed

1. replace Jackson serialization engine with Gson (`platform-core` project)
2. replace Apache HttpClient with Google Http Client (`rest-spring`)
3. remove Jackson dependencies from Spring Boot (`rest-spring`)
4. interceptor improvement

---

## Version 1.11.33, 3/25/2019

### Added

N/A

### Removed

N/A

### Changed

1. Move safe.data.models validation rules from EventEnvelope to SimpleMapper
2. Apache fluent HTTP client downgraded to version 4.5.6 because the pom file in 4.5.7 is invalid

---

## Version 1.11.30, 3/7/2019

### Added

Added retry logic in persistent queue when OS cannot update local file metadata in real-time for Windows based machine.

### Removed

N/A

### Changed

pom.xml changes - update with latest 3rd party open sources dependencies. 

---

## Version 1.11.29, 1/25/2019

### Added

`platform-core`

1. Support for long running functions so that any long queries will not block the rest of the system.
2. "safe.data.models" is available as an option in the application.properties. 
   This is an additional security measure to protect against Jackson deserialization vulnerability. 
   See example below:

```
#
# additional security to protect against model injection
# comma separated list of model packages that are considered safe to be used for object deserialization
#
#safe.data.models=com.accenture.models
```

`rest-spring`

"/env" endpoint is added. See sample application.properties below:

```
#
# environment and system properties to be exposed to the "/env" admin endpoint
#
show.env.variables=USER, TEST
show.application.properties=server.port, cloud.connector
```

### Removed

N/A

### Changed

`platform-core`

Use Java Future and an elastic cached thread pool for executing user functions.

### Fixed

N/A

---


## Version 1.11.28, 12/20/2018

### Added

Hazelcast support is added. This includes two projects (hazelcast-connector and hazelcast-presence).

Hazelcast-connector is a cloud connector library. Hazelcast-presence is the "Presence Monitor" for monitoring the 
presence status of each application instance.

### Removed

`platform-core`

The "fixed resource manager" feature is removed because the same outcome can be achieved at the application level. 
e.g. The application can broadcast requests to multiple application instances with the same route name and use a 
callback function to receive response asynchronously. The services can provide resource metrics so that the caller
can decide which is the most available instance to contact. 

For simplicity, resources management is better left to the cloud platform or the application itself.

### Changed

N/A

### Fixed

N/A
