package com.f0xrge.declarum.manifest.model;

import java.util.ArrayList;
import java.util.List;

public class ManifestDefinition {

    private String apiVersion;
    private String kind;
    private ManifestMetadata metadata;
    private List<ResourceDefinition> resources = new ArrayList<>();

    public ManifestDefinition() {
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public ManifestMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ManifestMetadata metadata) {
        this.metadata = metadata;
    }

    public List<ResourceDefinition> getResources() {
        return resources;
    }

    public void setResources(List<ResourceDefinition> resources) {
        this.resources = resources;
    }
}
