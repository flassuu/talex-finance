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
| Lost or stolen device | Data at rest must remain unreadable without authentication. |
| Another person using an unlocked device | App-level authentication is required, not only device unlock. |
| Offline extraction | ADB backup, rooted devices and copied app data must not yield plaintext. |
| Screenshots and recents | Financial screens may be captured by the system or other apps. |
| Logs and crash reports | Logs must never contain amounts, account names or keys. |
| Over-broad permissions | The app should request only what a feature needs. |
| Export leakage | The user chooses where exported data goes; the app must warn about plaintext. |
| Future synchronization | A sync feature would create a new network and conflict surface. It is out of initial scope. |

Physical coercion, compromised operating systems and malicious system-level
actors are not solved by this document. A dedicated review is required before
release.

## Requirements

- Financial data is encrypted at rest.
- Access is gated by biometric authentication, with an app-specific PIN as the
  fallback.
- The app is fully functional without a network connection.
- No silent telemetry, analytics or cloud backup.
- Secrets, keystores and real financial data never enter the repository.
- Backup and export are explicit user actions.

## Proposed controls

All controls below are provisional until implemented and tested.

| Area | Direction |
|---|---|
| Database encryption | SQLCipher with a randomly generated database passphrase. |
| Key storage | Wrap the passphrase with a key held in Android Keystore. |
| Authentication | AndroidX Biometric, with an app-specific PIN fallback (ADR-0003). |
| App switching | Re-lock after a configurable timeout and on process restart. |
| Screenshots | Consider `FLAG_SECURE` for financial screens and the recents preview. |
| Android backup | Exclude the database and key material from system backup. |
| Logging | Redact amounts, identifiers and keys; disable verbose logs in release. |
| Network | No network permission unless a feature requires it; no third-party trackers. |
| Export | User-initiated, with a clear warning when the output is unencrypted. |

## Key management

Biometric authentication is a user-presence check; it does not by itself encrypt
data. The database passphrase must still be protected explicitly.

Accepted direction:

- Biometric authentication is the primary gate.
- An app-specific PIN is the fallback when biometrics are unavailable
  (ADR-0003).
- There is no key recovery. Losing the PIN without a usable backup means losing
  the data.

Still open: how the PIN is converted into or unwraps the database key, which
key-derivation function and retry limits are used, and how biometric
re-enrollment is handled. These are tracked as Q-019 in
[../open-questions.md](../open-questions.md). Do not implement the key scheme
until it is designed and reviewed.

## Recovery

- The app has no recovery code, escrow or server-side reset.
- A backup is the only recovery path.
- The setup flow must state this before the user creates the PIN.
- Restoring a backup on another device must be tested; whether it requires the
  original PIN or a separate backup passphrase depends on Q-011.

## Backup and export

- A backup is user data, not an internal cache.
- Prefer an authenticated, versioned container; document whether it is
  encrypted.
- Never upload automatically.
- Delete temporary export files after use where the platform allows it.
- Restoring must validate the format before replacing existing data.

## Security review checklist

- [ ] Database file and side files are unreadable without the key.
- [ ] Keys are not stored in preferences, logs or source control.
- [ ] Authentication is enforced on cold start and after timeout.
- [ ] System backup excludes the database and key material.
- [ ] Screenshots and recents are considered.
- [ ] Logs and crash reports are redacted.
- [ ] No unexpected network calls or third-party SDKs.
- [ ] Export and backup behavior is explained to the user.

## Related

- [Architecture](architecture.md)
- [Database](database.md)
- [Open questions](../open-questions.md)
- [Decision log](../decisions/README.md)
