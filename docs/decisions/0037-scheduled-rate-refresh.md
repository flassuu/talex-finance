# ADR-0037 – Scheduled and per-currency rate refresh

- **Status:** Accepted
- **Date:** 2026-11-24
- **Deciders:** project owner
- **Related questions:** none
- **Extends:** ADR-0032 (the "never scheduled" clause is superseded)

## Context

ADR-0032 made rate fetching explicit: a manual action only, with no automatic
network access. In practice this is too rigid. Users want a fallback for
currencies ECB data omits (UAH), a way to keep only some pairs in sync, a
configurable number of decimals, and an optional refresh schedule. They also
want to know what leaves the device before the first sync.

## Decision

Rate synchronisation becomes a set of opt-in preferences stored in the encrypted
`settings` table with the profile:

- **Providers.** The primary source stays frankfurter.dev (ECB). When it does not
  return a currency, open.er-api.com fills the gap. Both are HTTPS, keyless and
  receive only the base and target currency codes.
- **Schedule.** The user chooses one of: only manually, on every launch, weekly,
  monthly, or a custom number of days. The schedule is evaluated on unlock; there
  is no background service, worker or scheduler. A due refresh runs only while
  the app is open and unlocked.
- **Per-currency switches.** Each pair can be set to sync or to stay manual. A
  manual-only pair is skipped by both the button and the schedule.
- **Precision.** One global setting from 2 to 8 decimal places. Fetched and manual
  rates are rounded to it before storage and display.
- **First-use disclosure.** The first time the user enables sync, an alert names
  both providers and states that only currency codes are sent and that accounts
  and amounts stay on the device. The acknowledgement is stored with the profile.
- **Manual action.** The "Update rates" button remains and follows the same
  per-currency switches and precision.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Keep explicit-only refresh | Does not meet the request and leaves UAH unusable automatically. |
| WorkManager or a background job | Adds a dependency and background network the privacy model avoids; the app already refreshes recurring entries on unlock (ADR-0009). |
| A provider per currency | More configuration for little gain; a fallback chain is simpler. |
| Per-pair precision | More settings to maintain; one global precision covers the request. |

## Consequences

- **Positive:** UAH and other non-ECB currencies can be fetched; users control
  which pairs and when; the fetch is transparent on first use.
- **Negative:** with a non-manual schedule the app may use the network on unlock;
  that is the user's explicit choice. A failed fetch retries on the next due
  launch. The fallback provider is a third-party service and may change.
- **Follow-up:** the network boundary in `security.md` now allows scheduled
  refresh; `roadmap.md` records the enhancement; unit tests cover due dates,
  precision and the fallback merge.

## Review

Revisit if a provider changes terms, if the app gains a real background
scheduler, or if rate sync should become per-profile visible to the user.
