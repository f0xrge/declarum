package com.f0xrge.declarum.observability;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TelemetryLog {

    private static final String EVENT_KEY = "event";

    private TelemetryLog() {
    }


    public static Map<String, Object> fields(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return Map.of();
        }

        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues must contain key/value pairs");
        }

        Map<String, Object> fields = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            Object key = keyValues[index];
            if (!(key instanceof String stringKey) || stringKey.isBlank()) {
                throw new IllegalArgumentException("Each key must be a non-blank string");
            }
            fields.put(stringKey, keyValues[index + 1]);
        }

        return fields;
    }

    public static void info(Logger logger, String eventName, Map<String, Object> fields) {
        emit(logger, eventName, fields, null, LogLevel.INFO);
    }

    public static void warn(Logger logger, String eventName, Map<String, Object> fields) {
        emit(logger, eventName, fields, null, LogLevel.WARN);
    }

    public static void error(Logger logger, String eventName, Map<String, Object> fields, Throwable throwable) {
        emit(logger, eventName, fields, throwable, LogLevel.ERROR);
    }

    private static void emit(
            Logger logger,
            String eventName,
            Map<String, Object> fields,
            Throwable throwable,
            LogLevel logLevel
    ) {
        Objects.requireNonNull(logger, "logger is required");
        Objects.requireNonNull(eventName, "eventName is required");

        Map<String, Object> safeFields = fields == null ? Map.of() : fields;
        Map<String, Object> enrichedFields = new LinkedHashMap<>();
        enrichedFields.put(EVENT_KEY, eventName);
        enrichedFields.putAll(safeFields);

        writeLog(logger, logLevel, throwable, enrichedFields);
        emitSpanEvent(eventName, enrichedFields);
    }

    private static void writeLog(Logger logger, LogLevel logLevel, Throwable throwable, Map<String, Object> fields) {
        LoggingEventBuilder builder = switch (logLevel) {
            case INFO -> logger.atInfo();
            case WARN -> logger.atWarn();
            case ERROR -> logger.atError();
        };

        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            builder.addKeyValue(entry.getKey(), toScalar(entry.getValue()));
        }

        if (throwable != null) {
            builder.setCause(throwable);
        }

        builder.log("declarum_event");
    }

    private static void emitSpanEvent(String eventName, Map<String, Object> fields) {
        AttributesBuilder attributesBuilder = Attributes.builder();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            attributesBuilder.put(entry.getKey(), toScalar(entry.getValue()));
        }
        Span.current().addEvent(eventName, attributesBuilder.build());
    }

    private static String toScalar(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String stringValue) {
            return stringValue;
        }

        return String.valueOf(value);
    }

    private enum LogLevel {
        INFO,
        WARN,
        ERROR
    }
}
