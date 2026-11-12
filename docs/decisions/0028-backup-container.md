# ADR 0028 – Single-file backup container

- **Status:** Accepted
- **Date:** 2026-11-12
- **Deciders:** project owner (delegated to the implementer)
- **Related questions:** Q-011 (D-032), Q-020

## Context

The portable artifact is the pair `talex.db` + `talex.db.meta` (ADR-0019), because
the Argon2id salt and parameters must be readable before the database can be
decrypted. ADR-0019 left open whether a later export should wrap the pair in a
single file. Export is a user-initiated snapshot to a user-chosen location
(D-032); a two-file artifact is easy to misplace and breaks the "one portable
file" promise made to the user.

## Decision

Export and import of the encrypted snapshot use a **single-file container**. The
container is a ZIP archive with two entries, `talex.db` and `talex.db.meta`,
written with the `.talex` extension. The sidecar stays inside the archive; it is
extracted first so the KDF parameters can be read and the key derived before the
database is opened.

Import validates the container and the sidecar format version before writing
anything. A version mismatch or a corrupt file changes nothing. The working copy
in internal storage remains the loose `talex.db` + `talex.db.meta` pair; only the
exported artifact is packaged.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Export the loose `.db` + `.meta` pair | Two files are easy to separate; violates the "one portable file" promise. |
| A custom header + encrypted payload container | Needs a wrapper layer and its own versioning; a ZIP is standard and sufficient. |
| Bundle only `talex.db` and derive the key without the sidecar | The sidecar carries the salt and parameters and is required before decryption (ADR-0019). |

## Consequences

- **Positive:** the user moves one file; restore on a clean install or another
  device is a single operation.
- **Negative:** import must extract to a temporary location first, then swap
  atomically; a ZIP adds a small amount of code and a versioning concern.
- **Follow-up:** `storage-and-portability.md` records the container format and the
  format-version field; restore is tested on a clean install and another device.

## Review

Revisit if a custom container format with streaming or stronger integrity checks
is ever required.
