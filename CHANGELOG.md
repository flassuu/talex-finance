# Changelog

All notable changes to Talex are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versioning follows
[Semantic Versioning](https://semver.org/).

## [Unreleased]

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
