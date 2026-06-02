package com.f0xrge.declarum.dfc.session;

import com.f0xrge.declarum.dfc.client.DfcReflection;

import java.util.Objects;

public class DfcDocumentumSessionFactory implements DocumentumSessionFactory {

    private final String docbase;
    private final String user;
    private final String password;
    private final String domain;

    public DfcDocumentumSessionFactory(String docbase, String user, String password) {
        this(docbase, user, password, null);
    }

    public DfcDocumentumSessionFactory(String docbase, String user, String password, String domain) {
        this.docbase = requireText(docbase, "docbase is required");
        this.user = requireText(user, "user is required");
        this.password = requireText(password, "password is required");
        this.domain = emptyToNull(domain);
    }

    @Override
    public DocumentumSession openSession() {
        Object clientX = DfcReflection.newInstance("com.documentum.com.DfClientX");
        Object client = DfcReflection.invoke(clientX, "getLocalClient");
        Object loginInfo = DfcReflection.invoke(clientX, "getLoginInfo");
        DfcReflection.invoke(loginInfo, "setUser", user);
        DfcReflection.invoke(loginInfo, "setPassword", password);
        if (domain != null) {
            DfcReflection.invoke(loginInfo, "setDomain", domain);
        }
        Object session = DfcReflection.invoke(client, "newSession", docbase, loginInfo);
        return new DfcDocumentumSession(session);
    }

    private static String requireText(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
