package com.first.ecs.example.log;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {
    private static final Logger log = LoggerFactory.getLogger(OpenTelemetryConfig.class);
    @Bean
    public SpanProcessor loggingSpanProcessor() {
        // LoggingSpanExporter handles printing structural details of spans directly to standard out
        return SimpleSpanProcessor.create(LoggingSpanExporter.create());
    }


    public void doHeavyWork(String tenant) {
        try {
            // 1. Inject the custom field into the log context (MDC)
            MDC.put("tenantId", tenant);

            // 2. Execute your logs (they will automatically inherit traceId, spanId, AND tenantId)
            log.info("Starting database extraction...");

            // Simulate processing logic
            Thread.sleep(100);

            log.info("Database extraction completed successfully.");

        } catch (InterruptedException e) {
            log.error("Job was interrupted", e);
        } finally {
            // 3. CRUCIAL: Always clean up the MDC context so it doesn't leak into other threads
            MDC.remove("tenantId");
        }
    }
}
