# ADR-0038 – Refunds linked to an original expense

- **Status:** Accepted
- **Date:** 2026-11-26
- **Deciders:** project owner
- **Related questions:** Q-042 (answered here)

## Context

Phase 6.1 adds refunds: an expense (for example 6 EUR on Amazon) is reversed a few
days later. A plain negative expense would still inflate spending, and a matching
income row would inflate income. The original transaction must stop counting as
spending without breaking the account balance or the history.

## Decision

A refund is a new entry type `refund` with a positive amount, written to the same
account and currency as the original expense. It links to the original through a
nullable `entries.refunded_entry_id` column (schema v5, additive, indexed). The
rules:

- A refund must link to a live `expense` on the same account and may be partial,
  but never larger than the original. A refund has no category and no split lines.
- Balance math is unchanged: the original expense and the refund cancel out on the
  account.
- Reports never treat a refund as income or a category total. Instead the linked
  refunds reduce the original's contribution to spending. A fully refunded expense
  drops out; a partial refund leaves the remaining amount.
- The reduction applies to the capital overview, analytics, budgets and the cash
  flow that feeds the insight signals.
- Editing a refund keeps its link; deleting the original does not silently delete
  the refund.

Creating a refund from the transaction screen and exporting refunds in CSV are
follow-ups; the data model and the reporting rules ship first.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| A plain negative expense with no link | Spending still looks inflated; the original is never netted out. |
| A positive income entry | Inflates income and breaks the "refunds are not income" rule. |
| An offsetting pair of entries | Doubles the history and complicates budgets and reports. |
| Defer refunds entirely | Leaves a common everyday case unsupported. |

## Consequences

- **Positive:** a refunded expense stops inflating spending while the account
  balance and history stay correct; the link is explicit and auditable.
- **Negative:** every spending computation must consult the refund map; schema v5
  and the reports carry a little more complexity.
- **Follow-up:** create-refund UI on the transaction screen, CSV export/import of
  refunds, and a unit test for the netting across overview, analytics and budgets.

## Review

Revisit if refunds should be allowed across accounts or currencies, or if the
reporting rule should keep the original and show the refund as an offsetting line.
