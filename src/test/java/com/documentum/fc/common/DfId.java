package com.documentum.fc.common;

public class DfId {

    private final String value;

    public DfId(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
