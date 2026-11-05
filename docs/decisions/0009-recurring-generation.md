# ADR 0009 — Catch-up generation of recurring transactions

- **Status:** Accepted
- **Date:** 2026-11-05
- **Deciders:** project owner
- **Related questions:** none

## Context

Recurring transactions are part of the first release (D-017). The reference
implementation deliberately required a manual "post" action, because a missed
period should not silently create a pile of entries. The product owner wants the
opposite: subscriptions, utilities and internet should be recorded automatically,
with a dialog on entering the app that reports what was created.

The application is local-first and its database is encrypted. The key is derived
from the master passphrase and is normally available only after the user unlocks
the app. A background worker cannot open the database while the app is locked
without keeping a device-bound key, which would weaken the "passphrase is the
root of trust" model (D-021). The decision therefore has to work within the
unlock boundary.

## Decision

Recurring transactions are generated automatically on app open, after the
database is unlocked and before the main screen is shown.

- Each rule stores its template, frequency, anchor date, next due date and
  active flag.
- On every unlock, the app walks each active rule and creates one transaction per
  due occurrence whose scheduled date is on or before today. It runs on cold
  start and on unlock, not on every screen navigation.
- The generated transaction is dated on the **scheduled due date**, not on the
  day the app was opened. This keeps monthly reports correct.
- All missed occurrences are generated, including several months at once.
- Each generated transaction stores the rule identifier and the schedule period
  it belongs to. A unique constraint on that pair makes generation idempotent:
  opening the app twice never duplicates an entry.
- After generation, the app shows a summary dialog listing what was created,
  grouped by rule: name, amount and date. The entries are ordinary transactions
  and can be edited or deleted individually. They carry a visible "created
  automatically" marker.
- Paused rules generate nothing. Resuming a rule advances its next due date to
  the first occurrence on or after today; periods missed while paused are not
  backfilled.
- Archived accounts or archived categories generate nothing. The dialog reports
  the skipped rule so the user can fix the account, restore the category or pause
  the rule.
- A rule's amount, description, category and account apply to future generated
  entries. Changing the frequency or anchor is allowed only before the first
  generated entry; afterwards the user creates a new rule instead of rewriting
  history.
- Recurring transfers create both linked entries of the transfer pair atomically
  (D-024).
- Splits and tags are not supported on recurring rules in the first version.

No background process writes to the database. A future reminder notification may
be scheduled through the platform, but it only notifies; it does not open the
database or create entries.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Manual "post" action, as in REF01 | The product owner wants automatic recording and a summary, not a per-payment click. |
| Background worker writes to the database | It would need a key available while locked, weakening the passphrase-based security model. |
| Generate only the latest missed occurrence | Loses real payments and makes monthly reports incomplete. |
| Generate silently on app start without a dialog | The user would not know what changed; the dialog is part of the requirement. |
| Date generated entries on the day the app opens | Distorts the month in which the payment actually happened. |
| Backfill periods missed while a rule was paused | Pausing means "skip", not "defer"; resuming should not create a backlog. |

## Consequences

- **Positive:** subscriptions are recorded without manual work; missed periods
  are recovered; the date and amount stay faithful to the schedule; the user sees
  exactly what was created.
- **Negative:** opening the app after a long absence may generate many entries at
  once; the unlock flow becomes part of the write path; schedule changes after the
  first generation require a new rule.
- **Follow-up:** the ledger needs fields for the source rule and schedule period;
  `docs/technical/ledger.md` defines how generated entries behave; tests cover
  month-end clamping, leap years, pause/resume, archived accounts, idempotency and
  transfers.
- **Limits:** no template history; if the amount changes retrospectively, already
  generated entries keep the amount they were created with. No per-rule time zone
  override; dates use the device's local calendar day.

## Review

Revisit if Android background execution can be made compatible with the
encryption model without weakening it, if users need a per-rule "skip this
period" action, or if catch-up generation after long gaps becomes confusing.
