package com.f0xrge.declarum.dfc.adapter.impl;

import com.f0xrge.declarum.dfc.adapter.AttributeChange;
import com.f0xrge.declarum.dfc.adapter.DfcAdapter;
import com.f0xrge.declarum.dfc.adapter.DifferenceAnalysis;
import com.f0xrge.declarum.dfc.adapter.DifferenceType;
import com.f0xrge.declarum.dfc.adapter.PathChange;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.adapter.SelectorResolution;
import com.f0xrge.declarum.dfc.adapter.SelectorResolutionStatus;
import com.f0xrge.declarum.dfc.repository.ManagedAttributeValueChecker;
import com.f0xrge.declarum.dfc.repository.RepositoryObjectOperations;
import com.f0xrge.declarum.dfc.session.SessionManager;
import com.f0xrge.declarum.manifest.model.ResourceDefinition;
import com.f0xrge.declarum.manifest.model.ResourceSpec;
import com.f0xrge.declarum.manifest.model.SelectorDefinition;
import com.f0xrge.declarum.observability.TelemetryLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SessionBackedDfcAdapter implements DfcAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionBackedDfcAdapter.class);

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

        TelemetryLog.info(LOGGER, "dfc.selector.resolve.start", TelemetryLog.fields(
                "resource.name", resourceDefinition.getName(),
                "selector.type", selectorDefinition.getType()
        ));

        SelectorResolution resolution = sessionManager.execute(session -> {
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

        TelemetryLog.info(LOGGER, "dfc.selector.resolve.result", TelemetryLog.fields(
                "resource.name", resourceDefinition.getName(),
                "selector.status", resolution.getStatus(),
                "repository.object_id", resolution.getObject() == null ? null : resolution.getObject().getObjectId()
        ));
        return resolution;
    }

    @Override
    public DifferenceAnalysis analyzeDifference(ResourceDefinition resourceDefinition, SelectorResolution selectorResolution) {
        Objects.requireNonNull(resourceDefinition, "resourceDefinition is required");
        Objects.requireNonNull(selectorResolution, "selectorResolution is required");

        if (SelectorResolutionStatus.AMBIGUOUS.equals(selectorResolution.getStatus())) {
            DifferenceAnalysis analysis = new DifferenceAnalysis(
                    DifferenceType.INVALID_SELECTION,
                    Map.of(),
                    "Selector resolved more than one object"
            );
            TelemetryLog.info(LOGGER, "dfc.diff.analyze.result", TelemetryLog.fields(
                    "resource.name", resourceDefinition.getName(),
                    "difference.type", analysis.getDifferenceType()
            ));
            return analysis;
        }

        Optional<RepositoryObjectSnapshot> actualObject = Optional.ofNullable(selectorResolution.getObject());

        if (resourceDefinition.isAbsentState()) {
            DifferenceAnalysis analysis = actualObject
                    .map(object -> new DifferenceAnalysis(DifferenceType.DELETE, Map.of(), "Object exists and must be deleted"))
                    .orElseGet(() -> new DifferenceAnalysis(DifferenceType.NO_CHANGES, Map.of(), "Object already absent"));
            TelemetryLog.info(LOGGER, "dfc.diff.analyze.result", TelemetryLog.fields(
                    "resource.name", resourceDefinition.getName(),
                    "difference.type", analysis.getDifferenceType()
            ));
            return analysis;
        }

        if (actualObject.isEmpty()) {
            DifferenceAnalysis analysis = new DifferenceAnalysis(DifferenceType.CREATE, Map.of(), "Object not found and must be created");
            TelemetryLog.info(LOGGER, "dfc.diff.analyze.result", TelemetryLog.fields(
                    "resource.name", resourceDefinition.getName(),
                    "difference.type", analysis.getDifferenceType()
            ));
            return analysis;
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
        Map<String, AttributeChange> attributeChanges = valueChecker.computeChanges(desiredAttributes, existingObject.getAttributes());
        List<PathChange> pathChanges = computePathChanges(spec, existingObject);

        if (attributeChanges.isEmpty() && pathChanges.isEmpty()) {
            DifferenceAnalysis analysis = new DifferenceAnalysis(
                    DifferenceType.NO_CHANGES,
                    Map.of(),
                    List.of(),
                    "Managed attributes and location are already compliant"
            );
            TelemetryLog.info(LOGGER, "dfc.diff.analyze.result", TelemetryLog.fields(
                    "resource.name", resourceDefinition.getName(),
                    "difference.type", analysis.getDifferenceType()
            ));
            return analysis;
        }

        DifferenceAnalysis analysis = new DifferenceAnalysis(
                DifferenceType.UPDATE,
                attributeChanges,
                pathChanges,
                differenceMessage(attributeChanges, pathChanges)
        );
        TelemetryLog.info(LOGGER, "dfc.diff.analyze.result", TelemetryLog.fields(
                "resource.name", resourceDefinition.getName(),
                "difference.type", analysis.getDifferenceType(),
                "difference.attribute_changed_count", analysis.getManagedAttributeChanges().size(),
                "difference.path_changed_count", analysis.getPathChanges().size()
        ));
        return analysis;
    }

    private List<PathChange> computePathChanges(ResourceSpec spec, RepositoryObjectSnapshot existingObject) {
        String desiredPath = spec == null || spec.getLocation() == null ? null : spec.getLocation().getPath();
        if (desiredPath == null || desiredPath.isBlank()) {
            return List.of();
        }

        List<String> currentPaths = existingObject.getFolderPaths();
        if (currentPaths.contains(desiredPath)) {
            return List.of();
        }
        return List.of(new PathChange(currentPaths, desiredPath));
    }

    private String differenceMessage(Map<String, AttributeChange> attributeChanges, List<PathChange> pathChanges) {
        if (!attributeChanges.isEmpty() && !pathChanges.isEmpty()) {
            return "Managed attributes and location differ";
        }
        if (!pathChanges.isEmpty()) {
            return "Managed location differs";
        }
        return "Managed attributes differ";
    }

    @Override
    public RepositoryObjectSnapshot createResource(ResourceDefinition resourceDefinition) {
        Objects.requireNonNull(resourceDefinition, "resourceDefinition is required");
        ResourceSpec spec = Objects.requireNonNull(resourceDefinition.getSpec(), "spec is required for create");

        String objectType = Objects.requireNonNull(spec.getObjectType(), "spec.objectType is required for create");
        Map<String, Object> attributes = spec.getAttributes() == null ? Map.of() : spec.getAttributes();
        String folderPath = spec.getLocation() == null ? null : spec.getLocation().getPath();

        TelemetryLog.info(LOGGER, "dfc.resource.create.start", TelemetryLog.fields(
                "resource.name", resourceDefinition.getName(),
                "repository.object_type", objectType,
                "repository.folder_path", folderPath
        ));

        RepositoryObjectSnapshot createdObject = sessionManager.execute(session -> objectOperations.create(session, objectType, attributes, folderPath));
        TelemetryLog.info(LOGGER, "dfc.resource.create.success", TelemetryLog.fields(
                "resource.name", resourceDefinition.getName(),
                "repository.object_id", createdObject.getObjectId()
        ));
        return createdObject;
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

        TelemetryLog.info(LOGGER, "dfc.resource.update.start", TelemetryLog.fields(
                "resource.name", resourceDefinition.getName(),
                "repository.object_id", actualObject.getObjectId(),
                "attributes.managed_count", safeManagedAttributes.size()
        ));

        RepositoryObjectSnapshot updatedObject = sessionManager.execute(
                session -> objectOperations.updateAttributes(session, actualObject.getObjectId(), safeManagedAttributes)
        );

        TelemetryLog.info(LOGGER, "dfc.resource.update.success", TelemetryLog.fields(
                "resource.name", resourceDefinition.getName(),
                "repository.object_id", updatedObject.getObjectId()
        ));
        return updatedObject;
    }

    @Override
    public void deleteResource(RepositoryObjectSnapshot actualObject) {
        Objects.requireNonNull(actualObject, "actualObject is required");
        TelemetryLog.info(LOGGER, "dfc.resource.delete.start", TelemetryLog.fields("repository.object_id", actualObject.getObjectId()));
        sessionManager.executeVoid(session -> {
            objectOperations.delete(session, actualObject.getObjectId());
            return null;
        });
        TelemetryLog.info(LOGGER, "dfc.resource.delete.success", TelemetryLog.fields("repository.object_id", actualObject.getObjectId()));
    }

}
