# Open Questions

Read this when you need to know what is not decided, or before making a choice
that this register already covers. This is the single register of unresolved
questions; other documents link here instead of keeping their own lists.

**Status:** active. Questions are added as they appear and closed only when a
decision is recorded.

## Rules

- Add a question instead of silently choosing an answer in code.
- Keep one question per row.
- When resolved, record an ADR if the choice is significant, then mark the row
  resolved with the date and a link.
- Do not delete rows; preserve the history.
- Do not invent an answer to unblock work. Ask the owner.

## Register

| ID | Area | Question | Why it matters | Status |
|---|---|---|---|---|
| Q-001 | Platform | What application ID and package name will the Android app use? | Needed for the Gradle project, signing, backups and store identity. | Resolved 2026-11-05 — `io.github.flassuu.talex` (ADR-0001) |
| Q-002 | Platform | What are the minimum and target Android SDK levels and the supported device profile? | Determines APIs, encryption behavior, biometric support and test matrix. | Resolved 2026-11-05 — minSdk 26, latest stable compile/target (ADR-0002) |
| Q-003 | Product | Which UI languages ship initially, and how are currency, numbers and dates formatted? | Affects resources, formatting and every screen; blocks hard-coded strings. | Resolved 2026-11-05 — English base, Ukrainian, German next; no Russian UI (D-026) |
| Q-004 | Data | Is the ledger single-entry or double-entry? | Determines the schema, transfers, balances, liabilities and corrections. | Resolved 2026-11-05 — single-entry with explicit transfer pairs (D-024) |
| Q-005 | Data | What identifier strategy is used for entities and exports? | Must remain stable across backup, import and future sync. | Resolved 2026-11-05 — UUIDv7 (D-033, ADR-0006) |
| Q-006 | Data | Do accounts, assets and liabilities share one entity or use separate models? | Defines balance calculations and valuation history. | Resolved 2026-11-05 — one account model with types and valuation entries (D-025) |
| Q-007 | Product | Is multi-currency supported in the first release, and how are exchange rates handled? | Changes storage, reporting and input complexity. | Resolved 2026-11-05 — multi-currency in v1 (D-018); rates per D-030 |
| Q-008 | Product | How are budget periods defined, and is rollover supported? | Determines budget data and calculations. | Resolved 2026-11-05 — calendar month, no rollover, effective-dated limits (ADR-0010) |
| Q-009 | Product | Are recurring or scheduled transactions part of the first release? | Adds generation rules, reminders and edge cases. | Resolved 2026-11-05 — recurring transactions are in the first release (at least for subscriptions, D-017) |
| Q-010 | Product | Will Talex ever support automatic bank imports or aggregators? | Defines whether network access and third-party data handling are in the product at all. | Resolved 2026-11-05 — no bank integrations; manual entry and CSV import (D-019) |
| Q-011 | Portability | Which import, export and backup formats are supported, and is backup encrypted? | Core to data ownership and safe restoration. | Resolved 2026-11-05 — encrypted DB snapshot + CSV import/export (D-032) |
| Q-012 | Security | What is the authentication fallback when biometrics are unavailable, and is key recovery possible? | Determines lockout behavior and whether data can be lost permanently. | Superseded 2026-11-05 — portable model in ADR-0004; master passphrase is the fallback |
| Q-013 | Product | Is shared or household use supported, and are multiple profiles needed? | Affects data ownership, UI and future sync. | Resolved 2026-11-05 — one user now; future separate profiles against one database (D-027) |
| Q-014 | Product | Which analytics and reports are required for the first release? | Prevents building reporting without agreed definitions. | Resolved 2026-11-05 — income/expenses, category breakdown, net-worth trend (D-028) |
| Q-015 | Project | What are the licensing and distribution models (free or paid), and is the repository public or private? | Blocks the LICENSE file, release channel and contribution policy. | Resolved 2026-11-05 — free and open source under GPL-3.0 (D-015, D-029); publishing the GitHub remote still pending |
| Q-016 | Architecture | What synchronization model may be adopted later, and how are conflicts resolved? | Not initial scope, but it constrains identifiers and data design. | Open — file-level sync of consistent snapshots is the direction (D-023); conflict model open |
| Q-017 | Architecture | When should the app split into Gradle modules, and along which boundaries? | Avoids premature modularization and build complexity. | Open |
| Q-018 | Research | What is the scope and evidence standard for market research? | Prevents unverified claims from entering product decisions. | Open |
| Q-019 | Security | How is the master passphrase converted into the database key, and how is offline guessing slowed? | The passphrase is the root of a portable file; KDF choice, parameters and retry policy must be designed before storage is implemented. | Resolved 2026-11-05 — Argon2id (D-034, ADR-0007); parameters tuned on target devices |
| Q-020 | Security | Is there a recovery mechanism (e.g. a separate Recovery Key for restoring on another device), or is backup the only recovery path? | Reconciles with ADR-0004; decides whether users can be locked out permanently. | Resolved 2026-11-05 — portable encrypted file + master passphrase; backups are the recovery path (ADR-0004); no separate recovery key (D-031) |
| Q-021 | Product | Who is the primary target audience, and what is the strongest positioning? | Focuses the feature set, messaging and research. | Resolved 2026-11-05 — personal tool first, shared later (D-012); capital dashboard with progressive simplicity (D-013) |
| Q-022 | Product | What is the exact MVP scope across the six functional areas? | Sets the first release boundary and development order. | Resolved 2026-11-05 — core: accounts, transactions, categories, overview (D-014) |
| Q-023 | Product | How are the capital-dashboard metrics defined — idle vs working vs lost money, and net-worth trend signals? | Shapes the overview and analytics screens. | Resolved 2026-11-05 — idle/working/lost shown from the first version (D-016) |
| Q-024 | Product | Is subscription tracking a first-class feature in the MVP? | The reference emphasizes subscriptions; confirms a dedicated workflow or a category convention. | Resolved 2026-11-05 — recurring transaction with a category, not a separate entity (D-017) |
| Q-025 | Product | How are exchange rates sourced and applied (manual entry, fixed rates, no auto-fetch)? | Multi-currency needs a rate source and consistent conversion in reports. | Resolved 2026-11-05 — local rate table, explicit refresh, optional per-transaction override (D-030) |
| Q-026 | Security | Is an optional recovery key file or second recovery passphrase offered? | A second recovery factor can help, but it must be stored separately and adds UX complexity. | Resolved 2026-11-05 — no separate recovery key in v1; passphrase + backup (D-031) |
| Q-027 | Data | How is an account's opening balance represented? | Balances are computed from initial balances and entries (D-008); the representation affects account creation, import and audit. | Resolved 2026-11-05 — a system opening entry created from the starting-balance field (ADR-0012) |
| Q-028 | Data | How is a transfer between accounts in different currencies represented and valued? | Two account currencies need both amounts and a rate; rounding and edit rules must be explicit. | Resolved 2026-11-05 — both amounts plus a stored rate from the local table, editable per transfer (ADR-0013) |
| Q-029 | Product | What are the exact formulas for the works / idle / lost capital metrics? | D-016 sets the direction, but the numbers need definitions before the overview can be built. | Open — direction in D-016; formulas belong to the capital overview specification |
| Q-030 | Product | Do savings goals link to a specific account or track progress independently? | Determines whether goal money is reserved from a balance or is only a planning overlay. | Open |
| Q-031 | Data | How are liabilities and repayments represented under the one-account model? | D-025 keeps one account model; repayments, interest and fees still need an explicit shape. | Open |

## Decision dependency map

| Decision area | Questions |
|---|---|
| First build | Q-017 |
| Ledger | — |
| Capital dashboard | Q-029 |
| Planning features | Q-030, Q-031 |
| Future sync and clients | Q-016 |
| Research | Q-018 |

## Related

- [Decision log](decisions/README.md)
- [Roadmap](roadmap.md)
- [Architecture](technical/architecture.md)
- [Database](technical/database.md)
- [Security](technical/security.md)
