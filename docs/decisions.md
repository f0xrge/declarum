# Decisions

## Decision 001 - Declarative provider model

The tool is designed as a declarative convergence engine, not as an imperative provisioning script.

## Decision 002 - Explicit desired state

The MVP supports only two desired states:

- `present`
- `absent`

## Decision 003 - Explicit selector model

The MVP supports only two selector strategies:

- `qualification`
- `path`

These are aligned with Documentum retrieval patterns similar to `getObjectByQualification` and `getObjectByPath`.

## Decision 004 - Narrow resource scope

The MVP supports only these resource handlers:

- `sysobject`
- `folder`

## Decision 005 - Explicit managed attributes

Only attributes declared in `spec.attributes` are managed.
Any other repository attributes remain untouched.

## Decision 006 - Strict manifest behavior

- `spec` is required for `present`
- `spec` is forbidden for `absent`
- Unknown YAML properties must fail loading
- Nested objects in attributes are forbidden in the MVP
- `null` values in attributes are forbidden in the MVP

## Decision 007 - Code language policy

All code artifacts must be written in English, including comments and identifiers.


## Decision 008 - Explicit integration-test profile

Real Documentum integration tests must not run as part of the default unit-test lifecycle. They are isolated behind the `documentum-it` Maven profile and the Failsafe plugin, and they read connection settings from environment variables.


## Decision 009 - Explicit location path cardinality

Managed repository folder links use an explicit cardinality contract:

- `spec.location.path` declares exactly one managed folder path.
- `spec.location.paths` declares one or more managed folder paths.
- `spec.location.path` and `spec.location.paths` are mutually exclusive.
- Location path strings must not be blank.
- `spec.location.paths` must not be empty and must not contain `null` or blank values.

This keeps the YAML contract strict while allowing resources to declare multiple managed repository links.
