package com.f0xrge.declarum.dfc.adapter.impl;

import com.f0xrge.declarum.dfc.adapter.DfcAdapter;
import com.f0xrge.declarum.dfc.adapter.DifferenceAnalysis;
import com.f0xrge.declarum.dfc.adapter.DifferenceType;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.adapter.SelectorResolution;
import com.f0xrge.declarum.dfc.adapter.SelectorResolutionStatus;
import com.f0xrge.declarum.dfc.repository.ManagedAttributeValueChecker;
import com.f0xrge.declarum.dfc.repository.RepositoryObjectOperations;
import com.f0xrge.declarum.dfc.session.SessionManager;
import com.f0xrge.declarum.manifest.model.ResourceDefinition;
import com.f0xrge.declarum.manifest.model.ResourceSpec;
import com.f0xrge.declarum.manifest.model.SelectorDefinition;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SessionBackedDfcAdapter implements DfcAdapter {

    private final SessionManager sessionManager;
    private final RepositoryObjectOperations objectOperations;
    private final ManagedAttributeValueChecker valueChecker;

    public SessionBackedDfcAdapter(
            SessionManager sessionManager,
            RepositoryObjectOperations objectOperations,
            ManagedAttributeValueChecker valueChecker
    ) {
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager is required");
        this.objectOperations = Objects.requireNonNull(objectOperations, "objectOperations is required");
        this.valueChecker = Objects.requireNonNull(valueChecker, "valueChecker is required");
    }

    @Override
    public SelectorResolution resolveBySelector(ResourceDefinition resourceDefinition) {
        Objects.requireNonNull(resourceDefinition, "resourceDefinition is required");
        SelectorDefinition selectorDefinition = Objects.requireNonNull(resourceDefinition.getSelector(), "selector is required");

        return sessionManager.execute(session -> {
            if (selectorDefinition.isQualificationSelector()) {
                List<RepositoryObjectSnapshot> matches = objectOperations.findByQualification(session, selectorDefinition.getDql());
                if (matches.isEmpty()) {
                    return SelectorResolution.notFound();
                }
                if (matches.size() > 1) {
                    return SelectorResolution.ambiguous();
                }
                return SelectorResolution.found(matches.get(0));
            }

            if (selectorDefinition.isPathSelector()) {
                return objectOperations.findByPath(session, selectorDefinition.getPath())
                        .map(SelectorResolution::found)
                        .orElseGet(SelectorResolution::notFound);
            }

            throw new IllegalArgumentException("Unsupported selector type");
        });
    }

    @Override
    public DifferenceAnalysis analyzeDifference(ResourceDefinition resourceDefinition, Optional<RepositoryObjectSnapshot> actualObject) {
        Objects.requireNonNull(resourceDefinition, "resourceDefinition is required");
        Objects.requireNonNull(actualObject, "actualObject is required");

        if (resourceDefinition.isAbsentState()) {
            return actualObject
                    .map(object -> new DifferenceAnalysis(DifferenceType.DELETE, Map.of(), "Object exists and must be deleted"))
                    .orElseGet(() -> new DifferenceAnalysis(DifferenceType.NO_CHANGES, Map.of(), "Object already absent"));
        }

        if (actualObject.isEmpty()) {
            return new DifferenceAnalysis(DifferenceType.CREATE, Map.of(), "Object not found and must be created");
        }

        RepositoryObjectSnapshot existingObject = actualObject.get();
        ResourceSpec spec = resourceDefinition.getSpec();
        String expectedType = resourceDefinition.getSelector() == null ? null : resourceDefinition.getSelector().getExpectedType();

        if (expectedType != null && !expectedType.equals(existingObject.getObjectType())) {
            return new DifferenceAnalysis(
                    DifferenceType.INVALID_SELECTION,
                    Map.of(),
                    "Resolved object type does not match selector.expectedType"
            );
        }

        if (spec != null && spec.getObjectType() != null && !spec.getObjectType().equals(existingObject.getObjectType())) {
            return new DifferenceAnalysis(
                    DifferenceType.INVALID_SELECTION,
                    Map.of(),
                    "Resolved object type does not match spec.objectType"
            );
        }

        Map<String, Object> desiredAttributes = spec == null || spec.getAttributes() == null ? Map.of() : spec.getAttributes();
        Map<String, ?> changes = valueChecker.computeChanges(desiredAttributes, existingObject.getAttributes());

        if (changes.isEmpty()) {
            return new DifferenceAnalysis(DifferenceType.NO_CHANGES, Map.of(), "Managed attributes are already compliant");
        }

        return new DifferenceAnalysis(DifferenceType.UPDATE, valueChecker.computeChanges(desiredAttributes, existingObject.getAttributes()),
                "Managed attributes differ");
    }

    @Override
    public RepositoryObjectSnapshot createResource(ResourceDefinition resourceDefinition) {
        Objects.requireNonNull(resourceDefinition, "resourceDefinition is required");
        ResourceSpec spec = Objects.requireNonNull(resourceDefinition.getSpec(), "spec is required for create");

        String objectType = Objects.requireNonNull(spec.getObjectType(), "spec.objectType is required for create");
        Map<String, Object> attributes = spec.getAttributes() == null ? Map.of() : spec.getAttributes();
        String folderPath = spec.getLocation() == null ? null : spec.getLocation().getPath();

        return sessionManager.execute(session -> objectOperations.create(session, objectType, attributes, folderPath));
    }

    @Override
    public RepositoryObjectSnapshot updateResource(
            ResourceDefinition resourceDefinition,
            RepositoryObjectSnapshot actualObject,
            DifferenceAnalysis differenceAnalysis
    ) {
        Objects.requireNonNull(resourceDefinition, "resourceDefinition is required");
        Objects.requireNonNull(actualObject, "actualObject is required");
        Objects.requireNonNull(differenceAnalysis, "differenceAnalysis is required");

        Map<String, Object> managedAttributes = Objects.requireNonNull(resourceDefinition.getSpec(), "spec is required for update")
                .getAttributes();
        Map<String, Object> safeManagedAttributes = managedAttributes == null ? Map.of() : managedAttributes;

        return sessionManager.execute(
                session -> objectOperations.updateAttributes(session, actualObject.getObjectId(), safeManagedAttributes)
        );
    }

    @Override
    public void deleteResource(RepositoryObjectSnapshot actualObject) {
        Objects.requireNonNull(actualObject, "actualObject is required");
        sessionManager.executeVoid(session -> {
            objectOperations.delete(session, actualObject.getObjectId());
            return null;
        });
    }

    public DifferenceAnalysis analyzeDifference(ResourceDefinition resourceDefinition, SelectorResolution selectorResolution) {
        Objects.requireNonNull(selectorResolution, "selectorResolution is required");

        if (SelectorResolutionStatus.AMBIGUOUS.equals(selectorResolution.getStatus())) {
            return new DifferenceAnalysis(DifferenceType.INVALID_SELECTION, Map.of(), "Selector resolved more than one object");
        }

        return analyzeDifference(resourceDefinition, Optional.ofNullable(selectorResolution.getObject()));
    }
}
