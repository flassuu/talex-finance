# Database

Read this when you are modeling financial data, planning a migration, or
designing export and backup. This document describes constraints and candidates;
it is not a schema.

**Status:** exploratory. No schema, entity set, migration or query is finalized.
Do not generate Room entities or migrations from this document.

## Storage direction

Room over SQLite, encrypted with SQLCipher. All financial data lives in a single
encrypted database file (D-011). The database is the local source of truth for
the initial product. Backup and export are logical operations, not file copies
of an open database.

Technology choice is provisional; see
[architecture.md](architecture.md).

## Design rules

These are working rules for any future schema. Several are established as
decisions in the [decision log](../decisions/README.md): integer minor units and
currency (D-010), multiple accounts (D-008), transfers treated separately
(D-009), and a single encrypted database (D-011).

1. **Never store money as floating point.** Use integer minor units together
   with an ISO 4217 currency code.
2. **Separate the user-facing date from audit timestamps.** Grouping by
   calendar day must not depend on the device timezone at read time.
3. **Identifiers are stable and opaque.** They must survive export and import.
   The exact strategy is open (Q-005).
4. **Financial records are not silently destroyed.** Decide soft deletion and
   audit behavior before writing delete paths.
5. **Migrations are versioned and tested.** A destructive fallback is not
   acceptable for financial data.
6. **Encryption belongs to the storage boundary.** Repositories must not
   reimplement key handling.

## Candidate entities

The list is a starting point for modeling discussions, not a commitment.

| Candidate | Purpose | Open dependency |
|---|---|---|
| Account | A place where money is held | Whether assets and liabilities share this entity (Q-006) |
| Transaction | Income, expense or transfer with date, amount and category | Single- or double-entry ledger (Q-004) |
| Category | User-defined classification for reporting and budgets | Hierarchy and archival behavior |
| Budget | A limit or plan for a category and period | Period and rollover model (Q-008) |
| Savings goal | A target amount with progress | Whether it links to a specific account |
| Asset | A holding with a value that changes over time | Valuation history and update cadence (Q-006) |
| Liability | A debt with a balance and repayment terms | Whether repayments are transactions or separate entries |
| Transfer | Movement between two accounts | Whether it is a transaction subtype or its own entity |
| Recurring rule | A schedule that generates future transactions | Whether this belongs in the first release (Q-009) |

## Ledger model

The most consequential unresolved decision is whether Talex uses a
single-entry model (each transaction has one account) or a double-entry model
(every movement has balanced postings). The choice affects transfers, account
balances, liabilities, corrections and reporting. It must be recorded as an ADR
before schema work starts. See Q-004.

## Relationships to settle

No entity-relationship diagram is drawn yet. The following relationships need a
decision:

- Transactions and categories: one category per transaction, or split lines?
- Transfers: how both sides are represented and kept consistent.
- Assets and liabilities: separate tables, or specialized accounts.
- Budgets: scoped to a category, a group of categories, or total spending.
- Savings goals: linked to an account balance, or independent progress tracking.
- Attachments or notes: not in scope unless requested.

## Backup, export and migration

- Export must be a documented, versioned logical format that does not depend on
  internal Room table names.
- Backup may be encrypted; the format and key handling are open (Q-011).
- Import must validate before writing and must report rejected records.
- Migration between app versions is separate from migration between devices.

## Explicit non-goals

- No final DDL, no schema version, no migration scripts.
- No synchronization metadata or conflict-resolution fields.
- No decision on server-side storage; the initial product has none.

## Related

- [Architecture](architecture.md)
- [Security](security.md)
- [Open questions](../open-questions.md)
- [Decision log](../decisions/README.md)
