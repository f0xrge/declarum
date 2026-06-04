package com.f0xrge.declarum.dfc.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RepositoryObjectSnapshot {

    private final String objectId;
    private final String objectType;
    private final Map<String, Object> attributes;
    private final List<String> folderPaths;

    public RepositoryObjectSnapshot(String objectId, String objectType, Map<String, Object> attributes) {
        this(objectId, objectType, attributes, List.of());
    }

    public RepositoryObjectSnapshot(String objectId, String objectType, Map<String, Object> attributes, List<String> folderPaths) {
        this.objectId = objectId;
        this.objectType = objectType;
        this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
        this.folderPaths = folderPaths == null ? List.of() : new ArrayList<>(folderPaths);
    }

    public String getObjectId() {
        return objectId;
    }

    public String getObjectType() {
        return objectType;
    }

    public Map<String, Object> getAttributes() {
        return new LinkedHashMap<>(attributes);
    }

    public List<String> getFolderPaths() {
        return new ArrayList<>(folderPaths);
    }
}
