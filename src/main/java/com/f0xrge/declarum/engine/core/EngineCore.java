package com.f0xrge.declarum.engine.core;

import com.f0xrge.declarum.dfc.adapter.DfcAdapter;
import com.f0xrge.declarum.dfc.adapter.DifferenceAnalysis;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.adapter.SelectorResolution;
import com.f0xrge.declarum.manifest.ManifestReader;
import com.f0xrge.declarum.manifest.model.ManifestDefinition;
import com.f0xrge.declarum.manifest.model.ResourceDefinition;
import com.f0xrge.declarum.manifest.validation.ManifestValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EngineCore {

    private final ManifestReader manifestReader;
    private final ManifestValidator manifestValidator;
    private final DfcAdapter dfcAdapter;

    public EngineCore(ManifestReader manifestReader, ManifestValidator manifestValidator, DfcAdapter dfcAdapter) {
        this.manifestReader = manifestReader;
        this.manifestValidator = manifestValidator;
        this.dfcAdapter = dfcAdapter;
    }

    public ManifestAnalysisResult analyze(Path manifestPath) throws IOException {
        ManifestDefinition manifestDefinition = manifestReader.read(manifestPath);
        manifestValidator.validate(manifestDefinition);

        List<ResourceAnalysisResult> resourceAnalysisResults = new ArrayList<>();
        for (ResourceDefinition resourceDefinition : manifestDefinition.getResources()) {
            SelectorResolution selectorResolution = dfcAdapter.resolveBySelector(resourceDefinition);
            Optional<RepositoryObjectSnapshot> actualObject = Optional.ofNullable(selectorResolution.getObject());
            DifferenceAnalysis differenceAnalysis = dfcAdapter.analyzeDifference(resourceDefinition, actualObject);

            resourceAnalysisResults.add(new ResourceAnalysisResult(
                    resourceDefinition,
                    selectorResolution,
                    differenceAnalysis
            ));
        }

        String manifestName = manifestDefinition.getMetadata() == null ? null : manifestDefinition.getMetadata().getName();
        return new ManifestAnalysisResult(manifestName, resourceAnalysisResults);
    }
}
