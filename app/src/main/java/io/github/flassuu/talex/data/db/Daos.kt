package io.github.flassuu.talex.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query(
        """
        SELECT a.*, COALESCE(
            (SELECT SUM(e.amount_minor) FROM entries e
             WHERE e.account_id = a.id AND e.deleted_at IS NULL), 0
        ) AS balance_minor
        FROM accounts a
        ORDER BY a.name COLLATE NOCASE ASC
        """,
    )
    fun observeAllWithBalance(): Flow<List<AccountWithBalance>>

    @Query(
        """
        SELECT a.*, COALESCE(
            (SELECT SUM(e.amount_minor) FROM entries e
             WHERE e.account_id = a.id AND e.deleted_at IS NULL), 0
        ) AS balance_minor
        FROM accounts a
        WHERE a.id = :id
        LIMIT 1
        """,
    )
    fun observeByIdWithBalance(id: String): Flow<AccountWithBalance?>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AccountEntity)

    @Query("UPDATE accounts SET name = :name, type = :type, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateNameAndType(id: String, name: String, type: String, updatedAt: Long)

    @Query("UPDATE accounts SET archived = :archived, updated_at = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM entries WHERE account_id = :id")
    suspend fun countEntries(id: String): Int

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface EntryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: EntryEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<EntryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: EntryEntity): Long

    @Query("SELECT COUNT(*) FROM entries WHERE source_rule_id = :ruleId AND schedule_period = :period")
    suspend fun countForRulePeriod(ruleId: String, period: String): Int

    @Query(
        """
        SELECT * FROM entries
        WHERE account_id = :accountId AND deleted_at IS NULL
        ORDER BY date DESC, id DESC
        """,
    )
    fun observeForAccount(accountId: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE deleted_at IS NULL ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<EntryEntity>>

    @Transaction
    @Query("SELECT * FROM entries WHERE deleted_at IS NULL ORDER BY date DESC, id DESC")
    fun observeAllWithLines(): Flow<List<EntryWithLines>>

    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EntryEntity?

    @Update
    suspend fun update(entity: EntryEntity)

    @Query("UPDATE entries SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM entry_tags WHERE entry_id IN (SELECT id FROM entries WHERE account_id = :accountId)")
    suspend fun deleteTagsForAccount(accountId: String)

    @Query("DELETE FROM entry_lines WHERE entry_id IN (SELECT id FROM entries WHERE account_id = :accountId)")
    suspend fun deleteLinesForAccount(accountId: String)

    @Query("DELETE FROM entries WHERE account_id = :accountId")
    suspend fun deleteForAccount(accountId: String)

    @Query("SELECT COALESCE(SUM(amount_minor), 0) FROM entries WHERE account_id = :accountId AND deleted_at IS NULL")
    suspend fun sumForAccount(accountId: String): Long
}

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE stable_key = :stableKey LIMIT 1")
    suspend fun findByStableKey(stableKey: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CategoryEntity)

    // Renaming a seeded category clears its stable key so the custom name is shown.
    @Query("UPDATE categories SET name = :name, stable_key = NULL, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateName(id: String, name: String, updatedAt: Long)

    @Query("UPDATE categories SET archived = :archived, updated_at = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM categories WHERE parent_id = :parentId")
    suspend fun countChildren(parentId: String): Int

    @Query("SELECT COUNT(*) FROM entries WHERE category_id = :categoryId")
    suspend fun countEntries(categoryId: String): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name_normalized = :normalized LIMIT 1")
    suspend fun findByNormalizedName(normalized: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryTag(entity: EntryTagEntity)

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN entry_tags et ON et.tag_id = t.id
        WHERE et.entry_id = :entryId
        ORDER BY t.name COLLATE NOCASE ASC
        """,
    )
    fun observeForEntry(entryId: String): Flow<List<TagEntity>>

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN entry_tags et ON et.tag_id = t.id
        WHERE et.entry_id = :entryId
        ORDER BY t.name COLLATE NOCASE ASC
        """,
    )
    suspend fun getForEntry(entryId: String): List<TagEntity>

    @Query("DELETE FROM entry_tags WHERE entry_id = :entryId")
    suspend fun deleteTagsForEntry(entryId: String)
}

@Dao
interface TransferDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGroup(entity: TransferGroupEntity)

    @Update
    suspend fun updateGroup(entity: TransferGroupEntity)

    @Query("SELECT * FROM transfer_groups WHERE id = :id LIMIT 1")
    suspend fun getGroup(id: String): TransferGroupEntity?

    @Query("SELECT * FROM transfer_groups ORDER BY date DESC, created_at DESC")
    fun observeGroups(): Flow<List<TransferGroupEntity>>

    @Query(
        "SELECT * FROM transfer_groups WHERE source_account_id = :accountId OR destination_account_id = :accountId",
    )
    suspend fun getGroupsForAccount(accountId: String): List<TransferGroupEntity>

    @Query("DELETE FROM transfer_groups WHERE id = :id")
    suspend fun deleteGroup(id: String)
}

@Dao
interface RateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RateEntity)

    @Query(
        "SELECT * FROM rates WHERE base_currency = :base AND quote_currency = :quote AND date = :date LIMIT 1",
    )
    suspend fun find(base: String, quote: String, date: String): RateEntity?

    @Query("SELECT * FROM rates ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<RateEntity>>

    @Query(
        "SELECT * FROM rates WHERE base_currency = :base AND quote_currency = :quote ORDER BY date DESC LIMIT 1",
    )
    suspend fun latest(base: String, quote: String): RateEntity?
}

@Dao
interface EntryLineDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<EntryLineEntity>)

    @Query("DELETE FROM entry_lines WHERE entry_id = :entryId")
    suspend fun deleteForEntry(entryId: String)

    @Query("SELECT * FROM entry_lines WHERE entry_id = :entryId ORDER BY position ASC")
    suspend fun getForEntry(entryId: String): List<EntryLineEntity>

    @Query("SELECT * FROM entry_lines WHERE entry_id = :entryId ORDER BY position ASC")
    fun observeForEntry(entryId: String): Flow<List<EntryLineEntity>>
}

@Dao
interface RecurringDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RecurringRuleEntity)

    @Update
    suspend fun update(entity: RecurringRuleEntity)

    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringRuleEntity?

    @Query("SELECT * FROM recurring_rules ORDER BY active DESC, next_due_date ASC")
    fun observeAll(): Flow<List<RecurringRuleEntity>>

    @Query("SELECT * FROM recurring_rules")
    suspend fun getAll(): List<RecurringRuleEntity>

    @Query(
        "UPDATE recurring_rules SET active = :active, next_due_date = :nextDueDate, updated_at = :updatedAt WHERE id = :id",
    )
    suspend fun setActive(id: String, active: Boolean, nextDueDate: String, updatedAt: Long)

    @Query("UPDATE recurring_rules SET next_due_date = :nextDueDate, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateNextDueDate(id: String, nextDueDate: String, updatedAt: Long)
}

@Dao
interface SettingsDao {

    @Query("SELECT value FROM settings WHERE key = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM settings WHERE key = :key LIMIT 1")
    fun observe(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: SettingEntity)
}
