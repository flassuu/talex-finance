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
| D-005 | There is no device synchronization; export/import of the snapshot replaces it. | Accepted |
| D-006 | The domain model stays independent of Android to allow future clients. | Accepted |
| D-007 | REF01 is a functional reference only; no code, structure or implementation is copied. | Accepted |
| D-008 | Multiple accounts are first-class; balances are computed from initial balances and financial entries. | Accepted |
| D-009 | Transfers are treated separately from ordinary income and expenses. | Accepted |
| D-010 | Monetary amounts are stored as integers in minor units and always associated with a currency. | Accepted |
| D-011 | All financial data lives in a single encrypted local SQLite database. | Accepted |
| D-012 | Primary stance: a personal tool first, to be shared with others later. | Accepted |
| D-013 | Product core: the capital dashboard, simple by default with depth for power users. | Accepted |
| D-014 | MVP core: accounts, transactions, categories and financial overview on the security and portability foundation. | Accepted |
| D-015 | Distribution model: free and open source. | Accepted |
| D-016 | The capital dashboard shows idle, working and lost money from the first version. | Accepted |
| D-017 | Subscriptions are recurring transactions with a category, not a separate entity. | Accepted |
| D-018 | Multiple currencies are supported from the first release. | Accepted |
| D-019 | No bank integrations; manual entry and CSV import only in the initial product. | Accepted |
| D-020 | All financial data lives in one portable encrypted database file, openable on any device with the master passphrase. | Accepted |
| D-021 | The master passphrase is the root of trust; biometric unlock is a local convenience. | Accepted |
| D-022 | Backups are encrypted copies or consistent snapshots; there is no server-side recovery. | Accepted |
| D-023 | Future sync is file-level local sync of consistent encrypted snapshots, not a live WAL database. | Superseded by D-035 |
| D-024 | The ledger uses single-entry accounting with explicit transfer pairs. | Accepted |
| D-025 | Accounts, assets and liabilities share one account model with types and valuation entries. | Accepted |
| D-026 | UI languages: English base, Ukrainian translation, German next; no Russian UI. | Accepted |
| D-027 | One user profile now; future versions may allow separate profiles against one database. | Accepted |
| D-028 | First-release analytics: income/expenses, category breakdown and net-worth trend. | Accepted |
| D-029 | License: GNU GPL v3.0. | Accepted |
| D-030 | Exchange rates are stored locally; selected currencies refresh on explicit user action; a per-transaction override is optional. | Accepted |
| D-031 | No separate recovery key in the first version; the master passphrase plus an encrypted backup is the recovery path. | Accepted |
| D-032 | Backup is an encrypted database snapshot; CSV is the plaintext import/export format. | Accepted |
| D-033 | Persistent entity identifiers are UUIDv7, stored as text. | Accepted |
| D-034 | The database key is derived from the master passphrase with Argon2id. | Accepted |
| D-035 | No device synchronization; portability is export/import of the encrypted snapshot. | Accepted |
| D-036 | The encrypted database lives in the app's internal storage and is not exposed to other apps. | Accepted |
| D-037 | No Android system or cloud backup; the database is excluded from automatic backup. | Accepted |
| D-038 | Backups are manual exports with a reminder; no scheduled background backup. | Accepted |
| D-039 | Navigation: a configurable bottom bar with a center quick-add button and a trailing animated FAB menu. | Accepted |
| D-040 | Visual style: Material 3 with dynamic color, in light and dark themes. | Accepted |
| D-041 | Quick add: the center button opens a fast transaction form. | Accepted |
| D-042 | Onboarding: a wizard (passphrase, currency, accounts, categories) with a create-or-demo choice. | Accepted |
| D-043 | Overview: capital cards with small inline charts. | Accepted |

## Index

| ADR | Title | Status |
|---|---|---|
| [ADR-0001](0001-application-id.md) | Application ID `io.github.flassuu.talex` | Accepted |
| [ADR-0002](0002-android-baseline.md) | Android baseline: minSdk 26, latest stable compile/target SDK | Accepted |
| [ADR-0003](0003-authentication-fallback.md) | Biometric authentication with an app-specific PIN fallback | Superseded by ADR-0004 |
| [ADR-0004](0004-portable-encrypted-database.md) | Portable encrypted database with a master passphrase | Accepted |
| [ADR-0005](0005-license-gpl-3.0.md) | License: GNU GPL v3.0 | Accepted |
| [ADR-0006](0006-uuidv7-identifiers.md) | UUIDv7 identifiers | Accepted |
| [ADR-0007](0007-argon2id-kdf.md) | Argon2id key derivation | Accepted |
| [ADR-0008](0008-split-transactions.md) | Split transactions across one category tree | Accepted |
| [ADR-0009](0009-recurring-generation.md) | Catch-up generation of recurring transactions | Accepted |
| [ADR-0010](0010-budget-model.md) | Monthly budget with effective-dated limits | Accepted |
| [ADR-0011](0011-category-hierarchy.md) | One-level category hierarchy with archival | Accepted |
| [ADR-0012](0012-opening-balance.md) | Opening balance as a system entry | Accepted |
| [ADR-0013](0013-cross-currency-transfer.md) | Cross-currency transfer with both amounts and a stored rate | Accepted |
| [ADR-0014](0014-single-app-module.md) | Start with a single `:app` module | Accepted |
| [ADR-0015](0015-no-synchronization.md) | No synchronization; export/import portability | Accepted |
| [ADR-0016](0016-capital-metrics.md) | Capital metrics: simple works / idle / lost model | Accepted |
| [ADR-0017](0017-goals-independent.md) | Savings goals are independent of accounts | Accepted |
| [ADR-0018](0018-liabilities-negative-accounts.md) | Liabilities as negative-balance accounts | Accepted |
| [ADR-0019](0019-kdf-metadata-sidecar.md) | KDF metadata sidecar for the encrypted database | Accepted |
| [ADR-0020](0020-phase-1-schema.md) | Phase 1 database schema v1 | Accepted |

## Open decisions

See [../open-questions.md](../open-questions.md) for questions that do not yet
have an accepted answer.

## Related

- [Open questions](../open-questions.md)
- [Architecture](../technical/architecture.md)
- [ADR template](../templates/adr.md)
