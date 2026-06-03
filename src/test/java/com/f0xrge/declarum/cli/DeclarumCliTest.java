package com.f0xrge.declarum.cli;

import com.f0xrge.declarum.dfc.adapter.AttributeChange;
import com.f0xrge.declarum.dfc.adapter.DfcAdapter;
import com.f0xrge.declarum.dfc.adapter.DifferenceAnalysis;
import com.f0xrge.declarum.dfc.adapter.DifferenceType;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.adapter.SelectorResolution;
import com.f0xrge.declarum.engine.core.EngineCore;
import com.f0xrge.declarum.manifest.ManifestReader;
import com.f0xrge.declarum.manifest.model.ResourceDefinition;
import com.f0xrge.declarum.manifest.validation.ManifestValidator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeclarumCliTest {

    @Test
    void shouldRunAnalysisAndPrintReadablePlan() throws Exception {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
        RecordingDfcAdapter dfcAdapter = new RecordingDfcAdapter();
        EngineCore engineCore = new EngineCore(new ManifestReader(), new ManifestValidator(), dfcAdapter);
        DeclarumCli cli = new DeclarumCli(
                engineCore,
                new PrintStream(outputBuffer, true, StandardCharsets.UTF_8),
                new PrintStream(errorBuffer, true, StandardCharsets.UTF_8)
        );

        int exitCode = cli.run(new String[]{getResourcePath("manifests/valid-manifest.yaml").toString()});

        assertEquals(0, exitCode);
        String output = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Declarum analysis plan"));
        assertTrue(output.contains("Manifest: billing-config"));
        assertTrue(output.contains("- app-config-main"));
        assertTrue(output.contains("Planned action: UPDATE"));
        assertTrue(output.contains("object_name: old-main-config -> main-config"));
        assertTrue(output.contains("- obsolete-config"));
        assertTrue(output.contains("Planned action: DELETE"));
        assertEquals("", errorBuffer.toString(StandardCharsets.UTF_8));
        assertEquals(List.of("app-config-main", "obsolete-config"), dfcAdapter.resolvedResources);
    }

    @Test
    void shouldRejectMissingManifestArgument() {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
        DeclarumCli cli = new DeclarumCli(
                new EngineCore(new ManifestReader(), new ManifestValidator(), new RecordingDfcAdapter()),
                new PrintStream(outputBuffer, true, StandardCharsets.UTF_8),
                new PrintStream(errorBuffer, true, StandardCharsets.UTF_8)
        );

        int exitCode = cli.run(new String[]{});

        assertEquals(2, exitCode);
        assertEquals("", outputBuffer.toString(StandardCharsets.UTF_8));
        assertTrue(errorBuffer.toString(StandardCharsets.UTF_8).contains("Usage:"));
    }

    private Path getResourcePath(String resourceName) throws URISyntaxException {
        return Path.of(Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI());
    }

    private static class RecordingDfcAdapter implements DfcAdapter {

        private final List<String> resolvedResources = new java.util.ArrayList<>();

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
            if (resourceDefinition.isPresentState()) {
                Map<String, AttributeChange> changes = new LinkedHashMap<>();
                changes.put("object_name", new AttributeChange("old-main-config", "main-config"));
                return new DifferenceAnalysis(DifferenceType.UPDATE, changes, "Managed attributes differ");
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
