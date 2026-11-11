package io.github.flassuu.talex.data.security

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.flassuu.talex.data.db.TalexDatabase
import io.github.flassuu.talex.data.db.TalexMigrations
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens Room on top of SQLCipher. SQLCipher receives the Argon2id output as its
 * passphrase material and applies its own per-file salt on top of it.
 */
@Singleton
class DatabaseFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun build(
        key: ByteArray,
        name: String = DatabaseFileStore.DATABASE_NAME,
    ): TalexDatabase = Room.databaseBuilder(
        context,
        TalexDatabase::class.java,
        name,
    )
        .openHelperFactory(SupportOpenHelperFactory(key))
        .addMigrations(*TalexMigrations.ALL)
        .build()
}
