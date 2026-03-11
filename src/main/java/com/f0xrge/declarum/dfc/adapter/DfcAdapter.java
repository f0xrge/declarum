package com.f0xrge.declarum.dfc.adapter;

import com.f0xrge.declarum.manifest.model.ResourceDefinition;

import java.util.Optional;

public interface DfcAdapter {

    SelectorResolution resolveBySelector(ResourceDefinition resourceDefinition);

    DifferenceAnalysis analyzeDifference(ResourceDefinition resourceDefinition, Optional<RepositoryObjectSnapshot> actualObject);

    RepositoryObjectSnapshot createResource(ResourceDefinition resourceDefinition);

    RepositoryObjectSnapshot updateResource(
            ResourceDefinition resourceDefinition,
            RepositoryObjectSnapshot actualObject,
            DifferenceAnalysis differenceAnalysis
    );

    void deleteResource(RepositoryObjectSnapshot actualObject);
}
