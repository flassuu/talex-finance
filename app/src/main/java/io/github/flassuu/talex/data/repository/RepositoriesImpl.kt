package io.github.flassuu.talex.data.repository

import androidx.room.withTransaction
import io.github.flassuu.talex.core.id.UuidV7
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.data.db.AccountEntity
import io.github.flassuu.talex.data.db.BudgetEntity
import io.github.flassuu.talex.data.db.CategoryEntity
import io.github.flassuu.talex.data.db.DatabaseProvider
import io.github.flassuu.talex.data.db.EntryEntity
import io.github.flassuu.talex.data.db.EntryLineEntity
import io.github.flassuu.talex.data.db.EntryTagEntity
import io.github.flassuu.talex.data.db.EntryWithLines
import io.github.flassuu.talex.data.db.GoalContributionEntity
import io.github.flassuu.talex.data.db.GoalEntity
import io.github.flassuu.talex.data.db.GoalWithTotal
import io.github.flassuu.talex.data.db.RateEntity
import io.github.flassuu.talex.data.db.RecurringRuleEntity
import io.github.flassuu.talex.data.db.SettingEntity
import io.github.flassuu.talex.data.db.TagEntity
import io.github.flassuu.talex.data.db.TalexDatabase
import io.github.flassuu.talex.data.db.TransferGroupEntity
import io.github.flassuu.talex.data.db.ValuationEntity
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryInput
import io.github.flassuu.talex.domain.model.EntryLine
import io.github.flassuu.talex.domain.model.EntryLineInput
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Frequency
import io.github.flassuu.talex.domain.model.Goal
import io.github.flassuu.talex.domain.model.GoalContribution
import io.github.flassuu.talex.domain.model.GoalContributionInput
import io.github.flassuu.talex.domain.model.GoalInput
import io.github.flassuu.talex.domain.model.Rate
import io.github.flassuu.talex.domain.model.RecurringInput
import io.github.flassuu.talex.domain.model.RecurringRule
import io.github.flassuu.talex.domain.model.RecurringSchedule
import io.github.flassuu.talex.domain.model.Tag
import io.github.flassuu.talex.domain.model.Transfer
import io.github.flassuu.talex.domain.model.TransferInput
import io.github.flassuu.talex.domain.model.Valuation
import io.github.flassuu.talex.domain.model.ValuationInput
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.BudgetRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import io.github.flassuu.talex.domain.repository.GoalRepository
import io.github.flassuu.talex.domain.repository.RateRepository
import io.github.flassuu.talex.domain.repository.RecurringRepository
import io.github.flassuu.talex.domain.repository.SettingsRepository
import io.github.flassuu.talex.domain.repository.TagRepository
import io.github.flassuu.talex.domain.repository.TransferRepository
import io.github.flassuu.talex.domain.repository.ValuationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
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
    unnecessary = unnecessary,
)

private fun TagEntity.toDomain() = Tag(id = id, name = name)

private fun ValuationEntity.toDomain() = Valuation(
    id = id,
    accountId = accountId,
    amount = Money(amountMinor, CurrencyCode.of(currency)),
    date = LocalDate.parse(date),
)

private fun BudgetEntity.toDomain() = Budget(
    id = id,
    categoryId = categoryId,
    amount = Money(amountMinor, CurrencyCode.of(currency)),
    effectiveFrom = LocalDate.parse(effectiveFrom),
    closedAt = closedAt?.let { LocalDate.parse(it) },
)

private fun GoalEntity.toDomain(currentMinor: Long) = Goal(
    id = id,
    name = name,
    targetAmount = Money(targetMinor, CurrencyCode.of(currency)),
    targetDate = targetDate?.let { LocalDate.parse(it) },
    currency = CurrencyCode.of(currency),
    currentAmount = Money(currentMinor, CurrencyCode.of(currency)),
)

private fun GoalContributionEntity.toDomain() = GoalContribution(
    id = id,
    goalId = goalId,
    amount = Money(amountMinor, CurrencyCode.of(currency)),
    date = LocalDate.parse(date),
    note = note,
)

private fun GoalWithTotal.toDomain() = goal.toDomain(currentMinor)

private fun RateEntity.toDomain() = Rate(
    base = CurrencyCode.of(baseCurrency),
    quote = CurrencyCode.of(quoteCurrency),
    date = LocalDate.parse(date),
    rate = rate.toBigDecimalOrNull() ?: BigDecimal.ONE,
    source = source,
)

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
    transferGroupId = transferGroupId,
    sourceRuleId = sourceRuleId,
    deletedAt = deletedAt,
)

private fun EntryLineEntity.toDomain(currency: CurrencyCode) = EntryLine(
    id = id,
    categoryId = categoryId,
    amount = Money(amountMinor, currency),
    note = note,
)

private fun EntryWithLines.toDomain(): Entry {
    val currency = CurrencyCode.of(entry.currency)
    return entry.toDomain().copy(lines = lines.map { it.toDomain(currency) })
}

private fun RecurringRuleEntity.toDomain() = RecurringRule(
    id = id,
    accountId = accountId,
    type = EntryType.valueOf(type),
    amount = Money(amountMinor, CurrencyCode.of(currency)),
    description = description,
    categoryId = categoryId,
    frequency = Frequency.valueOf(frequency),
    anchorDate = LocalDate.parse(anchorDate),
    nextDueDate = LocalDate.parse(nextDueDate),
    active = active,
)

private fun TransferGroupEntity.toDomain() = Transfer(
    id = id,
    sourceAccountId = sourceAccountId,
    destinationAccountId = destinationAccountId,
    sourceAmount = Money(sourceAmountMinor, CurrencyCode.of(sourceCurrency)),
    destinationAmount = Money(destinationAmountMinor, CurrencyCode.of(destinationCurrency)),
    rate = rate.toBigDecimalOrNull() ?: BigDecimal.ONE,
    date = LocalDate.parse(date),
    description = description,
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
                    if (type.isValuationBased) {
                        // An asset starts with its first valuation, not an opening entry.
                        database.valuationDao().insert(
                            ValuationEntity(
                                id = UuidV7.generate(),
                                accountId = accountId,
                                amountMinor = startingBalance.amountMinor,
                                currency = currency.value,
                                date = openingDate.toString(),
                                createdAt = now,
                            ),
                        )
                    } else {
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
            database.withTransaction {
                // Remove both sides of every transfer that touches this account, so
                // no half-transfer or dangling group remains.
                database.transferDao().getGroupsForAccount(id).forEach { group ->
                    listOf(group.sourceEntryId, group.destinationEntryId).forEach { entryId ->
                        database.tagDao().deleteTagsForEntry(entryId)
                        database.entryLineDao().deleteForEntry(entryId)
                        database.entryDao().deleteById(entryId)
                    }
                    database.transferDao().deleteGroup(group.id)
                }
                database.entryDao().deleteTagsForAccount(id)
                database.entryDao().deleteLinesForAccount(id)
                database.entryDao().deleteForAccount(id)
                database.valuationDao().deleteForAccount(id)
                database.accountDao().deleteById(id)
            }
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun adjustBalance(id: String, newBalance: Money, date: LocalDate): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val account = database.accountDao().getById(id)
            ?: return Outcome.Failure(AppError.ACCOUNT_NOT_FOUND)
        if (CurrencyCode.of(account.currency) != newBalance.currency) {
            return Outcome.Failure(AppError.CURRENCY_MISMATCH)
        }
        return try {
            database.withTransaction {
                val current = database.entryDao().sumForAccount(id)
                val delta = newBalance.amountMinor - current
                if (delta != 0L) {
                    val now = System.currentTimeMillis()
                    database.entryDao().insert(
                        EntryEntity(
                            id = UuidV7.generate(),
                            accountId = id,
                            type = EntryType.ADJUSTMENT.name,
                            amountMinor = delta,
                            currency = account.currency,
                            date = date.toString(),
                            description = ADJUSTMENT_DESCRIPTION,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                }
            }
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    companion object {
        const val OPENING_DESCRIPTION = "sys:opening"
        const val ADJUSTMENT_DESCRIPTION = "sys:adjustment"
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

    override suspend fun setUnnecessary(id: String, unnecessary: Boolean): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return try {
            database.categoryDao().setUnnecessary(id, unnecessary, System.currentTimeMillis())
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

    override fun observeAllWithLines(): Flow<List<Entry>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.entryDao().observeAllWithLines()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun get(id: String): Entry? {
        val database = provider.get() ?: return null
        val entity = database.entryDao().getById(id) ?: return null
        val currency = CurrencyCode.of(entity.currency)
        val lines = database.entryLineDao().getForEntry(id).map { it.toDomain(currency) }
        return entity.toDomain().copy(lines = lines)
    }

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
                sourceRuleId = input.sourceRuleId,
                schedulePeriod = input.schedulePeriod,
                createdAt = now,
                updatedAt = now,
            )
            database.withTransaction {
                database.entryDao().insert(entity)
                linkTags(database, entity.id, input.tagNames, now)
                writeLines(database, entity.id, input.lines)
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
                database.entryLineDao().deleteForEntry(id)
                writeLines(database, id, input.lines)
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

    override suspend fun existsForRulePeriod(ruleId: String, period: String): Boolean =
        (provider.get()?.entryDao()?.countForRulePeriod(ruleId, period) ?: 0) > 0
}

private suspend fun writeLines(
    database: TalexDatabase,
    entryId: String,
    lines: List<EntryLineInput>,
) {
    if (lines.isEmpty()) return
    database.entryLineDao().insertAll(
        lines.mapIndexed { index, line ->
            EntryLineEntity(
                id = UuidV7.generate(),
                entryId = entryId,
                categoryId = line.categoryId,
                amountMinor = line.amount.amountMinor,
                note = line.note,
                position = index,
            )
        },
    )
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

@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val provider: DatabaseProvider,
) : TransferRepository {

    override fun observeTransfers(): Flow<List<Transfer>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.transferDao().observeGroups()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun get(id: String): Transfer? =
        provider.get()?.transferDao()?.getGroup(id)?.toDomain()

    override suspend fun create(input: TransferInput): Outcome<Transfer> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val now = System.currentTimeMillis()
        val groupId = UuidV7.generate()
        val sourceEntryId = UuidV7.generate()
        val destinationEntryId = UuidV7.generate()
        val rate = transferRate(input)
        val rateText = rate.toPlainString()
        val description = input.description.trim().ifEmpty { DEFAULT_DESCRIPTION }
        return try {
            database.withTransaction {
                database.entryDao().insert(
                    transferEntry(
                        id = sourceEntryId,
                        accountId = input.sourceAccountId,
                        amountMinor = -input.sourceAmount.amountMinor,
                        currency = input.sourceAmount.currency.value,
                        date = input.date.toString(),
                        description = description,
                        groupId = groupId,
                        now = now,
                    ),
                )
                database.entryDao().insert(
                    transferEntry(
                        id = destinationEntryId,
                        accountId = input.destinationAccountId,
                        amountMinor = input.destinationAmount.amountMinor,
                        currency = input.destinationAmount.currency.value,
                        date = input.date.toString(),
                        description = description,
                        groupId = groupId,
                        now = now,
                    ),
                )
                database.transferDao().insertGroup(
                    transferGroup(
                        id = groupId,
                        sourceEntryId = sourceEntryId,
                        destinationEntryId = destinationEntryId,
                        input = input,
                        rateText = rateText,
                        description = description,
                        now = now,
                    ),
                )
                database.rateDao().upsert(
                    RateEntity(
                        baseCurrency = input.sourceAmount.currency.value,
                        quoteCurrency = input.destinationAmount.currency.value,
                        date = input.date.toString(),
                        rate = rateText,
                        source = RATE_SOURCE_MANUAL,
                        updatedAt = now,
                    ),
                )
            }
            Outcome.Success(toTransfer(groupId, input, description, rate))
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun update(id: String, input: TransferInput): Outcome<Transfer> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val group = database.transferDao().getGroup(id) ?: return Outcome.Failure(AppError.ENTRY_NOT_FOUND)
        val source = database.entryDao().getById(group.sourceEntryId)
        val destination = database.entryDao().getById(group.destinationEntryId)
        if (source == null || destination == null || source.deletedAt != null || destination.deletedAt != null) {
            return Outcome.Failure(AppError.ENTRY_NOT_FOUND)
        }
        val now = System.currentTimeMillis()
        val rate = transferRate(input)
        val rateText = rate.toPlainString()
        val description = input.description.trim().ifEmpty { DEFAULT_DESCRIPTION }
        return try {
            database.withTransaction {
                database.entryDao().update(
                    source.copy(
                        accountId = input.sourceAccountId,
                        amountMinor = -input.sourceAmount.amountMinor,
                        currency = input.sourceAmount.currency.value,
                        date = input.date.toString(),
                        description = description,
                        updatedAt = now,
                    ),
                )
                database.entryDao().update(
                    destination.copy(
                        accountId = input.destinationAccountId,
                        amountMinor = input.destinationAmount.amountMinor,
                        currency = input.destinationAmount.currency.value,
                        date = input.date.toString(),
                        description = description,
                        updatedAt = now,
                    ),
                )
                database.transferDao().updateGroup(
                    transferGroup(
                        id = group.id,
                        sourceEntryId = group.sourceEntryId,
                        destinationEntryId = group.destinationEntryId,
                        input = input,
                        rateText = rateText,
                        description = description,
                        now = group.createdAt,
                    ),
                )
            }
            Outcome.Success(toTransfer(id, input, description, rate))
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val group = database.transferDao().getGroup(id) ?: return Outcome.Failure(AppError.ENTRY_NOT_FOUND)
        return try {
            val now = System.currentTimeMillis()
            database.withTransaction {
                database.entryDao().softDelete(group.sourceEntryId, now)
                database.entryDao().softDelete(group.destinationEntryId, now)
            }
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    private fun transferRate(input: TransferInput): BigDecimal {
        if (input.sourceAmount.isZero) return BigDecimal.ONE
        val sourceMajor = BigDecimal(input.sourceAmount.amountMinor)
            .movePointLeft(input.sourceAmount.currency.fractionDigits)
        val destinationMajor = BigDecimal(input.destinationAmount.amountMinor)
            .movePointLeft(input.destinationAmount.currency.fractionDigits)
        return destinationMajor.divide(sourceMajor, 8, RoundingMode.HALF_UP).stripTrailingZeros()
    }

    private fun toTransfer(
        id: String,
        input: TransferInput,
        description: String,
        rate: BigDecimal,
    ) = Transfer(
        id = id,
        sourceAccountId = input.sourceAccountId,
        destinationAccountId = input.destinationAccountId,
        sourceAmount = input.sourceAmount,
        destinationAmount = input.destinationAmount,
        rate = rate,
        date = input.date,
        description = description,
    )

    private fun transferEntry(
        id: String,
        accountId: String,
        amountMinor: Long,
        currency: String,
        date: String,
        description: String,
        groupId: String,
        now: Long,
    ) = EntryEntity(
        id = id,
        accountId = accountId,
        type = EntryType.TRANSFER.name,
        amountMinor = amountMinor,
        currency = currency,
        date = date,
        description = description,
        transferGroupId = groupId,
        createdAt = now,
        updatedAt = now,
    )

    private fun transferGroup(
        id: String,
        sourceEntryId: String,
        destinationEntryId: String,
        input: TransferInput,
        rateText: String,
        description: String,
        now: Long,
    ) = TransferGroupEntity(
        id = id,
        sourceEntryId = sourceEntryId,
        destinationEntryId = destinationEntryId,
        sourceAccountId = input.sourceAccountId,
        destinationAccountId = input.destinationAccountId,
        sourceAmountMinor = input.sourceAmount.amountMinor,
        destinationAmountMinor = input.destinationAmount.amountMinor,
        sourceCurrency = input.sourceAmount.currency.value,
        destinationCurrency = input.destinationAmount.currency.value,
        rate = rateText,
        date = input.date.toString(),
        rateDate = input.date.toString(),
        rateSource = RATE_SOURCE_MANUAL,
        description = description,
        createdAt = now,
    )

    companion object {
        const val DEFAULT_DESCRIPTION = "Transfer"
        const val RATE_SOURCE_MANUAL = "manual"
    }
}

@Singleton
class RecurringRepositoryImpl @Inject constructor(
    private val provider: DatabaseProvider,
) : RecurringRepository {

    override fun observeRules(): Flow<List<RecurringRule>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.recurringDao().observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun get(id: String): RecurringRule? =
        provider.get()?.recurringDao()?.getById(id)?.toDomain()

    override suspend fun getAll(): List<RecurringRule> =
        provider.get()?.recurringDao()?.getAll()?.map { it.toDomain() } ?: emptyList()

    override suspend fun create(input: RecurringInput): Outcome<RecurringRule> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val now = System.currentTimeMillis()
        val entity = RecurringRuleEntity(
            id = UuidV7.generate(),
            accountId = input.accountId,
            type = input.type.name,
            amountMinor = input.amount.amountMinor,
            currency = input.amount.currency.value,
            description = input.description,
            categoryId = input.categoryId,
            frequency = input.frequency.name,
            anchorDate = input.anchorDate.toString(),
            nextDueDate = input.anchorDate.toString(),
            active = input.active,
            createdAt = now,
            updatedAt = now,
        )
        return try {
            database.recurringDao().insert(entity)
            Outcome.Success(entity.toDomain())
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun update(id: String, input: RecurringInput): Outcome<RecurringRule> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val existing = database.recurringDao().getById(id)
            ?: return Outcome.Failure(AppError.ENTRY_NOT_FOUND)
        val updated = existing.copy(
            accountId = input.accountId,
            type = input.type.name,
            amountMinor = input.amount.amountMinor,
            currency = input.amount.currency.value,
            description = input.description,
            categoryId = input.categoryId,
            frequency = input.frequency.name,
            anchorDate = input.anchorDate.toString(),
            active = input.active,
            updatedAt = System.currentTimeMillis(),
        )
        return try {
            database.recurringDao().update(updated)
            Outcome.Success(updated.toDomain())
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return try {
            database.recurringDao().deleteById(id)
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun setActive(id: String, active: Boolean, today: LocalDate): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val existing = database.recurringDao().getById(id)
            ?: return Outcome.Failure(AppError.ENTRY_NOT_FOUND)
        val rule = existing.toDomain()
        val nextDue = if (active) {
            RecurringSchedule.firstOnOrAfter(rule.anchorDate, rule.frequency, today)
        } else {
            rule.nextDueDate
        }
        return try {
            database.recurringDao().setActive(id, active, nextDue.toString(), System.currentTimeMillis())
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun updateNextDueDate(id: String, nextDueDate: LocalDate) {
        provider.get()?.recurringDao()?.updateNextDueDate(
            id,
            nextDueDate.toString(),
            System.currentTimeMillis(),
        )
    }
}

@Singleton
class RateRepositoryImpl @Inject constructor(
    private val provider: DatabaseProvider,
) : RateRepository {

    override fun observeLatest(): Flow<Map<Pair<String, String>, String>> {
        val database = provider.get() ?: return flowOf(emptyMap<Pair<String, String>, String>())
        return database.rateDao().observeAll()
            .map { rows ->
                val result = mutableMapOf<Pair<String, String>, String>()
                rows.forEach { row ->
                    result.getOrPut(row.baseCurrency to row.quoteCurrency) { row.rate }
                }
                result.toMap()
            }
            .catch { emit(emptyMap<Pair<String, String>, String>()) }
    }

    override fun observeAll(): Flow<List<Rate>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.rateDao().observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun latest(base: String, quote: String): String? =
        provider.get()?.rateDao()?.latest(base, quote)?.rate
}

@Singleton
class ValuationRepositoryImpl @Inject constructor(
    private val provider: DatabaseProvider,
) : ValuationRepository {

    override fun observeAll(): Flow<List<Valuation>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.valuationDao().observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override fun observeForAccount(accountId: String): Flow<List<Valuation>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.valuationDao().observeForAccount(accountId)
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun get(id: String): Valuation? =
        provider.get()?.valuationDao()?.getById(id)?.toDomain()

    override suspend fun latestForAccount(accountId: String): Valuation? =
        provider.get()?.valuationDao()?.latestForAccount(accountId)?.toDomain()

    override suspend fun add(accountId: String, input: ValuationInput): Outcome<Valuation> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val account = database.accountDao().getById(accountId)
            ?: return Outcome.Failure(AppError.ACCOUNT_NOT_FOUND)
        if (!AccountType.valueOf(account.type).isValuationBased) {
            return Outcome.Failure(AppError.ACCOUNT_TYPE_INVALID)
        }
        if (CurrencyCode.of(account.currency) != input.amount.currency) {
            return Outcome.Failure(AppError.CURRENCY_MISMATCH)
        }
        if (input.amount.isNegative) return Outcome.Failure(AppError.AMOUNT_INVALID)
        val entity = ValuationEntity(
            id = UuidV7.generate(),
            accountId = accountId,
            amountMinor = input.amount.amountMinor,
            currency = input.amount.currency.value,
            date = input.date.toString(),
            createdAt = System.currentTimeMillis(),
        )
        return runCatching {
            database.valuationDao().insert(entity)
            Outcome.Success(entity.toDomain())
        }.getOrElse { Outcome.Failure(AppError.UNKNOWN) }
    }

    override suspend fun update(id: String, input: ValuationInput): Outcome<Valuation> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val existing = database.valuationDao().getById(id)
            ?: return Outcome.Failure(AppError.VALUATION_NOT_FOUND)
        val account = database.accountDao().getById(existing.accountId)
            ?: return Outcome.Failure(AppError.ACCOUNT_NOT_FOUND)
        if (CurrencyCode.of(account.currency) != input.amount.currency) {
            return Outcome.Failure(AppError.CURRENCY_MISMATCH)
        }
        if (input.amount.isNegative) return Outcome.Failure(AppError.AMOUNT_INVALID)
        val updated = existing.copy(
            amountMinor = input.amount.amountMinor,
            currency = input.amount.currency.value,
            date = input.date.toString(),
        )
        return runCatching {
            database.valuationDao().update(updated)
            Outcome.Success(updated.toDomain())
        }.getOrElse { Outcome.Failure(AppError.UNKNOWN) }
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return runCatching {
            database.valuationDao().deleteById(id)
            Outcome.Success(Unit)
        }.getOrElse { Outcome.Failure(AppError.UNKNOWN) }
    }
}

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val provider: DatabaseProvider,
) : BudgetRepository {

    override fun observeBudgets(): Flow<List<Budget>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.budgetDao().observeAll()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun setLimit(categoryId: String, amount: Money, today: LocalDate): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val category = database.categoryDao().getById(categoryId)
            ?: return Outcome.Failure(AppError.CATEGORY_NOT_FOUND)
        if (category.kind != CategoryKind.EXPENSE.name) {
            return Outcome.Failure(AppError.CATEGORY_KIND_MISMATCH)
        }
        if (amount.isZero || amount.isNegative) return Outcome.Failure(AppError.AMOUNT_INVALID)
        val now = System.currentTimeMillis()
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        return try {
            database.withTransaction {
                val active = database.budgetDao().activeForCategory(categoryId)
                val activeFrom = active?.effectiveFrom?.let { LocalDate.parse(it) }
                if (active != null && activeFrom != null &&
                    !activeFrom.isBefore(monthStart) && !activeFrom.isAfter(monthEnd)
                ) {
                    // A limit for this month already exists; change it in place.
                    database.budgetDao().updateAmount(active.id, amount.amountMinor, now)
                } else {
                    // Close the previous limit and append one effective this month.
                    if (active != null) {
                        database.budgetDao().close(active.id, today.toString(), now)
                    }
                    database.budgetDao().insert(
                        BudgetEntity(
                            id = UuidV7.generate(),
                            categoryId = categoryId,
                            amountMinor = amount.amountMinor,
                            currency = amount.currency.value,
                            effectiveFrom = monthStart.toString(),
                            closedAt = null,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                }
            }
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }

    override suspend fun removeBudget(categoryId: String, today: LocalDate): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return try {
            val active = database.budgetDao().activeForCategory(categoryId)
                ?: return Outcome.Success(Unit)
            database.budgetDao().close(active.id, today.toString(), System.currentTimeMillis())
            Outcome.Success(Unit)
        } catch (t: Throwable) {
            Outcome.Failure(AppError.UNKNOWN)
        }
    }
}

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val provider: DatabaseProvider,
) : GoalRepository {

    override fun observeGoals(): Flow<List<Goal>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.goalDao().observeAllWithTotal()
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    override suspend fun get(id: String): Goal? {
        val database = provider.get() ?: return null
        val row = database.goalDao().getByIdWithTotal(id) ?: return null
        val contributions = database.goalDao().contributionsFor(id).map { it.toDomain() }
        return row.toDomain().copy(contributions = contributions)
    }

    override suspend fun create(input: GoalInput): Outcome<Goal> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val name = input.name.trim()
        if (name.isEmpty()) return Outcome.Failure(AppError.NAME_REQUIRED)
        if (name.length > MAX_NAME_LENGTH) return Outcome.Failure(AppError.NAME_TOO_LONG)
        if (input.targetAmount.isZero || input.targetAmount.isNegative) {
            return Outcome.Failure(AppError.AMOUNT_INVALID)
        }
        val now = System.currentTimeMillis()
        val entity = GoalEntity(
            id = UuidV7.generate(),
            name = name,
            targetMinor = input.targetAmount.amountMinor,
            currency = input.targetAmount.currency.value,
            targetDate = input.targetDate?.toString(),
            createdAt = now,
            updatedAt = now,
        )
        return runCatching {
            database.goalDao().insert(entity)
            Outcome.Success(entity.toDomain(0L))
        }.getOrElse { Outcome.Failure(AppError.UNKNOWN) }
    }

    override suspend fun update(id: String, input: GoalInput): Outcome<Goal> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val existing = database.goalDao().getByIdWithTotal(id)
            ?: return Outcome.Failure(AppError.GOAL_NOT_FOUND)
        val name = input.name.trim()
        if (name.isEmpty()) return Outcome.Failure(AppError.NAME_REQUIRED)
        if (name.length > MAX_NAME_LENGTH) return Outcome.Failure(AppError.NAME_TOO_LONG)
        if (input.targetAmount.isZero || input.targetAmount.isNegative) {
            return Outcome.Failure(AppError.AMOUNT_INVALID)
        }
        if (CurrencyCode.of(existing.goal.currency) != input.targetAmount.currency) {
            return Outcome.Failure(AppError.CURRENCY_MISMATCH)
        }
        val updated = existing.goal.copy(
            name = name,
            targetMinor = input.targetAmount.amountMinor,
            targetDate = input.targetDate?.toString(),
            updatedAt = System.currentTimeMillis(),
        )
        return runCatching {
            database.goalDao().update(updated)
            Outcome.Success(GoalWithTotal(updated, existing.currentMinor).toDomain())
        }.getOrElse { Outcome.Failure(AppError.UNKNOWN) }
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return runCatching {
            // Contributions are removed by the foreign-key cascade.
            database.goalDao().deleteById(id)
            Outcome.Success(Unit)
        }.getOrElse { Outcome.Failure(AppError.UNKNOWN) }
    }

    override suspend fun addContribution(goalId: String, input: GoalContributionInput): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        val goal = database.goalDao().getByIdWithTotal(goalId)?.goal
            ?: return Outcome.Failure(AppError.GOAL_NOT_FOUND)
        if (input.amount.isZero) return Outcome.Failure(AppError.AMOUNT_INVALID)
        if (CurrencyCode.of(goal.currency) != input.amount.currency) {
            return Outcome.Failure(AppError.CURRENCY_MISMATCH)
        }
        val entity = GoalContributionEntity(
            id = UuidV7.generate(),
            goalId = goalId,
            amountMinor = input.amount.amountMinor,
            currency = input.amount.currency.value,
            date = input.date.toString(),
            note = input.note?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = System.currentTimeMillis(),
        )
        return runCatching {
            database.goalDao().insertContribution(entity)
            Outcome.Success(Unit)
        }.getOrElse { Outcome.Failure(AppError.UNKNOWN) }
    }

    override suspend fun removeContribution(contributionId: String): Outcome<Unit> {
        val database = provider.get() ?: return Outcome.Failure(AppError.DATABASE_LOCKED)
        return runCatching {
            database.goalDao().deleteContribution(contributionId)
            Outcome.Success(Unit)
        }.getOrElse { Outcome.Failure(AppError.UNKNOWN) }
    }

    override fun observeContributions(goalId: String): Flow<List<GoalContribution>> {
        val database = provider.get() ?: return flowOf(emptyList())
        return database.goalDao().observeContributions(goalId)
            .map { rows -> rows.map { it.toDomain() } }
            .catch { emit(emptyList()) }
    }

    companion object {
        const val MAX_NAME_LENGTH = 60
    }
}
