package com.f0xrge.declarum.manifest;

import com.f0xrge.declarum.manifest.model.ManifestDefinition;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ManifestReader {

    private final ObjectMapper objectMapper;

    public ManifestReader() {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.objectMapper.findAndRegisterModules();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public ManifestDefinition read(Path manifestPath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(manifestPath)) {
            return objectMapper.readValue(inputStream, ManifestDefinition.class);
        }
    }
}
