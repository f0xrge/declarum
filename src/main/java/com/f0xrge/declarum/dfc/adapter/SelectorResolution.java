package com.f0xrge.declarum.dfc.adapter;

public class SelectorResolution {

    private final SelectorResolutionStatus status;
    private final RepositoryObjectSnapshot object;

    private SelectorResolution(SelectorResolutionStatus status, RepositoryObjectSnapshot object) {
        this.status = status;
        this.object = object;
    }

    public static SelectorResolution notFound() {
        return new SelectorResolution(SelectorResolutionStatus.NOT_FOUND, null);
    }

    public static SelectorResolution found(RepositoryObjectSnapshot object) {
        return new SelectorResolution(SelectorResolutionStatus.FOUND, object);
    }

    public static SelectorResolution ambiguous() {
        return new SelectorResolution(SelectorResolutionStatus.AMBIGUOUS, null);
    }

    public SelectorResolutionStatus getStatus() {
        return status;
    }

    public RepositoryObjectSnapshot getObject() {
        return object;
    }
}
