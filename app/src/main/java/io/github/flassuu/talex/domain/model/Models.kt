package io.github.flassuu.talex.domain.model

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import java.time.LocalDate

enum class AccountType {
    CHECKING,
    SAVINGS,
    CASH,
    CREDIT_CARD,
    INVESTMENT,
    LOAN,
    OTHER,
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
    OPENING,
}

data class Entry(
    val id: String,
    val accountId: String,
    val type: EntryType,
    val amount: Money,
    val date: LocalDate,
    val description: String,
    val categoryId: String?,
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
)

data class Tag(
    val id: String,
    val name: String,
)
