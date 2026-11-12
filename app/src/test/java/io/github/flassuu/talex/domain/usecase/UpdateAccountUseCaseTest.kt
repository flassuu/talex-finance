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
import org.junit.Test
import java.time.LocalDate

class UpdateAccountUseCaseTest {

    @Test
    fun rejectsBlankName() = runTest {
        val repository = FakeAccountRepository()
        val result = UpdateAccountUseCase(repository)(UpdateAccountInput("id", "   ", AccountType.CASH))
        assertEquals(Outcome.Failure(AppError.NAME_REQUIRED), result)
        assertNull(repository.lastUpdate)
    }

    @Test
    fun rejectsNameThatIsTooLong() = runTest {
        val repository = FakeAccountRepository()
        val result = UpdateAccountUseCase(repository)(
            UpdateAccountInput("id", "x".repeat(UpdateAccountUseCase.MAX_NAME_LENGTH + 1), AccountType.CASH),
        )
        assertEquals(Outcome.Failure(AppError.NAME_TOO_LONG), result)
        assertNull(repository.lastUpdate)
    }

    @Test
    fun trimsNameAndForwardsTheType() = runTest {
        val repository = FakeAccountRepository()
        val result = UpdateAccountUseCase(repository)(
            UpdateAccountInput("id", "  Wallet  ", AccountType.CASH),
        )
        assertEquals(Outcome.Success(Unit), result)
        assertEquals("Wallet", repository.lastUpdate?.name)
        assertEquals(AccountType.CASH, repository.lastUpdate?.type)
    }

    @Test
    fun rejectsSwitchingBetweenEntryAndValuationBasis() = runTest {
        val repository = FakeAccountRepository()
        val result = UpdateAccountUseCase(repository)(
            UpdateAccountInput("id", "Home", AccountType.PROPERTY),
        )
        assertEquals(Outcome.Failure(AppError.ACCOUNT_TYPE_INVALID), result)
        assertNull(repository.lastUpdate)
    }

    private class FakeAccountRepository : AccountRepository {

        data class Update(val name: String, val type: AccountType)

        var lastUpdate: Update? = null

        override fun observeAccounts(): Flow<List<Account>> = flowOf(emptyList())

        override fun observeAccount(id: String): Flow<Account?> = flowOf(null)

        override suspend fun get(id: String): Account? =
            Account(id, "Wallet", AccountType.CHECKING, CurrencyCode.EUR, archived = false, balance = Money.zero(CurrencyCode.EUR))

        override suspend fun createAccount(
            name: String,
            type: AccountType,
            currency: CurrencyCode,
            startingBalance: Money?,
            openingDate: LocalDate,
        ): Outcome<Account> = Outcome.Failure(AppError.UNKNOWN)

        override suspend fun updateAccount(id: String, name: String, type: AccountType): Outcome<Unit> {
            lastUpdate = Update(name, type)
            return Outcome.Success(Unit)
        }

        override suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit> =
            Outcome.Success(Unit)

        override suspend fun deleteAccount(id: String): Outcome<Unit> =
            Outcome.Success(Unit)
    }
}
