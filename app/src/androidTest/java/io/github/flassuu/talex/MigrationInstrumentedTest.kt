package io.github.flassuu.talex

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.flassuu.talex.data.db.TalexDatabase
import io.github.flassuu.talex.data.db.TalexMigrations
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "migration-test.db"
    private val key = ByteArray(32) { (it + 1).toByte() }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = TalexDatabase::class.java,
        specs = emptyList(),
        openFactory = SupportOpenHelperFactory(key),
    )

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
        context.deleteDatabase("$databaseName-wal")
        context.deleteDatabase("$databaseName-shm")
    }

    @Test
    fun migratesVersion1To2() {
        helper.createDatabase(databaseName, 1).close()
        helper.runMigrationsAndValidate(databaseName, 2, true, TalexMigrations.MIGRATION_1_2).close()
    }

    @Test
    fun migratesVersion2To3() {
        helper.createDatabase(databaseName, 2).close()
        helper.runMigrationsAndValidate(databaseName, 3, true, TalexMigrations.MIGRATION_2_3).close()
    }

    @Test
    fun migratesVersion3To4() {
        helper.createDatabase(databaseName, 3).close()
        helper.runMigrationsAndValidate(databaseName, 4, true, TalexMigrations.MIGRATION_3_4).close()
    }

    @Test
    fun migratesVersion1To4AndKeepsData() = runBlocking {
        helper.createDatabase(databaseName, 1).use { db ->
            db.execSQL(
                "INSERT INTO accounts (id, name, type, currency, archived, created_at, updated_at) " +
                    "VALUES ('acc', 'Card', 'CHECKING', 'EUR', 0, 0, 0)",
            )
            db.execSQL(
                "INSERT INTO entries (id, account_id, type, amount_minor, currency, date, description, created_at, updated_at) " +
                    "VALUES ('e1', 'acc', 'OPENING', 50000, 'EUR', '2026-01-01', 'sys:opening', 0, 0)",
            )
            db.execSQL(
                "INSERT INTO categories (id, kind, name, created_at, updated_at) " +
                    "VALUES ('c1', 'EXPENSE', 'Food', 0, 0)",
            )
        }

        helper.runMigrationsAndValidate(
            databaseName,
            4,
            true,
            TalexMigrations.MIGRATION_1_2,
            TalexMigrations.MIGRATION_2_3,
            TalexMigrations.MIGRATION_3_4,
        ).close()

        val database = Room.databaseBuilder(context, TalexDatabase::class.java, databaseName)
            .openHelperFactory(SupportOpenHelperFactory(key))
            .addMigrations(*TalexMigrations.ALL)
            .build()
        assertEquals("Card", database.accountDao().getById("acc")?.name)
        assertEquals(50_000L, database.entryDao().sumForAccount("acc"))
        assertNotNull(database.entryDao().getById("e1"))
        assertEquals(false, database.categoryDao().getById("c1")?.unnecessary)
        database.close()
    }
}
