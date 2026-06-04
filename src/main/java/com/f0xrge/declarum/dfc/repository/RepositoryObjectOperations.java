package com.f0xrge.declarum.dfc.repository;

import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.session.DocumentumSession;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RepositoryObjectOperations {

    List<RepositoryObjectSnapshot> findByQualification(DocumentumSession session, String dqlQualification);

    Optional<RepositoryObjectSnapshot> findByPath(DocumentumSession session, String path);

    RepositoryObjectSnapshot create(
            DocumentumSession session,
            String objectType,
            Map<String, Object> attributes,
            List<String> folderPaths
    );

    RepositoryObjectSnapshot update(
            DocumentumSession session,
            String objectId,
            Map<String, Object> attributes,
            List<String> desiredFolderPaths,
            List<String> managedFolderPathsToRemove
    );

    void delete(DocumentumSession session, String objectId);
}
