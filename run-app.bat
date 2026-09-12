@echo off
:: 1. Clear any lingering variables from previous sessions
set OTEL_EXPORTER_OTLP_PROTOCOL=
set OTEL_EXPORTER_OTLP_TRACES_PROTOCOL=

:: 2. Set the configuration parameters as native environment variables
set OTEL_SERVICE_NAME=my-springboot-app
set OTEL_METRICS_EXPORTER=none
set OTEL_LOGS_EXPORTER=none
set OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=https://ingest.au0.observability.splunkcloud.com:443
set OTEL_EXPORTER_OTLP_TRACES_PROTOCOL=grpc
set OTEL_EXPORTER_OTLP_HEADERS=X-SF-Token=6ZGQghv3ik0YuTjSmyLHww
set OTEL_RESOURCE_ATTRIBUTES=deployment.environment=local,sf_environment=local

:: 3. Execute the plain clean jar launch
java -javaagent:target/agents/splunk-otel-javaagent.jar -jar target/first-ecs-0.0.1-SNAPSHOT.jar
