# CSV Flow Adapter

This is an example flow adapter to illustrate how to write a new flow adapter.

## Purpose

A flow consists of a number of tasks (composable functions) where a flow adapter
will launch a flow and send an event to its first task.

Based on the event flow configuration script, the system will perform 
event choreography by routing event among the tasks until the end of the flow
is reached.

The system comes with a built-in HTTP Flow Adapter to convert incoming REST
requests to events.

Each composable function is self-contained and independent. The system will map
the external input into arguments and parameters to your composable functions.

This decoupling means that your tasks do not need to be aware of the external
world. Therefore, we can connect any flow adapter to any flow provided that
we can map dataset of the input as arguments to your tasks.

The second most popular flow adapter is the "Kafka Flow Adapter". You may
implement your own Kafka Flow Adapter to handle different requirements based on 
specific infrastructure and enterprise architecture governance. We therefore
do not provide a Kafka Flow Adapter out of the box.

This subproject uses a simple use case to walk through the steps to write a new
Flow Adapter.

## Solution Design

Events can come in different forms. The conventional means of event systems are
message conduits such as ActiveMQ and Kafka. REST requests, for example, can be
converted to an event. The built-in HTTP Flow Adapter uses the underlying REST
automation system to convert HTTP requests and responses as events. An HTTP request
is passed as an event to the HTTP Flow Adapter which will launch a corresponding
flow according to REST endpoint definition in the "rest.yaml" configuration file.

For this example, we are treating files in the "File Staging" area as input requests.

We will write a "CSV Flow Adapter" to scan a file staging area to find new files.
For each row in a CSV file, the CSV Flow Adapter will launch an event flow to process
the record from the row.

The processing of the record is a place-holder. It just prints out the key-values onto
the console and does nothing more.

In your real project, the file staging area would be a S3 bucket and you should listen
to S3 file creation event and your Flow Adapter can then pick up the new file from S3.

For each row, you can publish the record into a Kafka topic and have a Kafka Flow Adapter
to listen to each Kafka event from the topic and launch an event flow to process the
record in the Kafka event. This way you can horizontally scale your batch processing with
good resilience design.

## Building a Flow Adapter

We will walk through the steps in building a new Flow Adapter.

### Associate flow-ID with an event trigger

The first thing is to define a configuration file for your Flow Adapter.

For this example, it is shown in the "csv-flow-adapter.yaml". It may look like this:

```yaml
adapter.flow.csv:
  - staging: 'file:/tmp/staging'
    archive: 'file:/tmp/archive'
    flow: 'file-csv-flow'

  - staging: 's3:/bucket1'
    archive: 's3:/bucket2'
    flow: 's3-csv-flow'
```

The above sample configuration shows two staging areas. One in the local file system
for demonstration purpose and one in the S3 object store. We will implement the
local file system path. The S3 is left as an exercise for you.

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

### Processing logic

The flow adapter configuration file is defined in the application.properties with the following parameter:

```properties
yaml.csv.flow.adapter=classpath:/csv-flow-adapter.yml
```

The FlowAdapterLoader will parse the `csv-flow-adapter.yml`. For each staging area, it will
load an instance of the CsvFlowAdapter with the staging/archive folder paths and the flow-id
that is associated with the staging area.

The CsvFlowAdapter is associated with a flow-id. When a CSV file is found in the
configured staging area, it will read each row from the file and then invoke a flow with the flow-id
to process the record from the row.

For simplicity, the CsvFlowAdapter will scan a file folder to see if there are any files in the staging area.
For production, you should consider the adapter to listen to file creation events so that the process is
event driven.

The csv-flow-adapter.yml contains the demo entry below:

```yaml
adapter.flow.csv:
  - staging: 'file:/tmp/staging'
    archive: 'file:/tmp/archive'
    flow: 'file-csv-flow'
```

It states that the staging area is located in the local file system "/tmp/staging" and the archive area
"/tmp/archive". The flow-id for this staging area is `file-csv-flow`

The file-csv-flow.yml is listed in the "flows.yaml" file.

## Testing the CSV flow adapter

To test the CSV flow adapter, you must create the "/tmp/staging" and "/tmp/archive" folders first.

```shell
mkdir /tmp/staging
mkdir /tmp/archive
```

You may then compile and build this application and run the application like this:

```shell
cd flow-adapter-example
mvn clean package
java -jar target/csv-flow-adapter-4.2.38.jar 
```

You will see the application starts with some actuator REST endpoints and load the file-csv-flow.yml
as an event script.

You may then copy the sample-data.csv into the `/tmp/staging` folder. The application will pick up the
file and execute the demo flow for the 3 rows in the sample-data.csv file.

Upon successful processing, the sample-data.csv file will be moved to the archive folder with filename
"sample-data.csv.done".

The console log will show the processing details.

## Packing the module as a library

This flow adapter is packaged as an application for demo purpose.

Please update pom.xml to turn this into a library.

The library should contain only the FlowAdapterLoader and the CsvFlowProcessor classes.

The configuration files in the resources folder and the DemoCsvProcessor should be moved to the "test" folder.
You should write some unit tests to ensure the library meets your requirements.

When you import your library into an application, the FlowAdapterLoader will run automatically provided your
application has the required configuration files such as rest.yaml, flows.yaml, csv-flow-adapter.yml and some
event flow configuration files for the staging area(s).
