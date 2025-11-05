# Simple Scheduler

Simple scheduler that uses YAML configuration for storing "cron" job schedules

## Cloud native deployment

More than one instance of this application can be deployed.
When this happens, the multiple instances will run in parallel.

To avoid duplicated job scheduling, the multiple instances will detect the presence of each other
and elect an leader to execute the scheduled jobs.

It does not need a database to do clustering. The multiple instances will sync up its job status.

## cron schedules

Cron jobs can be defined in a "cron.yaml" file.

You can change the file path in application.properties:
```
yaml.cron=file:/tmp/config/cron.yaml, classpath:/cron.yaml
```
In this example, it assumes the configuration file is available in "/tmp/config/cron.yaml".
If it is not there, it will use the default one in the classpath.

Note the the one in the classpath is just an example. Please update the YAML configuration accordingly.

## Admin endpoints

There are a few admin endpoints
```
GET /api/jobs
This retrieves a list of scheduled jobs

DELETE /api/jobs/{name}
This stop a job if it has been started

PUT /api/jobs/{name}
This start a job if it has been stopped

POST /api/jobs/{name}
This execute a job immediately

```

## Sample cron.yaml

```
jobs:
  # you can cancel a job from an admin endpoint and restart it anytime
  - name: "demo"
    description: "execute demo service every minute"
    cron: "0 0/1 * 1/1 * ? *"
    service: "hello.world"
    # optional parameter to tell the service what to do
    parameters:
      hello: "world"
```
In this example, there is one scheduled job called "demo" and it will execute the service "hello.world" every minute.

## Sample job listing

```
GET http://127.0.0.1:8083/api/jobs

{
  "total": 1,
  "jobs": [
    {
      "start_time": "2020-06-05T22:14:11.021Z",
      "last_execution": "2020-06-05T22:15:00.012Z",
      "cron_schedule": "0 0/1 * 1/1 * ? *",
      "service": "hello.world",
      "created": "2020-06-05T22:14:10.928Z",
      "name": "demo",
      "description": "execute demo service every minute",
      "parameters": {
        "hello": "world"
      },
      "iterations": 1
    }
  ],
  "time": "2020-06-05T22:15:05.970Z"
}
```

# cron expression

Quartz cron expressions are strings used to define schedules for jobs in the Quartz Scheduler. These expressions consist of six or seven sub-expressions (fields) separated by white space, each representing a specific time unit in the schedule.
Fields of a Quartz Cron Expression:

Seconds: (0-59)
Minutes: (0-59)
Hours: (0-23)
Day-of-Month: (1-31)
Month: (1-12 or JAN-DEC)
Day-of-Week: (1-7 or SUN-SAT)
Year: (optional, e.g., 1970-2099)

## Special Characters and Their Meanings:
* (Asterisk): Represents all values within a field. For example, * in the minute field means "every minute."
  ? (Question Mark): Used for "no specific value" in either the Day-of-Month or Day-of-Week field, but not both simultaneously. It indicates that one of these fields should be ignored.
- (Hyphen): Specifies a range of values. For example, 10-12 in the hour field means "hours 10, 11, and 12."
  , (Comma): Specifies a list of individual values. For example, MON,WED,FRI in the Day-of-Week field means "Monday, Wednesday, and Friday."
  / (Slash): Used to specify increments. For example, 0/15 in the minute field means "every 15 minutes, starting at minute 0."
  L (Last):
  In Day-of-Month: "last day of the month."
  In Day-of-Week: "last day of the week," typically "last Friday" if used with a specific day like 6L.
  W (Weekday): Used in the Day-of-Month field to specify the nearest weekday to the given day. For example, 15W means "the nearest weekday to the 15th of the month."

(Hash): Used in the Day-of-Week field to specify the "nth" instance of a day of the week in the month. For example, 6#3 means "the third Friday of the month."
Examples:
0 0 12 * * ? : Fires at 12 PM (noon) every day.
0 15 10 ? * MON-FRI : Fires at 10:15 AM every Monday, Tuesday, Wednesday, Thursday, and Friday.
0 0/5 14 * * ? : Fires every 5 minutes from 2:00 PM to 2:55 PM, every day.
0 15 10 L * ? : Fires at 10:15 AM on the last day of every month. 
