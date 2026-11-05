# Ledger

Read this when you are modeling accounts, entries, transfers, splits, balances or
the write path that changes them. This document defines behavior, not DDL. The
physical schema, indexes and migrations belong to
[database.md](database.md) and are not finalized.

**Status:** provisional behavior specification. It records the direction set by
D-024, D-025, D-010, ADR-0008, ADR-0009, ADR-0010, ADR-0011, ADR-0012 and
ADR-0013. Open items are listed at the end and tracked in
[../open-questions.md](../open-questions.md).

## Scope

In scope:

- entry-based accounts and their balances;
- income, expense, transfer and opening entries;
- split entries;
- categories and tags on entries;
- validation, editing, deletion and listing;
- the effect on reports, budgets and net worth.

Out of scope:

- valuation-based accounts such as property and vehicles;
- liabilities with repayment schedules;
- budgets, recurring rules and analytics, which have their own documents;
- import and backup formats;
- synchronization and conflict resolution.

## Money and sign convention

- Every amount is an integer in minor units, never floating point (D-010).
- The currency is stored with the entry and must match the account's currency.
- Amounts are signed:
  - income: positive;
  - expense: negative;
  - transfer out: negative;
  - transfer in: positive;
  - opening balance: positive for an asset, negative for a debt.
- An account balance is the sum of its entry amounts. No separate balance column
  exists.
- Reports display absolute values and derive direction from the entry type, but
  aggregation itself uses the signed integers.
- Zero amounts are invalid.

The sign convention differs from REF01, which stores positive amounts and derives
direction from the type. Talex uses signed minor units so a same-currency transfer
pair sums to zero and a balance is a single sum; a cross-currency pair carries
both amounts and a rate.

## Accounts

An account is a place where money is held. The first version uses one account
model with types (D-025). This document covers **entry-based** accounts:

| Type | Meaning | Typical sign |
|---|---|---|
| checking | bank current account | positive |
| savings | savings account or deposit | positive |
| cash | physical wallet | positive |
| credit_card | revolving credit | zero or negative |
| investment | brokerage cash balance | positive |
| loan | borrowed money | negative |
| other | an entry-based account that does not fit | either |

Valuation-based accounts (property, vehicles, other assets) share the account
identity but their balance comes from the latest valuation, not from entries.
They are outside this document.

Rules:

- An account has one currency. Changing it after entries exist is not allowed.
- An account can be archived. Archived accounts keep their history and balance
  but cannot receive new entries.
- Deleting an account is a destructive operation. The product should prefer
  archiving; the exact deletion flow is open.
- A negative balance is valid. It represents a debt or a credit-card balance, and
  it reduces net worth. This is a deliberate difference from REF01, which
  excludes credit cards from capital.

## Entries

An entry is one movement on one account. Transfers are two linked entries.

| Field | Required | Meaning |
|---|---|---|
| identifier | yes | UUIDv7 (D-033) |
| account | yes | the account the entry belongs to |
| type | yes | income, expense, transfer, opening |
| amount | yes | signed integer minor units, non-zero |
| currency | yes | must equal the account currency |
| date | yes | calendar date, no time zone |
| description | yes | short user-facing text |
| payee | no | merchant or counterparty |
| notes | no | free text |
| category | conditional | optional for a simple entry; required for each split line; null for transfers |
| tags | no | zero or more tags |
| transfer group | conditional | required for transfer entries, null otherwise |
| split lines | conditional | two or more for a split entry, none otherwise |
| source rule | no | set when generated from a recurring rule |
| schedule period | no | the rule period this entry was generated for; unique with the source rule |
| created / updated / deleted | yes | audit and soft-deletion fields |

The user-facing date is a calendar day. It must not depend on the device time
zone at read time. Audit timestamps may carry time and zone.

## Validation matrix

| Rule | Applies to |
|---|---|
| Amount is non-zero | all entries |
| Income amount is positive | income |
| Expense amount is negative | expense |
| Transfer entries have opposite signs and belong to different accounts | transfer |
| Transfer has no category and no split lines | transfer |
| Transfer pair has exactly two entries, same date and same group | transfer |
| Same-currency transfer has equal magnitudes | transfer |
| Different-currency transfer stores both amounts and a positive rate | transfer |
| Category kind matches the entry type | income, expense |
| Category is not archived | new and edited entries |
| Opening amount may be positive or negative | opening |
| Opening entry has no category, tags, payee or split lines | opening |
| One opening entry per account | opening |
| Opening entry is excluded from income/expense totals | opening |
| Simple entry has at most one category | income, expense |
| Split entry has no category and at least two lines | split |
| Split lines are non-zero, signed and sum exactly to the entry amount | split |
| All split lines share one top-level category | split |
| Split line category kind matches the entry type | split |
| Split is not allowed on a transfer | transfer |
| Archived accounts accept no new entries | all |
| Soft-deleted entries are not editable or countable | all |
| Tags exist and are not deleted | all |

Business-rule failures are reported as a validation outcome, not as a crash. The
UI shows a message near the offending field when possible; otherwise it shows a
general error.

## Transfers

A transfer moves money between two accounts. It is not income or expense and is
excluded from income/expense reports, budgets and the works/idle/lost metrics.

### Same currency

One entry leaves the source account and one enters the destination account:

```text
source:      -50 000
destination: +50 000
transfer group: G
```

Rules:

- exactly two entries;
- same currency and equal magnitudes;
- different accounts;
- same date;
- no category, no split lines;
- both entries share the transfer group;
- both are written atomically.

### Different currencies

Each entry is in its own account's currency. The group stores both amounts, the
rate, the rate date and the rate source:

```text
source:      -10 000 EUR
destination: +11 000 USD
rate:         1.10 USD per EUR
transfer group: G
```

Rules:

- the user enters the source amount;
- the app proposes the destination amount from the local rate table for the
  transfer date (D-030);
- the user may edit the destination amount and the rate; both are stored;
- both amounts are authoritative for balances; the stored rate is for display,
  audit and reporting;
- the destination amount is rounded half-up to the destination currency's minor
  unit; the difference is part of the exchange, not an error;
- later rate-table changes do not rewrite the transfer;
- the pair is still exactly two entries.

### Editing and deleting

- Editing the source or destination amount recomputes the rate; editing the rate
  recomputes the destination amount. The UI shows which value was last edited.
- Editing one side of a transfer updates the group atomically.
- Deleting a transfer soft-deletes both entries together. A half-deleted transfer
  is never a valid state.
- A transfer whose destination account was archived remains readable; it is not
  rewritten.

## Splits

A split divides one income or expense across two or more categories of one
top-level category.

```text
entry:  -10 000  (expense, 100.00)
line 1:  -7 000  Groceries
line 2:  -3 000  Sweets (child of Groceries)
```

Rules:

- the parent entry has no category;
- two or more lines;
- every line has a category and a non-zero signed amount;
- lines sum exactly to the parent amount;
- all line categories share one top-level parent;
- lines may carry a note; tags live on the parent only;
- a transfer cannot be split;
- replacing the line set is atomic;
- clearing the line set turns the entry back into a simple entry, which may then
  take one category.

Aggregation:

- a line contributes to its own category;
- the parent total includes direct entries on the parent and all child lines;
- a transaction counts once per top-level category it touches;
- an entry split within one parent counts once for that parent.

## Categories and tags on entries

- A simple income or expense may have one category.
- The category kind must match the entry type.
- A category may be top-level or a direct child; one level only (ADR-0011).
- Archived categories cannot be chosen for new entries; existing entries keep
  their category and remain in historical reports.
- Tags are optional, many-to-many, and orthogonal to categories.
- Removing a tag from an entry does not delete the tag itself.

## Opening balance

An account is created with an optional starting balance. A non-zero amount is
stored as a system entry of type `opening` (ADR-0012). A zero amount creates no
entry, because zero entries are invalid.

```text
account: checking
type: opening
amount: +50 000
date: account opening date
category: none
```

Rules:

- created from the "starting balance" field at account creation;
- one opening entry per account;
- no category, tags, payee or split lines;
- included in the account balance;
- excluded from income, expense, budgets and savings-rate calculations;
- included in net-worth history from its date;
- editable while the account exists; deleting it is part of account deletion.

This keeps "balance is a sum of entries" true and leaves an audit trail for the
starting amount.

## Balance

```text
balance(account) = sum(amount of all non-deleted entries on the account)
```

Consequences:

- no stored balance to drift;
- deleting or editing an entry changes the balance immediately;
- an opening entry sets the starting point;
- a credit-card purchase makes the balance negative;
- a payment to a credit card is a transfer from a depository account to the card:
  the depository balance falls and the card balance rises toward zero;
- a transfer between two depository accounts does not change total cash.

For multi-currency accounts, each balance is in its own currency. Net worth and
reports convert to the base currency using the rate for the relevant date (D-030).

## Editing and deletion

### Editing

- Simple entries can change account, type, amount, date, description, payee,
  notes, category and tags.
- A type change revalidates every rule against the resulting state, not only the
  fields that were sent.
- A transfer edit updates the pair as one operation.
- A split edit replaces the line set as one operation.
- Generated entries can be edited like any other entry; editing them does not
  change the recurring rule.

### Deletion

- Deletion is soft: the entry gets a deletion timestamp and is excluded from
  balances, reports and budgets.
- The row is kept as a tombstone for future synchronization (D-023).
- Deleting a transfer soft-deletes both entries.
- Deleting a split soft-deletes its lines.
- Deleting a recurring rule does not delete entries it already generated.
- Restoring a soft-deleted entry is a future action unless the UI offers undo.

## Listing, filters and search

The list is ordered by date descending, then by identifier descending for a
stable order.

Supported filters:

- date range, month or year;
- account;
- category, including entries whose category appears only in a split line;
- tag;
- type;
- text search over description, payee and notes, case-insensitive;
- amount sorting ascending or descending.

Rules:

- a category filter finds a split entry when any line matches;
- a text search spans all periods; the UI must not silently restrict it to the
  selected month;
- pagination is stable; keyset pagination is preferred on large data sets;
- the total count uses the same filters as the page.

## Atomicity and idempotency

The following writes are atomic:

- creating, editing or deleting a transfer pair;
- replacing a split line set;
- creating an account together with its opening entry;
- a batch of recurring entries generated in one catch-up run;
- an import batch.

Idempotency:

- recurring generation is keyed by rule and schedule period (ADR-0009);
- imported entries carry an import hash or external identifier, so a repeated
  import does not duplicate them;
- retrying a failed write must not create a second transfer side or a second
  split line.

## Worked examples

### Salary and groceries

```text
+300 000  income   checking   Salary
-100 000  expense  checking   Groceries
balance(checking) = 200 000
```

### Split receipt

```text
-10 000  expense  checking  no category
   -7 000  Groceries
   -3 000  Sweets
```

Groceries total = 10 000; one transaction; two leaf lines.

### Transfer to savings

```text
checking: -50 000  transfer
savings:  +50 000  transfer
```

Total cash is unchanged. Both entries share one transfer group.

### Credit card

```text
credit card purchase: -20 000  expense
card payment from checking:
  checking: -20 000  transfer
  card:     +20 000  transfer
card balance: 0
```

Before the payment the card balance is negative and reduces net worth.

### Opening balance

```text
+50 000  opening  checking
```

The account starts at 50 000 without pretending it was income.

### Currency transfer

```text
EUR account: -10 000 EUR
USD account: +11 000 USD
rate: 1.10
```

Each balance stays in its own currency; net worth converts both to the base
currency.

## Interaction with other features

| Feature | Uses the ledger as |
|---|---|
| Reports | income/expense entries and split shares, transfers excluded |
| Budgets | expense entries plus child and split shares, converted to base (ADR-0010) |
| Capital dashboard | account balances for cash, latest valuations for assets |
| Recurring rules | templates that produce ordinary entries dated on the scheduled due date (ADR-0009) |
| Import | a batch of ordinary entries with idempotency keys |
| Backup | entries, split lines, transfer groups and tombstones |

## Open questions

| ID | Question | Why it matters |
|---|---|---|
| Q-029 | Exact formulas for the works / idle / lost capital metrics. | The ledger supplies balances; the dashboard defines meaning. |

## Related

- [Database](database.md)
- [Architecture](architecture.md)
- [Product plan](../product/plan.md)
- [REF01 disposition](../product/reference-ref01.md)
- [Decision log](../decisions/README.md)
- [Open questions](../open-questions.md)
