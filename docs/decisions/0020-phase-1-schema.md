# ADR 0020 — Phase 1 database schema v1

- **Status:** Accepted
- **Date:** 2026-11-10
- **Deciders:** project owner
- **Related questions:** Q-004, Q-005, Q-006, Q-027

## Context

Phase 1 needs a vertical slice: create an account with a starting balance, list it,
and read it back after unlock. [database.md](../technical/database.md) deliberately
kept the schema open. The owner chose to freeze an extended first schema now,
including categories and tags, so the slice exercises more than accounts.

## Decision

Ship schema version 1 with six tables:

| Table | Purpose |
|---|---|
| `accounts` | One account model with types (D-025); archived instead of hard-deleted. |
| `entries` | Income, expense and `opening` rows; signed minor units; soft delete via `deleted_at`. |
| `categories` | Income/expense kinds with one optional parent level (ADR-0011) and stable keys. |
| `tags` | Case-insensitive de-duplication via a normalized-name unique index. |
| `entry_tags` | Many-to-many link between entries and tags. |
| `settings` | Key-value store so the base currency stays inside the encrypted file. |

Opening balances are `entries.type = 'opening'` written atomically with the account
(ADR-0012). An entry's currency must match its account. Splits, transfer groups,
recurring rules and budgets arrive in Phase 2 as additive migrations.

The generated Room schema is the authoritative shape; this ADR records the choice,
not the DDL.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Accounts and entries only | Rejected by the owner, who wants categories and tags in the first schema. |
| The full Phase 2 schema now | Splits, transfers and recurring rules are not designed enough and belong to Phase 2. |
| No schema, probe the database only | Cannot satisfy the P1-06 vertical slice. |

## Consequences

- **Positive:** the slice is realistic and later phases extend rather than rewrite
  the storage.
- **Negative:** some tables ship before their features; migration discipline starts
  now, and no destructive fallback is acceptable for financial data.
- **Follow-up:** Phase 2 adds tables by migration and keeps
  [database.md](../technical/database.md) in sync.

## Review

Revisit when Phase 2 modeling starts, or if the first real queries expose a
structural problem.
