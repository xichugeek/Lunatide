package com.xichugeek.lunatide.ui.data

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.data.backup.BackupError
import com.xichugeek.lunatide.ui.components.PrivateTextInput

@Composable
fun DataDialogs(state: DataUiState, onCancel: () -> Unit, onPassword: (String) -> Unit,
    onRestore: (Boolean, Boolean) -> Unit, onCsv: () -> Unit, onCloseResult: () -> Unit) {
    val stage = state.stage
    if (stage in listOf(DataStage.EXPORT_PASSWORD, DataStage.IMPORT_PASSWORD)) {
        val export = stage == DataStage.EXPORT_PASSWORD
        var password by remember(stage) { mutableStateOf("") }
        var confirmation by remember(stage) { mutableStateOf("") }
        AlertDialog(onDismissRequest = onCancel, title = { Text(stringResource(if (export) R.string.backup_password_set else R.string.backup_password_enter)) },
            text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (export) Text(stringResource(R.string.backup_password_notice))
                PrivateTextInput(password, stringResource(R.string.backup_password), password = true) { password = it }
                if (export) PrivateTextInput(confirmation, stringResource(R.string.backup_password_confirm), password = true) { confirmation = it }
                state.error?.let { Text(stringResource(errorText(it)), color = MaterialTheme.colorScheme.error) }
            } }, confirmButton = { TextButton(enabled = if (export) password.length >= 6 && password == confirmation else password.isNotEmpty(),
                onClick = { onPassword(password); password = ""; confirmation = "" }) { Text(stringResource(if (export) R.string.backup_export else R.string.backup_preview)) } },
            dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } })
    } else if (stage == DataStage.PREVIEW) {
        var merge by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = onCancel, title = { Text(stringResource(R.string.backup_preview)) }, text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(pluralStringResource(R.plurals.backup_period_count, state.periods, state.periods))
                Text(pluralStringResource(R.plurals.backup_day_count, state.days, state.days))
                Text(stringResource(R.string.backup_exported_at, state.exportedAt))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { RadioButton(!merge, { merge = false }); Text(stringResource(R.string.backup_overwrite)) }
                if (!state.recovery) Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { RadioButton(merge, { merge = true }); Text(stringResource(R.string.backup_merge)) }
                Text(stringResource(if (merge) R.string.backup_merge_body else R.string.backup_overwrite_body))
            }
        }, confirmButton = { TextButton(onClick = { onRestore(merge, false) }) { Text(stringResource(R.string.backup_restore)) } }, dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } })
    } else if (stage == DataStage.OVERWRITE) {
        AlertDialog(onDismissRequest = onCancel, title = { Text(stringResource(R.string.backup_overwrite)) }, text = { Text(stringResource(R.string.backup_overwrite_confirm)) },
            confirmButton = { TextButton(onClick = { onRestore(false, true) }) { Text(stringResource(R.string.confirm)) } }, dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } })
    } else if (stage == DataStage.CSV_WARNING) {
        var acknowledged by remember { mutableStateOf(false) }
        val label = stringResource(R.string.csv_acknowledge)
        AlertDialog(onDismissRequest = onCancel, title = { Text(stringResource(R.string.csv_export)) }, text = { Column {
            Text(stringResource(R.string.csv_warning), color = MaterialTheme.colorScheme.error)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Checkbox(acknowledged, { acknowledged = it }, modifier = Modifier.semantics { contentDescription = label }); Text(label) }
        } }, confirmButton = { TextButton(enabled = acknowledged, onClick = onCsv) { Text(stringResource(R.string.csv_export)) } }, dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } })
    } else if (stage == DataStage.BUSY) {
        AlertDialog(onDismissRequest = {}, text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { CircularProgressIndicator(); Text(stringResource(R.string.data_working)) } }, confirmButton = {})
    } else if (stage == DataStage.DONE) {
        AlertDialog(onDismissRequest = onCloseResult, title = { Text(stringResource(R.string.done)) }, text = { Column {
            if (state.saved) Text(stringResource(R.string.backup_saved))
            state.result?.let { result ->
                Text(pluralStringResource(R.plurals.import_added_periods, result.addedPeriods, result.addedPeriods))
                Text(pluralStringResource(R.plurals.import_added_days, result.addedDays, result.addedDays))
                Text(pluralStringResource(R.plurals.import_updated_days, result.updatedDays, result.updatedDays))
            }
        } }, confirmButton = { TextButton(onClick = onCloseResult) { Text(stringResource(R.string.done)) } })
    } else if (state.error != null) {
        AlertDialog(onDismissRequest = onCancel, text = { Text(stringResource(errorText(state.error))) }, confirmButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.done)) } })
    }
}
private fun errorText(error: BackupError) = when (error) {
    BackupError.NOT_BACKUP -> R.string.backup_not_file
    BackupError.NEWER_VERSION -> R.string.backup_new_version
    BackupError.AUTHENTICATION -> R.string.backup_wrong_password
    BackupError.TOO_LARGE -> R.string.backup_too_large
    BackupError.INVALID_DATA -> R.string.backup_invalid_data
    BackupError.STORAGE -> R.string.backup_storage_error
}
