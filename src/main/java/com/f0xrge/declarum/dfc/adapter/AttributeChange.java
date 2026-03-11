package com.f0xrge.declarum.dfc.adapter;

public class AttributeChange {

    private final Object currentValue;
    private final Object desiredValue;

    public AttributeChange(Object currentValue, Object desiredValue) {
        this.currentValue = currentValue;
        this.desiredValue = desiredValue;
    }

    public Object getCurrentValue() {
        return currentValue;
    }

    public Object getDesiredValue() {
        return desiredValue;
    }
}
