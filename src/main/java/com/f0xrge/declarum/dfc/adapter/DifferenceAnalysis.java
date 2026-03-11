package com.f0xrge.declarum.dfc.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

public class DifferenceAnalysis {

    private final DifferenceType differenceType;
    private final Map<String, AttributeChange> managedAttributeChanges;
    private final String message;

    public DifferenceAnalysis(DifferenceType differenceType, Map<String, AttributeChange> managedAttributeChanges, String message) {
        this.differenceType = differenceType;
        this.managedAttributeChanges = managedAttributeChanges == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(managedAttributeChanges);
        this.message = message;
    }

    public DifferenceType getDifferenceType() {
        return differenceType;
    }

    public Map<String, AttributeChange> getManagedAttributeChanges() {
        return new LinkedHashMap<>(managedAttributeChanges);
    }

    public String getMessage() {
        return message;
    }
}
