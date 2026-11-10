package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CreateAccountUseCaseTest {

    private val date = LocalDate.of(2026, 1, 1)
    private val eur = CurrencyCode.EUR
    private val usd = CurrencyCode.USD
    private val stubAccount = Account(
        id = "id",
        name = "Card",
        type = AccountType.CHECKING,
        currency = eur,
        archived = false,
        balance = Money(50_000, eur),
    )

    @Test
    fun rejectsBlankName() = runTest {
        val repository = FakeAccountRepository(Outcome.Success(stubAccount))
        val result = CreateAccountUseCase(repository)(
            CreateAccountInput("   ", AccountType.CHECKING, eur, null, date),
        )
        assertEquals(Outcome.Failure(AppError.NAME_REQUIRED), result)
        assertNull(repository.lastInput)
    }

    @Test
    fun rejectsNameThatIsTooLong() = runTest {
        val repository = FakeAccountRepository(Outcome.Success(stubAccount))
        val result = CreateAccountUseCase(repository)(
            CreateAccountInput("x".repeat(CreateAccountUseCase.MAX_NAME_LENGTH + 1), AccountType.CHECKING, eur, null, date),
        )
        assertEquals(Outcome.Failure(AppError.NAME_TOO_LONG), result)
        assertNull(repository.lastInput)
    }

    @Test
    fun rejectsCurrencyMismatchBetweenAccountAndBalance() = runTest {
        val repository = FakeAccountRepository(Outcome.Success(stubAccount))
        val result = CreateAccountUseCase(repository)(
            CreateAccountInput("Card", AccountType.CHECKING, eur, Money(100, usd), date),
        )
        assertEquals(Outcome.Failure(AppError.AMOUNT_INVALID), result)
        assertNull(repository.lastInput)
    }

    @Test
    fun trimsNameAndForwardsValidInput() = runTest {
        val repository = FakeAccountRepository(Outcome.Success(stubAccount))
        val result = CreateAccountUseCase(repository)(
            CreateAccountInput(" Card ", AccountType.CHECKING, eur, Money(50_000, eur), date),
        )
        assertTrue(result is Outcome.Success)
        assertEquals("Card", repository.lastInput?.name)
        assertEquals(date, repository.lastInput?.date)
    }

    private class FakeAccountRepository(
        private val result: Outcome<Account>,
    ) : AccountRepository {

        var lastInput: CreateAccountInput? = null

        override fun observeAccounts(): Flow<List<Account>> = flowOf(emptyList())

        override suspend fun createAccount(
            name: String,
            type: AccountType,
            currency: CurrencyCode,
            startingBalance: Money?,
            openingDate: LocalDate,
        ): Outcome<Account> {
            lastInput = CreateAccountInput(name, type, currency, startingBalance, openingDate)
            return result
        }
    }
}
