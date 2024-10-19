FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu
EXPOSE 9092
WORKDIR /app
COPY target/kafka-standalone-3.1.2.jar .
ENTRYPOINT ["java","-jar","kafka-standalone-4.0.9.jar"]
