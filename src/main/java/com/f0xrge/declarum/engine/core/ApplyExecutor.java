package com.f0xrge.declarum.engine.core;

import com.f0xrge.declarum.dfc.adapter.DfcAdapter;
import com.f0xrge.declarum.dfc.adapter.DifferenceType;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ApplyExecutor {

    private final DfcAdapter dfcAdapter;

    public ApplyExecutor(DfcAdapter dfcAdapter) {
        this.dfcAdapter = dfcAdapter;
    }

    public ManifestApplyResult apply(ManifestAnalysisResult manifestAnalysisResult) {
        List<ResourceApplyResult> resourceApplyResults = new ArrayList<>();

        for (ResourceAnalysisResult resourceAnalysisResult : manifestAnalysisResult.getResources()) {
            resourceApplyResults.add(applyResource(resourceAnalysisResult));
        }

        return new ManifestApplyResult(manifestAnalysisResult.getManifestName(), resourceApplyResults);
    }

    private ResourceApplyResult applyResource(ResourceAnalysisResult resourceAnalysisResult) {
        DifferenceType differenceType = resourceAnalysisResult.getDifferenceAnalysis().getDifferenceType();

        if (DifferenceType.CREATE.equals(differenceType)) {
            dfcAdapter.createResource(resourceAnalysisResult.getResourceDefinition());
            return new ResourceApplyResult(resourceAnalysisResult.getResourceName(), ApplyActionType.CREATED, "Resource created");
        }

        if (DifferenceType.UPDATE.equals(differenceType)) {
            RepositoryObjectSnapshot actualObject = requireActualObject(resourceAnalysisResult);
            dfcAdapter.updateResource(
                    resourceAnalysisResult.getResourceDefinition(),
                    actualObject,
                    resourceAnalysisResult.getDifferenceAnalysis()
            );
            return new ResourceApplyResult(resourceAnalysisResult.getResourceName(), ApplyActionType.UPDATED, "Resource updated");
        }

        if (DifferenceType.DELETE.equals(differenceType)) {
            RepositoryObjectSnapshot actualObject = requireActualObject(resourceAnalysisResult);
            dfcAdapter.deleteResource(actualObject);
            return new ResourceApplyResult(resourceAnalysisResult.getResourceName(), ApplyActionType.DELETED, "Resource deleted");
        }

        if (DifferenceType.NO_CHANGES.equals(differenceType)) {
            return new ResourceApplyResult(resourceAnalysisResult.getResourceName(), ApplyActionType.NO_OPERATION, "No changes detected");
        }

        return new ResourceApplyResult(
                resourceAnalysisResult.getResourceName(),
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
