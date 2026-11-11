# ADR 0022 – Account deletion and balance adjustment

- **Status:** Accepted
- **Date:** 2026-11-11
- **Deciders:** project owner
- **Related questions:** none

## Context

The ledger said account deletion is destructive and that archiving is preferred;
the exact flow was open. The owner now wants to delete any account, and to
reconcile an account with reality by changing its balance. A balance edit must
not break "balance is the sum of entries" or the audit trail.

## Decision

- **Hard delete any account with explicit confirmation.** The confirmation warns
  that the account and its transactions are removed. The operation removes the
  account, all of its entries, and both sides of every transfer that touches it,
  together with the transfer group. Archiving remains the safe alternative.
- **A balance change writes a system `adjustment` entry.** The amount is the
  difference between the target balance and the current sum of entries. The entry
  is excluded from income, expenses, budgets and the savings rate, and cannot be
  edited through the normal entry form.
- `adjustment` joins `opening` as a system entry type.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Archive-only deletion | Rejected by the owner; deletion is part of the account screen. |
| Edit the balance without an entry | Breaks the balance identity and loses the audit trail. |
| Soft-delete the account | Leaves a hidden row with no clear restore flow. |
| Cascade delete without cleaning transfers | Leaves a dangling transfer group or a one-sided pair. |

## Consequences

- **Positive:** accounts can be removed even when they have history; balance
  corrections are auditable and keep balances equal to the sum of entries.
- **Negative:** deletion is irreversible and can remove the other side of a
  transfer; the confirmation must state this clearly.
- **Follow-up:** `ledger.md` and `database.md` describe the new type and the
  cascade behavior.

## Review

Revisit if an undo or restore flow is added, or if deletion must be restricted
again.
