package io.github.flassuu.talex.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The single ordered list of schema migrations (ADR-0021).
 *
 * Every schema bump adds an entry here and registers it in
 * [io.github.flassuu.talex.data.security.DatabaseFactory]. Never add a destructive
 * fallback: a failing migration must abort the open, not wipe data.
 */
object TalexMigrations {

    /** v1 -> v2: transfer groups, the local rate table and the entry link column. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `transfer_groups` (" +
                    "`id` TEXT NOT NULL, `source_entry_id` TEXT NOT NULL, " +
                    "`destination_entry_id` TEXT NOT NULL, `source_account_id` TEXT NOT NULL, " +
                    "`destination_account_id` TEXT NOT NULL, `source_amount_minor` INTEGER NOT NULL, " +
                    "`destination_amount_minor` INTEGER NOT NULL, `source_currency` TEXT NOT NULL, " +
                    "`destination_currency` TEXT NOT NULL, `rate` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                    "`rate_date` TEXT NOT NULL, `rate_source` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                    "`created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `rates` (" +
                    "`base_currency` TEXT NOT NULL, `quote_currency` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                    "`rate` TEXT NOT NULL, `source` TEXT NOT NULL, `updated_at` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`base_currency`, `quote_currency`, `date`))",
            )
            db.execSQL("ALTER TABLE `entries` ADD COLUMN `transfer_group_id` TEXT")
        }
    }

    /** v2 -> v3: split lines, recurring rules and the idempotency key on entries. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `entry_lines` (" +
                    "`id` TEXT NOT NULL, `entry_id` TEXT NOT NULL, `category_id` TEXT NOT NULL, " +
                    "`amount_minor` INTEGER NOT NULL, `note` TEXT, `position` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`), FOREIGN KEY(`entry_id`) REFERENCES `entries`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_entry_lines_entry_id` ON `entry_lines` (`entry_id`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_entry_lines_category_id` ON `entry_lines` (`category_id`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `recurring_rules` (" +
                    "`id` TEXT NOT NULL, `account_id` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                    "`amount_minor` INTEGER NOT NULL, `currency` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                    "`category_id` TEXT, `frequency` TEXT NOT NULL, `anchor_date` TEXT NOT NULL, " +
                    "`next_due_date` TEXT NOT NULL, `active` INTEGER NOT NULL, " +
                    "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL("ALTER TABLE `entries` ADD COLUMN `source_rule_id` TEXT")
            db.execSQL("ALTER TABLE `entries` ADD COLUMN `schedule_period` TEXT")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_entries_source_rule_id_schedule_period` " +
                    "ON `entries` (`source_rule_id`, `schedule_period`)",
            )
        }
    }

    /**
     * v3 -> v4: budgets, goals, valuations and the unnecessary category flag. The
     * migration is additive; no existing row is rewritten.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `categories` ADD COLUMN `unnecessary` INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `valuations` (" +
                    "`id` TEXT NOT NULL, `account_id` TEXT NOT NULL, `amount_minor` INTEGER NOT NULL, " +
                    "`currency` TEXT NOT NULL, `date` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`), FOREIGN KEY(`account_id`) REFERENCES `accounts`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_valuations_account_id` ON `valuations` (`account_id`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `budget_limits` (" +
                    "`id` TEXT NOT NULL, `category_id` TEXT NOT NULL, `amount_minor` INTEGER NOT NULL, " +
                    "`currency` TEXT NOT NULL, `effective_from` TEXT NOT NULL, `closed_at` TEXT, " +
                    "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_budget_limits_category_id` ON `budget_limits` (`category_id`)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `goals` (" +
                    "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `target_minor` INTEGER NOT NULL, " +
                    "`currency` TEXT NOT NULL, `target_date` TEXT, " +
                    "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `goal_contributions` (" +
                    "`id` TEXT NOT NULL, `goal_id` TEXT NOT NULL, `amount_minor` INTEGER NOT NULL, " +
                    "`currency` TEXT NOT NULL, `date` TEXT NOT NULL, `note` TEXT, " +
                    "`created_at` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`goal_id`) REFERENCES `goals`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_goal_contributions_goal_id` " +
                    "ON `goal_contributions` (`goal_id`)",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
