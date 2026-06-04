package com.f0xrge.declarum.cli;

import com.f0xrge.declarum.dfc.adapter.AttributeChange;
import com.f0xrge.declarum.dfc.adapter.DfcAdapter;
import com.f0xrge.declarum.dfc.adapter.DifferenceAnalysis;
import com.f0xrge.declarum.dfc.adapter.PathChange;
import com.f0xrge.declarum.dfc.adapter.RepositoryObjectSnapshot;
import com.f0xrge.declarum.dfc.adapter.SelectorResolution;
import com.f0xrge.declarum.dfc.adapter.impl.SessionBackedDfcAdapter;
import com.f0xrge.declarum.dfc.repository.DfcRepositoryObjectOperations;
import com.f0xrge.declarum.dfc.repository.ManagedAttributeValueChecker;
import com.f0xrge.declarum.dfc.session.DefaultSessionManager;
import com.f0xrge.declarum.dfc.session.DfcDocumentumSessionFactory;
import com.f0xrge.declarum.dfc.session.DocumentumSessionFactory;
import com.f0xrge.declarum.dfc.session.SessionManager;
import com.f0xrge.declarum.engine.core.ApplyActionType;
import com.f0xrge.declarum.engine.core.ApplyExecutor;
import com.f0xrge.declarum.engine.core.EngineCore;
import com.f0xrge.declarum.engine.core.ManifestAnalysisResult;
import com.f0xrge.declarum.engine.core.ManifestApplyResult;
import com.f0xrge.declarum.engine.core.ResourceAnalysisResult;
import com.f0xrge.declarum.engine.core.ResourceApplyResult;
import com.f0xrge.declarum.manifest.ManifestReader;
import com.f0xrge.declarum.manifest.validation.ManifestValidator;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DeclarumCli {

    private static final String DOCUMENTUM_DOCBASE = "DOCUMENTUM_DOCBASE";
    private static final String DOCUMENTUM_USER = "DOCUMENTUM_USER";
    private static final String DOCUMENTUM_PASSWORD = "DOCUMENTUM_PASSWORD";
    private static final String DOCUMENTUM_DOMAIN = "DOCUMENTUM_DOMAIN";

    private final EngineCore engineCore;
    private final ApplyExecutor applyExecutor;
    private final PrintStream output;
    private final PrintStream errorOutput;

    public DeclarumCli(EngineCore engineCore, PrintStream output, PrintStream errorOutput) {
        this(engineCore, null, output, errorOutput);
    }

    public DeclarumCli(EngineCore engineCore, ApplyExecutor applyExecutor, PrintStream output, PrintStream errorOutput) {
        this.engineCore = Objects.requireNonNull(engineCore, "engineCore is required");
        this.applyExecutor = applyExecutor;
        this.output = Objects.requireNonNull(output, "output is required");
        this.errorOutput = Objects.requireNonNull(errorOutput, "errorOutput is required");
    }

    public static void main(String[] args) {
        CliCommand command = parseCommand(args);
        if (command == null) {
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
            System.err.println("Declarum " + command.getModeName() + " failed: " + exception.getMessage());
            System.exit(1);
        }
    }

    public int run(String[] args) {
        CliCommand command = parseCommand(args);
        if (command == null) {
            printUsage(errorOutput);
            return 2;
        }

        try {
            ManifestAnalysisResult analysisResult = engineCore.analyze(command.manifestPath());
            output.print(renderPlan(analysisResult));

            if (!command.applyMode()) {
                return 0;
            }

            if (applyExecutor == null) {
                throw new IllegalStateException("ApplyExecutor is required for apply mode");
            }

            ManifestApplyResult applyResult = applyExecutor.apply(analysisResult);
            output.print(renderApplySummary(applyResult));
            return containsFailedAction(applyResult) ? 1 : 0;
        } catch (IOException | RuntimeException exception) {
            errorOutput.println("Declarum " + command.getModeName() + " failed: " + exception.getMessage());
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
            appendPathChanges(builder, differenceAnalysis.getPathChanges());
            builder.append('\n');
        }

        return builder.toString();
    }


    public static String renderApplySummary(ManifestApplyResult applyResult) {
        Objects.requireNonNull(applyResult, "applyResult is required");

        Map<ApplyActionType, Integer> actionCounts = new EnumMap<>(ApplyActionType.class);
        for (ApplyActionType actionType : ApplyActionType.values()) {
            actionCounts.put(actionType, 0);
        }

        for (ResourceApplyResult resourceResult : applyResult.getResources()) {
            actionCounts.put(resourceResult.getActionType(), actionCounts.get(resourceResult.getActionType()) + 1);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Declarum apply summary\n");
        builder.append("CREATED: ").append(actionCounts.get(ApplyActionType.CREATED)).append('\n');
        builder.append("UPDATED: ").append(actionCounts.get(ApplyActionType.UPDATED)).append('\n');
        builder.append("DELETED: ").append(actionCounts.get(ApplyActionType.DELETED)).append('\n');
        builder.append("NO_OPERATION: ").append(actionCounts.get(ApplyActionType.NO_OPERATION)).append('\n');
        builder.append("FAILED: ").append(actionCounts.get(ApplyActionType.FAILED)).append('\n');

        for (ResourceApplyResult resourceResult : applyResult.getResources()) {
            if (ApplyActionType.FAILED.equals(resourceResult.getActionType())) {
                builder.append("- ").append(resourceResult.getResourceName()).append(": FAILED");
                if (resourceResult.getMessage() != null && !resourceResult.getMessage().isBlank()) {
                    builder.append(" - ").append(resourceResult.getMessage());
                }
                builder.append('\n');
            }
        }

        return builder.toString();
    }

    private static boolean containsFailedAction(ManifestApplyResult applyResult) {
        for (ResourceApplyResult resourceResult : applyResult.getResources()) {
            if (ApplyActionType.FAILED.equals(resourceResult.getActionType())) {
                return true;
            }
        }
        return false;
    }

    private static CliCommand parseCommand(String[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        if (args.length == 1) {
            return new CliCommand(false, Path.of(args[0]));
        }

        if (args.length == 2 && "plan".equals(args[0])) {
            return new CliCommand(false, Path.of(args[1]));
        }

        if (args.length == 2 && ("apply".equals(args[0]) || "--apply".equals(args[0]))) {
            return new CliCommand(true, Path.of(args[1]));
        }

        return null;
    }

    private static DeclarumCli createDefaultCli() {
        ManifestReader manifestReader = new ManifestReader();
        ManifestValidator manifestValidator = new ManifestValidator();
        DfcAdapter dfcAdapter = createDefaultDfcAdapter(System.getenv());
        EngineCore engineCore = new EngineCore(manifestReader, manifestValidator, dfcAdapter);
        ApplyExecutor applyExecutor = new ApplyExecutor(dfcAdapter);
        return new DeclarumCli(engineCore, applyExecutor, System.out, System.err);
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

    private static void appendPathChanges(StringBuilder builder, List<PathChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        builder.append("  Path changes:\n");
        for (PathChange change : changes) {
            builder.append("    - folder path: ").append(change.getCurrentPaths())
                    .append(" -> ").append(change.getDesiredPath())
                    .append('\n');
        }
    }

    private static void printUsage(PrintStream errorOutput) {
        errorOutput.println("Usage: java com.f0xrge.declarum.cli.DeclarumCli [plan|apply|--apply] <manifest.yaml>");
        errorOutput.println("Default mode is plan. Apply mode must be requested explicitly and modifies the repository.");
    }

    private record CliCommand(boolean applyMode, Path manifestPath) {

        private String getModeName() {
            return applyMode ? "apply" : "plan";
        }
    }
}
