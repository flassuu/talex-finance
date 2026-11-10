# ADR 0018 — Liabilities as negative-balance accounts

- **Status:** Accepted
- **Date:** 2026-11-10
- **Deciders:** project owner
- **Related questions:** Q-031

## Context

D-025 keeps one account model for accounts, assets and liabilities. The shape of
liabilities and repayments was open (Q-031). The owner is not an accountant and
wants the simplest honest representation.

## Decision

A liability is an account with a negative balance (for example a loan or credit
card). Repaying it is a transfer from a depository account to the liability
account, which reduces the debt. Interest and fees are ordinary expense entries.
There is no amortization schedule in the first version.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Separate liability entity | Conflicts with D-025 and adds a parallel model. |
| Repayment with an explicit interest split | More accurate but more fields; deferred. |
| Full amortization schedule (principal/interest by month) | Too complex for the first version. |

## Consequences

- **Positive:** one model; net worth subtracts liabilities; repayment is a
  transfer, not an expense, so it does not double-count.
- **Negative:** interest and fees must be entered manually as expenses.
- **Follow-up:** credit-card payments are transfers from a depository account to
  the card; a debt's opening balance is negative.

## Review

Revisit if amortization or an interest split becomes a stated need.
