# Simple Scheduler

Simple scheduler that uses YAML configuration for storing "cron" job schedules

## Simple clustering

To support multiple application instances for resilience, you can implement a "state resolver" function.
An example is shown in the StateResolver class under the "services" folder.

The state resolver function should persist the time when a task is executed by the scheduler and support
a query API to test if the previous task has been expired so that it can schedule another one. You may use
a simple key-value store or SQL database as the persistent store.

To reduce racing condition, the scheduler defers a job from 0 to 9 seconds so that application instances
will run a schedule at a slightly different time. This allows the state resolver to easily detect if a task
has been scheduled.

(Alternatively, you may also turn on Quartz's cluster mode and configure the database accordingly.)

## cron schedules

Cron jobs can be defined in a "cron.yaml" file.

You can change the file path in application.properties:
```
yaml.cron=file:/tmp/config/cron.yaml, classpath:/cron.yaml
```
In this example, it assumes the configuration file is available in "/tmp/config/cron.yaml".
If it is not there, it will use the default one in the classpath.

Note the the one in the classpath is just an example. Please update the YAML configuration accordingly.

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
```
In this example, there are two scheduled jobs (demo-task and demo-flow). The scheduler executes
the service "hello.world" every 15 seconds and the flow "hello-flow" every 25 seconds.

## Running this application

1. You can run the application class MainApp from the IDE
2. You may also run the application by building it using `mvn clean package` and then run the application as follows:

```shell
java -jar target/scheduler-example-4.3.30.jar
```

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
