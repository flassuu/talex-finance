# ADR 0003 – Authentication fallback

- **Status:** Superseded by ADR-0004
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** Q-012

> Superseded: the portable encrypted database model in ADR-0004 replaces the
> device-bound "no recovery" consequence and the app-PIN fallback described
> below.

## Context

Talex protects financial data with biometric authentication. Biometrics can be
unavailable: the sensor may fail, the user may disable it, or enrollment may
change. The fallback determines the key model and what happens when the user
cannot authenticate.

## Decision

Use biometric authentication as the primary gate and an **app-specific PIN** as
the fallback. Do not implement key recovery: if the user forgets the PIN and has
no usable backup, the data is lost. A backup is the recovery path.

The exact PIN-to-key derivation, retry limits and lockout behavior remain a
technical question (Q-019) and must be designed before implementation.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Device PIN/pattern as fallback | Simpler, but couples access to the device credential and gives less control over the app's lockout behavior. |
| Biometric only | A broken sensor or changed enrollment can lock the user out permanently. |
| Recoverable key escrow | Conflicts with the principle that the user alone owns the data and adds a server or secret the project does not want. |

## Consequences

- **Positive:** the app remains usable when biometrics fail, and the owner keeps
  full control of access.
- **Negative:** a forgotten PIN means irreversible loss without a backup; this
  must be explained clearly during setup.
- **Follow-up:** choose a PIN-based key derivation and retry policy (Q-019), and
  make backup availability part of onboarding.

## Review

A separate Recovery Key for restoring data on another device is now under
consideration (Q-020). If adopted, it supersedes the "no recovery" consequence
above and may change the fallback model. Revisit this ADR when Q-020 is decided,
or if the PIN scheme proves impractical on target devices.
