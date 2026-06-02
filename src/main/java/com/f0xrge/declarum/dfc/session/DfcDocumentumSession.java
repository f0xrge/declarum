package com.f0xrge.declarum.dfc.session;

import com.f0xrge.declarum.dfc.client.DfcReflection;

import java.util.Objects;

public class DfcDocumentumSession implements DocumentumSession {

    private final Object dfcSession;

    public DfcDocumentumSession(Object dfcSession) {
        this.dfcSession = Objects.requireNonNull(dfcSession, "dfcSession is required");
    }

    public Object getDfcSession() {
        return dfcSession;
    }

    @Override
    public String getSessionIdentifier() {
        Object sessionIdentifier = DfcReflection.invoke(dfcSession, "getSessionId");
        return sessionIdentifier == null ? null : sessionIdentifier.toString();
    }

    @Override
    public void close() {
        DfcReflection.invoke(dfcSession, "disconnect");
    }
}
