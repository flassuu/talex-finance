# ADR 0001 – Application ID

- **Status:** Accepted
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** Q-001

## Context

The Android application needs a stable identity before the Gradle project is
created. The project has no owned domain name, and the repository is associated
with the GitHub account `flassuu`. The application ID cannot be changed after a
Play Store release without publishing a new application.

## Decision

Use `io.github.flassuu.talex` as the Android application ID and as the base
package name.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| `com.talex.finance` | Depends on controlling the `talex.finance` domain, which is not established. |
| A neutral ID such as `app.talex` | Less clearly owned and less conventional for an independent project. |
| Defer the choice | Blocks the Gradle project and signing identity. |

## Consequences

- **Positive:** the ID is free, conventional and clearly tied to the project's
  GitHub identity.
- **Negative:** if the GitHub account or project ownership changes, the ID no
  longer reflects it; this is acceptable for a personal project.
- **Follow-up:** use the same value for the Gradle `applicationId` and base
  package unless a later ADR changes it before the first public release.

## Review

Revisit only before the first public release. After release, treat the ID as
permanent.
