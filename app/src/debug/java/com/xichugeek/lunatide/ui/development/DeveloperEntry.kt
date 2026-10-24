package com.xichugeek.lunatide.ui.development

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.ui.theme.LunatideTheme

@Composable
fun DeveloperEntry() {
    val context = LocalContext.current
    val activity = remember(context) { findActivity(context) } ?: return
    val model = remember(activity) { ViewModelProvider(activity)[DemoViewModel::class.java] }
    val state by model.state.collectAsState()
    DeveloperCard(state, { model.fill() }, { model.fill(true) })
}
private tailrec fun findActivity(context: Context): ComponentActivity? = when (context) {
    is ComponentActivity -> context
    is ContextWrapper -> findActivity(context.baseContext)
    else -> null
}
@Composable private fun DeveloperCard(state: DemoState, onFill: () -> Unit, onPerformance: () -> Unit = {}) {
    var confirm by remember { mutableStateOf(false) }
    var performance by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        TextButton(onClick = { performance = false; confirm = true }, enabled = !state.busy) { Text(stringResource(R.string.developer_seed)) }
        TextButton(onClick = { performance = true; confirm = true }, enabled = !state.busy) { Text(stringResource(R.string.developer_performance)) }
        if (state.busy) CircularProgressIndicator()
        if (state.done) Text(stringResource(R.string.developer_seed_done))
        if (state.failed) Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error)
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.developer_seed)) },
        text = { Text(stringResource(if (performance) R.string.developer_performance_warning else R.string.developer_seed_warning)) }, confirmButton = { TextButton(onClick = { confirm = false; if (performance) onPerformance() else onFill() }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } })
}
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { DeveloperCard(DemoState(), {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { DeveloperCard(DemoState(), {}) } }
