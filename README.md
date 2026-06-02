# documentum-declarative-provider

A Maven-based Java 21 starter project for a declarative Documentum provider.

## Current scope

This starter pack includes:

- YAML manifest model
- Strict YAML loading via Jackson YAML
- Engine core orchestration to load/validate manifests and request DFC difference analysis
- Engine apply executor to run create/update/delete actions from analyzed differences
- DFC adapter contract for selector resolution and managed-difference analysis
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

## Initial package layout

```text
com.f0xrge.declarum
├── dfc
│   └── adapter
│       ├── DfcAdapter.java
│       ├── SelectorResolution.java
│       └── DifferenceAnalysis.java
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
```
