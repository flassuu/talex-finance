# Changelog

All notable changes to Talex are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versioning follows
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Edit, archive and hard-delete empty accounts, and open an account detail screen
  with its transactions.
- Seeded income and expense categories with one level of subcategories; create,
  rename, archive and delete categories.
- Record income and expenses with amount, account, category, date, description,
  payee, notes and tags.
- Browse, edit and soft-delete transactions; the center button opens a quick
  expense form.
- Unit tests for entry validation and category naming.
- Database migration policy (ADR-0021): versioned, tested, forward-only and never
  destructive.
- Record transfers between accounts, including cross-currency transfers with both
  amounts and a stored rate; the pair is written, edited and deleted atomically.
- Keep a local exchange-rate table for future rate suggestions.
- Split an income or expense across several categories of one top-level category.
- Recurring rules with weekly, monthly and yearly schedules; due entries are
  generated on unlock, dates stay on the schedule, and pausing does not backfill.
- Capital overview with works, idle and lost, income and expenses for the month,
  a savings rate and a 30-day cash trend, plus a top-categories roll-up.
- Sort and filter transactions; a category that is in use links to its filtered
  transaction list.
- A transfer rate field with a prefilled rate from the local rate table.
- Adjust an account balance; the difference is stored as an adjustment entry.
- The center add button offers expense, income and transfer.

### Changed

- Descriptions are optional.
- A split total is computed from its lines instead of being typed.
- Accounts can be deleted with confirmation; linked transfer sides are removed.
- Older databases are migrated to the current schema instead of recreated;
  migrations from 0.1.0 are covered by instrumentation tests.

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
