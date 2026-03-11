package com.example.docrepo.manifest.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResourceSpec {

    private String objectType;
    private Map<String, Object> attributes = new LinkedHashMap<>();
    private ResourceLocation location;

    public ResourceSpec() {
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public ResourceLocation getLocation() {
        return location;
    }

    public void setLocation(ResourceLocation location) {
        this.location = location;
    }
}
