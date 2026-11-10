# ADR 0004 – Portable encrypted database

- **Status:** Accepted
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** Q-019, Q-020, Q-026, Q-016

## Context

Talex must be local-first, portable and eventually usable on more than one
device. A device-bound key held only in Android Keystore would tie the database
to one installation and make future desktop access and local synchronization
awkward. The owner wants a KeePass-like model: one encrypted file that can be
opened with a password on any device, synced through a local service, and backed
up as a file.

## Decision

- Store all financial data in a single encrypted SQLite database file.
- Derive the database key from a user-chosen **master passphrase** using a
  memory-hard KDF. The file is portable and can be opened on another device with
  the same passphrase.
- Treat biometric authentication on Android as a **local convenience** that
  unlocks Keystore-protected key material; it is not the root of trust.
- Use encrypted copies or consistent snapshots of the database as backups. There
  is no server-side recovery.
- When synchronization is added, sync a consistent encrypted snapshot at the
  file level (Syncthing or a self-hosted service), not a live WAL database.
  Conflict resolution is a separate future design.

This supersedes the device-bound assumptions and the "no recovery" consequence
of ADR-0003.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Device-bound Keystore key only | Not portable; breaks future desktop access and local file sync. |
| Account-based cloud service | Violates local-first and privacy-first principles. |
| Export-only portability | The database would not be a live, portable artifact. |
| Live SQLite file synced directly | WAL and multi-device writes make it unsafe. |

## Consequences

- **Positive:** the data is portable, backup is a file operation, and future
  clients can reuse the same format.
- **Negative:** a short PIN is not sufficient for a portable file; the user must
  choose and remember a passphrase. Forgetting it without a backup means losing
  the data. Offline brute-force resistance depends on the KDF.
- **Follow-up:** define the KDF and passphrase policy (Q-019), decide whether a
  recovery key file is offered (Q-026), and design sync conflict handling
  (Q-016).

## Review

Revisit when the KDF and passphrase policy are chosen, when a desktop client is
designed, or when synchronization work starts.
