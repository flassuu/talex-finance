# REF01 Reference Disposition

Read this when you are considering a behavior from REF01, or when you need to
know which parts of the external reference were deliberately not adopted. This
file records decisions, not mechanics. The detailed behavior notes stay outside
the repository.

**Status:** active. REF01 is an external personal-finance application studied as a
functional reference in a clean-room process. Its identity is kept out of the
repository on purpose; use the code **REF01** in all project documents. Its
licence does not permit direct reuse, so only observed behavior is studied. No
code, file structure, identifiers or implementation are copied (D-007).

## Rules for using REF01

1. **Behavior only.** Record what the user sees and what the rules are, not how
   the reference implements them.
2. **Talex decisions win.** If a behavior conflicts with an accepted decision, it
   is rejected or adapted through an ADR; it is never mixed in silently.
3. **One owner per fact.** This file says whether a behavior is adopted and why.
   The owning technical or product document defines how it works.
4. **No scope creep.** A useful REF01 feature that is not in the current phase is
   marked `Defer`, not implemented early.
5. **No copied prose.** Describe behavior in Talex terms; do not paste reference
   documentation.

## Verdicts

| Verdict | Meaning |
|---|---|
| **Adopt** | Behavior fits Talex and is recorded in an owning document or ADR. |
| **Adapt** | The idea is kept, but the representation or scope changes for Talex. |
| **Reject** | Conflicts with an accepted decision, security model or product principle. |
| **Defer** | Useful later; not in the current phase. |

## Disposition matrix

### Accounts and ledger

| ID | Behavior | Verdict | Reason | Owner |
|---|---|---|---|---|
| A-ACC-01 | Account balance is derived from entries, never stored as a mutable number. | Adopt | Removes drift and makes corrections automatic. | `technical/ledger.md` |
| A-ACC-02 | Separate account and asset entities. | Reject | Conflicts with D-025 (one account model with types and valuations). | `technical/database.md` |
| A-ACC-03 | Account archiving hides it without deleting history. | Adopt | Needed for day-to-day usability and history. | `technical/ledger.md` |
| A-ACC-04 | No opening balance; accounts start at zero. | Reject | D-008 requires balances from initial balances and entries; the starting amount is a system opening entry (ADR-0012). | ADR-0012 |
| A-LED-01 | One transfer row with two account references. | Reject | Conflicts with D-024 (explicit transfer pair). | `technical/ledger.md` |
| A-LED-02 | Positive amount plus a type field. | Adapt | Talex uses signed integer minor units, so a same-currency pair sums to zero and a balance is a simple sum. | `technical/ledger.md` |
| A-LED-03 | One purchase divided across subcategories of one parent. | Adopt | Solves the receipt case; recorded with exact-sum and one-parent rules. | ADR-0008 |
| A-LED-04 | Transfers excluded from income, expenses, budgets and reports. | Adopt | A transfer is not economic income or spending. | `technical/ledger.md` |
| A-LED-05 | Bulk import is all-or-nothing. | Adopt | A partial import is worse than a rejected one. | `technical/ledger.md` |
| A-LED-06 | Description is auto-capitalized. | Reject | Cosmetic, locale-dependent and surprising for user-entered text. | — |
| A-LED-07 | Hard delete without a tombstone. | Reject | Future sync (D-023) and the plan's change-tracking direction require soft deletion and tombstones. | `technical/database.md` |

### Categories and tags

| ID | Behavior | Verdict | Reason | Owner |
|---|---|---|---|---|
| A-CAT-01 | One level of subcategories. | Adopt | Covers the receipt case without deep-tree complexity. | ADR-0011 |
| A-CAT-02 | Child spending rolls up into the parent. | Adopt | Required for budgets, reports and the capital dashboard. | `technical/ledger.md` |
| A-CAT-03 | Deleting a category nulls references. | Reject | Silently removes classification from history; archive instead. | ADR-0011 |
| A-CAT-04 | Default categories cannot be deleted while used. | Adapt | Talex archives any referenced category instead of special-casing defaults. | ADR-0011 |
| A-CAT-05 | Free-form tags shared by many entries. | Adopt | Tags are in the Phase 2 scope and are orthogonal to categories. | `technical/ledger.md` |
| A-CAT-06 | Case-insensitive tag de-duplication. | Adopt | Avoids near-duplicate tags. | `technical/ledger.md` |

### Planning

| ID | Behavior | Verdict | Reason | Owner |
|---|---|---|---|---|
| A-PLAN-01 | One standing monthly limit per expense category. | Adapt | Kept as the model, but limits are effective-dated so history does not change. | ADR-0010 |
| A-PLAN-02 | A parent budget includes its children; a child budget counts only itself. | Adopt | Matches the category roll-up and is easy to explain. | ADR-0010 |
| A-PLAN-03 | Recurring payments require a manual post. | Reject | The owner chose automatic catch-up generation with a summary dialog. | ADR-0009 |
| A-PLAN-04 | Goals are a contribution log; current amount is derived. | Defer | Goals are Phase 3. The log shape is a strong candidate when they arrive. | `roadmap.md` |
| A-PLAN-05 | No rollover of unused budget money. | Adopt | The owner chose no rollover. | ADR-0010 |

### Capital and analytics

| ID | Behavior | Verdict | Reason | Owner |
|---|---|---|---|---|
| A-CAP-01 | Separate asset entity with its own valuation history. | Adapt | D-025 keeps one account model; the valuation-history idea is kept. | `technical/database.md` |
| A-CAP-02 | Asset value is the latest dated valuation. | Adopt | Simple and auditable. | `technical/ledger.md` |
| A-CAP-03 | Daily net-worth series is forward-filled from sparse events. | Adopt | Avoids gaps in the chart. | `technical/ledger.md` |
| A-CAP-04 | Capital roles: income / neutral / drain. | Defer | D-016 uses works / idle / lost; roles are a different axis. | Q-029 |
| A-CAP-05 | Risk tiers: low / medium / high. | Defer | Useful later, not part of the first-release capital dashboard. | Q-029 |
| A-CAP-06 | Idle cash: balance above a threshold with no activity for N days. | Adapt | Reused as one input to the "idle" metric, with Talex thresholds. | Q-029 |
| A-CAP-07 | Credit cards and liabilities do not reduce net worth. | Reject | Conflicts with D-025 and makes capital dishonest. | `technical/database.md` |
| A-AN-01 | Dashboard: income, spending, net, savings rate, top categories plus "Other". | Adopt | Clear first screen; fits D-028. | `product/plan.md` |
| A-AN-02 | Savings rate is net divided by real income, undefined when income is zero. | Adopt | Avoids a misleading zero. | `product/plan.md` |
| A-AN-03 | Month-by-month cash-flow view, transfers excluded, empty months filled. | Adopt | Fits D-028 and the capital narrative. | `product/plan.md` |
| A-AN-04 | Category detail and ranking reports. | Defer | Beyond the first-release analytics limit (D-028). | `roadmap.md` |
| A-AN-05 | Proactive alerts and advice. | Defer | Requires history and a product decision on notifications. | `roadmap.md` |
| A-AN-06 | ROI calculator. | Defer | Owner chose later. | `roadmap.md` |

### Crypto

| ID | Behavior | Verdict | Reason | Owner |
|---|---|---|---|---|
| A-CR-01 | Crypto portfolios, trades and live prices. | Defer | Owner chose later; no schema work now. | `roadmap.md` |
| A-CR-02 | Weighted-average cost and unrealized P&L. | Defer | Relevant only if crypto returns. | `roadmap.md` |
| A-CR-03 | External price provider dependency. | Defer | Conflicts with offline-first defaults if introduced too early. | `roadmap.md` |

### Import, backup and security

| ID | Behavior | Verdict | Reason | Owner |
|---|---|---|---|---|
| A-IMP-01 | Guided CSV import with mapping and preview. | Adapt | Phase 4; Android file access and streaming replace the browser flow. | `technical/database.md` |
| A-IMP-02 | Bank-specific CSV profiles. | Defer | Useful, but only after the generic importer exists. | `roadmap.md` |
| A-IMP-03 | Duplicate detection by date, type, amount and description. | Adapt | Talex should use a stable import hash or external id, not text matching alone. | `technical/database.md` |
| A-DATA-01 | Open JSON backup of every table. | Reject | D-032 requires an encrypted database snapshot; CSV is the plaintext export. | `technical/storage-and-sync.md` |
| A-DATA-02 | Backup format version and reference validation before restore. | Adopt | Prevents partial or incompatible restores. | `technical/storage-and-sync.md` |
| A-SEC-01 | No built-in authentication, optional HTTP Basic Auth, unencrypted database. | Reject | Conflicts with D-003, D-020 and D-021. | `technical/security.md` |

### UX

| ID | Behavior | Verdict | Reason | Owner |
|---|---|---|---|---|
| A-UX-01 | Month and year selectors on history screens. | Adopt | Fast navigation through months. | feature specs |
| A-UX-02 | Range pills for 30 days / 90 days / 1 year / 5 years / all. | Adopt | Compact and predictable. | feature specs |
| A-UX-03 | Hide-balance toggle for money amounts. | Adopt | Useful in public places; percentages can stay visible. | feature specs |
| A-UX-04 | Split editor with a remaining-amount indicator. | Adopt | Prevents the most common split error. | ADR-0008 |
| A-UX-05 | Import preview with skipped rows and duplicate count. | Adopt | Builds trust before writing data. | feature specs |
| A-UX-06 | Search across all periods, with a jump back to the month. | Adopt | Finds old purchases without knowing the month. | feature specs |
| A-UX-07 | Fourteen-item sidebar as the primary mobile navigation. | Reject | Too dense for a phone; Talex groups navigation. | feature specs |

## Decisions recorded from REF01

| ADR | Decision |
|---|---|
| ADR-0008 | Split transactions across one category tree. |
| ADR-0009 | Catch-up generation of recurring transactions on unlock. |
| ADR-0010 | Monthly budget with effective-dated limits, no rollover. |
| ADR-0011 | One-level category hierarchy with archival. |

## Explicitly not taken

- Separate account and asset models, and the absence of liabilities.
- A single transfer row instead of the D-024 pair.
- Positive-only amounts with type-driven direction.
- Currency as a display label with no conversion.
- Open JSON backups and an unencrypted database.
- HTTP Basic Auth as the access model.
- Credit cards excluded from net worth.
- Hard delete and `SET NULL` category references.
- Server, scheduler and external price dependency in the core.
- Capital roles and risk tiers in the first-release dashboard.
- Crypto, ROI calculator, alerts and advice in the current scope.
- The full fourteen-screen navigation on mobile.

## Open items

| ID | Question | Owner |
|---|---|---|
| Q-029 | What are the exact formulas for the works / idle / lost capital metrics? | `product/plan.md` |
| Q-030 | Do savings goals link to an account? | `roadmap.md` |
| Q-031 | How are liabilities and repayments represented? | `roadmap.md` |

## Related

- [Product plan](plan.md)
- [Database](../technical/database.md)
- [Ledger](../technical/ledger.md)
- [Decision log](../decisions/README.md)
- [Open questions](../open-questions.md)
