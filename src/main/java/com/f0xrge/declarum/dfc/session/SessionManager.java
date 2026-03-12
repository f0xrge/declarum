package com.f0xrge.declarum.dfc.session;

public interface SessionManager {

    <T> T execute(SessionOperation<T> operation);

    default void executeVoid(SessionOperation<Void> operation) {
        execute(operation);
    }
}
