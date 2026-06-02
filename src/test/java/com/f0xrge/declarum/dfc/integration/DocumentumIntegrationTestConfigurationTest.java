package com.f0xrge.declarum.dfc.integration;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentumIntegrationTestConfigurationTest {

    @Test
    void shouldReturnEmptyConfigurationWhenRequiredEnvironmentVariablesAreMissing() {
        assertTrue(DocumentumIntegrationTestConfiguration.from(Map.of()).isEmpty());
    }

    @Test
    void shouldReadRequiredAndOptionalEnvironmentVariables() {
        DocumentumIntegrationTestConfiguration configuration = DocumentumIntegrationTestConfiguration.from(Map.of(
                DocumentumIntegrationTestConfiguration.DOCBASE_VARIABLE, "repository",
                DocumentumIntegrationTestConfiguration.USER_VARIABLE, "user",
                DocumentumIntegrationTestConfiguration.PASSWORD_VARIABLE, "password",
                DocumentumIntegrationTestConfiguration.DOMAIN_VARIABLE, "domain",
                DocumentumIntegrationTestConfiguration.TEST_PATH_VARIABLE, "/Temp/Object",
                DocumentumIntegrationTestConfiguration.TEST_QUALIFICATION_VARIABLE, "dm_document where object_name = 'Object'"
        )).orElseThrow();

        assertEquals("repository", configuration.getDocbase());
        assertEquals("user", configuration.getUser());
        assertEquals("password", configuration.getPassword());
        assertEquals("domain", configuration.getDomain());
        assertEquals("/Temp/Object", configuration.getTestPath().orElseThrow());
        assertEquals("dm_document where object_name = 'Object'", configuration.getTestQualification().orElseThrow());
    }
}
