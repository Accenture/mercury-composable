# CSV Flow Demo

This is an example application to consume the sample CSV Flow Adapter.

## Library dependency

The pom.xml build script in this example application includes the following 
dependencies:

```xml
<dependency>
    <groupId>org.platformlambda</groupId>
    <artifactId>csv-flow-adapter</artifactId>
    <version>4.2.39</version>
</dependency>
<dependency>
    <groupId>org.platformlambda</groupId>
    <artifactId>event-script-engine</artifactId>
    <version>4.2.39</version>
</dependency>
```

Please build the libraries using maven like this:

```shell
cd sandbox/mercury-composable
mvn clean install
cd examples/csv-flow-adapter
mvn clean install
```

The first one at the root project level builds the foundation core libraries.
The second one in the csv-flow-adapter subproject builds the sample CSV flow adapter.

## Main application

Every application has an entry point. The MainApp in this subproject is the entry point.
You can run it from the IDE by running the main() method. Alternatively, you can build
this application using `mvn clean package` and run the application like this:

```shell
cd sandbox/mercury-composable/examples/csv-flow-demo
mvn clean package
java -jar target/csv-flow-adapter-4.2.39.jar
```

## Testing this application

You can drop one or more CSV files into the "/tmp/staging" folder. The application will execute multiple 
flow instances to process the rows of the CSV files.

Processed CSV files are moved to the "/tmp/archive" folder.
