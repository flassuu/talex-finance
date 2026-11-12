package io.github.flassuu.talex.domain.repository

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Budget
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryInput
import io.github.flassuu.talex.domain.model.Goal
import io.github.flassuu.talex.domain.model.GoalContribution
import io.github.flassuu.talex.domain.model.GoalContributionInput
import io.github.flassuu.talex.domain.model.GoalInput
import io.github.flassuu.talex.domain.model.Rate
import io.github.flassuu.talex.domain.model.RecurringInput
import io.github.flassuu.talex.domain.model.RecurringRule
import io.github.flassuu.talex.domain.model.Tag
import io.github.flassuu.talex.domain.model.Transfer
import io.github.flassuu.talex.domain.model.TransferInput
import io.github.flassuu.talex.domain.model.Valuation
import io.github.flassuu.talex.domain.model.ValuationInput
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>

    fun observeAccount(id: String): Flow<Account?>

    suspend fun get(id: String): Account?

    suspend fun createAccount(
        name: String,
        type: AccountType,
        currency: CurrencyCode,
        startingBalance: Money?,
        openingDate: LocalDate,
    ): Outcome<Account>

    suspend fun updateAccount(id: String, name: String, type: AccountType): Outcome<Unit>

    suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit>

    /** Hard delete; the UI must confirm because it removes the account and its history. */
    suspend fun deleteAccount(id: String): Outcome<Unit>

    /** Writes an adjustment entry so the account balance becomes [newBalance]. */
    suspend fun adjustBalance(id: String, newBalance: Money, date: LocalDate): Outcome<Unit> =
        Outcome.Failure(AppError.UNKNOWN)
}

interface EntryRepository {
    fun observeForAccount(accountId: String): Flow<List<Entry>>

    fun observeAll(): Flow<List<Entry>>

    /** Entries with their split lines, for reporting and the overview. */
    fun observeAllWithLines(): Flow<List<Entry>>

    suspend fun get(id: String): Entry?

    suspend fun create(input: EntryInput): Outcome<Entry>

    suspend fun update(id: String, input: EntryInput): Outcome<Entry>

    suspend fun delete(id: String): Outcome<Unit>

    suspend fun tagsForEntry(id: String): List<Tag>

    suspend fun existsForRulePeriod(ruleId: String, period: String): Boolean
}

interface RecurringRepository {
    fun observeRules(): Flow<List<RecurringRule>>

    suspend fun get(id: String): RecurringRule?

    suspend fun getAll(): List<RecurringRule>

    suspend fun create(input: RecurringInput): Outcome<RecurringRule>

    suspend fun update(id: String, input: RecurringInput): Outcome<RecurringRule>

    suspend fun delete(id: String): Outcome<Unit>

    /** Pausing advances the next due date; resuming does not backfill missed periods. */
    suspend fun setActive(id: String, active: Boolean, today: LocalDate): Outcome<Unit>

    suspend fun updateNextDueDate(id: String, nextDueDate: LocalDate)
}

interface RateRepository {
    /** Latest rate per (base, quote) pair, as decimal strings. */
    fun observeLatest(): Flow<Map<Pair<String, String>, String>>

    /** Every stored rate point, for date-aware conversion (budgets, ADR-0010). */
    fun observeAll(): Flow<List<Rate>>

    suspend fun latest(base: String, quote: String): String?
}

interface ValuationRepository {
    fun observeAll(): Flow<List<Valuation>>

    fun observeForAccount(accountId: String): Flow<List<Valuation>>

    suspend fun get(id: String): Valuation?

    suspend fun latestForAccount(accountId: String): Valuation?

    suspend fun add(accountId: String, input: ValuationInput): Outcome<Valuation>

    suspend fun update(id: String, input: ValuationInput): Outcome<Valuation>

    suspend fun delete(id: String): Outcome<Unit>
}

interface BudgetRepository {
    fun observeBudgets(): Flow<List<Budget>>

    /** Sets or changes the limit in force for the current month (ADR-0010). */
    suspend fun setLimit(categoryId: String, amount: Money, today: LocalDate): Outcome<Unit>

    /** Closes the current period; historical rows stay for past months. */
    suspend fun removeBudget(categoryId: String, today: LocalDate): Outcome<Unit>
}

interface GoalRepository {
    fun observeGoals(): Flow<List<Goal>>

    suspend fun get(id: String): Goal?

    suspend fun create(input: GoalInput): Outcome<Goal>

    suspend fun update(id: String, input: GoalInput): Outcome<Goal>

    suspend fun delete(id: String): Outcome<Unit>

    suspend fun addContribution(goalId: String, input: GoalContributionInput): Outcome<Unit>

    suspend fun removeContribution(contributionId: String): Outcome<Unit>

    fun observeContributions(goalId: String): Flow<List<GoalContribution>>
}

interface TransferRepository {
    fun observeTransfers(): Flow<List<Transfer>>

    suspend fun get(id: String): Transfer?

    suspend fun create(input: TransferInput): Outcome<Transfer>

    suspend fun update(id: String, input: TransferInput): Outcome<Transfer>

    suspend fun delete(id: String): Outcome<Unit>
}

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>

    suspend fun get(id: String): Category?

    suspend fun create(kind: CategoryKind, name: String, parentId: String? = null): Outcome<Category>

    suspend fun rename(id: String, name: String): Outcome<Unit>

    suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit>

    /** Marks spending in this category as lost money (ADR-0024). */
    suspend fun setUnnecessary(id: String, unnecessary: Boolean): Outcome<Unit>

    suspend fun delete(id: String): Outcome<Unit>

    /** Idempotent seeding of the default categories, guarded by a settings flag. */
    suspend fun seedDefaults(): Outcome<Unit>
}

interface TagRepository {
    fun observeTags(): Flow<List<Tag>>
    suspend fun findOrCreate(name: String): Outcome<Tag>
}

interface SettingsRepository {
    fun observeBaseCurrency(): Flow<CurrencyCode?>
    suspend fun getBaseCurrency(): CurrencyCode?
    suspend fun setBaseCurrency(code: CurrencyCode)
}

/** One row the CSV import could not write, with a reason code for the UI. */
data class ImportSkip(val line: Int, val reason: AppError)

data class ImportReport(
    val imported: Int,
    val skipped: List<ImportSkip>,
)

/** Plaintext CSV export and import (ADR-0027). */
interface DataTransferRepository {
    suspend fun exportTransactionsCsv(): String
    suspend fun exportCategoriesCsv(): String

    /** Validates and writes the whole batch atomically, or rejects it. */
    suspend fun importTransactionsCsv(text: String): Outcome<ImportReport>
}
