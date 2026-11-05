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
- Biometric gate, app PIN fallback and key handling.
- Recovery model decided (Q-020).
- One vertical slice through UI, domain and data.
- Verified commands for build, test and lint in `AGENTS.md`.

**Exit criteria:** the app opens an encrypted database, survives restart,
re-locks, and accepts both biometric and PIN authentication; the key scheme is
documented and reviewed.

## Phase 2 — Core ledger

The MVP core is decided (D-014): accounts, transactions, categories and a
financial overview, on the encryption and portability foundation.

- Accounts and account balances.
- Income and expense entry, editing and deletion behavior.
- Categories and basic filtering.
- Transfers, if the ledger model supports them.
- Import/export groundwork only if needed for testing.

**Exit criteria:** a user can record and review their complete transaction
history offline, with correct balances.

## Phase 3 — Planning

- Budgets with period behavior.
- Savings goals.
- Assets and liabilities.
- Recurring entries, if confirmed in scope.

**Exit criteria:** the planned capabilities are usable and consistent with the
ledger model.

## Phase 4 — Insight and portability

- Financial analytics and reports.
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
- Licensing and distribution decision (Q-015).

**Exit criteria:** the release checklist is satisfied and the distribution model
is decided.

## Later

- Synchronization, possibly through Syncthing or a self-hosted service. The
  protocol is deliberately undefined (Q-016).
- Desktop and web clients.
- Shared or household scenarios, if confirmed (Q-013).

## Blockers

| Phase | Blocking questions |
|---|---|
| Phase 1 | Q-017, Q-019, Q-020 |
| Phase 2 | Q-004, Q-005, Q-006, Q-024 |
| Phase 3 | Q-007, Q-008, Q-009 |
| Phase 4 | Q-011, Q-014, Q-023 |
| Phase 5 | Q-015 |
| Research | Q-018 |
| Later | Q-013, Q-016 |

## Related

- [Product overview](product/overview.md)
- [Product plan](product/plan.md)
- [Open questions](open-questions.md)
- [Decision log](decisions/README.md)
- [Security](technical/security.md)
