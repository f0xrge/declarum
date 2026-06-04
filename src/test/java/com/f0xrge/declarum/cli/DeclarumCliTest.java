package com.f0xrge.declarum.cli;

import com.f0xrge.declarum.dfc.adapter.AttributeChange;
import com.f0xrge.declarum.dfc.adapter.DfcAdapter;
import com.f0xrge.declarum.dfc.adapter.DifferenceAnalysis;
import com.f0xrge.declarum.dfc.adapter.DifferenceType;
import com.f0xrge.declarum.dfc.adapter.PathChange;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.adapter.SelectorResolution;
import com.f0xrge.declarum.dfc.adapter.SelectorResolutionStatus;
import com.f0xrge.declarum.engine.core.ApplyExecutor;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeclarumCliTest {

    @Test
    void shouldRejectInvalidUsage() {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
        RecordingDfcAdapter dfcAdapter = new RecordingDfcAdapter();
        DeclarumCli cli = createCli(dfcAdapter, outputBuffer, errorBuffer);

        int exitCode = cli.run(new String[]{"apply", "one.yaml", "two.yaml"});

        assertEquals(2, exitCode);
        assertEquals("", outputBuffer.toString(StandardCharsets.UTF_8));
        assertTrue(errorBuffer.toString(StandardCharsets.UTF_8).contains("Usage:"));
        assertEquals(List.of(), dfcAdapter.resolvedResources);
        assertEquals(0, dfcAdapter.createdResources);
        assertEquals(0, dfcAdapter.updatedResources);
        assertEquals(0, dfcAdapter.deletedResources);
    }

    @Test
    void shouldRunPlanWithoutApplyingRepositoryModifications() throws Exception {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
        RecordingDfcAdapter dfcAdapter = new RecordingDfcAdapter();
        DeclarumCli cli = createCli(dfcAdapter, outputBuffer, errorBuffer);

        int exitCode = cli.run(new String[]{"plan", getResourcePath("manifests/valid-manifest.yaml").toString()});

        assertEquals(0, exitCode);
        String output = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Declarum analysis plan"));
        assertTrue(output.contains("Manifest: billing-config"));
        assertTrue(output.contains("- app-config-main"));
        assertTrue(output.contains("Planned action: UPDATE"));
        assertTrue(output.contains("object_name: old-main-config -> main-config"));
        assertTrue(output.contains("Path changes:"));
        assertTrue(output.contains("folder path: [/Cabinet/OldConfig] -> /Cabinet/Config"));
        assertTrue(output.contains("- obsolete-config"));
        assertTrue(output.contains("Planned action: DELETE"));
        assertEquals("", errorBuffer.toString(StandardCharsets.UTF_8));
        assertEquals(List.of("app-config-main", "obsolete-config"), dfcAdapter.resolvedResources);
        assertEquals(0, dfcAdapter.createdResources);
        assertEquals(0, dfcAdapter.updatedResources);
        assertEquals(0, dfcAdapter.deletedResources);
    }

    @Test
    void shouldDefaultToPlanWithoutApplyingRepositoryModifications() throws Exception {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
        RecordingDfcAdapter dfcAdapter = new RecordingDfcAdapter();
        DeclarumCli cli = createCli(dfcAdapter, outputBuffer, errorBuffer);

        int exitCode = cli.run(new String[]{getResourcePath("manifests/valid-manifest.yaml").toString()});

        assertEquals(0, exitCode);
        assertTrue(outputBuffer.toString(StandardCharsets.UTF_8).contains("Declarum analysis plan"));
        assertEquals("", errorBuffer.toString(StandardCharsets.UTF_8));
        assertEquals(List.of("app-config-main", "obsolete-config"), dfcAdapter.resolvedResources);
        assertEquals(0, dfcAdapter.createdResources);
        assertEquals(0, dfcAdapter.updatedResources);
        assertEquals(0, dfcAdapter.deletedResources);
    }

    @Test
    void shouldApplyExplicitlyRequestedRepositoryModifications() throws Exception {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
        RecordingDfcAdapter dfcAdapter = new RecordingDfcAdapter();
        DeclarumCli cli = createCli(dfcAdapter, outputBuffer, errorBuffer);

        int exitCode = cli.run(new String[]{"apply", getResourcePath("manifests/valid-manifest.yaml").toString()});

        assertEquals(0, exitCode);
        String output = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Declarum analysis plan"));
        assertTrue(output.contains("Declarum apply summary"));
        assertTrue(output.contains("CREATED: 0"));
        assertTrue(output.contains("UPDATED: 1"));
        assertTrue(output.contains("DELETED: 1"));
        assertTrue(output.contains("NO_OPERATION: 0"));
        assertTrue(output.contains("FAILED: 0"));
        assertEquals("", errorBuffer.toString(StandardCharsets.UTF_8));
        assertEquals(List.of("app-config-main", "obsolete-config"), dfcAdapter.resolvedResources);
        assertEquals(0, dfcAdapter.createdResources);
        assertEquals(1, dfcAdapter.updatedResources);
        assertEquals(1, dfcAdapter.deletedResources);
    }

    @Test
    void shouldFailApplyWhenInvalidSelectionProducesFailedAction() throws Exception {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
        RecordingDfcAdapter dfcAdapter = new RecordingDfcAdapter();
        dfcAdapter.invalidSelection = true;
        DeclarumCli cli = createCli(dfcAdapter, outputBuffer, errorBuffer);

        int exitCode = cli.run(new String[]{"--apply", getResourcePath("manifests/valid-manifest.yaml").toString()});

        assertEquals(1, exitCode);
        String output = outputBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Planned action: INVALID_SELECTION"));
        assertTrue(output.contains("Declarum apply summary"));
        assertTrue(output.contains("FAILED: 1"));
        assertTrue(output.contains("- app-config-main: FAILED - Selector resolved to multiple objects"));
        assertEquals("", errorBuffer.toString(StandardCharsets.UTF_8));
        assertEquals(0, dfcAdapter.createdResources);
        assertEquals(0, dfcAdapter.updatedResources);
        assertEquals(1, dfcAdapter.deletedResources);
    }

    private DeclarumCli createCli(
            RecordingDfcAdapter dfcAdapter,
            ByteArrayOutputStream outputBuffer,
            ByteArrayOutputStream errorBuffer
    ) {
        EngineCore engineCore = new EngineCore(new ManifestReader(), new ManifestValidator(), dfcAdapter);
        return new DeclarumCli(
                engineCore,
                new ApplyExecutor(dfcAdapter),
                new PrintStream(outputBuffer, true, StandardCharsets.UTF_8),
                new PrintStream(errorBuffer, true, StandardCharsets.UTF_8)
        );
    }

    private Path getResourcePath(String resourceName) throws URISyntaxException {
        return Path.of(Thread.currentThread().getContextClassLoader().getResource(resourceName).toURI());
    }

    private static class RecordingDfcAdapter implements DfcAdapter {

        private final List<String> resolvedResources = new java.util.ArrayList<>();
        private boolean invalidSelection;
        private int createdResources;
        private int updatedResources;
        private int deletedResources;

        @Override
        public SelectorResolution resolveBySelector(ResourceDefinition resourceDefinition) {
            resolvedResources.add(resourceDefinition.getName());

            if (invalidSelection && resourceDefinition.isPresentState()) {
                return SelectorResolution.ambiguous();
            }

            if (resourceDefinition.isPresentState()) {
                Map<String, Object> actualAttributes = new LinkedHashMap<>();
                actualAttributes.put("object_name", "old-main-config");
                return SelectorResolution.found(new RepositoryObjectSnapshot(
                        "0900000000000001",
                        "my_app_config",
                        actualAttributes,
                        List.of("/Cabinet/OldConfig")
                ));
            }

            return SelectorResolution.found(new RepositoryObjectSnapshot("0900000000000002", "dm_document", Map.of()));
        }

        @Override
        public DifferenceAnalysis analyzeDifference(ResourceDefinition resourceDefinition, SelectorResolution selectorResolution) {
            if (selectorResolution != null && SelectorResolutionStatus.AMBIGUOUS.equals(selectorResolution.getStatus())) {
                return new DifferenceAnalysis(
                        DifferenceType.INVALID_SELECTION,
                        Map.of(),
                        "Selector resolved to multiple objects"
                );
            }

            if (resourceDefinition.isPresentState()) {
                Map<String, AttributeChange> changes = new LinkedHashMap<>();
                changes.put("object_name", new AttributeChange("old-main-config", "main-config"));
                return new DifferenceAnalysis(
                        DifferenceType.UPDATE,
                        changes,
                        List.of(new PathChange(List.of("/Cabinet/OldConfig"), "/Cabinet/Config")),
                        "Managed attributes and location differ"
                );
            }

            return new DifferenceAnalysis(DifferenceType.DELETE, Map.of(), "Object exists and must be deleted");
        }

        @Override
        public RepositoryObjectSnapshot createResource(ResourceDefinition resourceDefinition) {
            createdResources++;
            return new RepositoryObjectSnapshot("0900000000000003", "my_app_config", Map.of());
        }

        @Override
        public RepositoryObjectSnapshot updateResource(
                ResourceDefinition resourceDefinition,
                RepositoryObjectSnapshot actualObject,
                DifferenceAnalysis differenceAnalysis
        ) {
            updatedResources++;
            return actualObject;
        }

        @Override
        public void deleteResource(RepositoryObjectSnapshot actualObject) {
            deletedResources++;
        }
    }
}
