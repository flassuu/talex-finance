# Product Overview

Read this when you need to understand what Talex is, who it serves, and what is
in scope. For technical structure, start with
[../technical/architecture.md](../technical/architecture.md).

**Status:** the product concept and principles are established. Detailed
requirements, information architecture and success metrics remain open.

## One-liner

Talex is a local-first personal finance application that gives one person a
complete view of their money – accounts, transactions, budgets, savings goals,
assets and liabilities, and analytics – without requiring an account or a
server.

## Why

Personal finance data is scattered across bank apps, spreadsheets and cloud
services. Reconstructing a complete picture takes manual work, and most tools
ask the user to trust an external service with the most sensitive data they
have.

Talex takes the opposite position: the device is the primary home of the data,
the application works fully offline, and the user remains in control of backup,
export and migration.

This is a product motivation, not a market claim. Market evidence is not yet
collected; see [market-research.md](market-research.md).

## Who it is for

An individual who manages their own finances and values privacy and local
ownership over convenience features that require a cloud account. Talex starts
with one profile; a future version may allow separate profiles against one
database (D-027).

## Principles

| Principle | Practical implication |
|---|---|
| Local-first | Every core workflow works with no network connection. |
| Privacy-first | No mandatory account, no server dependency, no silent telemetry. |
| Security | Data is encrypted at rest; the master passphrase is the root of trust and biometrics are a local convenience. |
| Data ownership | Backup, restore, export and migration are first-class features. |
| Extensibility | The domain model is independent of Android so future clients can reuse it. |
| Portability | No device synchronization; export/import of the encrypted snapshot moves data between devices. |

## Capability areas

The following areas define the intended product. They are not implemented and
their detailed behavior is not specified yet. The working breakdown of these
areas and the MVP direction live in [plan.md](plan.md).

| Area | Purpose |
|---|---|
| Accounts | Represent where money is held and track balances. |
| Transactions | Record income and expenses, including transfers and categorization. |
| Budgets | Set monthly spending limits for categories and track progress. |
| Savings goals | Define targets and track progress toward them. |
| Assets and liabilities | Track holdings and debts outside ordinary transaction flow. |
| Analytics | Summarize spending, income, trends and progress. |
| Portability | Back up, restore, export and migrate the complete data set. |
| Access control | Protect the application with biometric authentication. |

## Scope

| In the initial product | Not in the initial product |
|---|---|
| Android application | Desktop, web and iOS clients |
| Fully offline operation | Device synchronization (export/import instead) |
| Local encrypted storage | Mandatory accounts or an external server |
| The capability areas above | Bank integrations (manual entry and CSV instead) |

Bank integrations, automatic imports and shared household use are open
questions rather than confirmed exclusions; see
[../open-questions.md](../open-questions.md).

## Success criteria

Not defined. The project needs measurable criteria for a usable personal
finance product before implementation priorities can be set. Tracked as an open
question in [../open-questions.md](../open-questions.md).
