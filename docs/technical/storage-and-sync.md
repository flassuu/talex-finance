# Storage, Portability and Synchronization

Read this when you are designing key handling, backup/restore, export, or any
future sync feature. This document records the intended storage model; it is not
an implementation.

**Status:** accepted direction. The detailed key derivation and sync protocol
are still open.

## The model

Talex follows a KeePass-like model adapted to a financial application:

- All financial data lives in **one encrypted SQLite database file**.
- The root secret is a **user-chosen master passphrase**.
- The passphrase derives the database key. The file can be opened on any device
  that can run Talex and knows the passphrase.
- Biometric unlock on Android is a **local convenience**, not the root of trust.
- Backups are encrypted copies or consistent snapshots of the database file.
- Synchronization, when it arrives, is **file-level local sync** of a consistent
  encrypted snapshot, not a live database with WAL.

This makes the database portable by design. A future desktop client can open the
same file with the same passphrase, without an account or a server.

## Key hierarchy

The intended shape:

```text
master passphrase
        │  memory-hard KDF
        ▼
database key  ──encrypts──►  single SQLite file
        │
        └── optionally wrapped by an Android Keystore key
            requiring biometric authentication (local convenience)
```

- The passphrase is never stored in plaintext.
- The KDF must be memory-hard and tuned so that offline guessing is expensive.
- On Android, after a successful passphrase entry, a Keystore-protected copy of
  the key material may enable biometric unlock on later launches.
- If biometrics fail or the key is invalidated, the passphrase still opens the
  file.

The exact KDF, parameters and key-wrapping scheme are open (Q-019).

## Passphrase, not a short PIN

A portable encrypted file can be copied and attacked offline. A four-digit PIN
is therefore not an acceptable root secret, regardless of the KDF. The root
secret should be a passphrase; a short PIN may only be a local quick-unlock that
wraps an already-derived key.

This supersedes the earlier "app PIN fallback, no recovery" model in ADR-0003.
Biometric authentication remains the everyday convenience; the passphrase is the
fallback and the recovery path.

## Backups

- A backup is a copy or snapshot of the encrypted database; it is safe at rest
  only as long as the passphrase remains secret.
- Backups are user-initiated or scheduled locally, never uploaded silently.
- Keep more than one generation; a corrupt latest copy must not destroy the
  previous one.
- A backup is only useful if restore is tested. Restoring on a clean install and
  on another device is part of the definition of done.
- CSV export is a separate, plaintext, user-initiated operation and must carry a
  clear warning.

## Synchronization

Synchronization is not part of the MVP. When it is designed, the constraints are:

- Do not sync a live SQLite database, especially with WAL enabled. Produce a
  consistent snapshot first (checkpoint/close, then copy).
- File-level sync through Syncthing or a self-hosted service is acceptable only
  if the app can detect and handle concurrent changes.
- Two devices editing offline will conflict. Last-writer-wins at the file level
  loses data and is not acceptable for financial records.
- A dedicated conflict-resolution layer or an append-only change log will be
  needed. This is future work (Q-016).
- The encrypted file is the unit of transfer; the sync service must never see
  plaintext.

## What this changes

- The database is portable, not device-bound.
- The master passphrase is the recovery path; losing it without a backup means
  losing the data.
- Biometric unlock is convenience only.
- Backups and restore become first-class, testable features.
- A future desktop or web client can reuse the same file format, subject to
  platform crypto support.

## Open questions

- Q-019: exact KDF and parameter choice.
- Q-016: sync model and conflict resolution.

Q-026 (optional recovery key) is resolved: no separate key in the first version.

## Related

- [Architecture](architecture.md)
- [Database](database.md)
- [Security](security.md)
- [ADR-0004](../decisions/0004-portable-encrypted-database.md)
- [Open questions](../open-questions.md)
