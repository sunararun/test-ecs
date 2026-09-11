package com.first.ecs.example.exception;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Exception ex) {
        Span currentSpan = Span.current();
        
        // 1. Record the full stack trace inside the active OpenTelemetry span
        currentSpan.recordException(ex);
        
        // 2. Explicitly flag the trace as failed
        currentSpan.setStatus(StatusCode.ERROR, ex.getMessage());
        
        return "An internal error occurred: " + ex.getMessage();
    }
}