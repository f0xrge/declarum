package com.f0xrge.declarum.dfc.session;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultSessionManagerTest {

    @Test
    void shouldOpenAndCloseSessionWhenOperationSucceeds() {
        RecordingSessionFactory factory = new RecordingSessionFactory();
        DefaultSessionManager sessionManager = new DefaultSessionManager(factory);

        String value = sessionManager.execute(session -> session.getSessionIdentifier());

        assertEquals("session-1", value);
        assertEquals(1, factory.openedSessions.get());
        assertEquals(1, factory.closedSessions.get());
    }

    @Test
    void shouldCloseSessionWhenOperationFails() {
        RecordingSessionFactory factory = new RecordingSessionFactory();
        DefaultSessionManager sessionManager = new DefaultSessionManager(factory);

        assertThrows(IllegalStateException.class,
                () -> sessionManager.execute(session -> {
                    throw new IllegalStateException("expected failure");
                }));

        assertEquals(1, factory.openedSessions.get());
        assertEquals(1, factory.closedSessions.get());
    }

    private static final class RecordingSessionFactory implements DocumentumSessionFactory {

        private final AtomicInteger openedSessions = new AtomicInteger();
        private final AtomicInteger closedSessions = new AtomicInteger();

        @Override
        public DocumentumSession openSession() {
            int index = openedSessions.incrementAndGet();
            return new DocumentumSession() {
                @Override
                public String getSessionIdentifier() {
                    return "session-" + index;
                }

                @Override
                public void close() {
                    closedSessions.incrementAndGet();
                }
            };
        }
    }
}
