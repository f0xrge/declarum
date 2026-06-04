# documentum-declarative-provider

A Maven-based Java 21 starter project for a declarative Documentum provider.

## Current scope

This starter pack includes:

- YAML manifest model
- Strict YAML loading via Jackson YAML
- Engine core orchestration to load/validate manifests and request DFC difference analysis
- Engine apply executor to run create/update/delete actions from analyzed differences
- MVP command-line entry point for manifest analysis and plan rendering
- DFC adapter contract for selector resolution and managed-difference analysis
- Concrete DFC session and repository operations backed by the real Documentum client at runtime
- Unit tests for manifest deserialization
- Architecture and decision records
- Codex guidance files

## Project goals

Build a declarative convergence engine for Documentum repositories with a workflow similar to Terraform or Ansible:

1. Load desired state from YAML manifests.
2. Validate structure and semantics.
3. Resolve actual state from Documentum through DFC.
4. Compute a plan.
5. Apply changes safely and idempotently.

## Build

```bash
./mvnw test
```

Use the Maven wrapper so the project runs with the expected Maven version. If needed, a local Maven installation can still run `mvn test`.


## CLI analysis mode

The MVP CLI runs analysis only: it loads a YAML manifest, validates it, asks the engine to analyze repository differences, and prints a readable plan. It does not apply changes.

Set the required Documentum connection variables before running the CLI:

```bash
export DOCUMENTUM_DOCBASE=your_docbase
export DOCUMENTUM_USER=your_user
export DOCUMENTUM_PASSWORD=your_password
```

Optional variable:

```bash
export DOCUMENTUM_DOMAIN=your_domain
```

Run plan analysis for a manifest with:

```bash
./mvnw -q compile exec:java \
  -Dexec.mainClass=com.f0xrge.declarum.cli.DeclarumCli \
  -Dexec.args=/path/to/manifest.yaml
```

If the real DFC client is not already available to the runtime, add it to the Maven or Java classpath before running the command.

## Windows execution script

A Windows helper script is available to run the analysis CLI against a real Documentum repository. It compiles the project, copies Maven runtime dependencies into `target\declarum-runtime-dependencies`, adds the DFC JAR to the runtime classpath, and then launches `com.f0xrge.declarum.cli.DeclarumCli`.

Required environment variables:

```cmd
set DOCUMENTUM_DOCBASE=your_docbase
set DOCUMENTUM_USER=your_user
set DOCUMENTUM_PASSWORD=your_password
set DOCUMENTUM_DFC_JAR=C:\path\to\dfc.jar
```

Optional environment variables:

```cmd
set DOCUMENTUM_DOMAIN=your_domain
set DOCUMENTUM_DFC_CONFIG_DIR=C:\path\to\documentum-config-directory
```

Run a plan analysis from a Windows command prompt with:

```cmd
scripts\declarum-plan.cmd C:\path\to\manifest.yaml
```

`DOCUMENTUM_DFC_CONFIG_DIR` should point to the directory containing runtime DFC configuration such as `dfc.properties` when your Documentum client setup requires it. The script only runs analysis mode; it does not apply changes.

## Documentum integration tests

Unit tests are isolated from real Documentum infrastructure. Classes named `*IT.java` are excluded from Surefire and are executed only by the Maven Failsafe plugin when the explicit `documentum-it` profile is enabled.

Set the required connection variables before running integration tests:

```bash
export DOCUMENTUM_DOCBASE=your_docbase
export DOCUMENTUM_USER=your_user
export DOCUMENTUM_PASSWORD=your_password
```

Optional variables:

```bash
export DOCUMENTUM_DOMAIN=your_domain
export DOCUMENTUM_DFC_JAR=/path/to/dfc.jar
export DOCUMENTUM_TEST_PATH=/Cabinet/ObjectForReadOnlyTest
export DOCUMENTUM_TEST_QUALIFICATION="dm_document where object_name = 'ObjectForReadOnlyTest'"
```

Run integration tests explicitly with:

```bash
./mvnw verify -Pdocumentum-it
```

The DFC implementation uses the real DFC client classes at runtime. Ensure the DFC JAR and any required Documentum runtime configuration are available on the integration-test classpath. Tests that require optional test object variables are skipped when those variables are not provided.

## Initial package layout

```text
com.f0xrge.declarum
├── cli
│   └── DeclarumCli.java
├── dfc
│   ├── adapter
│   │   ├── DfcAdapter.java
│   │   ├── SelectorResolution.java
│   │   └── DifferenceAnalysis.java
│   ├── repository
│   │   ├── DfcRepositoryObjectOperations.java
│   │   └── RepositoryObjectOperations.java
│   └── session
│       ├── DfcDocumentumSessionFactory.java
│       └── DocumentumSessionFactory.java
├── engine
│   └── core
│       ├── EngineCore.java
│       ├── ApplyExecutor.java
│       └── ManifestAnalysisResult.java
├── manifest
│   ├── ManifestReader.java
│   └── model
│       ├── DesiredState.java
│       ├── ManifestDefinition.java
│       ├── ManifestMetadata.java
│       ├── ResourceDefinition.java
│       ├── ResourceLocation.java
│       ├── ResourceSpec.java
│       ├── ResourceType.java
│       ├── SelectorDefinition.java
│       └── SelectorType.java
```

## YAML location contract

A present resource may declare managed repository folder links with `spec.location`. Use `location.path` for exactly one managed folder path, or `location.paths` for one or more managed folder paths. The two fields are mutually exclusive, path values must not be blank, `paths` must not be empty, and unknown YAML properties still fail strict deserialization.

## YAML example

```yaml
apiVersion: docrepo/v1alpha1
kind: Manifest
metadata:
  name: billing-config
resources:
  - name: app-config-main
    resourceType: sysobject
    state: present
    selector:
      type: qualification
      dql: "my_app_config where config_key = 'main'"
      expectedType: my_app_config
    spec:
      objectType: my_app_config
      attributes:
        object_name: "main-config"
        config_key: "main"
        config_value: "https://api.internal.local"
        is_active: true
        tags:
          - "billing"
          - "prod"
      location:
        paths:
          - "/Cabinet/Config"
          - "/Cabinet/Archive"
```
