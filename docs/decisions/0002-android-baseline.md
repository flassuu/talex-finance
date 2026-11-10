# ADR 0002 – Android baseline

- **Status:** Accepted
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** Q-002

## Context

Talex targets Android first. The minimum SDK determines which devices can
install the app, which security APIs are available, and how much legacy behavior
must be supported. Raising the minimum later is easy; lowering it after release
is costly.

## Decision

Set `minSdk` to **26 (Android 8.0)**. Use the latest stable Android SDK for
`compileSdk` and `targetSdk` available when the Gradle project is created, and
record the exact versions in the build files.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| API 28 or 29 | Narrower device coverage; the security APIs Talex needs are available on API 26. |
| API 23-25 | Older devices and weaker biometric/keystore behavior; not worth the support cost. |
| Decide later | Blocks the initial Gradle configuration. |

## Consequences

- **Positive:** broad coverage of active Android devices while keeping modern
  Keystore and biometric APIs.
- **Negative:** API 26-27 behavior must be tested, especially around biometric
  and key protection.
- **Follow-up:** raise `minSdk` only with a new ADR and device-support evidence.

## Review

Revisit if a required security capability is unavailable on API 26, or if
device telemetry shows that supporting API 26 is no longer worthwhile.
