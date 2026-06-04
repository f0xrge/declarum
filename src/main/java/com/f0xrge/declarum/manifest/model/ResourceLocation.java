package com.f0xrge.declarum.manifest.model;

import java.util.ArrayList;
import java.util.List;

public class ResourceLocation {

    private String path;
    private List<String> paths;

    public ResourceLocation() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public List<String> managedPaths() {
        if (path != null) {
            return List.of(path);
        }
        if (paths == null) {
            return List.of();
        }
        return new ArrayList<>(paths);
    }
}
