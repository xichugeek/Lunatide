package com.xichugeek.lunatide.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.core.security.*
import com.xichugeek.lunatide.data.settings.AppSettings
import com.xichugeek.lunatide.ui.theme.LunatideTheme

@Composable
fun SecuritySettingsScreen(state: LockUiState, biometricAvailable: Boolean, onPin: (PinMode) -> Unit,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit, onBack: () -> Unit) {
    val s = state.settings
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }; Text(stringResource(R.string.security_title), style = MaterialTheme.typography.headlineMedium) }
        item { TextButton(onClick = { onPin(if (state.pinSet) PinMode.CHANGE else PinMode.SET) }) { Text(stringResource(if (state.pinSet) R.string.pin_change else R.string.pin_enable)) }
            if (state.pinSet) TextButton(onClick = { onPin(PinMode.DISABLE) }) { Text(stringResource(R.string.pin_disable)) }
            Text(stringResource(R.string.pin_explanation), style = MaterialTheme.typography.bodySmall) }
        item { SecurityToggle(R.string.pin_biometric, s.lockBiometric, state.pinSet && biometricAvailable) { onUpdate { s -> s.copy(lockBiometric = it) } }
            if (!biometricAvailable) Text(stringResource(R.string.pin_biometric_unavailable)) }
        item { Text(stringResource(R.string.pin_timeout), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(0 to R.string.lock_immediate, 30 to R.string.lock_30seconds, 60 to R.string.lock_1minute, 300 to R.string.lock_5minutes).forEach { (value, label) ->
                FilterChip(s.lockTimeoutSeconds == value, { onUpdate { it.copy(lockTimeoutSeconds = value) } }, label = { Text(stringResource(label)) }, modifier = Modifier.heightIn(min = 48.dp))
            } }
        }
        item { SecurityToggle(R.string.secure_screen, s.secureScreen) { onUpdate { s -> s.copy(secureScreen = it) } }; Text(stringResource(R.string.secure_screen_body), style = MaterialTheme.typography.bodySmall) }
        item { SecurityToggle(R.string.notification_private_mode, s.notificationPrivateMode) { onUpdate { s -> s.copy(notificationPrivateMode = it) } }; Text(stringResource(R.string.notification_private_body), style = MaterialTheme.typography.bodySmall) }
        if (state.error == LockError.STORAGE) item { Text(stringResource(R.string.storage_error), color = MaterialTheme.colorScheme.error) }
    }
}
@Composable private fun SecurityToggle(labelId: Int, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val label = stringResource(labelId)
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(checked, onChange, enabled = enabled, modifier = Modifier.semantics { contentDescription = label })
    }
}
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { SecuritySettingsScreen(LockUiState(ready = true), false, {}, {}, {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { SecuritySettingsScreen(LockUiState(ready = true, pinSet = true), true, {}, {}, {}) } }
