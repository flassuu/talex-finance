# Market Research

Read this when you are planning differentiation, pricing, positioning or a
release. This document is a framework, not a source of findings.

**Status:** not started. No market findings have been recorded. Do not cite this
file as evidence and do not present assumptions as research.

## Purpose

Understand which products already serve privacy-conscious personal finance
users, which capabilities are table stakes, and where Talex can be meaningfully
different. Research informs scope; it does not override the project principles.

## Research questions

| ID | Question | Why it matters |
|---|---|---|
| RQ-01 | Which Android finance apps target users who want local or self-hosted data? | Defines the competitive set. |
| RQ-02 | Which capabilities do users expect from a "complete" personal finance app? | Separates table stakes from optional scope. |
| RQ-03 | What are the most common complaints about cloud-based finance apps? | Tests whether the privacy position solves a real problem. |
| RQ-04 | Which import formats and migration paths do users need when switching? | Sets the minimum portability scope. |
| RQ-05 | Is there evidence of willingness to pay, and under which model? | Informs licensing and distribution, currently undecided. |
| RQ-06 | Where do existing apps stop — manual entry, analytics, shared budgets, sync? | Reveals gaps that are realistic to address. |

## Functional references

| Reference | Use |
|---|---|
| REF01 | Functional reference only: study which user problems it addresses and which workflows it exposes. Do not copy code, structure, identifiers or implementation. It is source-available under a noncommercial licence, which restricts direct reuse. Detailed behavior notes stay outside the repository; adopt/reject decisions are in [reference-ref01.md](reference-ref01.md). |

## Candidate competitors to evaluate

Candidates named by the owner, to be checked before any conclusion is drawn.
None of these has been assessed yet.

| Candidate | What to verify |
|---|---|
| Actual Budget | Local-first budgeting and financial planning; offline and privacy posture. |
| Firefly III | Self-hosted finance management; accounting model and reporting. |
| Money Manager (Realbyte) | Mobile expense tracking; usability and manual-entry flow. |
| Outbank | German multibanking; local storage and security approach. |
| YNAB | Zero-based budgeting; planning methodology. |
| Finanzguru | Banking automation; financial analysis and user experience. |

Positioning hypotheses derived from this list are recorded in
[plan.md](plan.md) and are not validated.

Other candidates must be identified and verified before they are added. Do not
list applications that have not been checked.

## Evaluation framework

Record the same fields for every candidate:

| Field | What to record |
|---|---|
| Platform and availability | Android, desktop, web; regions |
| Data model | Local-only, cloud account, self-hosted, hybrid |
| Security | Encryption at rest, authentication, backup behavior |
| Core modules | Accounts, transactions, budgets, goals, assets, liabilities, analytics |
| Portability | Export formats, restore, migration |
| Pricing | Free, one-time, subscription, open source |
| Strengths | Concrete observed advantages |
| Weaknesses | Concrete observed gaps, with source |
| Relevance to Talex | Which research question it informs |

## Research log

| Date | Source | Observation | Confidence |
|---|---|---|---|
| — | — | No entries yet. | — |

## Rules

- Record the source and date for every observation.
- Separate what was observed from what it might mean.
- Prefer primary sources: store listings, documentation, hands-on use.
- Do not generalize from a single review or a personal impression.
- Do not copy text, assets or code from a reference product.
- Link conclusions to the decision log or the open-question register.

## Related

- [Product overview](overview.md)
- [Product plan](plan.md)
- [Roadmap](../roadmap.md)
- [Open questions](../open-questions.md)
- [Decision log](../decisions/README.md)
