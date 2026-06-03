package com.f0xrge.declarum.engine.core;

import com.f0xrge.declarum.dfc.adapter.DfcAdapter;
import com.f0xrge.declarum.dfc.adapter.DifferenceAnalysis;
import com.f0xrge.declarum.dfc.adapter.DifferenceType;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.adapter.SelectorResolution;
import com.f0xrge.declarum.manifest.model.ResourceDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplyExecutorTest {

    @Test
    void shouldExecuteCreateUpdateDeleteAndSkipNoChanges() {
        RecordingApplyDfcAdapter adapter = new RecordingApplyDfcAdapter();
        ApplyExecutor applyExecutor = new ApplyExecutor(adapter);

        ManifestApplyResult applyResult = applyExecutor.apply(new ManifestAnalysisResult("apply-test", List.of(
                analysis("create-resource", DifferenceType.CREATE, SelectorResolution.notFound()),
                analysis("update-resource", DifferenceType.UPDATE, SelectorResolution.found(snapshot("0900001"))),
                analysis("delete-resource", DifferenceType.DELETE, SelectorResolution.found(snapshot("0900002"))),
                analysis("unchanged-resource", DifferenceType.NO_CHANGES, SelectorResolution.found(snapshot("0900003")))
        )));

        assertEquals("apply-test", applyResult.getManifestName());
        assertEquals(4, applyResult.getResources().size());
        assertEquals(1, adapter.createdResources);
        assertEquals(1, adapter.updatedResources);
        assertEquals(1, adapter.deletedResources);

        assertEquals(ApplyActionType.CREATED, applyResult.getResources().get(0).getActionType());
        assertEquals(ApplyActionType.UPDATED, applyResult.getResources().get(1).getActionType());
        assertEquals(ApplyActionType.DELETED, applyResult.getResources().get(2).getActionType());
        assertEquals(ApplyActionType.NO_OPERATION, applyResult.getResources().get(3).getActionType());
    }

    @Test
    void shouldFailWhenUpdateOrDeleteIsMissingActualObject() {
        RecordingApplyDfcAdapter adapter = new RecordingApplyDfcAdapter();
        ApplyExecutor applyExecutor = new ApplyExecutor(adapter);

        ManifestAnalysisResult manifestWithMissingObject = new ManifestAnalysisResult("apply-test", List.of(
                analysis("broken-update", DifferenceType.UPDATE, SelectorResolution.notFound())
        ));

        assertThrows(IllegalStateException.class, () -> applyExecutor.apply(manifestWithMissingObject));
    }

    private ResourceAnalysisResult analysis(String resourceName, DifferenceType differenceType, SelectorResolution selectorResolution) {
        ResourceDefinition resourceDefinition = new ResourceDefinition();
        resourceDefinition.setName(resourceName);

        return new ResourceAnalysisResult(
                resourceDefinition,
                selectorResolution,
                new DifferenceAnalysis(differenceType, Map.of(), "difference")
        );
    }

    private RepositoryObjectSnapshot snapshot(String objectId) {
        return new RepositoryObjectSnapshot(objectId, "dm_document", Map.of());
    }

    private static class RecordingApplyDfcAdapter implements DfcAdapter {

        private int createdResources;
        private int updatedResources;
        private int deletedResources;

        @Override
        public SelectorResolution resolveBySelector(ResourceDefinition resourceDefinition) {
            throw new UnsupportedOperationException("Not required for apply tests");
        }

        @Override
        public DifferenceAnalysis analyzeDifference(ResourceDefinition resourceDefinition, SelectorResolution selectorResolution) {
            throw new UnsupportedOperationException("Not required for apply tests");
        }

        @Override
        public RepositoryObjectSnapshot createResource(ResourceDefinition resourceDefinition) {
            createdResources++;
            return new RepositoryObjectSnapshot("created", "dm_document", Map.of());
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
