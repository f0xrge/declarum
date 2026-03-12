package com.f0xrge.declarum.dfc.session;

public interface DocumentumSession extends AutoCloseable {

    String getSessionIdentifier();

    @Override
    void close();
}
