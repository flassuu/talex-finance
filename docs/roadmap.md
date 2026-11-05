# Roadmap

Read this when you are planning the next phase, sequencing work, or checking
what "done" means for a milestone.

**Status:** directional. The sequence is tentative, has no dates and is not a
commitment. Resolve blocking questions before starting a phase. Decisions live in
the [decision log](decisions/README.md); unresolved choices live in
[open-questions.md](open-questions.md).

## How to read this roadmap

- A **phase** is a shippable milestone, not a folder or a sprint.
- Each phase lists its goal, workstreams, decisions, blockers, exit criteria and
  what is explicitly out of scope.
- A workstream is done only when its observable criteria are met and its rules
  have tests.
- A decision that changes behavior must be recorded as an ADR before code; do not
  silently diverge from an accepted decision.
- Features marked **Later** are not designed or estimated yet.

## Decision map

| Area | Decision | Phase |
|---|---|---|
| Money | Integer minor units, signed; direction is explicit (D-010) | 1–2 |
| Module strategy | One `:app` module with package boundaries (ADR-0014) | 1 |
| Identifiers | UUIDv7 (D-033, ADR-0006) | 1 |
| Encryption | Portable encrypted database, master passphrase, Argon2id, biometric convenience (D-020, D-021, D-034; ADR-0004, ADR-0007) | 1 |
| Accounts | One account model with types and valuation entries (D-025); opening balance as a system entry (ADR-0012) | 2 |
| Categories | One level of subcategories; archive instead of nulling history (ADR-0011) | 2 |
| Splits | One purchase across one category tree, exact sum (ADR-0008) | 2 |
| Transfers | Explicit pair (D-024); cross-currency uses both amounts and a stored rate (ADR-0013) | 2 |
| Recurring | Catch-up generation on unlock, summary dialog, idempotent (D-017, ADR-0009) | 2 |
| Capital overview | Works / idle / lost, derived from accounts and categories (D-016); exact formulas Q-029 | 2 |
| Budgets | Monthly, effective-dated limits, no rollover (ADR-0010) | 3 |
| Goals | Contribution log; account link open (Q-030) | 3 |
| Liabilities | One account model; repayment shape open (Q-031) | 3 |
| Analytics | Income/expenses, category breakdown, net-worth trend (D-028) | 4 |
| Portability | Encrypted snapshot backup, CSV import/export, migration (D-032) | 4 |
| Crypto | Portfolios, trades, live prices | Later |
| ROI calculator | Standalone payback and compounding tool | Later |
| Alerts and advice | Proactive signals and plain-language notes | Later |
| Sync and profiles | File-level sync and multiple profiles (D-023, D-027) | Later |

## Phase 0 — Documentation foundation

**Status:** complete (2026-11-05). The documentation set, decision log and
question register exist. Q-017 is answered by ADR-0014.

**Work items**

- Repository structure and documentation set.
- Preliminary architecture, data and security direction.
- Decision log, ADR process and open-question register.
- REF01 disposition and the ledger behavior specification.
- Decisions for splits, recurring generation, budgets, categories, opening
  balance and cross-currency transfers.

**Exit criteria**

- The documentation is reviewed.
- Q-017 is answered: start with one `:app` module (ADR-0014).
- Every Phase 1 decision has an accepted ADR.

## Phase 1 — Technical foundation

**Goal:** prove the risky parts before building product features.

| # | Work item | Done when |
|---|---|---|
| P1-01 | Gradle project and baseline configuration | `:app` builds; application ID, minSdk 26 and compile/target are applied (ADR-0001, ADR-0002). |
| P1-02 | Package boundaries | `core`, `data`, `domain`, `feature`, `ui` packages exist; the domain package has no Android imports. |
| P1-03 | Encrypted database | Room over SQLCipher opens with the master passphrase; the key is derived with Argon2id (ADR-0004, ADR-0007). |
| P1-04 | Unlock and lock | Passphrase and biometric unlock work; the app re-locks on the defined policy; no biometric hardware falls back to the passphrase. |
| P1-05 | Portable file | The same database file opens on a clean install or a second device with the passphrase. |
| P1-06 | Vertical slice | Create an account with a starting balance, list it, close the app and see it again after unlock. |
| P1-07 | Commands | Real, verified build, test and lint commands are recorded in `AGENTS.md`. |
| P1-08 | Tests | At least one unit test per domain rule and one instrumentation test for opening the encrypted database. |

**Decisions:** D-001–D-006, D-020–D-021, D-033–D-034; ADR-0001–0004, ADR-0006,
ADR-0007, ADR-0014.

**Blockers:** none.

**Exit criteria:** the app opens an encrypted database, survives restart,
re-locks and unlocks by passphrase and biometric, the same file opens on a second
device, and the key scheme is documented and reviewed.

**Not in scope:** any transaction feature, import, budgets, assets, crypto.

## Phase 2 — Core ledger and capital overview

**Goal:** a complete, honest history of money in and out, across accounts and
currencies, with a first capital view.

**First demo:** encrypted database, accounts with opening balance, income, expense
and transfer entries, categories and tags, and the overview with a capital
summary. Splits, multi-currency, recurring and the full capital view complete the
phase.

### 2.1 Accounts

- Account types: checking, savings, cash, credit card, investment, loan, other.
- One currency per account; changing it after entries exist is not allowed.
- Starting balance at creation, stored as a system `opening` entry (ADR-0012).
- Archive instead of delete; archived accounts keep history and balance.
- Negative balances are valid and represent debt.

**Done when:** a card created with 500 € shows 500 €; the opening entry is not
counted as income; archiving hides the account but keeps its history; a credit
card with a debt reduces total capital.

### 2.2 Entries

- Signed integer minor units; income positive, expense negative.
- Entry types: income, expense, transfer, opening.
- Date is a calendar day, independent of the device time zone at read time.
- Description, payee, notes, category and tags.
- Soft deletion with tombstones; audit fields.
- Validation matrix from `technical/ledger.md`.
- Editing revalidates the resulting state, not only the changed fields.

**Done when:** the ledger rules have unit tests; a deleted entry disappears from
balances and reports but remains in storage; a type change that would violate a
rule is rejected.

### 2.3 Categories and tags

- Two kinds: income and expense; kind is fixed at creation.
- One level of subcategories (ADR-0011).
- Default categories seeded on first run; stable keys for translation.
- Archive referenced categories; hard delete only unused ones.
- Child spending rolls up into the parent.
- Tags are many-to-many, case-insensitive de-duplicated and orthogonal to
  categories.

**Done when:** a parent total includes direct and child entries; an archived
category disappears from pickers but stays in old reports; a duplicate tag is not
created.

### 2.4 Splits

- One income or expense entry divided into two or more lines.
- One top-level parent; parent itself or direct children only.
- Exact sum in minor units.
- No splits on transfers.
- Atomic replacement of the line set (ADR-0008).
- Split-aware category filters, reports and budgets.

**Done when:** a 100 € receipt split 70/30 shows as one transaction, one parent
total with a child breakdown, and both leaf categories see their share.

### 2.5 Transfers

- Two linked entries, written atomically (D-024).
- Same currency: equal magnitudes, rate 1.
- Different currencies: both amounts and a stored rate from the local table
  (ADR-0013); the user can edit the destination and the rate.
- No category, no splits; excluded from income, expenses, budgets and the capital
  metrics.
- Credit-card payment is a transfer from a depository account to the card.

**Done when:** a 100 € → 110 $ transfer leaves −100 € and +110 $, the rate is
stored, later rate-table changes do not rewrite it, and total cash is unchanged
for a same-currency transfer.

### 2.6 Recurring transactions

- Rule template: account, type, amount, description, category or transfer
  destination, frequency, anchor date, active flag.
- Frequencies: weekly, monthly, yearly; month-end clamping and 29 February.
- Catch-up generation on unlock, before the main screen (ADR-0009).
- Generated entry is dated on the scheduled due date.
- All missed occurrences are generated; a summary dialog reports them.
- Idempotent by rule and schedule period.
- Paused rules generate nothing; resuming does not backfill paused periods.
- Archived accounts or categories skip generation and are reported.
- Recurring transfers create both entries atomically.
- No splits or tags on recurring rules in the first version.

**Done when:** opening the app after three missed months creates three correct
entries with a summary; opening again creates none; a paused rule creates none; a
month-end rule clamps correctly.

### 2.7 Capital overview (cash side)

- Works / idle / lost, derived from account types and categories (D-016).
- Exact formulas must be written and accepted before implementation (Q-029).
- Cash side only at this phase; assets and liabilities arrive in Phase 3.
- Net-worth trend for the cash side, with a daily series that fills gaps forward.
- Transfers do not change total cash.

**Done when:** Q-029 is closed; the overview shows the three numbers with a
drill-down; changing an account type or category changes the metrics as defined;
the trend matches the underlying balances.

### 2.8 Quality gates

- Unit tests for balances, transfers, splits, category roll-up and opening
  balance.
- Property-style tests for "balance is the sum of entries" and "split sum equals
  parent".
- Migration test from an empty database to the Phase 2 schema.
- Large-data check on at least 10,000 entries without floating-point money.
- No financial values in logs.

**Exit criteria:** a user can record and review a complete multi-currency history
offline, with correct balances, splits, transfers and recurring entries, and see
the first works/idle/lost view.

**Blockers:** Q-029.

**Not in scope:** budgets, goals, assets, liabilities, import UI, crypto, ROI,
alerts and advice.

## Phase 3 — Planning and balance sheet

**Goal:** turn the ledger into a planning tool and make net worth complete.

### 3.1 Budgets

- Monthly limit per expense category, effective-dated (ADR-0010).
- No rollover; unused money expires.
- Parent budget includes children; child budget counts only itself.
- Split-aware.
- Spending converted to the base currency using the transaction-date rate.
- Missing rate makes the status incomplete, not silently wrong.
- Over-budget is strictly greater than the limit; percent is not clamped.

**Done when:** changing a limit does not rewrite past months; a parent and child
budget can coexist; a split contributes to both; a missing rate is visible.

### 3.2 Goals

- Target amount, optional date, contribution log.
- Current amount is the sum of contributions.
- Negative contribution is a withdrawal; zero is invalid.
- Account link is open (Q-030): independent progress or reserved money.

**Done when:** Q-030 is closed; the chosen model has tests; deleting a goal
removes its contributions; a goal never changes the ledger by accident.

### 3.3 Assets and liabilities

- One account model with types and valuation entries (D-025).
- Valuation-based accounts use the latest dated valuation.
- Credit cards and loans are entry-based and can have negative balances.
- Repayment, interest and fee representation is open (Q-031).
- Net worth includes assets and subtracts liabilities.

**Done when:** Q-031 is closed; a mortgage or car loan reduces net worth; a
property valuation changes the trend from its date; a credit-card payment is a
transfer, not an expense.

### 3.4 Full capital overview

- Extend works / idle / lost to assets and liabilities.
- Keep the three metrics stable; do not add REF01-style roles or risk tiers in
  this phase.
- The net-worth trend includes cash, assets and liabilities.

**Done when:** the overview reconciles with account balances plus latest
valuations; the three metrics have written definitions and tests.

**Blockers:** Q-030, Q-031.

## Phase 4 — Insight and portability

**Goal:** make the data understandable outside the app and safe to move.

### 4.1 Basic analytics

- Income and expenses over time.
- Category breakdown with subcategory roll-up.
- Net-worth trend on the analytics screen, reusing the series built in Phase 2
  and extended in Phase 3.
- No ranking reports, advice or alerts in this phase.

**Done when:** each chart reconciles with the ledger and with the capital
overview; transfers are excluded; empty periods render as zero, not as gaps.

### 4.2 CSV import and export

- Generic column mapping, preview, skipped-row report and duplicate detection.
- Atomic import in one database transaction.
- Stable import hash or external identifier, not text matching alone.
- Export of transactions and categories.
- Bank-specific profiles are a later addition.

**Done when:** a malformed row rejects the whole batch; a repeated import does not
duplicate entries; skipped rows have human-readable reasons.

### 4.3 Backup, restore and migration

- Encrypted database snapshot (D-032).
- Restore replaces all data atomically; version check before writing.
- CSV as the plaintext export, with a warning.
- Migration between app versions and between devices.
- Validation and recovery from a bad import.

**Done when:** a backup restores on a clean install; a corrupt file changes
nothing; a schema migration preserves every entry, split line and transfer pair.

## Phase 5 — Release hardening

- Security review against `technical/security.md`.
- Accessibility: touch targets, contrast, TalkBack, font scaling.
- Performance and large-data testing.
- Release signing and store readiness.
- GPL-3.0 compliance and license notices.
- Documentation review: commands, ADRs and open questions are current.

**Exit criteria:** the release checklist is satisfied, the license obligations
are met, and a fresh install passes the full smoke path.

## Later

These are deliberately outside the first release:

| Item | Why later | Related |
|---|---|---|
| File-level synchronization | Conflict handling is undefined; the data model keeps UUIDs and tombstones ready. | D-023, Q-016 |
| Separate profiles | One user first; profiles affect storage and UI. | D-027 |
| Crypto portfolios and prices | External provider dependency; not core to the first release. | — |
| ROI calculator | Useful, but independent of the ledger and capital model. | — |
| Alerts and advice | Need several months of history and a notification decision. | — |
| Advanced reports and ranking | Beyond D-028. | — |
| Attachments and receipts | Not in the data model yet. | — |

**Explicitly out of the first release:** bank integrations or aggregators (D-019),
a mandatory server, cloud accounts, telemetry, and web or desktop clients.

## Critical path

1. **Phase 0 is complete.** Phase 1 is next.
2. **Phase 1 proves** encryption, portability and the vertical slice.
3. **Phase 2 builds** the ledger and the cash-side capital view; Q-029 must be
   closed before the overview work starts.
4. **Phase 3 adds** budgets, goals, assets and liabilities; Q-030 and Q-031 must
   be closed before their workstreams.
5. **Phase 4 makes** the data portable and understandable.
6. **Phase 5 hardens** security, accessibility, performance and release.

Do not start a phase before its blockers are closed. If a decision changes after
code exists, write a superseding ADR and a migration plan; do not rewrite an
accepted ADR.

## Related

- [Product overview](product/overview.md)
- [Product plan](product/plan.md)
- [REF01 disposition](product/reference-ref01.md)
- [Open questions](open-questions.md)
- [Decision log](decisions/README.md)
- [Security](technical/security.md)
- [Storage, portability and sync](technical/storage-and-sync.md)
- [Ledger](technical/ledger.md)
- [Database](technical/database.md)
