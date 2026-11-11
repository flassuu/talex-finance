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
import io.github.flassuu.talex.domain.model.Tag
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SaveEntryUseCaseTest {

    private val date = LocalDate.of(2026, 4, 1)
    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode.USD

    private val card = Account("acc", "Card", AccountType.CHECKING, eur, archived = false, balance = Money(0, eur))
    private val archived = Account("arch", "Old", AccountType.CASH, eur, archived = true, balance = Money(0, eur))
    private val food = Category("cat-exp", CategoryKind.EXPENSE, "Food", null, archived = false)
    private val salary = Category("cat-inc", CategoryKind.INCOME, "Salary", null, archived = false)
    private val archivedCategory = Category("cat-arch", CategoryKind.EXPENSE, "Old", null, archived = true)

    private fun useCase(
        accounts: List<Account> = listOf(card, archived),
        categories: List<Category> = listOf(food, salary, archivedCategory),
        entries: FakeEntryRepository = FakeEntryRepository(),
    ): Pair<SaveEntryUseCase, FakeEntryRepository> =
        SaveEntryUseCase(
            FakeAccountRepository(accounts),
            FakeCategoryRepository(categories),
            entries,
        ) to entries

    @Test
    fun rejectsZeroAmount() = runTest {
        val (useCase, entries) = useCase()
        val result = useCase(input(amount = Money(0, eur)))
        assertEquals(Outcome.Failure(AppError.AMOUNT_INVALID), result)
        assertNull(entries.created)
    }

    @Test
    fun rejectsIncomeWithNegativeAmount() = runTest {
        val (useCase, _) = useCase()
        val result = useCase(input(type = EntryType.INCOME, amount = Money(-100, eur)))
        assertEquals(Outcome.Failure(AppError.AMOUNT_INVALID), result)
    }

    @Test
    fun rejectsExpenseWithPositiveAmount() = runTest {
        val (useCase, _) = useCase()
        val result = useCase(input(type = EntryType.EXPENSE, amount = Money(100, eur)))
        assertEquals(Outcome.Failure(AppError.AMOUNT_INVALID), result)
    }

    @Test
    fun rejectsBlankDescription() = runTest {
        val (useCase, _) = useCase()
        val result = useCase(input(description = "   "))
        assertEquals(Outcome.Failure(AppError.DESCRIPTION_REQUIRED), result)
    }

    @Test
    fun rejectsArchivedAccount() = runTest {
        val (useCase, _) = useCase()
        val result = useCase(input(accountId = "arch"))
        assertEquals(Outcome.Failure(AppError.ACCOUNT_ARCHIVED), result)
    }

    @Test
    fun rejectsCurrencyMismatch() = runTest {
        val (useCase, _) = useCase()
        val result = useCase(input(amount = Money(-100, usd)))
        assertEquals(Outcome.Failure(AppError.CURRENCY_MISMATCH), result)
    }

    @Test
    fun rejectsCategoryKindMismatch() = runTest {
        val (useCase, _) = useCase()
        val result = useCase(input(categoryId = "cat-inc"))
        assertEquals(Outcome.Failure(AppError.CATEGORY_KIND_MISMATCH), result)
    }

    @Test
    fun rejectsArchivedCategory() = runTest {
        val (useCase, _) = useCase()
        val result = useCase(input(categoryId = "cat-arch"))
        assertEquals(Outcome.Failure(AppError.CATEGORY_ARCHIVED), result)
    }

    @Test
    fun updateKeepsAnExistingArchivedCategory() = runTest {
        val existing = Entry(
            id = "entry-1",
            accountId = "acc",
            type = EntryType.EXPENSE,
            amount = Money(-100, eur),
            date = date,
            description = "Old",
            categoryId = "cat-arch",
        )
        val (useCase, _) = useCase(entries = FakeEntryRepository(existing))
        val result = useCase(input(id = "entry-1", categoryId = "cat-arch"))
        assertEquals(true, result is Outcome.Success)
    }

    @Test
    fun forwardsValidExpenseAndTrimsDescription() = runTest {
        val (useCase, entries) = useCase()
        val result = useCase(input(description = "  Lunch  "))
        assertEquals(true, result is Outcome.Success)
        assertNotNull(entries.created)
        assertEquals("Lunch", entries.created?.description)
        assertEquals(-1000L, entries.created?.amount?.amountMinor)
        assertEquals(EntryType.EXPENSE, entries.created?.type)
    }

    @Test
    fun updatePathCallsRepositoryUpdate() = runTest {
        val (useCase, entries) = useCase()
        val result = useCase(input(id = "entry-1"))
        assertEquals(true, result is Outcome.Success)
        assertEquals("entry-1", entries.updatedId)
        assertNull(entries.created)
    }

    private fun input(
        id: String? = null,
        type: EntryType = EntryType.EXPENSE,
        accountId: String = "acc",
        amount: Money = Money(-1000, eur),
        description: String = "Lunch",
        categoryId: String? = "cat-exp",
    ) = SaveEntryInput(
        id = id,
        accountId = accountId,
        type = type,
        amount = amount,
        date = date,
        description = description,
        categoryId = categoryId,
    )

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

    private class FakeCategoryRepository(
        private val categories: List<Category>,
    ) : CategoryRepository {
        private val byId = categories.associateBy { it.id }

        override fun observeCategories(): Flow<List<Category>> = flowOf(categories)
        override suspend fun get(id: String): Category? = byId[id]

        override suspend fun create(kind: CategoryKind, name: String, parentId: String?): Outcome<Category> =
            Outcome.Failure(AppError.UNKNOWN)

        override suspend fun rename(id: String, name: String): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun delete(id: String): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun seedDefaults(): Outcome<Unit> = Outcome.Success(Unit)
    }

    private class FakeEntryRepository(
        private val existingEntry: Entry? = null,
    ) : EntryRepository {
        var created: EntryInput? = null
        var updatedId: String? = null

        override fun observeForAccount(accountId: String): Flow<List<Entry>> = flowOf(emptyList())
        override fun observeAll(): Flow<List<Entry>> = flowOf(emptyList())
        override suspend fun get(id: String): Entry? = existingEntry?.takeIf { it.id == id }

        override suspend fun create(input: EntryInput): Outcome<Entry> {
            created = input
            return Outcome.Success(toEntry(input))
        }

        override suspend fun update(id: String, input: EntryInput): Outcome<Entry> {
            updatedId = id
            return Outcome.Success(toEntry(input))
        }

        override suspend fun delete(id: String): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun tagsForEntry(id: String): List<Tag> = emptyList()

        private fun toEntry(input: EntryInput) = Entry(
            id = "generated",
            accountId = input.accountId,
            type = input.type,
            amount = input.amount,
            date = input.date,
            description = input.description,
        )
    }
}
