package io.github.flassuu.talex.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema version 5 adds capital roles and risk tiers on accounts (ADR-0035) and
 * the refund link on entries (Q-042). See ADR-0021 for the migration policy;
 * version 5 is additive.
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
        ValuationEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
    ],
    version = 5,
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
    abstract fun valuationDao(): ValuationDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
}
