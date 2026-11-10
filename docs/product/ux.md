# UX and Design

Read this when you are designing or changing screens, navigation, themes, the
transaction entry flow or onboarding. This document owns the visual and
interaction decisions; individual screens are specified in feature specs during
implementation.

**Status:** direction agreed. Details are fixed screen by screen as each phase
is built.

## Visual style

- Material 3 with dynamic color that follows the wallpaper, in light and dark
  themes (D-040).
- Material symbols for icons; keep decorative elements minimal.
- Money amounts can be hidden with a toggle for privacy.
- Motion is subtle and short; avoid long animations on core flows.

## Navigation

- A bottom navigation bar with a fixed layout:
  - up to three user-chosen screen slots;
  - a center **+** button for the quick-add transaction form (D-041);
  - a trailing **…** button that opens an animated drop-up FAB menu with the
    remaining screens and settings (D-039).
- Default slots are Overview, Transactions and Accounts; the user can change
  which screens occupy the slots.

## Quick add

- The center **+** opens a fast transaction form: amount, account, category and
  date, with the type defaulting to expense. Recording a transaction is a
  two-to-three tap action.

## Onboarding

- First run offers a choice: create a profile, or open a demo profile (D-042).
- The create path is a wizard: master passphrase → base currency → accounts →
  categories.
- The demo profile seeds example data so the application is browsable
  immediately; it is clearly marked and can be deleted.

## Overview

- The overview is the capital dashboard: cards with total capital, the
  works / idle / lost split, income and expenses for the period, the net-worth
  trend and the savings rate, with small inline charts (D-043).

## Screens

- Overview, Transactions, Accounts, Budgets, Goals, Assets and liabilities,
  Analytics, Settings.

## Feature scope

Decided for the product:

- In: savings rate on the overview, cash flow by month, asset value dynamics,
  crypto portfolio (later), ROI calculator (later).
- Out: forecast and planned expenses, proactive alerts and advice, bank-specific
  CSV profiles, category ranking reports.
