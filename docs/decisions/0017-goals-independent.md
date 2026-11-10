# ADR 0017 — Savings goals are independent of accounts

- **Status:** Accepted
- **Date:** 2026-11-10
- **Deciders:** project owner
- **Related questions:** Q-030

## Context

Savings goals need a home in the data model. The owner prefers simplicity: a
goal should track progress without interfering with account balances.

## Decision

Goals are independent of accounts. A goal has a target amount, an optional date
and a contribution log; its current amount is the sum of contributions. Goal
money is not reserved from, and does not change, any account balance. Deleting a
goal removes its contributions, and a goal never changes the ledger.

If the user wants money actually set aside, they create a dedicated account.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Goal linked to an account that reserves the balance | Adds balance-reconciliation complexity the owner does not want. |
| Hybrid: independent by default, optionally linked | Two behaviors to explain and test; rejected for the first version. |

## Consequences

- **Positive:** goals are a planning overlay with no effect on the ledger or
  net worth.
- **Negative:** a goal does not by itself reduce available cash; the user must
  move money manually.
- **Follow-up:** negative contribution means a withdrawal; zero is invalid.

## Review

Revisit only if account-linked reservation becomes a stated need.
