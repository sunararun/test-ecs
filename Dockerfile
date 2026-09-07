FROM eclipse-temurin:17-jdk
WORKDIR /app

# Jenkins passes the file path (e.g., target\myapp-1.0.jar)
ARG JAR_FILE

# Copy the file into the container's current working directory (/app)
COPY ${JAR_FILE} ./

# Extract just the filename (e.g., myapp-1.0.jar) and store it in an ENV variable
# This converts Windows backslashes (\) to forward slashes (/) so Linux can parse it
RUN export CLEAN_PATH=$(echo "${JAR_FILE}" | tr '\\' '/') && \
    echo "JAR_NAME=$(basename $CLEAN_PATH)" >> /etc/environment

# Use shell form for ENTRYPOINT to load the saved environment variable
ENTRYPOINT . /etc/environment && java -jar "$JAR_NAME"