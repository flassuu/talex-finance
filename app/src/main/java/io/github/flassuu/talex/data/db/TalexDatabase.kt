package io.github.flassuu.talex.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema version 2 adds transfer groups and the local rate table. Splits and
 * recurring rules follow. See ADR-0021 for the migration policy.
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
    ],
    version = 2,
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
}
