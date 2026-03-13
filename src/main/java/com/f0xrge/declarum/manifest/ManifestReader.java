package com.f0xrge.declarum.manifest;

import com.f0xrge.declarum.manifest.model.ManifestDefinition;
import com.f0xrge.declarum.observability.TelemetryLog;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ManifestReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManifestReader.class);

    private final ObjectMapper objectMapper;

    public ManifestReader() {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.objectMapper.findAndRegisterModules();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public ManifestDefinition read(Path manifestPath) throws IOException {
        TelemetryLog.info(LOGGER, "manifest.read.start", TelemetryLog.fields("manifest.path", String.valueOf(manifestPath)));
        try (InputStream inputStream = Files.newInputStream(manifestPath)) {
            ManifestDefinition manifestDefinition = objectMapper.readValue(inputStream, ManifestDefinition.class);
            int resourceCount = manifestDefinition.getResources() == null ? 0 : manifestDefinition.getResources().size();
            TelemetryLog.info(LOGGER, "manifest.read.success", TelemetryLog.fields(
                    "manifest.path", String.valueOf(manifestPath),
                    "manifest.resource_count", resourceCount,
                    "manifest.name", manifestDefinition.getMetadata() == null ? null : manifestDefinition.getMetadata().getName()
            ));
            return manifestDefinition;
        } catch (IOException exception) {
            TelemetryLog.error(LOGGER, "manifest.read.failure", TelemetryLog.fields(
                    "manifest.path", String.valueOf(manifestPath),
                    "error.type", exception.getClass().getSimpleName(),
                    "error.message", exception.getMessage()
            ), exception);
            throw exception;
        }
    }
}
