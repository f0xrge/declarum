package com.f0xrge.declarum.manifest.validation;

import com.f0xrge.declarum.manifest.model.ManifestDefinition;
import com.f0xrge.declarum.manifest.model.ResourceDefinition;
import com.f0xrge.declarum.manifest.model.SelectorDefinition;
import com.f0xrge.declarum.observability.TelemetryLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ManifestValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestValidator.class);
    private static final String SUPPORTED_API_VERSION = "docrepo/v1alpha1";
    private static final String SUPPORTED_KIND = "Manifest";

    public void validate(ManifestDefinition manifest) {
        List<String> errors = new ArrayList<>();
        TelemetryLog.info(LOGGER, "manifest.validation.start", TelemetryLog.fields(
                "manifest.name", manifest == null || manifest.getMetadata() == null ? null : manifest.getMetadata().getName()
        ));

        if (manifest == null) {
            errors.add("Manifest must not be null");
            TelemetryLog.warn(LOGGER, "manifest.validation.failed", TelemetryLog.fields("validation.error_count", errors.size()));
            throw new ManifestValidationException(errors);
        }

        validateTopLevel(manifest, errors);
        validateResources(manifest.getResources(), errors);

        if (!errors.isEmpty()) {
            TelemetryLog.warn(LOGGER, "manifest.validation.failed", TelemetryLog.fields(
                    "manifest.name", manifest.getMetadata() == null ? null : manifest.getMetadata().getName(),
                    "validation.error_count", errors.size()
            ));
            throw new ManifestValidationException(errors);
        }

        TelemetryLog.info(LOGGER, "manifest.validation.success", TelemetryLog.fields(
                "manifest.name", manifest.getMetadata() == null ? null : manifest.getMetadata().getName(),
                "manifest.resource_count", manifest.getResources() == null ? 0 : manifest.getResources().size()
        ));
    }

    private void validateTopLevel(ManifestDefinition manifest, List<String> errors) {
        if (!SUPPORTED_API_VERSION.equals(manifest.getApiVersion())) {
            errors.add("apiVersion must be '" + SUPPORTED_API_VERSION + "'");
        }

        if (!SUPPORTED_KIND.equals(manifest.getKind())) {
            errors.add("kind must be '" + SUPPORTED_KIND + "'");
        }

        if (manifest.getResources() == null || manifest.getResources().isEmpty()) {
            errors.add("resources must not be empty");
        }
    }

    private void validateResources(List<ResourceDefinition> resources, List<String> errors) {
        if (resources == null) {
            return;
        }

        Set<String> resourceNames = new HashSet<>();
        for (int i = 0; i < resources.size(); i++) {
            ResourceDefinition resource = resources.get(i);
            String resourcePath = "resources[" + i + "]";

            if (resource == null) {
                errors.add(resourcePath + " must not be null");
                continue;
            }

            validateResourceName(resource, resourcePath, resourceNames, errors);
            validateResourceType(resource, resourcePath, errors);
            validateResourceStateAndSpec(resource, resourcePath, errors);
            validateSelector(resource.getSelector(), resourcePath, errors);
            validateLocation(resource, resourcePath, errors);
            validateAttributes(resource, resourcePath, errors);
        }
    }

    private void validateResourceName(
            ResourceDefinition resource,
            String resourcePath,
            Set<String> resourceNames,
            List<String> errors
    ) {
        if (resource.getName() == null || resource.getName().isBlank()) {
            errors.add(resourcePath + ".name is required");
            return;
        }

        if (!resourceNames.add(resource.getName())) {
            errors.add("Duplicate resource name: " + resource.getName());
        }
    }

    private void validateResourceType(ResourceDefinition resource, String resourcePath, List<String> errors) {
        if (resource.getResourceType() == null) {
            errors.add(resourcePath + ".resourceType is required");
        }
    }

    private void validateResourceStateAndSpec(ResourceDefinition resource, String resourcePath, List<String> errors) {
        if (resource.getState() == null) {
            errors.add(resourcePath + ".state is required");
            return;
        }

        if (resource.isPresentState() && resource.getSpec() == null) {
            errors.add(resourcePath + ".spec is required when state=present");
        }

        if (resource.isAbsentState() && resource.getSpec() != null) {
            errors.add(resourcePath + ".spec is forbidden when state=absent");
        }

        if (resource.getSpec() != null && (resource.getSpec().getObjectType() == null || resource.getSpec().getObjectType().isBlank())) {
            errors.add(resourcePath + ".spec.objectType is required when spec is present");
        }
    }

    private void validateSelector(SelectorDefinition selector, String resourcePath, List<String> errors) {
        if (selector == null) {
            errors.add(resourcePath + ".selector is required");
            return;
        }

        if (selector.getType() == null) {
            errors.add(resourcePath + ".selector.type is required");
            return;
        }

        if (selector.isQualificationSelector()) {
            if (selector.getDql() == null || selector.getDql().isBlank()) {
                errors.add(resourcePath + ".selector.dql is required when selector.type=qualification");
            }
            if (selector.getPath() != null) {
                errors.add(resourcePath + ".selector.path is forbidden when selector.type=qualification");
            }
        }

        if (selector.isPathSelector()) {
            if (selector.getPath() == null || selector.getPath().isBlank()) {
                errors.add(resourcePath + ".selector.path is required when selector.type=path");
            }
            if (selector.getDql() != null) {
                errors.add(resourcePath + ".selector.dql is forbidden when selector.type=path");
            }
        }
    }

    private void validateLocation(ResourceDefinition resource, String resourcePath, List<String> errors) {
        if (resource.getSpec() == null || resource.getSpec().getLocation() == null) {
            return;
        }

        String path = resource.getSpec().getLocation().getPath();
        List<String> paths = resource.getSpec().getLocation().getPaths();
        boolean hasPath = path != null;
        boolean hasPaths = paths != null;

        if (!hasPath && !hasPaths) {
            errors.add(resourcePath + ".spec.location must define either path or paths");
            return;
        }

        if (hasPath && hasPaths) {
            errors.add(resourcePath + ".spec.location.path and .spec.location.paths are mutually exclusive");
        }

        if (hasPath && path.isBlank()) {
            errors.add(resourcePath + ".spec.location.path must not be blank");
        }

        if (hasPaths) {
            validateLocationPaths(paths, resourcePath, errors);
        }
    }

    private void validateLocationPaths(List<String> paths, String resourcePath, List<String> errors) {
        if (paths.isEmpty()) {
            errors.add(resourcePath + ".spec.location.paths must not be empty");
            return;
        }

        for (int i = 0; i < paths.size(); i++) {
            String locationPath = paths.get(i);
            if (locationPath == null) {
                errors.add(resourcePath + ".spec.location.paths[" + i + "] must not be null");
            } else if (locationPath.isBlank()) {
                errors.add(resourcePath + ".spec.location.paths[" + i + "] must not be blank");
            }
        }
    }

    private void validateAttributes(ResourceDefinition resource, String resourcePath, List<String> errors) {
        if (resource.getSpec() == null || resource.getSpec().getAttributes() == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : resource.getSpec().getAttributes().entrySet()) {
            String attributePath = resourcePath + ".spec.attributes." + entry.getKey();
            validateAttributeValue(entry.getValue(), attributePath, errors);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateAttributeValue(Object value, String attributePath, List<String> errors) {
        if (value == null) {
            errors.add(attributePath + " must not be null");
            return;
        }

        if (value instanceof Map<?, ?>) {
            errors.add(attributePath + " must not be a nested object");
            return;
        }

        if (value instanceof List<?> listValue) {
            for (int i = 0; i < listValue.size(); i++) {
                Object listItem = listValue.get(i);
                if (listItem == null) {
                    errors.add(attributePath + "[" + i + "] must not be null");
                } else if (listItem instanceof Map<?, ?> || listItem instanceof List<?>) {
                    errors.add(attributePath + "[" + i + "] must be a scalar value");
                }
            }
        }
    }
}
