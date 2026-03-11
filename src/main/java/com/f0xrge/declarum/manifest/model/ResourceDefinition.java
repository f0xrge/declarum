package com.f0xrge.declarum.manifest.model;

public class ResourceDefinition {

    private String name;
    private ResourceType resourceType;
    private DesiredState state;
    private SelectorDefinition selector;
    private ResourceSpec spec;

    public ResourceDefinition() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public DesiredState getState() {
        return state;
    }

    public void setState(DesiredState state) {
        this.state = state;
    }

    public SelectorDefinition getSelector() {
        return selector;
    }

    public void setSelector(SelectorDefinition selector) {
        this.selector = selector;
    }

    public ResourceSpec getSpec() {
        return spec;
    }

    public void setSpec(ResourceSpec spec) {
        this.spec = spec;
    }

    public boolean isPresentState() {
        return DesiredState.PRESENT.equals(state);
    }

    public boolean isAbsentState() {
        return DesiredState.ABSENT.equals(state);
    }
}
