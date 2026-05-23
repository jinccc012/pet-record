# Multi-stage build: compile the jar, then run it on a slim JRE.
# Works with `gcloud run deploy --source .` (Cloud Build runs this).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
# Let the JVM use most of the container's memory; exit cleanly on OOM so the
# platform restarts the instance instead of hanging.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
