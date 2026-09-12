# === Stage 1: Build the application ===
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# Download dependencies, package the app, and let the maven-dependency-plugin download the agent jar
RUN mvn clean package -DskipTests

# === Stage 2: Run the application (Optimised for Size) ===
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user for better ECS security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the generated JAR from the build stage
COPY --from=build /app/target/*.jar ./first-ecs.jar

# Copy the agent jar pulled by your pom.xml maven-dependency-plugin
COPY --from=build /app/target/agents/splunk-otel-javaagent.jar ./splunk-otel-javaagent.jar

# --- Splunk & OpenTelemetry Configurations ---
# --- Splunk & OpenTelemetry Configurations ---
# --- Splunk & OpenTelemetry Configurations ---
# --  OTEL_EXPORTER_OTLP_HEADERS="X-SF-Token=6ZGQghv3ik0YuTjSmyLHww" \
# for local deployment place it after  OTEL_EXPORTER_OTLP_ENDPOINT
ENV OTEL_SERVICE_NAME="my-springboot-app" \
    OTEL_METRICS_EXPORTER="otlp" \
    OTEL_LOGS_EXPORTER="none" \
    OTEL_EXPORTER_OTLP_PROTOCOL="grpc" \
    OTEL_EXPORTER_OTLP_ENDPOINT="https://ingest.au0.observability.splunkcloud.com:443" \

    OTEL_RESOURCE_ATTRIBUTES="deployment.environment=local,sf_environment=local"

# Change ownership of the copied files to the non-root user before switching contexts
RUN chown -R spring:spring /app

USER spring:spring

# Start Java with the agent hooked in using environment-variable-backed defaults
ENTRYPOINT ["java", "-javaagent:/app/splunk-otel-javaagent.jar", "-jar", "first-ecs.jar"]
