package com.f0xrge.declarum.dfc.session;

import java.util.Objects;

public class DefaultSessionManager implements SessionManager {

    private final DocumentumSessionFactory sessionFactory;

    public DefaultSessionManager(DocumentumSessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "sessionFactory is required");
    }

    @Override
    public <T> T execute(SessionOperation<T> operation) {
        Objects.requireNonNull(operation, "operation is required");

        try (DocumentumSession session = sessionFactory.openSession()) {
            return operation.execute(session);
        }
    }
}
