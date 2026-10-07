# === Stage 1: Build the application ===
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# === Stage 2: Run the application ===
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/target/first-ecs-0.0.1-SNAPSHOT.jar ./first-ecs.jar
COPY --from=build /app/target/agents/splunk-otel-javaagent.jar /otel/splunk-otel-javaagent.jar

USER spring:spring

ENTRYPOINT ["java", "-jar", "first-ecs.jar"]