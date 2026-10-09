# Simple Scheduler

Simple scheduler that uses YAML configuration for storing "cron" job schedules

## Simple clustering

To support multiple application instances for resilience, you can implement a "state resolver" function.
An example is shown in the StateResolver class under the "services" folder.

The state resolver function should persist the time when a task is executed by the scheduler and support
a query API to test if the previous task has been expired so that it can schedule another one. You may use
a simple key-value store or SQL database as the persistent store.

To reduce racing condition, you can set "deferred.start" to true in cron.yaml and the scheduler will defer
a job from 0 to 9 seconds so that application instances will run a schedule at a slightly different time.
This allows the state resolver to easily detect if a task has been scheduled.

For backward compatibility, the mini-scheduler can perform a "leader election" to select an application
instance using a minimalist service mesh (e.g. cloud.connector=kafka). To enable this feature,
set "leader.election" to true.

(Alternatively, you may also turn on Quartz's cluster mode and configure the database accordingly.)

## Active-active sites: one environment runs the jobs

A scheduler deployed in two sites (Production and DR) active-active fires every job in both; a job that
publishes to the site's Kafka cluster then wakes the DR applications that must stay idle while Production
is up. Two properties keep the jobs in one site:

```
scheduler.environment=${ENV_NAME:prod}
scheduler.active.environment=prod
```

`scheduler.environment` names this instance's environment and `scheduler.active.environment` the one whose
scheduler runs the jobs at startup; both default to `prod`, so an unconfigured installation behaves as
before. The scheduler is **active** when the two match and on **standby** otherwise: a scheduled job is
skipped with one log line (`Standby: job demo-task skipped - the active environment is prod, this instance
is DR`), and an operator's manual run is still honoured.

The operations team moves the active environment around a Production maintenance window - to `DR` before,
back to `prod` after - through the application's REST endpoint (`GET`/`POST /api/scheduler/environment`
in the example, `{"active": "DR", "operator": "..."}`). The value is **persisted** through the function
named by `scheduler.environment.store` (default `v1.environment.store`), which the application implements
against its own data store - the example's `EnvironmentStore` writes a file; replace it with a database
such as PostgreSQL or MongoDB - because Kubernetes restarts a pod without notice and an in-memory value
would reset. The store is read before every scheduled job, so one call moves every instance of every
site, and the persisted value wins over the property. Without a registered store the value lives in
memory only, and the scheduler says so at startup.

## cron schedules

Cron jobs can be defined in a "cron.yaml" file.

You can change the file path in application.properties:
```
yaml.cron=file:/tmp/config/cron.yaml, classpath:/cron.yaml
```
In this example, it assumes the configuration file is available in "/tmp/config/cron.yaml".
If it is not there, it will use the default one in the classpath.

Note that the one in the classpath is just an example. Please update the YAML configuration accordingly.

## Sample cron.yaml

```
jobs:
  - name: "demo-task"
    description: "execute demo service every 15 seconds"
    cron: "0/15 0/1 * 1/1 * ? *"
    service: "hello.world"
    # optional parameter to tell the service what to do
    parameters:
      hello: "world"
    resolver: "v1.state.resolver"

  - name: "demo-flow"
    description: "execute demo service every 25 seconds"
    cron: "0/25 0/1 * 1/1 * ? *"
    service: "flow://hello-flow"
    # In the demo flow, it has 2 tasks. We use input data mapping to map "db" and "other" parameters accordingly.
    # See "hello-flow.yml for details.
    parameters:
      db:
        host: "db://demo"
        feature: "read only"
      other:
        demo: "some value"
    resolver: "v1.state.resolver"
    
deferred.start: true
leader.election: false    
```
In this example, there are two scheduled jobs (demo-task and demo-flow). The scheduler executes
the service "hello.world" every 15 seconds and the flow "hello-flow" every 25 seconds.

## Running this application

1. You can run the application class MainApp from the IDE
2. You may also run the application by building it using `mvn clean package` and then run the application as follows:

> **Note**: `x.y.z` denotes the current Mercury version shown in the root `pom.xml`.

```shell
java -jar target/scheduler-example-x.y.z.jar
```

## Admin endpoints

The application is pre-configured with 2 scheduled jobs in cron.yaml above.

You can check the running jobs using "GET /api/schedule". It will show something like this:

```json
{
  "jobs": [
    {
      "start": "2025-12-08T05:03:37.012Z",
      "name": "demo-task",
      "end": "2025-12-08T05:03:37.020Z"
    },
    {
      "start": "2025-12-08T05:03:28.007Z",
      "name": "demo-flow",
      "end": "2025-12-08T05:03:28.018Z"
    }
  ],
  "time": "2025-12-08T05:03:42.134Z",
  "message": "GET /api/schedule/{name}"
}
```

To query individual job, use "GET /api/schedule/{name}" where name is "demo-flow" or "demo-task".
A job status report may look like this:

```json
{
  "elapsed": "11 ms",
  "schedule": "0/25 0/1 * 1/1 * ? *",
  "service": "flow://hello-flow",
  "name": "demo-flow",
  "start": "2025-12-08T05:03:59.009Z",
  "end": "2025-12-08T05:03:59.020Z",
  "parameters": {
    "other": {
      "demo": "some value"
    },
    "db": {
      "feature": "read only",
      "host": "db://demo"
    }
  }
}
```

To run a job manually, you can do "POST /api/schedule/{name}" where name is "demo-flow" or "demo-task".
Set content-type to "application/json" and put an operator name like this:

```json
{"operator": "Peter"}
```

The application will run the job and print a log message like this:

```text
JobExecutor:79 - Operator Peter runs demo-flow manually
```

When you run a job manually using this admin endpoint, the job will start without checking if the job has been
run by another scheduler instance recently.

Manual invocation of job may be used in test environments.

# cron expression

Quartz cron expressions are strings used to define schedules for jobs in the Quartz Scheduler. These expressions
consist of six or seven sub-expressions (fields) separated by white space, each representing a specific time unit
in the schedule. Fields of a Quartz Cron Expression:

```text
Seconds: (0-59)
Minutes: (0-59)
Hours: (0-23)
Day-of-Month: (1-31)
Month: (1-12 or JAN-DEC)
Day-of-Week: (1-7 or SUN-SAT)
Year: (optional, e.g., 1970-2099)
```

## Special Characters and Their Meanings:

```text
(Asterisk *): Represents all values within a field. For example, * in the minute field means "every minute."
  ? (Question Mark): Used for "no specific value" in either the Day-of-Month or Day-of-Week field, but not both 
                     simultaneously. It indicates that one of these fields should be ignored.
(Hyphen -): Specifies a range of values. For example, 10-12 in the hour field means "hours 10, 11, and 12."
  , (Comma): Specifies a list of individual values. For example, MON,WED,FRI in the Day-of-Week field means 
             "Monday, Wednesday, and Friday."
  / (Slash): Used to specify increments. For example, 0/15 in the minute field means "every 15 minutes, starting
             at minute 0."
  L (Last):
  In Day-of-Month: "last day of the month."
  In Day-of-Week: "last day of the week," typically "last Friday" if used with a specific day like 6L.
  W (Weekday): Used in the Day-of-Month field to specify the nearest weekday to the given day. 
               For example, 15W means "the nearest weekday to the 15th of the month."

(Hash): Used in the Day-of-Week field to specify the "nth" instance of a day of the week in the month. 
        For example, 6#3 means "the third Friday of the month."
```

Examples:

0 0 12 * * ? : Fires at 12 PM (noon) every day.

0 15 10 ? * MON-FRI : Fires at 10:15 AM every Monday, Tuesday, Wednesday, Thursday, and Friday.

0 0/5 14 * * ? : Fires every 5 minutes from 2:00 PM to 2:55 PM, every day.

0 15 10 L * ? : Fires at 10:15 AM on the last day of every month. 
