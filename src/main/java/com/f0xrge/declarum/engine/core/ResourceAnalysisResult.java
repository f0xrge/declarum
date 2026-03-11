package com.f0xrge.declarum.engine.core;

import com.f0xrge.declarum.dfc.adapter.DifferenceAnalysis;
import com.f0xrge.declarum.dfc.adapter.SelectorResolution;
import com.f0xrge.declarum.manifest.model.ResourceDefinition;

public class ResourceAnalysisResult {

    private final ResourceDefinition resourceDefinition;
    private final SelectorResolution selectorResolution;
    private final DifferenceAnalysis differenceAnalysis;

    public ResourceAnalysisResult(
            ResourceDefinition resourceDefinition,
            SelectorResolution selectorResolution,
            DifferenceAnalysis differenceAnalysis
    ) {
        this.resourceDefinition = resourceDefinition;
        this.selectorResolution = selectorResolution;
        this.differenceAnalysis = differenceAnalysis;
    }

    public ResourceDefinition getResourceDefinition() {
        return resourceDefinition;
    }

    public String getResourceName() {
        return resourceDefinition.getName();
    }

    public SelectorResolution getSelectorResolution() {
        return selectorResolution;
    }

    public DifferenceAnalysis getDifferenceAnalysis() {
        return differenceAnalysis;
    }
}
