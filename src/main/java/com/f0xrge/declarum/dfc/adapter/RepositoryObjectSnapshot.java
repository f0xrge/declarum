package com.f0xrge.declarum.dfc.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

public class RepositoryObjectSnapshot {

    private final String objectId;
    private final String objectType;
    private final Map<String, Object> attributes;

    public RepositoryObjectSnapshot(String objectId, String objectType, Map<String, Object> attributes) {
        this.objectId = objectId;
        this.objectType = objectType;
        this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
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
}
