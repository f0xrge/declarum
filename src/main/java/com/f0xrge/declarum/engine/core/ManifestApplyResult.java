package com.f0xrge.declarum.engine.core;

import java.util.ArrayList;
import java.util.List;

public class ManifestApplyResult {

    private final String manifestName;
    private final List<ResourceApplyResult> resources;

    public ManifestApplyResult(String manifestName, List<ResourceApplyResult> resources) {
        this.manifestName = manifestName;
        this.resources = resources == null ? new ArrayList<>() : new ArrayList<>(resources);
    }

    public String getManifestName() {
        return manifestName;
    }

    public List<ResourceApplyResult> getResources() {
        return new ArrayList<>(resources);
    }
}
