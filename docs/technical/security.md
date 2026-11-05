# Security and Privacy

Read this when you touch storage, authentication, backups, logging, permissions
or any network boundary.

**Status:** security requirements are established at the product level; the
implementation is provisional and has not been reviewed.

## Assets

- Transaction history, balances, budgets, goals, assets and liabilities.
- Account names and any identifiers the user enters.
- Encryption keys and authentication material.
- Backup and export files, including temporary files.
- Metadata that reveals financial behavior, such as timestamps and file names.

## Threat model

| Threat | Consideration |
|---|---|
| Lost or stolen device | Data at rest must remain unreadable without the master passphrase. |
| Another person using an unlocked device | App-level authentication is required, not only device unlock. |
| Offline extraction | ADB backup, rooted devices and copied app data must not yield plaintext. |
| Copied database file | The portable file can be attacked offline; passphrase strength and KDF choice matter. |
| Screenshots and recents | Financial screens may be captured by the system or other apps. |
| Logs and crash reports | Logs must never contain amounts, account names or keys. |
| Over-broad permissions | The app should request only what a feature needs. |
| Export leakage | The user chooses where exported data goes; the app must warn about plaintext. |
| Future synchronization | A sync feature adds a network and conflict surface. It is out of initial scope. |

Physical coercion, compromised operating systems and malicious system-level
actors are not solved by this document. A dedicated review is required before
release.

## Requirements

- Financial data is encrypted at rest.
- The root secret is a master passphrase; biometric unlock is a local
  convenience, not the root of trust.
- The database file is portable and can be opened on another device with the
  passphrase.
- The app is fully functional without a network connection.
- No silent telemetry, analytics or cloud backup.
- Secrets, keystores and real financial data never enter the repository.
- Backup and export are explicit user actions.

## Proposed controls

All controls below are provisional until implemented and tested.

| Area | Direction |
|---|---|
| Database encryption | SQLCipher; the database key is derived from the master passphrase via a memory-hard KDF. |
| Key storage | The passphrase is never stored; optional Keystore-protected key material enables biometric unlock. |
| Authentication | Master passphrase; AndroidX Biometric as a local convenience (ADR-0004). |
| App switching | Re-lock after a configurable timeout and on process restart. |
| Screenshots | Consider `FLAG_SECURE` for financial screens and the recents preview. |
| Android backup | Exclude the database and key material from automatic system backup; user backups are explicit file copies. |
| Logging | Redact amounts, identifiers and keys; disable verbose logs in release. |
| Network | No network permission unless a feature requires it; no third-party trackers. |
| Export | User-initiated, with a clear warning when the output is unencrypted. |
| Sync | When added, transfer consistent encrypted snapshots, never a live WAL database. |

## Key management

The root of trust is a user-chosen **master passphrase**. A memory-hard KDF
derives the database key; the passphrase itself is never stored in plaintext.
The encrypted file is portable and can be opened on another device with the same
passphrase (ADR-0004).

On Android, after a successful passphrase entry, Keystore-protected key material
may enable biometric unlock on later launches. Biometric authentication is a
user-presence convenience; it does not replace the root secret. If biometrics
fail or the key is invalidated, the passphrase still opens the file.

A short PIN is not acceptable as the root secret: the portable file can be
attacked offline, regardless of the KDF. A PIN may only be a local quick-unlock
that wraps an already-derived key.

The KDF is **Argon2id** (D-034). The exact parameters and the passphrase policy
are set during implementation and recorded with the storage format. No separate
recovery key is offered in the first version (D-031). Do not implement the key
scheme until it is designed and reviewed.

## Recovery

- The app has no server-side recovery, escrow or reset.
- The master passphrase plus an encrypted backup is the recovery path.
- No separate recovery key is offered in the first version (D-031).
- The setup flow must state clearly that a forgotten passphrase without a backup
  means permanent loss.
- Restoring a backup on a clean install and on another device must be tested.

## Backup and export

- A backup is an encrypted copy or consistent snapshot of the database; it is
  only as safe as the passphrase.
- Keep more than one generation; do not let a corrupt latest copy destroy the
  previous one.
- Never upload automatically.
- Test restore, not only backup.
- CSV export is the plaintext import/export format and is user-initiated; warn
  before writing it (D-032).
- Delete temporary export files after use where the platform allows it.
- A live SQLite database with WAL must not be copied as a backup; checkpoint or
  snapshot first.

## Security review checklist

- [ ] Database file and side files are unreadable without the master passphrase.
- [ ] The passphrase is not stored in plaintext; KDF parameters are documented.
- [ ] Biometric unlock does not replace the root secret.
- [ ] Authentication is enforced on cold start and after timeout.
- [ ] System backup excludes the database and key material.
- [ ] User backup and restore are tested on a clean install and another device.
- [ ] Screenshots and recents are considered.
- [ ] Logs and crash reports are redacted.
- [ ] No unexpected network calls or third-party SDKs.
- [ ] If sync exists, it transfers consistent encrypted snapshots.
- [ ] Export and backup behavior is explained to the user.

## Related

- [Architecture](architecture.md)
- [Database](database.md)
- [Storage, portability and sync](storage-and-sync.md)
- [Open questions](../open-questions.md)
- [Decision log](../decisions/README.md)
