# Coding Standards

## Language

All code must be written in English.

This includes:

- source code
- comments
- package names
- class names
- method names
- variable names
- test names
- documentation embedded in code

## General rules

- Prefer simple and explicit designs.
- Keep parsing, validation, and execution concerns separate.
- Avoid hidden side effects.
- Favor immutability when practical, but keep compatibility with Jackson deserialization.
- Fail fast on invalid input.

## Testing

- Every rule added to validation should have tests.
- Prefer focused tests with descriptive names.
- Tests should document the expected contract.

## Error handling

- Throw precise exceptions for unsupported enum values.
- Preserve strict YAML parsing.
- Do not silently ignore invalid fields.
