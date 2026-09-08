# === Stage 1: Build the application ===
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom.xml and source code from your directory
COPY pom.xml .
COPY src ./src

# Package the application (skipping tests to speed up the process)
RUN mvn clean package -DskipTests

# === Stage 2: Run the application ===
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy the generated heavy/fat JAR directly from the build stage
COPY --from=build /app/target/*.jar ./app.jar

# Safe entrypoint
ENTRYPOINT ["java", "-jar", "app.jar"]
