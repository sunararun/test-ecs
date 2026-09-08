# === Stage 1: Build the application ===
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# === Stage 2: Run the application (Optimised for Size) ===
# Using a slim JRE Alpine base image instead of a full JDK
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user for better ECS security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the generated JAR from the build stage
COPY --from=build /app/target/*.jar ./first-ecs.jar

ENTRYPOINT ["java", "-jar", "first-ecs.jar"]
