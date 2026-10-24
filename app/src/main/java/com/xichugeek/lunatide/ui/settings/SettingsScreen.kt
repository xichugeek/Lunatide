package com.xichugeek.lunatide.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.data.settings.AppSettings
import com.xichugeek.lunatide.domain.prediction.PredictionResult
import com.xichugeek.lunatide.ui.theme.LunatideTheme
import com.xichugeek.lunatide.ui.components.PageHeader

@Composable
fun SettingsScreen(s: AppSettings, prediction: PredictionResult?, onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onReminders: () -> Unit, onSecurity: () -> Unit, onData: () -> Unit, onAbout: () -> Unit, onBack: () -> Unit) {
    var length by remember { mutableIntStateOf(0) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeader(stringResource(R.string.settings), onBack) }
        item { Section(R.string.cycle_settings)
            val cycle = prediction?.cycleLength ?: 28
            val period = prediction?.periodLength ?: 5
            SettingLink(R.string.average_cycle, s.manualCycleLength?.let { pluralStringResource(R.plurals.days_value, it, it) } ?: pluralStringResource(R.plurals.auto_length, cycle, cycle)) { length = 1 }
            SettingLink(R.string.average_period, s.manualPeriodLength?.let { pluralStringResource(R.plurals.days_value, it, it) } ?: pluralStringResource(R.plurals.auto_length, period, period)) { length = 2 }
            SettingSwitch(R.string.show_fertility, s.showFertility) { onUpdate { old -> old.copy(showFertility = it) } }
            Text(stringResource(R.string.health_short), style = MaterialTheme.typography.bodySmall)
        }
        item { Section(R.string.record_settings)
            SettingSwitch(R.string.show_bbt, s.showBbt) { onUpdate { old -> old.copy(showBbt = it) } }
            SettingSwitch(R.string.show_intercourse, s.showIntercourse) { onUpdate { old -> old.copy(showIntercourse = it) } }
            Text(stringResource(R.string.week_start), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("MONDAY" to R.string.monday, "SUNDAY" to R.string.sunday).forEach { (value, label) ->
                    FilterChip(s.weekStart == value, { onUpdate { it.copy(weekStart = value) } }, label = { Text(stringResource(label)) }, modifier = Modifier.heightIn(min = 48.dp))
                }
            }
        }
        item { SettingLink(R.string.reminders, onClick = onReminders) }
        item { SettingLink(R.string.security_title, onClick = onSecurity) }
        item { SettingLink(R.string.data_title, onClick = onData) }
        item { Section(R.string.appearance)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("SYSTEM" to R.string.theme_system, "LIGHT" to R.string.theme_light, "DARK" to R.string.theme_dark).forEach { (value, label) ->
                FilterChip(s.theme == value, { onUpdate { it.copy(theme = value) } }, label = { Text(stringResource(label)) }, modifier = Modifier.heightIn(min = 48.dp))
            } }
        }
        item { SettingLink(R.string.about, onClick = onAbout) }
    }
    if (length != 0) {
        val cycle = length == 1
        var automatic by remember(cycle) { mutableStateOf(if (cycle) s.manualCycleLength == null else s.manualPeriodLength == null) }
        var value by remember(cycle) { mutableIntStateOf(if (cycle) s.manualCycleLength ?: prediction?.cycleLength ?: 28 else s.manualPeriodLength ?: prediction?.periodLength ?: 5) }
        val range = if (cycle) 15..90 else 1..14
        AlertDialog(onDismissRequest = { length = 0 }, title = { Text(stringResource(if (cycle) R.string.average_cycle else R.string.average_period)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingSwitch(R.string.automatic, automatic) { automatic = it }
                if (!automatic) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ value-- }, enabled = value > range.first) { Text(stringResource(R.string.decrease)) }
                        Text(pluralStringResource(R.plurals.days_value, value, value))
                        OutlinedButton({ value++ }, enabled = value < range.last) { Text(stringResource(R.string.increase)) }
                    }
                }
            } }, confirmButton = { TextButton(onClick = { onUpdate { if (cycle) it.copy(manualCycleLength = value.takeUnless { automatic }) else it.copy(manualPeriodLength = value.takeUnless { automatic }) }; length = 0 }) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { TextButton(onClick = { length = 0 }) { Text(stringResource(R.string.cancel)) } })
    }
}
@Composable private fun Section(id: Int) { Text(stringResource(id), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
@Composable private fun SettingLink(id: Int, detail: String? = null, onClick: () -> Unit) {
    Surface(onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(stringResource(id), style = MaterialTheme.typography.titleSmall); detail?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) } }
            Icon(painterResource(R.drawable.ic_chevron), null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}
@Composable private fun SettingSwitch(id: Int, checked: Boolean, change: (Boolean) -> Unit) {
    val label = stringResource(id)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, change, modifier = Modifier.semantics { contentDescription = label })
    }
}
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { SettingsScreen(AppSettings(), null, {}, {}, {}, {}, {}, {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { SettingsScreen(AppSettings(), null, {}, {}, {}, {}, {}, {}) } }
