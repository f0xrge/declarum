package com.f0xrge.declarum.manifest.validation;

import java.util.Collections;
import java.util.List;

public class ManifestValidationException extends RuntimeException {

    private final List<String> errors;

    public ManifestValidationException(List<String> errors) {
        super(buildMessage(errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    private static String buildMessage(List<String> errors) {
        return "Manifest validation failed: " + String.join("; ", errors);
    }
}
