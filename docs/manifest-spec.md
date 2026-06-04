# Manifest Specification

## Top-level structure

```yaml
apiVersion: docrepo/v1alpha1
kind: Manifest
metadata:
  name: example-manifest
resources:
  - ...
```

## Top-level fields

### `apiVersion`

- Type: `string`
- Required: yes
- Allowed value for MVP: `docrepo/v1alpha1`

### `kind`

- Type: `string`
- Required: yes
- Allowed value for MVP: `Manifest`

### `metadata.name`

- Type: `string`
- Required: no
- Meaning: logical name of the manifest for reporting and logs

### `resources`

- Type: `array`
- Required: yes
- Must not be empty

## Resource structure

```yaml
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
      is_active: true
    location:
      path: "/Cabinet/Config"
```

## Resource fields

### `name`

- Type: `string`
- Required: yes
- Must be unique within the manifest

### `resourceType`

- Type: `string`
- Required: yes
- Allowed values for MVP:
  - `sysobject`
  - `folder`

### `state`

- Type: `string`
- Required: yes
- Allowed values for MVP:
  - `present`
  - `absent`

### `selector`

- Type: `object`
- Required: yes

#### `selector.type`

- Allowed values:
  - `qualification`
  - `path`

#### `selector.dql`

- Required when `selector.type = qualification`
- Forbidden when `selector.type = path`

#### `selector.path`

- Required when `selector.type = path`
- Forbidden when `selector.type = qualification`

#### `selector.expectedType`

- Optional
- Used as a safety check after resolution

### `spec`

- Type: `object`
- Required when `state = present`
- Forbidden when `state = absent`

#### `spec.objectType`

- Type: `string`
- Required when `spec` is present

#### `spec.attributes`

- Type: `map<string, scalar or array of scalar>`
- Optional
- Only declared attributes are managed
- Nested objects are forbidden in the MVP
- `null` values are forbidden in the MVP

#### `spec.location`

- Optional
- If present, it must define exactly one of `path` or `paths`.
- Empty `location` objects are invalid.

#### `spec.location.path`

- Optional
- Type: `string`
- Must not be blank when present.
- Identifies one managed target repository folder link.
- Mutually exclusive with `spec.location.paths`.

#### `spec.location.paths`

- Optional
- Type: `array<string>`
- Must not be empty when present.
- Items must not be `null` or blank.
- Identifies multiple managed target repository folder links.
- Mutually exclusive with `spec.location.path`.

## Behavioral rules

- A selector must resolve to at most one object.
- A location declares managed folder links with either `spec.location.path` for one path or `spec.location.paths` for multiple paths, never both.
- `present` means create if absent, update if different, and leave unchanged if already compliant.
- `absent` means delete if present and do nothing if already absent.
- No implicit delete is allowed by omission from the manifest.
