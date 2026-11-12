package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Transfer
import io.github.flassuu.talex.domain.model.TransferInput
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.TransferRepository
import java.time.LocalDate
import javax.inject.Inject

data class SaveTransferInput(
    val id: String? = null,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val sourceAmount: Money,
    val destinationAmount: Money,
    val date: LocalDate,
    val description: String = "",
)

/**
 * Validates a transfer pair (ADR-0013): two different active accounts, positive
 * magnitudes, matching currencies, and equal amounts for a same-currency transfer.
 */
class SaveTransferUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository,
) {
    suspend operator fun invoke(input: SaveTransferInput): Outcome<Transfer> {
        if (input.sourceAccountId == input.destinationAccountId) {
            return Outcome.Failure(AppError.SAME_ACCOUNT_TRANSFER)
        }
        if (input.sourceAmount.isZero || input.sourceAmount.isNegative) {
            return Outcome.Failure(AppError.AMOUNT_INVALID)
        }
        if (input.destinationAmount.isZero || input.destinationAmount.isNegative) {
            return Outcome.Failure(AppError.AMOUNT_INVALID)
        }

        val source = accountRepository.get(input.sourceAccountId)
            ?: return Outcome.Failure(AppError.ACCOUNT_NOT_FOUND)
        val destination = accountRepository.get(input.destinationAccountId)
            ?: return Outcome.Failure(AppError.ACCOUNT_NOT_FOUND)
        if (source.archived || destination.archived) {
            return Outcome.Failure(AppError.ACCOUNT_ARCHIVED)
        }
        if (source.type.isValuationBased || destination.type.isValuationBased) {
            return Outcome.Failure(AppError.ACCOUNT_TYPE_INVALID)
        }
        if (source.currency != input.sourceAmount.currency) {
            return Outcome.Failure(AppError.CURRENCY_MISMATCH)
        }
        if (destination.currency != input.destinationAmount.currency) {
            return Outcome.Failure(AppError.CURRENCY_MISMATCH)
        }
        if (source.currency == destination.currency &&
            input.sourceAmount.amountMinor != input.destinationAmount.amountMinor
        ) {
            return Outcome.Failure(AppError.TRANSFER_AMOUNT_MISMATCH)
        }
        if (input.description.trim().length > MAX_DESCRIPTION_LENGTH) {
            return Outcome.Failure(AppError.DESCRIPTION_TOO_LONG)
        }

        val transferInput = TransferInput(
            sourceAccountId = input.sourceAccountId,
            destinationAccountId = input.destinationAccountId,
            sourceAmount = input.sourceAmount,
            destinationAmount = input.destinationAmount,
            date = input.date,
            description = input.description,
        )
        return if (input.id == null) {
            transferRepository.create(transferInput)
        } else {
            transferRepository.update(input.id, transferInput)
        }
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 120
    }
}
