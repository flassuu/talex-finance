# ADR 0008 – Split transactions across one category tree

- **Status:** Accepted
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** none

## Context

A single purchase can cover several kinds of goods that belong to different
subcategories of the same parent category. A supermarket receipt may contain
groceries, sweets and alcohol in one payment. Recording it as one category
distorts reports, while recording it as several transactions makes the ledger
harder to reconcile with the bank statement and duplicates the payment metadata.

The ledger is single-entry with explicit transfer pairs (D-024), money is stored
in integer minor units (D-010), and categories have one level of subcategories
(ADR-0011). The split model has to fit all three constraints and remain usable
by budgets, reports and the capital dashboard.

## Decision

A transaction is either simple or split, never both:

- A simple transaction has one optional category.
- A split transaction has no category of its own and two or more split lines.
- Every split line has a category, a non-zero signed amount and an optional note.
- All split categories must belong to the same top-level category: either that
  parent itself or one of its direct children.
- The sum of the split line amounts must equal the transaction amount exactly, in
  minor units.
- Splits are allowed only on income and expense transactions, not on transfers.
- Editing a split replaces the whole set of lines atomically. An empty set turns
  the transaction back into a simple one, which may then take one category.
- Reports, budgets and category filters count a transaction once per top-level
  category it touches and roll each split line into its own leaf category.
- Deleting the transaction also removes its split lines.

The UI presents a split as one base category plus a list of lines. The base
category is a top-level category; each line selects that category or one of its
direct children. The form shows the unallocated remainder, over-allocation and a
fully allocated state, calculated in minor units.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Two or more separate transactions | Duplicates the payment, breaks reconciliation and makes one receipt look like several purchases. |
| One category for the whole receipt | Simple, but categories and budgets become inaccurate and the main use case is lost. |
| Arbitrary categories from different parents | Breaks roll-up: one receipt would be split across unrelated top-level reports. |
| JSON array on the transaction row | Harder to validate, query and index; split lines need their own identity for edits and reporting. |
| Full double-entry postings for each line | More accounting machinery than a personal finance app needs; D-024 already chose single-entry with explicit transfer pairs. |

## Consequences

- **Positive:** one receipt stays one transaction; category reports and budgets
  can still distinguish the goods; the bank statement reconciles.
- **Negative:** every aggregation that reads categories must also read split
  lines; a category filter has to include split membership; the form is more
  complex than a single category select.
- **Follow-up:** `docs/technical/database.md` gains a split entity;
  `docs/technical/ledger.md` defines the write and aggregation rules; reports,
  budgets and exports must be tested with split data.
- **Limits:** a purchase spanning unrelated top-level categories is recorded as
  separate transactions, not as one split.

## Review

Revisit if users need splits across different top-level categories, if split
lines need their own tags or attachments, or if reporting queries become too
complex to maintain. A future change must supersede this ADR, not edit it.
