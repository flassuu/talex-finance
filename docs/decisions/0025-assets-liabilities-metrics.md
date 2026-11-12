# ADR 0025 – Assets in works, liabilities in net worth only

- **Status:** Accepted
- **Date:** 2026-11-12
- **Deciders:** project owner
- **Related questions:** Q-034 (resolved by this ADR); extends ADR-0016

## Context

ADR-0016 fixed works as investment accounts and idle as cash accounts. Roadmap 3.4
extends the capital overview to assets and liabilities but says to keep the three
metrics stable. It was unclear where valuation assets and negative-balance
liabilities belong.

## Decision

- **Works** is the sum of investment accounts plus valuation-based asset accounts
  (property, vehicles, other assets). Assets are value-bearing and are treated as
  working capital.
- **Idle** stays the sum of cash-type accounts (checking, savings, cash).
- **Lost** stays the current month's subscriptions plus unnecessary spending.
- **Liabilities** do not appear in works, idle or lost. They reduce **net worth**
  only, through their negative balances.
- Net worth is the sum of all account balances: entry-based balances plus the
  latest valuation of each asset account, minus liabilities.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Subtract liabilities from works or idle by account type | Makes the three numbers stop meaning "money that works / sits / is lost" and is hard to explain. |
| Add a fourth metric for debt | More surface area than the first release needs; net worth already carries it. |
| Leave assets out of the three metrics | Contradicts roadmap 3.4, which asks to extend the three metrics to assets. |

## Consequences

- **Positive:** the three metrics stay simple and stable; net worth becomes the one
  number that includes debt.
- **Negative:** a large mortgage does not change works or idle, so the overview
  must show net worth prominently to avoid a misleading picture.
- **Follow-up:** the overview computes net worth as a separate value and includes
  it in the trend; the metric definitions get tests.

## Review

Revisit if users expect debt to be reflected in the three metrics directly.
