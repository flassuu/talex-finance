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

The seven principles in [overview.md](overview.md), plus two that shape the
product design:

| Principle | Implication |
|---|---|
| Financial completeness | Day-to-day tracking and long-term planning live in one model. |
| Simplicity | Despite the breadth, the everyday workflow must stay fast and understandable. |

## Target audiences (not finalized)

| Segment | Description |
|---|---|
| Everyday controllers | People who want a clearer view of where their money goes each month. |
| Active planners | People who deliberately manage budgets, savings, assets and liabilities. |
| Privacy-conscious | People who prefer local-first and self-hosted tools over cloud services. |

The primary audience and the strongest differentiator are open (Q-021).

## The capital dashboard

The guiding product idea, drawn from the functional reference (not copied):

- Track every movement of money, including subscriptions and unnecessary
  spending.
- Show overspending by category and expose categories that can be reduced or
  removed.
- Show whether total capital is growing, flat, or falling, so the user can act.
- Distinguish idle money from money that works: what is idle, what earns, what
  is simply being lost.

This is the emotional core of the product and should shape both the overview
screen and the analytics. The precise metrics are open (Q-023).

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
- Categories and tags.
- Transaction splitting.
- Recurring transactions (Q-009) and subscription tracking (Q-024).
- Import and export (Q-011).

### 3. Budgeting and planning

- Budgets by category or period.
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

### 6. Privacy and data ownership

- Encrypted database.
- Biometric access.
- Recovery mechanism (open — Q-020).
- Backup and restore.
- Import and export.
- Future synchronization.

## MVP direction (preliminary)

Candidate scope for the first usable release:

- Account management.
- Income, expenses and transfers.
- Categories and tags.
- Multiple currencies (open — Q-007).
- Basic budgeting.
- Basic financial overview.
- CSV import/export.
- Encrypted local database.
- Biometric authentication.
- Backup and recovery foundation.

Deferred until later: synchronization, banking integrations, desktop and web
clients, advanced forecasting, and any AI assistance.

The exact MVP is not committed (Q-022).

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

- Money is stored as integers in minor units, always tied to a currency.
- Multiple accounts are first-class.
- Transfers are separate from ordinary income and expenses.
- Balances are computed from initial balances and entries.
- Financial operations are atomic.
- Entity identifiers are stable (UUIDs) for future synchronization.

## Future synchronization (anticipate, do not build)

Synchronization is a long-term goal, not an MVP feature. The design leaves room
for it without implementing it:

- Stable UUID identifiers.
- Change tracking.
- Soft deletion and tombstones.
- Conflict resolution.
- Device identification.
- Atomic financial operations.

A live SQLite file must not be synced directly. A dedicated synchronization and
conflict-resolution layer will be needed when the time comes.

## Licensing and independence

REF01 is licensed under a noncommercial licence, which restricts commercial
use. Talex therefore develops independently: REF01 is studied as a functional
reference only, with no code, structure or implementation copied. Talex's own
licensing and distribution model is undecided (Q-015).

## Open questions and next steps

New product questions are tracked in
[../open-questions.md](../open-questions.md) (Q-020 … Q-024). The immediate
next step is to agree on MVP scope (Q-022), positioning (Q-021) and the
recovery model (Q-020).

## Related

- [Product overview](overview.md)
- [Market research](market-research.md)
- [Database](../technical/database.md)
- [Roadmap](../roadmap.md)
- [Open questions](../open-questions.md)
