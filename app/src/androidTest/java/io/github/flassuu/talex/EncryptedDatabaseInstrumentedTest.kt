package io.github.flassuu.talex

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.data.db.AccountEntity
import io.github.flassuu.talex.data.db.DatabaseProvider
import io.github.flassuu.talex.data.db.EntryEntity
import io.github.flassuu.talex.data.db.TalexDatabase
import io.github.flassuu.talex.data.repository.AccountRepositoryImpl
import io.github.flassuu.talex.data.security.Argon2KeyDeriver
import io.github.flassuu.talex.data.security.KdfMetadata
import io.github.flassuu.talex.domain.model.AccountType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "phase1-encrypted-test.db"
    private val key = ByteArray(32) { (it + 1).toByte() }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
        context.deleteDatabase("$databaseName-wal")
        context.deleteDatabase("$databaseName-shm")
    }

    @Test
    fun opensWithCorrectKeyAndRejectsWrongKey() {
        System.loadLibrary("sqlcipher")

        val database = open(key)
        database.openHelper.writableDatabase
        runBlocking {
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
            database.entryDao().insert(
                EntryEntity(
                    id = "e1",
                    accountId = "acc",
                    type = "OPENING",
                    amountMinor = 50_000,
                    currency = "EUR",
                    date = "2026-01-01",
                    description = "sys:opening",
                    createdAt = 0,
                    updatedAt = 0,
                ),
            )
            assertEquals(50_000L, database.entryDao().sumForAccount("acc"))
        }
        database.close()

        val reopened = open(key)
        runBlocking { assertNotNull(reopened.accountDao().getById("acc")) }
        reopened.close()

        val wrongKeyDatabase = open(ByteArray(32) { 9 })
        try {
            wrongKeyDatabase.openHelper.writableDatabase
            fail("Opening with the wrong key must fail")
        } catch (expected: Throwable) {
            // Expected: SQLCipher cannot decrypt the file.
        } finally {
            runCatching { wrongKeyDatabase.close() }
        }
    }

    @Test
    fun argon2IsDeterministicOnDevice() {
        val deriver = Argon2KeyDeriver()
        val salt = ByteArray(16) { it.toByte() }
        val metadata = KdfMetadata(
            version = 1,
            algorithm = "argon2id",
            memoryKib = 8192,
            iterations = 2,
            parallelism = 1,
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            hashLengthBytes = 32,
        )

        val first = deriver.derive("correct horse".toCharArray(), metadata)
        val second = deriver.derive("correct horse".toCharArray(), metadata)

        assertArrayEquals(first, second)
        assertEquals(32, first.size)
        assertFalse(first.contentEquals(deriver.derive("wrong".toCharArray(), metadata)))
    }

    @Test
    fun accountWithStartingBalanceCreatesOpeningEntry() = runBlocking {
        System.loadLibrary("sqlcipher")

        val provider = DatabaseProvider()
        val database = open(key)
        database.openHelper.writableDatabase
        provider.set(database)
        val repository = AccountRepositoryImpl(provider)

        val created = repository.createAccount(
            name = "Card",
            type = AccountType.CHECKING,
            currency = CurrencyCode.EUR,
            startingBalance = Money(50_000, CurrencyCode.EUR),
            openingDate = LocalDate.of(2026, 1, 1),
        )
        assertTrue(created is Outcome.Success)
        val account = (created as Outcome.Success).value
        val entries = database.entryDao().observeForAccount(account.id).first()
        assertEquals(1, entries.size)
        assertEquals("OPENING", entries.single().type)
        assertEquals(50_000L, database.entryDao().sumForAccount(account.id))

        val zero = repository.createAccount(
            name = "Wallet",
            type = AccountType.CASH,
            currency = CurrencyCode.EUR,
            startingBalance = null,
            openingDate = LocalDate.of(2026, 1, 1),
        )
        assertTrue(zero is Outcome.Success)
        val zeroAccount = (zero as Outcome.Success).value
        assertEquals(0, database.entryDao().observeForAccount(zeroAccount.id).first().size)

        database.close()
    }

    @Test
    fun archivingKeepsTheAccountInStorage() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        val provider = DatabaseProvider().apply { set(database) }
        val repository = AccountRepositoryImpl(provider)

        val created = repository.createAccount(
            name = "Card",
            type = AccountType.CHECKING,
            currency = CurrencyCode.EUR,
            startingBalance = null,
            openingDate = LocalDate.of(2026, 1, 1),
        )
        assertTrue(created is Outcome.Success)
        val accountId = (created as Outcome.Success).value.id

        assertTrue(repository.setArchived(accountId, true) is Outcome.Success)
        val account = repository.observeAccounts().first().single { it.id == accountId }
        assertTrue(account.archived)
        assertNotNull(database.accountDao().getById(accountId))

        database.close()
    }

    private fun open(passphrase: ByteArray): TalexDatabase =
        Room.databaseBuilder(context, TalexDatabase::class.java, databaseName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
}
