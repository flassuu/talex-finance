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
import io.github.flassuu.talex.data.repository.AccountRepositoryImpl
import io.github.flassuu.talex.data.repository.TransferRepositoryImpl
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.TransferInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class AccountLifecycleInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "account-lifecycle-test.db"
    private val key = ByteArray(32) { (it + 1).toByte() }
    private val date = LocalDate.of(2026, 11, 11)

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
        context.deleteDatabase("$databaseName-wal")
        context.deleteDatabase("$databaseName-shm")
    }

    @Test
    fun adjustBalanceWritesAnAdjustmentEntry() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        val provider = DatabaseProvider().apply { set(database) }
        val accounts = AccountRepositoryImpl(provider)

        val created = accounts.createAccount(
            name = "Card",
            type = AccountType.CHECKING,
            currency = CurrencyCode.EUR,
            startingBalance = Money(50_000, CurrencyCode.EUR),
            openingDate = date,
        )
        assertTrue(created is Outcome.Success)
        val accountId = (created as Outcome.Success).value.id

        assertTrue(accounts.adjustBalance(accountId, Money(45_000, CurrencyCode.EUR), date) is Outcome.Success)
        assertEquals(45_000L, database.entryDao().sumForAccount(accountId))
        val entries = database.entryDao().observeForAccount(accountId).first()
        assertTrue(entries.any { it.type == "ADJUSTMENT" && it.amountMinor == -5_000L })

        database.close()
    }

    @Test
    fun deleteAccountRemovesItsEntriesAndTransferSides() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        listOf("a", "b").forEach { id ->
            database.accountDao().insert(
                AccountEntity(
                    id = id,
                    name = id,
                    type = "CHECKING",
                    currency = "EUR",
                    createdAt = 0,
                    updatedAt = 0,
                ),
            )
        }
        val provider = DatabaseProvider().apply { set(database) }
        val accounts = AccountRepositoryImpl(provider)
        val transfers = TransferRepositoryImpl(provider)

        val created = transfers.create(
            TransferInput(
                sourceAccountId = "a",
                destinationAccountId = "b",
                sourceAmount = Money(1_000, CurrencyCode.EUR),
                destinationAmount = Money(1_000, CurrencyCode.EUR),
                date = date,
                description = "Move",
            ),
        )
        assertTrue(created is Outcome.Success)
        val groupId = (created as Outcome.Success).value.id

        assertTrue(accounts.deleteAccount("a") is Outcome.Success)
        assertNull(database.accountDao().getById("a"))
        assertTrue(database.entryDao().observeAll().first().isEmpty())
        assertNull(database.transferDao().getGroup(groupId))
        assertEquals(0L, database.entryDao().sumForAccount("b"))

        database.close()
    }

    private fun open(passphrase: ByteArray): TalexDatabase =
        Room.databaseBuilder(context, TalexDatabase::class.java, databaseName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
}
