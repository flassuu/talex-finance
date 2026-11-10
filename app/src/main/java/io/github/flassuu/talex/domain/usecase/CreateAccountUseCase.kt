package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.repository.AccountRepository
import java.time.LocalDate
import javax.inject.Inject

data class CreateAccountInput(
    val name: String,
    val type: AccountType,
    val currency: CurrencyCode,
    val startingBalance: Money?,
    val date: LocalDate,
)

class CreateAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(input: CreateAccountInput): Outcome<Account> {
        val name = input.name.trim()
        if (name.isEmpty()) return Outcome.Failure(AppError.NAME_REQUIRED)
        if (name.length > MAX_NAME_LENGTH) return Outcome.Failure(AppError.NAME_TOO_LONG)
        if (input.startingBalance != null && input.startingBalance.currency != input.currency) {
            return Outcome.Failure(AppError.AMOUNT_INVALID)
        }
        return repository.createAccount(
            name = name,
            type = input.type,
            currency = input.currency,
            startingBalance = input.startingBalance,
            openingDate = input.date,
        )
    }

    companion object {
        const val MAX_NAME_LENGTH = 80
    }
}
