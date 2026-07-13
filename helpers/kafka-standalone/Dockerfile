FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu
EXPOSE 9092
WORKDIR /app
COPY target/kafka-standalone-4.8.2-exec.jar .
ENTRYPOINT ["java","-jar","kafka-standalone-4.8.2-exec.jar"]
