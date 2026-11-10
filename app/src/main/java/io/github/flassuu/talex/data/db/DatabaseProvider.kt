package io.github.flassuu.talex.data.db

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the currently opened encrypted database. The database exists only while the
 * session is unlocked; repositories obtain it lazily for every operation.
 */
@Singleton
class DatabaseProvider @Inject constructor() {

    @Volatile
    private var database: TalexDatabase? = null

    fun set(database: TalexDatabase?) {
        this.database = database
    }

    fun get(): TalexDatabase? = database

    fun require(): TalexDatabase = checkNotNull(database) { "Database is locked" }
}
