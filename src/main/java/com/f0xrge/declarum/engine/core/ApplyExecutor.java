package com.f0xrge.declarum.engine.core;

import com.f0xrge.declarum.dfc.adapter.DfcAdapter;
import com.f0xrge.declarum.dfc.adapter.DifferenceType;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.observability.TelemetryLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApplyExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplyExecutor.class);

    private final DfcAdapter dfcAdapter;

    public ApplyExecutor(DfcAdapter dfcAdapter) {
        this.dfcAdapter = dfcAdapter;
    }

    public ManifestApplyResult apply(ManifestAnalysisResult manifestAnalysisResult) {
        TelemetryLog.info(LOGGER, "engine.apply.start", TelemetryLog.fields(
                "manifest.name", manifestAnalysisResult.getManifestName(),
                "manifest.resource_count", manifestAnalysisResult.getResources().size()
        ));

        List<ResourceApplyResult> resourceApplyResults = new ArrayList<>();

        for (ResourceAnalysisResult resourceAnalysisResult : manifestAnalysisResult.getResources()) {
            resourceApplyResults.add(applyResource(resourceAnalysisResult));
        }

        TelemetryLog.info(LOGGER, "engine.apply.success", TelemetryLog.fields(
                "manifest.name", manifestAnalysisResult.getManifestName(),
                "manifest.resource_count", resourceApplyResults.size()
        ));

        return new ManifestApplyResult(manifestAnalysisResult.getManifestName(), resourceApplyResults);
    }

    private ResourceApplyResult applyResource(ResourceAnalysisResult resourceAnalysisResult) {
        String resourceName = resourceAnalysisResult.getResourceName();
        DifferenceType differenceType = resourceAnalysisResult.getDifferenceAnalysis().getDifferenceType();
        TelemetryLog.info(LOGGER, "engine.apply.resource.start", TelemetryLog.fields(
                "resource.name", resourceName,
                "difference.type", differenceType
        ));

        if (DifferenceType.CREATE.equals(differenceType)) {
            dfcAdapter.createResource(resourceAnalysisResult.getResourceDefinition());
            TelemetryLog.info(LOGGER, "engine.apply.resource.created", TelemetryLog.fields("resource.name", resourceName));
            return new ResourceApplyResult(resourceName, ApplyActionType.CREATED, "Resource created");
        }

        if (DifferenceType.UPDATE.equals(differenceType)) {
            RepositoryObjectSnapshot actualObject = requireActualObject(resourceAnalysisResult);
            dfcAdapter.updateResource(
                    resourceAnalysisResult.getResourceDefinition(),
                    actualObject,
                    resourceAnalysisResult.getDifferenceAnalysis()
            );
            TelemetryLog.info(LOGGER, "engine.apply.resource.updated", TelemetryLog.fields(
                    "resource.name", resourceName,
                    "repository.object_id", actualObject.getObjectId()
            ));
            return new ResourceApplyResult(resourceName, ApplyActionType.UPDATED, "Resource updated");
        }

        if (DifferenceType.DELETE.equals(differenceType)) {
            RepositoryObjectSnapshot actualObject = requireActualObject(resourceAnalysisResult);
            dfcAdapter.deleteResource(actualObject);
            TelemetryLog.info(LOGGER, "engine.apply.resource.deleted", TelemetryLog.fields(
                    "resource.name", resourceName,
                    "repository.object_id", actualObject.getObjectId()
            ));
            return new ResourceApplyResult(resourceName, ApplyActionType.DELETED, "Resource deleted");
        }

        if (DifferenceType.NO_CHANGES.equals(differenceType)) {
            TelemetryLog.info(LOGGER, "engine.apply.resource.no_changes", TelemetryLog.fields("resource.name", resourceName));
            return new ResourceApplyResult(resourceName, ApplyActionType.NO_OPERATION, "No changes detected");
        }

        TelemetryLog.warn(LOGGER, "engine.apply.resource.failed", TelemetryLog.fields(
                "resource.name", resourceName,
                "difference.type", differenceType,
                "difference.message", resourceAnalysisResult.getDifferenceAnalysis().getMessage()
        ));
        return new ResourceApplyResult(
                resourceName,
                ApplyActionType.FAILED,
                resourceAnalysisResult.getDifferenceAnalysis().getMessage()
        );
    }

    private RepositoryObjectSnapshot requireActualObject(ResourceAnalysisResult resourceAnalysisResult) {
        RepositoryObjectSnapshot actualObject = resourceAnalysisResult.getSelectorResolution().getObject();
        if (actualObject == null) {
            throw new IllegalStateException("Actual object is required for update/delete actions");
        }
        return actualObject;
    }
}
