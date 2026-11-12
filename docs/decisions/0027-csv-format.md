# ADR 0027 – CSV import/export format

- **Status:** Accepted
- **Date:** 2026-11-12
- **Deciders:** project owner (delegated to the implementer)
- **Related questions:** Q-011 (D-032), none open

## Context

CSV is the plaintext import/export format for interoperability (D-032). It must
round-trip the ledger faithfully, reject bad data safely and never duplicate
entries on a repeated import. The ledger already defines the sign convention,
split lines and transfer pairs in [ledger.md](../technical/ledger.md); the CSV
format must map onto those without inventing a second data model.

## Decision

The first-version CSV covers **transactions** (income, expense, transfer) and
**categories**; accounts, budgets, goals and assets are not exported. Accounts
are referenced by name and must already exist at import time.

Transactions are UTF-8 text in RFC 4180 form with a header row, ISO 8601 dates
(`YYYY-MM-DD`) and signed decimal amounts in major units (`-12.34`) plus an
ISO 4217 currency column:

| Column | Required | Meaning |
|---|---|---|
| `id` | yes | entry UUIDv7; a split entry repeats this id on every line row |
| `type` | yes | `income`, `expense` or `transfer` |
| `date` | yes | calendar day, `YYYY-MM-DD` |
| `amount` | yes | signed decimal; expense negative, income positive, transfer sides carry their own sign |
| `currency` | yes | ISO 4217 code, must match the account |
| `account` | yes | account name, mapped to an existing account on import |
| `description` | no | short text |
| `payee` | no | merchant or counterparty |
| `category` | no | category path such as `Food/Groceries`; required on each split line, empty for transfers |
| `tags` | no | semicolon-separated tag names |
| `group_id` | no | transfer group UUIDv7, shared by the two transfer rows |
| `external_id` | no | optional external identifier carried for interoperability |

Representation rules:

- A simple entry is one row.
- A split entry expands to one row per split line; all line rows share the entry
  `id` and each carries its line `category` and `amount`.
- A transfer exports as two rows, one per side, sharing a `group_id`; each row
  carries its own entry id.
- Opening and adjustment entries are system entries and are not exported.

Duplicate detection on import uses the entry `id` when present, because the id is
stable across export and import; otherwise it falls back to a stable hash over
`(date, type, amount, currency, account, description)`. Text matching alone is
never the key.

Import is atomic: the whole batch is written in one database transaction, and a
malformed row rejects the batch rather than importing a partial file.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Export every entity in one file | Budgets, goals and assets have richer shapes and are not needed for CSV interoperability in the first version. |
| A single row per split entry with a merged category string | Loses the per-line structure and cannot round-trip. |
| Text matching for duplicate detection | Unstable across renames and translations; a stable key is required by [ledger.md](../technical/ledger.md). |
| One CSV file per entity kind with no explicit `id` column | Without a stable id, split lines and repeated imports cannot be reconstructed safely. |

## Consequences

- **Positive:** the CSV round-trips income, expense, transfer and split structure;
  repeated imports are idempotent; skipped rows have a reason.
- **Negative:** accounts are referenced by name and are not portable through CSV;
  opening/adjustment entries move only through the encrypted snapshot.
- **Follow-up:** `storage-and-portability.md` owns the exact column specification;
  bank-specific profiles remain a later addition.

## Review

Revisit when bank-specific CSV profiles are introduced, or when accounts, budgets
or goals are required to move through CSV.
