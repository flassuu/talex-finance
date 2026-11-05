# ADR 0013 — Cross-currency transfer with both amounts and a stored rate

- **Status:** Accepted
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** Q-028 (resolved by this ADR)

## Context

A transfer between accounts in different currencies changes two balances in two
currencies: 100 € leave a euro account and 110 $ arrive in a dollar account. The
ledger uses a linked pair of entries (D-024), and exchange rates live in a local
table with explicit refresh and optional per-transaction overrides (D-030).

If only the source amount is stored and the destination is derived at read time,
the historical transfer changes whenever the rate table changes. If only one side
is stored, one of the two balances is wrong. The product owner chose to record
both amounts, with the rate taken from the local table and editable.

## Decision

A transfer group stores both entry amounts and the rate used.

- The source entry is in the source account's currency and is negative.
- The destination entry is in the destination account's currency and is positive.
- The group stores the source amount, the destination amount, the rate, the rate
  date and the rate source.
- The user enters the source amount. The app proposes the destination amount using
  the local rate for the transfer date. The user may edit the destination amount
  and the rate; both are stored.
- If no rate is available for the date, the user must enter the destination amount
  manually; the app does not invent a rate.
- Same-currency transfers use rate 1 and equal magnitudes.
- Both amounts are authoritative for balances. The stored rate is for display,
  audit and reporting; it is derived from the two amounts and kept at sufficient
  precision. Rounding differences are part of the exchange, not an error.
- Editing the source or destination amount recomputes the rate. Editing the rate
  recomputes the destination amount. The UI shows which value was last edited.
  Past transfers are not rewritten by later rate-table changes.
- Transfers remain excluded from income, expenses, budgets and the
  works/idle/lost metrics. Net worth converts account balances using the rate for
  the relevant date (D-030), not the transfer's own rate.
- Validation: two entries, different accounts, opposite signs, same date, both
  amounts positive, rate positive, no category, no split lines.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Store only the source amount and derive the destination on read | Historical transfers would change whenever the rate table changes. |
| Store only the destination amount | The source balance would be wrong. |
| Require the user to type the rate but not the destination | The destination balance would be derived and could drift from the bank statement. |
| Use a hard-coded or network-fetched rate | Conflicts with D-030 (local table, explicit refresh, optional override). |
| Treat the two sides as unrelated income and expense entries | Destroys the transfer relationship and distorts reports. |

## Consequences

- **Positive:** both balances match reality; the exchange is auditable; later rate
  changes do not alter past transfers; same-currency and cross-currency transfers
  share one model.
- **Negative:** the transfer group carries more data; the UI has two amounts and a
  rate; rounding needs a documented rule; tests must cover rate edits.
- **Follow-up:** `docs/technical/ledger.md` defines the invariants;
  `docs/technical/database.md` records the transfer group; the rate table and
  per-transaction override follow D-030; reports and net worth use their own rate
  rules.
- **Resolved question:** Q-028 is closed as "both amounts plus a stored rate from
  the local table, editable per transfer".

## Review

Revisit if transfers need fees or commissions, if the rate table gains a different
precision policy, or if users need a transfer to be split across several
destination accounts.
