package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.GoalContributionInput
import io.github.flassuu.talex.domain.model.GoalInput
import io.github.flassuu.talex.domain.model.ValuationInput
import io.github.flassuu.talex.domain.repository.AccountRepository
import io.github.flassuu.talex.domain.repository.BudgetRepository
import io.github.flassuu.talex.domain.repository.CategoryRepository
import io.github.flassuu.talex.domain.repository.GoalRepository
import io.github.flassuu.talex.domain.repository.ValuationRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Seeds a browsable example profile (ADR-0031). The data is synthetic and stays
 * in the demo database; every rule goes through the normal use cases.
 */
class SeedDemoDataUseCase @Inject constructor(
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val saveEntry: SaveEntryUseCase,
    private val budgets: BudgetRepository,
    private val goals: GoalRepository,
    private val valuations: ValuationRepository,
) {

    suspend operator fun invoke(today: LocalDate = LocalDate.now()): Outcome<Unit> {
        val currency = CurrencyCode.EUR
        categories.seedDefaults()

        val checking = createAccount("Checking", AccountType.CHECKING, currency, 245_000, today)
            ?: return Outcome.Failure(AppError.UNKNOWN)
        createAccount("Savings", AccountType.SAVINGS, currency, 600_000, today)
            ?: return Outcome.Failure(AppError.UNKNOWN)
        val cash = createAccount("Cash", AccountType.CASH, currency, 12_000, today)
            ?: return Outcome.Failure(AppError.UNKNOWN)
        createAccount("Investments", AccountType.INVESTMENT, currency, 1_200_000, today)
            ?: return Outcome.Failure(AppError.UNKNOWN)
        createAccount("Car loan", AccountType.LOAN, currency, -450_000, today)
            ?: return Outcome.Failure(AppError.UNKNOWN)

        val byKey = categories.observeCategories().first().associateBy { it.stableKey }

        fun expense(account: Account, amount: Long, date: LocalDate, key: String, title: String) =
            SaveEntryInput(
                accountId = account.id,
                type = EntryType.EXPENSE,
                amount = Money(-amount, currency),
                date = date,
                description = title,
                categoryId = byKey[key]?.id,
            )

        fun income(amount: Long, date: LocalDate) = SaveEntryInput(
            accountId = checking.id,
            type = EntryType.INCOME,
            amount = Money(amount, currency),
            date = date,
            description = "Salary",
            categoryId = byKey["salary"]?.id,
        )

        val entries = buildList {
            for (monthsAgo in 3L downTo 1L) {
                val monthStart = today.minusMonths(monthsAgo).withDayOfMonth(1)
                add(income(280_000, monthStart))
                add(expense(checking, 95_000, monthStart, "housing", "Rent"))
                add(expense(checking, 1_299, monthStart.plusDays(14), "entertainment", "Streaming"))
            }
            add(expense(checking, 8_640, today.minusDays(2), "food_groceries", "Groceries"))
            add(expense(checking, 11_230, today.minusDays(9), "food_groceries", "Groceries"))
            add(expense(checking, 6_475, today.minusDays(16), "food_groceries", "Groceries"))
            add(expense(checking, 9_810, today.minusDays(23), "food_groceries", "Groceries"))
            add(expense(checking, 4_200, today.minusDays(5), "food_restaurants", "Dinner"))
            add(expense(checking, 6_850, today.minusDays(12), "food_restaurants", "Lunch"))
            add(expense(cash, 5_800, today.minusDays(3), "transport", "Fuel"))
            add(expense(cash, 3_320, today.minusDays(17), "transport", "Public transport"))
            add(expense(checking, 3_500, today.minusDays(20), "health", "Pharmacy"))
        }
        for (entry in entries) {
            if (saveEntry(entry) is Outcome.Failure) {
                return Outcome.Failure(AppError.UNKNOWN)
            }
        }

        byKey["food_groceries"]?.let { budgets.setLimit(it.id, Money(35_000, currency), today) }
        byKey["food_restaurants"]?.let { budgets.setLimit(it.id, Money(12_000, currency), today) }
        byKey["transport"]?.let { budgets.setLimit(it.id, Money(10_000, currency), today) }

        val goal = goals.create(
            GoalInput(
                name = "Holiday",
                targetAmount = Money(200_000, currency),
                targetDate = today.plusMonths(8),
            ),
        )
        if (goal is Outcome.Success) {
            goals.addContribution(
                goal.value.id,
                GoalContributionInput(Money(30_000, currency), today.minusMonths(2)),
            )
            goals.addContribution(
                goal.value.id,
                GoalContributionInput(Money(20_000, currency), today.minusMonths(1)),
            )
        }

        val property = createAccount("Apartment", AccountType.PROPERTY, currency, null, today)
            ?: return Outcome.Failure(AppError.UNKNOWN)
        valuations.add(property.id, ValuationInput(Money(18_000_000, currency), today))

        return Outcome.Success(Unit)
    }

    private suspend fun createAccount(
        name: String,
        type: AccountType,
        currency: CurrencyCode,
        startingBalanceMinor: Long?,
        today: LocalDate,
    ): Account? {
        val balance = startingBalanceMinor?.let { Money(it, currency) }
        return when (val result = accounts.createAccount(name, type, currency, balance, today)) {
            is Outcome.Success -> result.value
            is Outcome.Failure -> null
        }
    }
}
