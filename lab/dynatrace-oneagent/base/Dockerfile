# The OneAgent lab's base image: Ubuntu 26.04 LTS with Java 25.
# The JDK, not the JRE, so jcmd and jstack can inspect the virtual threads during the experiment.
FROM ubuntu:26.04

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-25-openjdk-arm64

RUN apt-get update \
 && apt-get install -y --no-install-recommends openjdk-25-jdk-headless ca-certificates curl unzip \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
CMD ["java", "-version"]
