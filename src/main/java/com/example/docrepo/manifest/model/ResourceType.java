package com.example.docrepo.manifest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ResourceType {

    SYSOBJECT("sysobject"),
    FOLDER("folder");

    private final String value;

    ResourceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ResourceType fromValue(String value) {
        for (ResourceType resourceType : ResourceType.values()) {
            if (resourceType.value.equalsIgnoreCase(value)) {
                return resourceType;
            }
        }
        throw new IllegalArgumentException("Unsupported resource type: " + value);
    }
}
