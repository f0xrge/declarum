package com.example.docrepo.manifest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DesiredState {

    PRESENT("present"),
    ABSENT("absent");

    private final String value;

    DesiredState(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DesiredState fromValue(String value) {
        for (DesiredState state : DesiredState.values()) {
            if (state.value.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unsupported desired state: " + value);
    }
}
