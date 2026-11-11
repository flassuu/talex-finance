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
    val payee: String? = null,
    val notes: String? = null,
    val categoryId: String? = null,
    val deletedAt: Long? = null,
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
)

data class Tag(
    val id: String,
    val name: String,
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
)
