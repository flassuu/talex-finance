# Product Plan

Read this when you need the complete picture of what Talex will be and do. This
is the working blueprint; it complements [overview.md](overview.md) (vision and
scope) and [../roadmap.md](../roadmap.md) (sequencing).

**Status:** working draft. The vision and principles are stable; the functional
areas, MVP scope and positioning are proposals and may still change.

## Vision

Talex is a private personal finance system — not a simple expense tracker. It
gives one person a complete and honest picture of their money: where it comes
from, where it goes, whether their capital is growing, and how much of it is
working for them. Everything runs locally and stays under the user's control.

## Project context

Talex is built first as a personal tool (D-012). It is also a diploma project at
a Ukrainian college and a portfolio piece for applying to European companies.
That has practical consequences: the documentation and architecture must be
presentable, the demo path must be reliable, and the app needs a real English
base plus a Ukrainian translation.

## The core problem

The motivating problem, in the user's own words:

- Money arrives and disappears without a trace; "you cannot explain where it
  went."
- There is no single place that answers: what do I own, what do I owe, and how
  is it changing?
- It is unclear whether capital is growing, flat, or shrinking — and why.
- It is unclear how much money sits idle, how much is working and earning, and
  how much is simply lost to subscriptions and unnecessary spending.

Talex's job is to answer these questions from the user's own data.

## Principles

The principles in [overview.md](overview.md), plus two that shape the product
design:

| Principle | Implication |
|---|---|
| Financial completeness | Day-to-day tracking and long-term planning live in one model. |
| Simplicity | Simple by default; advanced depth stays available for power users who need it. |

## Target audiences (not finalized)

| Segment | Description |
|---|---|
| Everyday controllers | People who want a clearer view of where their money goes each month. |
| Active planners | People who deliberately manage budgets, savings, assets and liabilities. |
| Privacy-conscious | People who prefer local-first and self-hosted tools over cloud services. |

Talex is built first as a personal tool, then shared (D-012). It starts with one
user profile; a future version may allow separate profiles against one database
(D-027). The segments below describe who it may serve once shared; none is
locked in, and the strongest differentiator — the capital dashboard with
progressive simplicity — is recorded as D-013.

## The capital dashboard

The guiding product idea, drawn from the functional reference (not copied):

- Track every movement of money, including subscriptions and unnecessary
  spending.
- Show overspending by category and expose categories that can be reduced or
  removed.
- Show whether total capital is growing, flat, or falling, so the user can act.
- Distinguish idle money from money that works: what is idle, what earns, what
  is simply being lost.

This is the emotional core of the product and shapes both the overview screen
and the analytics. The idle/working/lost split is committed from the first
version (D-016): it is derived from account types (working vs idle cash) and from
categories (money being lost to subscriptions and unnecessary spending), rather
than a separate asset module. The exact metric definitions are refined during
implementation (Q-029). REF01's income/neutral/drain roles and risk tiers are a
different axis and are not part of the first-release dashboard.

## Functional areas

The product is organized into six areas. The detailed feature lists are
candidates, not commitments.

### 1. Financial overview

A single dashboard answering "what is my situation right now":

- Total balance and net worth.
- Balances per account.
- Income vs expenses for the current period.
- Savings progress.
- Liabilities.
- Capital trend: growing, flat, or falling.

### 2. Transaction management

- Income, expenses and transfers.
- Accounts.
- Categories with one level of subcategories (ADR-0011) and tags.
- Transaction splitting across one category tree (ADR-0008).
- Recurring transactions, including subscriptions (D-017), generated on app open
  with a summary dialog (ADR-0009).
- Multi-currency with rates from a local table and an explicit refresh (D-030).
- Import and export (D-032).

### 3. Budgeting and planning

- Monthly limits per expense category, effective-dated and without rollover
  (ADR-0010).
- Spending limits.
- Available and planned amounts.
- Remaining budget.
- Simple forecasts.

### 4. Goals and future planning

- Savings goals.
- Planned purchases.
- Financial targets.
- Contributions toward goals.
- Future scenarios.

### 5. Assets and analytics

- Assets and liabilities.
- Net worth over time.
- Historical changes.
- Expense and income reports.
- Financial statistics.

First-release analytics are limited to income/expenses, category breakdown and
the net-worth trend (D-028).

### 6. Privacy and data ownership

- Portable encrypted database with a master passphrase (ADR-0004).
- Biometric access as a local convenience.
- Encrypted snapshot backups and CSV import/export (D-032).
- Import and export.
- Future synchronization.

## MVP

The MVP core is decided (D-014): accounts, transactions, categories and a
financial overview, built on the encryption and portability foundation
(encrypted database, biometric access, backup and export).

Committed additions for the first release:

- Multiple currencies (D-018), with rates from the local table (D-030).
- Subscriptions as recurring transactions (D-017).
- CSV import/export (D-032).
- The capital dashboard with the idle/working/lost split (D-016).

The wider candidate set, to be prioritized later:

- Basic budgeting.
- Goals and planned purchases.
- Assets and liabilities.

Deferred until later: synchronization, banking integrations, desktop and web
clients, advanced forecasting, and any AI assistance.

## Localization

English is the base language. Ukrainian ships as a translation and German is the
next target; there is no Russian UI (D-026). All user-facing strings are
externalized from the start, and number, date and currency formatting follows
the active locale.

## Positioning hypotheses (not validated)

Offline operation and encryption are not differentiators by themselves — they
already exist in competing products. The hypotheses to investigate:

1. A complete finance manager in a truly standalone local-first Android app.
2. Privacy that does not sacrifice usability.
3. Tracking combined with meaningful planning, not just logging.
4. Manual entry made fast and frictionless.
5. A single view of accounts, savings, debts, assets and net worth.

These are hypotheses. Validate before betting on any of them.

## Competitors to study

Candidates identified for research, to be verified before drawing conclusions:
Actual Budget, Firefly III, Money Manager by Realbyte, Outbank, YNAB,
Finanzguru. Track them in [market-research.md](market-research.md).

## Financial data principles

Established design constraints; see
[../technical/database.md](../technical/database.md) for the details:

- Money is stored as integers in minor units, always tied to a currency;
  multiple currencies are supported from the first release (D-018).
- Multiple accounts are first-class, using one account model with types and
  valuation entries for assets and liabilities (D-025).
- The ledger is single-entry with explicit transfer pairs (D-024); a
  cross-currency pair stores both amounts and a rate from the local table
  (ADR-0013).
- Balances are computed from an opening entry and entries (ADR-0012).
- Financial operations are atomic.
- Entity identifiers are stable UUIDv7 values (D-033).
- Exchange rates live in a local table; selected currencies refresh only on an
  explicit user action, with an optional per-transaction override (D-030).

## Future synchronization (anticipate, do not build)

Synchronization is a long-term goal, not an MVP feature. The design leaves room
for it without implementing it:

- Stable UUID identifiers.
- Change tracking.
- Soft deletion and tombstones.
- Conflict resolution.
- Device identification.
- Atomic financial operations.

The working direction is file-level local sync of a consistent encrypted
snapshot, for example through Syncthing or a self-hosted service (D-023). A live
SQLite database with WAL must not be synced directly. Two devices editing offline
will conflict; a dedicated conflict-resolution layer is future work (Q-016).

## Licensing and independence

REF01 is source-available under a noncommercial licence, which restricts direct
reuse. Talex therefore develops independently: REF01 is studied as a functional
reference only, with no code, structure or implementation copied (D-007). Talex
itself is free and open source under **GPL-3.0** (D-015, D-029), and the full
license text ships in `LICENSE`.

## Open questions and next steps

Remaining open items are capital metric formulas (Q-029), goal-account linkage
(Q-030), liability representation (Q-031), modularization (Q-017), synchronization
(Q-016) and research (Q-018); they follow with their phases. The detailed
sequence is in [../roadmap.md](../roadmap.md), and the REF01 disposition is
recorded in [reference-ref01.md](reference-ref01.md).

## Related

- [Product overview](overview.md)
- [Market research](market-research.md)
- [REF01 disposition](reference-ref01.md)
- [Database](../technical/database.md)
- [Ledger](../technical/ledger.md)
- [Storage, portability and sync](../technical/storage-and-sync.md)
- [Roadmap](../roadmap.md)
- [Open questions](../open-questions.md)
