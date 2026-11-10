# Database

Read this when you are modeling financial data, planning a migration, or
designing export and backup. This document describes constraints and candidates;
it is not a schema.

**Status:** Phase 1 schema v1 exists (ADR-0020). This document describes
constraints and direction; the generated Room schema in `app/schemas/` is the
authoritative shape. Phase 2 tables are not designed yet.

## Storage direction

Room over SQLite, encrypted with SQLCipher. All financial data lives in a single
encrypted, portable database file (D-011, D-020) opened with the master
passphrase. The database is the local source of truth for the initial product.
Backups use consistent encrypted snapshots; export is a logical operation, not a
copy of an open database.

Technology choice is provisional; see [architecture.md](architecture.md) and
[storage-and-portability.md](storage-and-portability.md).

## Phase 1 schema

Schema version 1 ships with the Phase 1 vertical slice (ADR-0020):

| Table | Notes |
|---|---|
| `accounts` | ID is UUIDv7 text; one currency per account; archived instead of deleted. |
| `entries` | `income`, `expense`, `opening`; signed minor units; `deleted_at` tombstone. |
| `categories` | Income/expense kinds; one optional parent; stable keys for seeded defaults. |
| `tags` | `name_normalized` unique index gives case-insensitive de-duplication. |
| `entry_tags` | Composite primary key `(entry_id, tag_id)`. |
| `settings` | Key-value rows; the base currency lives here so it stays encrypted. |

Splits, transfer groups, recurring rules and budgets are Phase 2 additive
migrations. The full generated definition is
`app/schemas/io.github.flassuu.talex.data.db.TalexDatabase/1.json`.

## Design rules

These are working rules for any future schema. Several are established as
decisions in the [decision log](../decisions/README.md): integer minor units and
currency (D-010), multiple accounts (D-008), transfers treated separately
(D-009), and a single encrypted database (D-011).

1. **Never store money as floating point.** Use signed integer minor units
   together with an ISO 4217 currency code. The sign carries direction; the type
   explains it.
2. **Separate the user-facing date from audit timestamps.** Grouping by
   calendar day must not depend on the device timezone at read time.
3. **Identifiers are stable and opaque.** Use UUIDv7 values (D-033) that survive
   export and import.
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
| Account | A place where money is held | One account model with types, including assets and liabilities (D-025) |
| Transaction | Income, expense or transfer with date, amount and category | Single-entry ledger with explicit transfers (D-024) |
| Transaction split | One category's share of a split transaction | One parent tree, exact sum (ADR-0008) |
| Transfer group | Links the two entries of a transfer and stores cross-currency data | Both amounts and a stored rate (ADR-0013) |
| Category | User-defined classification for reporting and budgets | Hierarchy and archival behavior (ADR-0011) |
| Budget | A monthly limit for a category | Effective-dated, no rollover (ADR-0010) |
| Budget limit | One effective-dated limit row for a budget | History must not rewrite past months (ADR-0010) |
| Savings goal | A target amount with progress | Whether it links to a specific account |
| Asset | A holding with a value that changes over time | Valuation history (D-025); update cadence open |
| Liability | A debt with a balance and repayment terms | Whether repayments are transactions or separate entries |
| Transfer | Movement between two accounts | Represented as a linked pair (D-024) |
| Recurring rule | A schedule that generates future transactions | Catch-up generation on unlock (ADR-0009) |

## Ledger model

The ledger is **single-entry with explicit transfer pairs** (D-024). Each entry
belongs to an account; a transfer creates two linked entries that are written
atomically. Amounts are signed integer minor units, so an account balance is the
sum of its entries. This affects balances, liabilities, corrections and
reporting, but it avoids the complexity of a full double-entry system. Revisit
only if accounting needs outgrow this model.

A transaction is either simple (one optional category) or split (two or more
lines under one top-level category); the rules are in
[ledger.md](ledger.md) and ADR-0008. Splits are not a property of transfers.

## Relationships to settle

No entity-relationship diagram is drawn yet. The following relationships need a
decision:

- Transactions and categories: resolved – one category for a simple entry, or
  split lines under one top-level category (ADR-0008).
- Transfers: a linked pair written atomically (D-024); both amounts and a stored
  rate for cross-currency transfers (ADR-0013).
- Assets and liabilities: one account model with types and valuation entries
  (D-025); valuation cadence is open.
- Budgets: resolved – monthly limit with effective-dated history and no rollover
  (ADR-0010); Q-008 is closed.
- Savings goals: linked to an account balance, or independent progress tracking.
- Categories: one level, archived when referenced (ADR-0011).
- Opening balances: a system opening entry created from the starting-balance
  field (ADR-0012).
- Attachments or notes: not in scope unless requested.

The behavioral rules for entries, transfers, splits, balances and deletion live
in [ledger.md](ledger.md). Do not derive a Room schema from this summary alone.

## Backup, export and migration

- Backup is an encrypted snapshot of the database file (D-032).
- CSV is the plaintext import/export format; it is user-initiated and must carry
  a warning.
- Import must validate before writing and must report rejected records.
- Migration between app versions is separate from migration between devices.

## Explicit non-goals

- No splits, transfer groups, recurring rules or budgets in v1; they are Phase 2
  migrations.
- No synchronization metadata or conflict-resolution fields (there is no device
  synchronization; D-035).
- No decision on server-side storage; the initial product has none.
