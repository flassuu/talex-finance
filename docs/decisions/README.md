# Decision Log

Read this when you are making a significant decision, checking whether a choice
is already settled, or superseding an old decision.

**Status:** active. Baseline decisions below come from the project brief. New
decisions use an ADR.

## How decisions are recorded

1. Copy [../templates/adr.md](../templates/adr.md) to
   `NNNN-short-title.md` in this folder, using the next number.
2. Set the status to `Proposed`, `Accepted`, `Rejected` or `Superseded`.
3. State the context, the decision and its consequences. Include alternatives
   that were considered.
4. Add a row to the index below.
5. Never rewrite an accepted ADR to change its meaning. Write a new one that
   supersedes it.

Small, easily reversible choices do not need an ADR. If a choice is hard to
reverse, affects data, security or public contracts, or would surprise a future
contributor, record it.

## Baseline decisions

These are established by the project brief and are binding until superseded.

| ID | Decision | Status |
|---|---|---|
| D-001 | Talex is local-first: core features work fully offline with no mandatory account or server. | Accepted |
| D-002 | Android is the first platform; desktop and web may follow. | Accepted |
| D-003 | Financial data is encrypted at rest and access is protected by biometric authentication. | Accepted |
| D-004 | The user controls backup, restore, export and migration of their complete data. | Accepted |
| D-005 | Synchronization is excluded from the initial implementation. | Accepted |
| D-006 | The domain model stays independent of Android to allow future clients. | Accepted |
| D-007 | REF01 is a functional reference only; no code, structure or implementation is copied. | Accepted |
| D-008 | Multiple accounts are first-class; balances are computed from initial balances and financial entries. | Accepted |
| D-009 | Transfers are treated separately from ordinary income and expenses. | Accepted |
| D-010 | Monetary amounts are stored as integers in minor units and always associated with a currency. | Accepted |
| D-011 | All financial data lives in a single encrypted local SQLite database. | Accepted |

## Index

| ADR | Title | Status |
|---|---|---|
| [ADR-0001](0001-application-id.md) | Application ID `io.github.flassuu.talex` | Accepted |
| [ADR-0002](0002-android-baseline.md) | Android baseline: minSdk 26, latest stable compile/target SDK | Accepted |
| [ADR-0003](0003-authentication-fallback.md) | Biometric authentication with an app-specific PIN fallback | Accepted |

## Open decisions

See [../open-questions.md](../open-questions.md) for questions that do not yet
have an accepted answer.

## Related

- [Open questions](../open-questions.md)
- [Architecture](../technical/architecture.md)
- [ADR template](../templates/adr.md)
