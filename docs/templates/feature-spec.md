# Feature Spec – Feature name

- **Status:** Draft | Approved | Implemented | Dropped
- **Owner:** name or role
- **Related questions:** Q-NNN, if any

## Problem

What user problem does this solve? Describe the situation before the feature,
not the proposed screen.

## Scope

| In scope | Out of scope |
|---|---|
| Concrete behavior | Explicit exclusion |

## User flow

Describe the main path and the important edge cases. Link to designs if they
exist; do not paste large mockups into the repository.

## Data

- Entities and fields affected.
- Migration or import implications.
- Whether the feature changes backup or export output.

Link the owning document in `docs/technical/database.md` instead of restating
the schema.

## Security and privacy

- What data is read or written.
- Whether authentication is required again.
- Whether anything leaves the device.
- Logging and screenshot implications.

## Failure behavior

What happens when input is invalid, storage is locked, the device has no
biometric hardware, or an import is partially valid?

## Acceptance criteria

- [ ] Observable criterion one.
- [ ] Observable criterion two.

## Testing

Which unit, integration, UI and security checks are needed?

## Open questions

List unresolved choices and add them to `docs/open-questions.md`; do not leave
them only here.
