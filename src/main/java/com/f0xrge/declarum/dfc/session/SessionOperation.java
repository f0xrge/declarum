package com.f0xrge.declarum.dfc.session;

@FunctionalInterface
public interface SessionOperation<T> {

    T execute(DocumentumSession session);
}
