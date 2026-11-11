package io.github.flassuu.talex

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.money.Money
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.data.db.AccountEntity
import io.github.flassuu.talex.data.db.CategoryEntity
import io.github.flassuu.talex.data.db.DatabaseProvider
import io.github.flassuu.talex.data.db.TalexDatabase
import io.github.flassuu.talex.data.repository.AccountRepositoryImpl
import io.github.flassuu.talex.data.repository.CategoryRepositoryImpl
import io.github.flassuu.talex.data.repository.EntryRepositoryImpl
import io.github.flassuu.talex.data.repository.RecurringRepositoryImpl
import io.github.flassuu.talex.domain.model.EntryType
import io.github.flassuu.talex.domain.model.Frequency
import io.github.flassuu.talex.domain.model.RecurringInput
import io.github.flassuu.talex.domain.usecase.GenerateRecurringUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RecurringRepositoryInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "phase2-recurring-test.db"
    private val key = ByteArray(32) { (it + 1).toByte() }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
        context.deleteDatabase("$databaseName-wal")
        context.deleteDatabase("$databaseName-shm")
    }

    @Test
    fun generationIsIdempotentAndAdvancesTheNextDueDate() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        database.accountDao().insert(
            AccountEntity(id = "acc", name = "Card", type = "CHECKING", currency = "EUR", createdAt = 0, updatedAt = 0),
        )
        database.categoryDao().insert(
            CategoryEntity(id = "cat", kind = "EXPENSE", name = "Housing", createdAt = 0, updatedAt = 0),
        )

        val provider = DatabaseProvider().apply { set(database) }
        val recurringRepository = RecurringRepositoryImpl(provider)
        val generate = GenerateRecurringUseCase(
            recurringRepository,
            AccountRepositoryImpl(provider),
            CategoryRepositoryImpl(provider),
            EntryRepositoryImpl(provider),
        )

        val created = recurringRepository.create(
            RecurringInput(
                accountId = "acc",
                type = EntryType.EXPENSE,
                amount = Money(-1_000, CurrencyCode.EUR),
                description = "Rent",
                categoryId = "cat",
                frequency = Frequency.MONTHLY,
                anchorDate = LocalDate.of(2026, 1, 1),
            ),
        )
        assertTrue(created is Outcome.Success)
        val rule = (created as Outcome.Success).value

        val firstRun = generate(LocalDate.of(2026, 3, 15))
        assertEquals(3, firstRun.created.size)
        assertEquals(3, database.entryDao().observeAll().first().size)

        val secondRun = generate(LocalDate.of(2026, 3, 15))
        assertEquals(0, secondRun.created.size)
        assertEquals(3, database.entryDao().observeAll().first().size)

        val updated = recurringRepository.get(rule.id)
        assertEquals(LocalDate.of(2026, 4, 1), updated?.nextDueDate)

        database.close()
    }

    private fun open(passphrase: ByteArray): TalexDatabase =
        Room.databaseBuilder(context, TalexDatabase::class.java, databaseName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
}
