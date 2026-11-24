# ADR 0031 – Demo profile lifecycle

- **Status:** Accepted
- **Date:** 2026-11-24
- **Deciders:** project owner (delegated to the implementer)
- **Related questions:** Q-040; D-042, ADR-0004, ADR-0029

## Context

D-042 offers a demo profile on first run so the app is browsable without setting
up real data. A demo that disappears on exit is useless for a guided tour, and a
demo with no protection would contradict the rule that every database is
encrypted. The demo also has to end cleanly: the user must be able to delete it
or turn it into a real profile without exporting and re-importing a backup.

The master passphrase is the root of trust (ADR-0004), but a demo profile has no
user secret yet. It cannot ask for a passphrase and still be "browsable
immediately".

## Decision

A demo profile is a **normal encrypted database with an app-managed passphrase**:

- It is created by generating a random 32-character passphrase and running the
  regular setup. The passphrase is stored wrapped by an app-private Android
  Keystore key in `talex.demo`, without a biometric or credential prompt, so the
  app can unlock the demo on launch.
- The demo is **kept across restarts** until the user acts. It is clearly marked
  with a banner and a card in Settings.
- **Convert:** the user sets a master passphrase; the existing rekey (ADR-0029)
  moves the database to that passphrase, the demo flag and wrapped passphrase
  are cleared, and the example data stays.
- **Delete:** the database, sidecar, demo key and flag are removed and the app
  returns to first-run setup.
- If the Keystore key is lost or invalidated, the demo is discarded and first-run
  setup is shown again; no real user data is involved.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Fixed known demo passphrase in the APK | Any APK reader can open the demo file; the same code path could be mistaken for real protection. |
| Unencrypted demo database | Violates D-003/D-020 and trains the wrong expectation. |
| Delete the demo on exit | The user cannot explore it across app restarts or show it twice. |
| Ask for a passphrase before the demo | Contradicts "browsable immediately" in D-042. |

## Consequences

- **Positive:** the demo is one tap away, encrypted like every other database, and
  converts to a real profile through the same rekey path as a passphrase change.
- **Negative:** one more small Keystore wrapper and a demo flag; the demo data is
  only as safe as the device sandbox, which is acceptable for synthetic data.
- **Follow-up:** the onboarding wizard gets accounts and categories steps;
  Settings gains demo actions; `technical/security.md` records the demo key.

## Review

Revisit if profiles become multiple (D-027) or if the demo should ship as a
prebuilt asset instead of being seeded at runtime.
