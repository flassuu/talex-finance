# ADR-0034 – Configurable bottom bar slots

- **Status:** Accepted
- **Date:** 2026-11-24
- **Deciders:** project owner
- **Related questions:** Q-041

## Context

D-039 promises a configurable bottom bar with a center quick-add button and a
trailing menu. The shipped bar is fixed: Overview, Transactions, the center
"+" and Accounts, plus a trailing More menu. Q-041 asked which screens may
occupy the configurable slots and how many slots exist.

The bar is part of the app shell. D-039 also requires the shell to be available
before unlock, so the slot choice cannot live in the encrypted database.

## Decision

The bottom bar has **three configurable slots**. Any of the main destinations can
fill a slot: Overview, Transactions, Accounts, Budgets, Goals, Assets, Analytics,
Recurring, Categories and Settings. The defaults are Overview, Transactions and
Accounts. The center quick-add button and the trailing More menu are fixed.

A slot choice must be distinct. Selecting a destination that already fills
another slot swaps the two, so the three slots never duplicate.

The choice is stored as an ordered list of route keys in plaintext
SharedPreferences, next to the language and lock preferences, outside the
encrypted database. It contains no financial data.

The More menu lists every main destination, so a screen removed from the slots
stays reachable.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Keep the bar fixed | Contradicts D-039. |
| Exclude Settings from slots | Settings is a normal destination; the More menu already leads to it, so allowing it costs nothing. |
| Store the choice in the database | The shell and its preferences must be available before unlock; the database is not open then. |
| Allow fewer than three slots | The bar layout expects three positions; always filling three keeps it balanced and avoids an empty slot. |

## Consequences

- **Positive:** the user can put the screens they use first on the bar; the
  shell preference survives restarts and is available before unlock; every
  destination stays reachable through More.
- **Negative:** two places (settings and the bar) must agree on the destination
  list; invalid or stale route keys need a fallback to the defaults.
- **Follow-up:** Q-041 is resolved; the destination catalogue and the
  preference sanitisation get unit tests; `roadmap.md` 5.4 is implemented.

## Review

Revisit if the bar gains notification badges or if any destination becomes
conditional (for example a screen that only exists after data is created).
