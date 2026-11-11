package io.github.flassuu.talex.data.repository

import androidx.room.withTransaction
import io.github.flassuu.talex.core.id.UuidV7
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.data.db.AccountEntity
import io.github.flassuu.talex.data.db.CategoryEntity
import io.github.flassuu.talex.data.db.DatabaseProvider
import io.github.flassuu.talex.data.db.EntryEntity
import io.github.flassuu.talex.data.db.EntryTagEntity
import io.github.flassuu.talex.data.db.SettingEntity
import io.github.flassuu.talex.data.db.TagEntity
import io.github.flassuu.talex.data.db.TalexDatabase
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryInput
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Tag
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private fun AccountEntity.toDomain(balanceMinor: Long) = Account(
    id = id,
    name = name,
    type = AccountType.valueOf(type),
    currency = CurrencyCode.of(currency),
    archived = archived,
    balance = Money(balanceMinor, CurrencyCode.of(currency)),
)

private fun CategoryEntity.toDomain() = Category(
    id = id,
    kind = CategoryKind.valueOf(kind),
    name = name,
    parentId = parentId,
    archived = archived,
    stableKey = stableKey,
)

private fun TagEntity.toDomain() = Tag(id = id, name = name)

private fun EntryEntity.toDomain() = Entry(
    id = id,
    accountId = accountId,
    type = EntryType.valueOf(type),
    amount = Money(amountMinor, CurrencyCode.of(currency)),
    date = LocalDate.parse(date),
    description = description,
    payee = payee,
    notes = notes,
    categoryId = categoryId,
    deletedAt = deletedAt,
)

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val provider: DatabaseProvider,
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.accountDao().observeAllWithBalance()
            .map { rows -> rows.map { it.account.toDomain(it.balanceMinor) } }
            .catch { emit(emptyList()) }
    }

    override fun observeAccount(id: String): Flow<Account?> {
        val database = provider.get() ?: return flowOf(null)
        return database.accountDao().observeByIdWithBalance(id)
            .map { row -> row?.let { it.account.toDomain(it.balanceMinor) } }
            .catch { emit(null) }
    }

    override suspend fun get(id: String): Account? {
        val entity = provider.get()?.accountDao()?.getById(id) ?: return null
        val currency = CurrencyCode.of(entity.currency)
        return Account(
            id = entity.id,
            name = entity.name,
            type = AccountType.valueOf(entity.type),
            currency = currency,
            archived = entity.archived,
            balance = Money.zero(currency),
        )
    }

    override suspend fun createAccount(
        name: String,
        type: AccountType,
        currency: CurrencyCode,
        startingBalance: Money?,
        openingDate: LocalDate,
    ): Outcome<Account> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return try {
            val accountId = UuidV7.generate()
            val now = System.currentTimeMillis()
            database.withTransaction {
                database.accountDao().insert(
                    AccountEntity(
                        id = accountId,
                        name = name,
                        type = type.name,
                        currency = currency.value,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                if (startingBalance != null && !startingBalance.isZero) {
                    database.entryDao().insert(
                        EntryEntity(
                            id = UuidV7.generate(),
                            accountId = accountId,
                            type = EntryType.OPENING.name,
                            amountMinor = startingBalance.amountMinor,
                            currency = currency.value,
                            date = openingDate.toString(),
                            description = OPENING_DESCRIPTION,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                }
            }
            Outcome.Success(
                Account(
                    id = accountId,
                    name = name,
                    type = type,
                    currency = currency,
                    archived = false,
                    balance = startingBalance ?: Money.zero(currency),
                ),
            )
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun updateAccount(id: String, name: String, type: AccountType): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return try {
            database.accountDao().getById(id)
                ?: return Outcome.Failure(AppError.ACCOUNT_NOT_FOUND)
            database.accountDao().updateNameAndType(id, name, type.name, System.currentTimeMillis())
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return try {
            database.accountDao().setArchived(id, archived, System.currentTimeMillis())
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun deleteAccount(id: String): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return try {
            if (database.accountDao().countEntries(id) > 0) {
                return Outcome.Failure(AppError.ACCOUNT_NOT_EMPTY)
            }
            database.accountDao().deleteById(id)
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    companion object {
        const val OPENING_DESCRIPTION = "sys:opening"
    }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val provider: DatabaseProvider,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.categoryDao().observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun get(id: String): Category? =
        provider.get()?.categoryDao()?.getById(id)?.toDomain()

    override suspend fun create(kind: CategoryKind, name: String, parentId: String?): Outcome<Category> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Outcome.Failure(AppError.NAME_REQUIRED)
        if (trimmed.length > MAX_NAME_LENGTH) return Outcome.Failure(AppError.NAME_TOO_LONG)
        if (parentId != null) {
            val parent = database.categoryDao().getById(parentId)
                ?: return Outcome.Failure(AppError.CATEGORY_NOT_FOUND)
            if (parent.kind != kind.name) return Outcome.Failure(AppError.CATEGORY_KIND_MISMATCH)
            if (parent.parentId != null) return Outcome.Failure(AppError.CATEGORY_PARENT_INVALID)
        }
        val now = System.currentTimeMillis()
        val entity = CategoryEntity(
            id = UuidV7.generate(),
            kind = kind.name,
            name = trimmed,
            parentId = parentId,
            createdAt = now,
            updatedAt = now,
        )
        return runCatching {
            database.categoryDao().insert(entity)
            Outcome.Success(entity.toDomain())
        }.getOrElse { Outcome.Failure(AppError.UNKNOWN) }
    }

    override suspend fun rename(id: String, name: String): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Outcome.Failure(AppError.NAME_REQUIRED)
        if (trimmed.length > MAX_NAME_LENGTH) return Outcome.Failure(AppError.NAME_TOO_LONG)
        return try {
            database.categoryDao().getById(id) ?: return Outcome.Failure(AppError.CATEGORY_NOT_FOUND)
            database.categoryDao().updateName(id, trimmed, System.currentTimeMillis())
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return try {
            database.categoryDao().setArchived(id, archived, System.currentTimeMillis())
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return try {
            if (database.categoryDao().countChildren(id) > 0) {
                return Outcome.Failure(AppError.CATEGORY_HAS_CHILDREN)
            }
            if (database.categoryDao().countEntries(id) > 0) {
                return Outcome.Failure(AppError.CATEGORY_IN_USE)
            }
            database.categoryDao().deleteById(id)
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun seedDefaults(): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return try {
            if (database.settingsDao().get(KEY_SEEDED) == "1") return Outcome.Success(Unit)
            val now = System.currentTimeMillis()
            database.withTransaction {
                defaultCategories().forEach { def ->
                    if (database.categoryDao().findByStableKey(def.stableKey) == null) {
                        val parentId = def.parentKey?.let { key ->
                            database.categoryDao().findByStableKey(key)?.id
                        }
                        database.categoryDao().insert(
                            CategoryEntity(
                                id = UuidV7.generate(),
                                kind = def.kind.name,
                                name = def.name,
                                parentId = parentId,
                                stableKey = def.stableKey,
                                createdAt = now,
                                updatedAt = now,
                            ),
                        )
                    }
                }
                database.settingsDao().put(SettingEntity(KEY_SEEDED, "1"))
            }
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    private data class DefaultCategory(
        val kind: CategoryKind,
        val stableKey: String,
        val name: String,
        val parentKey: String? = null,
    )

    private fun defaultCategories(): List<DefaultCategory> = listOf(
        DefaultCategory(CategoryKind.EXPENSE, "food", "Food"),
        DefaultCategory(CategoryKind.EXPENSE, "food_groceries", "Groceries", "food"),
        DefaultCategory(CategoryKind.EXPENSE, "food_restaurants", "Restaurants", "food"),
        DefaultCategory(CategoryKind.EXPENSE, "transport", "Transport"),
        DefaultCategory(CategoryKind.EXPENSE, "housing", "Housing"),
        DefaultCategory(CategoryKind.EXPENSE, "health", "Health"),
        DefaultCategory(CategoryKind.EXPENSE, "shopping", "Shopping"),
        DefaultCategory(CategoryKind.EXPENSE, "entertainment", "Entertainment"),
        DefaultCategory(CategoryKind.EXPENSE, "other_expense", "Other"),
        DefaultCategory(CategoryKind.INCOME, "salary", "Salary"),
        DefaultCategory(CategoryKind.INCOME, "bonus", "Bonus"),
        DefaultCategory(CategoryKind.INCOME, "gift", "Gift"),
        DefaultCategory(CategoryKind.INCOME, "other_income", "Other"),
    )

    companion object {
        const val MAX_NAME_LENGTH = 60
        const val KEY_SEEDED = "categories.seeded.v1"
    }
}

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val provider: DatabaseProvider,
) : TagRepository {

    override fun observeTags(): Flow<List<Tag>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.tagDao().observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun findOrCreate(name: String): Outcome<Tag> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Outcome.Failure(AppError.NAME_REQUIRED)
        val normalized = trimmed.lowercase(Locale.ROOT)

        database.tagDao().findByNormalizedName(normalized)?.let { return Outcome.Success(it.toDomain()) }

        val entity = TagEntity(
            id = UuidV7.generate(),
            name = trimmed,
            nameNormalized = normalized,
            createdAt = System.currentTimeMillis(),
        )
        database.tagDao().insert(entity)
        val stored = database.tagDao().findByNormalizedName(normalized)
            ?: return Outcome.Failure(AppError.UNKNOWN)
        return Outcome.Success(stored.toDomain())
    }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val provider: DatabaseProvider,
) : SettingsRepository {

    override fun observeBaseCurrency(): Flow<CurrencyCode?> {
        val database = provider.get() ?: return flowOf(null)
        return database.settingsDao().observe(KEY_BASE_CURRENCY)
            .map { value -> value?.let { runCatching { CurrencyCode.of(it) }.getOrNull() } }
            .catch { emit(null) }
    }

    override suspend fun getBaseCurrency(): CurrencyCode? {
        val database = provider.get() ?: return null
        return database.settingsDao().get(KEY_BASE_CURRENCY)
            ?.let { runCatching { CurrencyCode.of(it) }.getOrNull() }
    }

    override suspend fun setBaseCurrency(code: CurrencyCode) {
        provider.get()?.settingsDao()?.put(SettingEntity(KEY_BASE_CURRENCY, code.value))
    }

    companion object {
        const val KEY_BASE_CURRENCY = "base_currency"
    }
}

@Singleton
class EntryRepositoryImpl @Inject constructor(
    private val provider: DatabaseProvider,
) : EntryRepository {

    override fun observeForAccount(accountId: String): Flow<List<Entry>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.entryDao().observeForAccount(accountId)
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override fun observeAll(): Flow<List<Entry>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.entryDao().observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun get(id: String): Entry? =
        provider.get()?.entryDao()?.getById(id)?.toDomain()

    override suspend fun create(input: EntryInput): Outcome<Entry> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return try {
            val now = System.currentTimeMillis()
            val entity = EntryEntity(
                id = UuidV7.generate(),
                accountId = input.accountId,
                type = input.type.name,
                amountMinor = input.amount.amountMinor,
                currency = input.amount.currency.value,
                date = input.date.toString(),
                description = input.description,
                payee = input.payee,
                notes = input.notes,
                categoryId = input.categoryId,
                createdAt = now,
                updatedAt = now,
            )
            database.withTransaction {
                database.entryDao().insert(entity)
                linkTags(database, entity.id, input.tagNames, now)
            }
            Outcome.Success(entity.toDomain())
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun update(id: String, input: EntryInput): Outcome<Entry> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val existing = database.entryDao().getById(id)
        if (existing == null || existing.deletedAt != null || existing.type == EntryType.OPENING.name) {
            return Outcome.Failure(AppError.ENTRY_NOT_FOUND)
        }
        return try {
            val now = System.currentTimeMillis()
            val updated = existing.copy(
                accountId = input.accountId,
                type = input.type.name,
                amountMinor = input.amount.amountMinor,
                currency = input.amount.currency.value,
                date = input.date.toString(),
                description = input.description,
                payee = input.payee,
                notes = input.notes,
                categoryId = input.categoryId,
                updatedAt = now,
            )
            database.withTransaction {
                database.entryDao().update(updated)
                database.tagDao().deleteTagsForEntry(id)
                linkTags(database, id, input.tagNames, now)
            }
            Outcome.Success(updated.toDomain())
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val existing = database.entryDao().getById(id)
        if (existing == null || existing.deletedAt != null || existing.type == EntryType.OPENING.name) {
            return Outcome.Failure(AppError.ENTRY_NOT_FOUND)
        }
        return try {
            database.entryDao().softDelete(id, System.currentTimeMillis())
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun tagsForEntry(id: String): List<Tag> =
        provider.get()?.tagDao()?.getForEntry(id)?.map { it.toDomain() } ?: emptyList()
}

private suspend fun linkTags(
    database: TalexDatabase,
    entryId: String,
    tagNames: List<String>,
    now: Long,
) {
    tagNames
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .forEach { name ->
            val normalized = name.lowercase(Locale.ROOT)
            val existing = database.tagDao().findByNormalizedName(normalized)
            val tagId = existing?.id ?: run {
                database.tagDao().insert(
                    TagEntity(
                        id = UuidV7.generate(),
                        name = name,
                        nameNormalized = normalized,
                        createdAt = now,
                    ),
                )
                database.tagDao().findByNormalizedName(normalized)?.id
            }
            if (tagId != null) {
                database.tagDao().insertEntryTag(EntryTagEntity(entryId = entryId, tagId = tagId))
            }
        }
}
