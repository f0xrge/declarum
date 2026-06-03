package com.f0xrge.declarum.cli;

import com.f0xrge.declarum.dfc.adapter.AttributeChange;
import com.f0xrge.declarum.dfc.adapter.DfcAdapter;
import com.f0xrge.declarum.dfc.adapter.DifferenceAnalysis;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.adapter.SelectorResolution;
import com.f0xrge.declarum.dfc.adapter.impl.SessionBackedDfcAdapter;
import com.f0xrge.declarum.dfc.repository.DfcRepositoryObjectOperations;
import com.f0xrge.declarum.dfc.repository.ManagedAttributeValueChecker;
import com.f0xrge.declarum.dfc.session.DefaultSessionManager;
import com.f0xrge.declarum.dfc.session.DfcDocumentumSessionFactory;
import com.f0xrge.declarum.dfc.session.DocumentumSessionFactory;
import com.f0xrge.declarum.dfc.session.SessionManager;
import com.f0xrge.declarum.engine.core.EngineCore;
import com.f0xrge.declarum.engine.core.ManifestAnalysisResult;
import com.f0xrge.declarum.engine.core.ResourceAnalysisResult;
import com.f0xrge.declarum.manifest.ManifestReader;
import com.f0xrge.declarum.manifest.validation.ManifestValidator;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public class DeclarumCli {

    private static final String DOCUMENTUM_DOCBASE = "DOCUMENTUM_DOCBASE";
    private static final String DOCUMENTUM_USER = "DOCUMENTUM_USER";
    private static final String DOCUMENTUM_PASSWORD = "DOCUMENTUM_PASSWORD";
    private static final String DOCUMENTUM_DOMAIN = "DOCUMENTUM_DOMAIN";

    private final EngineCore engineCore;
    private final PrintStream output;
    private final PrintStream errorOutput;

    public DeclarumCli(EngineCore engineCore, PrintStream output, PrintStream errorOutput) {
        this.engineCore = Objects.requireNonNull(engineCore, "engineCore is required");
        this.output = Objects.requireNonNull(output, "output is required");
        this.errorOutput = Objects.requireNonNull(errorOutput, "errorOutput is required");
    }

    public static void main(String[] args) {
        if (args == null || args.length != 1) {
            printUsage(System.err);
            System.exit(2);
            return;
        }

        try {
            int exitCode = createDefaultCli().run(args);
            if (exitCode != 0) {
                System.exit(exitCode);
            }
        } catch (RuntimeException exception) {
            System.err.println("Declarum plan failed: " + exception.getMessage());
            System.exit(1);
        }
    }

    public int run(String[] args) {
        if (args == null || args.length != 1) {
            printUsage(errorOutput);
            return 2;
        }

        try {
            ManifestAnalysisResult analysisResult = engineCore.analyze(Path.of(args[0]));
            output.print(renderPlan(analysisResult));
            return 0;
        } catch (IOException | RuntimeException exception) {
            errorOutput.println("Declarum plan failed: " + exception.getMessage());
            return 1;
        }
    }

    public static String renderPlan(ManifestAnalysisResult analysisResult) {
        Objects.requireNonNull(analysisResult, "analysisResult is required");

        StringBuilder builder = new StringBuilder();
        String manifestName = analysisResult.getManifestName() == null ? "<unnamed>" : analysisResult.getManifestName();
        builder.append("Declarum analysis plan\n");
        builder.append("Manifest: ").append(manifestName).append('\n');
        builder.append("Resources: ").append(analysisResult.getResources().size()).append("\n\n");

        for (ResourceAnalysisResult resourceResult : analysisResult.getResources()) {
            DifferenceAnalysis differenceAnalysis = resourceResult.getDifferenceAnalysis();
            SelectorResolution selectorResolution = resourceResult.getSelectorResolution();
            RepositoryObjectSnapshot resolvedObject = selectorResolution == null ? null : selectorResolution.getObject();

            builder.append("- ").append(resourceResult.getResourceName()).append('\n');
            builder.append("  Resource type: ").append(resourceResult.getResourceDefinition().getResourceType().getValue()).append('\n');
            builder.append("  Desired state: ").append(resourceResult.getResourceDefinition().getState().getValue()).append('\n');
            builder.append("  Selector status: ").append(selectorResolution == null ? "unknown" : selectorResolution.getStatus()).append('\n');
            if (resolvedObject != null) {
                builder.append("  Repository object: ").append(resolvedObject.getObjectId())
                        .append(" (").append(resolvedObject.getObjectType()).append(")\n");
            }
            builder.append("  Planned action: ").append(differenceAnalysis.getDifferenceType()).append('\n');
            if (differenceAnalysis.getMessage() != null && !differenceAnalysis.getMessage().isBlank()) {
                builder.append("  Reason: ").append(differenceAnalysis.getMessage()).append('\n');
            }
            appendAttributeChanges(builder, differenceAnalysis.getManagedAttributeChanges());
            builder.append('\n');
        }

        return builder.toString();
    }

    private static DeclarumCli createDefaultCli() {
        ManifestReader manifestReader = new ManifestReader();
        ManifestValidator manifestValidator = new ManifestValidator();
        DfcAdapter dfcAdapter = createDefaultDfcAdapter(System.getenv());
        EngineCore engineCore = new EngineCore(manifestReader, manifestValidator, dfcAdapter);
        return new DeclarumCli(engineCore, System.out, System.err);
    }

    private static DfcAdapter createDefaultDfcAdapter(Map<String, String> environment) {
        DocumentumSessionFactory sessionFactory = new DfcDocumentumSessionFactory(
                requiredEnvironmentValue(environment, DOCUMENTUM_DOCBASE),
                requiredEnvironmentValue(environment, DOCUMENTUM_USER),
                requiredEnvironmentValue(environment, DOCUMENTUM_PASSWORD),
                environment.get(DOCUMENTUM_DOMAIN)
        );
        SessionManager sessionManager = new DefaultSessionManager(sessionFactory);
        return new SessionBackedDfcAdapter(
                sessionManager,
                new DfcRepositoryObjectOperations(),
                new ManagedAttributeValueChecker()
        );
    }

    private static String requiredEnvironmentValue(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " environment variable is required");
        }
        return value;
    }

    private static void appendAttributeChanges(StringBuilder builder, Map<String, AttributeChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        builder.append("  Attribute changes:\n");
        for (Map.Entry<String, AttributeChange> changeEntry : changes.entrySet()) {
            AttributeChange change = changeEntry.getValue();
            builder.append("    - ").append(changeEntry.getKey())
                    .append(": ").append(change.getCurrentValue())
                    .append(" -> ").append(change.getDesiredValue())
                    .append('\n');
        }
    }

    private static void printUsage(PrintStream errorOutput) {
        errorOutput.println("Usage: java com.f0xrge.declarum.cli.DeclarumCli <manifest.yaml>");
        errorOutput.println("The MVP CLI runs analysis only and does not apply changes.");
    }
}
