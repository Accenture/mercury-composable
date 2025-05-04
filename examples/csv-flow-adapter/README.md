# CSV Flow Adapter

This is an example flow adapter to illustrate how to write a new flow adapter.

## Purpose

A Flow adapter decouples a flow from the external world. i.e. A flow does not
need to be aware of the platform and infrastructure. A flow will start when an
event arrives from a flow adapter which can serve requests coming from 
a REST endpoint, an enterprise event system, a file staging area, etc.

The system comes with a built-in HTTP Flow Adapter to convert incoming REST
requests to events because HTTP is the most common API interface.

You can use this flow adapter example as a starting point to design and implement
your own Flow Adapter for your specific requirements. e.g. Kafka, SQS, MQ and
file staging area.

## Solution Design

This example flow adapter is designed in the following steps:

### Flow-Adapter loader

The FlowAdapterLoader.class contains the code to parse a adapter configuration file
and instantiate one or more flow adapter instances.

The sample flow adapter configuration file is defined in the application.properties 
with the following parameter:

```properties
yaml.csv.flow.adapter=classpath:/csv-flow-adapter.yml
```

The configuration file `csv-flow-adapter.yml` may look like this:

```yaml
adapter.flow.csv:
  - staging: 'file:/tmp/staging'
    archive: 'file:/tmp/archive'
    flow: 'file-csv-flow'

  - staging: 's3:/bucket1'
    archive: 's3:/bucket2'
    flow: 's3-csv-flow'
```

The above sample configuration shows two staging areas with their corresponding "flow ID".

One in the local file system for demonstration purpose and one in the S3 object store.
We will implement the local file system path here. The S3 is left as an exercise for you.

The loader starts a flow adapter like this:

```java
CsvFlowAdapter adapter = new CsvFlowAdapter(staging, archive, flowId);
adapter.start();
```

### CSV Flow Adapter

The CSV flow adapter will scan the staging area and read files one by one.
For each file, it will read each row and convert it into a map of key-values.
The CSV parsing algorithm is minimalist for demo purpose. It does not handle
encoding of comma character. For production, you should use a proper CSV parser
instead.

It will then launch a flow instance with the configured flow-id and construct
payload for the flow as the "input" dataset.

In the example flow adapter, the input dataset is constructed like this:

```java
var dataset = new MultiLevelMap();
dataset.setElement("body", data);
dataset.setElement("header.filename", f.getName());
dataset.setElement("header.row", rowNumber);
```

Note that the input dataset must contain at least the "body" section and an
optional "header" section. For some flow adapters like Kafka Flow Adapter, you
may add a "metadata" section to hold topic name, partition and other information
essential for a flow to process.

The processing of the record is a place-holder. It just prints out the key-values onto
the console and does nothing more.

In your real project, the file staging area would be a S3 bucket and you should listen
to S3 file creation event and your Flow Adapter can then pick up the new file from S3.

For each row, you can publish the record into a Kafka topic and have a Kafka Flow Adapter
to listen to each Kafka event from the topic and launch an event flow to process the
record in the Kafka event. This way you can horizontally scale your batch processing with
good resilience design.

### Flow Adapter Loader

This CSV Flow Adapter is designed as a library and it will automatically start when
it is added to your project's library dependencies using maven or gradle.

```java
@MainApplication
public class FlowAdapterLoader implements EntryPoint {

    @Override
    public void start(String[] args) throws Exception {
        // setup code here
    }
}
```

To automatically starts your flow adapter, you can create a class that implements
the `EntryPoint` and annotates with `MainApplication`. The "start" method is the
starting point for your flow adapter to run.

## Unit test for the CSV flow adapter

The unit test will boostrap the system using this:

```java
    @BeforeAll
static void setup() {
    AutoStart.main(new String[0]);
}
```

It will then create the "/tmp/staging" and "/tmp/archive" folders.

When you run the unit test, it will print out application log to indicate successful test assertion.

```text
CsvFlowAdapter:96 - Processed 3 rows from /tmp/staging/sample-data.csv
```

## Packaging this sample CSV Flow Adapter as a library

You can build this project into a library using `mvn clean install`.
