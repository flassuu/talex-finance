# ADR 0021 – Database migration policy

- **Status:** Accepted
- **Date:** 2026-11-11
- **Deciders:** project owner
- **Related questions:** none (closes the migration gap left open by ADR-0020)

## Context

Schema version 1 ships with the Phase 1 vertical slice (ADR-0020). The database is
Room over SQLCipher, `exportSchema = true`, and the generated
`app/schemas/io.github.flassuu.talex.data.db.TalexDatabase/1.json` is committed.
No `Migration` object is registered yet, and `DatabaseFactory` does not call
`.addMigrations(...)` or `fallbackToDestructiveMigration()`.

That default is safe: bumping `version` without a migration makes the app fail to
open instead of wiping data. But it is not yet a policy. Phase 2 adds tables and
columns (transfers, splits, recurring rules), users already run 0.1.0 with real
data, and the encrypted file is portable across devices (ADR-0004). A wrong
migration could silently destroy financial history, and a file made by a newer
version can reach an older app. The owner asked to settle this before Phase 2
features start.

## Decision

1. **Versioning.** Every shipped schema change bumps `version` by exactly one and
   commits the new exported JSON. Versions are never reused or decremented. Each
   bump ships with a migration in the same change.
2. **Mechanism: hybrid.**
   - Purely additive, data-preserving changes (new table, nullable column, index)
     use Room `@AutoMigration`, verified against the committed schema JSON.
   - Changes that need a data backfill, seeding, or a `NOT NULL` column with a
     computed value use a hand-written `Migration` object with raw SQL.
   - All migrations are listed in one `TalexMigrations.ALL` array and registered
     with `.addMigrations(*TalexMigrations.ALL)` in `DatabaseFactory`.
3. **No destructive fallback.** Never call `fallbackToDestructiveMigration()`.
   If the file's schema is newer than the app supports (an older app opening a
   newer backup), opening fails and the unlock screen shows a specific message
   asking the user to update the app. Data is not deleted and no automatic
   downgrade is attempted.
4. **Forward-only.** A newer app migrates an older file on first open. An older
   app cannot open a newer file. This is part of the portability contract in
   ADR-0004 and is stated in `database.md`.
5. **Seeding is runtime, not migration.** Default categories are seeded by an
   idempotent routine guarded by a flag in `settings` (for example
   `categories.seeded.v1`), not inside a migration. A fresh install creates the
   latest schema directly and never runs an old migration, so the seed must live
   where both fresh installs and upgrades reach it. The seed is safe to re-run and
   never duplicates a default because it checks the flag and the stable key.
6. **Tests.** Add `androidx.room:room-testing`. Use `MigrationTestHelper` with
   SQLCipher's `SupportOpenHelperFactory` so the helper opens the encrypted test
   database with a fixed key. Test every version transition and one full
   `v1 -> latest` path: create the old schema, insert a representative row
   (an account with an opening entry), migrate, and assert the row survives and
   the new schema is usable. Schema JSONs are exposed to the instrumentation
   tests through `androidTest.assets.srcDir("$projectDir/schemas")`.
7. **Atomic transitions.** Migrations run in the SQLite transaction Room opens for
   the upgrade. A failing migration must abort the open and surface a recoverable
   error rather than leave a half-migrated file.

## Alternatives considered

| Alternative | Why it was not chosen |
|---|---|
| Only hand-written migrations | Full control, but more code and tests for every trivial additive change. |
| Only auto-migrations | Cannot express backfills or seeding; `AutoMigrationSpec` becomes the same hand-written SQL with less clarity. |
| `fallbackToDestructiveMigration` | Unacceptable for financial history; a silent wipe is the worst outcome. |
| Seed categories inside a migration | A fresh install never runs the old migration, so a second seeding path would be needed anyway. |
| No policy until Phase 4 | The first real migration lands in Phase 2, before the portability phase; deciding then is too late. |

## Consequences

- **Positive:** old databases keep working as features are added; the first real
  migration is covered by a test; a newer backup is refused instead of corrupted.
- **Negative:** each schema change carries a migration plus a test; a direct
  downgrade is impossible, which must be documented for users moving files.
- **Follow-up:** `database.md` gains a migration section; `roadmap.md` links the
  Phase 2 quality gate to this ADR; `TalexMigrations.ALL` is the single list to
  extend.

## Review

Revisit if a change cannot be expressed as an additive migration, if Room's
auto-migration cannot reproduce a required change, or if the app ever offers a
downgrade path.
