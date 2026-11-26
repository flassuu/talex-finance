# Changelog

All notable changes to Talex are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versioning follows
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- A Refund type in the transaction form, with a picker for the original expense, the amount and the destination account; the one-tap refund from an expense stays.

### Changed

- The center + opens the expense form directly; the type selector at the top switches between expense, income, split, refund and transfer.
- A transfer is now entered and saved in the same form, not on a separate screen.
- Search on the transactions screen moved to a top-bar button; the duplicate transfer button is gone.

### Fixed

- The transaction form now has a save action in the top bar, so saving no longer requires scrolling to the bottom.
- The amount field offers +, −, ×, ÷, = and C keys for the inline calculator.

## [0.6.1] - 2026-11-26

### Added

- Edit any transaction from the list and save it in place with full revalidation.
- Create a refund from an expense; the refund returns the money and reduces the original spending.
- Open category reports from the More menu: a ranking by expense or income and a per-month drill-down.
- See a subscriptions overview with the active count and monthly expense and income totals.
- Settings is grouped into General, Finance and About.

### Changed

- The More menu now includes category reports.

## [0.6.0] - 2026-11-26

### Added

- Assign each asset a capital role (income, neutral or drain) and a risk tier (low, medium or high); the overview adds a capital-by-role breakdown, a capital-by-risk breakdown and the medium-and-high share.
- Get local alerts for a negative cash-flow streak, a falling net worth, an over-budget month, risky allocation and idle cash.
- Read short advice about a rising category, an unbudgeted top category and a change in the savings rate.
- Adjust every insight threshold and switch each signal off in the Insights settings.
- Search transactions across the whole history by description, payee or notes.
- Type a small expression such as 20,10-12= in the amount field and get the result before saving.
- Fetch rates from a fallback provider, so UAH and other currencies missing from ECB data work.
- Choose the number of decimal places for rates.
- Turn sync on or off for each currency; a disabled pair stays manual-only.
- See a one-time note about where rates come from and what data is sent.
- Fetch a single pair's rate from the add-rate form with a button next to the currency.
- Schedule rate refresh on launch, weekly, monthly or a custom number of days.

### Changed

- The demo profile is marked in Settings only; the permanent banner left the main screens.
- Schema v5 adds the capital role, risk and reference cash flow to accounts, and a refund link to entries.

## [0.5.0] - 2026-11-24

### Added

- Start with a demo profile from the first-run screen: example accounts, entries,
  budgets, a goal, a property and a loan are created without a passphrase, so the
  app is browsable immediately.
- Convert the demo into a real profile by setting a master passphrase, or delete
  its data from Settings; the demo is marked with a banner and kept across
  restarts.
- The onboarding wizard now covers the master passphrase, base currency, the
  first account and the default categories.
- Change the master passphrase in Settings: the database is re-encrypted with the
  new key and the app locks so the new passphrase is proven.
- Choose when the app re-locks: immediately, 30 seconds, 1, 5 or 15 minutes.
- Turn biometric unlock on or off; disabling it clears the wrapped key.
- Hide every amount on screen from Settings or with the eye button on the
  overview.
- Choose the app language in Settings: follow the system, English or Ukrainian.
- Manage exchange rates on a dedicated screen: see the pairs in use, add or
  override a dated rate, review history and delete a point.
- Update rates explicitly from frankfurter.app with one tap; the app makes no
  network call on its own.
- Change the base currency from Settings with a preview; open budget limits are
  converted using stored rates and closed history stays unchanged.
- Pick any ISO 4217 currency in the currency selectors, with the six common ones
  first and a search field.
- Choose the three screens on the bottom bar; the center add button and More stay
  fixed, and the choice survives a restart.
- Map CSV columns interactively when a file uses different headers, then import
  it as usual.
- Manage tags on a dedicated screen: rename a tag everywhere or delete it
  without touching the transactions.

### Changed

- The re-lock timeout is configurable instead of fixed at one minute.

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
