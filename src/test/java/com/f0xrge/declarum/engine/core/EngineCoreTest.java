package com.f0xrge.declarum.engine.core;

import com.f0xrge.declarum.dfc.adapter.DfcAdapter;
import com.f0xrge.declarum.dfc.adapter.DifferenceAnalysis;
import com.f0xrge.declarum.dfc.adapter.DifferenceType;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.adapter.SelectorResolution;
import com.f0xrge.declarum.manifest.ManifestReader;
import com.f0xrge.declarum.manifest.model.ResourceDefinition;
import com.f0xrge.declarum.manifest.validation.ManifestValidationException;
import com.f0xrge.declarum.manifest.validation.ManifestValidator;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EngineCoreTest {

    @Test
    void shouldAnalyzeManifestAndDelegateDifferenceAnalysisToDfcAdapter() throws Exception {
        RecordingDfcAdapter recordingDfcAdapter = new RecordingDfcAdapter();
        EngineCore engineCore = new EngineCore(new ManifestReader(), new ManifestValidator(), recordingDfcAdapter);

        ManifestAnalysisResult result = engineCore.analyze(getResourcePath("manifests/valid-manifest.yaml"));

        assertEquals("billing-config", result.getManifestName());
        assertEquals(2, result.getResources().size());
        assertEquals(2, recordingDfcAdapter.resolvedResources.size());
        assertEquals(2, recordingDfcAdapter.analyzedResources.size());

        assertEquals("app-config-main", result.getResources().get(0).getResourceName());
        assertEquals(DifferenceType.UPDATE, result.getResources().get(0).getDifferenceAnalysis().getDifferenceType());
        assertEquals("obsolete-config", result.getResources().get(1).getResourceName());
        assertEquals(DifferenceType.DELETE, result.getResources().get(1).getDifferenceAnalysis().getDifferenceType());
    }

    @Test
    void shouldFailFastWhenManifestIsInvalid() {
        RecordingDfcAdapter recordingDfcAdapter = new RecordingDfcAdapter();
        EngineCore engineCore = new EngineCore(new ManifestReader(), new ManifestValidator(), recordingDfcAdapter);

        assertThrows(ManifestValidationException.class,
                () -> engineCore.analyze(getResourcePath("manifests/invalid/invalid-state-spec-combination.yaml")));
        assertEquals(0, recordingDfcAdapter.resolvedResources.size());
        assertEquals(0, recordingDfcAdapter.analyzedResources.size());
    }

    private Path getResourcePath(String resourceName) throws URISyntaxException {
        return Path.of(Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI());
    }

    private static class RecordingDfcAdapter implements DfcAdapter {

        private final List<String> resolvedResources = new ArrayList<>();
        private final List<String> analyzedResources = new ArrayList<>();

        @Override
        public SelectorResolution resolveBySelector(ResourceDefinition resourceDefinition) {
            resolvedResources.add(resourceDefinition.getName());

            if (resourceDefinition.isPresentState()) {
                Map<String, Object> actualAttributes = new LinkedHashMap<>();
                actualAttributes.put("object_name", "old-main-config");
                return SelectorResolution.found(new RepositoryObjectSnapshot("0900000000000001", "my_app_config", actualAttributes));
            }

            return SelectorResolution.found(new RepositoryObjectSnapshot("0900000000000002", "dm_document", Map.of()));
        }

        @Override
        public DifferenceAnalysis analyzeDifference(ResourceDefinition resourceDefinition, Optional<RepositoryObjectSnapshot> actualObject) {
            analyzedResources.add(resourceDefinition.getName());

            if (resourceDefinition.isPresentState()) {
                return new DifferenceAnalysis(DifferenceType.UPDATE, Map.of(), "Managed attributes differ");
            }

            return new DifferenceAnalysis(DifferenceType.DELETE, Map.of(), "Object exists and must be deleted");
        }

        @Override
        public RepositoryObjectSnapshot createResource(ResourceDefinition resourceDefinition) {
            throw new UnsupportedOperationException("Not required in this test");
        }

        @Override
        public RepositoryObjectSnapshot updateResource(
                ResourceDefinition resourceDefinition,
                RepositoryObjectSnapshot actualObject,
                DifferenceAnalysis differenceAnalysis
        ) {
            throw new UnsupportedOperationException("Not required in this test");
        }

        @Override
        public void deleteResource(RepositoryObjectSnapshot actualObject) {
            throw new UnsupportedOperationException("Not required in this test");
        }
    }
}
