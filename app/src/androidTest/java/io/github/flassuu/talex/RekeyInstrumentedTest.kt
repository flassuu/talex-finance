package io.github.flassuu.talex

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.flassuu.talex.data.db.AccountEntity
import io.github.flassuu.talex.data.db.TalexDatabase
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the SQLCipher export used by a passphrase change (ADR-0029): a complete
 * copy written with the new key must open, and the old key must stop working.
 */
@RunWith(AndroidJUnit4::class)
class RekeyInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "rekey-test.db"
    private val oldKey = ByteArray(32) { (it + 1).toByte() }
    private val newKey = ByteArray(32) { (it + 64).toByte() }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
        context.deleteDatabase("$databaseName-wal")
        context.deleteDatabase("$databaseName-shm")
        context.deleteDatabase("$databaseName-old")
    }

    @Test
    fun exportWithNewKeyKeepsDataAndInvalidatesTheOldKey() = runBlocking {
        System.loadLibrary("sqlcipher")

        val database = open(databaseName, oldKey)
        database.openHelper.writableDatabase
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
        val target = context.getDatabasePath("$databaseName-old")
        target.parentFile?.mkdirs()
        target.delete()
        export(database, target, newKey)
        database.close()

        val installed = context.getDatabasePath(databaseName)
        val staged = context.getDatabasePath("$databaseName-old")
        check(staged.renameTo(installed)) { "Could not install the exported database" }

        val reopened = open(databaseName, newKey)
        assertEquals("Card", reopened.accountDao().getById("acc")?.name)
        reopened.close()

        val withOldKey = open(databaseName, oldKey)
        try {
            withOldKey.openHelper.writableDatabase
            fail("The old key must not open the re-keyed file")
        } catch (expected: Throwable) {
            // Expected: SQLCipher cannot decrypt with the previous key.
        } finally {
            runCatching { withOldKey.close() }
        }
    }

    private fun export(database: TalexDatabase, target: java.io.File, key: ByteArray) {
        val sql = database.openHelper.writableDatabase as SQLiteDatabase
        sql.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        val stagingKey = "staging-passphrase-for-test"
        sql.rawExecSQL(
            "ATTACH DATABASE '${target.absolutePath}' AS rekeyed KEY '$stagingKey'",
        )
        try {
            sql.rawExecSQL("SELECT sqlcipher_export('rekeyed')")
        } finally {
            runCatching { sql.rawExecSQL("DETACH DATABASE rekeyed") }
        }
        val copy = SQLiteDatabase.openDatabase(
            target.absolutePath,
            stagingKey,
            null,
            SQLiteDatabase.OPEN_READWRITE,
            null,
        )
        try {
            copy.changePassword(key)
        } finally {
            copy.close()
        }
    }

    private fun open(name: String, key: ByteArray): TalexDatabase =
        Room.databaseBuilder(context, TalexDatabase::class.java, name)
            .openHelperFactory(SupportOpenHelperFactory(key))
            .build()
}
