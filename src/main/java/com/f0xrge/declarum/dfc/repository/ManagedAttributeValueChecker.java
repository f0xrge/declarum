package com.f0xrge.declarum.dfc.repository;

import com.f0xrge.declarum.dfc.adapter.AttributeChange;
import com.f0xrge.declarum.observability.TelemetryLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ManagedAttributeValueChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedAttributeValueChecker.class);

    public Map<String, AttributeChange> computeChanges(Map<String, Object> desiredAttributes, Map<String, Object> actualAttributes) {
        Objects.requireNonNull(desiredAttributes, "desiredAttributes is required");

        Map<String, Object> safeActualAttributes = actualAttributes == null ? Map.of() : actualAttributes;
        Map<String, AttributeChange> changes = new LinkedHashMap<>();

        for (Map.Entry<String, Object> desiredEntry : desiredAttributes.entrySet()) {
            String attributeName = desiredEntry.getKey();
            Object desiredValue = desiredEntry.getValue();
            Object actualValue = safeActualAttributes.get(attributeName);

            if (!Objects.equals(actualValue, desiredValue)) {
                changes.put(attributeName, new AttributeChange(actualValue, desiredValue));
            }
        }

        TelemetryLog.info(LOGGER, "dfc.diff.attributes.computed", TelemetryLog.fields(
                "attributes.managed_count", desiredAttributes.size(),
                "attributes.changed_count", changes.size()
        ));
        return changes;
    }
}
