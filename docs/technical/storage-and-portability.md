# Storage, Portability and Export

Read this when you are designing key handling, the storage location,
backup/restore, or export and import. This document records the intended storage
model; it is not an implementation.

**Status:** accepted direction. Phase 1 implements the sidecar and Argon2id
parameters; the passphrase policy and export/import details are still to come.

## The model

Talex follows a KeePass-like model adapted to a financial application, with one
deliberate simplification: **there is no device synchronization**.

- All financial data lives in **one encrypted SQLite database file**.
- The file lives in the **app's internal storage** and is not visible to other
  apps or the file manager (D-036).
- The root secret is a **user-chosen master passphrase**.
- The passphrase derives the database key. The file can be opened on any device
  that runs Talex and knows the passphrase.
- Biometric unlock on Android is a **local convenience**, not the root of trust.
- Portability is explicit **export and import** of the encrypted snapshot, not a
  background sync (D-035).
- There is no Android system or cloud backup; the database is excluded from
  automatic backup (D-037).

This keeps the database portable by design without the complexity and risk of a
sync protocol.

## Storage location

- The working database lives in the app's internal storage, in the Android
  sandbox. It is not on shared external storage and is not readable by other
  apps.
- **Export** writes a copy of the encrypted snapshot to a user-chosen location
  through the Storage Access Framework. The working copy stays internal.
- **Import / restore** reads a snapshot from a user-chosen location and replaces
  the working copy only after the format version is validated.

The user decides where exported files go; the app never uploads them.

## Key hierarchy

The intended shape:

```text
master passphrase
        │  Argon2id (D-034)
        ▼
database key  ──encrypts──►  single SQLite file
        │
        └── optionally wrapped by an Android Keystore key
            requiring biometric authentication (local convenience)
```

- The passphrase is never stored in plaintext.
- Argon2id is memory-hard and tuned so that offline guessing is expensive.
- On Android, after a successful passphrase entry, a Keystore-protected copy of
  the key material may enable biometric unlock on later launches.
- If biometrics fail or the key is invalidated, the passphrase still opens the
  file.

The passphrase policy and the exact Argon2id parameters are set during
implementation and recorded in the storage format so future changes can rekey
existing files.

## Phase 1 implementation

The encrypted database is a SQLCipher file opened by Room. The Argon2id header
(salt and cost parameters) lives in a plaintext sidecar, `talex.db.meta`, next to
`talex.db` in the app's internal storage; the portable artifact is that pair and
import/export must move both (ADR-0019).

| Parameter | Initial value |
|---|---|
| Algorithm | Argon2id |
| Memory | 64 MiB |
| Iterations | 3 |
| Parallelism | 1 |
| Salt | 16 random bytes |
| Derived key | 32 bytes |

The values and the format version are stored in the sidecar so a future change can
rekey existing files. Biometric unlock wraps a copy of the derived key with an
app-private Android Keystore key and gates its use through `BiometricPrompt`, so it
is local convenience only; hardware per-use binding is a later hardening step.

## Passphrase, not a short PIN

A portable encrypted file can be copied and attacked offline. A four-digit PIN
is therefore not an acceptable root secret, regardless of the KDF. The root
secret should be a passphrase; a short PIN may only be a local quick-unlock that
wraps an already-derived key.

This supersedes the earlier "app PIN fallback, no recovery" model in ADR-0003.
Biometric authentication remains the everyday convenience; the passphrase is the
fallback and the recovery path.

## Export and import

- Export is an **encrypted snapshot** of the database written to a user-chosen
  location (D-032).
- The snapshot is packaged as a single `.talex` file holding `talex.db` and its
  KDF sidecar (ADR-0028); the user moves one file, not a loose pair.
- Import restores that snapshot on a clean install or another device. The
  version is checked before anything is written; a mismatch changes nothing.
- Restore requires the passphrase of the backup; the current data is replaced
  only after that passphrase is proven to open the snapshot.
- CSV is the plaintext export for interoperability and carries a clear warning.
- Both operations are user-initiated and never run silently.

## Backups and reminders

- Backups are **manual exports**; the app reminds the user to make one rather
  than scheduling background copies (D-038).
- Keep more than one generation; a corrupt latest copy must not destroy the
  previous one.
- There is no system or cloud backup of the database (D-037).
- A backup is only useful if restore is tested. Restoring on a clean install and
  on another device is part of the definition of done.

## Recovery

- The app has no server-side recovery, escrow or reset.
- The master passphrase plus an exported snapshot is the recovery path.
- No separate recovery key is offered in the first version (D-031).
- The setup flow must state clearly that a forgotten passphrase without an
  exported snapshot means permanent loss.

## What this changes

- The database is portable, not device-bound.
- The master passphrase is the recovery path.
- Biometric unlock is convenience only.
- Portability is export/import; there is no synchronization to design or secure.
- A future desktop client can reuse the same file format through export/import,
  subject to platform crypto support.
