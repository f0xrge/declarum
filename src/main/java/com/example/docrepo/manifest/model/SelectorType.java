package com.example.docrepo.manifest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SelectorType {

    QUALIFICATION("qualification"),
    PATH("path");

    private final String value;

    SelectorType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SelectorType fromValue(String value) {
        for (SelectorType selectorType : SelectorType.values()) {
            if (selectorType.value.equalsIgnoreCase(value)) {
                return selectorType;
            }
        }
        throw new IllegalArgumentException("Unsupported selector type: " + value);
    }
}
