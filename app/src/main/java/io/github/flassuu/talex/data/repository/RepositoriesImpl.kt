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
import io.github.flassuu.talex.data.db.SettingEntity
import io.github.flassuu.talex.data.db.TagEntity
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Tag
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
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
)

private fun TagEntity.toDomain() = Tag(id = id, name = name)

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

    override suspend fun create(kind: CategoryKind, name: String, parentId: String?): Outcome<Category> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Outcome.Failure(AppError.NAME_REQUIRED)
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
