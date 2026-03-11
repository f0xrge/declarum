package com.f0xrge.declarum.manifest;

import com.f0xrge.declarum.manifest.model.DesiredState;
import com.f0xrge.declarum.manifest.model.ManifestDefinition;
import com.f0xrge.declarum.manifest.model.ResourceType;
import com.f0xrge.declarum.manifest.model.SelectorType;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ManifestReaderTest {

    @Test
    void shouldReadValidManifest() throws Exception {
        ManifestReader manifestReader = new ManifestReader();
        Path manifestPath = getResourcePath("manifests/valid-manifest.yaml");

        ManifestDefinition manifest = manifestReader.read(manifestPath);

        assertNotNull(manifest);
        assertEquals("docrepo/v1alpha1", manifest.getApiVersion());
        assertEquals("Manifest", manifest.getKind());
        assertNotNull(manifest.getMetadata());
        assertEquals("billing-config", manifest.getMetadata().getName());
        assertEquals(2, manifest.getResources().size());

        assertEquals("app-config-main", manifest.getResources().get(0).getName());
        assertEquals(ResourceType.SYSOBJECT, manifest.getResources().get(0).getResourceType());
        assertEquals(DesiredState.PRESENT, manifest.getResources().get(0).getState());
        assertEquals(SelectorType.QUALIFICATION, manifest.getResources().get(0).getSelector().getType());
        assertEquals("my_app_config", manifest.getResources().get(0).getSpec().getObjectType());

        assertEquals("obsolete-config", manifest.getResources().get(1).getName());
        assertEquals(DesiredState.ABSENT, manifest.getResources().get(1).getState());
    }

    private Path getResourcePath(String resourceName) throws URISyntaxException {
        return Path.of(Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI());
    }
}
