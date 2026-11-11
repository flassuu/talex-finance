package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryInput
import io.github.flassuu.talex.domain.model.RecurringRule
import io.github.flassuu.talex.domain.model.RecurringSchedule
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.EntryRepository
import io.github.flassuu.talex.domain.repository.RecurringRepository
import java.time.LocalDate
import javax.inject.Inject

data class RecurringGeneration(
    val created: List<Entry>,
    val skipped: List<RecurringRule>,
)

/**
 * Catch-up generation on unlock (ADR-0009). Every due occurrence up to today is
 * created and dated on its scheduled day. The unique (rule, period) pair makes a
 * second run idempotent. Paused rules are skipped; archived accounts or categories
 * are reported and retried on the next unlock.
 */
class GenerateRecurringUseCase @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val entryRepository: EntryRepository,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now()): RecurringGeneration {
        val created = mutableListOf<Entry>()
        val skipped = mutableListOf<RecurringRule>()

        recurringRepository.getAll().forEach { rule ->
            if (!rule.active) return@forEach

            val account = accountRepository.get(rule.accountId)
            if (account == null || account.archived) {
                skipped.add(rule)
                return@forEach
            }
            if (rule.categoryId != null) {
                val category = categoryRepository.get(rule.categoryId)
                if (category == null || category.archived) {
                    skipped.add(rule)
                    return@forEach
                }
            }

            var due = rule.nextDueDate
            while (!due.isAfter(today)) {
                val period = due.toString()
                if (!entryRepository.existsForRulePeriod(rule.id, period)) {
                    val result = entryRepository.create(
                        EntryInput(
                            accountId = rule.accountId,
                            type = rule.type,
                            amount = rule.amount,
                            date = due,
                            description = rule.description,
                            categoryId = rule.categoryId,
                            sourceRuleId = rule.id,
                            schedulePeriod = period,
                        ),
                    )
                    if (result is Outcome.Success) {
                        created.add(result.value)
                    }
                }
                due = RecurringSchedule.firstOnOrAfter(
                    rule.anchorDate,
                    rule.frequency,
                    due.plusDays(1),
                )
            }
            recurringRepository.updateNextDueDate(rule.id, due)
        }

        return RecurringGeneration(created, skipped)
    }
}
