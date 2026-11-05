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
| Q-001 | Platform | What application ID and package name will the Android app use? | Needed for the Gradle project, signing, backups and store identity. | Open |
| Q-002 | Platform | What are the minimum and target Android SDK levels and the supported device profile? | Determines APIs, encryption behavior, biometric support and test matrix. | Open |
| Q-003 | Product | Which UI languages ship initially, and how are currency, numbers and dates formatted? | Affects resources, formatting and every screen; blocks hard-coded strings. | Open |
| Q-004 | Data | Is the ledger single-entry or double-entry? | Determines the schema, transfers, balances, liabilities and corrections. | Open |
| Q-005 | Data | What identifier strategy is used for entities and exports? | Must remain stable across backup, import and future sync. | Open |
| Q-006 | Data | Do accounts, assets and liabilities share one entity or use separate models? | Defines balance calculations and valuation history. | Open |
| Q-007 | Product | Is multi-currency supported in the first release, and how are exchange rates handled? | Changes storage, reporting and input complexity. | Open |
| Q-008 | Product | How are budget periods defined, and is rollover supported? | Determines budget data and calculations. | Open |
| Q-009 | Product | Are recurring or scheduled transactions part of the first release? | Adds generation rules, reminders and edge cases. | Open |
| Q-010 | Product | Will Talex ever support automatic bank imports or aggregators? | Defines whether network access and third-party data handling are in the product at all. | Open |
| Q-011 | Portability | Which import, export and backup formats are supported, and is backup encrypted? | Core to data ownership and safe restoration. | Open |
| Q-012 | Security | What is the authentication fallback when biometrics are unavailable, and is key recovery possible? | Determines lockout behavior and whether data can be lost permanently. | Open |
| Q-013 | Product | Is shared or household use supported, and are multiple profiles needed? | Affects data ownership, UI and future sync. | Open |
| Q-014 | Product | Which analytics and reports are required for the first release? | Prevents building reporting without agreed definitions. | Open |
| Q-015 | Project | What are the licensing and distribution models, and is the repository public or private? | Blocks the LICENSE file, release channel and contribution policy. | Open |
| Q-016 | Architecture | What synchronization model may be adopted later, and how are conflicts resolved? | Not initial scope, but it constrains identifiers and data design. | Open |
| Q-017 | Architecture | When should the app split into Gradle modules, and along which boundaries? | Avoids premature modularization and build complexity. | Open |
| Q-018 | Research | What is the scope and evidence standard for market research? | Prevents unverified claims from entering product decisions. | Open |

## Decision dependency map

| Decision area | Questions |
|---|---|
| First build | Q-001, Q-002, Q-012, Q-017 |
| Ledger and accounts | Q-004, Q-005, Q-006 |
| Planning features | Q-007, Q-008, Q-009 |
| Insight and portability | Q-011, Q-014 |
| Security model | Q-012 |
| Future sync and clients | Q-013, Q-016 |
| Release and licensing | Q-015 |

## Related

- [Decision log](decisions/README.md)
- [Roadmap](roadmap.md)
- [Architecture](technical/architecture.md)
- [Database](technical/database.md)
- [Security](technical/security.md)
