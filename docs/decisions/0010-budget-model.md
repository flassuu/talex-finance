# ADR 0010 — Monthly budget with effective-dated limits

- **Status:** Accepted
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** Q-008 (resolved by this ADR)

## Context

Q-008 asked how budget periods are defined and whether unused money rolls over.
The product owner chose a simple monthly limit that stays in place until changed,
without rollover.

A single mutable "current limit" is not enough: if the limit changes from 300 to
400 in June, recalculating January against 400 would rewrite history. Budgets are
compared with past months, so the limit that was in force at the time must remain
visible. Multi-currency spending also has to be expressed in one comparable
number.

## Decision

A budget is a series of effective-dated limits for one expense category.

- The period is the calendar month. There is no rollover: unused money expires at
  the end of the month.
- A limit row stores the category, the amount in the base currency, and the date
  from which it applies.
- A category has at most one limit in force at a time. Changing the limit updates
  the current month's row when one already exists for that month; otherwise it
  appends a new row effective from the first day of the current month. Historical
  rows are not rewritten.
- The limit used for a month is the latest row whose effective date is on or
  before the last day of that month.
- Removing a budget closes the current period; historical rows remain for past
  months.
- Spending for the month includes the category itself, its direct children and
  split lines, exactly as the category roll-up defines. Each amount is converted
  to the base currency using the exchange rate for its transaction date (D-030).
  If no rate is available on or before that date, the status is reported as
  incomplete instead of showing a silently wrong number.
- Status for a month is: limit, spent, remaining, percent and an over-budget flag.
  Over-budget means spent is strictly greater than the limit; percent may exceed
  100 and is not clamped in the data.
- Budgets can be set only on expense categories, one category per budget.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| One mutable current limit for all months (REF01 model) | Changing the limit rewrites past months and makes historical reports dishonest. |
| A separate budget row for every month | Duplicates data, requires filling gaps, and makes a permanent limit tedious to maintain. |
| Rollover of unused money | The product owner explicitly chose no rollover; it also complicates the meaning of "remaining". |
| Envelope budgeting with multiple buckets per category | More planning machinery than the first release needs. |
| Converting all spending at the month-end rate | Simpler, but a transaction's budget impact would change as rates move after the fact. |

## Consequences

- **Positive:** current limits are easy to set; history stays correct; past months
  do not change when a new limit is entered; the model works with the existing
  category roll-up and multi-currency direction.
- **Negative:** one more entity and query join; mid-month changes apply to the
  whole current month, not proportionally; a missing exchange rate makes the
  status incomplete rather than exact.
- **Follow-up:** `docs/technical/database.md` records the limit-history entity;
  `docs/technical/ledger.md` defines spending and conversion; the budget screen
  shows the limit in force for the selected month and warns when a rate is
  missing.
- **Resolved question:** Q-008 is closed as "calendar month, no rollover,
  effective-dated limits".

## Review

Revisit if users need weekly or yearly budgets, rollover, per-month overrides, or
budget limits in a currency other than the base currency. Any change must
supersede this ADR.
