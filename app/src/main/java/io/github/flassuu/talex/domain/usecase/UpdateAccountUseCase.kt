package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.repository.AccountRepository
import javax.inject.Inject

data class UpdateAccountInput(
    val id: String,
    val name: String,
    val type: AccountType,
)

class UpdateAccountUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    suspend operator fun invoke(input: UpdateAccountInput): Outcome<Unit> {
        val name = input.name.trim()
        if (name.isEmpty()) return Outcome.Failure(AppError.NAME_REQUIRED)
        if (name.length > MAX_NAME_LENGTH) return Outcome.Failure(AppError.NAME_TOO_LONG)
        return repository.updateAccount(input.id, name, input.type)
    }

    companion object {
        const val MAX_NAME_LENGTH = 80
    }
}
