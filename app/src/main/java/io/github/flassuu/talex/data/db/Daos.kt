package io.github.flassuu.talex.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: AccountEntity)

    @Query("UPDATE accounts SET archived = 1, updated_at = :updatedAt WHERE id = :id")
    suspend fun archive(id: String, updatedAt: Long)
}

@Dao
interface EntryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: EntryEntity)

    @Query(
        """
        SELECT * FROM entries
        WHERE account_id = :accountId AND deleted_at IS NULL
        ORDER BY date DESC, id DESC
        """,
    )
    fun observeForAccount(accountId: String): Flow<List<EntryEntity>>

    @Query("SELECT COALESCE(SUM(amount_minor), 0) FROM entries WHERE account_id = :accountId AND deleted_at IS NULL")
    suspend fun sumForAccount(accountId: String): Long
}

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CategoryEntity)

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
