# ADR-0035 – Capital roles and risk tiers

- **Status:** Accepted
- **Date:** 2026-11-24
- **Deciders:** project owner
- **Related questions:** Q-029 (answered by ADR-0016, amended here)

## Context

ADR-0016 chose the simple works / idle / lost model and explicitly rejected
REF01's capital roles (income / neutral / drain) and risk tiers (low / medium /
high) as a different axis, deferring them. The reference analysis shows these
are cheap, high-value analytics that deepen the capital dashboard – the
product's emotional core. The owner now wants them as a second axis, keeping
works / idle / lost as the primary metric.

## Decision

Keep works / idle / lost unchanged as the primary capital metric. Add, as a
second axis on the capital dashboard, two user-assigned attributes on each
valuation-based account:

- **Role:** income / neutral / drain.
- **Risk:** low / medium / high.

Plus an optional reference **monthly cash flow** per asset that is never a
transaction, never part of a balance and never enters income, expenses or
budgets.

Roles and risk are assigned by the user, not derived from account type: the same
notebook can be a work tool (neutral) or a dead weight (drain). Cash-type
account balances have no role and always count as low risk. The three roles and
three risk tiers are always present in the breakdowns, even at zero, so the
blocks read as a fixed scale. The 80/20 rule – the medium and high share of
capital should stay at or below 20% – feeds the risky-allocation alert
(ADR-0036).

The columns are additive on the account model; schema v5 migrates `v4 -> v5`.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Keep rejecting (ADR-0016 as written) | Simpler, but loses the risk/role view and the reference's strongest cheap analytics. |
| Derive role/risk from account type | Brittle: the same asset type can differ by usage (paid-off vs mortgaged property). |
| Fold roles/risk into the primary metric | Would destabilize the simple three numbers; a second axis keeps them stable. |

## Consequences

- **Positive:** risk concentration and the role mix become visible; feeds the
  risky-allocation alert; the capital dashboard gains depth without changing its
  headline numbers.
- **Negative:** two more fields to maintain per asset; schema v5 migration and
  breakdown UI work.
- **Follow-up:** mark ADR-0016 superseded; update `reference-ref01.md`
  (A-CAP-04, A-CAP-05); implement `roadmap.md` 6.2; test the `v4 -> v5`
  migration.

## Review

Revisit if roles/risk add entry friction without value, or if a reliable derived
classification appears.
