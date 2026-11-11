package io.github.flassuu.talex.data.db

import androidx.room.migration.Migration

/**
 * The single ordered list of schema migrations (ADR-0021).
 *
 * Every schema bump adds an entry here and registers it in [io.github.flassuu.talex.data.security.DatabaseFactory].
 * Auto-migrations declared on [TalexDatabase] additionally require their
 * `@AutoMigrationSpec` to be passed to `Room.databaseBuilder`. Never add a
 * destructive fallback: a failing migration must abort the open, not wipe data.
 */
object TalexMigrations {

    val ALL: Array<Migration> = emptyArray()
}
