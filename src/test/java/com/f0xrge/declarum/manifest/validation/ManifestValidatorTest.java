package com.f0xrge.declarum.manifest.validation;

import com.f0xrge.declarum.manifest.ManifestReader;
import com.f0xrge.declarum.manifest.model.ManifestDefinition;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ManifestValidatorTest {

    private final ManifestReader manifestReader = new ManifestReader();
    private final ManifestValidator manifestValidator = new ManifestValidator();

    @Test
    void shouldValidateWellFormedManifest() throws Exception {
        ManifestDefinition manifest = manifestReader.read(getResourcePath("manifests/valid-manifest.yaml"));

        assertDoesNotThrow(() -> manifestValidator.validate(manifest));
    }

    @Test
    void shouldFailWhenResourceTypeIsMissing() throws Exception {
        ManifestDefinition manifest = manifestReader.read(getResourcePath("manifests/invalid/missing-resource-type.yaml"));

        assertValidationErrorContains(manifest, "resourceType is required");
    }

    @Test
    void shouldFailWhenSpecIsProvidedForAbsentState() throws Exception {
        ManifestDefinition manifest = manifestReader.read(getResourcePath("manifests/invalid/invalid-state-spec-combination.yaml"));

        assertValidationErrorContains(manifest, ".spec is forbidden when state=absent");
    }

    @Test
    void shouldFailWhenPathSelectorContainsDql() throws Exception {
        ManifestDefinition manifest = manifestReader.read(getResourcePath("manifests/invalid/invalid-selector-combination.yaml"));

        assertValidationErrorContains(manifest, ".selector.dql is forbidden when selector.type=path");
    }

    @Test
    void shouldFailWhenAttributesContainNullOrNestedObject() throws Exception {
        ManifestDefinition manifest = manifestReader.read(getResourcePath("manifests/invalid/invalid-attributes.yaml"));

        assertValidationErrorContains(manifest, ".spec.attributes.nullable_value must not be null");
        assertValidationErrorContains(manifest, ".spec.attributes.nested_value must not be a nested object");
    }

    private void assertValidationErrorContains(ManifestDefinition manifest, String expectedFragment) {
        try {
            manifestValidator.validate(manifest);
            fail("Expected ManifestValidationException to be thrown");
        } catch (ManifestValidationException exception) {
            assertTrue(exception.getMessage().contains(expectedFragment));
        }
    }

    private Path getResourcePath(String resourceName) throws URISyntaxException {
        return Path.of(Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI());
    }
}
