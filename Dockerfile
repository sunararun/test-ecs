FROM eclipse-temurin:17-jdk
WORKDIR /app

# Accept the exact filename and path from Jenkins
ARG JAR_NAME
ARG JAR_PATH

# Copy the file from 'target/your-app-version.jar' into the container
COPY ${JAR_PATH} ./

# Persist the clean filename into an environment variable for runtime
ENV RUN_JAR=${JAR_NAME}

# Use the shell form of ENTRYPOINT to dynamically resolve the variable name
ENTRYPOINT java -jar ${RUN_JAR}