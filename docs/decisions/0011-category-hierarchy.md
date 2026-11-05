# ADR 0011 — One-level category hierarchy with archival

- **Status:** Accepted
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** none

## Context

Categories classify income and expenses and feed reports, budgets and the capital
dashboard. The reference implementation allows one level of subcategories and
deletes a category by setting references to null, which silently removes
classification from history. Talex needs a hierarchy that is simple enough to
aggregate reliably and a deletion policy that does not rewrite the past.

The product owner chose one level of subcategories: a parent may have children,
but a child cannot have children of its own.

## Decision

Categories form a two-level tree with a fixed kind.

- Every category has a kind: income or expense. The kind is set at creation and
  cannot change.
- A category may have a parent. The parent must be top-level, must have the same
  kind, and cannot be the category itself.
- A category that already has children cannot become a child.
- Category names are free text; default categories have stable keys for
  translation.
- Reports, budgets and the capital dashboard roll a child's amounts into its
  parent. A parent's total includes direct entries on the parent and entries on
  its children.
- A category that is referenced by any entry, split line, child category or
  budget cannot be hard deleted. It can be archived instead.
- Archiving a parent archives its children with it; restoring the parent restores
  them. Archived categories disappear from pickers and new entries but remain
  visible in historical reports, budgets and existing entries.
- An unreferenced category may be hard deleted. This covers mistakes made during
  setup without leaving clutter.
- Archiving a category closes its current budget period; historical budget
  amounts remain comparable.
- Archived categories cannot be selected for new entries, budgets or recurring
  rules.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Hard delete and set references to null (REF01 model) | History loses its classification silently; reports and budgets become incomplete. |
| A flat list with no subcategories | Too coarse for receipts and for the "sweets inside groceries" use case the owner wants. |
| Arbitrarily deep hierarchy | Complicates roll-up, budgets and the UI; one level covers the stated need. |
| Archive everything, never hard delete | Leaves abandoned setup categories forever; unused categories have no history to preserve. |
| Reassign references before deletion | A useful future merge tool, but too much for the first release. |

## Consequences

- **Positive:** history keeps its classification; pickers stay clean; roll-up is
  predictable; the model is ready for tombstones and future synchronization.
- **Negative:** every category query needs an archived filter; archiving a parent
  affects children; a deleted unused category cannot be restored; an existing
  recurring rule that points at an archived category is skipped and reported
  during generation.
- **Follow-up:** `docs/technical/database.md` records the parent and archival
  fields; `docs/technical/ledger.md` defines category selection and roll-up;
  exports and backups must preserve archived state.
- **Limits:** no category merge, no renaming of default category keys, no
  per-profile category sets in the first version.

## Review

Revisit if users need deeper nesting, category merging, or per-profile category
sets. A change to the hierarchy must supersede this ADR and include a migration
plan for existing references.
