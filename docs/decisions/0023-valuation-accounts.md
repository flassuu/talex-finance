# ADR 0023 – Valuation-based asset accounts and manual valuations

- **Status:** Accepted
- **Date:** 2026-11-12
- **Deciders:** project owner
- **Related questions:** Q-032 (resolved by this ADR); refines D-025 and roadmap 3.3

## Context

D-025 keeps one account model for accounts, assets and liabilities, and roadmap
3.3 says valuation-based accounts use the latest dated valuation. The account
model had no asset types and no way to record a value over time, so property and
vehicles could not be represented. The valuation cadence was left open in
`docs/technical/database.md`.

## Decision

Add valuation-based account types `PROPERTY`, `VEHICLE` and `ASSET` to the one
account model, and a `valuations` table with one dated amount per account in that
account's currency.

- A valuation row stores the account, the amount in the account currency, and the
  valuation date.
- The balance of a valuation-based account is the latest valuation on or before
  the date being shown; it is not the sum of ledger entries.
- Valuations are entered manually on demand. There is no schedule, no automatic
  refresh and no external price source.
- An account is either entry-based or valuation-based by its type; transfers,
  expenses and income apply only to entry-based accounts.
- The first valuation can be entered when the account is created; it is stored as
  a valuation, not as an opening entry.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| A boolean "valuation-based" flag on existing types | Splits the meaning of a type and makes "what is an asset" implicit; explicit types read better in reports. |
| A separate asset entity | Conflicts with the one account model (D-025) and adds a parallel balance calculation. |
| Automatic price refresh | Needs network access, which the product deliberately avoids (D-001, D-019). |
| Scheduled revaluation prompts | Adds notification machinery that the first release does not need. |

## Consequences

- **Positive:** property, vehicles and other assets join net worth and the
  works/idle/lost model without a second entity; a valuation is a plain dated
  amount that is easy to audit.
- **Negative:** the user must remember to update valuations; an asset balance is
  not derived from the ledger, so it is a separate code path in balance, net worth
  and trend calculations.
- **Follow-up:** schema v4 adds `valuations`; `docs/technical/database.md` and
  `docs/technical/ledger.md` describe the valuation path; the assets screen groups
  asset and liability accounts and offers manual valuation entry.

## Review

Revisit if assets need scheduled valuations, multiple value sources, or a
liability schedule.
