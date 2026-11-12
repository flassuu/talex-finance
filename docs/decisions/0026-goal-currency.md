# ADR 0026 – Each savings goal has one currency

- **Status:** Accepted
- **Date:** 2026-11-12
- **Deciders:** project owner
- **Related questions:** Q-035 (resolved by this ADR); completes ADR-0017

## Context

ADR-0017 makes goals an independent contribution log but does not say which
currency a goal and its contributions use. Money values always carry a currency
(D-010), so the goal needs an explicit answer.

## Decision

Each goal has one currency. Its target and every contribution are in that
currency; the current amount is the sum of its contributions.

- A contribution is a signed amount in the goal currency: positive adds, negative
  withdraws, zero is invalid.
- Goals are independent of accounts, so a contribution is not tied to an account
  or to an exchange rate.
- A goal is not converted in the overview or in the ledger; it is a planning
  overlay only.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Goals always in the base currency, contributions converted | Needs a rate per contribution and makes progress depend on rate history. |
| Any currency per contribution | More flexible, but mixing currencies inside one goal is hard to explain and test. |

## Consequences

- **Positive:** the model matches the one-currency pattern of accounts and needs no
  conversion logic.
- **Negative:** a user saving in several currencies needs separate goals.
- **Follow-up:** schema v4 adds `goals` and `goal_contributions`; the goal screen
  fixes the currency at creation.

## Review

Revisit if cross-currency goals become a stated need.
