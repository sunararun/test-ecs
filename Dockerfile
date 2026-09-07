FROM eclipse-temurin:17-jdk
WORKDIR /app

# Jenkins will pass the exact file path here
ARG JAR_FILE 
RUN if [ -z "$JAR_FILE" ]; then echo "JAR_FILE arg is required" && exit 1; fi

# Copy the file and preserve its original name in the container
COPY ${JAR_FILE} ./

# Store the filename in an ENV variable so ENTRYPOINT can read it at runtime
ENV RUN_JAR=${JAR_FILE}

# Use shell form for ENTRYPOINT to properly expand the ENV variable
ENTRYPOINT java -jar ${RUN_JAR}