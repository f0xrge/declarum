# AGENTS.md

## Purpose

This repository hosts a declarative Documentum provider implemented in Java.

The system must converge a Documentum repository toward a desired state declared in YAML manifests.

## Mandatory rules

- All code must be written in English.
- All comments must be written in English.
- All identifiers must be written in English.
- All package names, class names, method names, test names, and variable names must be written in English.
- Prefer explicit code over implicit magic.
- Validation must be strict.
- Unknown YAML properties must fail deserialization.
- Do not weaken the manifest contract unless the documentation is updated in the same change.

## Before making changes

Read these files first:

1. `README.md`
2. `docs/architecture.md`
3. `docs/manifest-spec.md`
4. `docs/decisions.md`
5. `docs/coding-standards.md`
6. `docs/roadmap.md`

## Coding workflow

1. Read the relevant spec and decision documents.
2. Implement the smallest coherent change.
3. Add or update tests.
4. Update documentation if the behavior or contract changes.
5. Keep the implementation aligned with the MVP scope.

## MVP manifest rules

- `apiVersion` must be `docrepo/v1alpha1`.
- `kind` must be `Manifest`.
- `state` supports only `present` and `absent`.
- `selector.type` supports only `qualification` and `path`.
- `resourceType` supports only `sysobject` and `folder`.
- `spec` is required when `state` is `present`.
- `spec` is forbidden when `state` is `absent`.
- A selector must resolve to at most one object.
- Only attributes explicitly declared in `spec.attributes` are managed.
- Nested objects in `spec.attributes` are forbidden in the MVP.
- `null` values in `spec.attributes` are forbidden in the MVP.

## Documentation policy

If you change a contract rule, update:

- `docs/manifest-spec.md`
- `docs/decisions.md`
- `README.md` when relevant

## Architectural boundaries

- `manifest.model` contains raw manifest structures.
- `manifest.validation` contains structural and semantic validation.
- `engine` contains plan and apply logic.
- `dfc` contains Documentum integration.

Do not mix DFC logic into the manifest model.
