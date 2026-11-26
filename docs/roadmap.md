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
| Money | Integer minor units, signed; direction is explicit (D-010) | 1-2 |
| Module strategy | One `:app` module with package boundaries (ADR-0014) | 1 |
| Design | Material 3 + dynamic color, configurable bottom bar, onboarding wizard (D-039–D-043) | 1 |
| Identifiers | UUIDv7 (D-033, ADR-0006) | 1 |
| Encryption | Portable encrypted database, master passphrase, Argon2id, biometric convenience (D-020, D-021, D-034; ADR-0004, ADR-0007) | 1 |
| Accounts | One account model with types and valuation entries (D-025); opening balance as a system entry (ADR-0012) | 2 |
| Categories | One level of subcategories; archive instead of nulling history (ADR-0011) | 2 |
| Splits | One purchase across one category tree, exact sum (ADR-0008) | 2 |
| Transfers | Explicit pair (D-024); cross-currency uses both amounts and a stored rate (ADR-0013) | 2 |
| Recurring | Catch-up generation on unlock, summary dialog, idempotent (D-017, ADR-0009) | 2 |
| Capital overview | Works / idle / lost, simple model (D-016, ADR-0016); assets in works, liabilities in net worth (ADR-0025) | 2-3 |
| Budgets | Monthly, effective-dated limits, no rollover (ADR-0010) | 3 |
| Goals | Contribution log, independent of accounts; one currency per goal (ADR-0017, ADR-0026) | 3 |
| Liabilities | Negative-balance accounts; repayment transfer, interest expense (ADR-0018) | 3 |
| Assets | Valuation-based account types and manual dated valuations (ADR-0023) | 3 |
| Lost metric | Subscriptions plus spending in categories flagged unnecessary (ADR-0016, ADR-0024) | 2-3 |
| Analytics | Income/expenses, category breakdown, cash flow, net-worth trend (D-028); base-currency reporting, hand-drawn Compose Canvas | 4 |
| Capital roles and risk | Asset roles (income/neutral/drain) and risk tiers (low/medium/high) with the 80/20 rule, as a second axis on the capital dashboard (ADR-0035) | 6 |
| Insights | Local alerts and advice, computed on read, configurable thresholds, no background or network (ADR-0036) | 6 |
| Search | Full-history transaction search with a jump back to the month | 6 |
| Subscriptions view | Recurring overview: total monthly cost, next due, status | 6 |
| Category reports | Drill-down of one category over time and a ranking across categories | 6 |
| Quick add | Inline calculator in the amount field | 6 |
| CSV format | Transactions and categories; one row per split line, linked transfer pairs, stable id or content-hash dedup (ADR-0027) | 4 |
| Backup | Single-file encrypted snapshot container (ADR-0028) | 4 |
| Portability | Encrypted snapshot backup, CSV import/export, migration (D-032) | 4 |
| Crypto | Portfolios, trades, live prices | Later |
| ROI calculator | Standalone payback and compounding tool | Later |
| Profiles | Multiple profiles on one database (D-027) | Later |

## Phase 0 – Documentation foundation

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

## Phase 1 – Technical foundation

**Goal:** prove the risky parts before building product features.

**Status:** implemented (2026-11-10). All work items have code; unit tests, lint
and instrumentation tests (SQLCipher open, wrong key, Argon2id, opening entry)
pass on a physical device. P1-05 is verified on a clean install; a second physical
device was not available for the final check.

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
| P1-09 | Design system and navigation shell | Material 3 theme with dynamic color, the bottom bar with a center + and a trailing …, and the onboarding wizard skeleton (D-039–D-043). |

**Decisions:** D-001–D-006, D-020–D-021, D-033–D-043; ADR-0001–0004, ADR-0006,
ADR-0007, ADR-0014, ADR-0015.

**Blockers:** none.

**Exit criteria:** the app opens an encrypted database, survives restart,
re-locks and unlocks by passphrase and biometric, the same file opens on a second
device, and the key scheme is documented and reviewed.

**Not in scope:** any transaction feature, import, budgets, assets, crypto.

## Phase 2 – Core ledger and capital overview

**Goal:** a complete, honest history of money in and out, across accounts and
currencies, with a first capital view.

**First demo:** encrypted database, accounts with opening balance, income, expense
and transfer entries, categories and tags, and the overview with a capital
summary. Splits, multi-currency, recurring and the full capital view complete the
phase.

**Status:** implemented for the cash side (2026-11-11). Accounts (2.1), categories
and tags (2.3), income/expense entries (2.2), transfers (2.5), splits (2.4),
recurring generation (2.6) and the cash-side capital overview (2.7) are in place.
Section 2.8 is met: unit and property tests, migration tests `v1 -> v3`, a
10,000-entry check, no financial values in logs, and a top-categories roll-up with
children and split shares. Descriptions are optional, accounts can be deleted with
confirmation, balance adjustments are system entries, and transactions can be
sorted and filtered by category. Known follow-ups: the "unnecessary" category flag
for the lost metric is deferred; the overview has no per-category drill-down
screen. Assets, liabilities and budgets remain Phase 3.

### 2.1 Accounts

- Account types: checking, savings, cash, credit card, investment, loan, other.
- One currency per account; changing it after entries exist is not allowed.
- Starting balance at creation, stored as a system `opening` entry (ADR-0012).
- Archive is the safe default; deletion is allowed with explicit confirmation and
  removes the account, its entries and both sides of linked transfers (ADR-0022).
- A balance adjustment writes a system `adjustment` entry (ADR-0022).
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

- Works / idle / lost, simple model: works = investment accounts, idle = cash
  accounts, lost = subscriptions + unnecessary spending (ADR-0016).
- Cash side only at this phase; assets and liabilities arrive in Phase 3.
- Savings rate on the overview.
- Net-worth trend for the cash side, with a daily series that fills gaps forward.
- Transfers do not change total cash.

**Done when:** the overview shows the three numbers and the savings rate with a
drill-down; changing an account type or category changes the metrics as defined;
the trend matches the underlying balances.

### 2.8 Quality gates

**Status:** complete (2026-11-11). Migration tests, property tests and the
10,000-entry check run on a physical device; `lintDebug` is clean.

- Unit tests for balances, transfers, splits, category roll-up and opening
  balance.
- Property-style tests for "balance is the sum of entries" and "split sum equals
  parent".
- Migration tests for every transition and one `v1 -> latest` path, following
  [ADR-0021](decisions/0021-migration-policy.md).
- Large-data check on at least 10,000 entries without floating-point money.
- No financial values in logs.

**Exit criteria:** a user can record and review a complete multi-currency history
offline, with correct balances, splits, transfers and recurring entries, and see
the first works/idle/lost view.

**Blockers:** none.

**Not in scope:** budgets, goals, assets, liabilities, import UI, crypto, ROI.

## Phase 3 – Planning and balance sheet

**Goal:** turn the ledger into a planning tool and make net worth complete.

**Status:** implemented (2026-11-12). Budgets, goals, valuation-based assets,
liabilities and the full capital overview are in place. Schema v4 adds the tables
and covers the migration; unit and repository tests cover the new rules. The
budget screen gained month navigation in 4.1. Known follow-up: cross-currency
budget spending is marked incomplete until a rate exists.

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
- Goals are independent of accounts (ADR-0017); they never change the ledger.

**Done when:** the model has tests; deleting a goal removes its contributions;
a goal never changes the ledger by accident.

### 3.3 Assets and liabilities

- One account model with types and valuation entries (D-025).
- Valuation-based accounts use the latest dated valuation.
- Liabilities are negative-balance accounts (ADR-0018): repayment is a transfer,
  interest and fees are expenses.
- Asset value dynamics are shown over time.
- Net worth includes assets and subtracts liabilities.

**Done when:** a mortgage or car loan reduces net worth; a property valuation
changes the trend from its date; a credit-card payment is a transfer, not an
expense.

### 3.4 Full capital overview

- Extend works / idle / lost to assets and liabilities.
- Keep the three metrics stable; do not add REF01-style roles or risk tiers in
  this phase.
- The net-worth trend includes cash, assets and liabilities.

**Done when:** the overview reconciles with account balances plus latest
valuations; the three metrics have written definitions and tests.

**Blockers:** none.

## Phase 4 – Insight and portability

**Goal:** make the data understandable outside the app and safe to move.

**Status:** implemented (2026-11-12). All workstreams have code and unit tests.
CSV import/export uses the entry `id` column for idempotency (ADR-0027); the backup
is a single `.talex` container (ADR-0028). Known follow-ups: CSV columns are mapped
by header name only (no interactive remap), and the optional `external_id` column is
carried for interoperability while dedup relies on the entry id or a content hash.

### 4.1 Basic analytics

- Replaces the `ANALYTICS` placeholder screen (reached from the "More" sheet).
- Charts are drawn with Compose Canvas; no chart library is added.
  - Income and expenses over time, grouped by month.
  - Category breakdown with subcategory roll-up: a parent total includes direct
    entries, child categories and split shares.
  - Cash flow by month (income minus expenses).
  - Net-worth trend, reusing the daily series built in Phase 2 and extended in
    Phase 3.
- All charts report in the base currency; foreign amounts convert at the
  transaction-date rate, as budgets do. A missing rate marks the affected period
  incomplete instead of silently wrong.
- A shared period selector (month / year / custom range) drives every chart. The
  same selector is reused on the budget screen, which gains month navigation and
  closes the Phase 3 follow-up (current month only).
- Transfers, opening and adjustment entries are excluded; empty periods render as
  zero, not as gaps.

**Done when:** each chart reconciles with the ledger and the capital overview;
transfers are excluded; empty periods render as zero; the budget screen navigates
across months with the shared selector.

### 4.2 CSV import and export

- Export covers transactions (income, expense, transfer) and categories; budgets,
  goals, assets and accounts are out of scope for the first version.
- Splits expand to one row per split line; transfers export as a linked pair with
  a shared group identifier. Opening and adjustment system entries are not
  exported.
- Format is UTF-8, RFC 4180, ISO 8601 dates and signed decimal amounts in major
  units with an ISO 4217 currency column (ADR-0027).
- Import offers generic column mapping, a preview, a skipped-row report with a
  human-readable reason, and duplicate detection.
- Duplicate detection uses a stable import hash or an optional external
  identifier; text matching alone is not enough.
- Import is atomic: one database transaction, and a malformed row rejects the
  whole batch.
- Bank-specific profiles are a later addition.

**Done when:** a malformed row rejects the whole batch; a repeated import does
not duplicate entries; skipped rows carry reasons; a split round-trips to its
lines and a transfer round-trips to its pair.

### 4.3 Backup, restore and migration

- Backup is an encrypted snapshot packaged as a single file that holds the
  database and its KDF sidecar (ADR-0028).
- Export writes the snapshot to a user-chosen location through the Storage Access
  Framework; import reads it back and validates the format version before writing
  anything.
- Restore replaces all data atomically; a version mismatch or a corrupt file
  changes nothing.
- CSV is the plaintext export and carries a clear warning (D-032).
- Migration between app versions (Room) is separate from moving between devices
  via export/import (D-035).
- Restore is tested on a clean install and on another device.

**Done when:** a backup restores on a clean install; a corrupt file changes
nothing; a schema migration preserves every entry, split line and transfer pair.

### 4.4 Quality gates and release

- Instrumentation tests run on a physical device.
- New tests: CSV round-trip, duplicate detection, backup/restore round-trip,
  corrupt-file handling, and a migration that preserves splits and transfers.
- `lintDebug` and the debug/release builds stay green.
- Release `v0.4.0` (versionCode 4) with a changelog and a signed APK.

**Blockers:** none after ADR-0027 and ADR-0028.

**Not in scope:** bank-specific CSV profiles, forecast, crypto, ROI, profiles.

## Phase 5 – Product completion

**Goal:** close the gap between accepted decisions and the shipped app, so the
release does not promise what it does not deliver. This phase adds security
settings, completes onboarding, turns the stored rates into a usable feature and
makes the shell configurable.

**Status:** complete (2026-11-24). 5.1 security and privacy settings, 5.2
onboarding with the demo profile, 5.3 multi-currency management, 5.4
personalization and 5.5 data polish are implemented; ADR-0029 to ADR-0034 are
accepted.

### 5.1 Security and privacy settings

**Status:** implemented and verified (2026-11-24). Passphrase change, lock
timeout, the biometric switch and amount masking are in place; unit tests and
all instrumentation tests pass on a physical device, including the rekey export.

- Change the master passphrase: re-key the database atomically and rewrite the
  KDF sidecar; rotate or clear the Keystore-wrapped biometric key.
- Configurable re-lock timeout (immediately, 1, 5, 15 minutes) applied when the
  app leaves the foreground.
- Biometric unlock can be turned on and off in Settings; disabling it clears the
  stored key and falls back to the passphrase.
- A hide-amounts toggle masks balances, charts and reports until it is turned
  off again.

**Done when:** the database opens with a changed passphrase on a clean install;
the shortest timeout locks the app after backgrounding; disabling biometrics
leaves the passphrase as the only path; hidden amounts leave no financial value
visible on any screen.

### 5.2 Onboarding

**Status:** implemented and verified (2026-11-24). The wizard covers passphrase,
base currency, first account and categories; the demo profile is seeded by a
use case, opens without a passphrase and converts or deletes through the rekey
path. The demo lifecycle is covered by an instrumentation test.

- First run offers a choice: create a profile or open the demo profile (D-042).
- The create path is the full wizard: passphrase, base currency, accounts and
  categories.
- The demo profile seeds example data, is visibly marked and can be deleted.

**Done when:** the demo profile is browsable immediately after install, deleting
it leaves an empty database, and the wizard creates the first account and the
starter categories.

### 5.3 Multi-currency management

**Status:** implemented (2026-11-24); extended with scheduled and per-currency
refresh (2026-11-26). The rates screen lists the pairs in use, adds or overrides
a dated rate, shows history and deletes a point; the base currency can be changed
with a budget-conversion preview. ADR-0032, ADR-0033 and ADR-0037 are accepted.
Unit tests cover date-aware conversion, the budget conversion plan, the opt-in
refresh, the fallback merge and the rate precision; `lintDebug` and the debug
build are green. The instrumentation suite was not re-run because no device was
attached.

- A rate screen lists stored pairs, adds or overrides a rate and records the
  date it applies to.
- Refresh is user-controlled: an explicit action, or an opt-in schedule
  (on launch, weekly, monthly or a custom interval) evaluated on unlock with no
  background service. The explicit path, the opt-in fetch and the ECB plus
  open.er-api fallback are available (ADR-0032, ADR-0037).
- Each pair can be switched to manual-only, and one global precision controls
  the stored decimals (ADR-0037).
- The base currency can be changed with a preview of the effect on reports; open
  budget limits are converted atomically (ADR-0033).

**Done when:** budgets and analytics convert using the stored rates; changing
the base currency rescales every report consistently; a rate is fetched only
after a user action or an opted-in schedule.

### 5.4 Personalization

**Status:** implemented (2026-11-24). The bottom bar has three configurable
slots; any main destination, including Settings, can fill one, and the center
quick-add and More stay fixed. The choice lives in plaintext SharedPreferences
outside the encrypted database. ADR-0034 is accepted; unit tests cover the
preference sanitisation.

- Configure the three bottom bar slots, with Overview, Transactions and Accounts
  as defaults (D-039).
- The configuration is stored outside the encrypted database so the shell is
  available before unlock.

**Done when:** any supported screen can occupy a slot, the center quick-add and
the trailing menu stay fixed, and the choice survives a restart.

### 5.5 Data polish

**Status:** implemented (2026-11-24). The CSV import offers an interactive column
mapping when required headers are missing, and a tags screen renames or deletes
tags in place. Unit tests cover the mapping helpers and a custom-header round
trip; `lintDebug` and the debug/release builds are green.

- Interactive CSV column remapping before an import runs.
- A tags screen with rename and delete.

**Done when:** a file with unknown headers imports after a manual mapping, and
renaming a tag updates every entry that uses it.

**Blockers:** none.

**Exit criteria:** every rule in `technical/security.md`, D-039 and D-042 is
implemented or explicitly superseded, with tests for the new data paths.

**Not in scope:** accessibility and performance, store readiness and release
hardening (Phases 7–8), separate profiles, crypto, ROI and attachments.

## Phase 6 – Insights and capital depth

**Goal:** make Talex proactive, not just descriptive. Add the missing insight
layer (alerts and advice), open the capital roles and risk axis, close the
search gap, give subscriptions a first-class view, add category reports and
speed up entry with an inline calculator.

**Status:** planned. Depends on ADR-0035 and ADR-0036 (accepted in this change)
and the schema v5 migration.

### 6.1 Quality-of-life improvements

Small, high-value changes that make daily use faster and quieter. The Settings
structure in item 6 is owned by `product/ux.md`; visual details are a Phase 7
task.

1. **Edit transactions.** Every entry type can be opened, changed and saved in
   place with full revalidation. **Done when:** income, expense, transfer and
   split entries are all editable from the transaction list.
2. **Faster entry flow.** The center + opens the expense form directly instead
   of a type picker; a segmented control at the top switches between Expense,
   Income, Split, Transfer and Refund. **Done when:** + lands on the expense
   form and switching type keeps the entered amount.
3. **Refunds.** A refund reverses an earlier transaction (for example a 6 EUR
   Amazon expense refunded three days later): it links to the original, returns
   the amount to the account, stays in the history and is excluded from income,
   expense, budgets and reports. Whether it also nets the original out of
   statistics, and whether it is a new entry type, is an open decision (Q-042)
   that likely changes the schema. **Done when:** a refunded expense no longer
   inflates spending; the refund appears in history but not in income or
   category totals.
4. **Realistic demo profile.** Spread the seeded demo transactions across
   several weeks so the charts and trends are readable. **Done when:** the demo
   shows activity across multiple days, not one cluster.
5. **Quiet demo banner.** Remove the persistent demo-profile banner from the
   main screens; keep convert/delete in Settings only. **Done when:** the demo
   is marked only in Settings, not as a permanent banner.
6. **Restructured Settings.** Group the flat settings list into sections
   following `product/ux.md`; labels and order are finalized in Phase 7.
   **Done when:** Settings follows the agreed sections and every setting is
   reachable from its group.

### 6.2 Capital roles and risk

- Supersedes ADR-0016: works / idle / lost stays the primary metric; roles and
  risk become a second axis on valuation-based accounts (ADR-0035).
- Each valuation account gains a user-assigned role (income / neutral / drain)
  and a risk tier (low / medium / high), plus an optional reference monthly cash
  flow that does not affect balances or reports.
- New capital breakdowns on the overview: by role (three fixed buckets, always
  present) and by risk (three tiers; cash counts as low). Cash has no role.
- The 80/20 rule: the medium+high share drives the risky-allocation alert.
- Schema v5 adds the columns; the migration is additive and tested `v4 -> v5`.

**Done when:** assigning a role and risk to an asset shows it in the breakdown;
cash stays low-risk; the three role buckets render even when empty; the
works / idle / lost numbers are unchanged by roles and risk.

### 6.3 Alerts

- Five signals, computed on read with no background task and no network
  (ADR-0036): negative cash-flow streak, net-worth decline streak, budget
  exceeded, risky allocation, idle cash.
- Only completed months count for the streak signals; the budget signal uses the
  current month so the user can react in time.
- Thresholds are configurable in Settings; each signal can be switched off.
- A banner on the overview lists active alerts; the risky-allocation alert is
  muted there and shown on the capital screen (design detail for Phase 7).

**Done when:** two months of negative net raise the streak alert and one month
does not; an over-budget current month raises it; the thresholds come from
Settings; disabling a signal removes it.

### 6.4 Advice

- Three generators (ADR-0036): rising category, top unbudgeted category, and
  savings-rate trend.
- A dedicated advice list with positive / neutral / warning tones and an empty
  state when there is nothing to say.
- Advice respects splits and subcategory roll-up, unlike the reference.

**Done when:** a category up 30% on its 3-month average is flagged; a top
category without a budget is suggested; the savings-rate trend uses the last two
completed months.

### 6.5 Search across the full history

- Search transactions by description, payee and notes (case-insensitive).
- Results open the matching entry in its month, with a clear path back.
- Works offline over the local database; a normal index is enough.

**Done when:** a query from any period finds old transactions; tapping a result
jumps to the correct month; clearing the query restores the transaction list.

### 6.6 Subscriptions and recurring overview

- A dedicated screen lists recurring rules: amount, frequency, next due date and
  status (due, overdue, paused), grouped by income / expense / transfer.
- A monthly total of recurring expenses feeds the "lost" narrative on the
  overview.
- Reuses the existing recurring-rule model; no schema change.

**Done when:** the monthly total reconciles with the active rules; next-due
dates match the schedule rules; pausing a rule removes it from the total.

### 6.7 Category reports

- Drill-down: one category (with its children) over a chosen period, with total,
  per-month series and transaction count; empty months render as zero.
- Ranking: top categories by expense or income for a period, split-aware.
- Reuses the shared period selector from Phase 4.

**Done when:** a parent category total includes its children and split shares;
the ranking matches the category breakdown; transfers and system entries are
excluded.

### 6.8 Inline calculator in the amount field

- The amount fields (quick add and the entry form) accept a small expression
  such as `20,10-12` or `20+5*2` and reduce it to one amount.
- A small safe decimal parser handles `+ - * /` and the locale decimal
  separator; no code evaluation, no new dependency.
- The reduced amount is what is stored; the expression is not saved.

**Done when:** `20,10-12` yields 8,10 in the field; an invalid expression falls
back to plain number entry; the parser is a pure function with unit tests.

**Decisions:** ADR-0035 (capital roles and risk), ADR-0036 (local insights).

**Blockers:** none after the two ADRs.

**Exit criteria:** the overview shows the roles/risk breakdowns and the insight
surfaces; alerts and advice fire on the defined rules; search finds any entry;
the subscriptions screen and category reports reconcile with the ledger; the
amount field evaluates expressions.

**Not in scope:** crypto, ROI calculator, attachments, forecast, bank-specific
CSV profiles, German localization.

## Phase 7 – Design and accessibility

**Goal:** one coherent look, a usable app for everyone, and a written design
spec that keeps screens consistent.

**Status:** planned.

### 7.1 Design system and visual pass

- Consolidate color, typography, spacing and shape tokens; every screen uses
  them, not ad-hoc values.
- Consistent empty states with a call to action, and consistent cards, list rows
  and form layouts across all screens.
- Polish motion and spacing on the overview, entry form and onboarding.

**Done when:** a token change propagates everywhere; the main screens share the
same card, list and empty-state patterns; no screen mixes ad-hoc colors.

### 7.2 Accessibility

- Touch targets at least 48dp, contrast meets WCAG AA, TalkBack reads every
  screen in order, and the app respects font scaling without clipping.
- Semantic labels on charts and money amounts; the hide-amounts toggle also
  hides values from TalkBack where it makes sense.

**Done when:** the main flows pass a TalkBack run-through; font scaling to the
largest size does not clip key labels; contrast passes a check.

### 7.3 Design spec

- A design document owns the tokens, component patterns, states and navigation
  conventions, so future screens follow it.

**Done when:** the spec documents the tokens and patterns; new screens reference
it instead of re-deciding layout.

**Blockers:** none.

**Not in scope:** new features beyond the Phase 6 surface; store listing assets.

## Phase 8 – Release hardening

- Security review against `technical/security.md`.
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
| Separate profiles | One user first; profiles affect storage and UI. | D-027 |
| Crypto portfolios and prices | External provider dependency; not core to the first release. | – |
| ROI calculator | Useful, but independent of the ledger and capital model. | – |
| Attachments and receipts | Not in the data model yet. | – |

**Dropped from the plan:** forecast and planned expenses, and bank-specific CSV
profiles.

**Explicitly out of the first release:** bank integrations or aggregators (D-019),
device synchronization (D-035), a mandatory server, cloud accounts, telemetry,
and web or desktop clients.

## Critical path

1. **Phases 0-5 are complete.** Phase 6 is next.
2. **Phase 1 proves** encryption, portability, the design shell and the vertical
   slice.
3. **Phase 2 builds** the ledger and the cash-side capital view.
4. **Phase 3 adds** budgets, goals, assets and liabilities.
5. **Phase 4 makes** the data portable and understandable.
6. **Phase 5 completes** security settings, onboarding, multi-currency
   management, personalization and the remaining data polish.
7. **Phase 6 adds** the insight layer (alerts, advice), capital roles and risk,
   search, subscriptions, category reports and the inline calculator.
8. **Phase 7 refines** the design system and accessibility.
9. **Phase 8 hardens** security, performance and release.

Do not start a phase before its blockers are closed. If a decision changes after
code exists, write a superseding ADR and a migration plan; do not rewrite an
accepted ADR.
