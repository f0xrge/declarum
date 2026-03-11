package com.f0xrge.declarum.engine.core;

public class ResourceApplyResult {

    private final String resourceName;
    private final ApplyActionType actionType;
    private final String message;

    public ResourceApplyResult(String resourceName, ApplyActionType actionType, String message) {
        this.resourceName = resourceName;
        this.actionType = actionType;
        this.message = message;
    }

    public String getResourceName() {
        return resourceName;
    }

    public ApplyActionType getActionType() {
        return actionType;
    }

    public String getMessage() {
        return message;
    }
}
