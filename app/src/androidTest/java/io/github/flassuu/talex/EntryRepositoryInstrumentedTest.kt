package io.github.flassuu.talex

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.data.db.AccountEntity
import io.github.flassuu.talex.data.db.CategoryEntity
import io.github.flassuu.talex.data.db.DatabaseProvider
import io.github.flassuu.talex.data.db.EntryEntity
import io.github.flassuu.talex.data.db.EntryTagEntity
import io.github.flassuu.talex.data.db.TagEntity
import io.github.flassuu.talex.data.db.TalexDatabase
import io.github.flassuu.talex.data.repository.AccountRepositoryImpl
import io.github.flassuu.talex.data.repository.CategoryRepositoryImpl
import io.github.flassuu.talex.data.repository.EntryRepositoryImpl
import io.github.flassuu.talex.domain.model.EntryLineInput
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.usecase.SaveEntryInput
import io.github.flassuu.talex.domain.usecase.SaveEntryUseCase
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class EntryRepositoryInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "phase2-entry-test.db"
    private val key = ByteArray(32) { (it + 1).toByte() }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
        context.deleteDatabase("$databaseName-wal")
        context.deleteDatabase("$databaseName-shm")
    }

    @Test
    fun saveEntryUseCaseCreatesExpenseWithCategoryAndTags() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        seed(database)

        val provider = DatabaseProvider().apply { set(database) }
        val useCase = SaveEntryUseCase(
            AccountRepositoryImpl(provider),
            CategoryRepositoryImpl(provider),
            EntryRepositoryImpl(provider),
        )

        val result = useCase(
            SaveEntryInput(
                accountId = "acc",
                type = EntryType.EXPENSE,
                amount = Money(-1250, CurrencyCode.EUR),
                date = LocalDate.of(2026, 11, 11),
                description = "Lunch",
                categoryId = "cat",
                tagNames = listOf("work", "Work"),
            ),
        )

        assertTrue("save failed: $result", result is Outcome.Success)
        val entries = database.entryDao().observeAll()
        assertNotNull(entries)
        database.close()
    }

    @Test
    fun savesSplitLinesAtomically() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        seed(database)
        database.categoryDao().insert(
            CategoryEntity(
                id = "cat-child",
                kind = "EXPENSE",
                name = "Groceries",
                parentId = "cat",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        val provider = DatabaseProvider().apply { set(database) }
        val useCase = SaveEntryUseCase(
            AccountRepositoryImpl(provider),
            CategoryRepositoryImpl(provider),
            EntryRepositoryImpl(provider),
        )

        val result = useCase(
            SaveEntryInput(
                accountId = "acc",
                type = EntryType.EXPENSE,
                amount = Money(-1250, CurrencyCode.EUR),
                date = LocalDate.of(2026, 11, 11),
                description = "Split lunch",
                categoryId = null,
                lines = listOf(
                    EntryLineInput("cat", Money(-750, CurrencyCode.EUR)),
                    EntryLineInput("cat-child", Money(-500, CurrencyCode.EUR)),
                ),
            ),
        )

        assertTrue("split failed: $result", result is Outcome.Success)
        val entry = (result as Outcome.Success).value
        val lines = database.entryLineDao().getForEntry(entry.id)
        assertEquals(2, lines.size)
        assertEquals(-500L, lines.first { it.categoryId == "cat-child" }.amountMinor)
        database.close()
    }

    @Test
    fun rawInsertWithCategoryAndTagSucceeds() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        seed(database)

        database.withTransaction {
            database.entryDao().insert(
                EntryEntity(
                    id = "e1",
                    accountId = "acc",
                    type = "EXPENSE",
                    amountMinor = -1250,
                    currency = "EUR",
                    date = "2026-11-11",
                    description = "Lunch",
                    categoryId = "cat",
                    createdAt = 0,
                    updatedAt = 0,
                ),
            )
            database.tagDao().insert(
                TagEntity(id = "t1", name = "work", nameNormalized = "work", createdAt = 0),
            )
            database.tagDao().insertEntryTag(EntryTagEntity(entryId = "e1", tagId = "t1"))
        }

        assertNotNull(database.entryDao().getById("e1"))
        database.close()
    }

    private suspend fun seed(database: TalexDatabase) {
        database.accountDao().insert(
            AccountEntity(
                id = "acc",
                name = "Card",
                type = "CHECKING",
                currency = "EUR",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        database.categoryDao().insert(
            CategoryEntity(
                id = "cat",
                kind = "EXPENSE",
                name = "Food",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
    }

    private fun open(passphrase: ByteArray): TalexDatabase =
        Room.databaseBuilder(context, TalexDatabase::class.java, databaseName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
}
