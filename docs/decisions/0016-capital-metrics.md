# ADR 0016 — Capital metric formulas

- **Status:** Accepted
- **Date:** 2026-11-10
- **Deciders:** project owner
- **Related questions:** Q-029

## Context

D-016 commits the capital dashboard to showing works / idle / lost from the
first version, but the exact formulas were open (Q-029). The owner is not an
accountant, so the definitions must be simple to explain and cheap to compute.

## Decision

Use the simple model:

- **Works** is the sum of balances of investment-type accounts.
- **Idle** is the sum of balances of cash-type accounts (checking, savings,
  cash).
- **Lost** is the current period's recurring (subscription) expenses plus
  spending in a category or flag the user marks as unnecessary.
- The net-worth trend is a separate series and is not one of the three numbers.

No idle threshold, no risk tiers, and no role classification are part of this
metric.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Idle = cash above a threshold with no activity for N days | Requires configuration and is harder to explain. |
| REF01 roles (income / neutral / drain) and risk tiers | A different axis; deferred and not part of the first dashboard. |

## Consequences

- **Positive:** derived from account types and categories, so it stays honest
  and easy to reconcile.
- **Negative:** "lost" needs a way to mark unnecessary spending; the exact
  mechanism (a reserved category or a flag) is refined during implementation.
- **Follow-up:** define the savings-rate formula alongside the overview, and
  write tests that reconcile the three numbers with account balances.

## Review

Revisit if the owner asks for an idle threshold or richer classification.
