# ADR 0019 – KDF metadata sidecar for the encrypted database

- **Status:** Accepted
- **Date:** 2026-11-10
- **Deciders:** project owner (delegated to the implementer)
- **Related questions:** Q-019, Q-020

## Context

Argon2id needs a per-file random salt and cost parameters before the database can be
decrypted. The encrypted SQLCipher file cannot expose that data in a readable form,
so it must live outside the encrypted region. ADR-0004 promises a portable file that
opens on another device with the master passphrase.

## Decision

Store the KDF header in a small plaintext sidecar, `talex.db.meta`, next to
`talex.db` in the app's internal storage. The sidecar is JSON and contains no secret:
format version, algorithm, memory/time/parallelism, a Base64 salt and the key length.

The portable artifact is the pair `talex.db` + `talex.db.meta`. Export and import
move both together; a later archive/container can package them as one file.

Initial parameters (versioned in the sidecar so a future change can rekey files):

| Parameter | Value |
|---|---|
| Algorithm | Argon2id |
| Memory | 64 MiB |
| Iterations | 3 |
| Parallelism | 1 |
| Salt | 16 random bytes |
| Derived key | 32 bytes |

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Rely on SQLCipher's built-in KDF only | ADR-0007 requires a memory-hard KDF; PBKDF2 alone is too cheap. |
| Encrypt the salt inside the database | Chicken-and-egg: the salt is needed before the database can be decrypted. |
| Single-file container: plaintext header + encrypted payload | More faithful to "one file", but needs a temp-file open and a wrapper layer; deferred to Phase 4 if required. |
| Fixed, shared salt | Defeats per-file uniqueness and weakens offline resistance. |

## Consequences

- **Positive:** simple and robust, no temp files, parameters are inspectable and
  versioned, and the sidecar carries no secret.
- **Negative:** the portable artifact is a pair, not literally one file; export,
  import and documentation must handle both.
- **Follow-up:** Phase 4 export packs the pair (or wraps it in a container);
  [storage-and-portability.md](../technical/storage-and-portability.md) records the
  exact format.

## Review

Revisit if a genuinely single-file artifact is required, or when an archive container
is added for export.
