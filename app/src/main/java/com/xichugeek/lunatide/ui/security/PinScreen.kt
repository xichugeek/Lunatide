package com.xichugeek.lunatide.ui.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import com.xichugeek.lunatide.R
import com.xichugeek.lunatide.core.security.*
import com.xichugeek.lunatide.ui.components.PrivateTextInput
import com.xichugeek.lunatide.ui.theme.LunatideTheme

@Composable
fun PinScreen(state: LockUiState, digit: (String) -> Unit, submit: () -> Unit, dismiss: () -> Unit,
    biometric: () -> Unit, erase: () -> Unit) {
    var forgot by remember { mutableIntStateOf(0) }
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val shake = remember { Animatable(0f) }
    DisposableEffect(view) { val previous = view.isSoundEffectsEnabled; view.isSoundEffectsEnabled = false; onDispose { view.isSoundEffectsEnabled = previous } }
    LaunchedEffect(state.error) { if (state.error == LockError.WRONG_PIN) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        shake.animateTo(0f, keyframes { durationMillis = 300; -6f at 50; 6f at 100; -6f at 150; 6f at 200; 0f at 300 })
    } }
    val countDescription = stringResource(R.string.pin_entered_count, state.digits.length)
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(when (state.mode) {
                PinMode.SET -> R.string.pin_set_title; PinMode.CONFIRM -> R.string.pin_confirm_title
                PinMode.CHANGE, PinMode.DISABLE -> R.string.pin_old_title; else -> R.string.pin_unlock_title
            }), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.pin_hint))
            Text("● ".repeat(state.digits.length).trim().ifEmpty { "○ ○ ○ ○" },
                Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) }.clearAndSetSemantics { contentDescription = countDescription }, style = MaterialTheme.typography.headlineMedium)
            if (state.busy || !state.ready) CircularProgressIndicator()
            if (state.cooldownSeconds > 0) Text(pluralStringResource(R.plurals.pin_cooldown, state.cooldownSeconds, state.cooldownSeconds), color = MaterialTheme.colorScheme.error)
            state.error?.let { Text(stringResource(when (it) {
                LockError.WRONG_PIN -> R.string.pin_wrong; LockError.MISMATCH -> R.string.pin_mismatch
                LockError.STORAGE -> R.string.storage_error; LockError.BIOMETRIC -> R.string.pin_biometric_failed
            }), color = MaterialTheme.colorScheme.error) }
            val enabled = state.ready && !state.busy && state.cooldownSeconds == 0
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { number -> OutlinedButton(onClick = { digit(number) }, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(number, style = MaterialTheme.typography.titleLarge) } }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.locked && state.settings.lockBiometric && state.pinSet) TextButton(onClick = biometric, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(stringResource(R.string.pin_biometric)) }
                else Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { digit("0") }, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("0", style = MaterialTheme.typography.titleLarge) }
                OutlinedButton(onClick = { digit("BACK") }, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(stringResource(R.string.pin_backspace)) }
            }
            Button(onClick = submit, enabled = enabled && state.digits.length in 4..6, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.confirm)) }
            if (state.locked) {
                TextButton(onClick = { forgot = 1 }, enabled = !state.busy) { Text(stringResource(R.string.pin_forgot)) }
            } else TextButton(onClick = dismiss, enabled = !state.busy) { Text(stringResource(R.string.cancel)) }
        }
    }
    if (forgot == 1) AlertDialog(onDismissRequest = { forgot = 0 }, title = { Text(stringResource(R.string.pin_forgot)) }, text = { Text(stringResource(R.string.pin_forgot_body)) },
        confirmButton = { TextButton(onClick = { forgot = 2 }) { Text(stringResource(R.string.erase_all)) } }, dismissButton = { TextButton(onClick = { forgot = 0 }) { Text(stringResource(R.string.cancel)) } })
    if (forgot == 2) EraseConfirmation({ forgot = 0 }) { forgot = 0; erase() }
}

@Composable
fun EraseConfirmation(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val word = stringResource(R.string.erase_confirmation_word)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.erase_all)) },
        text = { Column { Text(stringResource(R.string.erase_warning, word)); PrivateTextInput(input, word) { input = it } } },
        confirmButton = { TextButton(enabled = input == word, onClick = onConfirm) { Text(stringResource(R.string.erase_all)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
@Preview(locale = "zh") @Composable private fun LightPreview() { LunatideTheme(false) { PinScreen(LockUiState(ready = true, mode = PinMode.UNLOCK), {}, {}, {}, {}, {}) } }
@Preview(locale = "zh", uiMode = 32) @Composable private fun DarkPreview() { LunatideTheme(true) { PinScreen(LockUiState(ready = true, mode = PinMode.SET, locked = false), {}, {}, {}, {}, {}) } }
