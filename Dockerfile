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

# --- Splunk & OpenTelemetry Configuration (AWS ECS defaults) ---
# These are defaults baked into the image for ECS/Fargate deployment, where the
# ADOT collector sidecar listens on localhost:4318 (OTLP/HTTP) within the same task.
# Override any of these via ECS task definition environment variables if deploying
# to a different environment (e.g. local Docker, a different collector endpoint).
ENV OTEL_SERVICE_NAME="my-springboot-app" \
    OTEL_EXPORTER_OTLP_ENDPOINT="http://localhost:4318" \
    OTEL_TRACES_EXPORTER="otlp" \
    OTEL_METRICS_EXPORTER="none" \
    OTEL_LOGS_EXPORTER="none" \
    OTEL_SDK_DISABLED="false" \
    OTEL_RESOURCE_ATTRIBUTES="deployment.environment=aws-ecs,sf_environment=aws-ecs"

# Change ownership of the copied files to the non-root user before switching contexts
RUN chown -R spring:spring /app

USER spring:spring

# Start Java with the agent hooked in using environment-variable-backed defaults
ENTRYPOINT ["java", "-javaagent:/app/splunk-otel-javaagent.jar", "-jar", "first-ecs.jar"]