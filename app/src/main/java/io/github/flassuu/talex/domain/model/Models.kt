package io.github.flassuu.talex.domain.model

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import java.math.BigDecimal
import java.time.LocalDate

enum class AccountType {
    CHECKING,
    SAVINGS,
    CASH,
    CREDIT_CARD,
    INVESTMENT,
    LOAN,
    OTHER,
    PROPERTY,
    VEHICLE,
    ASSET,
    ;

    /**
     * A valuation-based account takes its balance from the latest dated valuation
     * instead of the sum of ledger entries (ADR-0023).
     */
    val isValuationBased: Boolean
        get() = this == PROPERTY || this == VEHICLE || this == ASSET
}

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val currency: CurrencyCode,
    val archived: Boolean,
    val balance: Money,
)

enum class EntryType {
    INCOME,
    EXPENSE,
    TRANSFER,
    OPENING,
    ADJUSTMENT,
}

data class Entry(
    val id: String,
    val accountId: String,
    val type: EntryType,
    val amount: Money,
    val date: LocalDate,
    val description: String,
    val payee: String? = null,
    val notes: String? = null,
    val categoryId: String? = null,
    val transferGroupId: String? = null,
    val sourceRuleId: String? = null,
    val lines: List<EntryLine> = emptyList(),
    val deletedAt: Long? = null,
)

/** One category share of a split entry. */
data class EntryLine(
    val id: String,
    val categoryId: String,
    val amount: Money,
    val note: String? = null,
)

data class EntryLineInput(
    val categoryId: String,
    val amount: Money,
    val note: String? = null,
)

enum class Frequency {
    WEEKLY,
    MONTHLY,
    YEARLY,
}

data class RecurringRule(
    val id: String,
    val accountId: String,
    val type: EntryType,
    val amount: Money,
    val description: String,
    val categoryId: String?,
    val frequency: Frequency,
    val anchorDate: LocalDate,
    val nextDueDate: LocalDate,
    val active: Boolean,
)

data class RecurringInput(
    val accountId: String,
    val type: EntryType,
    val amount: Money,
    val description: String,
    val categoryId: String?,
    val frequency: Frequency,
    val anchorDate: LocalDate,
    val active: Boolean = true,
)

enum class CategoryKind {
    INCOME,
    EXPENSE,
}

data class Category(
    val id: String,
    val kind: CategoryKind,
    val name: String,
    val parentId: String?,
    val archived: Boolean,
    /** Non-null for seeded defaults; used for idempotent seeding and translation. */
    val stableKey: String? = null,
    /** When true, spending in this category counts as lost money (ADR-0024). */
    val unnecessary: Boolean = false,
)

data class Tag(
    val id: String,
    val name: String,
)

/** A linked pair of entries that move money between two accounts (ADR-0013). */
data class Transfer(
    val id: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val sourceAmount: Money,
    val destinationAmount: Money,
    val rate: BigDecimal,
    val date: LocalDate,
    val description: String,
)

data class TransferInput(
    val sourceAccountId: String,
    val destinationAccountId: String,
    val sourceAmount: Money,
    val destinationAmount: Money,
    val date: LocalDate,
    val description: String,
)

/** A validated, ready-to-write entry. The use case fills it after checking account and category. */
data class EntryInput(
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
    val sourceRuleId: String? = null,
    val schedulePeriod: String? = null,
)

/** One dated value of a valuation-based account, in that account's currency (ADR-0023). */
data class Valuation(
    val id: String,
    val accountId: String,
    val amount: Money,
    val date: LocalDate,
)

data class ValuationInput(
    val amount: Money,
    val date: LocalDate,
)

/** A stored exchange rate for one pair and date (D-030). */
data class Rate(
    val base: CurrencyCode,
    val quote: CurrencyCode,
    val date: LocalDate,
    val rate: BigDecimal,
    val source: String,
)

/**
 * One effective-dated budget limit for an expense category (ADR-0010). The limit
 * is in the base currency. `closedAt` marks the day the budget was removed.
 */
data class Budget(
    val id: String,
    val categoryId: String,
    val amount: Money,
    val effectiveFrom: LocalDate,
    val closedAt: LocalDate? = null,
)

/** A savings goal; the current amount is the sum of its contributions (ADR-0017, ADR-0026). */
data class Goal(
    val id: String,
    val name: String,
    val targetAmount: Money,
    val targetDate: LocalDate?,
    val currency: CurrencyCode,
    val currentAmount: Money,
    val contributions: List<GoalContribution> = emptyList(),
)

data class GoalContribution(
    val id: String,
    val goalId: String,
    val amount: Money,
    val date: LocalDate,
    val note: String? = null,
)

data class GoalInput(
    val name: String,
    val targetAmount: Money,
    val targetDate: LocalDate?,
)

data class GoalContributionInput(
    val amount: Money,
    val date: LocalDate,
    val note: String? = null,
)
