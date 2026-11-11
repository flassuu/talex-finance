package io.github.flassuu.talex

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.flassuu.talex.data.db.AccountEntity
import io.github.flassuu.talex.data.db.EntryEntity
import io.github.flassuu.talex.data.db.TalexDatabase
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BalanceInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "balance-test.db"
    private val key = ByteArray(32) { (it + 1).toByte() }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
        context.deleteDatabase("$databaseName-wal")
        context.deleteDatabase("$databaseName-shm")
    }

    @Test
    fun balanceEqualsSumOfRandomEntriesAndExcludesDeleted() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        database.accountDao().insert(account())

        val random = kotlin.random.Random(42)
        val amounts = LongArray(2_000) {
            val magnitude = random.nextLong(1, 100_001)
            if (random.nextBoolean()) magnitude else -magnitude
        }
        database.entryDao().insertAll(
            amounts.mapIndexed { index, amount ->
                entry(index, amount)
            },
        )

        assertEquals(amounts.sum(), database.entryDao().sumForAccount(ACCOUNT_ID))

        val deleted = amounts.take(10).sum()
        amounts.take(10).indices.forEach { index ->
            database.entryDao().softDelete("e$index", 1L)
        }
        assertEquals(amounts.sum() - deleted, database.entryDao().sumForAccount(ACCOUNT_ID))

        database.close()
    }

    @Test
    fun tenThousandEntriesRemainExact() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        database.accountDao().insert(account())

        database.entryDao().insertAll(
            (0 until 10_000).map { index ->
                entry(index, 100L + index)
            },
        )

        val expected = (0 until 10_000).sumOf { 100L + it }
        assertEquals(expected, database.entryDao().sumForAccount(ACCOUNT_ID))

        database.close()
    }

    private fun account() = AccountEntity(
        id = ACCOUNT_ID,
        name = "Card",
        type = "CHECKING",
        currency = "EUR",
        createdAt = 0,
        updatedAt = 0,
    )

    private fun entry(index: Int, amountMinor: Long) = EntryEntity(
        id = "e$index",
        accountId = ACCOUNT_ID,
        type = if (amountMinor >= 0) "INCOME" else "EXPENSE",
        amountMinor = amountMinor,
        currency = "EUR",
        date = "2026-01-01",
        description = "e$index",
        createdAt = 0,
        updatedAt = 0,
    )

    private fun open(passphrase: ByteArray): TalexDatabase =
        Room.databaseBuilder(context, TalexDatabase::class.java, databaseName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()

    private companion object {
        const val ACCOUNT_ID = "acc"
    }
}
