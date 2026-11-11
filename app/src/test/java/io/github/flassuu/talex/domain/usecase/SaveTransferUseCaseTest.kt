package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Transfer
import io.github.flassuu.talex.domain.model.TransferInput
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class SaveTransferUseCaseTest {

    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode.USD
    private val date = LocalDate.of(2026, 11, 11)

    private val checking = Account("a", "Checking", AccountType.CHECKING, eur, archived = false, balance = Money(0, eur))
    private val savings = Account("b", "Savings", AccountType.SAVINGS, eur, archived = false, balance = Money(0, eur))
    private val dollars = Account("c", "USD", AccountType.CHECKING, usd, archived = false, balance = Money(0, usd))
    private val archived = Account("d", "Old", AccountType.CASH, eur, archived = true, balance = Money(0, eur))

    private fun useCase(
        accounts: List<Account> = listOf(checking, savings, dollars, archived),
        transfers: FakeTransferRepository = FakeTransferRepository(),
    ): Pair<SaveTransferUseCase, FakeTransferRepository> =
        SaveTransferUseCase(FakeAccountRepository(accounts), transfers) to transfers

    @Test
    fun rejectsSameAccount() = runTest {
        val (useCase, transfers) = useCase()
        val result = useCase(input(source = "a", destination = "a"))
        assertEquals(Outcome.Failure(AppError.SAME_ACCOUNT_TRANSFER), result)
        assertNull(transfers.created)
    }

    @Test
    fun rejectsZeroAmount() = runTest {
        val (useCase, _) = useCase()
        val result = useCase(input(sourceAmount = Money(0, eur), destinationAmount = Money(0, eur)))
        assertEquals(Outcome.Failure(AppError.AMOUNT_INVALID), result)
    }

    @Test
    fun rejectsArchivedAccount() = runTest {
        val (useCase, _) = useCase()
        val result = useCase(input(source = "a", destination = "d", destinationAmount = Money(100, eur)))
        assertEquals(Outcome.Failure(AppError.ACCOUNT_ARCHIVED), result)
    }

    @Test
    fun rejectsSourceCurrencyMismatch() = runTest {
        val (useCase, _) = useCase()
        val result = useCase(input(sourceAmount = Money(100, usd)))
        assertEquals(Outcome.Failure(AppError.CURRENCY_MISMATCH), result)
    }

    @Test
    fun rejectsSameCurrencyWithDifferentAmounts() = runTest {
        val (useCase, _) = useCase()
        val result = useCase(input(source = "a", destination = "b", sourceAmount = Money(100, eur), destinationAmount = Money(90, eur)))
        assertEquals(Outcome.Failure(AppError.TRANSFER_AMOUNT_MISMATCH), result)
    }

    @Test
    fun createsSameCurrencyTransfer() = runTest {
        val (useCase, transfers) = useCase()
        val result = useCase(input(source = "a", destination = "b", sourceAmount = Money(100, eur), destinationAmount = Money(100, eur)))
        assertEquals(true, result is Outcome.Success)
        assertEquals(Money(100, eur), transfers.created?.sourceAmount)
        assertEquals(Money(100, eur), transfers.created?.destinationAmount)
    }

    @Test
    fun createsCrossCurrencyTransfer() = runTest {
        val (useCase, transfers) = useCase()
        val result = useCase(
            input(
                source = "a",
                destination = "c",
                sourceAmount = Money(100, eur),
                destinationAmount = Money(110, usd),
            ),
        )
        assertEquals(true, result is Outcome.Success)
        assertEquals(Money(100, eur), transfers.created?.sourceAmount)
        assertEquals(Money(110, usd), transfers.created?.destinationAmount)
    }

    @Test
    fun updatePathCallsRepositoryUpdate() = runTest {
        val (useCase, transfers) = useCase()
        val result = useCase(input(id = "g", source = "a", destination = "b"))
        assertEquals(true, result is Outcome.Success)
        assertEquals("g", transfers.updatedId)
        assertNull(transfers.created)
    }

    private fun input(
        id: String? = null,
        source: String = "a",
        destination: String = "b",
        sourceAmount: Money = Money(100, eur),
        destinationAmount: Money = Money(100, eur),
    ) = SaveTransferInput(
        id = id,
        sourceAccountId = source,
        destinationAccountId = destination,
        sourceAmount = sourceAmount,
        destinationAmount = destinationAmount,
        date = date,
        description = "Move",
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

    private class FakeTransferRepository : TransferRepository {
        var created: TransferInput? = null
        var updatedId: String? = null

        override fun observeTransfers(): Flow<List<Transfer>> = flowOf(emptyList())
        override suspend fun get(id: String): Transfer? = null

        override suspend fun create(input: TransferInput): Outcome<Transfer> {
            created = input
            return Outcome.Success(toTransfer(input))
        }

        override suspend fun update(id: String, input: TransferInput): Outcome<Transfer> {
            updatedId = id
            return Outcome.Success(toTransfer(input))
        }

        override suspend fun delete(id: String): Outcome<Unit> = Outcome.Success(Unit)

        private fun toTransfer(input: TransferInput) = Transfer(
            id = "g",
            sourceAccountId = input.sourceAccountId,
            destinationAccountId = input.destinationAccountId,
            sourceAmount = input.sourceAmount,
            destinationAmount = input.destinationAmount,
            rate = BigDecimal.ONE,
            date = input.date,
            description = input.description,
        )
    }
}
