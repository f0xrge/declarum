package com.f0xrge.declarum.dfc.session;

import com.f0xrge.declarum.observability.TelemetryLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

public class DefaultSessionManager implements SessionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSessionManager.class);

    private final DocumentumSessionFactory sessionFactory;

    public DefaultSessionManager(DocumentumSessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory is required");
    }

    @Override
    public <T> T execute(SessionOperation<T> operation) {
        Objects.requireNonNull(operation, "operation is required");
        TelemetryLog.info(LOGGER, "dfc.session.execute.start", TelemetryLog.fields());

        try (DocumentumSession session = sessionFactory.openSession()) {
            T result = operation.execute(session);
            TelemetryLog.info(LOGGER, "dfc.session.execute.success", TelemetryLog.fields());
            return result;
        } catch (RuntimeException exception) {
            TelemetryLog.error(LOGGER, "dfc.session.execute.failure", TelemetryLog.fields(
                    "error.type", exception.getClass().getSimpleName(),
                    "error.message", exception.getMessage()
            ), exception);
            throw exception;
        }
    }
}
