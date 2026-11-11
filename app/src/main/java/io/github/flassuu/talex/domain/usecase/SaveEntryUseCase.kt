package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryInput
import io.github.flassuu.talex.domain.model.EntryLineInput
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import java.time.LocalDate
import javax.inject.Inject

data class SaveEntryInput(
    val id: String? = null,
    val accountId: String,
    val type: EntryType,
    val amount: Money,
    val date: LocalDate,
    val description: String,
    val payee: String? = null,
    val notes: String? = null,
    val categoryId: String? = null,
    val tagNames: List<String> = emptyList(),
    val lines: List<EntryLineInput> = emptyList(),
)

/**
 * Validates the entry against the ledger rules (docs/technical/ledger.md) before
 * writing. Editing revalidates the resulting state, not only the changed fields.
 */
class SaveEntryUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke(input: SaveEntryInput): Outcome<Entry> {
        if (input.type == EntryType.OPENING ||
            input.type == EntryType.TRANSFER ||
            input.type == EntryType.ADJUSTMENT
        ) {
            return Outcome.Failure(AppError.ENTRY_NOT_FOUND)
        }
        if (input.amount.isZero) return Outcome.Failure(AppError.AMOUNT_INVALID)
        if (input.type == EntryType.INCOME && input.amount.isNegative) {
            return Outcome.Failure(AppError.AMOUNT_INVALID)
        }
        if (input.type == EntryType.EXPENSE && !input.amount.isNegative) {
            return Outcome.Failure(AppError.AMOUNT_INVALID)
        }

        val description = input.description.trim()
        if (description.length > MAX_DESCRIPTION_LENGTH) {
            return Outcome.Failure(AppError.DESCRIPTION_TOO_LONG)
        }

        val account = accountRepository.get(input.accountId)
            ?: return Outcome.Failure(AppError.ACCOUNT_NOT_FOUND)
        if (account.archived) return Outcome.Failure(AppError.ACCOUNT_ARCHIVED)
        if (account.currency != input.amount.currency) {
            return Outcome.Failure(AppError.CURRENCY_MISMATCH)
        }

        val expectedKind = if (input.type == EntryType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE

        if (input.lines.isNotEmpty()) {
            val splitError = validateSplit(input, expectedKind)
            if (splitError != null) return Outcome.Failure(splitError)
        } else if (input.categoryId != null) {
            val category = categoryRepository.get(input.categoryId)
                ?: return Outcome.Failure(AppError.CATEGORY_NOT_FOUND)
            // An entry may keep an archived category it already had; a different archived
            // category cannot be chosen.
            val existingCategoryId = if (input.id != null) entryRepository.get(input.id)?.categoryId else null
            if (category.archived && input.categoryId != existingCategoryId) {
                return Outcome.Failure(AppError.CATEGORY_ARCHIVED)
            }
            if (category.kind != expectedKind) return Outcome.Failure(AppError.CATEGORY_KIND_MISMATCH)
        }

        val entryInput = EntryInput(
            accountId = input.accountId,
            type = input.type,
            amount = input.amount,
            date = input.date,
            description = description,
            payee = input.payee?.trim()?.takeIf { it.isNotEmpty() },
            notes = input.notes?.trim()?.takeIf { it.isNotEmpty() },
            categoryId = if (input.lines.isEmpty()) input.categoryId else null,
            tagNames = input.tagNames,
            lines = input.lines,
        )
        return if (input.id == null) {
            entryRepository.create(entryInput)
        } else {
            entryRepository.update(input.id, entryInput)
        }
    }

    private suspend fun validateSplit(
        input: SaveEntryInput,
        expectedKind: CategoryKind,
    ): AppError? {
        if (input.categoryId != null) return AppError.SPLIT_INVALID
        if (input.lines.size < 2) return AppError.SPLIT_INVALID
        var sum = 0L
        val topLevelIds = mutableSetOf<String>()
        for (line in input.lines) {
            if (line.amount.isZero) return AppError.AMOUNT_INVALID
            if (input.type == EntryType.INCOME && line.amount.isNegative) return AppError.AMOUNT_INVALID
            if (input.type == EntryType.EXPENSE && !line.amount.isNegative) return AppError.AMOUNT_INVALID
            val category = categoryRepository.get(line.categoryId)
                ?: return AppError.CATEGORY_NOT_FOUND
            if (category.archived) return AppError.CATEGORY_ARCHIVED
            if (category.kind != expectedKind) return AppError.CATEGORY_KIND_MISMATCH
            topLevelIds.add(category.parentId ?: category.id)
            sum += line.amount.amountMinor
        }
        if (topLevelIds.size != 1) return AppError.SPLIT_SAME_PARENT
        if (sum != input.amount.amountMinor) return AppError.SPLIT_SUM_MISMATCH
        return null
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 120
    }
}
