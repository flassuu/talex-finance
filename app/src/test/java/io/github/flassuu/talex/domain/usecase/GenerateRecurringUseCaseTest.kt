package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryInput
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Frequency
import io.github.flassuu.talex.domain.model.RecurringInput
import io.github.flassuu.talex.domain.model.RecurringRule
import io.github.flassuu.talex.domain.model.Tag
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import io.github.flassuu.talex.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GenerateRecurringUseCaseTest {

    private val eur = CurrencyCode.EUR
    private val account = Account("acc", "Card", AccountType.CHECKING, eur, archived = false, balance = Money(0, eur))
    private val archivedAccount = Account("arch", "Old", AccountType.CHECKING, eur, archived = true, balance = Money(0, eur))

    private fun rule(
        id: String = "r1",
        active: Boolean = true,
        anchor: LocalDate = LocalDate.of(2026, 1, 1),
        nextDue: LocalDate = anchor,
        accountId: String = "acc",
    ) = RecurringRule(
        id = id,
        accountId = accountId,
        type = EntryType.EXPENSE,
        amount = Money(-1000, eur),
        description = "Rent",
        categoryId = null,
        frequency = Frequency.MONTHLY,
        anchorDate = anchor,
        nextDueDate = nextDue,
        active = active,
    )

    private fun useCase(
        rules: List<RecurringRule>,
        accounts: List<Account> = listOf(account),
        entries: FakeEntryRepository = FakeEntryRepository(),
        recurring: FakeRecurringRepository = FakeRecurringRepository(rules),
    ): Triple<GenerateRecurringUseCase, FakeEntryRepository, FakeRecurringRepository> = Triple(
        GenerateRecurringUseCase(
            recurring,
            FakeAccountRepository(accounts),
            FakeCategoryRepository(),
            entries,
        ),
        entries,
        recurring,
    )

    @Test
    fun generatesAllDueOccurrences() = runTest {
        val (useCase, entries, recurring) = useCase(listOf(rule()))
        val result = useCase(LocalDate.of(2026, 3, 15))

        assertEquals(3, result.created.size)
        assertEquals(
            listOf("2026-01-01", "2026-02-01", "2026-03-01"),
            entries.created.map { it.schedulePeriod },
        )
        assertEquals(LocalDate.of(2026, 4, 1), recurring.updatedNextDue["r1"])
    }

    @Test
    fun isIdempotentWhenPeriodsAlreadyExist() = runTest {
        val entries = FakeEntryRepository(
            existing = mutableSetOf("r1" to "2026-01-01", "r1" to "2026-02-01", "r1" to "2026-03-01"),
        )
        val (useCase, _, recurring) = useCase(listOf(rule()), entries = entries)
        val result = useCase(LocalDate.of(2026, 3, 15))

        assertEquals(0, result.created.size)
        assertEquals(0, entries.created.size)
        assertEquals(LocalDate.of(2026, 4, 1), recurring.updatedNextDue["r1"])
    }

    @Test
    fun pausedRuleGeneratesNothing() = runTest {
        val (useCase, entries, _) = useCase(listOf(rule(active = false)))
        val result = useCase(LocalDate.of(2026, 3, 15))

        assertEquals(0, result.created.size)
        assertEquals(0, entries.created.size)
    }

    @Test
    fun archivedAccountIsSkippedAndReported() = runTest {
        val (useCase, entries, _) = useCase(
            listOf(rule(accountId = "arch")),
            accounts = listOf(archivedAccount),
        )
        val result = useCase(LocalDate.of(2026, 3, 15))

        assertEquals(0, result.created.size)
        assertEquals(1, result.skipped.size)
        assertEquals(0, entries.created.size)
    }

    @Test
    fun monthEndClampsOccurrences() = runTest {
        val (useCase, entries, recurring) = useCase(
            listOf(rule(anchor = LocalDate.of(2026, 1, 31))),
        )
        val result = useCase(LocalDate.of(2026, 3, 1))

        assertEquals(2, result.created.size)
        assertEquals(listOf("2026-01-31", "2026-02-28"), entries.created.map { it.schedulePeriod })
        assertEquals(LocalDate.of(2026, 3, 31), recurring.updatedNextDue["r1"])
    }

    private class FakeAccountRepository(
        private val accounts: List<Account>,
    ) : AccountRepository {
        private val byId = accounts.associateBy { it.id }

        override fun observeAccounts(): Flow<List<Account>> = flowOf(accounts)
        override fun observeAccount(id: String): Flow<Account?> = flowOf(byId[id])
        override suspend fun get(id: String): Account? = byId[id]

        override suspend fun createAccount(
            name: String,
            type: AccountType,
            currency: CurrencyCode,
            startingBalance: Money?,
            openingDate: LocalDate,
        ): Outcome<Account> = Outcome.Failure(AppError.UNKNOWN)

        override suspend fun updateAccount(id: String, name: String, type: AccountType): Outcome<Unit> =
            Outcome.Success(Unit)

        override suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun deleteAccount(id: String): Outcome<Unit> = Outcome.Success(Unit)
    }

    private class FakeCategoryRepository : CategoryRepository {
        override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun get(id: String): Category? = null

        override suspend fun create(kind: CategoryKind, name: String, parentId: String?): Outcome<Category> =
            Outcome.Failure(AppError.UNKNOWN)

        override suspend fun rename(id: String, name: String): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun delete(id: String): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun seedDefaults(): Outcome<Unit> = Outcome.Success(Unit)
    }

    private class FakeEntryRepository(
        private val existing: MutableSet<Pair<String, String>> = mutableSetOf(),
    ) : EntryRepository {
        val created = mutableListOf<EntryInput>()

        override fun observeForAccount(accountId: String): Flow<List<Entry>> = flowOf(emptyList())
        override fun observeAll(): Flow<List<Entry>> = flowOf(emptyList())
        override fun observeAllWithLines(): Flow<List<Entry>> = flowOf(emptyList())
        override suspend fun get(id: String): Entry? = null

        override suspend fun create(input: EntryInput): Outcome<Entry> {
            created.add(input)
            return Outcome.Success(
                Entry(
                    id = "e${created.size}",
                    accountId = input.accountId,
                    type = input.type,
                    amount = input.amount,
                    date = input.date,
                    description = input.description,
                ),
            )
        }

        override suspend fun update(id: String, input: EntryInput): Outcome<Entry> =
            Outcome.Failure(AppError.UNKNOWN)

        override suspend fun delete(id: String): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun tagsForEntry(id: String): List<Tag> = emptyList()

        override suspend fun existsForRulePeriod(ruleId: String, period: String): Boolean =
            (ruleId to period) in existing
    }

    private class FakeRecurringRepository(
        private val rules: List<RecurringRule>,
    ) : RecurringRepository {
        val updatedNextDue = mutableMapOf<String, LocalDate>()

        override fun observeRules(): Flow<List<RecurringRule>> = flowOf(rules)
        override suspend fun get(id: String): RecurringRule? = rules.firstOrNull { it.id == id }
        override suspend fun getAll(): List<RecurringRule> = rules

        override suspend fun create(input: RecurringInput): Outcome<RecurringRule> =
            Outcome.Failure(AppError.UNKNOWN)

        override suspend fun update(id: String, input: RecurringInput): Outcome<RecurringRule> =
            Outcome.Failure(AppError.UNKNOWN)

        override suspend fun delete(id: String): Outcome<Unit> = Outcome.Success(Unit)

        override suspend fun setActive(id: String, active: Boolean, today: LocalDate): Outcome<Unit> =
            Outcome.Success(Unit)

        override suspend fun updateNextDueDate(id: String, nextDueDate: LocalDate) {
            updatedNextDue[id] = nextDueDate
        }
    }
}
