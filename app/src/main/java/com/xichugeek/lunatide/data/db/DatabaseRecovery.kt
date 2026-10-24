package com.xichugeek.lunatide.data.db

import android.content.Context
import android.util.AtomicFile
import androidx.room.Room
import com.xichugeek.lunatide.core.security.DatabaseKeyManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.*
import javax.inject.Inject

/** Only used after the user decrypts a backup and explicitly confirms replacing inaccessible data. */
class DatabaseRecovery @Inject constructor(
    @ApplicationContext private val context: Context, private val owner: DatabaseOwner, private val helper: HelperFactoryProvider,
    private val wrappingKeys: DatabaseKeyManager,
) {
    suspend fun replace(write: suspend (LunatideDatabase) -> Unit) {
        owner.close()
        finishPending(context)
        discardStaging(context)
        val stagedDatabase = context.getDatabasePath(STAGED_DB)
        val stagedKey = File(context.noBackupFilesDir, STAGED_KEY)
        val keys = DatabaseKeyManager(stagedKey, stagedDatabase, wrappingKeys.alias)
        val password = try { keys.password() } catch (_: Exception) {
            // This path is explicitly replacing data already known to be inaccessible.
            keys.delete(); keys.password()
        }
        val db = try { Room.databaseBuilder(context, LunatideDatabase::class.java, STAGED_DB).openHelperFactory(helper.create(password)).build().also {
            it.openHelper.writableDatabase.query("SELECT 1").close()
        } }
        finally { password.fill(0) }
        var prepared = false
        try {
            write(db)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
            db.close()
            val atomic = AtomicFile(File(context.noBackupFilesDir, MARKER))
            val output = atomic.startWrite()
            try { output.write(byteArrayOf(1)); atomic.finishWrite(output) }
            catch (error: Exception) { atomic.failWrite(output); throw error }
            prepared = true
            finishPending(context)
        } finally {
            db.close()
            if (!prepared) discardStaging(context)
        }
    }
    companion object {
        private const val STAGED_DB = "lunatide.restore.db"
        private const val STAGED_KEY = "db_key.restore.bin"
        private const val MARKER = "restore_ready"
        /** A durable marker completes both renames after interruption; it contains no user values. */
        fun finishPending(context: Context) {
            val marker = AtomicFile(File(context.noBackupFilesDir, MARKER))
            if (!marker.baseFile.exists() && !File(marker.baseFile.path + ".bak").exists()) return
            marker.readFully()
            val stagedDb = context.getDatabasePath(STAGED_DB)
            val stagedKey = File(context.noBackupFilesDir, STAGED_KEY)
            val target = context.getDatabasePath("lunatide.db")
            listOf("-wal", "-shm").forEach { suffix ->
                val sidecar = File(target.path + suffix)
                check(!sidecar.exists() || sidecar.delete())
            }
            if (stagedDb.exists()) Files.move(stagedDb.toPath(), target.toPath(), REPLACE_EXISTING, ATOMIC_MOVE)
            if (stagedKey.exists()) {
                val oldBackup = File(context.noBackupFilesDir, "db_key.bin.bak")
                check(!oldBackup.exists() || oldBackup.delete())
                Files.move(stagedKey.toPath(), File(context.noBackupFilesDir, "db_key.bin").toPath(), REPLACE_EXISTING, ATOMIC_MOVE)
            }
            marker.delete()
            discardStaging(context)
        }
        fun discardStaging(context: Context) {
            context.deleteDatabase(STAGED_DB)
            AtomicFile(File(context.noBackupFilesDir, STAGED_KEY)).delete()
        }
        fun clear(context: Context) {
            AtomicFile(File(context.noBackupFilesDir, MARKER)).delete()
            discardStaging(context)
        }
    }
}
