package com.f0xrge.declarum.dfc.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DifferenceAnalysis {

    private final DifferenceType differenceType;
    private final Map<String, AttributeChange> managedAttributeChanges;
    private final List<PathChange> pathChanges;
    private final String message;

    public DifferenceAnalysis(DifferenceType differenceType, Map<String, AttributeChange> managedAttributeChanges, String message) {
        this(differenceType, managedAttributeChanges, List.of(), message);
    }

    public DifferenceAnalysis(
            DifferenceType differenceType,
            Map<String, AttributeChange> managedAttributeChanges,
            List<PathChange> pathChanges,
            String message
    ) {
        this.differenceType = differenceType;
        this.managedAttributeChanges = managedAttributeChanges == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(managedAttributeChanges);
        this.pathChanges = pathChanges == null ? List.of() : new ArrayList<>(pathChanges);
        this.message = message;
    }

    public DifferenceType getDifferenceType() {
        return differenceType;
    }

    public Map<String, AttributeChange> getManagedAttributeChanges() {
        return new LinkedHashMap<>(managedAttributeChanges);
    }

    public List<PathChange> getPathChanges() {
        return new ArrayList<>(pathChanges);
    }

    public String getMessage() {
        return message;
    }
}
