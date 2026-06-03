package com.f0xrge.declarum.engine.core;

import com.f0xrge.declarum.dfc.adapter.DfcAdapter;
import com.f0xrge.declarum.dfc.adapter.DifferenceAnalysis;
import com.f0xrge.declarum.dfc.adapter.SelectorResolution;
import com.f0xrge.declarum.manifest.ManifestReader;
import com.f0xrge.declarum.manifest.model.ManifestDefinition;
import com.f0xrge.declarum.manifest.model.ResourceDefinition;
import com.f0xrge.declarum.manifest.validation.ManifestValidator;
import com.f0xrge.declarum.observability.TelemetryLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EngineCore {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngineCore.class);

    private final ManifestReader manifestReader;
    private final ManifestValidator manifestValidator;
    private final DfcAdapter dfcAdapter;

    public EngineCore(ManifestReader manifestReader, ManifestValidator manifestValidator, DfcAdapter dfcAdapter) {
        this.manifestReader = manifestReader;
        this.manifestValidator = manifestValidator;
        this.dfcAdapter = dfcAdapter;
    }

    public ManifestAnalysisResult analyze(Path manifestPath) throws IOException {
        TelemetryLog.info(LOGGER, "engine.analyze.start", TelemetryLog.fields("manifest.path", String.valueOf(manifestPath)));

        ManifestDefinition manifestDefinition = manifestReader.read(manifestPath);
        manifestValidator.validate(manifestDefinition);

        List<ResourceAnalysisResult> resourceAnalysisResults = new ArrayList<>();
        for (ResourceDefinition resourceDefinition : manifestDefinition.getResources()) {
            TelemetryLog.info(LOGGER, "engine.analyze.resource.start", TelemetryLog.fields("resource.name", resourceDefinition.getName()));
            SelectorResolution selectorResolution = dfcAdapter.resolveBySelector(resourceDefinition);
            DifferenceAnalysis differenceAnalysis = dfcAdapter.analyzeDifference(resourceDefinition, selectorResolution);

            resourceAnalysisResults.add(new ResourceAnalysisResult(
                    resourceDefinition,
                    selectorResolution,
                    differenceAnalysis
            ));

            TelemetryLog.info(LOGGER, "engine.analyze.resource.success", TelemetryLog.fields(
                    "resource.name", resourceDefinition.getName(),
                    "selector.status", selectorResolution.getStatus(),
                    "difference.type", differenceAnalysis.getDifferenceType()
            ));
        }

        String manifestName = manifestDefinition.getMetadata() == null ? null : manifestDefinition.getMetadata().getName();
        TelemetryLog.info(LOGGER, "engine.analyze.success", TelemetryLog.fields(
                "manifest.name", manifestName,
                "manifest.resource_count", resourceAnalysisResults.size()
        ));
        return new ManifestAnalysisResult(manifestName, resourceAnalysisResults);
    }
}
