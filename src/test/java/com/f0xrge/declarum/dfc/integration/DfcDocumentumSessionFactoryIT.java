package com.f0xrge.declarum.dfc.integration;

import com.f0xrge.declarum.dfc.session.DfcDocumentumSessionFactory;
import com.f0xrge.declarum.dfc.session.DocumentumSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DfcDocumentumSessionFactoryIT {

    @Test
    void shouldOpenRealDfcSessionFromEnvironmentConfiguration() {
        DocumentumIntegrationTestConfiguration configuration = requireConfiguration();
        DfcDocumentumSessionFactory sessionFactory = new DfcDocumentumSessionFactory(
                configuration.getDocbase(),
                configuration.getUser(),
                configuration.getPassword(),
                configuration.getDomain()
        );

        try (DocumentumSession session = sessionFactory.openSession()) {
            assertNotNull(session.getSessionIdentifier());
        }
    }

    private DocumentumIntegrationTestConfiguration requireConfiguration() {
        return DocumentumIntegrationTestConfiguration.fromEnvironment()
                .orElseGet(() -> {
                    assumeTrue(false, "Documentum integration environment variables are not configured");
                    return null;
                });
    }
}
