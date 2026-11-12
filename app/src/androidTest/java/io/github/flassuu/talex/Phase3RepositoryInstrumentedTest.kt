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
import io.github.flassuu.talex.data.repository.BudgetRepositoryImpl
import io.github.flassuu.talex.data.repository.CategoryRepositoryImpl
import io.github.flassuu.talex.data.repository.GoalRepositoryImpl
import io.github.flassuu.talex.data.repository.ValuationRepositoryImpl
import io.github.flassuu.talex.domain.model.AccountType
import io.github.flassuu.talex.domain.model.GoalContributionInput
import io.github.flassuu.talex.domain.model.GoalInput
import io.github.flassuu.talex.domain.model.ValuationInput
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
class Phase3RepositoryInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "phase3-repository-test.db"
    private val key = ByteArray(32) { (it + 1).toByte() }
    private val eur = CurrencyCode.EUR

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
        context.deleteDatabase("$databaseName-wal")
        context.deleteDatabase("$databaseName-shm")
    }

    @Test
    fun budgetChangesInPlaceThisMonthAndKeepsHistoryAcrossMonths() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        database.categoryDao().insert(
            CategoryEntity(id = "food", kind = "EXPENSE", name = "Food", createdAt = 0, updatedAt = 0),
        )
        val provider = DatabaseProvider().apply { set(database) }
        val budgets = BudgetRepositoryImpl(provider)

        assertTrue(budgets.setLimit("food", Money(30_000, eur), LocalDate.of(2026, 1, 15)) is Outcome.Success)
        val january = budgets.observeBudgets().first().single()
        assertEquals(LocalDate.of(2026, 1, 1), january.effectiveFrom)

        // The same month updates the row instead of appending.
        budgets.setLimit("food", Money(40_000, eur), LocalDate.of(2026, 1, 20))
        val stillOne = budgets.observeBudgets().first()
        assertEquals(1, stillOne.size)
        assertEquals(40_000L, stillOne.single().amount.amountMinor)

        // A later month closes the old row and appends a new one; January is not rewritten.
        budgets.setLimit("food", Money(50_000, eur), LocalDate.of(2026, 3, 10))
        val rows = budgets.observeBudgets().first().sortedBy { it.effectiveFrom }
        assertEquals(2, rows.size)
        assertEquals(40_000L, rows[0].amount.amountMinor)
        assertEquals(LocalDate.of(2026, 1, 1), rows[0].effectiveFrom)
        assertEquals(LocalDate.of(2026, 3, 10), rows[0].closedAt)
        assertEquals(50_000L, rows[1].amount.amountMinor)
        assertEquals(LocalDate.of(2026, 3, 1), rows[1].effectiveFrom)

        database.close()
    }

    @Test
    fun goalProgressIsTheSumOfItsContributions() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        val provider = DatabaseProvider().apply { set(database) }
        val goals = GoalRepositoryImpl(provider)

        val created = goals.create(
            GoalInput("Trip", Money(100_000, eur), LocalDate.of(2026, 12, 1)),
        )
        assertTrue(created is Outcome.Success)
        val goal = (created as Outcome.Success).value

        goals.addContribution(goal.id, GoalContributionInput(Money(20_000, eur), LocalDate.of(2026, 1, 5)))
        goals.addContribution(goal.id, GoalContributionInput(Money(5_000, eur), LocalDate.of(2026, 2, 5)))
        goals.addContribution(goal.id, GoalContributionInput(Money(-2_000, eur), LocalDate.of(2026, 2, 6)))

        val reloaded = goals.get(goal.id)
        assertEquals(23_000L, reloaded?.currentAmount?.amountMinor)
        assertEquals(3, reloaded?.contributions?.size)

        database.close()
    }

    @Test
    fun anAssetBalanceIsItsLatestValuation() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        database.accountDao().insert(
            AccountEntity(
                id = "home",
                name = "Home",
                type = AccountType.PROPERTY.name,
                currency = "EUR",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        val provider = DatabaseProvider().apply { set(database) }
        val valuations = ValuationRepositoryImpl(provider)
        val accounts = AccountRepositoryImpl(provider)

        valuations.add("home", ValuationInput(Money(200_000, eur), LocalDate.of(2026, 1, 1)))
        valuations.add("home", ValuationInput(Money(210_000, eur), LocalDate.of(2026, 2, 1)))

        val balance = accounts.observeAccounts().first().single().balance
        assertEquals(210_000L, balance.amountMinor)

        database.close()
    }

    @Test
    fun theUnnecessaryFlagIsPersisted() = runBlocking {
        System.loadLibrary("sqlcipher")
        val database = open(key)
        database.openHelper.writableDatabase
        database.categoryDao().insert(
            CategoryEntity(id = "fun", kind = "EXPENSE", name = "Fun", createdAt = 0, updatedAt = 0),
        )
        val provider = DatabaseProvider().apply { set(database) }
        val categories = CategoryRepositoryImpl(provider)

        assertTrue(categories.setUnnecessary("fun", true) is Outcome.Success)
        assertEquals(true, categories.get("fun")?.unnecessary)

        database.close()
    }

    private fun open(passphrase: ByteArray): TalexDatabase =
        Room.databaseBuilder(context, TalexDatabase::class.java, databaseName)
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
}
