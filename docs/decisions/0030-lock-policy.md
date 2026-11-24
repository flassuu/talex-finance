# ADR 0030 – Lock timeout and biometric control

- **Status:** Accepted
- **Date:** 2026-11-24
- **Deciders:** project owner (delegated to the implementer)
- **Related questions:** Q-037; ADR-0004

## Context

`technical/security.md` requires the app to re-lock "after a configurable
timeout and on process restart". The current build has a hard-coded one-minute
timeout in `MainActivity` and enables biometric unlock automatically after the
first passphrase entry, with no way to turn it off. `AppSessionManager` already
exposes `enableBiometric()` and `disableBiometric()`, but no screen calls them.

The lock decision is taken in `Activity.onStart`, before the database is needed,
and must keep working when the process outlived the background period. A
setting stored only inside the encrypted database would be unreadable at the
moment the decision is made.

## Decision

- **Timeout values:** immediately, 30 seconds, 1 minute, 5 minutes and 15
  minutes. The default is 1 minute, matching the current behavior.
- **"Immediately"** locks the app in `onStop`; the other values compare elapsed
  time in `onStart` and lock when the threshold is reached.
- **Storage:** the timeout and the biometric preference live in plaintext
  `SharedPreferences` (`talex.security`), next to the language choice, because
  they are not secrets and must be readable without an open database.
- **Biometrics:** the preference is enabled by default on devices that support
  it. Turning it off clears the Keystore-wrapped key; turning it on re-wraps the
  current key. Auto-enrollment after unlock happens only while the preference is
  enabled.
- Changing the passphrase (ADR-0029) clears the wrapped key and re-creates it
  only when the preference is enabled.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Keep the timeout in the encrypted `settings` table | Not readable when the lock decision is taken after the process has been backgrounded. |
| Lock only on process restart | Leaves an unlocked database visible to anyone who picks up the device. |
| Always-on biometrics with no switch | The user cannot opt out of a convenience factor; `security.md` treats it as optional. |
| An arbitrary free-form timeout | A short fixed list is easier to test and to explain; custom values add no real value. |

## Consequences

- **Positive:** the lock policy matches `security.md`, works before unlock, and
  biometric unlock becomes a user choice.
- **Negative:** a second small plaintext preferences file; the "immediately"
  value closes and reopens the database more often.
- **Follow-up:** `technical/security.md` records the values and the file; the
  settings screen and the overview quick toggle are covered by tests.

## Review

Revisit if the app adds a PIN quick-unlock, a device-credential fallback, or
per-screen privacy rules that make a single timeout insufficient.
