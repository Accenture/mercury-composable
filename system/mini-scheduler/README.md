# Simple Scheduler

Simple scheduler that uses YAML configuration for storing "cron" job schedules

The user guide is [Mini-Scheduler](https://accenture.github.io/mercury-composable/guides/mini-scheduler/) in the
Mercury documentation (`docs/guides/mini-scheduler.md`): the cron file, the state resolver's contract and claim,
the active environment, manual runs, the log lines and troubleshooting. This README summarizes the module.

## Running more than one instance

To run the scheduler in more than one application instance for resilience, implement a "state resolver"
function; the StateResolver class under the unit test folder shows its contract. Every instance fires every
job, and the resolver decides which one runs it:

- `expires` answers whether the job may run now. With more than one instance it **claims** the job for the
  instance that asks, in one conditional write to a database the instances share. The write succeeds when no
  instance has claimed the job within the resolver's window, or when the asking instance holds the claim
  already, and the database refuses it otherwise, so exactly one instance gets `true` however close together
  the instances ask.
- `start` and `end` record the run: its service, schedule and parameters, and when it started and ended.

Every request names the instance that sends it: its parameters carry the instance's origin id under
`_origin`. The id stays the same while the instance runs, and the instance logs it at startup
(`Scheduler instance 20261104... - the state resolver receives it as _origin`), so a person who reads a state
record can tell which instance wrote it.

The claim needs three things:

1. **One conditional write.** Reading the record and then writing it lets two instances both read "expired"
   and both run the job: two instances that fired at the same instant did so in 10 of 11 firings, and with
   `deferred.start` in 12 of 240. A write followed by a delay and a read-back holds only while every write
   lands within the delay, so a pause longer than the delay between an instance's read and its write runs the
   job on both. A record lock held while the resolver answers makes the other instance wait, and the
   scheduler reports the wait as a timeout.
2. **A unique key on the job's name.** When the condition fails, the write becomes an insert, and the
   database must refuse a second record for the same name. Use the name as the primary key (PostgreSQL) or as
   the `_id` (MongoDB). A separate name field needs a *unique* index, `createIndex({name: 1}, {unique: true})`
   in MongoDB: with a plain index both instances insert a record and both run the job.
3. **A window** longer than the time from a claim to the last question that can follow it, and shorter than
   the job's period. A peer may ask up to 9 seconds after the claim (`deferred.start`) and ask again up to 9
   seconds after that (see *Slow answers: retries* below), so the window is more than 18 seconds plus the
   database's own latency - 30 seconds in the examples. It is less than the job's period minus 9 seconds,
   because the previous run may have been claimed 9 seconds into its period; for a daily job in a region with
   daylight saving, the period is 23 hours on the day the clocks go forward (see *Time zone*).

`deferred.start: true` defers each job by 0 to 9 seconds in each instance. The claim does not need it to pick
one instance; it only spreads the instances' questions over those seconds.

Both worked examples below were run with eight instances claiming the same job at the same instant, 300 times
on a first run and 300 times after an expired run: exactly one instance won every time, on PostgreSQL 18.4
and MongoDB 8.0. The resolver's `handleEvent` dispatches on the `type` header as the sample does: `expires`
returns `claim(name, input)`, and `start` and `end` run the writes that follow the claim.

### Worked example: PostgreSQL

The job's name is the primary key, and the parameters, with `_origin` among them, are a JSONB column:

```sql
CREATE TABLE job_state (
    name       TEXT PRIMARY KEY,
    service    TEXT,
    schedule   TEXT,
    parameters JSONB,
    start_time TIMESTAMPTZ NOT NULL,
    end_time   TIMESTAMPTZ
)
```

`expires` claims the job in one statement. It inserts the job's record, or, when the record exists, updates
it only if the window has passed or the asking instance holds the claim:

```java
private static final String CLAIM = """
        INSERT INTO job_state (name, parameters, start_time) VALUES (?, ?::jsonb, now())
        ON CONFLICT (name) DO UPDATE
            SET parameters = EXCLUDED.parameters, start_time = EXCLUDED.start_time, end_time = NULL
            WHERE job_state.start_time < now() - interval '30 seconds'
               OR job_state.parameters ->> '_origin' = EXCLUDED.parameters ->> '_origin'""";

/** "expires": claim the job for the instance that "_origin" names. */
private boolean claim(String name, Map<String, Object> parameters) throws SQLException {
    var json = SimpleMapper.getInstance().getMapper().writeValueAsString(parameters);
    try (var db = dataSource.getConnection(); var sql = db.prepareStatement(CLAIM)) {
        sql.setString(1, name);
        sql.setString(2, json);
        // one row: this instance holds the claim; none: a peer claimed the job within the window
        return sql.executeUpdate() == 1;
    }
}
```

`now()` is the database's clock, so the instances' clocks do not enter into it. Being ACID does not pick one
winner by itself: the same statement without its `WHERE` succeeds for every instance, and in the lab both
instances ran the job in 300 of 300 rounds. `start` and `end` record the run:

```sql
-- start: the run that the claim allowed, or an operator's manual run
INSERT INTO job_state (name, service, schedule, parameters, start_time) VALUES (?, ?, ?, ?::jsonb, now())
ON CONFLICT (name) DO UPDATE
    SET service = EXCLUDED.service, schedule = EXCLUDED.schedule, parameters = EXCLUDED.parameters,
        start_time = EXCLUDED.start_time, end_time = NULL;
-- end
UPDATE job_state SET end_time = now() WHERE name = ?;
```

### Worked example: MongoDB

The job's name is the document's `_id`, which MongoDB always indexes uniquely, and the parameters, with
`_origin` among them, are a sub-document:

```java
private static final long WINDOW = 30_000;
// MAJORITY: a failover of the primary cannot lose a claim it acknowledged
private final MongoCollection<Document> states = client.getDatabase("scheduler").getCollection("job_state")
        .withWriteConcern(WriteConcern.MAJORITY);

/** "expires": claim the job for the instance that "_origin" names. */
private boolean claim(String name, Map<String, Object> parameters) {
    var now = new Date();
    var filter = Filters.and(Filters.eq("_id", name), Filters.or(
            Filters.lt("start", new Date(now.getTime() - WINDOW)),
            Filters.eq("parameters._origin", parameters.get("_origin"))));
    var update = Updates.combine(Updates.set("parameters", new Document(parameters)),
            Updates.set("start", now), Updates.unset("end"));
    try {
        states.updateOne(filter, update, new UpdateOptions().upsert(true));
        return true;
    } catch (MongoWriteException e) {
        if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
            // the filter missed, so the upsert's insert met the peer's document for this job
            return false;
        }
        throw e;
    }
}
```

The filter matches when the window has passed or the asking instance holds the claim. When it does not, the
upsert inserts a document with the job's name as its `_id`, which the peer's document already has, and MongoDB
refuses it. The answer to the write is the decision, so nothing is read back. The window is measured on the
instance's clock, which differs from its peers' by far less than 30 seconds. `start` and `end` record the run:

```java
// start: the run that the claim allowed, or an operator's manual run
states.updateOne(Filters.eq("_id", name), Updates.combine(Updates.set("service", service),
        Updates.set("schedule", schedule), Updates.set("parameters", new Document(parameters)),
        Updates.set("start", new Date()), Updates.unset("end")), new UpdateOptions().upsert(true));
// end
states.updateOne(Filters.eq("_id", name), Updates.set("end", new Date()));
```

The [scheduler example](../../examples/scheduler-example) runs this code as an application, with an environment
store and a dashboard on the same database, and tests it against an embedded MongoDB-compatible server:
eight instances asking at the same instant get one winner. For a test drive without Docker,
[`helpers/mongodb-standalone`](../../helpers/mongodb-standalone) runs that server on `127.0.0.1:27017`.

### Other ways to pick one instance

For backward compatibility, the mini-scheduler can perform a "leader election" to select an application
instance using a minimalist service mesh (e.g. cloud.connector=kafka). To enable this feature,
set "leader.election" to true.

(Alternatively, you may also turn on Quartz's cluster mode and configure the database accordingly.)

## Slow answers: retries

At every scheduled point the scheduler asks two functions and waits for their answers: the environment
store (see below) and the state resolver's `expires`. Each question waits 3 seconds. One that is not
answered in time is asked again, up to 3 times, so a function that answers slowly at a cold start is
retried instead of failing the job. Each timed-out attempt is a warning in the application log, and a
question that was answered after a retry says so:

```
WARN  State resolver v1.state.resolver for job demo-task - attempt 1 of 4 timed out after 3000 ms, retrying
INFO  State resolver v1.state.resolver for job demo-task answered at attempt 2 of 4
```

When no attempt is answered, the job is skipped for that firing, with an error and a warning naming it.
An error answer is not retried.

A timed-out question is not cancelled: the resolver still works on it, and only its answer is discarded. So
the claim that a timed-out attempt made belongs to the instance that asks again, and the retry carries the
same `_origin`: the resolver answers `true` again to the instance that holds the claim, which is the `OR`
condition on `_origin` in both worked examples. Without it, the retry finds the claim, answers "not expired",
and the job is skipped for that firing. The scheduler logs a warning when "not expired" follows a timed-out
attempt; on an instance that lost the claim to a peer, that warning is expected.

A parameter key that starts with `_` is reserved for the system: the scheduler ignores one in a job's
own `parameters`, with a warning at startup, and the job's service or flow never receives `_origin`. A
resolver that stores the parameters stores `_origin` with them, so a dashboard that shows them filters
the `_` keys out when it reads them, as the example's `GET /api/schedule/{name}` does.

## Active-active sites: one environment runs the jobs

A scheduler deployed in two sites (Production and DR) active-active fires every job in both; a job that
publishes to the site's Kafka cluster then wakes the DR applications that must stay idle while Production
is up. One property names this instance's environment:

```
scheduler.environment.name=${ENV_NAME:prod}
```

Before 4.12.26 the key was `scheduler.environment`, which is still read with a warning when it stands alone.

The scheduler is **active** when its environment is the *active environment* and on **standby** otherwise:
a scheduled job is skipped with one log line (`Standby: job demo-task skipped - the active environment is
prod, this instance is DR`), and an operator's manual run is still honoured. Both default to `prod`, so an
unconfigured installation behaves as before.

The active environment's **source of truth is a data store**, not a property: the function named by
`scheduler.environment.store` (default `v1.environment.store`), which the application implements against
its own database - the example's `EnvironmentStore` keeps it in MongoDB; PostgreSQL or a distributed cache
serve as well - because Kubernetes restarts a pod without notice. The scheduler reads it **at every
scheduled point**, before a job runs, so one call moves every instance of every site. The operations team
makes that call around a Production maintenance window - to `DR` before, back to `prod` after - through the
application's REST endpoint (`GET`/`POST /api/scheduler/environment` in the example,
`{"active": "DR", "operator": "..."}`). Until an operator sets a value the active environment is `prod`.
Without a registered store the value lives in memory only, and the scheduler says so at startup.

## cron schedules

Cron jobs can be defined in a "cron.yaml" file.

You can change the file path in application.properties:
```
yaml.cron=file:/tmp/config/cron.yaml, classpath:/cron.yaml
```
In this example, it assumes the configuration file is available in "/tmp/config/cron.yaml".
If it is not there, it will use the default one in the classpath. Only the first file found is read,
so a file in "/tmp/config" must list every job.

Note that the one in the classpath is just an example. Please update the YAML configuration accordingly.

## Sample cron.yaml

```
jobs:
  - name: "demo-task"
    description: "execute demo service every 10 seconds"
    cron: "0/10 0/1 * 1/1 * ? *"
    service: "hello.world"
    # optional parameter to tell the service what to do
    parameters:
      hello: "world"
    resolver: "v1.state.resolver"

  - name: "demo-flow"
    description: "execute demo service every 10 seconds"
    cron: "0/10 0/1 * 1/1 * ? *"
    service: "flow://hello-flow"
    # optional parameter to tell the service what to do
    parameters:
      hello: "flow"
    resolver: "v1.state.resolver"
    
deferred.start: true
leader.election: false    
```
In this example, there are two scheduled jobs (demo-task and demo-flow). The scheduler executes
the service "hello.world" and the flow "hello-flow" every 10 seconds.

## Time zone

A cron statement is read in the JVM's default time zone, which is UTC in most containers. UTC has no
daylight saving, so a job whose business hour is local moves by one hour against the local clock twice a
year. Give the job a `timezone` and Quartz reads its statement in that zone:

```
jobs:
  - name: "morning-report"
    description: "09:00 New York time, all year"
    cron: "0 0 9 * * ?"
    timezone: "America/New_York"
    service: "morning.report"
    resolver: "v1.state.resolver"
```

The job runs at 09:00 New York time on both sides of a changeover: 13:00 UTC under daylight saving time
and 14:00 UTC under standard time. Name a region such as `America/New_York`, not an abbreviation; a fixed
offset such as `UTC` or `-05:00` never follows daylight saving. A job without the key keeps the JVM's time
zone, as before. An id the JVM does not know (`America/NewYork`) rejects the job with an error at startup,
rather than run it on GMT, and the startup log names each job's time zone. On a changeover day a job set in
the hour the clocks skip does not run, and a job set in the hour they repeat runs once.

The state resolver receives the schedule for information; the job's name is its unique reference. For a
job that names a time zone, the `schedule` header carries the cron statement followed by ` @` and the zone,
`0 0 9 * * ? @America/New_York`, so the column that holds the schedule holds both. A job that names none
sends the statement alone, as before. No zone id contains `@` or a space, so the last ` @` separates the
two; the suffix adds up to 34 characters (`America/Argentina/ComodRivadavia` is the longest region id).

Keep the resolver's expiry window short. In a region that observes daylight saving, a daily job's runs are
23 hours apart on the day the clocks go forward and 25 hours apart on the day they go back, so a window as
long as the job's period - 24 hours for a daily job - would refuse the run after the spring changeover. The
window only has to stop the instances from running the same fire time: more than 18 seconds plus the
database's latency (see *Running more than one instance*), 30 seconds in the worked examples. The sample
resolver in the unit test serves one instance and 10-second demo jobs, and uses 10 seconds; the scheduler
example's resolver, on MongoDB, uses 30 seconds with one-minute demo jobs.

## Using this library

1. Add the mini-scheduler dependency in pom.xml.
2. Implement a "state resolver" function using the route name "v1.state.resolver". Follow the StateResolver.class
   in the unit test for its contract, and for more than one instance claim the job in `expires` as the worked
   examples in *Running more than one instance* do.
3. Configure a cron.yaml file to schedule your tasks. An example is shown in the unit test's resources folder.

> **Note**: `x.y.z` denotes the current Mercury version shown in the root `pom.xml`.

```xml
<dependency>
    <groupId>org.platformlambda</groupId>
    <artifactId>mini-scheduler</artifactId>
    <version>x.y.z</version>
</dependency>
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
