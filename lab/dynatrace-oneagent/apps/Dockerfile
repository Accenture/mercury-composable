# One of the lab's three Java processes, on the base image (Ubuntu 26.04, Java 25). OneAgent is installed in
# Colima's VM and injects itself into the JVM when it starts, so the image carries no agent. The jar keeps its
# own name, so Dynatrace sees three process groups rather than one.
FROM mercury-lab/ubuntu-java:26.04-25
ARG APP
ENV APP=${APP}
COPY ${APP}.jar /app/${APP}.jar
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/${APP}.jar"]
