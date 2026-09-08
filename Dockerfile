FROM eclipse-temurin:17-jdk
WORKDIR /app

# Jenkins passes the exact path to the target folder artifact
ARG JAR_PATH

# Copy and rename it to a static name inside the container image
COPY ${JAR_PATH} ./app.jar

# Safe exec-form entrypoint that ECS will never misinterpret
ENTRYPOINT ["java", "-jar", "app.jar"]