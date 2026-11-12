package io.github.flassuu.talex

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.data.db.AccountEntity
import io.github.flassuu.talex.data.db.CategoryEntity
import io.github.flassuu.talex.data.db.DatabaseProvider
import io.github.flassuu.talex.data.db.EntryEntity
import io.github.flassuu.talex.data.db.EntryLineEntity
import io.github.flassuu.talex.data.db.TalexDatabase
import io.github.flassuu.talex.data.db.TransferGroupEntity
import io.github.flassuu.talex.data.repository.DataTransferRepositoryImpl
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataTransferInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val sourceName = "data-transfer-source.db"
    private val targetName = "data-transfer-target.db"
    private val key = ByteArray(32) { (it + 1).toByte() }

    @After
    fun tearDown() {
        listOf(sourceName, targetName).forEach {
            context.deleteDatabase(it)
            context.deleteDatabase("$it-wal")
            context.deleteDatabase("$it-shm")
        }
    }

    @Test
    fun exportsFromOneDatabaseAndImportsIntoAnother() = runBlocking {
        System.loadLibrary("sqlcipher")
        val source = open(sourceName)
        source.openHelper.writableDatabase
        seedEntries(source)
        val sourceTransfer = DataTransferRepositoryImpl(DatabaseProvider().apply { set(source) })

        val csv = sourceTransfer.exportTransactionsCsv()
        assertTrue(csv.contains("Lunch"))

        // Importing the same file back is a no-op and reports the three units as duplicates.
        val repeated = sourceTransfer.importTransactionsCsv(csv) as Outcome.Success
        assertEquals(0, repeated.value.imported)
        assertEquals(3, repeated.value.skipped.size)

        // A second, empty database receives the data.
        val target = open(targetName)
        target.openHelper.writableDatabase
        seedLookups(target)
        val targetTransfer = DataTransferRepositoryImpl(DatabaseProvider().apply { set(target) })
        val fresh = targetTransfer.importTransactionsCsv(csv) as Outcome.Success
        assertEquals(3, fresh.value.imported)
        assertEquals(0, fresh.value.skipped.size)

        val entries = target.entryDao().getAllWithLines()
        assertTrue(entries.any { it.entry.type == "EXPENSE" && it.lines.size == 2 })
        assertEquals(1, target.transferDao().getAllGroups().size)
        assertEquals(2, entries.count { it.entry.type == "TRANSFER" })

        source.close()
        target.close()
    }

    private suspend fun seedLookups(database: TalexDatabase) {
        database.accountDao().insert(
            AccountEntity(id = "acc1", name = "Checking", type = "CHECKING", currency = "EUR", createdAt = 0, updatedAt = 0),
        )
        database.accountDao().insert(
            AccountEntity(id = "acc2", name = "Savings", type = "SAVINGS", currency = "EUR", createdAt = 0, updatedAt = 0),
        )
        database.categoryDao().insert(
            CategoryEntity(id = "food", kind = "EXPENSE", name = "Food", createdAt = 0, updatedAt = 0),
        )
        database.categoryDao().insert(
            CategoryEntity(id = "fun", kind = "EXPENSE", name = "Fun", createdAt = 0, updatedAt = 0),
        )
    }

    private suspend fun seedEntries(database: TalexDatabase) {
        seedLookups(database)
        database.entryDao().insert(
            EntryEntity(
                id = "e1",
                accountId = "acc1",
                type = "EXPENSE",
                amountMinor = -1_000,
                currency = "EUR",
                date = "2026-03-01",
                description = "Lunch",
                categoryId = "food",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        database.entryDao().insert(
            EntryEntity(
                id = "e2",
                accountId = "acc1",
                type = "EXPENSE",
                amountMinor = -3_000,
                currency = "EUR",
                date = "2026-03-02",
                description = "Receipt",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        database.entryLineDao().insertAll(
            listOf(
                EntryLineEntity(id = "l1", entryId = "e2", categoryId = "food", amountMinor = -2_000, position = 0),
                EntryLineEntity(id = "l2", entryId = "e2", categoryId = "fun", amountMinor = -1_000, position = 1),
            ),
        )
        database.entryDao().insert(
            EntryEntity(
                id = "t1",
                accountId = "acc1",
                type = "TRANSFER",
                amountMinor = -5_000,
                currency = "EUR",
                date = "2026-03-03",
                description = "Move",
                transferGroupId = "g1",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        database.entryDao().insert(
            EntryEntity(
                id = "t2",
                accountId = "acc2",
                type = "TRANSFER",
                amountMinor = 5_000,
                currency = "EUR",
                date = "2026-03-03",
                description = "Move",
                transferGroupId = "g1",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        database.transferDao().insertGroup(
            TransferGroupEntity(
                id = "g1",
                sourceEntryId = "t1",
                destinationEntryId = "t2",
                sourceAccountId = "acc1",
                destinationAccountId = "acc2",
                sourceAmountMinor = 5_000,
                destinationAmountMinor = 5_000,
                sourceCurrency = "EUR",
                destinationCurrency = "EUR",
                rate = "1",
                date = "2026-03-03",
                rateDate = "2026-03-03",
                rateSource = "manual",
                description = "Move",
                createdAt = 0,
            ),
        )
    }

    private fun open(databaseName: String): TalexDatabase =
        Room.databaseBuilder(context, TalexDatabase::class.java, databaseName)
            .openHelperFactory(SupportOpenHelperFactory(key))
            .build()
}
