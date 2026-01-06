# reactive-postgres-client

This is a reactive version of the PostGreSQL database client using Spring R2DBC.

## Building this library

Before you build this library, please ensure that you have a PostGreSQL server running and update the following
parameters in application.properties accordingly. Then run `mvn clean install` to build the library in your
".m2" local repository.

This library is using an embedded PostGreSQL server for unit test for more realistic validation of the database
access patterns.

```properties
#
# Add your organization package path for Spring R2DBC configurator to scan for repositories
#
postgres.repository.scan=my.org
#
# PostGreSQL connection pool size
#
postgres.connection.pool=25
#
# PostGreSQL credentials
#
postgres.database=postgres
postgres.host=127.0.0.1
postgres.port=5432
postgres.user={$PG_USER}
postgres.password={$PG_PASSWORD}
postgres.ssl=false
```

## Database access patterns

This module supports four ways to access PostGreSQL:

1. Reactive Repository pattern
2. Database client API
3. PostOffice RPC calls to the PgService using route "postgres.service"
4. PgRequest query and update methods that are wrappers of method-3 above

# Repository pattern

The ReactivePgConfig class is configured to scan "com.accenture" and your organization package path
for models and repositories.

Your organization package path must be defined in the `postgres.repository.scan` parameter in application.properties.
For example, if your organization package name is "my.org", the parameter would look like this:

```properties
#
# Add your organization package path for Spring R2DBC configurator to scan for repositories
#
postgres.repository.scan=my.org
```

You should also update the `web.component.scan` to include your organization package path if you have user functions
under your own organization path.

```properties
web.component.scan=my.org
```

Please review the TempTestData class and TempRepo interface in the "src/test/models" and "src/test/repository" folders.

Repositories are defined as "interface" and thus there is no code required in the repository classes.
You can create custom methods in addition to some out-of-box standard CRUD methods. For each custom method,
please add a "@Query" annotation for your custom SQL statement with input parameters. 
The arguments in the method will be mapped to the ":param" fields in the SQL statement.

Since this is a reactive library, the return object is usually a Flux<T> or Mono<T> class.
The Flux class is a reactive stream of records (the PoJo specified in your database
table model). The Mono class is a reactive object of a single record or an Integer indicating the number
of rows inserted, updated or deleted.

## Database client API

The repository pattern provides simple configuration of atomic database requests.

For more sophisticated use cases, you may use the database client API. You have full control of how the database
operation should behave, including reactive mapping of Flux into a list of Mono objects using the "collectionList()"
or "next()" method.

The RowParser is a helper class for your app to convert a record (Row) into a map of key-values or a PoJo (database
model). See DbDemoFunction for usage pattern.

## SQL statement parameter binding

PostGreSQL prepared statement supports 2 ways of parameter binding

1. Positional parameter - you use the "$n" syntax as a positional parameter in a SQL statement where "n" starts from 1.
   However, the actual binding index starts from 0. The PgQueryStatement and PgUpdateStatement will set the index
   correctly.
2. Named parameter - you use the ":name" syntax as a named parameter in a SQL statement where "name" is an identifier
   of the parameter. 

## Autowiring

You can use Spring autowiring in a LambdaFunction or TypedLambdaFunction. DO NOT add the "@Service"
annotation because the platform foundation code will automate the wiring for you. The "@PreLoad" annotation is
used to scan the class and perform autowiring.

Note that this library requires the "rest-spring-3" dependency that has been tuned to be compatible with the
Spring ecosystem.

See the HttpTestEndpoint and DbDemoFunction for how autowiring is done.

## Simple use cases

The Unit Test "ReactiveDbTest" class is designed as a worked example for the four database access methods. 

Please review the models, repositories, composable functions and the unit tests for details.

## pg-schema.sql

The default SQL initialization script is available in both the main/resources and test/resources folders.
The one in the main/resources is need to ensure the health check DB table is available.

The unit test's version of the pg-schema.sql is used to create a temp table to read/write test records.

## R2DBC documentation

Please refer to Spring R2DBC for additional details. Both Atomic request and transaction are supported.

## Connection pool

There is a single parameter in application.properties. Please be conservative in the pool size.
It should be a small number less than 100. Otherwise, your PostGreSQL database may run out of connections.

```properties
postgres.connection.pool=25
```

## Avoid blocking code

Please NEVER use the ".block()" method in reactive pattern. The "block()" API will stop the rest
of the system from running smoothly. Java virtual thread system is designed for reactive and very fast
execution of functions. Any blocking code will break the JVM.

## The subscribe() method

Usually, you do not need to invoke the "subscribe()" method because the platform foundation code will
subscribe to a Flux or Mono response. When you use subscribe(), the response will NOT be returned to the
calling function.

## JPA not supported

Java Persistence Adapter (JPA) is blocking code and, thus it is not supported in the new Platform version 4
and its libraries.

Please note that EntityManager and the DbQuery classes have been retired.

## Database timestamp fields

PostGreSQL supports LocalDateTime (TIMESTAMP) and UTC (TIMESTAMPTZ).

The system will convert timestamps between LocalDateTime and UTC Date objects automatically.

The conversion is based on the default system time zone in the deployed Kubernetes' POD.

## SQL statement execution debug logging

Add the following key-values if you want to see what SQL statements have been executed in your code.

```properties
#
# DO NOT SET THIS IN PRODUCTION - IT CAN LEAK PII DATA
# ----------------------------------------------------
# Optional: R2DBC debug logging for dev and test only
#
logging.level.io.r2dbc.postgresql.QUERY=DEBUG
logging.level.io.r2dbc.postgresql.PARAM=DEBUG
```

## Known issue for unit test in Windows

During the "teardown" phase when running the ReactiveDbTest under Windows OS, a fatal PostGreSQL server log
"terminating connection due to administrator command" is shown and the ReactorNettyClient class will print
out "error" log message about SocketException. The ReactorNettyClient log is suppressed using a custom log4j.xml
in the test/resources folder.

These database error logs do not affect the orderly shutdown of the embedded PostGreSQL server. Therefore, the error
log can be safely ignored.

This issue does not occur when running unit tests in a Mac OS-X or Linux machine.
