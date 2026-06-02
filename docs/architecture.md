# Architecture

## Objective

The project implements a declarative convergence engine for Documentum repositories.

The engine reads YAML manifests that describe the desired state of managed resources, compares them with the actual repository state, and applies the required changes.

## Layers

### Manifest layer

Responsible for loading and representing YAML manifests as Java objects.

### Validation layer

Responsible for strict structural and semantic validation of manifests before any repository interaction.

### Engine layer

Responsible for resource resolution, diff computation, plan rendering, and apply operations.

### DFC adapter layer

Responsible for interacting with Documentum through DFC. The core adapter depends on repository and session interfaces, while the concrete DFC session factory and repository operations live inside the DFC layer and use the real Documentum client at runtime.

## High-level workflow

1. Load YAML manifest.
2. Validate manifest structure.
3. Validate manifest semantics.
4. Resolve actual state from repository.
5. Normalize actual state.
6. Compute diff.
7. Produce plan.
8. Apply changes if requested.
9. Report results.

## Initial module boundaries

- `manifest.model`
- `manifest.validation`
- `engine`
- `dfc`

## Design principles

- Declarative model first.
- Strict validation before execution.
- Idempotent operations.
- No implicit resource deletion.
- Explicit selector semantics.
- Narrow MVP scope.
