package com.f0xrge.declarum.dfc.adapter.impl;

import com.f0xrge.declarum.dfc.adapter.DifferenceAnalysis;
import com.f0xrge.declarum.dfc.adapter.DifferenceType;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.adapter.SelectorResolution;
import com.f0xrge.declarum.dfc.adapter.SelectorResolutionStatus;
import com.f0xrge.declarum.dfc.repository.ManagedAttributeValueChecker;
import com.f0xrge.declarum.dfc.repository.RepositoryObjectOperations;
import com.f0xrge.declarum.dfc.session.DocumentumSession;
import com.f0xrge.declarum.dfc.session.SessionManager;
import com.f0xrge.declarum.manifest.model.DesiredState;
import com.f0xrge.declarum.manifest.model.ResourceDefinition;
import com.f0xrge.declarum.manifest.model.ResourceLocation;
import com.f0xrge.declarum.manifest.model.ResourceSpec;
import com.f0xrge.declarum.manifest.model.SelectorDefinition;
import com.f0xrge.declarum.manifest.model.SelectorType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SessionBackedDfcAdapterTest {

    @Test
    void shouldResolveSingleObjectByQualificationAndDetectNoChanges() {
        RecordingRepositoryObjectOperations operations = new RecordingRepositoryObjectOperations();
        operations.qualificationResults = List.of(snapshot("0900001", "dm_document", Map.of("object_name", "main")));

        SessionBackedDfcAdapter adapter = new SessionBackedDfcAdapter(
                new InlineSessionManager(),
                operations,
                new ManagedAttributeValueChecker()
        );

        ResourceDefinition resourceDefinition = presentResource("object_name", "main");
        resourceDefinition.getSelector().setType(SelectorType.QUALIFICATION);
        resourceDefinition.getSelector().setDql("dm_document where object_name = 'main'");

        SelectorResolution resolution = adapter.resolveBySelector(resourceDefinition);
        DifferenceAnalysis difference = adapter.analyzeDifference(resourceDefinition, resolution);

        assertEquals(SelectorResolutionStatus.FOUND, resolution.getStatus());
        assertEquals(DifferenceType.NO_CHANGES, difference.getDifferenceType());
        assertEquals(1, operations.findByQualificationCalls);
    }

    @Test
    void shouldCreateUpdateAndDeleteObjectsThroughRepositoryOperations() {
        RecordingRepositoryObjectOperations operations = new RecordingRepositoryObjectOperations();
        SessionBackedDfcAdapter adapter = new SessionBackedDfcAdapter(
                new InlineSessionManager(),
                operations,
                new ManagedAttributeValueChecker()
        );

        ResourceDefinition createDefinition = presentResource("title", "created-title");
        ResourceLocation location = new ResourceLocation();
        location.setPath("/Cabinet/Test");
        createDefinition.getSpec().setLocation(location);

        RepositoryObjectSnapshot created = adapter.createResource(createDefinition);
        assertEquals("created-id", created.getObjectId());
        assertEquals(1, operations.createCalls);

        ResourceDefinition updateDefinition = presentResource("title", "updated-title");
        RepositoryObjectSnapshot current = snapshot("0900001", "dm_document", Map.of("title", "old-title"));

        DifferenceAnalysis updateDifference = adapter.analyzeDifference(updateDefinition, SelectorResolution.found(current));
        RepositoryObjectSnapshot updated = adapter.updateResource(updateDefinition, current, updateDifference);
        assertEquals("updated-id", updated.getObjectId());
        assertEquals(1, operations.updateCalls);

        adapter.deleteResource(current);
        assertEquals(1, operations.deleteCalls);
    }

    @Test
    void shouldMarkAmbiguousSelectionAsInvalid() {
        RecordingRepositoryObjectOperations operations = new RecordingRepositoryObjectOperations();
        operations.qualificationResults = List.of(
                snapshot("0900001", "dm_document", Map.of()),
                snapshot("0900002", "dm_document", Map.of())
        );

        SessionBackedDfcAdapter adapter = new SessionBackedDfcAdapter(
                new InlineSessionManager(),
                operations,
                new ManagedAttributeValueChecker()
        );

        ResourceDefinition resourceDefinition = presentResource("title", "value");
        SelectorResolution resolution = adapter.resolveBySelector(resourceDefinition);
        DifferenceAnalysis difference = adapter.analyzeDifference(resourceDefinition, resolution);

        assertEquals(SelectorResolutionStatus.AMBIGUOUS, resolution.getStatus());
        assertEquals(DifferenceType.INVALID_SELECTION, difference.getDifferenceType());
    }

    private ResourceDefinition presentResource(String attributeName, Object attributeValue) {
        ResourceDefinition resourceDefinition = new ResourceDefinition();
        resourceDefinition.setName("resource");
        resourceDefinition.setState(DesiredState.PRESENT);

        SelectorDefinition selectorDefinition = new SelectorDefinition();
        selectorDefinition.setType(SelectorType.QUALIFICATION);
        selectorDefinition.setDql("dm_document where object_name = 'resource'");
        selectorDefinition.setExpectedType("dm_document");
        resourceDefinition.setSelector(selectorDefinition);

        ResourceSpec spec = new ResourceSpec();
        spec.setObjectType("dm_document");
        spec.setAttributes(new LinkedHashMap<>(Map.of(attributeName, attributeValue)));
        resourceDefinition.setSpec(spec);

        return resourceDefinition;
    }

    private RepositoryObjectSnapshot snapshot(String objectId, String objectType, Map<String, Object> attributes) {
        return new RepositoryObjectSnapshot(objectId, objectType, attributes);
    }

    private static final class InlineSessionManager implements SessionManager {

        @Override
        public <T> T execute(com.f0xrge.declarum.dfc.session.SessionOperation<T> operation) {
            DocumentumSession session = new DocumentumSession() {
                @Override
                public String getSessionIdentifier() {
                    return "inline-session";
                }

                @Override
                public void close() {
                }
            };

            return operation.execute(session);
        }
    }

    private static final class RecordingRepositoryObjectOperations implements RepositoryObjectOperations {

        private int findByQualificationCalls;
        private int createCalls;
        private int updateCalls;
        private int deleteCalls;

        private List<RepositoryObjectSnapshot> qualificationResults = new ArrayList<>();

        @Override
        public List<RepositoryObjectSnapshot> findByQualification(DocumentumSession session, String dqlQualification) {
            findByQualificationCalls++;
            return qualificationResults;
        }

        @Override
        public Optional<RepositoryObjectSnapshot> findByPath(DocumentumSession session, String path) {
            return Optional.empty();
        }

        @Override
        public RepositoryObjectSnapshot create(
                DocumentumSession session,
                String objectType,
                Map<String, Object> attributes,
                String folderPath
        ) {
            createCalls++;
            assertNotNull(objectType);
            return new RepositoryObjectSnapshot("created-id", objectType, attributes);
        }

        @Override
        public RepositoryObjectSnapshot updateAttributes(DocumentumSession session, String objectId, Map<String, Object> attributes) {
            updateCalls++;
            return new RepositoryObjectSnapshot("updated-id", "dm_document", attributes);
        }

        @Override
        public void delete(DocumentumSession session, String objectId) {
            deleteCalls++;
        }
    }
}
