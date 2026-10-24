package com.xichugeek.lunatide.ui.data

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.ui.security.EraseConfirmation
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import com.xichugeek.lunatide.ui.components.PageHeader

data class DataActions(val export: () -> Unit = {}, val import: (Boolean) -> Unit = {}, val csv: () -> Unit = {}, val erase: () -> Unit = {})

@Composable
fun DataSettingsScreen(actions: DataActions, onBack: () -> Unit) {
    var erase by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeader(stringResource(R.string.data_title), onBack) }
        item { Text(stringResource(R.string.data_notice_body)); Text(stringResource(R.string.backup_local_body)) }
        item { OutlinedButton(onClick = actions.export, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.backup_export)) } }
        item { OutlinedButton(onClick = { actions.import(false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.backup_import)) } }
        item { OutlinedButton(onClick = actions.csv, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(stringResource(R.string.csv_export)) } }
        item { TextButton(onClick = { erase = true }) { Text(stringResource(R.string.erase_all), color = MaterialTheme.colorScheme.error) } }
    }
    if (erase) EraseConfirmation({ erase = false }) { erase = false; actions.erase() }
}
@Composable
fun RecoveryScreen(actions: DataActions, onRetry: () -> Unit) {
    var erase by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.safeDrawingPadding().widthIn(max = 600.dp).fillMaxWidth(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(stringResource(R.string.recovery_title), style = MaterialTheme.typography.headlineMedium); Text(stringResource(R.string.recovery_body)) }
            item { Button(onClick = { actions.import(true) }) { Text(stringResource(R.string.backup_import)) } }
            item { OutlinedButton(onClick = { erase = true }) { Text(stringResource(R.string.recovery_reset)) } }
            item { TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) } }
        }
    }
    if (erase) EraseConfirmation({ erase = false }) { erase = false; actions.erase() }
}
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { DataSettingsScreen(DataActions(), {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { DataSettingsScreen(DataActions(), {}) } }
@Preview(locale = "zh") @Composable private fun RecoveryLightPreview() { LunatideTheme(false) { RecoveryScreen(DataActions(), {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun RecoveryDarkPreview() { LunatideTheme(true) { RecoveryScreen(DataActions(), {}) } }
