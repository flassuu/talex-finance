package io.github.flassuu.talex

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.flassuu.talex.core.money.CurrencyCode
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.data.db.DatabaseProvider
import io.github.flassuu.talex.data.repository.AccountRepositoryImpl
import io.github.flassuu.talex.data.repository.BudgetRepositoryImpl
import io.github.flassuu.talex.data.repository.CategoryRepositoryImpl
import io.github.flassuu.talex.data.repository.EntryRepositoryImpl
import io.github.flassuu.talex.data.repository.GoalRepositoryImpl
import io.github.flassuu.talex.data.repository.SettingsRepositoryImpl
import io.github.flassuu.talex.data.repository.ValuationRepositoryImpl
import io.github.flassuu.talex.data.security.Argon2KeyDeriver
import io.github.flassuu.talex.data.security.BiometricKeyStore
import io.github.flassuu.talex.data.security.DatabaseFactory
import io.github.flassuu.talex.data.security.DatabaseFileStore
import io.github.flassuu.talex.data.security.DemoProfileStore
import io.github.flassuu.talex.data.session.AppSessionManager
import io.github.flassuu.talex.data.session.LockState
import io.github.flassuu.talex.domain.usecase.SaveEntryUseCase
import io.github.flassuu.talex.domain.usecase.SeedDemoDataUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * End-to-end demo lifecycle (ADR-0031): create, seed, auto-unlock, convert and
 * delete. Runs against the real app database, so it must start from a clean
 * install; the caller restores any user data afterwards.
 */
@RunWith(AndroidJUnit4::class)
class DemoProfileInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val provider = DatabaseProvider()
    private val fileStore = DatabaseFileStore(context)
    private val session = AppSessionManager(
        context = context,
        fileStore = fileStore,
        keyDeriver = Argon2KeyDeriver(),
        databaseFactory = DatabaseFactory(context),
        provider = provider,
        biometricKeyStore = BiometricKeyStore(context),
        demoStore = DemoProfileStore(context),
    )

    private val accounts = AccountRepositoryImpl(provider)
    private val categories = CategoryRepositoryImpl(provider)
    private val entries = EntryRepositoryImpl(provider)
    private val budgets = BudgetRepositoryImpl(provider)
    private val goals = GoalRepositoryImpl(provider)
    private val valuations = ValuationRepositoryImpl(provider)

    @After
    fun tearDown() = runBlocking {
        session.deleteDemo()
        Unit
    }

    @Test
    fun demoProfileSupportsSeedConvertAndDelete() = runBlocking {
        System.loadLibrary("sqlcipher")

        assertTrue(session.setupDemo() is Outcome.Success)
        session.markUnlocked()
        assertTrue(session.isDemoProfile())
        assertTrue(fileStore.hasMetadata())

        SettingsRepositoryImpl(provider).setBaseCurrency(CurrencyCode.EUR)
        val seed = SeedDemoDataUseCase(
            accounts = accounts,
            categories = categories,
            saveEntry = SaveEntryUseCase(accounts, categories, entries),
            budgets = budgets,
            goals = goals,
            valuations = valuations,
        )
        assertTrue(seed(LocalDate.now()) is Outcome.Success)

        val createdAccounts = accounts.observeAccounts().first()
        assertEquals(6, createdAccounts.size)
        assertFalse(entries.observeAll().first().isEmpty())
        assertFalse(goals.observeGoals().first().isEmpty())
        assertTrue(createdAccounts.any { it.type.name == "PROPERTY" })

        // A new launch opens the demo without a passphrase.
        session.lock()
        assertEquals(LockState.LOCKED, session.state.value)
        assertTrue(session.unlockDemo() is Outcome.Success)

        // Converting keeps the data and switches to a user passphrase.
        val newPassphrase = "demo-converted-1".toCharArray()
        assertTrue(session.changeDemoPassphrase(newPassphrase) is Outcome.Success)
        assertFalse(session.isDemoProfile())
        assertEquals(LockState.LOCKED, session.state.value)
        assertTrue(session.unlock("demo-converted-1".toCharArray()) is Outcome.Success)
        assertEquals(6, accounts.observeAccounts().first().size)

        assertTrue(session.deleteDemo() is Outcome.Success)
        assertEquals(LockState.NOT_INITIALIZED, session.state.value)
        assertFalse(fileStore.hasMetadata())
        assertFalse(session.isDemoProfile())
    }
}
