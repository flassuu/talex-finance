package io.github.flassuu.talex.domain.repository

import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Account
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.model.Entry
import io.github.flassuu.talex.domain.model.EntryInput
import io.github.flassuu.talex.domain.model.Tag
import io.github.flassuu.talex.domain.model.Transfer
import io.github.flassuu.talex.domain.model.TransferInput
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>

    fun observeAccount(id: String): Flow<Account?>

    suspend fun get(id: String): Account?

    suspend fun createAccount(
        name: String,
        type: AccountType,
        currency: CurrencyCode,
        startingBalance: Money?,
        openingDate: LocalDate,
    ): Outcome<Account>

    suspend fun updateAccount(id: String, name: String, type: AccountType): Outcome<Unit>

    suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit>

    /** Hard delete is allowed only for an account that has no entry rows at all. */
    suspend fun deleteAccount(id: String): Outcome<Unit>
}

interface EntryRepository {
    fun observeForAccount(accountId: String): Flow<List<Entry>>

    fun observeAll(): Flow<List<Entry>>

    suspend fun get(id: String): Entry?

    suspend fun create(input: EntryInput): Outcome<Entry>

    suspend fun update(id: String, input: EntryInput): Outcome<Entry>

    suspend fun delete(id: String): Outcome<Unit>

    suspend fun tagsForEntry(id: String): List<Tag>
}

interface TransferRepository {
    fun observeTransfers(): Flow<List<Transfer>>

    suspend fun get(id: String): Transfer?

    suspend fun create(input: TransferInput): Outcome<Transfer>

    suspend fun update(id: String, input: TransferInput): Outcome<Transfer>

    suspend fun delete(id: String): Outcome<Unit>
}

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>

    suspend fun get(id: String): Category?

    suspend fun create(kind: CategoryKind, name: String, parentId: String? = null): Outcome<Category>

    suspend fun rename(id: String, name: String): Outcome<Unit>

    suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit>

    suspend fun delete(id: String): Outcome<Unit>

    /** Idempotent seeding of the default categories, guarded by a settings flag. */
    suspend fun seedDefaults(): Outcome<Unit>
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
