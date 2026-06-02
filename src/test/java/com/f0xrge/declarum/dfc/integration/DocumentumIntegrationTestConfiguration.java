package com.f0xrge.declarum.dfc.integration;

import java.util.Map;
import java.util.Optional;

final class DocumentumIntegrationTestConfiguration {

    static final String DOCBASE_VARIABLE = "DOCUMENTUM_DOCBASE";
    static final String USER_VARIABLE = "DOCUMENTUM_USER";
    static final String PASSWORD_VARIABLE = "DOCUMENTUM_PASSWORD";
    static final String DOMAIN_VARIABLE = "DOCUMENTUM_DOMAIN";
    static final String TEST_PATH_VARIABLE = "DOCUMENTUM_TEST_PATH";
    static final String TEST_QUALIFICATION_VARIABLE = "DOCUMENTUM_TEST_QUALIFICATION";

    private final String docbase;
    private final String user;
    private final String password;
    private final String domain;
    private final String testPath;
    private final String testQualification;

    private DocumentumIntegrationTestConfiguration(
            String docbase,
            String user,
            String password,
            String domain,
            String testPath,
            String testQualification
    ) {
        this.docbase = docbase;
        this.user = user;
        this.password = password;
        this.domain = domain;
        this.testPath = testPath;
        this.testQualification = testQualification;
    }

    static Optional<DocumentumIntegrationTestConfiguration> fromEnvironment() {
        return from(System.getenv());
    }

    static Optional<DocumentumIntegrationTestConfiguration> from(Map<String, String> environment) {
        String docbase = textOrNull(environment.get(DOCBASE_VARIABLE));
        String user = textOrNull(environment.get(USER_VARIABLE));
        String password = textOrNull(environment.get(PASSWORD_VARIABLE));
        if (docbase == null || user == null || password == null) {
            return Optional.empty();
        }
        return Optional.of(new DocumentumIntegrationTestConfiguration(
                docbase,
                user,
                password,
                textOrNull(environment.get(DOMAIN_VARIABLE)),
                textOrNull(environment.get(TEST_PATH_VARIABLE)),
                textOrNull(environment.get(TEST_QUALIFICATION_VARIABLE))
        ));
    }

    String getDocbase() {
        return docbase;
    }

    String getUser() {
        return user;
    }

    String getPassword() {
        return password;
    }

    String getDomain() {
        return domain;
    }

    Optional<String> getTestPath() {
        return Optional.ofNullable(testPath);
    }

    Optional<String> getTestQualification() {
        return Optional.ofNullable(testQualification);
    }

    private static String textOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
