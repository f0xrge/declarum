package com.f0xrge.declarum.dfc.repository;

import com.documentum.fc.common.DfId;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.session.DfcDocumentumSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DfcRepositoryObjectOperationsTest {

    @Test
    void shouldUpdateAttributesAndLocationsInOneSave() {
        FakeDfcObject object = new FakeDfcObject("0900001", "dm_document", List.of("/Cabinet/Old", "/Cabinet/Unmanaged"));
        FakeDfcSession fakeSession = new FakeDfcSession(object);
        DfcRepositoryObjectOperations operations = new DfcRepositoryObjectOperations();

        RepositoryObjectSnapshot snapshot = operations.update(
                new DfcDocumentumSession(fakeSession),
                "0900001",
                Map.of("object_name", "main"),
                List.of("/Cabinet/Expected"),
                List.of("/Cabinet/Old")
        );

        assertEquals(Map.of("object_name", "main"), object.attributes);
        assertEquals(List.of("/Cabinet/Expected"), object.linkedPaths);
        assertEquals(List.of("/Cabinet/Old"), object.unlinkedPaths);
        assertEquals(1, object.saveCalls);
        assertEquals(List.of("/Cabinet/Unmanaged", "/Cabinet/Expected"), snapshot.getFolderPaths());
    }

    private static final class FakeDfcSession {

        private final FakeDfcObject object;

        private FakeDfcSession(FakeDfcObject object) {
            this.object = object;
            object.session = this;
        }

        public FakeDfcObject getObject(DfId objectId) {
            if (object.objectId.equals(objectId.toString())) {
                return object;
            }
            return null;
        }

        public FakeFolder getObject(String folderPath) {
            return new FakeFolder(folderPath);
        }
    }

    private static final class FakeDfcObject {

        private final String objectId;
        private final String objectType;
        private final List<String> folderPaths = new ArrayList<>();
        private final Map<String, Object> attributes = new LinkedHashMap<>();
        private final List<String> linkedPaths = new ArrayList<>();
        private final List<String> unlinkedPaths = new ArrayList<>();
        private FakeDfcSession session;
        private int saveCalls;

        private FakeDfcObject(String objectId, String objectType, List<String> folderPaths) {
            this.objectId = objectId;
            this.objectType = objectType;
            this.folderPaths.addAll(folderPaths);
        }

        public FakeObjectId getObjectId() {
            return new FakeObjectId(objectId);
        }

        public String getTypeName() {
            return objectType;
        }

        public int getAttrCount() {
            return 0;
        }

        public void setString(String attributeName, String value) {
            attributes.put(attributeName, value);
        }

        public int getFolderIdCount() {
            return folderPaths.size();
        }

        public String getFolderId(int index) {
            return folderPaths.get(index);
        }

        public FakeDfcSession getSession() {
            return session;
        }

        public void link(String folderPath) {
            linkedPaths.add(folderPath);
            folderPaths.add(folderPath);
        }

        public void unlink(String folderPath) {
            unlinkedPaths.add(folderPath);
            folderPaths.remove(folderPath);
        }

        public void save() {
            saveCalls++;
        }
    }

    private record FakeObjectId(String value) {
        @Override
        public String toString() {
            return value;
        }
    }

    private record FakeFolder(String folderPath) {
        public int getFolderPathCount() {
            return 1;
        }

        public String getFolderPath(int index) {
            return folderPath;
        }
    }
}
