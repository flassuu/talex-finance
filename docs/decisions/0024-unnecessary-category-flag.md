# ADR 0024 – The "unnecessary" category flag for the lost metric

- **Status:** Accepted
- **Date:** 2026-11-12
- **Deciders:** project owner
- **Related questions:** Q-033 (resolved by this ADR); completes ADR-0016

## Context

ADR-0016 defines lost money as the current period's subscriptions plus spending
the user marks as unnecessary, but deliberately deferred the marking mechanism.
Phase 2 shipped only the subscription half, so the lost metric was incomplete.

## Decision

Mark spending as unnecessary with a boolean flag on an expense category. The lost
metric for the current month is the sum of expense entries that are either
generated from a recurring rule or belong to a category marked unnecessary.

- The flag lives on the category, not on an entry and not in a reserved category.
- A parent flag applies to its own entries and to its children only through the
  parent roll-up; each category carries its own flag.
- The flag is part of the category model and the category editor.
- An entry counted as recurring is not counted a second time for its category.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| A flag on each entry | Exact, but adds a decision to every expense and complicates the entry form. |
| A reserved seeded category | Fragile: the user can rename or archive it, and it conflicts with the one-category tree. |
| Keep only subscriptions | Leaves the product's central "lost" idea half-implemented. |

## Consequences

- **Positive:** lost becomes meaningful for one-off waste, not only subscriptions;
  the rule stays a single predicate on categories.
- **Negative:** a whole category is either wasteful or not, so mixed spending needs
  a child category or a split.
- **Follow-up:** schema v4 adds `categories.unnecessary`; the category editor
  exposes the toggle; the overview tests cover the union of recurring and flagged
  spending.

## Review

Revisit if users need a per-entry override or a temporary flag for a single month.
