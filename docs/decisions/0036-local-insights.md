# ADR-0036 – Local proactive insights (alerts and advice)

- **Status:** Accepted
- **Date:** 2026-11-24
- **Deciders:** project owner
- **Related questions:** none open (previously listed as dropped)

## Context

The roadmap and `ux.md` had dropped "proactive alerts and advice" as out of
scope. REF01 makes proactivity one of three product pillars and
specifies five alert signals and three advice generators. Talex already holds
all the underlying data: the net-worth series, budgets, cash flow and accounts.
The owner now wants this layer back, computed locally and offline.

## Decision

Add a local insights feature with two surfaces:

- **Alerts** (warning tone) shown as a banner on the overview.
- **Advice** (positive / neutral / warning tone) in a dedicated list.

Five alerts, computed on read, with no background task and no network:

1. Negative cash-flow streak – two or more completed months with net below zero.
2. Net-worth decline streak – two or more completed months where month-end
   capital is below the previous month.
3. Budget exceeded – at least one budget over its limit in the current month.
4. Risky allocation – the medium and high share of capital above 20%.
5. Idle cash – an account with a balance at or above a threshold and no activity
   for at least N days.

Three advice generators:

1. Rising category – an expense category at least 25% above its three-month
   average.
2. Top unbudgeted category – the largest current-month expense category without
   a limit.
3. Savings-rate trend – the savings rate changed by at least 5 percentage points
   versus the previous month.

Only completed months count for the streak signals; the budget signal uses the
current month so the user can react in time. Thresholds are configurable in
Settings and each signal can be disabled. Advice respects splits and subcategory
roll-up, unlike the reference. Everything is a pure domain function with unit
tests; no ML, no network and no new dependency.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Keep it dropped | Simplest, but the app stays purely descriptive and loses a product pillar. |
| Push notifications / a background scheduler | Contradicts local-first and "no silent background"; the rules can run on read. |
| Derive advice from an external service | Contradicts the privacy principles and offline operation. |

## Consequences

- **Positive:** the app becomes proactive and ties into the "found unnecessary
  spending" narrative while staying fully local and private.
- **Negative:** more strings and threshold settings to design, localize and
  test; risk of noisy alerts.
- **Follow-up:** update `reference-ref01.md` (A-AN-04, A-AN-05); implement
  `roadmap.md` 6.3 and 6.4; store thresholds with the app settings and allow
  per-signal disable.

## Review

Revisit after real use if alerts feel noisy; adjust defaults and keep the
per-signal switch as the safety valve.
