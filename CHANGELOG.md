# Changelog

All notable changes to Talex are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versioning follows
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Fixed

- Asset accounts no longer appear in the account pickers for transactions, transfers and recurring rules, so they cannot be selected where they are not supported.

## [0.4.0] - 2026-11-12

### Added

- Analytics screen with income and expenses over time, cash flow, spending by
  category with subcategory roll-up and the net-worth trend, all in the base
  currency.
- A shared period selector (month, year, custom range) used by analytics and
  budgets; the budget screen now navigates across months.
- Export transactions and categories to CSV, with header detection, a preview and
  an import report that lists skipped rows with a reason.
- Import transactions from CSV atomically: splits expand one row per line,
  transfers export as a linked pair, and duplicate imports are skipped.
- Export one encrypted `.talex` backup file holding the database and its KDF
  sidecar, and restore it by entering the backup passphrase.

### Changed

- Budget editing is limited to the current month; past months stay read-only so
  history is not rewritten.

## [0.3.0] - 2026-11-12

### Added

- Set a monthly limit for an expense category and see spent, remaining, the
  percentage and an over-budget flag; changing a limit does not rewrite past
  months and a missing exchange rate is shown as incomplete.
- Create savings goals with a target amount, an optional date and a contribution
  log; negative contributions are withdrawals.
- Add valuation-based asset accounts (property, vehicle, other asset) with manual
  dated valuations; the balance is the latest valuation.
- See net worth, and see assets joined to works while liabilities reduce net worth
  only.
- Mark an expense category as unnecessary so its spending counts as lost money
  next to subscriptions.
- Account detail shows valuation history for asset accounts.

### Changed

- The capital trend now shows net worth, including assets and liabilities, instead
  of cash only.
- The lost metric now combines subscriptions and spending in unnecessary
  categories.
- The works metric includes valuation-based assets.

### Fixed

- Switching an account between entry-based and valuation-based types is rejected,
  so entries and valuations cannot be orphaned.

## [0.2.0] - 2026-11-11

### Added

- Edit, archive and delete accounts, and open an account detail screen with its
  transactions.
- Seed, create, rename and archive income and expense categories with one level
  of subcategories.
- Record income and expenses with an account, category, date, description, payee,
  notes and tags.
- Record transfers between accounts, including cross-currency transfers with an
  editable rate and a local rate table.
- Split an income or expense across several categories of one top-level category.
- Create recurring rules; due entries are generated on unlock, dated on the
  schedule, and pausing does not backfill.
- See works, idle and lost money, monthly income and expenses, a savings rate, a
  30-day cash trend and top spending categories.
- Sort and filter transactions; a category that is in use links to its filtered
  transaction list.
- Adjust an account balance; the difference is stored as an adjustment entry.
- The center add button offers expense, income and transfer.

### Changed

- Descriptions are optional.
- A split total is computed from its lines instead of being typed.
- Accounts can be deleted with confirmation; linked transfer sides are removed.
- Databases from 0.1.0 are migrated automatically.

### Fixed

- The Transactions tab opens the list instead of the last transaction form.
- Validation errors show the correct message instead of "Something went wrong".
- The archived-accounts toggle only appears when there is an archived account.

## [0.1.0] - 2026-11-10

### Added

- Android app skeleton with Material 3, dynamic color and a bottom bar with a
  center quick-add and a trailing menu.
- First-run wizard for the master passphrase and base currency, plus passphrase
  and biometric unlock.
- Encrypted local database with Room over SQLCipher; the key is derived from the
  master passphrase with Argon2id.
- Account list and account creation with an opening balance, stored as a system
  entry.
- Automatic re-lock after one minute in the background and on every cold start.
- Screenshots and the recents preview are blocked, and the app requests no
  network permission.
- English base and Ukrainian translation for the first-run and account screens.
- Documentation foundation: product plan, architecture, database, security,
  ledger, roadmap, decision log and ADRs 0001-0020.

### Fixed

- Biometric unlock saves the wrapped database key on the first passphrase
  unlock, so the biometric button appears on later launches.
