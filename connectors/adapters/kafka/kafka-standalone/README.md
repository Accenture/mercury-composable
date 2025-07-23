# Kafka standalone server

This is provided as a convenient application to run Kafka as a standalone server.
 
It uses the "/tmp" directory to store working files. This Kafka standalone server is designed to simplify 
software development and testing and should not be used for production purpose.

Note that when you restart the Kafka standalone server, all topics will be deleted. This is intentional 
because the kafka standalone server is designed for dev and testing only.

Please note that this tool is not designed for production use.

## Dual servers

By default, this application starts a single standalone Kafka server at port 9092.

You can also configure it to start two standalone Kafka servers if you want to test your application code
with more than one server.

The first and second servers will use address 127.0.0.1:9092 and 127.0.0.1:8092 respectively.

To do this, please set `dual.servers=true` in application.properties.

```properties
dual.servers=true
```

Alternatively, you can instruct this application to start two Kafka servers using run-time parameter like this:

```shell
java -Ddual.servers=true -jar target/kafka-standalone-4.3.5.jar
```

## Using docker

If you are using Windows machine, the best way to run this kafka standalone server is to dockerize it. 
Please make sure your base image uses Java version 21.

```
docker build -t kafka-standalone .
docker run -p 9092:9092 -p 2181:2181 kafka-standalone
```

After this step, you can start/stop it from the Docker Desktop app.
