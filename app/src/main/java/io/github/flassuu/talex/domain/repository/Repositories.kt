package io.github.flassuu.talex.domain.repository

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>

    suspend fun createAccount(
        name: String,
        type: AccountType,
        currency: CurrencyCode,
        startingBalance: Money?,
        openingDate: LocalDate,
    ): Outcome<Account>
}

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun create(kind: CategoryKind, name: String, parentId: String? = null): Outcome<Category>
}

interface TagRepository {
    fun observeTags(): Flow<List<Tag>>
    suspend fun findOrCreate(name: String): Outcome<Tag>
}

interface SettingsRepository {
    fun observeBaseCurrency(): Flow<CurrencyCode?>
    suspend fun getBaseCurrency(): CurrencyCode?
    suspend fun setBaseCurrency(code: CurrencyCode)
}
