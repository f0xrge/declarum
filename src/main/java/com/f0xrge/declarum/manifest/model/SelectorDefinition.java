package com.f0xrge.declarum.manifest.model;

public class SelectorDefinition {

    private SelectorType type;
    private String dql;
    private String path;
    private String expectedType;

    public SelectorDefinition() {
    }

    public SelectorType getType() {
        return type;
    }

    public void setType(SelectorType type) {
        this.type = type;
    }

    public String getDql() {
        return dql;
    }

    public void setDql(String dql) {
        this.dql = dql;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getExpectedType() {
        return expectedType;
    }

    public void setExpectedType(String expectedType) {
        this.expectedType = expectedType;
    }

    public boolean isQualificationSelector() {
        return SelectorType.QUALIFICATION.equals(type);
    }

    public boolean isPathSelector() {
        return SelectorType.PATH.equals(type);
    }
}
