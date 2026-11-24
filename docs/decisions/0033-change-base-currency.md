# ADR-0033 – Changing the base currency

- **Status:** Accepted
- **Date:** 2026-11-24
- **Deciders:** project owner
- **Related questions:** Q-038

## Context

Accounts, entries, goals and valuations each store their own currency. Budgets
are different: a limit is entered and shown in the base currency, and
`BudgetMath` treats the stored limit as already being in the base currency. The
base currency itself is a value in the `settings` table and has so far been
chosen only in the onboarding wizard.

Q-038 asks whether the base currency may change after data exists and how the
effect on reports is shown. Reports (overview, analytics, budgets) convert to
the base currency at read time, so a change rescales them without rewriting
history. Budget limits, however, are stored numbers, so a base change would
silently reinterpret them as a different currency unless they are converted.

## Decision

The base currency can be changed from Settings at any time. The change is one
atomic operation:

1. Every open budget limit (`closed_at IS NULL`) is converted to the new base
   currency using the latest rate on or before the change date, and its amount
   and currency are updated.
2. The new base currency is written to `settings`.
3. Closed budget limits and all other stored money are left untouched, because
   they carry their own currency and reports convert them on read.

Before applying, the screen shows a preview of each open limit as "old → new".
If any open limit has no rate to the new base currency, the change is blocked
until the rate is recorded on the rates screen. Accounts and goals whose
currency has no rate are reported as a warning, not a block, because their
reports fall back to the unconverted amount (ADR-0010 marks affected budget
states incomplete rather than guessing).

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Leave budget limits as they are | The stored number would be read as the new currency, so every open budget would be wrong by the exchange factor. |
| Ask the user to re-enter limits | Loses the plan and is easy to do incorrectly. |
| Forbid changing the base currency | Contradicts D-030 and the assumption that reports are base-currency views. |
| Convert closed limits too | Rewrites history and breaks the effective-dated budget trail (ADR-0010). |

## Consequences

- **Positive:** the base currency becomes a real, reversible setting; open plans
  stay comparable across the change; the internal invariant "an open limit is in
  the base currency" is preserved.
- **Negative:** the change is blocked when a needed rate is missing; the budget
  conversion must run in one transaction with the settings write.
- **Follow-up:** Q-038 is resolved; `database.md` records that open budget
  limits follow the base currency and closed ones keep their own. The preview
  and the conversion get unit tests.

## Review

Revisit if budgets ever gain a per-budget currency (ADR-0010 would be
superseded) or if the base currency becomes a per-report choice.
