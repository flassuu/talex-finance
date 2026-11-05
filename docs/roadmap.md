# Roadmap

Read this when you are planning the next phase, sequencing work, or checking
what "done" means for a milestone.

**Status:** directional. The sequence is tentative, has no dates and is not a
commitment. Resolve blocking questions before starting a phase.

## Phase 0 — Documentation foundation

Current phase.

- Repository structure and documentation set.
- Preliminary architecture, data and security direction.
- Decision log and open-question register.

**Exit criteria:** the documentation is reviewed and the blocking questions for
Phase 1 are answered.

## Phase 1 — Technical foundation

Build the smallest end-to-end Android application that proves the risky parts.

- Gradle project and baseline configuration.
- Encrypted local database opening successfully.
- Master passphrase, biometric unlock and key handling.
- The same database file opens on a clean install or second device with the
  passphrase.
- One vertical slice through UI, domain and data.
- Verified commands for build, test and lint in `AGENTS.md`.

**Exit criteria:** the app opens an encrypted database, survives restart,
re-locks, unlocks by passphrase and biometric, and the same file opens on a
second device with the passphrase; the key scheme is documented and reviewed.

## Phase 2 — Core ledger

The MVP core is decided (D-014): accounts, transactions, categories and a
financial overview, on the encryption and portability foundation.

- Accounts and account balances, including multiple currencies (D-018).
- Income and expense entry, editing and deletion behavior.
- Categories and tags.
- Transfers, treated separately from income and expenses (D-009).
- Recurring transactions, at least for subscriptions (D-017).
- Exchange rates from the local table, with explicit refresh (D-030).
- The capital overview with the idle/working/lost split (D-016).
- Import/export groundwork only if needed for testing.

**Exit criteria:** a user can record and review their complete transaction
history offline, across multiple currencies, with correct balances.

## Phase 3 — Planning

- Budgets with period behavior.
- Savings goals.
- Assets and liabilities.

**Exit criteria:** the planned capabilities are usable and consistent with the
ledger model.

## Phase 4 — Insight and portability

- Basic analytics: income/expenses, category breakdown, net-worth trend (D-028).
- Backup, restore and export.
- Migration between devices and app versions.
- Data validation and recovery from bad imports.

**Exit criteria:** a user can move their complete data set to a new device and
restore it without loss.

## Phase 5 — Release hardening

- Security review against
  [technical/security.md](technical/security.md).
- Accessibility, performance and large-data testing.
- Release signing and store readiness.
- GPL-3.0 compliance check and license notices.

**Exit criteria:** the release checklist is satisfied and the license
obligations are met.

## Later

- Synchronization via file-level local sync of consistent encrypted snapshots
  (D-023); conflict handling is undefined (Q-016).
- Desktop and web clients.
- Separate profiles against one database, if confirmed (D-027).

## Blockers

| Phase | Blocking questions |
|---|---|
| Phase 1 | Q-017, Q-019 |
| Phase 2 | Q-005 |
| Phase 3 | Q-008 |
| Research | Q-018 |
| Later | Q-016 |

## Related

- [Product overview](product/overview.md)
- [Product plan](product/plan.md)
- [Open questions](open-questions.md)
- [Decision log](decisions/README.md)
- [Security](technical/security.md)
- [Storage, portability and sync](technical/storage-and-sync.md)
