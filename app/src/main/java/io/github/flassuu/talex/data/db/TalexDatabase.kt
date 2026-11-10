package io.github.flassuu.talex.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema version 1 for the Phase 1 vertical slice. Splits, transfer groups and
 * recurring rules arrive in Phase 2 as additive migrations.
 */
@Database(
    entities = [
        AccountEntity::class,
        EntryEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        EntryTagEntity::class,
        SettingEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class TalexDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun entryDao(): EntryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun settingsDao(): SettingsDao
}
