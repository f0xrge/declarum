# documentum-declarative-provider

A Maven-based Java 21 starter project for a declarative Documentum provider.

## Current scope

This starter pack includes:

- YAML manifest model
- Strict YAML loading via Jackson YAML
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
mvn test
```

## Initial package layout

```text
com.f0xrge.declarum
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
