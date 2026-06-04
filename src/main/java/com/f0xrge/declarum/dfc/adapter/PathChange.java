package com.f0xrge.declarum.dfc.adapter;

import java.util.ArrayList;
import java.util.List;

public class PathChange {

    private final List<String> currentPaths;
    private final String desiredPath;

    public PathChange(List<String> currentPaths, String desiredPath) {
        this.currentPaths = currentPaths == null ? List.of() : new ArrayList<>(currentPaths);
        this.desiredPath = desiredPath;
    }

    public List<String> getCurrentPaths() {
        return new ArrayList<>(currentPaths);
    }

    public String getDesiredPath() {
        return desiredPath;
    }
}
