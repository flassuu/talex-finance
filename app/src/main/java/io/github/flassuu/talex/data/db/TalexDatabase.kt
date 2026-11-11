package io.github.flassuu.talex.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema version 3 adds split lines and recurring rules. See ADR-0021 for the
 * migration policy; debug builds may recreate the database during Phase 2.
 */
@Database(
    entities = [
        AccountEntity::class,
        EntryEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        EntryTagEntity::class,
        SettingEntity::class,
        TransferGroupEntity::class,
        RateEntity::class,
        EntryLineEntity::class,
        RecurringRuleEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class TalexDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun entryDao(): EntryDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun settingsDao(): SettingsDao
    abstract fun transferDao(): TransferDao
    abstract fun rateDao(): RateDao
    abstract fun entryLineDao(): EntryLineDao
    abstract fun recurringDao(): RecurringDao
}
