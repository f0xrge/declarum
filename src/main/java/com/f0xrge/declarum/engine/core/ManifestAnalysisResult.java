package com.f0xrge.declarum.engine.core;

import java.util.ArrayList;
import java.util.List;

public class ManifestAnalysisResult {

    private final String manifestName;
    private final List<ResourceAnalysisResult> resources;

    public ManifestAnalysisResult(String manifestName, List<ResourceAnalysisResult> resources) {
        this.manifestName = manifestName;
        this.resources = resources == null ? new ArrayList<>() : new ArrayList<>(resources);
    }

    public String getManifestName() {
        return manifestName;
    }

    public List<ResourceAnalysisResult> getResources() {
        return new ArrayList<>(resources);
    }
}
