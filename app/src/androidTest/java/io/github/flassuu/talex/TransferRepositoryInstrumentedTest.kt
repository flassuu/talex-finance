package io.github.flassuu.talex

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.data.db.AccountEntity
import io.github.flassuu.talex.data.db.DatabaseProvider
import io.github.flassuu.talex.data.db.TalexDatabase
import io.github.flassuu.talex.data.repository.TransferRepositoryImpl
import io.github.flassuu.talex.domain.model.TransferInput
import kotlinx.coroutines.flow.first
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
class TransferRepositoryInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "phase2-transfer-test.db"
    private val key = ByteArray(32) { (it + 1).toByte() }
    private val date = LocalDate.of(2026, 11, 11)

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
        context.deleteDatabase("$databaseName-wal")
        context.deleteDatabase("$databaseName-shm")
    }

    @Test
    fun crossCurrencyTransferCreatesLinkedPairAndStoresRate() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        seed(database)
        val provider = DatabaseProvider().apply { set(database) }
        val repository = TransferRepositoryImpl(provider)

        val result = repository.create(
            TransferInput(
                sourceAccountId = "eur",
                destinationAccountId = "usd",
                sourceAmount = Money(10_000, CurrencyCode.EUR),
                destinationAmount = Money(11_000, CurrencyCode.USD),
                date = date,
                description = "Exchange",
            ),
        )
        assertTrue("transfer failed: $result", result is Outcome.Success)
        val transfer = (result as Outcome.Success).value

        val entries = database.entryDao().observeAll().first()
        assertEquals(2, entries.size)
        assertEquals(-10_000L, entries.first { it.accountId == "eur" }.amountMinor)
        assertEquals(11_000L, entries.first { it.accountId == "usd" }.amountMinor)
        assertEquals(-10_000L, database.entryDao().sumForAccount("eur"))
        assertEquals(11_000L, database.entryDao().sumForAccount("usd"))
        assertEquals("1.1", transfer.rate.toPlainString())
        assertNotNull(database.transferDao().getGroup(transfer.id))

        assertTrue(repository.delete(transfer.id) is Outcome.Success)
        assertTrue(database.entryDao().observeAll().first().isEmpty())
        assertEquals(0L, database.entryDao().sumForAccount("eur"))
        assertEquals(0L, database.entryDao().sumForAccount("usd"))

        database.close()
    }

    @Test
    fun sameCurrencyTransferKeepsTotalCashUnchanged() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        seed(database)
        val provider = DatabaseProvider().apply { set(database) }
        val repository = TransferRepositoryImpl(provider)

        val result = repository.create(
            TransferInput(
                sourceAccountId = "eur",
                destinationAccountId = "eur2",
                sourceAmount = Money(5_000, CurrencyCode.EUR),
                destinationAmount = Money(5_000, CurrencyCode.EUR),
                date = date,
                description = "Move",
            ),
        )
        assertTrue("transfer failed: $result", result is Outcome.Success)

        val first = database.entryDao().sumForAccount("eur")
        val second = database.entryDao().sumForAccount("eur2")
        assertEquals(-5_000L, first)
        assertEquals(5_000L, second)
        assertEquals(0L, first + second)

        database.close()
    }

    private suspend fun seed(database: TalexDatabase) {
        listOf(
            AccountEntity(id = "eur", name = "EUR", type = "CHECKING", currency = "EUR", createdAt = 0, updatedAt = 0),
            AccountEntity(id = "eur2", name = "EUR 2", type = "SAVINGS", currency = "EUR", createdAt = 0, updatedAt = 0),
            AccountEntity(id = "usd", name = "USD", type = "CHECKING", currency = "USD", createdAt = 0, updatedAt = 0),
        ).forEach { database.accountDao().insert(it) }
    }

    private fun open(passphrase: ByteArray): TalexDatabase =
        Room.databaseBuilder(context, TalexDatabase::class.java, databaseName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
}
