# ADR-0032 – Manual and opt-in exchange-rate refresh

- **Status:** Accepted
- **Date:** 2026-11-24
- **Deciders:** project owner
- **Related questions:** Q-039

## Context

D-030 says that exchange rates live in a local table and that selected
currencies refresh on explicit user action, with an optional per-transaction
override. Until now only the per-transfer rate was stored: creating a
cross-currency transfer writes one row into `rates` for that date (ADR-0013).
There is no screen to inspect, add or change a rate, and no way to update the
whole table.

The app has advertised itself as fully offline and the Android manifest states
that no `INTERNET` permission is ever requested. "Explicit refresh" in D-030
therefore had no implementation path: nothing could fetch rates.

This decision defines how rates enter the local table and deliberately changes
the network stance from "never" to "only on an explicit user action".

## Decision

Rates are managed on a dedicated screen and can be entered in two ways:

1. **Manual entry.** The user adds or overrides a rate for a pair and a date;
   the value is stored with `source = "manual"`.
2. **Opt-in fetch.** A single "Update rates" action fetches rates for the pairs
   the data currently uses from `frankfurter.app` (ECB reference data, HTTPS,
   no key) and stores them with `source = "fetched"` and today's date.

The fetch is never automatic: no background job, scheduler, startup refresh or
telemetry. It runs only after the user taps the action. The network stack is the
platform's `HttpURLConnection` plus the built-in JSON parser, so no new
dependency is added. A failure leaves the table untouched.

Only one direction is stored per pair: the base currency to the quote currency.
The inverse is derived by division at read time, as it already is.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Manual entry only | Simplest and fully offline, but tedious for many currencies and misses D-030's "refresh". |
| Import rates from a CSV file | Useful later, but a second format and parser for a small gain; the screen covers the need. |
| Automatic or scheduled refresh | Violates the offline-first stance and adds background network and privacy risk. |
| A full third-party client library (OkHttp/Retrofit) | A new dependency for one HTTP GET; the platform stack is enough. |

## Consequences

- **Positive:** rates are usable and auditable; the table can be populated in
  one tap or by hand; the network boundary stays narrow and explicit.
- **Negative:** the app now requests `INTERNET`; `frankfurter.app` does not
  publish every currency (notably UAH is absent from ECB reference data), so
  those pairs must be entered manually.
- **Follow-up:** the manifest comment changes from "no INTERNET permission" to
  "network only for the explicit Update rates action"; `security.md` records the
  boundary; Q-039 is resolved. The offline promise becomes "offline by default,
  network only on an explicit tap".

## Review

Revisit if a refresh provider covers the currencies in use better, if the app
ever needs automatic refresh (which would contradict D-001), or if the provider
changes its terms or endpoint.
