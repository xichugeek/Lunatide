package com.xichugeek.lunatide.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.xichugeek.lunatide.core.security.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import net.zetetic.database.Logger
import net.zetetic.database.NoopTarget
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

fun interface HelperFactoryProvider { fun create(password: ByteArray): SupportSQLiteOpenHelper.Factory }

@Singleton
class DatabaseOwner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keys: DatabaseKeyManager, private val helper: HelperFactoryProvider,
) {
    private var database: LunatideDatabase? = null

    /** Call from Dispatchers.IO. Open eagerly before wiping the caller's password buffer. */
    @Synchronized
    fun get(): LunatideDatabase {
        database?.let { return it }
        val password = keys.password()
        var candidate: LunatideDatabase? = null
        try {
            candidate = Room.databaseBuilder(context, LunatideDatabase::class.java, "lunatide.db")
                .openHelperFactory(helper.create(password)).build()
            candidate.openHelper.writableDatabase.query("SELECT 1").close()
            database = candidate
            return candidate
        } catch (error: Exception) {
            candidate?.close()
            throw DataUnavailable(error)
        } finally { password.fill(0) }
    }

    @Synchronized fun close() { database?.close(); database = null }
    fun remove() {
        close()
        if (!context.deleteDatabase("lunatide.db") && context.getDatabasePath("lunatide.db").exists()) throw DataUnavailable()
        keys.delete()
    }
}

fun encryptedHelperProvider(): HelperFactoryProvider {
    System.loadLibrary("sqlcipher")
    Logger.setTarget(NoopTarget())
    return HelperFactoryProvider { password ->
        SupportSQLiteOpenHelper.Factory { configuration ->
            val workingKey = password.copyOf()
            val delegate = SupportOpenHelperFactory(workingKey).create(configuration)
            object : SupportSQLiteOpenHelper by delegate {
                override fun close() {
                    try { delegate.close() } finally { workingKey.fill(0) }
                }
            }
        }
    }
}
