package com.xichugeek.lunatide.ui.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xichugeek.lunatide.core.time.TodayProvider
import com.xichugeek.lunatide.data.backup.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class DataStage { NONE, EXPORT_PASSWORD, IMPORT_PASSWORD, PREVIEW, OVERWRITE, CSV_WARNING, BACKUP_READY, CSV_READY, BUSY, DONE }
data class DataUiState(val stage: DataStage = DataStage.NONE, val error: BackupError? = null, val periods: Int = 0, val days: Int = 0,
    val exportedAt: String = "", val recovery: Boolean = false, val result: ImportResult? = null, val saved: Boolean = false, val restart: Boolean = false)

@HiltViewModel
class DataViewModel @Inject constructor(
    @ApplicationContext private val context: Context, private val backups: BackupRepository, private val clock: TodayProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(DataUiState())
    val state = _state.asStateFlow()
    private var encrypted: ByteArray? = null
    private var outgoing: ByteArray? = null
    private var preview: BackupDocument? = null
    fun export() { if (_state.value.stage == DataStage.NONE) _state.value = DataUiState(DataStage.EXPORT_PASSWORD) }
    fun csv() { if (_state.value.stage == DataStage.NONE) _state.value = DataUiState(DataStage.CSV_WARNING) }
    fun beginImport(recovery: Boolean) { cancel(); _state.value = DataUiState(recovery = recovery) }
    fun selected(uri: Uri?) {
        if (uri == null) { cancel(); return }
        work {
            encrypted?.fill(0)
            encrypted = withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openInputStream(uri)).use { BackupCodec.readLimited(it) } }
            _state.update { it.copy(stage = DataStage.IMPORT_PASSWORD, error = null) }
        }
    }
    fun encrypt(password: String) {
        if (password.length !in 6..256) return
        work {
            val document = backups.snapshot()
            outgoing = withContext(Dispatchers.Default) {
                val bytes = document.encode(); val chars = password.toCharArray()
                try { BackupCodec().encrypt(bytes, chars) } finally { bytes.fill(0); chars.fill('0') }
            }
            _state.update { it.copy(stage = DataStage.BACKUP_READY, error = null) }
        }
    }
    fun decrypt(password: String) {
        val source = encrypted ?: return
        work {
            preview = withContext(Dispatchers.Default) {
                val chars = password.toCharArray()
                val bytes = try { BackupCodec().decrypt(source, chars) } finally { chars.fill('0') }
                try { BackupDocument.decode(bytes).also { it.records(clock.today.value) } } finally { bytes.fill(0) }
            }
            val document = requireNotNull(preview)
            _state.update { it.copy(stage = DataStage.PREVIEW, periods = document.periods.size, days = document.dayLogs.size, exportedAt = document.exportedAt.substringBefore('T'), error = null) }
        }
    }
    fun prepareCsv(labels: CsvLabels) { work {
        val document = backups.snapshot()
        val records = document.records(clock.today.value)
        outgoing = withContext(Dispatchers.Default) { exportCsv(records.first, records.second, clock.today.value, document.settings.showIntercourse, labels) }
        _state.value = DataUiState(DataStage.CSV_READY)
    } }
    fun consumeReady() { _state.update { it.copy(stage = DataStage.BUSY) } }
    fun filename(csv: Boolean): String {
        val stamp = Instant.ofEpochMilli(clock.timestamp()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
        return if (csv) "lunatide-records-$stamp.csv" else "lunatide-backup-$stamp.lunatide"
    }
    fun save(uri: Uri?) {
        if (uri == null) { _state.update { it.copy(stage = DataStage.NONE) }; cancel(); return }
        work {
            val bytes = requireNotNull(outgoing)
            try { withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes); it.flush() } } }
            finally { bytes.fill(0); outgoing = null }
            _state.value = DataUiState(DataStage.DONE, saved = true)
        }
    }
    fun restore(merge: Boolean, confirmed: Boolean = false) {
        if (!merge && !confirmed) { _state.update { it.copy(stage = DataStage.OVERWRITE) }; return }
        val document = preview ?: return
        val recover = _state.value.recovery
        work {
            val result = backups.restore(document, merge, recover)
            encrypted?.fill(0); encrypted = null; preview = null
            _state.value = DataUiState(DataStage.DONE, result = result, recovery = recover)
        }
    }
    fun closeResult() { if (_state.value.recovery && _state.value.result != null) _state.value = DataUiState(restart = true) else cancel() }
    fun cancel() { if (_state.value.stage != DataStage.BUSY) { encrypted?.fill(0); outgoing?.fill(0); encrypted = null; outgoing = null; preview = null; _state.value = DataUiState() } }
    private fun work(block: suspend () -> Unit) {
        _state.update { it.copy(stage = DataStage.BUSY, error = null) }
        viewModelScope.launch {
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { _state.update { it.copy(stage = if (encrypted != null && preview == null) DataStage.IMPORT_PASSWORD else DataStage.NONE,
                error = (error as? BackupException)?.reason ?: BackupError.STORAGE) } }
        }
    }
    override fun onCleared() { encrypted?.fill(0); outgoing?.fill(0); preview = null }
}
