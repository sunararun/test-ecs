package com.first.ecs.example.log;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

    @Bean
    public SpanProcessor loggingSpanProcessor() {
        // LoggingSpanExporter handles printing structural details of spans directly to standard out
        return SimpleSpanProcessor.create(LoggingSpanExporter.create());
    }
}
