package com.f0xrge.declarum.dfc.adapter;

import com.f0xrge.declarum.manifest.model.ResourceDefinition;

public interface DfcAdapter {

    SelectorResolution resolveBySelector(ResourceDefinition resourceDefinition);

    DifferenceAnalysis analyzeDifference(ResourceDefinition resourceDefinition, SelectorResolution selectorResolution);

    RepositoryObjectSnapshot createResource(ResourceDefinition resourceDefinition);

    RepositoryObjectSnapshot updateResource(
            ResourceDefinition resourceDefinition,
            RepositoryObjectSnapshot actualObject,
            DifferenceAnalysis differenceAnalysis
    );

    void deleteResource(RepositoryObjectSnapshot actualObject);
}
