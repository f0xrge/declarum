package com.f0xrge.declarum.dfc.integration;

import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.repository.DfcRepositoryObjectOperations;
import com.f0xrge.declarum.dfc.session.DfcDocumentumSessionFactory;
import com.f0xrge.declarum.dfc.session.DocumentumSession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DfcRepositoryObjectOperationsIT {

    @Test
    void shouldFindRepositoryObjectByPathWhenTestPathIsConfigured() {
        DocumentumIntegrationTestConfiguration configuration = requireConfiguration();
        String path = configuration.getTestPath()
                .orElseGet(() -> {
                    assumeTrue(false, "DOCUMENTUM_TEST_PATH is not configured");
                    return null;
                });

        DfcRepositoryObjectOperations operations = new DfcRepositoryObjectOperations();
        try (DocumentumSession session = sessionFactory(configuration).openSession()) {
            Optional<RepositoryObjectSnapshot> snapshot = operations.findByPath(session, path);
            assertTrue(snapshot.isPresent());
            assertFalse(snapshot.orElseThrow().getObjectId().isBlank());
        }
    }

    @Test
    void shouldFindRepositoryObjectsByQualificationWhenTestQualificationIsConfigured() {
        DocumentumIntegrationTestConfiguration configuration = requireConfiguration();
        String qualification = configuration.getTestQualification()
                .orElseGet(() -> {
                    assumeTrue(false, "DOCUMENTUM_TEST_QUALIFICATION is not configured");
                    return null;
                });

        DfcRepositoryObjectOperations operations = new DfcRepositoryObjectOperations();
        try (DocumentumSession session = sessionFactory(configuration).openSession()) {
            List<RepositoryObjectSnapshot> snapshots = operations.findByQualification(session, qualification);
            assertFalse(snapshots.isEmpty());
            assertFalse(snapshots.get(0).getObjectId().isBlank());
        }
    }

    private DfcDocumentumSessionFactory sessionFactory(DocumentumIntegrationTestConfiguration configuration) {
        return new DfcDocumentumSessionFactory(
                configuration.getDocbase(),
                configuration.getUser(),
                configuration.getPassword(),
                configuration.getDomain()
        );
    }

    private DocumentumIntegrationTestConfiguration requireConfiguration() {
        return DocumentumIntegrationTestConfiguration.fromEnvironment()
                .orElseGet(() -> {
                    assumeTrue(false, "Documentum integration environment variables are not configured");
                    return null;
                });
    }
}
